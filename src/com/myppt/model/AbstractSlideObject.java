package com.myppt.model;

import java.awt.Graphics;
import java.io.Serializable;
import java.awt.Point; // [!] 新增: 引入Point类来表示坐标点


/**
 * 这是一个抽象类，是所有幻灯片元素（文本、图形、图片等）的“共同祖先”。
 * 它定义了所有元素都必须具备的公共属性和行为。
 */
public abstract class AbstractSlideObject implements Serializable {
    // 所有元素都有x, y坐标
    protected int x;
    protected int y;

    // [!] 新增: 选中状态标志
    protected boolean selected = false;

    public AbstractSlideObject(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // 这是一个抽象方法。
    // 它只声明“所有子类都必须会画自己”，但具体怎么画，由子类自己去实现。
    public abstract void draw(Graphics g);

    // [!] 新增: 抽象的碰撞检测方法
    public abstract boolean contains(Point p);

    // Getters and Setters for position
    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
    
    // [!] 新增: isSelected 和 setSelected 方法
    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}