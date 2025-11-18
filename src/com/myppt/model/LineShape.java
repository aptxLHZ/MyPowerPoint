package com.myppt.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.Line2D; // 用于精确的碰撞检测

public class LineShape extends AbstractSlideObject {
    // 使用 x, y 作为起点 (x1, y1)，并额外存储终点 (x2, y2)
    public int x2;
    public int y2;
    private Color lineColor;
    private float strokeWidth;

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
        return distance < 5; // 5像素的点击容差
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        Color originalColor = g2d.getColor();
        Stroke originalStroke = g2d.getStroke();

        g2d.setColor(this.lineColor);
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
}