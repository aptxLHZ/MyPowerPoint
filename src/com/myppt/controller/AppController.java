package com.myppt.controller;
import com.myppt.view.PlayerFrame; 

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.ActionEvent; 
import java.awt.event.MouseEvent;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

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
import com.myppt.view.SlideThumbnail;
import com.myppt.view.ThumbnailPanel;

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
    private boolean isUpdatingUI = false;
    private InteractionStrategy currentStrategy; // 核心: 当前的交互策略
    private File currentFile = null; // [!] 新增: 用于追踪当前正在编辑的文件
    private boolean isDirty = false; // [!] 新增: “脏”标记

    public AppController(Presentation presentation, MainFrame mainFrame) {
        this.presentation = presentation;
        this.mainFrame = mainFrame;
        this.mainFrame.setupCanvas(presentation);
        
        // AppController一启动，默认的策略就是“选择”策略
        this.currentStrategy = new SelectStrategy(this);
        
        this.attachListeners();
        updateUI();

        updateTitle();
    }

    // --- Getters and Setters for shared state ---
    public void markAsDirty() { 
        if (!this.isDirty) { // 只有在状态从“干净”变“脏”时才更新
            this.isDirty = true;
            updateTitle(); // [!] 核心: 立即更新标题
        } 
    }
    public MainFrame getMainFrame() { return mainFrame; }
    public Presentation getPresentation() { return presentation; }
    public String getCurrentMode() { return currentMode; }
    public AbstractSlideObject getSelectedObject() { return selectedObject; }
    public InteractionStrategy getCurrentStrategy() { return currentStrategy; }
    
    public void setSelectedObject(AbstractSlideObject object) {
        this.selectedObject = object;
    }

    /**
     * 处理“新建”命令。
     */
    private void newFile() {
        if (!promptToSave()) {
            return; // 如果用户取消，则中断“新建”操作
        }
        
        // 1. 创建一个全新的 Presentation
        this.presentation = new Presentation();
        this.currentFile = null; // 新文件没有关联路径
        this.isDirty = false; 
        updateTitle();
        
        // 2. 更新 CanvasPanel 的数据模型
        mainFrame.getCanvasPanel().setPresentation(this.presentation);
        
        // 3. 更新整个UI
        updateUI();
        
        // 4. 重置视图
        SwingUtilities.invokeLater(() -> {
            fitToWindow();
        });
    }

    /**
     * 处理“保存”命令。
     * 如果是新文件（从未保存过），则调用“另存为”。
     * 否则，直接在当前文件上覆盖保存。
     */
    private void saveToFile() {
        if (currentFile == null) {
            saveAsToFile();
        } else {
            doSave(currentFile);
        }
    }

    /**
     * 处理“另存为”命令。
     * 总是会弹出文件选择对话框。
     * 保存成功后，程序将开始编辑这个新文件。
     */
    private void saveAsToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("另存为...");
        // ... (FileFilter 代码不变)
        
        int userSelection = fileChooser.showSaveDialog(mainFrame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".myppt")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".myppt");
            }
            
            // 调用保存逻辑
            if (doSave(fileToSave)) { // 检查 doSave 是否成功
                // 如果保存成功，则更新当前文件引用
                this.currentFile = fileToSave;
                updateTitle(); // [!] 更新窗口标题
            }
        }
    }

    /**
     * 真正执行保存到指定文件操作的私有方法。
     * @param file 要保存到的文件
     * @return 如果保存成功，返回true；否则返回false。
     */
    private boolean doSave(File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(presentation);
            
            // [!] 核心: 保存成功后，文件状态变为“干净”
            this.isDirty = false;
            updateTitle(); // [!] 新增: 保存成功后，立即更新标题以移除星号
            
            JOptionPane.showMessageDialog(mainFrame, "保存成功！");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "保存失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }


    /**
     * 处理“打开”命令。
     * 打开一个选中的.myppt文件，如果当前有未保存的更改，会提示用户是否保存。
     */
    private void openFromFile() {
        if (!promptToSave()) {
            return; // 如果用户取消，则中断“打开”操作
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("打开幻灯片");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".myppt");
            }
            public String getDescription() {
                return "MyPPT 幻灯片 (*.myppt)";
            }
        });

        int userSelection = fileChooser.showOpenDialog(mainFrame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(currentFile))) {
                Presentation loadedPresentation = (Presentation) ois.readObject();
                
                // 1. 直接替换 AppController 持有的数据模型引用
                this.presentation = loadedPresentation;

                this.isDirty = false; // [!] 打开后，文件是干净的
                updateTitle();
                
                // 2. 告诉 CanvasPanel 也使用这个新的数据模型
                mainFrame.getCanvasPanel().setPresentation(this.presentation);
                
                // 3. 更新整个UI（特别是左侧缩略图）
                updateUI();
                
                // 4. 自动重置视图
                SwingUtilities.invokeLater(() -> {
                    fitToWindow();
                });

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(mainFrame, "打开失败: 文件可能已损坏或格式不兼容。", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
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
        // 这个方法现在只在构造函数中调用一次
        attachFrameListeners();
        attachCanvasListeners();
    }

    private void attachFrameListeners() {
        attachComponentListeners();
        attachButtonListeners();
        attachMouseWheelListener();
        attachKeyBindings();
    }

    private void attachCanvasListeners() {
        CanvasController canvasController = new CanvasController(this);
        mainFrame.getCanvasPanel().addMouseListener(canvasController);
        mainFrame.getCanvasPanel().addMouseMotionListener(canvasController);
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
        mainFrame.getNewSlideButton().addActionListener(e -> {
            markAsDirty();
            presentation.addNewSlide();
            updateUI();
        });
        mainFrame.getDeleteSlideButton().addActionListener(e -> {
            // 为了防止误删，可以加一个确认对话框
            int choice = JOptionPane.showConfirmDialog(
                mainFrame, 
                "确定要删除当前页面吗?", // 提示：之后有了Undo/Redo就可以撤销了
                "确认删除", 
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            
            if (choice == JOptionPane.YES_OPTION) {
                markAsDirty();
                presentation.removeCurrentSlide();
                updateUI(); // 重新渲染UI以反映变化
            }
        });
        Runnable updateFontAction = () -> {
            // [核心] 如果是程序自己在更新UI，则忽略本次事件，直接返回
            if (isUpdatingUI) {
                return;
            }

            if (selectedObject instanceof TextBox) {
                TextBox tb = (TextBox) selectedObject;
                
                String name = (String) mainFrame.getFontNameBox().getSelectedItem();
                int size = (Integer) mainFrame.getFontSizeSpinner().getValue();
                int style = Font.PLAIN;
                if (mainFrame.getBoldCheckBox().isSelected()) style |= Font.BOLD;
                if (mainFrame.getItalicCheckBox().isSelected()) style |= Font.ITALIC;
                
                Font newFont = new Font(name, style, size);
                tb.setFont(newFont);
                markAsDirty();
                mainFrame.getCanvasPanel().repaint();
                repaintThumbnails(); 
            }
        };
        mainFrame.getFontNameBox().addActionListener(e -> updateFontAction.run());
        mainFrame.getFontSizeSpinner().addChangeListener(e -> updateFontAction.run());
        mainFrame.getBoldCheckBox().addActionListener(e -> updateFontAction.run());
        mainFrame.getItalicCheckBox().addActionListener(e -> updateFontAction.run());
        mainFrame.getOpenMenuItem().addActionListener(e -> openFromFile());
        mainFrame.getSaveMenuItem().addActionListener(e -> saveToFile());
        mainFrame.getNewMenuItem().addActionListener(e -> newFile());
        mainFrame.getSaveAsMenuItem().addActionListener(e -> saveAsToFile());
        mainFrame.getBorderColorButton().addActionListener(e -> {
            if (selectedObject instanceof RectangleShape || selectedObject instanceof EllipseShape) {
                Color currentColor = Color.BLACK;
                if (selectedObject instanceof RectangleShape) {
                    currentColor = ((RectangleShape) selectedObject).getBorderColor();
                } else if (selectedObject instanceof EllipseShape) {
                    currentColor = ((EllipseShape) selectedObject).getBorderColor();
                }

                Color newColor = JColorChooser.showDialog(mainFrame, "选择边框颜色", currentColor);
                if (newColor != null) {
                    if (selectedObject instanceof RectangleShape) {
                        ((RectangleShape) selectedObject).setBorderColor(newColor);
                    } else if (selectedObject instanceof EllipseShape) {
                        ((EllipseShape) selectedObject).setBorderColor(newColor);
                    }
                    markAsDirty();
                    mainFrame.getCanvasPanel().repaint();
                    repaintThumbnails();
                }
            }
        });
        mainFrame.getBorderStyleBox().addActionListener(e -> {
            if (isUpdatingUI) return; // 防止UI串扰
            if (selectedObject instanceof RectangleShape || selectedObject instanceof EllipseShape) {
                int selectedIndex = mainFrame.getBorderStyleBox().getSelectedIndex();
                
                // "无边框" 选项被选中
                if (selectedIndex == 3) {
                    if (selectedObject instanceof RectangleShape) {
                        ((RectangleShape) selectedObject).setBorderWidth(0f);
                    } else {
                        ((EllipseShape) selectedObject).setBorderWidth(0f);
                    }
                } else {
                    // 如果之前是"无边框"，切换回来时给一个默认宽度
                    if (selectedObject instanceof RectangleShape) {
                        RectangleShape rect = (RectangleShape) selectedObject;
                        if (rect.getBorderWidth() == 0) rect.setBorderWidth(1.0f);
                        rect.setBorderStyle(selectedIndex);
                    } else if (selectedObject instanceof EllipseShape) {
                        EllipseShape ellipse = (EllipseShape) selectedObject;
                        if (ellipse.getBorderWidth() == 0) ellipse.setBorderWidth(1.0f);
                        ellipse.setBorderStyle(selectedIndex);
                    }
                }
                
                markAsDirty();
                mainFrame.getCanvasPanel().repaint();
                repaintThumbnails();
                updatePropertiesPanel(); // 需要调用这个来同步 "粗细" 微调器的状态
            }
        });
        mainFrame.getBorderWidthSpinner().addChangeListener(e -> {
            if (isUpdatingUI) return; // 防止UI串扰
            if (selectedObject instanceof RectangleShape || selectedObject instanceof EllipseShape) {
                double newWidth = ((Number)mainFrame.getBorderWidthSpinner().getValue()).floatValue();
                if (selectedObject instanceof RectangleShape) {
                    ((RectangleShape) selectedObject).setBorderWidth(newWidth);
                } else {
                    ((EllipseShape) selectedObject).setBorderWidth(newWidth);
                }
                markAsDirty();
                mainFrame.getCanvasPanel().repaint();
                repaintThumbnails();
                updatePropertiesPanel(); // 宽度改变可能会影响到"无边框"选项
            }
        });
        mainFrame.getPlayButton().addActionListener(e -> {
            // 隐藏主窗口 (可选，但体验更好)
            mainFrame.setVisible(false); 
            
            // 创建并启动播放器
            PlayerFrame player = new PlayerFrame(presentation);
            // 3. 添加一个窗口监听器，以便在播放器关闭时得到通知
            player.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    // 当 player.dispose() 被调用后，这个方法会被触发
                    // 4. 重新显示主编辑窗口
                    mainFrame.setVisible(true);
                    // 5. 确保主窗口恢复焦点
                    mainFrame.toFront();
                    mainFrame.requestFocus();
                }
            });
            player.start();

        });
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
                    // System.out.println("Delete/Backspace key pressed, deleting object."); // 调试信息
                    markAsDirty();
                    presentation.getCurrentSlide().removeObject(selectedObject);
                    setSelectedObject(null);
                    updatePropertiesPanel();
                    mainFrame.getCanvasPanel().repaint();
                    repaintThumbnails();// 刷新缩略图面板
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

        // --- 更新字体样式控件 ---
        boolean isTextSelected = selectedObject instanceof TextBox;
        mainFrame.getFontNameBox().setEnabled(isTextSelected);
        mainFrame.getFontSizeSpinner().setEnabled(isTextSelected);
        mainFrame.getBoldCheckBox().setEnabled(isTextSelected);
        mainFrame.getItalicCheckBox().setEnabled(isTextSelected);

        if (isTextSelected) {
            // [核心] 开始用模型数据更新UI前，设置标志位
            isUpdatingUI = true;
            
            TextBox tb = (TextBox) selectedObject;
            Font f = tb.getFont();
            
            mainFrame.getFontNameBox().setSelectedItem(f.getFamily());
            mainFrame.getFontSizeSpinner().setValue(f.getSize());
            mainFrame.getBoldCheckBox().setSelected(f.isBold());
            mainFrame.getItalicCheckBox().setSelected(f.isItalic());
            
            // [核心] 更新UI结束后，清除标志位
            isUpdatingUI = false;
        }

        // [!] 新增: 边框控件的可见性/可用性
        boolean hasBorder = selectedObject instanceof RectangleShape || selectedObject instanceof EllipseShape;
        mainFrame.getBorderColorButton().setVisible(hasBorder);
        mainFrame.getBorderWidthSpinner().getParent().setVisible(hasBorder); // getParent() to get the JPanel
        mainFrame.getBorderStyleBox().getParent().setVisible(hasBorder); // [!] 新增: 控制线型选择器的可见性

        if (hasBorder) {
            mainFrame.getBorderColorButton().setEnabled(true);
            mainFrame.getBorderWidthSpinner().setEnabled(true);
            mainFrame.getBorderStyleBox().setEnabled(true); // [!] 新增: 启用

            if (selectedObject instanceof RectangleShape) {
                RectangleShape rect = (RectangleShape) selectedObject;
                mainFrame.getBorderWidthSpinner().setValue(rect.getBorderWidth());
                // [!] 新增: 根据模型状态更新UI
                if (rect.getBorderWidth() == 0) {
                    mainFrame.getBorderStyleBox().setSelectedIndex(3); // "无边框"
                } else {
                    mainFrame.getBorderStyleBox().setSelectedIndex(rect.getBorderStyle());
                }
            } else if (selectedObject instanceof EllipseShape) {
                EllipseShape ellipse = (EllipseShape) selectedObject;
                mainFrame.getBorderWidthSpinner().setValue(ellipse.getBorderWidth());
                // [!] 新增
                if (ellipse.getBorderWidth() == 0) {
                    mainFrame.getBorderStyleBox().setSelectedIndex(3);
                } else {
                    mainFrame.getBorderStyleBox().setSelectedIndex(ellipse.getBorderStyle());
                }
            }
        }

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
            markAsDirty();
            if (selectedObject instanceof RectangleShape) ((RectangleShape) selectedObject).setFillColor(newColor);
            else if (selectedObject instanceof EllipseShape) ((EllipseShape) selectedObject).setFillColor(newColor);
            else if (selectedObject instanceof LineShape) ((LineShape) selectedObject).setLineColor(newColor);
            else if (selectedObject instanceof TextBox) ((TextBox) selectedObject).setTextColor(newColor);
            mainFrame.getCanvasPanel().repaint();
            repaintThumbnails();
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
                    markAsDirty();
                    selectedTextBox.setText(newText);
                    mainFrame.getCanvasPanel().repaint();
                    repaintThumbnails();
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
                presentation.getCurrentSlide().addObject(imageObj);
                markAsDirty();
                mainFrame.getCanvasPanel().repaint();
                repaintThumbnails();
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

    // [!] 新增: 一个总的UI更新方法
    public void updateUI() {
        updateThumbnailList();
        mainFrame.getCanvasPanel().repaint();
        updatePropertiesPanel();
    }
    
    // [!] 新增: 更新左侧缩略图列表的核心方法
    private void updateThumbnailList() {
        ThumbnailPanel panel = mainFrame.getThumbnailPanel();
        panel.removeAll(); // 清空旧的缩略图

        List<Slide> slides = presentation.getSlides();
        for (int i = 0; i < slides.size(); i++) {
            Slide slide = slides.get(i);
            SlideThumbnail thumb = new SlideThumbnail(slide, i + 1);
            
            final int index = i; // 必须是final才能在lambda中使用
            thumb.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // 点击缩略图时，切换当前页面
                    presentation.setCurrentSlideIndex(index);
                    mainFrame.getCanvasPanel().repaint();//切换页面后，必须重绘主画布
                    updateUI(); // 更新整个UI
                }
            });

            // 设置当前选中页面的高亮边框
            if (i == presentation.getCurrentSlideIndex()) {
                thumb.setSelected(true);
            }

            panel.add(thumb);
        }
        
        // [!] 关键: 更新UI后需要 revalidate 和 repaint
        panel.revalidate();
        panel.repaint();
    }

    /**
     * 一个轻量级的方法，只重绘左侧的缩略图面板，而不重新创建所有组件。
     * 用于在画布内容改变时进行同步。
     */
    public void repaintThumbnails() {
        if (mainFrame != null && mainFrame.getThumbnailPanel() != null) {
            mainFrame.getThumbnailPanel().repaint();
        }
    }

    /**
     * 检查当前文件是否已修改，如果是，则弹出对话框询问用户是否保存。
     * @return 如果用户选择“取消”，则返回false，表示后续操作（如新建、打开）应被中断。
     *         否则返回true。
     */
    private boolean promptToSave() {
        if (!isDirty) {
            return true; // 文件没被修改，直接继续
        }
        
        int result = JOptionPane.showConfirmDialog(
            mainFrame,
            "当前文件已修改，是否保存？",
            "保存文件",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        switch (result) {
            case JOptionPane.YES_OPTION:
                saveToFile();
                return true; // 保存后继续
            case JOptionPane.NO_OPTION:
                return true; // 不保存，直接继续
            case JOptionPane.CANCEL_OPTION:
            case JOptionPane.CLOSED_OPTION:
                return false; // 用户取消，中断后续操作
        }
        return false;
    }

    /**
     * 根据当前文件状态，更新主窗口的标题栏。
     */
    private void updateTitle() {
        String title = "My PowerPoint - ";
        if (currentFile == null) {
            title += "未命名文件";
        } else {
            title += currentFile.toString(); // 只显示文件名，而不是完整路径
        }
        // [!] 核心: 如果文件已修改，则添加星号
        if (isDirty) {
            title += "*";
        }
        mainFrame.setTitle(title);
    }
}