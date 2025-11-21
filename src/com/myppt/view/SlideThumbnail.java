package com.myppt.view;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.myppt.model.Slide;

/**
 * 一个用于在左侧面板中显示单个幻灯片缩略图的组件。
 */
public class SlideThumbnail extends JPanel {
    private Slide slide;
    private int pageNumber;

    private static final int THUMB_WIDTH = 180;
    private static final int THUMB_HEIGHT = 101; // 180 * 9/16

    public static int getThumbWidth() {
        return THUMB_WIDTH;
    }

    public SlideThumbnail(Slide slide, int pageNumber) {
        this.slide = slide;
        this.pageNumber = pageNumber;
        
        Dimension size = new Dimension(THUMB_WIDTH, THUMB_HEIGHT + 20); // 加上下方页码的空间
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // 开启抗锯齿，让缩略图更平滑
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);

        // 绘制白色背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, THUMB_WIDTH, THUMB_HEIGHT);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(0, 0, THUMB_WIDTH - 1, THUMB_HEIGHT - 1);
        
        // --- 绘制缩略图内容 ---
        double scaleX = (double) THUMB_WIDTH / Slide.PAGE_WIDTH;
        double scaleY = (double) THUMB_HEIGHT / Slide.PAGE_HEIGHT;
        g2d.scale(scaleX, scaleY);
        
        // 设置裁剪区域，防止内容画出缩略图边界
        g2d.setClip(0, 0, Slide.PAGE_WIDTH, Slide.PAGE_HEIGHT);

        for (com.myppt.model.AbstractSlideObject object : slide.getSlideObjects()) {
            object.draw(g2d);
        }

        g2d.dispose();

        // 绘制页码
        g.setColor(Color.BLACK);
        g.drawString("第 " + this.pageNumber + " 页", 10, THUMB_HEIGHT + 15);
    }
    
    public void setSelected(boolean selected) {
        if (selected) {
            setBorder(BorderFactory.createLineBorder(Color.BLUE, 3));
        } else {
            setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }
    }
}