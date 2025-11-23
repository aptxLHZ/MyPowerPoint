package com.myppt.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.util.EnumMap;
import java.util.Map;

import com.myppt.controller.strategies.ResizeHandle;

public class LineShape extends AbstractSlideObject {
    private static final long serialVersionUID = 1L;
    
    public int x2;
    public int y2;
    private Color lineColor;
    private float strokeWidth;
    private int borderStyle = AbstractSlideObject.BORDER_STYLE_SOLID; 

    public LineShape(int x1, int y1, int x2, int y2, Color color, float width) {
        super(x1, y1);
        this.x2 = x2;
        this.y2 = y2;
        this.lineColor = color;
        this.strokeWidth = width;
    }

    @Override
    public void setX(int x1) {
        int dx = x1 - this.x;
        this.x = x1;
        this.x2 += dx;
    }

    @Override
    public void setY(int y1) {
        int dy = y1 - this.y;
        this.y = y1;
        this.y2 += dy;
    }
    
    public void setLineColor(Color color) {
        this.lineColor = color;
    }

    @Override
    public boolean contains(Point p) {
        double distance = Line2D.ptSegDist(this.x, this.y, this.x2, this.y2, p.x, p.y);
        return distance < 8;
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

        // [FIX] 使用上面 switch 语句中创建的 lineStroke，而不是创建一个新的
        g2d.setStroke(lineStroke);
        g2d.drawLine(this.x, this.y, this.x2, this.y2);

        if (this.selected) {
            g2d.setColor(Color.BLUE);
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
        int minX = Math.min(this.x, this.x2);
        int minY = Math.min(this.y, this.y2);
        int width = Math.abs(this.x - this.x2);
        int height = Math.abs(this.y - this.y2);
        return new Rectangle(minX, minY, width, height);
    }

    @Override
    public void setBounds(Rectangle bounds) {
        // Not implemented for lines
    }

    @Override
    public Map<ResizeHandle, Rectangle> getResizeHandles() {
        EnumMap<ResizeHandle, Rectangle> handles = new EnumMap<>(ResizeHandle.class);
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