package com.myppt.view;

import javax.swing.*;
// import javax.swing.border.Border;
import java.awt.*;

import com.myppt.model.Slide;

/**
 * 一个用于在左侧面板中显示单个幻灯片缩略图的组件。
 */
public class SlideThumbnail extends JPanel {
    private Slide slide;
    private int pageNumber;
    private static final int THUMB_WIDTH = 180;
    private static final int THUMB_HEIGHT = 101; // 180 * 9/16

    public static int getThumbWidth(){return THUMB_WIDTH;}

    public SlideThumbnail(Slide slide, int pageNumber) {
        this.slide = slide;
        this.pageNumber = pageNumber;
        Dimension size = new Dimension(THUMB_WIDTH, THUMB_HEIGHT + 20);
        setPreferredSize(size);
        setMinimumSize(size);   // [!] 新增
        setMaximumSize(size);   // [!] 新增
        setBorder(BorderFactory.createLineBorder(Color.GRAY)); 
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // 绘制白色背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, THUMB_WIDTH, THUMB_HEIGHT);
        
        // --- 绘制缩略图内容 ---
        // 计算缩放比例，将1280x720的页面内容缩放到160x90的区域
        double scaleX = (double) THUMB_WIDTH / Slide.PAGE_WIDTH;
        double scaleY = (double) THUMB_HEIGHT / Slide.PAGE_HEIGHT;
        g2d.scale(scaleX, scaleY);
        
        // 遍历并绘制slide中的所有对象
        for (com.myppt.model.AbstractSlideObject object : slide.getSlideObjects()) {
            object.draw(g2d);
        }

        g2d.dispose();

        // 绘制页码（暂不实现，仅作占位）
        g.setColor(Color.BLACK);
        g.drawString("第 " + this.pageNumber + " 页", 10, THUMB_HEIGHT + 15);
    }
    
    // [!] 新增: 设置选中状态的边框
    public void setSelected(boolean selected) {
        if (selected) {
            setBorder(BorderFactory.createLineBorder(Color.BLUE, 3));
        } else {
            setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }
    }
}