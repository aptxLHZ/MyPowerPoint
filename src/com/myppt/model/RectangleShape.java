package com.myppt.model;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.BasicStroke; // [!] 新增
import java.awt.Stroke;      // [!] 新增
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * 表示一个矩形。这是第一个具体的幻灯片元素。
 */
public class RectangleShape extends AbstractSlideObject {
    private static final long serialVersionUID = 1L;
    private int width;
    private int height;
    private Color fillColor;
    private Color borderColor = Color.BLACK; // [!] 新增: 默认为黑色
    private double borderWidth = 1.0f;        // [!] 新增: 默认为1像素

    private int borderStyle = BORDER_STYLE_SOLID; // 默认为实线

    public RectangleShape(int x, int y, int width, int height, Color fillColor) {
        super(x, y); // 调用父类的构造方法，设置x, y坐标
        this.width = width;
        this.height = height;
        this.fillColor = fillColor;
    }
    public Color getBorderColor() { return borderColor; }
    public void setBorderColor(Color borderColor) { this.borderColor = borderColor; }
    public double getBorderWidth() { return borderWidth; }
    public void setBorderWidth(double borderWidth) { this.borderWidth = borderWidth; }
    public int getBorderStyle() { return borderStyle; }
    public void setBorderStyle(int borderStyle) { this.borderStyle = borderStyle; }
    // [!] 新增: 设置填充颜色的方法
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    // 实现父类中定义的抽象方法
    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g; // [!] 转换为 Graphics2D
        
        // 保存原始颜色和笔触
        Color originalColor = g2d.getColor();
        Stroke originalStroke = g2d.getStroke();

        // 绘制填充矩形
        g2d.setColor(this.fillColor);
        g2d.fillRect(this.x, this.y, this.width, this.height);
        
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
            g2d.drawRect(this.x, this.y, this.width, this.height);
        }

        // [!] 新增: 如果对象被选中，绘制一个虚线框作为高亮
        if (this.selected) {
            g2d.setColor(Color.BLUE);
            // 创建一个虚线笔触: 10个像素实线，5个像素空白
            Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10, 5}, 0);
            g2d.setStroke(dashed);
            // 绘制一个比原矩形稍大的框
            g2d.drawRect(this.x - 3, this.y - 3, this.width + 6, this.height + 6);
            
            //画控制点
            for (Rectangle handle : getResizeHandles().values()) {
                g2d.fill(handle);
            }
        }

        // 恢复原始颜色和笔触
        g2d.setColor(originalColor);
        g2d.setStroke(originalStroke);
    }

    // [!] 新增: 实现 contains 方法
    @Override
    public boolean contains(Point p) {
        // 判断点 p 的 x, y 坐标是否在矩形的范围内
        return p.x >= this.x && p.x <= (this.x + this.width) &&
               p.y >= this.y && p.y <= (this.y + this.height);
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
    @Override
    public Style getStyle() {
        return new ShapeStyle(this.fillColor, this.borderColor, this.borderWidth, this.borderStyle);
    }

    @Override
    public void setStyle(Style style) {
        if (style instanceof ShapeStyle) {
            ShapeStyle ss = (ShapeStyle) style;
            this.setFillColor(ss.getFillColor());
            this.setBorderColor(ss.getBorderColor());
            this.setBorderWidth(ss.getBorderWidth());
            this.setBorderStyle(ss.getBorderStyle());
        }
    }
}