package com.myppt.view;

import javax.swing.JPanel;
import com.myppt.model.Presentation;
import com.myppt.model.Slide;
import com.myppt.model.AbstractSlideObject;

import java.awt.Color;
import java.awt.Graphics;

/**
 * 这是我们的核心绘图区域。
 * 所有幻灯片元素都在这个面板上被绘制出来。
 */
public class CanvasPanel extends JPanel {
    private Presentation presentation;
    private int currentSlideIndex;

    public CanvasPanel(Presentation presentation) {
        this.presentation = presentation;
        this.currentSlideIndex = 0; // 默认显示第一页
        setBackground(Color.WHITE);
    }

    /**
     * 这是Swing中最重要的绘图方法。
     * 当我们调用 repaint() 时，Swing最终会调用这个方法来重绘组件。
     * 我们绝不能直接调用它，而是通过调用 repaint() 来触发它。
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // 必须先调用父类的方法，它会清空画布

        // 获取当前要显示的幻灯片页面
        Slide currentSlide = presentation.getSlides().get(currentSlideIndex);

        // 遍历当前页面上的所有元素
        for (AbstractSlideObject object : currentSlide.getSlideObjects()) {
            // 让每个元素自己画自己（多态的体现）
            object.draw(g);
        }
    }
}