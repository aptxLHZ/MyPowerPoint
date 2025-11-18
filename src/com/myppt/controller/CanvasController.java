package com.myppt.controller;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
// import com.myppt.controller.strategies.InteractionStrategy;

/**
 * 专门负责处理画布(CanvasPanel)上的所有鼠标交互。
 * 将具体的交互逻辑委托给当前的交互策略(Strategy)。
 */
public class CanvasController extends MouseAdapter {
    private AppController appController;

    public CanvasController(AppController appController) {
        this.appController = appController;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        appController.getCurrentStrategy().mousePressed(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        appController.getCurrentStrategy().mouseDragged(e);
    }
            
    @Override
    public void mouseReleased(MouseEvent e) {
        appController.getCurrentStrategy().mouseReleased(e);
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        appController.getCurrentStrategy().mouseMoved(e);
    }

}