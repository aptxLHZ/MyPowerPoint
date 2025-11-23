package com.myppt.controller;

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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Desktop; 

import java.net.URI; 

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import javax.swing.AbstractAction; // [!] 新增
import javax.swing.ActionMap;      // [!] 新增
import javax.swing.InputMap;       // [!] 新增
import javax.swing.JComponent;     // [!] 新增
import javax.swing.KeyStroke;      // [!] 新增
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.datatransfer.Clipboard;

import java.util.Timer;
import java.util.TimerTask;


import com.myppt.view.PlayerFrame; 
import com.myppt.controller.strategies.*; // 导入所有策略
import com.myppt.model.*;
import com.myppt.utils.PdfExporter;
import com.myppt.view.CanvasPanel;
import com.myppt.view.MainFrame;
import com.myppt.view.SlideThumbnail;
import com.myppt.view.ThumbnailPanel;
import com.myppt.commands.AddObjectCommand;
import com.myppt.commands.Command;
import com.myppt.commands.DeleteObjectCommand;
import com.myppt.commands.ChangeColorCommand;
import com.myppt.commands.ChangeTextCommand;
import com.myppt.commands.ChangeFontCommand;
import com.myppt.commands.ChangeBorderCommand;
import com.myppt.commands.ChangeOpacityCommand;

import org.apache.pdfbox.pdmodel.PDDocument; 
import org.apache.pdfbox.pdmodel.PDPage;     
import org.apache.pdfbox.pdmodel.common.PDRectangle; 
import java.io.BufferedReader; 
import java.io.InputStreamReader; 

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
    private UndoManager undoManager;
    private double borderWidthBeforeChange;
    private boolean isSpinnerDragging = false;
    private Style copiedStyle = null;
    private float opacityBeforeChange; 
    private JSlider opacitySlider; 
    private AbstractSlideObject clipboardObject = null; // [!] 新增: 内部剪贴板，用于存储深拷贝的对象
    
    private int pasteOffset = 0; // [!] 新增: 记录粘贴的累计偏移量
    private static final String AUTOSAVE_DIR_NAME = ".autosave"; // [!] 新增: 自动保存目录名
    private static final String AUTOSAVE_FILE_NAME = AUTOSAVE_DIR_NAME + File.separator + "current.myppt.tmp"; // [!] 修改: 完整文件路径
    private static final long AUTOSAVE_INTERVAL_MS = 2 * 1000; // 2秒自动保存一次
    private Timer autosaveTimer; // [!] 新增

    public AppController(Presentation presentation, MainFrame mainFrame) {
        
        this.presentation = presentation;
        this.mainFrame = mainFrame;
        this.mainFrame.setupCanvas(presentation);
        
        // AppController一启动，默认的策略就是“选择”策略
        this.currentStrategy = new SelectStrategy(this);
        
        this.undoManager = new UndoManager();
        this.opacitySlider = mainFrame.getOpacitySlider();

        checkAndRestoreAutosave();// [!] 核心修复: 在构造函数中，先检查自动保存文件
        this.attachListeners();
        updateUI();

        updateTitle();
        updateMenuState();
        
        startAutosaveTimer();// [!] 核心修复: 启动自动保存定时器
    }


    private void updateMenuState() {
        mainFrame.getUndoMenuItem().setEnabled(undoManager.canUndo());
        mainFrame.getRedoMenuItem().setEnabled(undoManager.canRedo());
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
    public UndoManager getUndoManager() { return this.undoManager; }
    public void setSelectedObject(AbstractSlideObject object) {
        this.selectedObject = object;
    }
    public Style getCopiedStyle() { return copiedStyle; }
    public void setCopiedStyle(Style copiedStyle) { this.copiedStyle = copiedStyle; }

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
    private boolean saveToFile() { // [!] 修改返回类型
        if (currentFile == null) {
            return saveAsToFile(); // 如果是新文件，行为同“另存为”，并返回其结果
        } else {
            return doSave(currentFile); // 直接覆盖保存，并返回其结果
        }
    }

    /**
     * 处理“另存为”命令。
     * 总是会弹出文件选择对话框。
     * 保存成功后，程序将开始编辑这个新文件。
     */
    private boolean saveAsToFile() { // [!] 修改返回类型
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("另存为...");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".myppt");
            }
            public String getDescription() {
                return "MyPPT 幻灯片 (*.myppt)";
            }
        });

        int userSelection = fileChooser.showSaveDialog(mainFrame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".myppt")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".myppt");
            }
            
            // 调用保存逻辑，并根据结果更新状态
            if (doSave(fileToSave)) { // 检查 doSave 是否成功
                this.currentFile = fileToSave;
                updateTitle();
                return true; // 保存成功，返回true
            } else {
                return false; // doSave 失败，返回false
            }
        } else {
            // [!] 核心修复: 用户在 JFileChooser 中点击了“取消”
            return false; // 保存操作未完成，返回false
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
            case "FORMAT_PAINTER":
                // 进入格式刷模式，我们不需要新的策略，因为它的逻辑很简单
                this.currentStrategy = new NullStrategy(); // 暂时禁用其他鼠标交互
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

    /**
     * 启动后台自动保存定时器。
     */
    private void startAutosaveTimer() {
        autosaveTimer = new Timer(true); // true 表示这是一个守护线程，程序退出时会自动停止
        autosaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // [!] 核心: 在后台线程中执行自动保存
                performAutosave();
            }
        }, AUTOSAVE_INTERVAL_MS, AUTOSAVE_INTERVAL_MS); // 延迟 AUTOSAVE_INTERVAL_MS 后开始，然后每隔 AUTOSAVE_INTERVAL_MS 执行
        System.out.println("自动保存定时器已启动，每 " + (AUTOSAVE_INTERVAL_MS / 1000) + " 秒保存一次。");
    }

    /**
     * 执行自动保存操作，保存到临时文件。
     */
    private void performAutosave() {
        // 1. 确保自动保存目录存在
        File autosaveDir = new File(AUTOSAVE_DIR_NAME);
        if (!autosaveDir.exists()) {
            autosaveDir.mkdirs(); // [!] 如果目录不存在，创建它
        }

        // 2. 构造临时文件路径
        File autosaveFile = new File(AUTOSAVE_FILE_NAME);
        
        // ... (后续保存逻辑不变) ...
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(autosaveFile))) {
            oos.writeObject(presentation);
            System.out.println("自动保存成功: " + AUTOSAVE_FILE_NAME);
        } catch (IOException e) {
            System.err.println("自动保存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查是否存在自动保存文件，如果存在，则询问用户是否恢复。
     */
    private void checkAndRestoreAutosave() {
        File autosaveFile = new File(AUTOSAVE_FILE_NAME);
        if (autosaveFile.exists()) {
            // [!] 发现自动保存文件，询问用户
            int result = JOptionPane.showConfirmDialog(
                mainFrame,
                "检测到上次意外关闭，是否恢复未保存的工作？",
                "自动恢复",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                // 用户选择恢复
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(autosaveFile))) {
                    this.presentation = (Presentation) ois.readObject();
                    // [!] 核心: 更新 CanvasPanel 的数据模型
                    mainFrame.getCanvasPanel().setPresentation(this.presentation);
                    this.isDirty = true; // 恢复的文件应被标记为已修改，需要用户手动保存
                    System.out.println("自动保存文件已恢复。");
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("恢复自动保存文件失败: " + e.getMessage());
                    JOptionPane.showMessageDialog(mainFrame, "自动恢复文件已损坏，无法恢复。", "错误", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
            
            // 无论是否恢复，都删除临时文件，防止下次启动再次提示
            deleteAutosaveFile(); 
        }
    }

    /**
     * 删除自动保存的临时文件。
     */
    private void deleteAutosaveFile() {
        File autosaveFile = new File(AUTOSAVE_FILE_NAME);
        File autosaveDir = new File(AUTOSAVE_DIR_NAME);

        if (autosaveFile.exists()) {
            if (autosaveFile.delete()) {
                System.out.println("自动保存文件已删除。");
            } else {
                System.err.println("无法删除自动保存文件: " + AUTOSAVE_FILE_NAME);
            }
        }
        // [!] 如果目录为空，也尝试删除目录
        if (autosaveDir.exists() && autosaveDir.isDirectory() && autosaveDir.list().length == 0) {
            if (autosaveDir.delete()) {
                System.out.println("自动保存目录已删除。");
            } else {
                System.err.println("无法删除自动保存目录: " + AUTOSAVE_DIR_NAME);
            }
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
        // [!] 核心修复: 为主窗口添加 WindowListener
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // 在窗口关闭前，询问用户是否保存
                if (!promptToSave()) {
                    // 如果用户选择“取消”，则阻止窗口关闭
                    mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                } else {
                    // [!] 核心修复: 正常关闭时删除自动保存文件
                    deleteAutosaveFile();
                    // 如果用户选择“是”或“否”，允许窗口正常关闭
                    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                }
            }
        });
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
        if (isUpdatingUI) return;

        if (selectedObject instanceof TextBox) {
            TextBox tb = (TextBox) selectedObject;
            Font oldFont = tb.getFont(); // 获取旧字体
            
            String name = (String) mainFrame.getFontNameBox().getSelectedItem();
            int size = (Integer) mainFrame.getFontSizeSpinner().getValue();
            int style = Font.PLAIN;
            if (mainFrame.getBoldCheckBox().isSelected()) style |= Font.BOLD;
            if (mainFrame.getItalicCheckBox().isSelected()) style |= Font.ITALIC;
            
            Font newFont = new Font(name, style, size);

            // 只有当字体真的改变时才创建命令
            if (!newFont.equals(oldFont)) {
                Command command = new ChangeFontCommand(tb, newFont);
                undoManager.executeCommand(command);

                markAsDirty();
                updateUI();
            }
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
            if (isUpdatingUI || selectedObject == null) return;
            if (!(selectedObject instanceof RectangleShape || selectedObject instanceof EllipseShape || selectedObject instanceof LineShape)) return;

            Color currentColor = Color.BLACK; // 默认值
            double currentWidth = 1.0;
            int currentStyle = AbstractSlideObject.BORDER_STYLE_SOLID;

            if (selectedObject instanceof RectangleShape) {
                currentColor = ((RectangleShape) selectedObject).getBorderColor();
                currentWidth = ((RectangleShape) selectedObject).getBorderWidth();
                currentStyle = ((RectangleShape) selectedObject).getBorderStyle();
            } else if (selectedObject instanceof EllipseShape) {
                currentColor = ((EllipseShape) selectedObject).getBorderColor();
                currentWidth = ((EllipseShape) selectedObject).getBorderWidth();
                currentStyle = ((EllipseShape) selectedObject).getBorderStyle();
            } else if (selectedObject instanceof LineShape) {
                currentColor = ((LineShape) selectedObject).getLineColor();
                currentWidth = ((LineShape) selectedObject).getStrokeWidth();
                currentStyle = ((LineShape) selectedObject).getBorderStyle();
            }
            
            Color newColor = JColorChooser.showDialog(mainFrame, "选择边框颜色", currentColor);
            
            if (newColor != null && !newColor.equals(currentColor)) {
                Command command = new ChangeBorderCommand(selectedObject, newColor, currentWidth, currentStyle);
                undoManager.executeCommand(command);

                markAsDirty();
                updateUI();
            }
        });
        mainFrame.getBorderWidthSpinner().addChangeListener(e -> {
            if (isUpdatingUI || selectedObject == null) return;
            if (!(selectedObject instanceof RectangleShape || selectedObject instanceof EllipseShape || selectedObject instanceof LineShape)) return;

            // ChangeListener 只是实时更新UI，不创建命令
            double newWidth = ((Number)mainFrame.getBorderWidthSpinner().getValue()).doubleValue();
            if (selectedObject instanceof RectangleShape) {
                ((RectangleShape) selectedObject).setBorderWidth(newWidth);
            } else if (selectedObject instanceof EllipseShape) {
                ((EllipseShape) selectedObject).setBorderWidth(newWidth);
            } else if (selectedObject instanceof LineShape) {
                ((LineShape) selectedObject).setStrokeWidth((float)newWidth);
            }
            markAsDirty();
            mainFrame.getCanvasPanel().repaint();
            repaintThumbnails();
            updatePropertiesPanel(); // 实时更新可能会影响线型下拉框的状态（比如宽度变为0）
        });
        mainFrame.getBorderWidthSpinner().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isUpdatingUI || selectedObject == null) return;
                isSpinnerDragging = true;
                if (selectedObject instanceof RectangleShape) {
                    borderWidthBeforeChange = ((RectangleShape) selectedObject).getBorderWidth();
                } else if (selectedObject instanceof EllipseShape) {
                    borderWidthBeforeChange = ((EllipseShape) selectedObject).getBorderWidth();
                } else if (selectedObject instanceof LineShape) {
                    borderWidthBeforeChange = ((LineShape) selectedObject).getStrokeWidth();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!isSpinnerDragging) return;
                isSpinnerDragging = false;
                double newValue = ((Number)mainFrame.getBorderWidthSpinner().getValue()).doubleValue();
                if (newValue != borderWidthBeforeChange) {
                    Object target = selectedObject;
                    
                    // 恢复模型状态 (确保 UndoManager 的 executeCommand 是从 oldValue 到 newValue)
                    if (target instanceof RectangleShape) ((RectangleShape) target).setBorderWidth(borderWidthBeforeChange);
                    else if (target instanceof EllipseShape) ((EllipseShape) target).setBorderWidth(borderWidthBeforeChange);
                    else if (target instanceof LineShape) ((LineShape) target).setStrokeWidth((float)borderWidthBeforeChange);

                    // 获取其他属性的当前值
                    Color currentColor = Color.BLACK; // 默认值
                    int currentStyle = AbstractSlideObject.BORDER_STYLE_SOLID;
                    if (selectedObject instanceof RectangleShape) {
                        currentColor = ((RectangleShape) selectedObject).getBorderColor();
                        currentStyle = ((RectangleShape) selectedObject).getBorderStyle();
                    } else if (selectedObject instanceof EllipseShape) {
                        currentColor = ((EllipseShape) selectedObject).getBorderColor();
                        currentStyle = ((EllipseShape) selectedObject).getBorderStyle();
                    } else if (selectedObject instanceof LineShape) {
                        currentColor = ((LineShape) selectedObject).getLineColor();
                        currentStyle = ((LineShape) selectedObject).getBorderStyle();
                    }
                    
                    Command command = new ChangeBorderCommand(target, currentColor, newValue, currentStyle); // newWidth 是 newValue
                    undoManager.executeCommand(command);
                    markAsDirty();
                }
            }
        });
        mainFrame.getBorderStyleBox().addActionListener(e -> {
            if (isUpdatingUI || selectedObject == null) return;
            if (!(selectedObject instanceof RectangleShape || selectedObject instanceof EllipseShape || selectedObject instanceof LineShape)) return;

            int selectedIndex = mainFrame.getBorderStyleBox().getSelectedIndex();
            
            Color currentColor = Color.BLACK;
            double currentWidth = 1.0;
            int currentStyle = AbstractSlideObject.BORDER_STYLE_SOLID;

            // 获取当前其他属性的值 (区分直线)
            if (selectedObject instanceof RectangleShape) {
                currentColor = ((RectangleShape) selectedObject).getBorderColor();
                currentWidth = ((RectangleShape) selectedObject).getBorderWidth();
                currentStyle = ((RectangleShape) selectedObject).getBorderStyle();
            } else if (selectedObject instanceof EllipseShape) {
                currentColor = ((EllipseShape) selectedObject).getBorderColor();
                currentWidth = ((EllipseShape) selectedObject).getBorderWidth();
                currentStyle = ((EllipseShape) selectedObject).getBorderStyle();
            } else if (selectedObject instanceof LineShape) {
                currentColor = ((LineShape) selectedObject).getLineColor();
                currentWidth = ((LineShape) selectedObject).getStrokeWidth();
                currentStyle = ((LineShape) selectedObject).getBorderStyle();
            }
            
            double newWidth = currentWidth;
            int newStyle = selectedIndex;

            // 处理“无边框”选项
            if (selectedIndex == 3) { // "无边框"
                newWidth = 0;
                // 保持原有的线型，以便恢复
                newStyle = currentStyle; // [!] 核心修复: 保持旧样式，而不是根据选中对象再判断
            } else {
                if (currentWidth == 0) {
                    newWidth = 1.0; // 如果之前是无边框，现在恢复默认宽度
                }
                newStyle = selectedIndex; // 真正的线型
            }

            // 只有当属性真的改变时才创建命令
            if (newWidth != currentWidth || newStyle != currentStyle) {
                Command command = new ChangeBorderCommand(selectedObject, currentColor, newWidth, newStyle);
                undoManager.executeCommand(command);
                markAsDirty();
                updateUI();
            }
        });
        mainFrame.getPlayFromStartButton().addActionListener(e -> {playPresentation(true);  });
        mainFrame.getPlayButton().addActionListener(e -> { playPresentation(false);  });
        mainFrame.getUndoMenuItem().addActionListener(e -> { undoManager.undo(); updateUI();});
        mainFrame.getRedoMenuItem().addActionListener(e -> { undoManager.redo(); updateUI();});
        mainFrame.getFormatPainterButton().addActionListener(e -> {
            if (selectedObject != null) {
                copiedStyle = selectedObject.getStyle();
                setMode("FORMAT_PAINTER"); // 进入一个新模式
                // [!] 核心修复: 直接使用系统提供的十字光标
                mainFrame.getCanvasPanel().setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }
        });
        mainFrame.getOpacitySlider().addChangeListener(e -> {
            if (isUpdatingUI || selectedObject == null) return;
            if (!(selectedObject instanceof ImageObject)) return;

            // ChangeListener 负责实时预览
            ImageObject img = (ImageObject) selectedObject;
            float newOpacity = (float)mainFrame.getOpacitySlider().getValue() / 100.0f; // Slider值是0-100，转为0.0-1.0
            img.setOpacity(newOpacity);
            markAsDirty();
            mainFrame.getCanvasPanel().repaint();
            repaintThumbnails();
        });
        mainFrame.getOpacitySlider().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isUpdatingUI || selectedObject == null) return;
                isSpinnerDragging = true; // 复用isSpinnerDragging标志，因为它也是拖拽调整
                if (selectedObject instanceof ImageObject) {
                    opacityBeforeChange = ((ImageObject) selectedObject).getOpacity();
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!isSpinnerDragging) return;
                isSpinnerDragging = false;
                if (!(selectedObject instanceof ImageObject)) return;

                float newOpacity = (float)mainFrame.getOpacitySlider().getValue() / 100.0f;
                if (newOpacity != opacityBeforeChange) {
                    ImageObject target = (ImageObject) selectedObject;
                    target.setOpacity(opacityBeforeChange); // 恢复旧状态
                    Command command = new ChangeOpacityCommand(target, newOpacity);
                    undoManager.executeCommand(command);
                    markAsDirty();
                }
            }
        });
        mainFrame.getHelpMenuItem().addActionListener(e -> showHelpDialog());
        mainFrame.getGithubMenuItem().addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/aptxLHZ/MyPowerPoint"));
            } catch (IOException | java.net.URISyntaxException ex) {
                JOptionPane.showMessageDialog(mainFrame, "无法打开浏览器，请手动访问：" + "https://github.com/aptxLHZ/MyPowerPoint", "错误", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });
        mainFrame.getExportPdfMenuItem().addActionListener(e -> exportToPdf());
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

    private void attachKeyBindings() {
        // [!] 核心修复: 将快捷键绑定到 JFrame 的根面板，而不是 CanvasPanel
        JRootPane rootPane = mainFrame.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        // --- 绑定 Delete 和 Backspace ---
        String deleteActionKey = "deleteAction";
        KeyStroke deleteKeyStroke = KeyStroke.getKeyStroke("DELETE");
        KeyStroke backspaceKeyStroke = KeyStroke.getKeyStroke("BACK_SPACE");
        inputMap.put(deleteKeyStroke, deleteActionKey);
        inputMap.put(backspaceKeyStroke, deleteActionKey);
        actionMap.put(deleteActionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedObject != null) {
                    markAsDirty();
                    Command command = new DeleteObjectCommand(presentation.getCurrentSlide(), selectedObject);
                    undoManager.executeCommand(command);
                    setSelectedObject(null);
                    updateUI();
                }
            }
        });

        // --- 绑定 Undo (Ctrl+Z) ---
        String undoActionKey = "undoAction";
        KeyStroke undoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(undoKeyStroke, undoActionKey);
        actionMap.put(undoActionKey, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                System.out.println(">>> Undo Action Triggered! (Ctrl+Z pressed)");
                undoManager.undo();
                updateUI();
            }
        });

        // --- 绑定 Redo (Ctrl+Y or Ctrl+Shift+Z) ---
        String redoActionKey = "redoAction";
        KeyStroke redoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        KeyStroke redoShiftZKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK);
        inputMap.put(redoKeyStroke, redoActionKey);
        inputMap.put(redoShiftZKeyStroke, redoActionKey);
        actionMap.put(redoActionKey, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                System.out.println(">>> Redo Action Triggered! (Ctrl+Y pressed)");
                undoManager.redo();
                updateUI();
            }
        });

        // --- 绑定 F5 (从头播放) ---
        String playActionKey = "playAction";
        KeyStroke f5KeyStroke = KeyStroke.getKeyStroke("F5");
        inputMap.put(f5KeyStroke, playActionKey);
        actionMap.put(playActionKey, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                playPresentation(true);
            }
        });

        // --- 绑定 ESC (取消格式刷模式) ---
        String cancelActionKey = "cancelAction";
        KeyStroke escKeyStroke = KeyStroke.getKeyStroke("ESCAPE");
        inputMap.put(escKeyStroke, cancelActionKey);
        actionMap.put(cancelActionKey, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (currentMode.equals("FORMAT_PAINTER")) {
                    setMode("SELECT");
                    mainFrame.getCanvasPanel().setCursor(Cursor.getDefaultCursor());
                    copiedStyle = null; // 清除复制的样式
                }
            }
        });



        // --- 绑定 Copy (Ctrl+C) ---
        String copyActionKey = "copyAction";
        KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(copyKeyStroke, copyActionKey);
        actionMap.put(copyActionKey, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                copySelectedObject();
            }
        });

        // --- 绑定 Cut (Ctrl+X) ---
        String cutActionKey = "cutAction";
        KeyStroke cutKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(cutKeyStroke, cutActionKey);
        actionMap.put(cutActionKey, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cutSelectedObject();
            }
        });

        // --- 绑定 Paste (Ctrl+V) ---
        String pasteActionKey = "pasteAction";
        KeyStroke pasteKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(pasteKeyStroke, pasteActionKey);
        actionMap.put(pasteActionKey, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                pasteObject();
            }
        });

    }

    public void updatePropertiesPanel() {
        isUpdatingUI = true;
        try {
            boolean isTextSelected = selectedObject instanceof TextBox;
            boolean hasBorder = selectedObject instanceof RectangleShape || selectedObject instanceof EllipseShape || selectedObject instanceof LineShape;
            boolean isImageSelected = selectedObject instanceof ImageObject;

            // --- 更新所有属性组的【可见性】---
            // [!] 核心修复: 控制包裹面板的可见性，而不是单个控件
            mainFrame.getTextStyleGroupPanel().setVisible(isTextSelected);
            mainFrame.getBorderStyleGroupPanel().setVisible(hasBorder);
            mainFrame.getOpacityGroupPanel().setVisible(isImageSelected);

            // --- 更新所有控件的【可用性】---
            // [!] 对所有控件设置启用/禁用，而不是依赖可见性
            mainFrame.getChangeColorButton().setEnabled(selectedObject != null); 
            mainFrame.getEditTextButton().setEnabled(isTextSelected);
            mainFrame.getFontNameBox().setEnabled(isTextSelected);
            mainFrame.getFontSizeSpinner().setEnabled(isTextSelected);
            mainFrame.getBoldCheckBox().setEnabled(isTextSelected);
            mainFrame.getItalicCheckBox().setEnabled(isTextSelected);
            mainFrame.getBorderColorButton().setEnabled(hasBorder);
            mainFrame.getBorderWidthSpinner().setEnabled(hasBorder);
            mainFrame.getBorderStyleBox().setEnabled(hasBorder);
            mainFrame.getOpacitySlider().setEnabled(isImageSelected);
            
            // --- 用模型数据【设置】所有控件的值 ---
            if (selectedObject != null) {
                // 设置颜色按钮的文字
                if (selectedObject instanceof LineShape) mainFrame.getChangeColorButton().setText("更改线条颜色");
                else if (isTextSelected) mainFrame.getChangeColorButton().setText("更改文字颜色");
                else mainFrame.getChangeColorButton().setText("更改填充颜色");
                
                // 设置字体属性
                if (isTextSelected) {
                    TextBox tb = (TextBox) selectedObject;
                    Font f = tb.getFont();
                    mainFrame.getFontNameBox().setSelectedItem(f.getFamily());
                    mainFrame.getFontSizeSpinner().setValue(f.getSize());
                    mainFrame.getBoldCheckBox().setSelected(f.isBold());
                    mainFrame.getItalicCheckBox().setSelected(f.isItalic());
                }

                // 设置边框属性
                if (hasBorder) {
                    double borderWidth = 0;
                    int borderStyle = 0;
                    if (selectedObject instanceof RectangleShape) {
                        RectangleShape rect = (RectangleShape) selectedObject;
                        borderWidth = rect.getBorderWidth();
                        borderStyle = rect.getBorderStyle();
                    } else if (selectedObject instanceof EllipseShape) {
                        EllipseShape ellipse = (EllipseShape) selectedObject;
                        borderWidth = ellipse.getBorderWidth();
                        borderStyle = ellipse.getBorderStyle();
                    } else if (selectedObject instanceof LineShape) {
                        LineShape line = (LineShape) selectedObject;
                        borderWidth = line.getStrokeWidth();
                        borderStyle = line.getBorderStyle();
                    }
                    mainFrame.getBorderWidthSpinner().setValue(borderWidth);
                    if (borderWidth == 0) {
                        mainFrame.getBorderStyleBox().setSelectedIndex(3);
                    } else {
                        mainFrame.getBorderStyleBox().setSelectedIndex(borderStyle);
                    }
                }

                // 设置透明度属性
                if (isImageSelected) {
                    ImageObject img = (ImageObject) selectedObject;
                    mainFrame.getOpacitySlider().setValue((int)(img.getOpacity() * 100));
                }
            }
        } finally {
            isUpdatingUI = false;
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
        if (newColor != null && !newColor.equals(currentColor)) {
            // [!] 核心修改:
            Command command = new ChangeColorCommand(selectedObject, newColor);
            undoManager.executeCommand(command);

            markAsDirty();
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
                if (newText != null && !newText.equals(selectedTextBox.getText())) {
                    Command command = new ChangeTextCommand(selectedTextBox, newText);
                    undoManager.executeCommand(command);

                    markAsDirty();
                    updateUI();
                }
            }
        }
    }
    
    /**
     * 插入图片，现在将图片数据嵌入到PPT文件中。
     */
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
        int userSelection = fileChooser.showOpenDialog(mainFrame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // [!] 核心修改: 读取整个文件为字节数组
                byte[] imageData = java.nio.file.Files.readAllBytes(selectedFile.toPath());
                
                // [!] 核心修改: 使用字节数组构造 ImageObject
                ImageObject imageObj = new ImageObject(100, 100, imageData);
                
                Command command = new AddObjectCommand(presentation.getCurrentSlide(), imageObj);
                undoManager.executeCommand(command);
                
                markAsDirty();
                updateUI();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(mainFrame, "无法读取图片文件或创建对象: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
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

    /**
     * 启动幻灯片放映。
     * @param fromStart 如果为 true，则从第一页开始播放；否则，从当前页开始。
     */
    private void playPresentation(boolean fromStart) {
        if (fromStart) {
            presentation.setCurrentSlideIndex(0); // 如果是从头播放，先将索引设为0
            updateUI(); // 更新UI以确保左侧高亮在第一页
        }

        mainFrame.setVisible(false);
        PlayerFrame player = new PlayerFrame(presentation);
        
        player.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                mainFrame.setVisible(true);
                mainFrame.toFront();
                mainFrame.requestFocus();
            }
        });
        
        player.start();
    }
    
    // [!] 新增: 一个总的UI更新方法
    public void updateUI() {
        updateThumbnailList();
        mainFrame.getCanvasPanel().repaint();
        updatePropertiesPanel();
        updateMenuState();
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
                return saveToFile(); // [!] 核心修复: 调用 saveToFile() 并返回它的结果
            case JOptionPane.NO_OPTION:
                return true; // 不保存，直接继续
            case JOptionPane.CANCEL_OPTION:
            case JOptionPane.CLOSED_OPTION:
                return false; // 用户取消，中断后续操作
        }
        return false; // 默认返回，理论上不会到达
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

    /**
     * 复制选中的对象到内部剪贴板。
     */
    private void copySelectedObject() {
        if (selectedObject != null) {
            clipboardObject = selectedObject.deepCopy();
            pasteOffset = 0; // [!] 新增: 复制新对象时，重置偏移量
            // [!] 可选: 也可以复制到系统剪贴板，但我们只做内部复制
            // StringSelection ss = new StringSelection(selectedObject.getId().toString()); // 示例：复制ID
            // systemClipboard.setContents(ss, null);
            System.out.println("Object copied to internal clipboard.");
        }
    }

    /**
     * 剪切选中的对象到内部剪贴板，并删除原对象。
     */
    private void cutSelectedObject() {
        if (selectedObject != null) {
            copySelectedObject(); // 先复制
            // 然后执行删除操作
            Command command = new DeleteObjectCommand(presentation.getCurrentSlide(), selectedObject);
            undoManager.executeCommand(command);
            markAsDirty();
            setSelectedObject(null); // 删除后取消选中
            updateUI();
            System.out.println("Object cut to internal clipboard.");
        }
    }

    /**
     * 从内部剪贴板粘贴对象。
     */
    private void pasteObject() {
        if (clipboardObject != null) {
            AbstractSlideObject pastedObject = clipboardObject.deepCopy(); // 每次粘贴都是一个新的副本
            
            // [!] 核心修复: 每次粘贴都使用累计偏移量
            int offsetX = 20; // 每次粘贴向右下偏移的像素
            int offsetY = 20;
            
            pastedObject.setX(pastedObject.getX() + pasteOffset + offsetX);
            pastedObject.setY(pastedObject.getY() + pasteOffset + offsetY);

            pasteOffset += offsetX; // 更新累计偏移量

            // 如果偏移量过大，可以考虑重置，或让它在页面内循环
            if (pastedObject.getX() > (Slide.PAGE_WIDTH - 50) || pastedObject.getY() > (Slide.PAGE_HEIGHT - 50)) {
                pasteOffset = 0; // 粘贴到页面边界附近时重置
            }


            // 执行添加操作
            Command command = new AddObjectCommand(presentation.getCurrentSlide(), pastedObject);
            undoManager.executeCommand(command);
            markAsDirty();
            setSelectedObject(pastedObject); // 粘贴后选中新对象
            updateUI();
            System.out.println("Object pasted from internal clipboard.");
        }
    }

    /**
     * 显示用户使用说明对话框。
     */
    // 用这个方法完整替换旧的 showHelpDialog()
    private void showHelpDialog() {
        StringBuilder helpHtmlTextBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    getClass().getResourceAsStream("/main/resources/help.html"), // [!] 核心: 从资源流读取文件
                    "UTF-8"))) { // [!] 指定编码
            String line;
            while ((line = reader.readLine()) != null) {
                helpHtmlTextBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("加载使用说明文件失败: " + e.getMessage());
            helpHtmlTextBuilder.append("<html><body><p><b>错误: 无法加载使用说明文件。</b></p></body></html>");
        }
        String helpHtmlText = helpHtmlTextBuilder.toString();

        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setText(helpHtmlText);
        textPane.setEditable(false);
        
        // [!] 设置一个合适的默认字体和大小，因为 HTML 默认字体可能很小
        // 注意：这里的字体设置只对 JTextPane 自己的默认样式有效，不会覆盖 HTML 中的 style 属性
        textPane.setFont(new Font("Dialog", Font.PLAIN, 10)); 

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setPreferredSize(new Dimension(650, 500));
        
        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(0); // 确保滚动条在顶部
        });
        
        JOptionPane.showMessageDialog(
            mainFrame,
            scrollPane,
            "MyPPT 使用说明",
            JOptionPane.INFORMATION_MESSAGE
        );
    }


    /**
     * 将当前幻灯片导出为 PDF 文件。
     */
    private void exportToPdf() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出为PDF");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".pdf");
            }
            public String getDescription() {
                return "PDF 文件 (*.pdf)";
            }
        });

        int userSelection = fileChooser.showSaveDialog(mainFrame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
            }
            
            PDDocument document = new PDDocument(); // PDFBox 的文档对象
            try {
                // [!] 核心: 遍历所有页面并绘制
                for (Slide slide : presentation.getSlides()) {
                    // [!] 核心修复: 修改 PDF 页面创建方式以适配 PDFBox v3.0.x
                    // PDRectangle.A4 默认是纵向，为了横向，我们需要交换宽高
                    PDRectangle pageSize = PDRectangle.A4;
                    // 交换宽高以实现横向
                    PDPage page = new PDPage(new PDRectangle(pageSize.getHeight(), pageSize.getWidth())); 
                    document.addPage(page);

                    PdfExporter exporter = new PdfExporter(document, page);
                    exporter.drawSlideToPdf(slide);
                    exporter.close(); // 关闭内容流
                }

                document.save(fileToSave);
                JOptionPane.showMessageDialog(mainFrame, "PDF导出成功！", "导出完成", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(mainFrame, "PDF导出失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    if (document != null) document.close();
                } catch (IOException e) {
                    System.err.println("关闭PDF文档失败: " + e.getMessage());
                }
            }
        }
    }
}