package com.myppt.controller;

import java.awt.Color;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.Font; 

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser; 
import java.io.File;             
import java.io.IOException;      

import com.myppt.model.AbstractSlideObject;
import com.myppt.model.EllipseShape;
import com.myppt.model.LineShape;
import com.myppt.model.Presentation;
import com.myppt.model.RectangleShape;
import com.myppt.model.Slide;
import com.myppt.view.CanvasPanel;
import com.myppt.view.MainFrame;
import com.myppt.model.TextBox;
import com.myppt.model.ImageObject; 

import java.util.List;

public class AppController {
    private Presentation presentation;
    private MainFrame mainFrame;
    private String currentMode = "SELECT";
    private double scale = 1.0;

    // --- 交互状态变量 ---
    private AbstractSlideObject selectedObject = null;
    private Point dragStartPoint = null;
    private Point objectStartPoint = null;
    private LineShape currentDrawingLine = null; // 用于处理直线绘制的临时对象

    public AppController(Presentation presentation, MainFrame mainFrame) {
        this.presentation = presentation;
        this.mainFrame = mainFrame;
        this.mainFrame.setupCanvas(presentation);
        this.attachListeners();
    }

    private void attachListeners() {
        // --- 窗口和按钮监听器 ---
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

        mainFrame.getAddRectButton().addActionListener(e -> {
            setMode("DRAW_RECT");
        });

        mainFrame.getAddEllipseButton().addActionListener(e -> {
            setMode("DRAW_ELLIPSE");
        });

        mainFrame.getAddLineButton().addActionListener(e -> {
            setMode("DRAW_LINE");
        });

        mainFrame.getResetViewButton().addActionListener(e -> {
            fitToWindow();
        });

        mainFrame.getChangeColorButton().addActionListener(e -> {
            changeSelectedObjectColor();
        });

        mainFrame.getAddTextButton().addActionListener(e -> {
            setMode("DRAW_TEXT");
        });

        mainFrame.getEditTextButton().addActionListener(e -> {
            if (selectedObject instanceof TextBox) {
                TextBox selectedTextBox = (TextBox) selectedObject;
                
                // 1. 创建JTextArea，并把当前文本框的内容预设进去
                JTextArea textArea = new JTextArea(selectedTextBox.getText(), 5, 20);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                JScrollPane scrollPane = new JScrollPane(textArea);

                // 2. 显示自定义对话框
                int result = JOptionPane.showConfirmDialog(
                    mainFrame, 
                    scrollPane, 
                    "修改文本内容", 
                    JOptionPane.OK_CANCEL_OPTION, 
                    JOptionPane.PLAIN_MESSAGE
                );

                // 3. 如果用户点击了 "OK"
                if (result == JOptionPane.OK_OPTION) {
                    String newText = textArea.getText();
                    // 注意：这里我们允许用户将文本清空，所以不做 notEmpty 检查
                    if (newText != null) {
                        selectedTextBox.setText(newText);
                        mainFrame.getCanvasPanel().repaint();
                    }
                }
            }
        });
        
        mainFrame.getAddImageButton().addActionListener(e -> {
            // 插入图片是一个独立的操作，不依赖于鼠标点击，所以不需要设置模式
            deselectAllObjects();

            // 1. 创建一个文件选择器
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("请选择一张图片");
            // 设置文件过滤器，只显示常见的图片格式
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(File f) {
                    if (f.isDirectory()) return true;
                    String name = f.getName().toLowerCase();
                    return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif");
                }
                public String getDescription() {
                    return "图片文件 (*.jpg, *.png, *.gif)";
                }
            });

            // 2. 显示对话框，并获取用户的选择结果
            int result = fileChooser.showOpenDialog(mainFrame);
            
            // 3. 如果用户选择了文件并点击了"打开"
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try {
                    // 4. 在页面中央创建一个新的ImageObject
                    // 为了简单，我们先把它放在页面的 (100, 100) 位置
                    ImageObject imageObj = new ImageObject(100, 100, selectedFile.getAbsolutePath());
                    presentation.getSlides().get(0).addObject(imageObj);
                    mainFrame.getCanvasPanel().repaint();
                } catch (IOException ex) {
                    // 5. 如果图片加载失败，给用户一个友好的提示
                    JOptionPane.showMessageDialog(mainFrame, "无法加载图片文件: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });        


        // --- 核心: 统一的鼠标事件处理器 ---
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point worldPoint = convertScreenToWorld(e.getPoint());

                if (!isPointInPage(worldPoint)) {
                    deselectAllObjects();
                    mainFrame.getCanvasPanel().repaint();
                    return;
                }

                switch (currentMode) {
                    case "SELECT":
                        handleSelectPressed(worldPoint);
                        break;
                    case "DRAW_LINE":
                        handleLineDrawPressed(worldPoint);
                        break;
                    case "DRAW_RECT":
                        handleRectDrawClicked(worldPoint);
                        break;
                    case "DRAW_ELLIPSE":
                        handleEllipseDrawClicked(worldPoint);
                        break;
                    case "DRAW_TEXT":
                        handleTextDrawClicked(worldPoint);
                        break;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point worldPoint = convertScreenToWorld(e.getPoint());

                switch (currentMode) {
                    case "SELECT":
                        handleSelectDragged(worldPoint);
                        break;
                    case "DRAW_LINE":
                        handleLineDrawDragged(worldPoint);
                        break;
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                switch (currentMode) {
                    case "SELECT":
                        handleSelectReleased();
                        break;
                    case "DRAW_LINE":
                        handleLineDrawReleased();
                        break;
                }
            }
        };

        mainFrame.getCanvasPanel().addMouseListener(mouseHandler);
        mainFrame.getCanvasPanel().addMouseMotionListener(mouseHandler);

        // --- 滚轮监听器 (不变) ---
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
                    
                    SwingUtilities.invokeLater(() -> {
                        scrollPane.getViewport().setViewPosition(new Point((int)newViewX, (int)newViewY));
                    });

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

    // --- 鼠标事件处理辅助方法 ---

    private void handleSelectPressed(Point worldPoint) {
    // 1. 先找出用户点击了哪个对象
    AbstractSlideObject clickedObject = findObjectAtPoint(worldPoint);

    // 2. 如果点击的对象不是当前已经选中的对象，那么就更新选择
    if (clickedObject != selectedObject) {
        deselectAllObjects(); // 先取消所有对象的选择
        selectedObject = clickedObject; // 然后将新点击的对象设为选中对象
        if (selectedObject != null) {
            selectedObject.setSelected(true);
        }
    }
    
    // 3. 无论如何，都为可能的拖动做准备
    if (selectedObject != null) {
        dragStartPoint = worldPoint;
        objectStartPoint = new Point(selectedObject.getX(), selectedObject.getY());
    }

    // 4. 更新UI
    updatePropertiesPanel();
    mainFrame.getCanvasPanel().repaint();
}

    private void handleSelectDragged(Point worldPoint) {
        if (selectedObject != null && dragStartPoint != null) {
            int dx = worldPoint.x - dragStartPoint.x;
            int dy = worldPoint.y - dragStartPoint.y;
            selectedObject.setX(objectStartPoint.x + dx);
            selectedObject.setY(objectStartPoint.y + dy);
            mainFrame.getCanvasPanel().repaint();
        }
    }

    private void handleSelectReleased() {
        dragStartPoint = null;
        objectStartPoint = null;
    }

    private void handleLineDrawPressed(Point worldPoint) {
        currentDrawingLine = new LineShape(worldPoint.x, worldPoint.y, worldPoint.x, worldPoint.y, Color.BLACK, 2f);
        presentation.getSlides().get(0).addObject(currentDrawingLine);
    }

    private void handleLineDrawDragged(Point worldPoint) {
        if (currentDrawingLine != null) {
            currentDrawingLine.x2 = worldPoint.x;
            currentDrawingLine.y2 = worldPoint.y;
            mainFrame.getCanvasPanel().repaint();
        }
    }

    private void handleLineDrawReleased() {
        currentDrawingLine = null;
        setMode("SELECT");
    }

    private void handleRectDrawClicked(Point worldPoint) {
        RectangleShape rect = new RectangleShape(worldPoint.x, worldPoint.y, 100, 60, Color.BLUE);
        presentation.getSlides().get(0).addObject(rect);
        mainFrame.getCanvasPanel().repaint();
        setMode("SELECT");
    }
    
    private void handleEllipseDrawClicked(Point worldPoint) {
        EllipseShape ellipse = new EllipseShape(worldPoint.x, worldPoint.y, 80, 80, Color.RED);
        presentation.getSlides().get(0).addObject(ellipse);
        mainFrame.getCanvasPanel().repaint();
        setMode("SELECT");
    }

    private void handleTextDrawClicked(Point worldPoint) {
        // 1. 创建一个JTextArea作为输入控件
        JTextArea textArea = new JTextArea(5, 20); // 5行20列
        textArea.setLineWrap(true); // 自动换行
        textArea.setWrapStyleWord(true); // 按单词换行
        
        // 2. 将JTextArea放入一个带滚动条的面板中，以防文字过多
        JScrollPane scrollPane = new JScrollPane(textArea);

        // 3. 使用JOptionPane来显示这个自定义组件
        int result = JOptionPane.showConfirmDialog(
            mainFrame, 
            scrollPane, // 把带滚动条的文本域放进去
            "请输入文字", 
            JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.PLAIN_MESSAGE
        );
        
        // 4. 检查用户是否点击了 "OK"
        if (result == JOptionPane.OK_OPTION) {
            String text = textArea.getText(); // 从JTextArea获取文本

            // 5. 检查用户是否输入了内容
            if (text != null && !text.trim().isEmpty()) {
                Font font = new Font("宋体", Font.PLAIN, 24);
                Color color = Color.BLACK;

                TextBox textBox = new TextBox(worldPoint.x, worldPoint.y, text, font, color);
                presentation.getSlides().get(0).addObject(textBox);
                mainFrame.getCanvasPanel().repaint();
            }
        }
        
        // 无论用户做什么，最后都回到选择模式
        setMode("SELECT");
    }

    // --- 业务逻辑辅助方法 ---

    private void setMode(String mode) {
        this.currentMode = mode;
        if (!mode.equals("SELECT")) {
            deselectAllObjects();
            mainFrame.getCanvasPanel().repaint();
        }
    }

    private void changeSelectedObjectColor() {
        if (selectedObject == null) return; // 如果没有选中对象，直接返回

        // 1. 根据选中对象的类型，获取其当前颜色
        Color currentColor = Color.BLACK; // 提供一个最终的默认值
        if (selectedObject instanceof RectangleShape) {
            currentColor = ((RectangleShape) selectedObject).getFillColor();
        } else if (selectedObject instanceof EllipseShape) {
            currentColor = ((EllipseShape) selectedObject).getFillColor();
        } else if (selectedObject instanceof LineShape) {
            currentColor = ((LineShape) selectedObject).getLineColor();
        } else if (selectedObject instanceof TextBox) {
            currentColor = ((TextBox) selectedObject).getTextColor();
        }

        // 2. 弹出颜色选择器，并将获取到的颜色设为默认值
        Color newColor = JColorChooser.showDialog(mainFrame, "选择颜色", currentColor);
        
        // 3. 如果用户选择了新颜色，则应用它
        if (newColor != null) {
            if (selectedObject instanceof RectangleShape) {
                ((RectangleShape) selectedObject).setFillColor(newColor);
            } else if (selectedObject instanceof EllipseShape) {
                ((EllipseShape) selectedObject).setFillColor(newColor);
            } else if (selectedObject instanceof LineShape) {
                ((LineShape) selectedObject).setLineColor(newColor);
            } else if (selectedObject instanceof TextBox) {
                ((TextBox) selectedObject).setTextColor(newColor);
            }
            mainFrame.getCanvasPanel().repaint();
        }
    }
    
    
    private void updatePropertiesPanel() {
        // 处理颜色按钮
        boolean enableColorButton = selectedObject != null &&
                                    (selectedObject instanceof RectangleShape || 
                                    selectedObject instanceof EllipseShape ||
                                    selectedObject instanceof LineShape ||
                                    selectedObject instanceof TextBox);
        
        JButton colorButton = mainFrame.getChangeColorButton();
        colorButton.setEnabled(enableColorButton);

        if (selectedObject instanceof LineShape) {
            colorButton.setText("更改线条颜色");
        } else if (selectedObject instanceof TextBox) {
            colorButton.setText("更改文字颜色");
        } else {
            colorButton.setText("更改填充颜色");
        }

        // [!] 新增: 处理“修改文本”按钮
        boolean enableTextButton = selectedObject != null && selectedObject instanceof TextBox;
        mainFrame.getEditTextButton().setEnabled(enableTextButton);
    }

    private Point convertScreenToWorld(Point screenPoint) {
        double logicCanvasX = screenPoint.x / scale;
        double logicCanvasY = screenPoint.y / scale;
        double pageStartX = (CanvasPanel.VIRTUAL_CANVAS_WIDTH - Slide.PAGE_WIDTH) / 2.0;
        double pageStartY = (CanvasPanel.VIRTUAL_CANVAS_HEIGHT - Slide.PAGE_HEIGHT) / 2.0;
        int finalWorldX = (int) (logicCanvasX - pageStartX);
        int finalWorldY = (int) (logicCanvasY - pageStartY);
        return new Point(finalWorldX, finalWorldY);
    }
    
    private boolean isPointInPage(Point worldPoint) {
        return worldPoint.x >= 0 && worldPoint.x <= Slide.PAGE_WIDTH &&
               worldPoint.y >= 0 && worldPoint.y <= Slide.PAGE_HEIGHT;
    }

    private AbstractSlideObject findObjectAtPoint(Point worldPoint) {
        Slide currentSlide = presentation.getSlides().get(0);
        List<AbstractSlideObject> objects = currentSlide.getSlideObjects();
        for (int i = objects.size() - 1; i >= 0; i--) {
            AbstractSlideObject object = objects.get(i);
            if (object.contains(worldPoint)) {
                return object;
            }
        }
        return null;
    }
    
    private void deselectAllObjects() {
        Slide currentSlide = presentation.getSlides().get(0);
        for (AbstractSlideObject object : currentSlide.getSlideObjects()) {
            object.setSelected(false);
        }
        selectedObject = null;
        updatePropertiesPanel();
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