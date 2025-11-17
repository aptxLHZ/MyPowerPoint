// src/com/myppt/App.java
package com.myppt;

import javax.swing.SwingUtilities;
import com.myppt.controller.AppController;
import com.myppt.model.Presentation;
import com.myppt.view.MainFrame;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 1. 创建核心数据模型
            Presentation presentation = new Presentation();
            
            // 2. 创建主窗口
            MainFrame mainFrame = new MainFrame();

            // 3. 创建控制器，将模型和视图连接起来
            new AppController(presentation, mainFrame);

            // 4. 显示窗口
            mainFrame.setVisible(true);
        });
    }
}