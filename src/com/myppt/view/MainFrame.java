package com.myppt.view;

import javax.swing.*;
import com.myppt.model.Presentation;
import java.awt.*;

public class MainFrame extends JFrame {

    private CanvasPanel canvasPanel;
    private JScrollPane canvasScrollPane;
    private JPanel thumbnailPanel;
    private JPanel propertiesPanel;
    private JSplitPane mainSplitPane;
    private JSplitPane leftSplitPane;

    private JButton addRectButton;
    private JButton addEllipseButton; // 声明
    private JButton addLineButton;    
    private JButton addTextButton;
    private JButton resetViewButton;
    private JButton changeColorButton;
    private JButton editTextButton;
    private JButton addImageButton;

    public MainFrame() {
        setTitle("My PowerPoint");
        setSize(1280, 720);
        setMinimumSize(new Dimension(800, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JMenuBar menuBar = new JMenuBar();
        JToolBar toolBar = new JToolBar();
        setLayout(new BorderLayout());

        JMenu fileMenu = new JMenu("文件");
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // --- 工具栏按钮创建 ---
        addRectButton = new JButton("添加矩形");
        addEllipseButton = new JButton("添加椭圆");
        addLineButton = new JButton("添加直线");
        addTextButton = new JButton("添加文本框");
        addImageButton = new JButton("插入图片");
        resetViewButton = new JButton("重置视图");
        
        
        
        // --- 添加到工具栏 ---
        toolBar.add(addRectButton);
        toolBar.add(addEllipseButton); // 添加
        toolBar.add(addLineButton);
        toolBar.add(addTextButton); 
        toolBar.add(addImageButton);
        toolBar.add(resetViewButton);
        add(toolBar, BorderLayout.NORTH);

        // --- 三栏布局 ---
        thumbnailPanel = new JPanel();
        thumbnailPanel.setBackground(Color.GRAY);
        thumbnailPanel.add(new JLabel("缩略图区"));

        propertiesPanel = new JPanel();
        propertiesPanel.setLayout(new BoxLayout(propertiesPanel, BoxLayout.Y_AXIS));
        propertiesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        propertiesPanel.add(new JLabel("属性区"));
        
        changeColorButton = new JButton("更改填充颜色");
        changeColorButton.setEnabled(false);
        propertiesPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        propertiesPanel.add(changeColorButton);

        editTextButton = new JButton("修改文本");
        editTextButton.setEnabled(false); // 默认禁用
        propertiesPanel.add(Box.createRigidArea(new Dimension(0, 5))); // 加一点间距
        propertiesPanel.add(editTextButton);
        
        leftSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, thumbnailPanel, null);
        leftSplitPane.setDividerLocation(200);

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPane, propertiesPanel);
        add(mainSplitPane, BorderLayout.CENTER);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                mainSplitPane.setDividerLocation(mainSplitPane.getWidth() - 250);
            }
        });
    }

    public void setupCanvas(Presentation presentation) {
        this.canvasPanel = new CanvasPanel(presentation);
        this.canvasScrollPane = new JScrollPane(this.canvasPanel);
        this.canvasScrollPane.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
        this.leftSplitPane.setRightComponent(this.canvasScrollPane);
    }

    // --- Getters ---

    public CanvasPanel getCanvasPanel() { return canvasPanel; }
    public JScrollPane getCanvasScrollPane() { return canvasScrollPane; }
    public JButton getAddRectButton() { return addRectButton; }
    public JButton getAddEllipseButton() { return addEllipseButton; } // Getter
    public JButton getResetViewButton() { return resetViewButton; }
    public JButton getChangeColorButton() { return changeColorButton; }
    public JButton getAddLineButton() { return addLineButton; }
    public JButton getAddTextButton() { return addTextButton; }
    public JButton getEditTextButton() { return editTextButton; }
    public JButton getAddImageButton() { return addImageButton; }
}