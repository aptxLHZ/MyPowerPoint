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
}