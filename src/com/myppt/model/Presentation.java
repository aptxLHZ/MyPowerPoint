package com.myppt.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 表示整个幻灯片文件。
 * 它是所有数据的顶层容器。
 * 实现 Serializable 接口是为了将来可以轻松地保存和加载。
 */
public class Presentation implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<Slide> slides;
    private int currentSlideIndex; // [!] 新增: 当前选中的页面索引

    public Presentation() {
        this.slides = new ArrayList<>();
        this.slides.add(new Slide()); // 启动时默认创建一个页面
        this.currentSlideIndex = 0;   // 默认选中第一个页面
    }

    public List<Slide> getSlides() {
        return slides;
    }

    // [!] 新增: 获取当前页面的便捷方法
    public Slide getCurrentSlide() {
        if (slides.isEmpty()) {
            addNewSlide(); // 保证至少有一页
        }
        return slides.get(currentSlideIndex);
    }
    
    // [!] 新增: 添加新页面
    public void addNewSlide() {
        slides.add(new Slide());
        currentSlideIndex = slides.size() - 1; // 添加后自动切换到新页面
    }
    
    // [!] 新增: 设置和获取当前页面索引
    public int getCurrentSlideIndex() {
        return currentSlideIndex;
    }
    
    public void setCurrentSlideIndex(int index) {
        if (index >= 0 && index < slides.size()) {
            this.currentSlideIndex = index;
        }
    }

    // [!] 新增: 删除当前页面的方法
    public void removeCurrentSlide() {
        if (slides.size() > 1) { // 保证至少留有一页
            slides.remove(currentSlideIndex);
            // 删除后，将当前页索引调整为前一页，或0
            if (currentSlideIndex >= slides.size()) {
                currentSlideIndex = slides.size() - 1;
            }
        } else {
            // 如果只剩一页，则不删除，而是清空这一页的内容
            slides.set(0, new Slide());
        }
    }
}