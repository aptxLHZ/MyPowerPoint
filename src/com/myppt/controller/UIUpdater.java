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
 * 负责更新所有UI组件的状态，使其与模型保持同步。
 */
public class UIUpdater {
    private final AppController controller;
    private final MainFrame mainFrame;
    private boolean isUpdatingUI = false; // Prevents recursive updates

    public UIUpdater(AppController controller) {
        this.controller = controller;
        this.mainFrame = controller.getMainFrame();
    }
    
    public boolean isUpdatingUI() {
        return isUpdatingUI;
    }

    public void updateUI() {
        updateThumbnailList();
        mainFrame.getCanvasPanel().repaint();
        updatePropertiesPanel();
        updateMenuState();
    }

    public void updateThumbnailList() {
        ThumbnailPanel panel = mainFrame.getThumbnailPanel();
        panel.removeAll();
        Presentation presentation = controller.getPresentation();
        List<Slide> slides = presentation.getSlides();
        for (int i = 0; i < slides.size(); i++) {
            SlideThumbnail thumb = new SlideThumbnail(slides.get(i), i + 1);
            final int index = i;
            thumb.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (presentation.getCurrentSlideIndex() != index) {
                        presentation.setCurrentSlideIndex(index);
                        mainFrame.getCanvasPanel().repaint(); // Must repaint canvas on slide change
                        updateUI();
                    }
                }
            });
            if (i == presentation.getCurrentSlideIndex()) {
                thumb.setSelected(true);
            }
            panel.add(thumb);
        }
        panel.revalidate();
        panel.repaint();
    }
    
    public void repaintThumbnails() {
        if (mainFrame != null && mainFrame.getThumbnailPanel() != null) {
            mainFrame.getThumbnailPanel().repaint();
        }
    }

    public void updateTitle() {
        String title = "My PowerPoint - ";
        FileHandler fileHandler = controller.getFileHandler();
        if (fileHandler.getCurrentFile() == null) {
            title += "未命名文件";
        } else {
            title += fileHandler.getCurrentFile().getName();
        }
        if (fileHandler.isDirty()) {
            title += "*";
        }
        mainFrame.setTitle(title);
    }
    
    public void updateMenuState() {
        UndoManager undoManager = controller.getUndoManager();
        mainFrame.getUndoMenuItem().setEnabled(undoManager.canUndo());
        mainFrame.getRedoMenuItem().setEnabled(undoManager.canRedo());
    }

    public void updatePropertiesPanel() {
        isUpdatingUI = true;
        try {
            AbstractSlideObject selected = controller.getSelectedObject();
            boolean isTextSelected = selected instanceof TextBox;
            // [FIX] Replaced `instanceof Shape` with original logic
            boolean hasBorder = selected instanceof RectangleShape || selected instanceof EllipseShape || selected instanceof LineShape;
            boolean isImageSelected = selected instanceof ImageObject;

            mainFrame.getTextStyleGroupPanel().setVisible(isTextSelected);
            mainFrame.getBorderStyleGroupPanel().setVisible(hasBorder);
            mainFrame.getOpacityGroupPanel().setVisible(isImageSelected);

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
            
            if (selected != null) {
                if (selected instanceof LineShape) mainFrame.getChangeColorButton().setText("更改线条颜色");
                else if (isTextSelected) mainFrame.getChangeColorButton().setText("更改文字颜色");
                else mainFrame.getChangeColorButton().setText("更改填充颜色");
                
                if (isTextSelected) {
                    TextBox tb = (TextBox) selected;
                    Font f = tb.getFont();
                    mainFrame.getFontNameBox().setSelectedItem(f.getFamily());
                    mainFrame.getFontSizeSpinner().setValue(f.getSize());
                    mainFrame.getBoldCheckBox().setSelected(f.isBold());
                    mainFrame.getItalicCheckBox().setSelected(f.isItalic());
                }

                if (hasBorder) {
                    // [FIX] Replaced generic property access with specific checks
                    double borderWidth = 0;
                    int borderStyle = 0;
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
                    if (borderWidth == 0) {
                        mainFrame.getBorderStyleBox().setSelectedIndex(3); // "无边框" option
                    } else {
                        mainFrame.getBorderStyleBox().setSelectedIndex(borderStyle);
                    }
                }

                if (isImageSelected) {
                    ImageObject img = (ImageObject) selected;
                    mainFrame.getOpacitySlider().setValue((int)(img.getOpacity() * 100));
                }
            }
        } finally {
            isUpdatingUI = false;
        }
    }
}