package com.myppt.model;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image; // 使用 java.awt.Image 来存储图片数据
import java.awt.Point;
import java.awt.Stroke;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.Rectangle;


public class ImageObject extends AbstractSlideObject {
    private static final long serialVersionUID = 1L;
    
    // transient 关键字告诉Java序列化机制：不要尝试保存这个字段。
    // 因为Image对象本身通常不是Serializable的，而且我们应该保存图片路径而不是图片数据。
    private transient Image image;
    public String imagePath; // 我们保存图片的路径，以便可以重新加载
    private int width;
    private int height;
    private final double aspectRatio;
    private float opacity = 1.0f;

    public ImageObject(int x, int y, String path) throws IOException {
        super(x, y);
        this.imagePath = path;
        loadImage(); // 从路径加载图片
        if (this.image != null) {
            this.width = this.image.getWidth(null);
            this.height = this.image.getHeight(null);
            this.aspectRatio = (double) this.width / this.height;
        } else {
            this.width = 100; // 默认值
            this.height = 100; // 默认值
            this.aspectRatio = 1.0;
            throw new IOException("无法加载图片: " + this.imagePath);
        }
    }

    // 一个私有方法，用于从路径加载图片
    private void loadImage() throws IOException {
        this.image = ImageIO.read(new File(this.imagePath));
        if (this.image != null) {
            this.width = this.image.getWidth(null); // 获取图片的原始宽度
            this.height = this.image.getHeight(null); // 获取图片的原始高度
        } else {
            throw new IOException("无法加载图片: " + this.imagePath);
        }
    }

    // [!] 新增: 自定义反序列化方法
    private void readObject(java.io.ObjectInputStream in) 
        throws IOException, ClassNotFoundException {
        // 先调用默认的反序列化方法，它会读取所有非transient的字段 (比如 imagePath)
        in.defaultReadObject();
        // 然后，手动加载被忽略的 transient 字段
        try {
            loadImage();
        } catch (IOException e) {
            System.err.println("反序列化时加载图片失败: " + imagePath);
            // 可以在这里设置一个“图片加载失败”的占位图
            image = null;
        }
    }

    @Override
    public boolean contains(Point p) {
        return p.x >= this.x && p.x <= (this.x + this.width) &&
               p.y >= this.y && p.y <= (this.y + this.height);
    }

    @Override
    public void draw(Graphics g) {
        if (image == null) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(this.x, this.y, this.width, this.height);
            g.setColor(Color.RED);
            g.drawString("图片加载失败", this.x + 10, this.y + this.height / 2);
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        Stroke originalStroke = g2d.getStroke();

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, this.opacity));

        // 绘制图片
        g2d.drawImage(this.image, this.x, this.y, this.width, this.height, null);

        // 如果被选中，绘制包围框
        if (this.selected) {
            g2d.setColor(Color.BLUE);
            Stroke dashed = new java.awt.BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10.0f, new float[]{4.0f, 4.0f}, 0.0f);
            g2d.setStroke(dashed);
            g2d.drawRect(this.x - 3, this.y - 3, this.width + 6, this.height + 6);
            g2d.setStroke(originalStroke); // 恢复笔触再画控制点
            for (Rectangle handle : getResizeHandles().values()) {
                g2d.fill(handle);
            }
        }
        // [!] 恢复 Graphics2D 的透明度，避免影响后续绘制
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
    public String getImagePath() { return imagePath; }

}