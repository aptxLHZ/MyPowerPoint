package com.myppt.controller;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Cursor;

import java.util.List;

import javax.swing.SwingUtilities;

import com.myppt.commands.Command;
import com.myppt.model.AbstractSlideObject;
import com.myppt.model.Style;
import com.myppt.commands.ChangeStyleCommand;  
import com.myppt.model.Slide; 
import com.myppt.view.MainFrame;

/**
 * 专门负责处理画布(CanvasPanel)上的所有鼠标交互。
 * 将具体的交互逻辑委托给当前的交互策略(Strategy)。
 */
public class CanvasController extends MouseAdapter {
    private AppController appController;
    private MainFrame mainFrame;

    public CanvasController(AppController appController) {
        this.appController = appController;
        this.mainFrame = appController.getMainFrame();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // [!] 核心修改: 优先处理格式刷模式
        if (appController.getCurrentMode().equals("FORMAT_PAINTER")) {
            handleFormatPainterClick(e);
            return;
        }

        // --- 原有的策略模式分发逻辑 ---
        Point worldPoint = appController.convertScreenToWorld(e.getPoint());

        // if (!appController.isPointInPage(worldPoint)) {
        //     // 如果不在页面内，清空选择并刷新UI
        //     // 这里不再调用 deselectAllObjects()，因为那会触发 updatePropertiesPanel()
        //     // 且仅在 FORMAT_PAINTER 模式下才需要特殊处理
        //     if (!appController.getCurrentMode().equals("FORMAT_PAINTER")) {
        //         appController.setSelectedObject(null);
        //         appController.updatePropertiesPanel();
        //     }
        //     mainFrame.getCanvasPanel().repaint();
        //     appController.repaintThumbnails();
        //     return;
        // }

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

    /**
     * 处理格式刷模式下的鼠标点击事件。
     */
    private void handleFormatPainterClick(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            Style copiedStyle = appController.getCopiedStyle();
            if (copiedStyle == null) {
                exitFormatPainterMode(); // 如果没有复制样式，就退出
                return;
            }

            Point worldPoint = appController.convertScreenToWorld(e.getPoint());
            AbstractSlideObject targetObject = findObjectAtPoint(worldPoint); // 在CanvasController中定义此方法
            
            if (targetObject != null) {
                // [!] 核心: 创建一个 ChangeStyleCommand
                Command command = new ChangeStyleCommand(targetObject, copiedStyle);
                appController.getUndoManager().executeCommand(command);
                appController.markAsDirty();
                appController.updateUI();
            }
            
            // 无论是否点中，应用一次后就退出格式刷模式
            exitFormatPainterMode();
        }
    }

    /**
     * [!] 从 AppController 迁移过来的辅助方法，用于在画布上查找对象。
     *     CanvasController 需要这个方法来处理格式刷点击。
     */
    private AbstractSlideObject findObjectAtPoint(Point worldPoint) {
        Slide currentSlide = appController.getPresentation().getCurrentSlide();
        List<AbstractSlideObject> objects = currentSlide.getSlideObjects();
        for (int i = objects.size() - 1; i >= 0; i--) {
            AbstractSlideObject object = objects.get(i);
            if (object.contains(worldPoint)) {
                return object;
            }
        }
        return null;
    }


    /**
     * 退出格式刷模式，恢复默认状态。
     */
    private void exitFormatPainterMode() {
        appController.setCopiedStyle(null); // 清除复制的样式
        appController.setMode("SELECT");    // 恢复到选择模式
        mainFrame.getCanvasPanel().setCursor(Cursor.getDefaultCursor()); // 恢复默认光标
        // 不需要调用 updateUI，因为 setMode 内部会处理
    }
}