package com.myppt.model;

import java.awt.Color;

public class ShapeStyle implements Style {
    private Color fillColor;
    private Color borderColor;
    private double borderWidth;
    private int borderStyle;

    public ShapeStyle(Color fillColor, Color borderColor, double borderWidth, int borderStyle) {
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.borderWidth = borderWidth;
        this.borderStyle = borderStyle;
    }

    // Add getters for all properties
    public Color getFillColor() { return fillColor; }
    public Color getBorderColor() { return borderColor; }
    public double getBorderWidth() { return borderWidth; }
    public int getBorderStyle() { return borderStyle; }
}