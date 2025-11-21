package com.myppt.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 表示单个幻灯片页面。
 * 它包含了页面上所有的元素对象。
 */
public class Slide implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public static final int PAGE_WIDTH = 1280; // 页面宽度
    public static final int PAGE_HEIGHT = 720; // 页面高度
    
    // 这就是我们的核心：一个可以容纳页面上所有元素的“万能”列表
    private List<AbstractSlideObject> slideObjects;

    public Slide() {
        this.slideObjects = new ArrayList<>();
    }

    public List<AbstractSlideObject> getSlideObjects() {
        return slideObjects;
    }

    public void addObject(AbstractSlideObject object) {
        this.slideObjects.add(object);
    }
    
    public void removeObject(AbstractSlideObject object) {
        this.slideObjects.remove(object);
    }

    public void bringToFront(AbstractSlideObject object) {
        if (slideObjects.remove(object)) {
            slideObjects.add(object); // 移动到列表末尾
        }
    }

    public void sendToBack(AbstractSlideObject object) {
        if (slideObjects.remove(object)) {
            slideObjects.add(0, object); // 移动到列表开头
        }
    }
    
    public void bringForward(AbstractSlideObject object) {
        int currentIndex = slideObjects.indexOf(object);
        if (currentIndex < slideObjects.size() - 1) { // 确保不是最顶层
            // 先移除，再插入到下一个位置
            slideObjects.remove(currentIndex);
            slideObjects.add(currentIndex + 1, object);
        }
    }

    public void sendBackward(AbstractSlideObject object) {
        int currentIndex = slideObjects.indexOf(object);
        if (currentIndex > 0) { // 确保不是最底层
            // 先移除，再插入到前一个位置
            slideObjects.remove(currentIndex);
            slideObjects.add(currentIndex - 1, object);
        }
    }

    // 设置对象列表的方法
    public void setSlideObjects(java.util.List<AbstractSlideObject> objects) {
        // 创建一个副本以保证封装性
        this.slideObjects = new java.util.ArrayList<>(objects);
    }
    
}