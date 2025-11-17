// src/com/myppt/view/MainFrame.java
package com.myppt.view;

import javax.swing.*;
import com.myppt.model.Presentation;
import java.awt.*;

/**
 * 程序的主窗口。
 */
public class MainFrame extends JFrame {
    private CanvasPanel canvasPanel;
    private JButton addRectButton;

    public MainFrame() {
        setTitle("My PowerPoint");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JMenuBar menuBar = new JMenuBar();
        JToolBar toolBar = new JToolBar();

        setLayout(new BorderLayout());

        JMenu fileMenu = new JMenu("文件");
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        
        addRectButton = new JButton("添加矩形");
        toolBar.add(addRectButton);
        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * 由控制器调用，用来初始化画布。
     * @param presentation 数据模型
     */
    public void setupCanvas(Presentation presentation) {
        this.canvasPanel = new CanvasPanel(presentation);
        add(this.canvasPanel, BorderLayout.CENTER);
    }

    // 提供给控制器的公共访问方法
    public CanvasPanel getCanvasPanel() {
        return canvasPanel;
    }

    public JButton getAddRectButton() {
        return addRectButton;
    }
}