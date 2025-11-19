package com.myppt.view;

import javax.swing.*;
import com.myppt.model.Presentation;
import java.awt.*;

public class MainFrame extends JFrame {

    private CanvasPanel canvasPanel;
    private JScrollPane canvasScrollPane;
    // private JPanel thumbnailPanel;
    private JPanel propertiesPanel;
    private JSplitPane mainSplitPane;
    private JSplitPane leftSplitPane;

    private ThumbnailPanel thumbnailPanel;
    private JScrollPane thumbnailScrollPane; 

    private JButton addRectButton;
    private JButton addEllipseButton; 
    private JButton addLineButton;    
    private JButton addTextButton;
    private JButton resetViewButton;
    private JButton changeColorButton;
    private JButton editTextButton;
    private JButton addImageButton;
    private JButton newSlideButton;
    private JButton deleteSlideButton;
    private JButton borderColorButton; 
    private JButton playButton;

    private JComboBox<String> fontNameBox;
    private JSpinner fontSizeSpinner;
    private JSpinner borderWidthSpinner; // [!] 新增
    private JCheckBox boldCheckBox;
    private JCheckBox italicCheckBox;

    private JMenuItem newMenuItem;
    private JMenuItem openMenuItem;
    private JMenuItem saveMenuItem;
    private JMenuItem saveAsMenuItem;

    private JComboBox<String> borderStyleBox;

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
    newMenuItem = new JMenuItem("新建");
    openMenuItem = new JMenuItem("打开...");
    saveMenuItem = new JMenuItem("保存");
    saveAsMenuItem = new JMenuItem("另存为...");

    fileMenu.add(newMenuItem);
    fileMenu.add(openMenuItem);
    fileMenu.add(saveMenuItem);
    fileMenu.add(saveAsMenuItem); 

    menuBar.add(fileMenu);
    setJMenuBar(menuBar);

    // --- 工具栏按钮 ---
    addRectButton = new JButton("添加矩形");
    addEllipseButton = new JButton("添加椭圆");
    addLineButton = new JButton("添加直线");
    addTextButton = new JButton("添加文本框");
    addImageButton = new JButton("插入图片");
    resetViewButton = new JButton("重置视图");
    newSlideButton = new JButton("新建页面");
    deleteSlideButton = new JButton("删除页面");
    playButton = new JButton("播放");
    

    toolBar.add(newSlideButton);
    toolBar.add(deleteSlideButton); 
    toolBar.add(addRectButton);
    toolBar.add(addEllipseButton);
    toolBar.add(addLineButton);
    toolBar.add(addTextButton); 
    toolBar.add(addImageButton);
    toolBar.add(resetViewButton);
    add(toolBar, BorderLayout.NORTH);

    toolBar.add(Box.createHorizontalGlue()); // 一个弹簧，会把播放按钮推到最右边
    toolBar.add(playButton); 

    // --- [!] 修正后的三栏布局逻辑 ---

    // 1. 创建左侧缩略图面板及其滚动窗格
    thumbnailPanel = new ThumbnailPanel(); 
    thumbnailScrollPane = new JScrollPane(thumbnailPanel); 
    thumbnailScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 禁止水平滚动
    Dimension thumbPaneSize = new Dimension(SlideThumbnail.getThumbWidth() + 30, 0);
    thumbnailScrollPane.setPreferredSize(thumbPaneSize);
    thumbnailScrollPane.setMinimumSize(thumbPaneSize); // 强制最小宽度

    // 2. 创建右侧属性面板
    propertiesPanel = new JPanel();
    propertiesPanel.setLayout(new BoxLayout(propertiesPanel, BoxLayout.Y_AXIS));
    propertiesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // --- 按钮组 ---
    changeColorButton = new JButton("更改颜色");
    editTextButton = new JButton("修改文本");
    // 设置对齐方式和最大尺寸
    changeColorButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    editTextButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    Dimension buttonSize = new Dimension(150, 30); // 统一按钮大小
    changeColorButton.setMaximumSize(buttonSize);
    editTextButton.setMaximumSize(buttonSize);

    borderColorButton = new JButton("边框颜色"); // [!] 新增
    borderColorButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    borderColorButton.setMaximumSize(buttonSize);

    propertiesPanel.add(changeColorButton);
    propertiesPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    propertiesPanel.add(borderColorButton); // [!] 新增
    propertiesPanel.add(Box.createRigidArea(new Dimension(0, 5)));  
    propertiesPanel.add(editTextButton);

    // --- 分隔符 ---
    propertiesPanel.add(Box.createRigidArea(new Dimension(0, 15)));
    JSeparator separator = new JSeparator();
    separator.setAlignmentX(Component.LEFT_ALIGNMENT);
    separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
    propertiesPanel.add(separator);
    propertiesPanel.add(Box.createRigidArea(new Dimension(0, 10)));

    // --- 文字样式组 ---
    JLabel styleLabel = new JLabel("文字样式:");
    styleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    propertiesPanel.add(styleLabel);

    // 字体选择
    String[] commonFonts = {"宋体", "黑体", "楷体", "微软雅黑", "华文琥珀", "华文隶书", "Arial", "Times New Roman"};
    fontNameBox = new JComboBox<>(commonFonts);
    fontNameBox.setAlignmentX(Component.LEFT_ALIGNMENT);
    fontNameBox.setMaximumSize(new Dimension(150, 30)); // 限制大小
    propertiesPanel.add(fontNameBox);

    // 字号选择
    JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    sizePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    // [!] 核心修复: 限制 sizePanel 本身的最大高度
    sizePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35)); // 高度略大于组件即可
    sizePanel.add(new JLabel("字号: "));
    fontSizeSpinner = new JSpinner(new SpinnerNumberModel(24, 1, 400, 1)); // 最小1, 最大200
    fontSizeSpinner.setMaximumSize(new Dimension(70, 30)); // 限制大小
    sizePanel.add(fontSizeSpinner);
    propertiesPanel.add(sizePanel);

    // 粗体/斜体
    JPanel stylePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    stylePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    // [!] 核心修复: 限制 stylePanel 本身的最大高度
    stylePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
    boldCheckBox = new JCheckBox("粗体");
    italicCheckBox = new JCheckBox("斜体");
    stylePanel.add(boldCheckBox);
    stylePanel.add(italicCheckBox);
    propertiesPanel.add(stylePanel);

    // [!] 新增: 边框样式组
    propertiesPanel.add(Box.createRigidArea(new Dimension(0, 10)));
    JLabel borderLabel = new JLabel("边框样式:");
    borderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    propertiesPanel.add(borderLabel);

    JPanel borderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    borderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    borderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
    borderPanel.add(new JLabel("粗细: "));
    borderWidthSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 20.0, 0.5)); // 最小0, 最大20, 步长0.5
    borderPanel.add(borderWidthSpinner);
    propertiesPanel.add(borderPanel);

    // [!] 新增: 边框线型选择
    JPanel borderStylePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    borderStylePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    borderStylePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
    borderStylePanel.add(new JLabel("线型: "));
    String[] styleNames = {"实线", "虚线", "点线", "无边框"};
    borderStyleBox = new JComboBox<>(styleNames);
    borderStylePanel.add(borderStyleBox);
    propertiesPanel.add(borderStylePanel);

    // 默认禁用所有样式控件
    changeColorButton.setEnabled(false);
    editTextButton.setEnabled(false);
    fontNameBox.setEnabled(false);
    fontSizeSpinner.setEnabled(false);
    boldCheckBox.setEnabled(false);
    italicCheckBox.setEnabled(false);
    borderColorButton.setEnabled(false);
    borderWidthSpinner.setEnabled(false);

    // [!] 添加一个“弹簧”，将所有组件推到顶部
    propertiesPanel.add(Box.createVerticalGlue());
    
    
    // 3. 创建左侧分割面板，使用【带滚动条】的 thumbnailScrollPane
    leftSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, thumbnailScrollPane, null);
    leftSplitPane.setDividerLocation(SlideThumbnail.getThumbWidth() + 30);
    

    // 4. 创建主分割面板
    mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPane, propertiesPanel);
    add(mainSplitPane, BorderLayout.CENTER);

    // 5. 添加窗口大小调整监听器
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

    public JMenuItem getOpenMenuItem() { return openMenuItem; }
    public JMenuItem getSaveMenuItem() { return saveMenuItem; }
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
    public ThumbnailPanel getThumbnailPanel() { return thumbnailPanel; }
    public JButton getNewSlideButton() { return this.newSlideButton; }
    public JButton getDeleteSlideButton() { return this.deleteSlideButton; }
    public JComboBox<String> getFontNameBox() { return fontNameBox; }
    public JSpinner getFontSizeSpinner() { return fontSizeSpinner; }
    public JCheckBox getBoldCheckBox() { return boldCheckBox; }
    public JCheckBox getItalicCheckBox() { return italicCheckBox; }
    public JMenuItem getNewMenuItem() { return newMenuItem; }
    public JMenuItem getSaveAsMenuItem() { return saveAsMenuItem; }
    public JButton getBorderColorButton() { return borderColorButton; }
    public JSpinner getBorderWidthSpinner() { return borderWidthSpinner; }
    public JComboBox<String> getBorderStyleBox() { return borderStyleBox; }
    public JButton getPlayButton() { return playButton; }
}