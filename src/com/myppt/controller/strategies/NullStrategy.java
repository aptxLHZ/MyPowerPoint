package com.myppt.controller.strategies;

import java.awt.event.MouseEvent;

/**
 * 空策略实现。
 * 当没有特定模式被激活时使用，它什么也不做。
 */
public class NullStrategy implements InteractionStrategy {
    @Override
    public void mousePressed(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Do nothing      
    }
}