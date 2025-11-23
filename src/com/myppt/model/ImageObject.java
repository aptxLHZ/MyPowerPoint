package com.myppt.model;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image; // 使用 java.awt.Image 来存储图片数据
import java.awt.Point;
import java.awt.Stroke;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.awt.Rectangle;


public class ImageObject extends AbstractSlideObject {
    private static final long serialVersionUID = 1L;
    
    // transient 关键字告诉Java序列化机制：不要尝试保存这个字段。
    // 因为Image对象本身通常不是Serializable的，而且我们应该保存图片路径而不是图片数据。

    private transient Image image; // 运行时缓存，依然 transient
    private byte[] imageData; // [!] 核心修改: 不再是路径，而是字节数组
    private int width;
    private int height;
    private final double aspectRatio;
    private float opacity = 1.0f;

    public ImageObject(int x, int y, byte[] imageData) throws IOException {
        super(x, y);
        this.imageData = imageData;
        
        try { // [!] 核心修复: 捕获异常
            loadImageFromData(); 
        } catch (IOException e) {
            System.err.println("ImageObject 构造函数中加载图片失败: " + e.getMessage());
            this.image = null; // 确保在加载失败时 image 为 null
            throw e; // 重新抛出，让 AppController 处理
        }
        
        if (this.image != null) {
            int originalWidth = this.image.getWidth(null);
            int originalHeight = this.image.getHeight(null);
            this.aspectRatio = (double) originalWidth / originalHeight;

            // [!] 核心修复: 限制图片插入时的默认尺寸
            // 假设我们希望默认插入的图片宽度不超过页面宽度的一半
            int defaultMaxWidth = Slide.PAGE_WIDTH / 2;
            int defaultMaxHeight = Slide.PAGE_HEIGHT / 2;

            this.width = originalWidth;
            this.height = originalHeight;

            // 如果原始宽度超过默认最大宽度
            if (this.width > defaultMaxWidth) {
                this.width = defaultMaxWidth;
                this.height = (int) (this.width / this.aspectRatio); // 等比例缩放高度
            }
            // 如果高度仍然太大 (例如图片很长很窄)
            if (this.height > defaultMaxHeight) {
                this.height = defaultMaxHeight;
                this.width = (int) (this.height * this.aspectRatio); // 等比例缩放宽度
            }
            // [!] 确保图片有一个最小尺寸，防止过小而无法选中
            if (this.width < 50) this.width = 50;
            if (this.height < 50) this.height = 50;

        } else {
            this.width = 100; // 默认值
            this.height = 100; // 默认值
            this.aspectRatio = 1.0;
            throw new IOException("无法从字节数据加载图片。");
        }
    }

    // [!] 核心修改: 从字节数组加载 Image 对象
    private void loadImageFromData() throws IOException {
        if (imageData == null || imageData.length == 0) {
            this.image = null;
            System.err.println("警告: 图像数据为空，无法加载图片。");
            return;
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
            this.image = ImageIO.read(bais);
            if (this.image == null) {
                System.err.println("错误: ImageIO.read() 返回 null，可能图片格式不受支持或数据损坏。数据长度: " + imageData.length);
            } else {
                System.out.println("图片已从字节数据成功加载。宽度: " + this.image.getWidth(null) + ", 高度: " + this.image.getHeight(null));
            }
        } catch (IOException e) {
            System.err.println("严重错误: 从字节数据加载图片时发生 IOException: " + e.getMessage());
            e.printStackTrace();
            this.image = null; // 确保在异常时设置为null
            throw e; // 重新抛出异常，让调用者知道加载失败
        }
    }

    // [!] 核心修改: 自定义反序列化方法，从 imageData 重新加载 Image
    private void readObject(java.io.ObjectInputStream in) 
        throws IOException, ClassNotFoundException {
        in.defaultReadObject(); // 读取非 transient 字段 (imageData)
        try {
            loadImageFromData(); // 从已读取的 imageData 重新加载 Image
        } catch (IOException e) {
            System.err.println("反序列化时从字节数据加载图片失败: " + e.getMessage());
            this.image = null;
        }
    }

    @Override
    public boolean contains(Point p) {
        return p.x >= this.x && p.x <= (this.x + this.width) &&
               p.y >= this.y && p.y <= (this.y + this.height);
    }

    @Override
    public void draw(Graphics g) {
        // 1. 如果图片加载失败 (image 为 null)，则绘制一个占位符
        if (image == null) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(this.x, this.y, this.width, this.height);
            g.setColor(Color.RED);
            g.drawString("图片加载失败", this.x + 10, this.y + this.height / 2);
            return;
        }

        // [!] 核心修复: 图片加载成功时，执行实际的绘图逻辑！

        Graphics2D g2d = (Graphics2D) g;
        Stroke originalStroke = g2d.getStroke();
        
        // 2. 应用透明度
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, this.opacity));

        // 3. 绘制图片
        g2d.drawImage(this.image, this.x, this.y, this.width, this.height, null);

        // 4. 绘制选中框
        if (this.selected) {
            g2d.setColor(Color.BLUE);
            Stroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10.0f, new float[]{4.0f, 4.0f}, 0.0f);
            g2d.setStroke(dashed);
            g2d.drawRect(this.x - 3, this.y - 3, this.width + 6, this.height + 6);
            g2d.setStroke(originalStroke); // 恢复笔触再画控制点
            for (Rectangle handle : getResizeHandles().values()) {
                g2d.fill(handle);
            }
        }
        // 5. 恢复 Graphics2D 的透明度，避免影响后续绘制
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); 
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }

    @Override
    public void setBounds(Rectangle bounds) {
        this.x = bounds.x;
        this.y = bounds.y;
        this.width = bounds.width;
        this.height = bounds.height;
    }

    @Override
    public Style getStyle() {
        return new ImageStyle(this.opacity); // ImageStyle 尚未创建，此处先留着，后续创建
    }

    @Override
    public void setStyle(Style style) {
        if (style instanceof ImageStyle) { // ImageStyle 尚未创建，此处先留着，后续创建
            ImageStyle is = (ImageStyle) style;
            this.setOpacity(is.getOpacity());
        }
    }

    public float getOpacity() { return opacity; }
    public void setOpacity(float opacity) { 
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity)); // 限制在0到1之间
    }

    public double getAspectRation(){ return this.aspectRatio; }
    // public String getImagePath() { return imagePath; }
    public byte[] getImageData() { return imageData; }

}