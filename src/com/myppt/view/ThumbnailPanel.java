package com.myppt.view;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.BorderFactory; 

import java.awt.Color;

/**
 * 左侧的缩略图容器面板。
 * 使用BoxLayout垂直排列所有缩略图。
 */
public class ThumbnailPanel extends JPanel {
    private static final int PADDING = 5; // 定义一个常量用于边距和间距
    
    public ThumbnailPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.DARK_GRAY);
        //给整个面板添加一个内边距
        setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
    }
    

}