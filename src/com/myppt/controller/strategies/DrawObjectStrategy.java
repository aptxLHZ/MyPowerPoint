package com.myppt.controller.strategies;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.myppt.controller.AppController;
import com.myppt.model.*;
import com.myppt.view.MainFrame;

public class DrawObjectStrategy implements InteractionStrategy {
    private AppController appController;
    private MainFrame mainFrame;
    // private Presentation presentation;
    private String objectType; // "RECT", "ELLIPSE", "TEXT"

    public DrawObjectStrategy(AppController appController, String objectType) {
        this.appController = appController;
        this.mainFrame = appController.getMainFrame();
        // this.presentation = appController.getPresentation();
        this.objectType = objectType;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point worldPoint = appController.convertScreenToWorld(e.getPoint());

        if (!appController.isPointInPage(worldPoint)) {
            appController.setMode("SELECT");
            return;
        }

        switch (objectType) {
            case "RECT":
                appController.markAsDirty();
                appController.getPresentation().getCurrentSlide().addObject(new RectangleShape(worldPoint.x, worldPoint.y, 100, 60, Color.BLUE));
                break;
            case "ELLIPSE":
                appController.markAsDirty();
                appController.getPresentation().getCurrentSlide().addObject(new EllipseShape(worldPoint.x, worldPoint.y, 80, 80, Color.RED));
                break;
            case "TEXT":
                handleTextDraw(worldPoint);
                break;
        }

        mainFrame.getCanvasPanel().repaint();
        if (!objectType.equals("TEXT")) { // Text handling has its own mode switch
             appController.setMode("SELECT");
        }
        appController.repaintThumbnails();
    }

    private void handleTextDraw(Point worldPoint) {
        JTextArea textArea = new JTextArea(5, 20);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        int result = JOptionPane.showConfirmDialog(mainFrame, scrollPane, "请输入文字", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String text = textArea.getText();
            if (text != null && !text.trim().isEmpty()) {
                Font font = new Font("宋体", Font.PLAIN, 24);
                TextBox textBox = new TextBox(worldPoint.x, worldPoint.y, text, font, Color.BLACK);
                appController.markAsDirty();
                appController.getPresentation().getCurrentSlide().addObject(textBox);
            }
        }
        appController.setMode("SELECT");
        appController.repaintThumbnails();
    }

    @Override
    public void mouseDragged(MouseEvent e) { } // 单击创建，拖动无操作
    @Override
    public void mouseReleased(MouseEvent e) { } // 单击创建，释放无操作

    @Override
    public void mouseMoved(MouseEvent e) {
        // 在绘制模式下，光标通常是十字形或默认，这里我们先不做改变
    }
}