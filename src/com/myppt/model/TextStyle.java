package com.myppt.model;

import java.awt.Color;
import java.awt.Font;

public class TextStyle implements Style {
    private Font font;
    private Color color;

    public TextStyle(Font font, Color color) {
        this.font = font;
        this.color = color;
    }

    public Font getFont() {
        return font;
    }

    public Color getColor() {
        return color;
    }
}