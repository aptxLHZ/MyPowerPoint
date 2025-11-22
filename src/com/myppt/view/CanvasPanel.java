package com.myppt.view;

import javax.swing.JPanel;
import com.myppt.model.Presentation;
import com.myppt.model.Slide;
import com.myppt.model.AbstractSlideObject;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class CanvasPanel extends JPanel {
    private Presentation presentation;
    
    private double scale = 1.0;

    // [!] 核心修改: 定义一个巨大的虚拟画布尺寸
    public static final int VIRTUAL_CANVAS_WIDTH = 10000;
    public static final int VIRTUAL_CANVAS_HEIGHT = 10000;

    public CanvasPanel(Presentation presentation) {
        this.presentation = presentation;
        // this.currentSlideIndex = 0;
        setBackground(Color.LIGHT_GRAY);
    }

    // [!] 新增: 设置一个新的数据模型
    public void setPresentation(Presentation presentation) {
        this.presentation = presentation;
    }

    public void setScale(double scale) {
        this.scale = scale;
        // [!] 修改点: 当缩放时，需要 revalidate 以更新滚动条
        this.revalidate();
        this.repaint();
    }

    // [!] 核心修改: 恢复 getPreferredSize，并使用巨大的虚拟尺寸乘以缩放比例
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(
            (int)(VIRTUAL_CANVAS_WIDTH * scale), 
            (int)(VIRTUAL_CANVAS_HEIGHT * scale)
        );
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        java.awt.geom.AffineTransform oldTransform = g2d.getTransform();

        // --- [!] 核心修改: 计算页面在巨大画布中心的位置 ---
        // 1. 计算页面左上角在虚拟画布中的坐标
        int pageX = (VIRTUAL_CANVAS_WIDTH - Slide.PAGE_WIDTH) / 2;
        int pageY = (VIRTUAL_CANVAS_HEIGHT - Slide.PAGE_HEIGHT) / 2;

        // 2. 将整个坐标系先应用缩放
        g2d.scale(scale, scale);
        
        // 3. 再平移到页面的绘制起点
        g2d.translate(pageX, pageY);

        // --- 在变换后的坐标系中进行绘制 (后续代码不变) ---

        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(Slide.PAGE_WIDTH + 2, 5, 5, Slide.PAGE_HEIGHT);
        g2d.fillRect(5, Slide.PAGE_HEIGHT + 2, Slide.PAGE_WIDTH - 3, 5);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, Slide.PAGE_WIDTH, Slide.PAGE_HEIGHT);
        
        g2d.setColor(Color.BLACK);
        g2d.drawRect(0, 0, Slide.PAGE_WIDTH, Slide.PAGE_HEIGHT);

        Slide currentSlide = presentation.getCurrentSlide(); // [!] 关键: 直接从模型获取当前页
        for (AbstractSlideObject object : currentSlide.getSlideObjects()) {
            object.draw(g2d);
        }
        
        g2d.setTransform(oldTransform);
    }

    public void setCursor(int crosshairCursor) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCursor'");
    }
}