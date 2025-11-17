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
    // 使用List来存储所有的幻灯片页面
    private List<Slide> slides;

    public Presentation() {
        this.slides = new ArrayList<>();
        // 一个新的幻灯片至少要有一个空白页面
        this.slides.add(new Slide());
    }

    public List<Slide> getSlides() {
        return slides;
    }
}