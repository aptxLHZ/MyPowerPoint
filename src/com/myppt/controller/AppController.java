// src/com/myppt/controller/AppController.java
package com.myppt.controller;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.myppt.model.Presentation;
import com.myppt.model.RectangleShape;
import com.myppt.model.Slide;
import com.myppt.view.MainFrame;

/**
 * 应用程序的总控制器。
 * 负责连接数据模型(Presentation)和视图(MainFrame)，并处理用户输入。
 */
public class AppController {
    private Presentation presentation;
    private MainFrame mainFrame;
    private String currentMode = "NONE"; // 当前操作模式

    public AppController(Presentation presentation, MainFrame mainFrame) {
        this.presentation = presentation;
        this.mainFrame = mainFrame;

        // 将视图和数据关联起来
        this.mainFrame.setupCanvas(presentation);
        
        // 绑定事件监听器
        this.attachListeners();
    }

    private void attachListeners() {
        // 监听“添加矩形”按钮的点击事件
        mainFrame.getAddRectButton().addActionListener(e -> {
            System.out.println("进入'添加矩形'模式");
            currentMode = "DRAW_RECT";
        });

        // 监听画布的鼠标点击事件
        mainFrame.getCanvasPanel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentMode.equals("DRAW_RECT")) {
                    System.out.println("在画布上点击，坐标: " + e.getX() + ", " + e.getY());
                    
                    // 获取当前幻灯片
                    Slide currentSlide = presentation.getSlides().get(0); // 暂时只处理第一页
                    
                    // 创建一个新的矩形对象
                    RectangleShape rect = new RectangleShape(e.getX(), e.getY(), 100, 60, Color.BLUE);
                    
                    // 将新矩形添加到幻灯片中
                    currentSlide.addObject(rect);
                    
                    // 通知画布重绘
                    mainFrame.getCanvasPanel().repaint();
                    
                    // 退出绘制模式，恢复到普通状态
                    currentMode = "NONE"; 
                    System.out.println("矩形添加完毕，恢复普通模式");
                }
            }
        });
    }
}