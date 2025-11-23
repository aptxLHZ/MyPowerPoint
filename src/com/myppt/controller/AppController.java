package com.myppt.controller;

import java.awt.Point;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import com.myppt.controller.strategies.DrawLineStrategy;
import com.myppt.controller.strategies.DrawObjectStrategy;
import com.myppt.controller.strategies.InteractionStrategy;
import com.myppt.controller.strategies.NullStrategy;
import com.myppt.controller.strategies.SelectStrategy;
import com.myppt.model.AbstractSlideObject;
import com.myppt.model.Presentation;
import com.myppt.model.Style;
import com.myppt.view.MainFrame;

/**
 * 应用程序的主控制器 (重构后)。
 * 负责持有核心状态和模型，并协调各个专门的处理器 (Handler/Manager)。
 */
public class AppController {
    // 核心模型和视图
    private Presentation presentation;
    private final MainFrame mainFrame;
    private final UndoManager undoManager;

    // 子控制器/处理器
    private final FileHandler fileHandler;
    private final ActionHandler actionHandler;
    private final UIUpdater uiUpdater;
    private final AutosaveManager autosaveManager;
    
    // 核心状态
    private String currentMode = "SELECT";
    private double scale = 1.0;
    private AbstractSlideObject selectedObject = null;
    private InteractionStrategy currentStrategy;
    private Style copiedStyle = null; // 用于格式刷
    private AbstractSlideObject clipboardObject = null; // 用于复制粘贴
    private int pasteOffset = 0; // 粘贴偏移量

    public AppController(Presentation presentation, MainFrame mainFrame) {
        this.presentation = presentation;
        this.mainFrame = mainFrame;
        this.mainFrame.setupCanvas(presentation);
        
        this.undoManager = new UndoManager();
        this.currentStrategy = new SelectStrategy(this);
        
        // 初始化所有子处理器
        this.uiUpdater = new UIUpdater(this);
        this.actionHandler = new ActionHandler(this);
        this.fileHandler = new FileHandler(this);
        this.autosaveManager = new AutosaveManager(this);
        
        // 检查自动恢复
        this.autosaveManager.checkAndRestore();

        // 统一设置监听器
        new ListenerSetup(this).attachAllListeners();

        // 初始化UI
        uiUpdater.updateUI();
        uiUpdater.updateTitle();
        
        // 启动自动保存
        this.autosaveManager.start();
    }

    // --- 状态管理 ---

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
                this.currentStrategy = new NullStrategy();
                break;
            default:
                this.currentStrategy = new NullStrategy();
        }
        
        if (!mode.equals("SELECT")) {
            if (this.selectedObject != null) {
                this.selectedObject.setSelected(false);
            }
            setSelectedObject(null);
            uiUpdater.updatePropertiesPanel();
            mainFrame.getCanvasPanel().repaint();
        }
    }

    public void fitToWindow() {
        JScrollPane scrollPane = mainFrame.getCanvasScrollPane();
        if (scrollPane == null) return;
        JViewport viewport = scrollPane.getViewport();
        if (viewport.getWidth() <= 0 || viewport.getHeight() <= 0) return;
        
        int viewWidth = viewport.getWidth() - 50;
        int viewHeight = viewport.getHeight() - 50;
        double scaleX = (double) viewWidth / com.myppt.model.Slide.PAGE_WIDTH;
        double scaleY = (double) viewHeight / com.myppt.model.Slide.PAGE_HEIGHT;
        scale = Math.min(scaleX, scaleY);
        
        mainFrame.getCanvasPanel().setScale(scale);
        
        SwingUtilities.invokeLater(() -> {
            JScrollBar hBar = scrollPane.getHorizontalScrollBar();
            JScrollBar vBar = scrollPane.getVerticalScrollBar();
            hBar.setValue((hBar.getMaximum() - hBar.getVisibleAmount()) / 2);
            vBar.setValue((vBar.getMaximum() - vBar.getVisibleAmount()) / 2);
        });
        System.out.println("视图已适应窗口大小并居中，当前缩放比例: " + String.format("%.2f", scale));
    }

    // --- 坐标转换工具 ---

    public Point convertScreenToWorld(Point screenPoint) {
        double logicCanvasX = screenPoint.x / scale;
        double logicCanvasY = screenPoint.y / scale;
        double pageStartX = (com.myppt.view.CanvasPanel.VIRTUAL_CANVAS_WIDTH - com.myppt.model.Slide.PAGE_WIDTH) / 2.0;
        double pageStartY = (com.myppt.view.CanvasPanel.VIRTUAL_CANVAS_HEIGHT - com.myppt.model.Slide.PAGE_HEIGHT) / 2.0;
        int finalWorldX = (int) (logicCanvasX - pageStartX);
        int finalWorldY = (int) (logicCanvasY - pageStartY);
        return new Point(finalWorldX, finalWorldY);
    }
    
    public boolean isPointInPage(Point worldPoint) {
        return worldPoint.x >= 0 && worldPoint.x <= com.myppt.model.Slide.PAGE_WIDTH &&
               worldPoint.y >= 0 && worldPoint.y <= com.myppt.model.Slide.PAGE_HEIGHT;
    }

    // --- Getters and Setters ---
    
    public Presentation getPresentation() { return presentation; }
    public void setPresentation(Presentation presentation) { this.presentation = presentation; }
    public MainFrame getMainFrame() { return mainFrame; }
    public UndoManager getUndoManager() { return undoManager; }
    public FileHandler getFileHandler() { return fileHandler; }
    public ActionHandler getActionHandler() { return actionHandler; }
    public UIUpdater getUiUpdater() { return uiUpdater; }
    public AutosaveManager getAutosaveManager() { return autosaveManager; }
    
    public String getCurrentMode() { return currentMode; }
    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = scale; }
    public AbstractSlideObject getSelectedObject() { return selectedObject; }
    public void setSelectedObject(AbstractSlideObject object) { 
        if (this.selectedObject != null) {
            this.selectedObject.setSelected(false);
        }
        this.selectedObject = object; 
        if (this.selectedObject != null) {
            this.selectedObject.setSelected(true);
        }
    }
    public InteractionStrategy getCurrentStrategy() { return currentStrategy; }
    public Style getCopiedStyle() { return copiedStyle; }
    public void setCopiedStyle(Style style) { this.copiedStyle = style; }
    public AbstractSlideObject getClipboardObject() { return clipboardObject; }
    public void setClipboardObject(AbstractSlideObject object) { this.clipboardObject = object; }
    public int getPasteOffset() { return pasteOffset; }
    public void setPasteOffset(int offset) { this.pasteOffset = offset; }


    // --- 委托方法 (DELEGATION METHODS) ---
    // 这些方法允许外部类（如策略类和画布控制器）与 AppController 交互，
    // 而无需了解内部的处理器（如 FileHandler, UIUpdater）。

    /**
     * 将演示文稿标记为已修改（“脏”状态）。
     * 委托给 FileHandler.markAsDirty()。
     */
    public void markAsDirty() {
        fileHandler.markAsDirty();
    }

    /**
     * 重绘缩略图面板。
     * 委托给 UIUpdater.repaintThumbnails()。
     */
    public void repaintThumbnails() {
        uiUpdater.repaintThumbnails();
    }

    /**
     * 更新整个用户界面。
     * 委托给 UIUpdater.updateUI()。
     */
    public void updateUI() {
        uiUpdater.updateUI();
    }

    /**
     * 根据当前选中项更新属性面板。
     * 委托给 UIUpdater.updatePropertiesPanel()。
     */
    public void updatePropertiesPanel() {
        uiUpdater.updatePropertiesPanel();
    }
}