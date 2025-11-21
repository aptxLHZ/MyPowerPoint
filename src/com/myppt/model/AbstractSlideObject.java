package com.myppt.model;

import java.awt.Graphics;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

import com.myppt.controller.strategies.ResizeHandle;

import java.awt.Point; // [!] 新增: 引入Point类来表示坐标点
import java.awt.Rectangle;

/**
 * 这是一个抽象类，是所有幻灯片元素（文本、图形、图片等）的“共同祖先”。
 * 它定义了所有元素都必须具备的公共属性和行为。
 */
public abstract class AbstractSlideObject implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // 所有元素都有x, y坐标
    public int x;
    public int y;

    public static final int HANDLE_SIZE = 8; // 控制点小方块的大小

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

    /**
     * 根据对象的边界计算并返回8个缩放控制点的位置。
     * 默认实现适用于所有矩形包围盒的对象。
     * @return 一个包含8个控制点类型及其位置(Rectangle)的Map。
     */
    public Map<ResizeHandle, Rectangle> getResizeHandles() {
        // 子类需要提供自己的 getBounds() 实现
        Rectangle bounds = getBounds();
        int x = bounds.x;
        int y = bounds.y;
        int width = bounds.width;
        int height = bounds.height;
        
        EnumMap<ResizeHandle, Rectangle> handles = new EnumMap<>(ResizeHandle.class);
        
        handles.put(ResizeHandle.TOP_LEFT, new Rectangle(x - HANDLE_SIZE / 2, y - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE));
        handles.put(ResizeHandle.TOP_CENTER, new Rectangle(x + width / 2 - HANDLE_SIZE / 2, y - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE));
        handles.put(ResizeHandle.TOP_RIGHT, new Rectangle(x + width - HANDLE_SIZE / 2, y - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE));
        handles.put(ResizeHandle.MIDDLE_LEFT, new Rectangle(x - HANDLE_SIZE / 2, y + height / 2 - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE));
        handles.put(ResizeHandle.MIDDLE_RIGHT, new Rectangle(x + width - HANDLE_SIZE / 2, y + height / 2 - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE));
        handles.put(ResizeHandle.BOTTOM_LEFT, new Rectangle(x - HANDLE_SIZE / 2, y + height - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE));
        handles.put(ResizeHandle.BOTTOM_CENTER, new Rectangle(x + width / 2 - HANDLE_SIZE / 2, y + height - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE));
        handles.put(ResizeHandle.BOTTOM_RIGHT, new Rectangle(x + width - HANDLE_SIZE / 2, y + height - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE));
        
        return handles;
    }

    /**
     * 返回对象的矩形包围盒。子类必须实现此方法。
     * @return 一个表示对象边界的Rectangle。
     */
    public abstract Rectangle getBounds();

    public abstract void setBounds(Rectangle bounds);

}