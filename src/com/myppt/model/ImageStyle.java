package com.myppt.model;


public class ImageStyle implements Style {
    private float opacity;

    public ImageStyle(float opacity) {
        this.opacity = opacity;
    }

    public float getOpacity() {
        return opacity;
    }
}