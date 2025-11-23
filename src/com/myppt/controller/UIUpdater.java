package com.myppt.controller;

import com.myppt.model.*;
import com.myppt.view.MainFrame;
import com.myppt.view.SlideThumbnail;
import com.myppt.view.ThumbnailPanel;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * UI更新器 (UIUpdater)
 * <p>
 * 职责：专门负责更新用户界面（UI）的所有逻辑，确保视图（View）与数据模型（Model）保持同步。
 * 它从 AppController 中剥离出来，体现了单一职责原则。
 */
public class UIUpdater {
    private final AppController controller;
    private final MainFrame mainFrame;
    
    /**
     * 标志位：指示当前是否正在通过代码程序化地更新UI控件。
     * <p>
     * 作用：当我们在代码中设置控件的值（如 spinner.setValue(...)）时，
     * 也会触发控件的 ChangeListener。这个标志位用于在监听器中判断
     * “这是用户的手动操作”还是“这是程序的自动更新”，从而防止逻辑递归或死循环。
     */
    private boolean isUpdatingUI = false; 

    public UIUpdater(AppController controller) {
        this.controller = controller;
        this.mainFrame = controller.getMainFrame();
    }
    
    /**
     * 获取当前是否正在程序化更新UI的状态。
     * 被 ListenerSetup 中的监听器调用以过滤事件。
     */
    public boolean isUpdatingUI() {
        return isUpdatingUI;
    }

    /**
     * 刷新整个用户界面的入口方法。
     * 依次更新缩略图、画布重绘、属性面板和菜单状态。
     */
    public void updateUI() {
        updateThumbnailList();
        mainFrame.getCanvasPanel().repaint();
        updatePropertiesPanel();
        updateMenuState();
    }

    /**
     * 重建左侧的幻灯片缩略图列表。
     * 当添加、删除页面或重新排序时调用。
     */
    public void updateThumbnailList() {
        ThumbnailPanel panel = mainFrame.getThumbnailPanel();
        panel.removeAll(); // 清空旧的缩略图
        
        Presentation presentation = controller.getPresentation();
        List<Slide> slides = presentation.getSlides();
        
        // 遍历所有幻灯片并创建缩略图组件
        for (int i = 0; i < slides.size(); i++) {
            SlideThumbnail thumb = new SlideThumbnail(slides.get(i), i + 1);
            final int index = i;
            
            // 为每个缩略图添加点击事件，用于切换页面
            thumb.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (presentation.getCurrentSlideIndex() != index) {
                        presentation.setCurrentSlideIndex(index);
                        mainFrame.getCanvasPanel().repaint(); // 切换页面后必须重绘画布
                        updateUI(); // 更新整个UI以反映新页面的状态
                    }
                }
            });
            
            // 高亮显示当前选中的页面
            if (i == presentation.getCurrentSlideIndex()) {
                thumb.setSelected(true);
            }
            panel.add(thumb);
        }
        
        // 刷新面板布局
        panel.revalidate();
        panel.repaint();
    }
    
    /**
     * 轻量级重绘缩略图面板。
     * 仅触发 repaint，不重新创建组件列表。适用于当页面内容微调时（如改颜色）同步缩略图外观。
     */
    public void repaintThumbnails() {
        if (mainFrame != null && mainFrame.getThumbnailPanel() != null) {
            mainFrame.getThumbnailPanel().repaint();
        }
    }

    /**
     * 更新主窗口的标题栏。
     * 显示当前文件名，如果文件有未保存的修改，则在标题后追加 "*" 号。
     */
    public void updateTitle() {
        String title = "My PowerPoint - ";
        FileHandler fileHandler = controller.getFileHandler();
        
        // 拼接文件名
        if (fileHandler.getCurrentFile() == null) {
            title += "未命名文件";
        } else {
            title += fileHandler.getCurrentFile().getName();
        }
        
        // 检查“脏”标记（是否已修改）
        if (fileHandler.isDirty()) {
            title += "*";
        }
        mainFrame.setTitle(title);
    }
    
    /**
     * 更新菜单栏和工具栏按钮的状态（启用/禁用）。
     * 主要根据撤销/重做栈的状态来决定“撤销”和“重做”按钮是否可用。
     */
    public void updateMenuState() {
        UndoManager undoManager = controller.getUndoManager();
        mainFrame.getUndoMenuItem().setEnabled(undoManager.canUndo());
        mainFrame.getRedoMenuItem().setEnabled(undoManager.canRedo());
    }

    /**
     * [核心方法] 根据当前选中的对象类型，动态更新右侧属性面板的内容。
     * 负责显隐对应的属性组（文本、边框、图片），并将模型的值回填到控件中。
     */
    public void updatePropertiesPanel() {
        isUpdatingUI = true; // [!] 开始更新，锁定标志位，防止触发监听器
        try {
            AbstractSlideObject selected = controller.getSelectedObject();
            
            // 判断选中对象的具体类型
            boolean isTextSelected = selected instanceof TextBox;
            boolean hasBorder = selected instanceof RectangleShape || selected instanceof EllipseShape || selected instanceof LineShape;
            boolean isImageSelected = selected instanceof ImageObject;

            // --- 1. 更新面板的可见性 (Visibility) ---
            mainFrame.getTextStyleGroupPanel().setVisible(isTextSelected);
            mainFrame.getBorderStyleGroupPanel().setVisible(hasBorder);
            mainFrame.getOpacityGroupPanel().setVisible(isImageSelected);

            // --- 2. 更新控件的启用状态 (Enabled) ---
            mainFrame.getChangeColorButton().setEnabled(selected != null); 
            mainFrame.getEditTextButton().setEnabled(isTextSelected);
            mainFrame.getFontNameBox().setEnabled(isTextSelected);
            mainFrame.getFontSizeSpinner().setEnabled(isTextSelected);
            mainFrame.getBoldCheckBox().setEnabled(isTextSelected);
            mainFrame.getItalicCheckBox().setEnabled(isTextSelected);
            mainFrame.getBorderColorButton().setEnabled(hasBorder);
            mainFrame.getBorderWidthSpinner().setEnabled(hasBorder);
            mainFrame.getBorderStyleBox().setEnabled(hasBorder);
            mainFrame.getOpacitySlider().setEnabled(isImageSelected);
            
            // --- 3. 将模型数据回填到控件 (Data Population) ---
            if (selected != null) {
                // 设置颜色按钮的动态文本
                if (selected instanceof LineShape) mainFrame.getChangeColorButton().setText("更改线条颜色");
                else if (isTextSelected) mainFrame.getChangeColorButton().setText("更改文字颜色");
                else mainFrame.getChangeColorButton().setText("更改填充颜色");
                
                // 回填文本属性
                if (isTextSelected) {
                    TextBox tb = (TextBox) selected;
                    Font f = tb.getFont();
                    mainFrame.getFontNameBox().setSelectedItem(f.getFamily());
                    mainFrame.getFontSizeSpinner().setValue(f.getSize());
                    mainFrame.getBoldCheckBox().setSelected(f.isBold());
                    mainFrame.getItalicCheckBox().setSelected(f.isItalic());
                }

                // 回填边框/线条属性
                if (hasBorder) {
                    double borderWidth = 0;
                    int borderStyle = 0;
                    
                    // 分别从不同类型的对象中获取边框属性
                    if (selected instanceof RectangleShape) {
                        RectangleShape rect = (RectangleShape) selected;
                        borderWidth = rect.getBorderWidth();
                        borderStyle = rect.getBorderStyle();
                    } else if (selected instanceof EllipseShape) {
                        EllipseShape ellipse = (EllipseShape) selected;
                        borderWidth = ellipse.getBorderWidth();
                        borderStyle = ellipse.getBorderStyle();
                    } else if (selected instanceof LineShape) {
                        LineShape line = (LineShape) selected;
                        borderWidth = line.getStrokeWidth();
                        borderStyle = line.getBorderStyle();
                    }
                    
                    mainFrame.getBorderWidthSpinner().setValue(borderWidth);
                    // 如果宽度为0，下拉框显示为“无边框”(假设索引3是无边框)
                    if (borderWidth == 0) {
                        mainFrame.getBorderStyleBox().setSelectedIndex(3); 
                    } else {
                        mainFrame.getBorderStyleBox().setSelectedIndex(borderStyle);
                    }
                }

                // 回填图片透明度属性
                if (isImageSelected) {
                    ImageObject img = (ImageObject) selected;
                    // 将 0.0-1.0 的浮点数转换为 0-100 的整数用于滑块显示
                    mainFrame.getOpacitySlider().setValue((int)(img.getOpacity() * 100));
                }
            }
        } finally {
            isUpdatingUI = false; // [!] 更新结束，释放标志位，恢复监听器响应
        }
    }
}