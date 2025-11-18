package com.myppt.controller;

import java.awt.Color;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.ActionEvent; // [!] 新增

import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.AbstractAction; // [!] 新增
import javax.swing.ActionMap;      // [!] 新增
import javax.swing.InputMap;       // [!] 新增
import javax.swing.JComponent;     // [!] 新增
import javax.swing.KeyStroke;      // [!] 新增


import com.myppt.controller.strategies.*; // 导入所有策略
import com.myppt.model.*;
import com.myppt.view.CanvasPanel;
import com.myppt.view.MainFrame;

/**
 * 应用程序的主控制器，负责协调各个部分。
 * 将画布的具体交互逻辑委托给 CanvasController 和相应的交互策略。
 */
public class AppController {
    private Presentation presentation;
    private MainFrame mainFrame;
    private String currentMode = "SELECT";
    private double scale = 1.0;
    private AbstractSlideObject selectedObject = null;
    
    private InteractionStrategy currentStrategy; // 核心: 当前的交互策略

    public AppController(Presentation presentation, MainFrame mainFrame) {
        this.presentation = presentation;
        this.mainFrame = mainFrame;
        this.mainFrame.setupCanvas(presentation);
        
        // AppController一启动，默认的策略就是“选择”策略
        this.currentStrategy = new SelectStrategy(this);
        
        this.attachListeners();
    }

    // --- Getters and Setters for shared state ---
    public MainFrame getMainFrame() { return mainFrame; }
    public Presentation getPresentation() { return presentation; }
    public String getCurrentMode() { return currentMode; }
    public AbstractSlideObject getSelectedObject() { return selectedObject; }
    public InteractionStrategy getCurrentStrategy() { return currentStrategy; }
    
    public void setSelectedObject(AbstractSlideObject object) {
        this.selectedObject = object;
    }

    public void setMode(String mode) {
        this.currentMode = mode;
        switch(mode) {
            case "SELECT":
                this.currentStrategy = new SelectStrategy(this);
                break;
            case "DRAW_RECT":
                this.currentStrategy = new DrawObjectStrategy(this, "RECT");
                break;
            case "DRAW_ELLIPSE":
                this.currentStrategy = new DrawObjectStrategy(this, "ELLIPSE");
                break;
            case "DRAW_TEXT":
                this.currentStrategy = new DrawObjectStrategy(this, "TEXT");
                break;
            case "DRAW_LINE":
                this.currentStrategy = new DrawLineStrategy(this);
                break;
            default:
                this.currentStrategy = new NullStrategy();
        }
        
        // 当进入任何绘制模式时，都应该取消当前的选择
        if (!mode.equals("SELECT")) {
            if (this.selectedObject != null) {
                this.selectedObject.setSelected(false);
            }
            setSelectedObject(null);
            updatePropertiesPanel();
            mainFrame.getCanvasPanel().repaint();
        }
    }

    // --- 核心监听器附加 ---
    private void attachListeners() {
        // 1. 创建并附加画布控制器，它会将事件转发给上面的 currentStrategy
        CanvasController canvasController = new CanvasController(this);
        mainFrame.getCanvasPanel().addMouseListener(canvasController);
        mainFrame.getCanvasPanel().addMouseMotionListener(canvasController);

        // 2. 附加非画布UI事件监听器
        attachComponentListeners();
        attachButtonListeners();
        attachMouseWheelListener();

        attachKeyBindings();

    }

    // --- 分离出来的监听器附加方法 ---

    private void attachComponentListeners() {
        mainFrame.addComponentListener(new ComponentAdapter() {
            private boolean isFirstTime = true;
            @Override
            public void componentShown(ComponentEvent e) {
                if (isFirstTime) {
                    fitToWindow();
                    isFirstTime = false;
                }
            }
        });
    }

    private void attachButtonListeners() {
        mainFrame.getAddRectButton().addActionListener(e -> setMode("DRAW_RECT"));
        mainFrame.getAddEllipseButton().addActionListener(e -> setMode("DRAW_ELLIPSE"));
        mainFrame.getAddLineButton().addActionListener(e -> setMode("DRAW_LINE"));
        mainFrame.getAddTextButton().addActionListener(e -> setMode("DRAW_TEXT"));
        mainFrame.getResetViewButton().addActionListener(e -> fitToWindow());
        mainFrame.getChangeColorButton().addActionListener(e -> changeSelectedObjectColor());
        mainFrame.getEditTextButton().addActionListener(e -> editSelectedText());
        mainFrame.getAddImageButton().addActionListener(e -> insertImage());
    }

    private void attachMouseWheelListener() {
        JScrollPane scrollPane = mainFrame.getCanvasScrollPane();
        scrollPane.addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
                if ((e.getModifiersEx() & shortcutMask) != 0) {
                    double oldScale = scale;
                    double scaleChange = (e.getWheelRotation() < 0) ? 1.1 : 1.0 / 1.1;
                    scale *= scaleChange;
                    scale = Math.max(0.02, Math.min(scale, 10.0));
                    Point center = new Point(scrollPane.getViewport().getWidth() / 2, scrollPane.getViewport().getHeight() / 2);
                    Point viewPos = scrollPane.getViewport().getViewPosition();
                    int centerX = viewPos.x + center.x;
                    int centerY = viewPos.y + center.y;
                    double newViewX = (centerX / oldScale * scale) - center.x;
                    double newViewY = (centerY / oldScale * scale) - center.y;
                    mainFrame.getCanvasPanel().setScale(scale);
                    SwingUtilities.invokeLater(() -> scrollPane.getViewport().setViewPosition(new Point((int)newViewX, (int)newViewY)));
                } else if (e.isShiftDown()) {
                    JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
                    int currentValue = horizontalScrollBar.getValue();
                    int amount = e.getUnitsToScroll() * horizontalScrollBar.getUnitIncrement();
                    horizontalScrollBar.setValue(currentValue + amount);
                } else {
                    JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
                    int currentValue = verticalScrollBar.getValue();
                    int amount = e.getUnitsToScroll() * verticalScrollBar.getUnitIncrement();
                    verticalScrollBar.setValue(currentValue + amount);
                }
            }
        });
    }

    // [!] 新增: 使用Key Bindings实现快捷键的方法
    private void attachKeyBindings() {
        // 获取画布组件的输入映射和动作映射
        InputMap inputMap = mainFrame.getCanvasPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = mainFrame.getCanvasPanel().getActionMap();

        // --- 绑定 Delete 键 ---
        // 1. 定义一个唯一的字符串标识这个动作
        String deleteActionKey = "deleteAction";
        // 2. 创建一个 KeyStroke 对象来代表 Delete 键
        KeyStroke deleteKeyStroke = KeyStroke.getKeyStroke("DELETE");
        // 3. 将按键和动作标识关联起来
        inputMap.put(deleteKeyStroke, deleteActionKey);

        // --- 绑定 Backspace 键 ---
        // 在macOS上，删除键通常是 Backspace
        KeyStroke backspaceKeyStroke = KeyStroke.getKeyStroke("BACK_SPACE");
        inputMap.put(backspaceKeyStroke, deleteActionKey); // 同样映射到 deleteActionKey

        // 4. 创建一个 AbstractAction 对象，它包含了要执行的具体逻辑
        AbstractAction deleteAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 这里的逻辑和之前右键菜单里的删除逻辑完全一样
                if (selectedObject != null) {
                    System.out.println("Delete/Backspace key pressed, deleting object."); // 调试信息
                    presentation.getSlides().get(0).removeObject(selectedObject);
                    setSelectedObject(null);
                    updatePropertiesPanel();
                    mainFrame.getCanvasPanel().repaint();
                }
            }
        };

        // 5. 将动作标识和具体的动作逻辑关联起来
        actionMap.put(deleteActionKey, deleteAction);
    }



    // --- 业务逻辑方法 (被策略类或监听器回调) ---

    public void updatePropertiesPanel() {
        boolean enableColorButton = selectedObject != null && (selectedObject instanceof RectangleShape || selectedObject instanceof EllipseShape || selectedObject instanceof LineShape || selectedObject instanceof TextBox);
        JButton colorButton = mainFrame.getChangeColorButton();
        colorButton.setEnabled(enableColorButton);
        if (selectedObject instanceof LineShape) colorButton.setText("更改线条颜色");
        else if (selectedObject instanceof TextBox) colorButton.setText("更改文字颜色");
        else colorButton.setText("更改填充颜色");
        
        boolean enableTextButton = selectedObject != null && selectedObject instanceof TextBox;
        mainFrame.getEditTextButton().setEnabled(enableTextButton);
    }

    private void changeSelectedObjectColor() {
        if (selectedObject == null) return;
        Color currentColor = Color.BLACK;
        if (selectedObject instanceof RectangleShape) currentColor = ((RectangleShape) selectedObject).getFillColor();
        else if (selectedObject instanceof EllipseShape) currentColor = ((EllipseShape) selectedObject).getFillColor();
        else if (selectedObject instanceof LineShape) currentColor = ((LineShape) selectedObject).getLineColor();
        else if (selectedObject instanceof TextBox) currentColor = ((TextBox) selectedObject).getTextColor();
        
        Color newColor = JColorChooser.showDialog(mainFrame, "选择颜色", currentColor);
        if (newColor != null) {
            if (selectedObject instanceof RectangleShape) ((RectangleShape) selectedObject).setFillColor(newColor);
            else if (selectedObject instanceof EllipseShape) ((EllipseShape) selectedObject).setFillColor(newColor);
            else if (selectedObject instanceof LineShape) ((LineShape) selectedObject).setLineColor(newColor);
            else if (selectedObject instanceof TextBox) ((TextBox) selectedObject).setTextColor(newColor);
            mainFrame.getCanvasPanel().repaint();
        }
    }

    private void editSelectedText() {
        if (selectedObject instanceof TextBox) {
            TextBox selectedTextBox = (TextBox) selectedObject;
            JTextArea textArea = new JTextArea(selectedTextBox.getText(), 5, 20);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            JScrollPane scrollPane = new JScrollPane(textArea);
            int result = JOptionPane.showConfirmDialog(mainFrame, scrollPane, "修改文本内容", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String newText = textArea.getText();
                if (newText != null) {
                    selectedTextBox.setText(newText);
                    mainFrame.getCanvasPanel().repaint();
                }
            }
        }
    }
    
    private void insertImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("请选择一张图片");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif");
            }
            public String getDescription() { return "图片文件 (*.jpg, *.png, *.gif)"; }
        });
        int result = fileChooser.showOpenDialog(mainFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                ImageObject imageObj = new ImageObject(100, 100, selectedFile.getAbsolutePath());
                presentation.getSlides().get(0).addObject(imageObj);
                mainFrame.getCanvasPanel().repaint();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(mainFrame, "无法加载图片文件: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- 坐标和视图控制方法 (被策略类或监听器回调) ---

    public Point convertScreenToWorld(Point screenPoint) {
        double logicCanvasX = screenPoint.x / scale;
        double logicCanvasY = screenPoint.y / scale;
        double pageStartX = (CanvasPanel.VIRTUAL_CANVAS_WIDTH - Slide.PAGE_WIDTH) / 2.0;
        double pageStartY = (CanvasPanel.VIRTUAL_CANVAS_HEIGHT - Slide.PAGE_HEIGHT) / 2.0;
        int finalWorldX = (int) (logicCanvasX - pageStartX);
        int finalWorldY = (int) (logicCanvasY - pageStartY);
        return new Point(finalWorldX, finalWorldY);
    }
    
    public boolean isPointInPage(Point worldPoint) {
        return worldPoint.x >= 0 && worldPoint.x <= Slide.PAGE_WIDTH &&
               worldPoint.y >= 0 && worldPoint.y <= Slide.PAGE_HEIGHT;
    }

    private void fitToWindow() {
        JScrollPane scrollPane = mainFrame.getCanvasScrollPane();
        if (scrollPane == null) return;
        JViewport viewport = scrollPane.getViewport();
        if (viewport.getWidth() <= 0 || viewport.getHeight() <= 0) return;
        int viewWidth = viewport.getWidth() - 50;
        int viewHeight = viewport.getHeight() - 50;
        double scaleX = (double) viewWidth / Slide.PAGE_WIDTH;
        double scaleY = (double) viewHeight / Slide.PAGE_HEIGHT;
        scale = Math.min(scaleX, scaleY);
        mainFrame.getCanvasPanel().setScale(scale);
        SwingUtilities.invokeLater(() -> {
            JScrollBar hBar = scrollPane.getHorizontalScrollBar();
            JScrollBar vBar = scrollPane.getVerticalScrollBar();
            int hMaxValue = hBar.getMaximum() - hBar.getVisibleAmount();
            int vMaxValue = vBar.getMaximum() - vBar.getVisibleAmount();
            hBar.setValue(hMaxValue / 2);
            vBar.setValue(vMaxValue / 2);
        });
        System.out.println("视图已适应窗口大小并居中，当前缩放比例: " + String.format("%.2f", scale));
    }
}