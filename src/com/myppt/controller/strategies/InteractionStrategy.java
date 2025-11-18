package com.myppt.controller.strategies;

import java.awt.event.MouseEvent;

/**
 * 交互策略接口。
 * 定义了所有画布交互模式（如选择、绘制）都需要响应的鼠标事件。
 */
public interface InteractionStrategy {
    void mousePressed(MouseEvent e);
    void mouseDragged(MouseEvent e);
    void mouseReleased(MouseEvent e);
    /**
     * 处理鼠标在画布上移动的事件（没有按键按下的移动）。
     * 主要用于更新鼠标光标样式。
     * @param e 鼠标事件对象
     */
    void mouseMoved(MouseEvent e); 
}