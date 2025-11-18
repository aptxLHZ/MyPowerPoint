package com.myppt.model;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image; // 使用 java.awt.Image 来存储图片数据
import java.awt.Point;
import java.awt.Stroke;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.Rectangle;

public class ImageObject extends AbstractSlideObject {
    // transient 关键字告诉Java序列化机制：不要尝试保存这个字段。
    // 因为Image对象本身通常不是Serializable的，而且我们应该保存图片路径而不是图片数据。
    private transient Image image;
    private String imagePath; // 我们保存图片的路径，以便可以重新加载
    private int width;
    private int height;

    public ImageObject(int x, int y, String path) throws IOException {
        super(x, y);
        this.imagePath = path;
        loadImage(); // 从路径加载图片
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

    @Override
    public boolean contains(Point p) {
        return p.x >= this.x && p.x <= (this.x + this.width) &&
               p.y >= this.y && p.y <= (this.y + this.height);
    }

    @Override
    public void draw(Graphics g) {
        if (image == null) return; // 如果图片加载失败，就不绘制

        Graphics2D g2d = (Graphics2D) g;
        Stroke originalStroke = g2d.getStroke();

        // 绘制图片
        g2d.drawImage(this.image, this.x, this.y, this.width, this.height, null);

        // 如果被选中，绘制包围框
        if (this.selected) {
            g2d.setColor(java.awt.Color.BLUE);
            Stroke dashed = new java.awt.BasicStroke(2, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_BEVEL, 0, new float[]{10, 5}, 0);
            g2d.setStroke(dashed);
            g2d.drawRect(this.x - 3, this.y - 3, this.width + 6, this.height + 6);
            g2d.setStroke(originalStroke);

            //画控制点
            for (Rectangle handle : getResizeHandles().values()) {
                g2d.fill(handle);
            }
        }
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
}