package com.myppt.controller;

import com.myppt.commands.*;
import com.myppt.model.*;
import com.myppt.view.PlayerFrame;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.*;

/**
 * 负责处理所有具体的UI动作逻辑，如修改对象属性、复制粘贴、播放等。
 */
public class ActionHandler {
    private final AppController controller;
    private double borderWidthBeforeChange;
    private float opacityBeforeChange;
    private boolean isSpinnerDragging = false; // 记录Spinner是否正在被鼠标拖动

    public ActionHandler(AppController controller) {
        this.controller = controller;
    }
    
    public boolean isSpinnerDragging() { return isSpinnerDragging; }
    public void setSpinnerDragging(boolean dragging) { this.isSpinnerDragging = dragging; }

    public void deleteSelectedObject() {
        AbstractSlideObject selected = controller.getSelectedObject();
        if (selected != null) {
            controller.getFileHandler().markAsDirty();
            Command command = new DeleteObjectCommand(controller.getPresentation().getCurrentSlide(), selected);
            controller.getUndoManager().executeCommand(command);
            controller.setSelectedObject(null);
            controller.getUiUpdater().updateUI();
        }
    }

    public void changeSelectedObjectColor() {
        AbstractSlideObject selected = controller.getSelectedObject();
        if (selected == null) return;

        Color currentColor = Color.BLACK;
        if (selected instanceof RectangleShape) currentColor = ((RectangleShape) selected).getFillColor();
        else if (selected instanceof EllipseShape) currentColor = ((EllipseShape) selected).getFillColor();
        else if (selected instanceof LineShape) currentColor = ((LineShape) selected).getLineColor();
        else if (selected instanceof TextBox) currentColor = ((TextBox) selected).getTextColor();
        
        Color newColor = JColorChooser.showDialog(controller.getMainFrame(), "选择颜色", currentColor);

        if (newColor != null && !newColor.equals(currentColor)) {
            Command command = new ChangeColorCommand(selected, newColor);
            controller.getUndoManager().executeCommand(command);
            controller.getFileHandler().markAsDirty();
            controller.getMainFrame().getCanvasPanel().repaint();
            controller.getUiUpdater().repaintThumbnails();
        }
    }

    public void editSelectedText() {
        if (controller.getSelectedObject() instanceof TextBox) {
            TextBox selectedTextBox = (TextBox) controller.getSelectedObject();
            JTextArea textArea = new JTextArea(selectedTextBox.getText(), 5, 20);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            int result = JOptionPane.showConfirmDialog(controller.getMainFrame(), new JScrollPane(textArea),
                    "修改文本内容", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String newText = textArea.getText();
                if (newText != null && !newText.equals(selectedTextBox.getText())) {
                    Command command = new ChangeTextCommand(selectedTextBox, newText);
                    controller.getUndoManager().executeCommand(command);
                    controller.getFileHandler().markAsDirty();
                    controller.getUiUpdater().updateUI();
                }
            }
        }
    }

    public void insertImage() {
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

        if (fileChooser.showOpenDialog(controller.getMainFrame()) == JFileChooser.APPROVE_OPTION) {
            try {
                byte[] imageData = java.nio.file.Files.readAllBytes(fileChooser.getSelectedFile().toPath());
                ImageObject imageObj = new ImageObject(100, 100, imageData);
                Command command = new AddObjectCommand(controller.getPresentation().getCurrentSlide(), imageObj);
                controller.getUndoManager().executeCommand(command);
                controller.getFileHandler().markAsDirty();
                controller.getUiUpdater().updateUI();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(controller.getMainFrame(), "无法读取图片文件: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void playPresentation(boolean fromStart) {
        Presentation presentation = controller.getPresentation();
        if (fromStart) {
            presentation.setCurrentSlideIndex(0);
            controller.getUiUpdater().updateUI();
        }

        controller.getMainFrame().setVisible(false);
        PlayerFrame player = new PlayerFrame(presentation);
        player.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                controller.getMainFrame().setVisible(true);
                controller.getMainFrame().toFront();
                controller.getMainFrame().requestFocus();
            }
        });
        player.start();
    }
    
    public void copySelectedObject() {
        if (controller.getSelectedObject() != null) {
            controller.setClipboardObject(controller.getSelectedObject().deepCopy());
            controller.setPasteOffset(0);
            System.out.println("Object copied to internal clipboard.");
        }
    }

    public void cutSelectedObject() {
        if (controller.getSelectedObject() != null) {
            copySelectedObject();
            deleteSelectedObject();
            System.out.println("Object cut to internal clipboard.");
        }
    }

    public void pasteObject() {
        if (controller.getClipboardObject() != null) {
            AbstractSlideObject pastedObject = controller.getClipboardObject().deepCopy();
            int offset = 20;
            int currentOffset = controller.getPasteOffset();
            pastedObject.setX(pastedObject.getX() + currentOffset + offset);
            pastedObject.setY(pastedObject.getY() + currentOffset + offset);
            controller.setPasteOffset(currentOffset + offset);

            if (pastedObject.getX() > (Slide.PAGE_WIDTH - 50) || pastedObject.getY() > (Slide.PAGE_HEIGHT - 50)) {
                controller.setPasteOffset(0);
            }

            Command command = new AddObjectCommand(controller.getPresentation().getCurrentSlide(), pastedObject);
            controller.getUndoManager().executeCommand(command);
            controller.getFileHandler().markAsDirty();
            controller.setSelectedObject(pastedObject);
            controller.getUiUpdater().updateUI();
            System.out.println("Object pasted from internal clipboard.");
        }
    }

    public void showHelpDialog() {
         StringBuilder helpHtml = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/main/resources/help.html"), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                helpHtml.append(line).append("\n");
            }
        } catch (Exception e) { 
            helpHtml.append("<html><body><b>Error: Could not load help file.</b></body></html>");
        }

        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setText(helpHtml.toString());
        textPane.setEditable(false);
        textPane.setFont(new Font("Dialog", Font.PLAIN, 10));

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setPreferredSize(new Dimension(650, 500));
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
        
        JOptionPane.showMessageDialog(controller.getMainFrame(), scrollPane, "MyPPT 使用说明", JOptionPane.INFORMATION_MESSAGE);
    }

    public void openGitHub() {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/aptxLHZ/MyPowerPoint"));
        } catch (IOException | URISyntaxException ex) {
            JOptionPane.showMessageDialog(controller.getMainFrame(), "无法打开浏览器，请手动访问 GitHub 链接。", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // [调试] 记录修改前的边框宽度
    public void recordBorderWidthBeforeChange() {
        AbstractSlideObject selected = controller.getSelectedObject();
        if (selected == null) return;
        
        if (selected instanceof RectangleShape) {
            borderWidthBeforeChange = ((RectangleShape) selected).getBorderWidth();
        } else if (selected instanceof EllipseShape) {
            borderWidthBeforeChange = ((EllipseShape) selected).getBorderWidth();
        } else if (selected instanceof LineShape) {
            borderWidthBeforeChange = ((LineShape) selected).getStrokeWidth();
        }
        System.out.println("【调试】ActionHandler: 记录下边框的'修改前'宽度为: " + borderWidthBeforeChange);
    }

    // [调试] 提交边框宽度的修改
    public void commitBorderWidthChange() {
        AbstractSlideObject selected = controller.getSelectedObject();
        if (selected == null) return;
        
        double newValue = ((Number)controller.getMainFrame().getBorderWidthSpinner().getValue()).doubleValue();
        
        System.out.println("【调试】准备提交宽度修改: 新值=" + newValue + ", 旧值=" + borderWidthBeforeChange);

        if (newValue != borderWidthBeforeChange) {
            // [关键] 为了创建正确的撤销命令，必须先将对象恢复到"旧状态"
            if (selected instanceof RectangleShape) ((RectangleShape) selected).setBorderWidth(borderWidthBeforeChange);
            else if (selected instanceof EllipseShape) ((EllipseShape) selected).setBorderWidth(borderWidthBeforeChange);
            else if (selected instanceof LineShape) ((LineShape) selected).setStrokeWidth((float)borderWidthBeforeChange);
            
            Color currentColor = Color.BLACK;
            int currentStyle = AbstractSlideObject.BORDER_STYLE_SOLID;
            
            if (selected instanceof RectangleShape) {
                currentColor = ((RectangleShape) selected).getBorderColor();
                currentStyle = ((RectangleShape) selected).getBorderStyle();
            } else if (selected instanceof EllipseShape) {
                currentColor = ((EllipseShape) selected).getBorderColor();
                currentStyle = ((EllipseShape) selected).getBorderStyle();
            } else if (selected instanceof LineShape) {
                currentColor = ((LineShape) selected).getLineColor();
                currentStyle = ((LineShape) selected).getBorderStyle();
            }
            
            Command command = new ChangeBorderCommand(selected, currentColor, newValue, currentStyle);
            controller.getUndoManager().executeCommand(command);
            controller.getFileHandler().markAsDirty();
            
            // 更新一下记录值，防止重复提交
            borderWidthBeforeChange = newValue; 
        }
    }
    
    public void recordOpacityBeforeChange() {
        if (controller.getSelectedObject() instanceof ImageObject) {
            opacityBeforeChange = ((ImageObject) controller.getSelectedObject()).getOpacity();
        }
    }
    
    public void commitOpacityChange() {
        if (controller.getSelectedObject() instanceof ImageObject) {
            ImageObject target = (ImageObject) controller.getSelectedObject();
            float newOpacity = (float)controller.getMainFrame().getOpacitySlider().getValue() / 100.0f;
            if (newOpacity != opacityBeforeChange) {
                target.setOpacity(opacityBeforeChange); // Restore for command
                Command command = new ChangeOpacityCommand(target, newOpacity);
                controller.getUndoManager().executeCommand(command);
                controller.getFileHandler().markAsDirty();
            }
        }
    }
}