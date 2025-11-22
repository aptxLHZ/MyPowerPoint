package com.myppt.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Line2D; // 用于精确的碰撞检测
import java.util.EnumMap;
import java.util.Map;

import com.myppt.controller.strategies.ResizeHandle;

public class LineShape extends AbstractSlideObject {
    private static final long serialVersionUID = 1L;
    
    // 使用 x, y 作为起点 (x1, y1)，并额外存储终点 (x2, y2)
    public int x2;
    public int y2;
    private Color lineColor;
    private float strokeWidth;
    private int borderStyle = AbstractSlideObject.BORDER_STYLE_SOLID; 

    public LineShape(int x1, int y1, int x2, int y2, Color color, float width) {
        super(x1, y1); // 父类的 x, y 就代表我们的 x1, y1
        this.x2 = x2;
        this.y2 = y2;
        this.lineColor = color;
        this.strokeWidth = width;
    }

    // 重写 setX/Y 以确保移动时起点和终点一起动
    @Override
    public void setX(int x1) {
        int dx = x1 - this.x; // 计算x方向的移动量
        this.x = x1;
        this.x2 += dx; // 终点也移动相同的量
    }

    @Override
    public void setY(int y1) {
        int dy = y1 - this.y; // 计算y方向的移动量
        this.y = y1;
        this.y2 += dy; // 终点也移动相同的量
    }
    
    public void setLineColor(Color color) {
        this.lineColor = color;
    }

    @Override
    public boolean contains(Point p) {
        // Line2D.ptSegDist 可以计算一个点到线段的最短距离
        // 如果这个距离小于一个阈值（比如5个像素），我们就认为点中了这条线
        double distance = Line2D.ptSegDist(this.x, this.y, this.x2, this.y2, p.x, p.y);
        return distance < 8; // 5像素的点击容差
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        Color originalColor = g2d.getColor();
        Stroke originalStroke = g2d.getStroke();

        g2d.setColor(this.lineColor);

        float width = this.strokeWidth;
        Stroke lineStroke;
        switch (borderStyle) {
            case AbstractSlideObject.BORDER_STYLE_DASHED:
                lineStroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 5.0f}, 0.0f);
                break;
            case AbstractSlideObject.BORDER_STYLE_DOTTED:
                lineStroke = new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{width, width * 2}, 0.0f);
                break;
            case AbstractSlideObject.BORDER_STYLE_SOLID:
            default:
                lineStroke = new BasicStroke(width);
                break;
        }

        g2d.setStroke(new BasicStroke(this.strokeWidth)); // 设置线条粗细
        g2d.drawLine(this.x, this.y, this.x2, this.y2);

        if (this.selected) {
            g2d.setColor(Color.BLUE);
            // 选中时，在起点和终点画两个小方块作为控制点
            g2d.fillRect(this.x - 4, this.y - 4, 8, 8);
            g2d.fillRect(this.x2 - 4, this.y2 - 4, 8, 8);
        }

        g2d.setColor(originalColor);
        g2d.setStroke(originalStroke);
    }

    public Color getLineColor() {
        return this.lineColor;
    }

    @Override
    public Rectangle getBounds() {
        // 计算能包围整条线的最小矩形
        int minX = Math.min(this.x, this.x2);
        int minY = Math.min(this.y, this.y2);
        int width = Math.abs(this.x - this.x2);
        int height = Math.abs(this.y - this.y2);
        return new Rectangle(minX, minY, width, height);
    }

    @Override
    public void setBounds(Rectangle bounds) {
        // 这个方法对于直线来说意义不大，可以空着，或者做一个简单的实现
        // 我们的主要逻辑在 SelectStrategy 中
    }

    @Override
    public Map<ResizeHandle, Rectangle> getResizeHandles() {
        EnumMap<ResizeHandle, Rectangle> handles = new EnumMap<>(ResizeHandle.class);
        // [!] 关键: 无论直线方向如何，TOP_LEFT 永远代表起点，BOTTOM_RIGHT 永远代表终点
        handles.put(ResizeHandle.TOP_LEFT, new Rectangle(x - HANDLE_SIZE / 2, y - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE));
        handles.put(ResizeHandle.BOTTOM_RIGHT, new Rectangle(x2 - HANDLE_SIZE / 2, y2 - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE));
        return handles;
    }


    @Override
    public Style getStyle() {
        return new ShapeStyle(Color.BLACK, this.lineColor, this.strokeWidth, this.borderStyle); 
    }

    @Override
    public void setStyle(Style style) {
        if (style instanceof ShapeStyle) {
            ShapeStyle ss = (ShapeStyle) style;
            this.setLineColor(ss.getBorderColor());
            this.setStrokeWidth((float)ss.getBorderWidth());
            this.setBorderStyle(ss.getBorderStyle());
        }
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
    }
    public float getStrokeWidth() {
        return this.strokeWidth;
    }

    public int getBorderStyle() { return borderStyle; }
    public void setBorderStyle(int borderStyle) { this.borderStyle = borderStyle; }

}