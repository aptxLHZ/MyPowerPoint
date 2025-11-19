package com.myppt.model;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D; // 使用Ellipse2D可以更精确地进行绘制和碰撞检测
import java.awt.BasicStroke;

public class EllipseShape extends AbstractSlideObject {
    private static final long serialVersionUID = 1L;
    
    private int width;
    private int height;
    private Color fillColor;
    private Color borderColor = Color.BLACK; // [!] 新增: 默认为黑色
    private double borderWidth = 1.0f;        // [!] 新增: 默认为1像素

    // [!] 新增: 线型定义
    public static final int BORDER_STYLE_SOLID = 0;
    public static final int BORDER_STYLE_DASHED = 1;
    public static final int BORDER_STYLE_DOTTED = 2;

    private int borderStyle = BORDER_STYLE_SOLID; // 默认为实线

    public EllipseShape(int x, int y, int width, int height, Color fillColor) {
        super(x, y);
        this.width = width;
        this.height = height;
        this.fillColor = fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }
    public Color getBorderColor() { return borderColor; }
    public void setBorderColor(Color borderColor) { this.borderColor = borderColor; }
    public double getBorderWidth() { return borderWidth; }
    public void setBorderWidth(double borderWidth) { this.borderWidth = borderWidth; }
    public int getBorderStyle() { return borderStyle; }
    public void setBorderStyle(int borderStyle) { this.borderStyle = borderStyle; }

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
        
        // [!] 核心修改: 绘制自定义边框
        if (borderWidth > 0) { // 只有当边框宽度大于0时才绘制
            g2d.setColor(this.borderColor);

            Stroke borderStroke;
            float width = (float) this.borderWidth;
            switch (borderStyle) {
                case BORDER_STYLE_DASHED:
                    // 虚线: 10像素实，5像素空
                    borderStroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 5.0f}, 0.0f);
                    break;
                case BORDER_STYLE_DOTTED:
                    // 点线: 宽度本身是点，宽度本身是空
                    borderStroke = new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{width, width * 2}, 0.0f);
                    break;
                case BORDER_STYLE_SOLID:
                default:
                    borderStroke = new BasicStroke(width);
                    break;
            }
            g2d.setStroke(borderStroke);
            g2d.drawOval(this.x, this.y, this.width, this.height);
        }

        // 如果被选中，绘制高亮
        if (this.selected) {
            // [!] 核心修复: 添加虚线框的绘制，并且画的是椭圆
            g2d.setColor(Color.LIGHT_GRAY);
            Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0);
            g2d.setStroke(dashed);
            // [!] 画一个比原椭圆稍大的虚线椭圆
            g2d.drawOval(this.x - 3, this.y - 3, this.width + 6, this.height + 6);
            
            // 恢复实线笔触再画控制点
            g2d.setStroke(originalStroke); 
            g2d.setColor(Color.BLUE);
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