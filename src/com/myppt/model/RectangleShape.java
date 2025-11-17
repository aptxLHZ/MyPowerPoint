package com.myppt.model;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * 表示一个矩形。这是第一个具体的幻灯片元素。
 */
public class RectangleShape extends AbstractSlideObject {
    private int width;
    private int height;
    private Color fillColor;

    public RectangleShape(int x, int y, int width, int height, Color fillColor) {
        super(x, y); // 调用父类的构造方法，设置x, y坐标
        this.width = width;
        this.height = height;
        this.fillColor = fillColor;
    }

    // 实现父类中定义的抽象方法
    @Override
    public void draw(Graphics g) {
        // 保存当前的颜色设置
        Color originalColor = g.getColor();

        // 设置我们矩形的颜色
        g.setColor(this.fillColor);
        // 绘制一个填充的矩形
        g.fillRect(this.x, this.y, this.width, this.height);
        
        // （可选）再用黑色给它画个边框
        g.setColor(Color.BLACK);
        g.drawRect(this.x, this.y, this.width, this.height);

        // 恢复画笔原来的颜色，这是一个好习惯
        g.setColor(originalColor);
    }
}