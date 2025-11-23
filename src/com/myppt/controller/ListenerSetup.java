package com.myppt.controller;

import com.myppt.commands.*;
import com.myppt.model.*;
import com.myppt.view.MainFrame;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * 负责为所有UI组件附加事件监听器。
 */
public class ListenerSetup {
    private final AppController controller;
    private final MainFrame mainFrame;
    private final ActionHandler actionHandler;
    private final FileHandler fileHandler;
    private final UIUpdater uiUpdater;
    private final UndoManager undoManager;

    public ListenerSetup(AppController controller) {
        this.controller = controller;
        this.mainFrame = controller.getMainFrame();
        this.actionHandler = controller.getActionHandler();
        this.fileHandler = controller.getFileHandler();
        this.uiUpdater = controller.getUiUpdater();
        this.undoManager = controller.getUndoManager();
    }

    public void attachAllListeners() {
        attachFrameListeners();
        attachCanvasListeners();
        attachButtonAndComponentListeners();
        attachMenuListeners();
        attachMouseWheelListener();
        attachKeyBindings();
    }
    
    private void attachFrameListeners() {
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (fileHandler.promptToSave()) {
                    controller.getAutosaveManager().deleteFile();
                    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                } else {
                    mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                }
            }
        });

        mainFrame.addComponentListener(new ComponentAdapter() {
            private boolean isFirstTime = true;
            @Override
            public void componentShown(ComponentEvent e) {
                if (isFirstTime) {
                    controller.fitToWindow();
                    isFirstTime = false;
                }
            }
        });
    }
    
    private void attachCanvasListeners() {
        CanvasController canvasController = new CanvasController(controller);
        mainFrame.getCanvasPanel().addMouseListener(canvasController);
        mainFrame.getCanvasPanel().addMouseMotionListener(canvasController);
    }

    private void attachButtonAndComponentListeners() {
        mainFrame.getAddRectButton().addActionListener(e -> controller.setMode("DRAW_RECT"));
        mainFrame.getAddEllipseButton().addActionListener(e -> controller.setMode("DRAW_ELLIPSE"));
        mainFrame.getAddLineButton().addActionListener(e -> controller.setMode("DRAW_LINE"));
        mainFrame.getAddTextButton().addActionListener(e -> controller.setMode("DRAW_TEXT"));
        mainFrame.getResetViewButton().addActionListener(e -> controller.fitToWindow());
        mainFrame.getAddImageButton().addActionListener(e -> actionHandler.insertImage());
        mainFrame.getPlayFromStartButton().addActionListener(e -> actionHandler.playPresentation(true));
        mainFrame.getPlayButton().addActionListener(e -> actionHandler.playPresentation(false));

        mainFrame.getNewSlideButton().addActionListener(e -> {
            fileHandler.markAsDirty();
            controller.getPresentation().addNewSlide();
            uiUpdater.updateUI();
        });
        mainFrame.getDeleteSlideButton().addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(mainFrame, "确定要删除当前页面吗?", "确认删除",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                fileHandler.markAsDirty();
                controller.getPresentation().removeCurrentSlide();
                uiUpdater.updateUI();
            }
        });

        mainFrame.getFormatPainterButton().addActionListener(e -> {
            if (controller.getSelectedObject() != null) {
                controller.setCopiedStyle(controller.getSelectedObject().getStyle());
                controller.setMode("FORMAT_PAINTER");
                mainFrame.getCanvasPanel().setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }
        });
        
        attachPropertiesPanelListeners();
    }
    
    private void attachPropertiesPanelListeners() {
        mainFrame.getChangeColorButton().addActionListener(e -> actionHandler.changeSelectedObjectColor());
        mainFrame.getEditTextButton().addActionListener(e -> actionHandler.editSelectedText());

        // Font controls
        Runnable updateFontAction = () -> {
            if (uiUpdater.isUpdatingUI() || !(controller.getSelectedObject() instanceof TextBox)) return;
            TextBox tb = (TextBox) controller.getSelectedObject();
            Font oldFont = tb.getFont();
            String name = (String) mainFrame.getFontNameBox().getSelectedItem();
            int size = (Integer) mainFrame.getFontSizeSpinner().getValue();
            int style = Font.PLAIN;
            if (mainFrame.getBoldCheckBox().isSelected()) style |= Font.BOLD;
            if (mainFrame.getItalicCheckBox().isSelected()) style |= Font.ITALIC;
            Font newFont = new Font(name, style, size);
            if (!newFont.equals(oldFont)) {
                undoManager.executeCommand(new ChangeFontCommand(tb, newFont));
                fileHandler.markAsDirty();
                uiUpdater.updateUI();
            }
        };
        mainFrame.getFontNameBox().addActionListener(e -> updateFontAction.run());
        mainFrame.getFontSizeSpinner().addChangeListener(e -> updateFontAction.run());
        mainFrame.getBoldCheckBox().addActionListener(e -> updateFontAction.run());
        mainFrame.getItalicCheckBox().addActionListener(e -> updateFontAction.run());

        // --- Border controls ---
        mainFrame.getBorderColorButton().addActionListener(e -> {
            if (uiUpdater.isUpdatingUI()) return;
            AbstractSlideObject selected = controller.getSelectedObject();
            if (selected == null || !(selected instanceof RectangleShape || selected instanceof EllipseShape || selected instanceof LineShape)) return;
            
            Color currentColor = Color.BLACK;
            double currentWidth = 1.0;
            int currentStyle = AbstractSlideObject.BORDER_STYLE_SOLID;

            if (selected instanceof RectangleShape) {
                currentColor = ((RectangleShape) selected).getBorderColor();
                currentWidth = ((RectangleShape) selected).getBorderWidth();
                currentStyle = ((RectangleShape) selected).getBorderStyle();
            } else if (selected instanceof EllipseShape) {
                currentColor = ((EllipseShape) selected).getBorderColor();
                currentWidth = ((EllipseShape) selected).getBorderWidth();
                currentStyle = ((EllipseShape) selected).getBorderStyle();
            } else if (selected instanceof LineShape) {
                currentColor = ((LineShape) selected).getLineColor();
                currentWidth = ((LineShape) selected).getStrokeWidth();
                currentStyle = ((LineShape) selected).getBorderStyle();
            }

            Color newColor = JColorChooser.showDialog(mainFrame, "选择边框颜色", currentColor);
            if (newColor != null && !newColor.equals(currentColor)) {
                Command command = new ChangeBorderCommand(selected, newColor, currentWidth, currentStyle);
                undoManager.executeCommand(command);
                fileHandler.markAsDirty();
                uiUpdater.updateUI();
            }
        });
        
        // 1. ChangeListener: 负责实时更新模型和视图 (当数值改变时立即触发)
        mainFrame.getBorderWidthSpinner().addChangeListener(e -> {
            if (uiUpdater.isUpdatingUI() || controller.getSelectedObject() == null) return;
            double newWidth = ((Number)mainFrame.getBorderWidthSpinner().getValue()).doubleValue();
            AbstractSlideObject selected = controller.getSelectedObject();
            if (selected instanceof RectangleShape) ((RectangleShape)selected).setBorderWidth(newWidth);
            else if (selected instanceof EllipseShape) ((EllipseShape)selected).setBorderWidth(newWidth);
            else if (selected instanceof LineShape) ((LineShape)selected).setStrokeWidth((float)newWidth);
            
            fileHandler.markAsDirty();
            mainFrame.getCanvasPanel().repaint();
            uiUpdater.repaintThumbnails();
            uiUpdater.updatePropertiesPanel(); 
        });

        // 2. Recursive MouseListener: 负责处理【点击箭头按钮】的撤销逻辑
        MouseAdapter spinnerMouseAdapter = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { 
                if (!uiUpdater.isUpdatingUI() && controller.getSelectedObject() != null) { 
                    actionHandler.setSpinnerDragging(true); 
                    actionHandler.recordBorderWidthBeforeChange(); 
                }
            }
            @Override public void mouseReleased(MouseEvent e) { 
                if (actionHandler.isSpinnerDragging()) { 
                    actionHandler.setSpinnerDragging(false); 
                    actionHandler.commitBorderWidthChange(); 
                }
            }
        };
        attachRecursiveMouseListener(mainFrame.getBorderWidthSpinner(), spinnerMouseAdapter);

        // [!] 3. 新增: 处理文本框【直接输入回车】的撤销逻辑
        JSpinner spinner = mainFrame.getBorderWidthSpinner();
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            
            // 当光标进入文本框时，记录旧值
            tf.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (!uiUpdater.isUpdatingUI() && controller.getSelectedObject() != null) {
                        actionHandler.recordBorderWidthBeforeChange();
                    }
                }
            });

            // 当按下回车键时，提交修改 (创建命令)
            tf.addActionListener(e -> {
                if (!uiUpdater.isUpdatingUI() && controller.getSelectedObject() != null) {
                    actionHandler.commitBorderWidthChange();
                    // 重新聚焦画布，方便后续快捷键操作
                    mainFrame.getCanvasPanel().requestFocusInWindow(); 
                }
            });
        }

        mainFrame.getBorderStyleBox().addActionListener(e -> {
            if (uiUpdater.isUpdatingUI() || controller.getSelectedObject() == null) return;
            AbstractSlideObject selected = controller.getSelectedObject();
            if (!(selected instanceof RectangleShape || selected instanceof EllipseShape || selected instanceof LineShape)) return;

            int selectedIndex = mainFrame.getBorderStyleBox().getSelectedIndex();
            Color currentColor = Color.BLACK;
            double currentWidth = 1.0;
            int currentStyle = AbstractSlideObject.BORDER_STYLE_SOLID;

            if (selected instanceof RectangleShape) {
                currentColor = ((RectangleShape) selected).getBorderColor();
                currentWidth = ((RectangleShape) selected).getBorderWidth();
                currentStyle = ((RectangleShape) selected).getBorderStyle();
            } else if (selected instanceof EllipseShape) {
                currentColor = ((EllipseShape) selected).getBorderColor();
                currentWidth = ((EllipseShape) selected).getBorderWidth();
                currentStyle = ((EllipseShape) selected).getBorderStyle();
            } else if (selected instanceof LineShape) {
                currentColor = ((LineShape) selected).getLineColor();
                currentWidth = ((LineShape) selected).getStrokeWidth();
                currentStyle = ((LineShape) selected).getBorderStyle();
            }
            
            double newWidth = currentWidth;
            int newStyle = selectedIndex;

            if (selectedIndex == 3) { // "无边框" option
                newWidth = 0;
                newStyle = currentStyle; 
            } else {
                if (currentWidth == 0) newWidth = 1.0; 
                newStyle = selectedIndex;
            }

            if (newWidth != currentWidth || newStyle != currentStyle) {
                Command command = new ChangeBorderCommand(selected, currentColor, newWidth, newStyle);
                undoManager.executeCommand(command);
                fileHandler.markAsDirty();
                uiUpdater.updateUI();
            }
        });
        
        mainFrame.getOpacitySlider().addChangeListener(e -> {
            if (uiUpdater.isUpdatingUI() || !(controller.getSelectedObject() instanceof ImageObject)) return;
            ImageObject img = (ImageObject) controller.getSelectedObject();
            float newOpacity = (float)mainFrame.getOpacitySlider().getValue() / 100.0f;
            img.setOpacity(newOpacity);
            fileHandler.markAsDirty();
            mainFrame.getCanvasPanel().repaint();
            uiUpdater.repaintThumbnails();
        });

        mainFrame.getOpacitySlider().addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { if (!uiUpdater.isUpdatingUI() && controller.getSelectedObject() instanceof ImageObject) { actionHandler.setSpinnerDragging(true); actionHandler.recordOpacityBeforeChange(); }}
            @Override public void mouseReleased(MouseEvent e) { if (actionHandler.isSpinnerDragging()) { actionHandler.setSpinnerDragging(false); actionHandler.commitOpacityChange(); }}
        });
    }

    private void attachRecursiveMouseListener(Component component, MouseListener listener) {
        component.addMouseListener(listener);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                attachRecursiveMouseListener(child, listener);
            }
        }
    }

    private void attachMenuListeners() {
        mainFrame.getNewMenuItem().addActionListener(e -> fileHandler.newFile());
        mainFrame.getOpenMenuItem().addActionListener(e -> fileHandler.openFromFile());
        mainFrame.getSaveMenuItem().addActionListener(e -> fileHandler.saveToFile());
        mainFrame.getSaveAsMenuItem().addActionListener(e -> fileHandler.saveAsToFile());
        mainFrame.getExportPdfMenuItem().addActionListener(e -> fileHandler.exportToPdf());
        mainFrame.getUndoMenuItem().addActionListener(e -> { undoManager.undo(); uiUpdater.updateUI(); });
        mainFrame.getRedoMenuItem().addActionListener(e -> { undoManager.redo(); uiUpdater.updateUI(); });
        mainFrame.getHelpMenuItem().addActionListener(e -> actionHandler.showHelpDialog());
        mainFrame.getGithubMenuItem().addActionListener(e -> actionHandler.openGitHub());
    }

    private void attachMouseWheelListener() {
        JScrollPane scrollPane = mainFrame.getCanvasScrollPane();
        scrollPane.addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
                if ((e.getModifiersEx() & shortcutMask) != 0) { // Zoom
                    double oldScale = controller.getScale();
                    double scaleChange = (e.getWheelRotation() < 0) ? 1.1 : 1.0 / 1.1;
                    double newScale = Math.max(0.1, Math.min(oldScale * scaleChange, 10.0));
                    
                    Point mousePoint = e.getPoint();
                    JViewport viewport = scrollPane.getViewport();
                    Point viewPos = viewport.getViewPosition();

                    double newViewX = (mousePoint.x / oldScale * newScale) - mousePoint.x + viewPos.x;
                    double newViewY = (mousePoint.y / oldScale * newScale) - mousePoint.y + viewPos.y;

                    mainFrame.getCanvasPanel().setScale(newScale);
                    controller.setScale(newScale); // Update controller's scale
                    SwingUtilities.invokeLater(() -> viewport.setViewPosition(new Point((int)newViewX, (int)newViewY)));
                } else if (e.isShiftDown()) { // Horizontal scroll
                    JScrollBar hBar = scrollPane.getHorizontalScrollBar();
                    hBar.setValue(hBar.getValue() + e.getUnitsToScroll() * hBar.getUnitIncrement());
                } else { // Vertical scroll
                    JScrollBar vBar = scrollPane.getVerticalScrollBar();
                    vBar.setValue(vBar.getValue() + e.getUnitsToScroll() * vBar.getUnitIncrement());
                }
            }
        });
    }

    private void attachKeyBindings() {
        JRootPane rootPane = mainFrame.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();
        int shortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        inputMap.put(KeyStroke.getKeyStroke("DELETE"), "delete");
        inputMap.put(KeyStroke.getKeyStroke("BACK_SPACE"), "delete");
        actionMap.put("delete", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { actionHandler.deleteSelectedObject(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut), "undo");
        actionMap.put("undo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { undoManager.undo(); uiUpdater.updateUI(); }
        });
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, shortcut), "redo");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut | InputEvent.SHIFT_DOWN_MASK), "redo");
        actionMap.put("redo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { undoManager.redo(); uiUpdater.updateUI(); }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcut), "copy");
        actionMap.put("copy", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { actionHandler.copySelectedObject(); }
        });
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, shortcut), "cut");
        actionMap.put("cut", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { actionHandler.cutSelectedObject(); }
        });
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcut), "paste");
        actionMap.put("paste", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { actionHandler.pasteObject(); }
        });

        inputMap.put(KeyStroke.getKeyStroke("F5"), "play");
        actionMap.put("play", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { actionHandler.playPresentation(true); }
        });
        
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
        actionMap.put("escape", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if ("FORMAT_PAINTER".equals(controller.getCurrentMode())) {
                    controller.setMode("SELECT");
                    mainFrame.getCanvasPanel().setCursor(Cursor.getDefaultCursor());
                    controller.setCopiedStyle(null);
                }
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcut), "save");
        actionMap.put("save", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                fileHandler.saveToFile();
            }
        });
    }
}