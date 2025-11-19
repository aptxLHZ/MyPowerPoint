package com.myppt.model;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D; // 使用Ellipse2D可以更精确地进行绘制和碰撞检测

public class EllipseShape extends AbstractSlideObject {
    private static final long serialVersionUID = 1L;
    
    private int width;
    private int height;
    private Color fillColor;

    public EllipseShape(int x, int y, int width, int height, Color fillColor) {
        super(x, y);
        this.width = width;
        this.height = height;
        this.fillColor = fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    @Override
    public boolean contains(Point p) {
        // 使用Ellipse2D的几何对象来进行精确的碰撞检测
        Ellipse2D.Float ellipse = new Ellipse2D.Float(x, y, width, height);
        return ellipse.contains(p);
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        Color originalColor = g2d.getColor();
        Stroke originalStroke = g2d.getStroke();

        // 绘制填充椭圆
        g2d.setColor(this.fillColor);
        g2d.fillOval(this.x, this.y, this.width, this.height);
        
        // 绘制边框
        g2d.setColor(Color.BLACK);
        g2d.drawOval(this.x, this.y, this.width, this.height);

        // 如果被选中，绘制高亮
        if (this.selected) {
            g2d.setColor(Color.BLUE);
            Stroke dashed = new java.awt.BasicStroke(2, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_BEVEL, 0, new float[]{10, 5}, 0);
            g2d.setStroke(dashed);
            // 绘制一个包围框
            g2d.drawRect(this.x - 3, this.y - 3, this.width + 6, this.height + 6);

            //画控制点
            for (Rectangle handle : getResizeHandles().values()) {
                g2d.fill(handle);
            }
        }

        g2d.setColor(originalColor);
        g2d.setStroke(originalStroke);
    }

    public Color getFillColor() {
        return this.fillColor;
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