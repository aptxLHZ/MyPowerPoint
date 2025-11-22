package com.myppt.commands;

import java.awt.Color;
import com.myppt.model.EllipseShape;
import com.myppt.model.RectangleShape;
import com.myppt.model.LineShape; // [!] 新增

public class ChangeBorderCommand implements Command {
    // [!] 核心修复: 统一接口，包含所有图形的边框/线条属性
    interface Borderable {
        // 用于 Rectangle/Ellipse
        void setBorderColor(Color color);
        Color getBorderColor();
        void setBorderWidth(double width);
        double getBorderWidth();
        void setBorderStyle(int style);
        int getBorderStyle();
        
        // 用于 LineShape
        void setLineColor(Color color);
        Color getLineColor();
        void setStrokeWidth(float width); // 注意这里是 float
        float getStrokeWidth();
    }

    private Borderable target;
    
    private Color oldColor, newColor;
    private double oldWidth, newWidth; // 统一用 double
    private int oldStyle, newStyle;

    public ChangeBorderCommand(Object targetObject, Color newColor, double newWidth, int newStyle) {
        if (targetObject instanceof RectangleShape) {
            this.target = new BorderableRectangleShape((RectangleShape) targetObject);
        } else if (targetObject instanceof EllipseShape) {
            this.target = new BorderableEllipseShape((EllipseShape) targetObject);
        } else if (targetObject instanceof LineShape) { // [!] 新增
            this.target = new BorderableLineShape((LineShape) targetObject);
        } else {
            throw new IllegalArgumentException("Target object does not support border/line modifications.");
        }
        
        // 记录旧状态
        this.oldColor = target.getLineColor(); // 先尝试获取线条色
        if (this.oldColor == null) { // 如果是图形，则获取边框色
            this.oldColor = target.getBorderColor();
        }
        this.oldWidth = target.getStrokeWidth(); // 先尝试获取线条粗细
        if (this.oldWidth == 0.0f) { // 如果是图形，则获取边框粗细
             this.oldWidth = target.getBorderWidth();
        }
        this.oldStyle = target.getBorderStyle();
        
        // 记录新状态
        this.newColor = newColor;
        this.newWidth = newWidth;
        this.newStyle = newStyle;
    }

    @Override
    public void execute() {
        if (target instanceof BorderableLineShape) {
            target.setLineColor(newColor);
            target.setStrokeWidth((float)newWidth);
            target.setBorderStyle(newStyle);
        } else { // RectangleShape 或 EllipseShape
            target.setBorderColor(newColor);
            target.setBorderWidth(newWidth);
            target.setBorderStyle(newStyle);
        }
    }

    @Override
    public void undo() {
        if (target instanceof BorderableLineShape) {
            target.setLineColor(oldColor);
            target.setStrokeWidth((float)oldWidth);
            target.setBorderStyle(oldStyle);
        } else { // RectangleShape 或 EllipseShape
            target.setBorderColor(oldColor);
            target.setBorderWidth(oldWidth);
            target.setBorderStyle(oldStyle);
        }
    }

    // --- 适配器类 ---
    private static class BorderableRectangleShape implements Borderable {
        private RectangleShape rect;
        BorderableRectangleShape(RectangleShape rect) { this.rect = rect; }
        public void setBorderColor(Color c) { rect.setBorderColor(c); }
        public Color getBorderColor() { return rect.getBorderColor(); }
        public void setBorderWidth(double w) { rect.setBorderWidth(w); }
        public double getBorderWidth() { return rect.getBorderWidth(); }
        public void setBorderStyle(int s) { rect.setBorderStyle(s); }
        public int getBorderStyle() { return rect.getBorderStyle(); }
        
        // 直线特有的方法，对于矩形，这些方法是空的
        public void setLineColor(Color c) { }
        public Color getLineColor() { return null; }
        public void setStrokeWidth(float w) { }
        public float getStrokeWidth() { return 0.0f; }
    }
    
    private static class BorderableEllipseShape implements Borderable {
        private EllipseShape ellipse;
        BorderableEllipseShape(EllipseShape ellipse) { this.ellipse = ellipse; }
        public void setBorderColor(Color c) { ellipse.setBorderColor(c); }
        public Color getBorderColor() { return ellipse.getBorderColor(); }
        public void setBorderWidth(double w) { ellipse.setBorderWidth(w); }
        public double getBorderWidth() { return ellipse.getBorderWidth(); }
        public void setBorderStyle(int s) { ellipse.setBorderStyle(s); }
        public int getBorderStyle() { return ellipse.getBorderStyle(); }

        // 直线特有的方法，对于椭圆，这些方法是空的
        public void setLineColor(Color c) { }
        public Color getLineColor() { return null; }
        public void setStrokeWidth(float w) { }
        public float getStrokeWidth() { return 0.0f; }
    }
    
    private static class BorderableLineShape implements Borderable {
        private LineShape line;
        BorderableLineShape(LineShape line) { this.line = line; }
        
        // 矩形/椭圆的边框方法，对于直线，它们映射到线条属性
        public void setBorderColor(Color c) { line.setLineColor(c); } // 映射到 LineColor
        public Color getBorderColor() { return line.getLineColor(); }
        public void setBorderWidth(double w) { line.setStrokeWidth((float)w); } // 映射到 StrokeWidth
        public double getBorderWidth() { return line.getStrokeWidth(); }
        public void setBorderStyle(int s) { line.setBorderStyle(s); }
        public int getBorderStyle() { return line.getBorderStyle(); }
        
        // 直线特有的方法
        public void setLineColor(Color c) { line.setLineColor(c); }
        public Color getLineColor() { return line.getLineColor(); }
        public void setStrokeWidth(float w) { line.setStrokeWidth(w); }
        public float getStrokeWidth() { return line.getStrokeWidth(); }
    }
}