package com.myppt.commands;

import java.awt.Color;
import com.myppt.model.EllipseShape;
import com.myppt.model.RectangleShape;

public class ChangeBorderCommand implements Command {
    // 这是一个接口，用于统一 RectangleShape 和 EllipseShape 的行为
    interface Borderable {
        void setBorderColor(Color color);
        Color getBorderColor();
        void setBorderWidth(double width);
        double getBorderWidth();
        void setBorderStyle(int style);
        int getBorderStyle();
    }

    private Borderable target;
    
    private Color oldColor, newColor;
    private double oldWidth, newWidth;
    private int oldStyle, newStyle;

    public ChangeBorderCommand(Object targetObject, Color newColor, double newWidth, int newStyle) {
        if (targetObject instanceof RectangleShape) {
            this.target = new BorderableRectangleShape((RectangleShape) targetObject);
        } else if (targetObject instanceof EllipseShape) {
            this.target = new BorderableEllipseShape((EllipseShape) targetObject);
        } else {
            throw new IllegalArgumentException("Target object does not support borders.");
        }
        
        // 记录旧状态
        this.oldColor = target.getBorderColor();
        this.oldWidth = target.getBorderWidth();
        this.oldStyle = target.getBorderStyle();
        
        // 记录新状态
        this.newColor = newColor;
        this.newWidth = newWidth;
        this.newStyle = newStyle;
    }

    @Override
    public void execute() {
        target.setBorderColor(newColor);
        target.setBorderWidth(newWidth);
        target.setBorderStyle(newStyle);
    }

    @Override
    public void undo() {
        target.setBorderColor(oldColor);
        target.setBorderWidth(oldWidth);
        target.setBorderStyle(oldStyle);
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
    }
}