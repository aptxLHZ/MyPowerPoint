package com.myppt.controller.strategies;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.awt.Rectangle;
import java.awt.Cursor;

import com.myppt.controller.AppController;
import com.myppt.model.AbstractSlideObject;
import com.myppt.model.LineShape;
import com.myppt.model.Presentation;
import com.myppt.model.Slide;
import com.myppt.view.MainFrame;

import javax.swing.JMenuItem;  // [!] 新增
import javax.swing.JPopupMenu; // [!] 新增
import javax.swing.SwingUtilities;


public class SelectStrategy implements InteractionStrategy {
    private AppController appController;
    private MainFrame mainFrame;
    private Presentation presentation;
    
    private AbstractSlideObject selectedObject = null;
    private Point dragStartPoint = null;
    private Point objectStartPoint = null;

    private ResizeHandle activeResizeHandle = null; // 当前被拖动的控制点
    private Rectangle originalBounds = null; // 开始缩放前对象的边界
    private double aspectRatio = 1.0;

    public SelectStrategy(AppController appController) {
        this.appController = appController;
        this.mainFrame = appController.getMainFrame();
        this.presentation = appController.getPresentation();
    }
    
    @Override
    public void mousePressed(MouseEvent e) {

        // [!] 修改点: 检查是否是右键点击
        if (SwingUtilities.isRightMouseButton(e)) {
            handleRightClick(e);
            return; // 处理完右键，直接返回
        }

        Point worldPoint = appController.convertScreenToWorld(e.getPoint());
        
        // 1. 优先检查是否点中了已选中对象的控制点
        if (selectedObject != null) {
            for (Map.Entry<ResizeHandle, Rectangle> entry : selectedObject.getResizeHandles().entrySet()) {
                if (entry.getValue().contains(worldPoint)) {
                    activeResizeHandle = entry.getKey();
                    originalBounds = selectedObject.getBounds();
                    dragStartPoint = worldPoint;
                    
                    // [!] 核心修改: 在开始缩放时，计算并存储当前的宽高比
                    if (originalBounds.height != 0) {
                        aspectRatio = (double) originalBounds.width / originalBounds.height;
                    } else {
                        aspectRatio = 1.0; // 防止除以零
                    }

                    mainFrame.getCanvasPanel().repaint();
                    return;
                }
            }
        }
        
        // 2. 如果没点中控制点，再执行之前的选取/移动逻辑
        AbstractSlideObject clickedObject = findObjectAtPoint(worldPoint);

        if (clickedObject != selectedObject) {
            deselectAllObjects();
            selectedObject = clickedObject;
            if (selectedObject != null) {
                selectedObject.setSelected(true);
            }
        }
        
        if (selectedObject != null) {
            dragStartPoint = worldPoint;
            objectStartPoint = new Point(selectedObject.getX(), selectedObject.getY());
        }
        
        appController.setSelectedObject(selectedObject);
        appController.updatePropertiesPanel();
        mainFrame.getCanvasPanel().repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (selectedObject == null || dragStartPoint == null) return;
        
        Point worldPoint = appController.convertScreenToWorld(e.getPoint());
        
        if (activeResizeHandle != null) {
            handleResize(worldPoint, e.isShiftDown());
        } else {
            int dx = worldPoint.x - dragStartPoint.x;
            int dy = worldPoint.y - dragStartPoint.y;
            selectedObject.setX(objectStartPoint.x + dx);
            selectedObject.setY(objectStartPoint.y + dy);
        }
        
        mainFrame.getCanvasPanel().repaint();
    }

    /**
     * 核心的缩放计算方法，现在支持对所有对象的通用等比例缩放。
     * @param currentPoint 当前鼠标的世界坐标
     * @param isShiftDown Shift键是否被按下
     */
    private void handleResize(Point currentPoint, boolean isShiftDown) {
        int dx = currentPoint.x - dragStartPoint.x;
        int dy = currentPoint.y - dragStartPoint.y;

        int newX = originalBounds.x;
        int newY = originalBounds.y;
        int newWidth = originalBounds.width;
        int newHeight = originalBounds.height;

        switch (activeResizeHandle) {
            case TOP_LEFT:
                newX += dx; newY += dy; newWidth -= dx; newHeight -= dy;
                break;
            case TOP_CENTER:
                newY += dy; newHeight -= dy;
                break;
            case TOP_RIGHT:
                newY += dy; newWidth += dx; newHeight -= dy;
                break;
            case MIDDLE_LEFT:
                newX += dx; newWidth -= dx;
                break;
            case MIDDLE_RIGHT:
                newWidth += dx;
                break;
            case BOTTOM_LEFT:
                newX += dx; newWidth -= dx; newHeight += dy;
                break;
            case BOTTOM_CENTER:
                newHeight += dy;
                break;
            case BOTTOM_RIGHT:
                newWidth += dx; newHeight += dy;
                break;
        }

        // [!] 核心修改: 通用的等比例缩放逻辑
        if (isShiftDown) {
            boolean isCornerHandle = activeResizeHandle == ResizeHandle.TOP_LEFT ||
                                    activeResizeHandle == ResizeHandle.TOP_RIGHT ||
                                    activeResizeHandle == ResizeHandle.BOTTOM_LEFT ||
                                    activeResizeHandle == ResizeHandle.BOTTOM_RIGHT;
            
            if (isCornerHandle) {
                // 如果拖动的是角点，则进行等比例缩放
                if (newWidth / aspectRatio > newHeight) {
                    newHeight = (int) (newWidth / aspectRatio);
                } else {
                    newWidth = (int) (newHeight * aspectRatio);
                }

                // 根据拖动的角点，重新校准 x 和 y 的位置
                switch (activeResizeHandle) {
                    case TOP_LEFT:
                        newX = originalBounds.x + originalBounds.width - newWidth;
                        newY = originalBounds.y + originalBounds.height - newHeight;
                        break;
                    case TOP_RIGHT:
                        newY = originalBounds.y + originalBounds.height - newHeight;
                        break;
                    case BOTTOM_LEFT:
                        newX = originalBounds.x + originalBounds.width - newWidth;
                        break;
                    case BOTTOM_RIGHT:
                        // newX 和 newY 不需要调整
                        break;
                    default:
                        break;
                }
            }
        }

        if (newWidth < 0) {
            newX += newWidth;
            newWidth = -newWidth;
        }
        if (newHeight < 0) {
            newY += newHeight;
            newHeight = -newHeight;
        }
        
        // 特殊处理直线
        if (selectedObject instanceof LineShape) {
            selectedObject.setBounds(new Rectangle(newX, newY, newWidth, newHeight));
        } else {
            selectedObject.setBounds(new Rectangle(newX, newY, newWidth, newHeight));
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragStartPoint = null;
        objectStartPoint = null;
        // [!] 核心修复: 缩放结束后，必须重置缩放状态
        activeResizeHandle = null; 
        originalBounds = null;
    }
  
    @Override
    public void mouseMoved(MouseEvent e) {
        Point worldPoint = appController.convertScreenToWorld(e.getPoint());
        
        // 默认光标样式
        int cursorType = Cursor.DEFAULT_CURSOR;
        
        // 只有当有对象被选中时，才检查是否悬停在控制点上
        if (selectedObject != null) {
            for (java.util.Map.Entry<ResizeHandle, java.awt.Rectangle> entry : selectedObject.getResizeHandles().entrySet()) {
                if (entry.getValue().contains(worldPoint)) {
                    // 如果鼠标在一个控制点上，就获取该控制点对应的光标类型
                    cursorType = entry.getKey().getCursor();
                    break; // 找到一个就不再继续找了
                }
            }
        }
        
        // 设置画布的鼠标光标
        mainFrame.getCanvasPanel().setCursor(Cursor.getPredefinedCursor(cursorType));
    }

    // --- 辅助方法 ---
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
        appController.setSelectedObject(null);
    }

    // [!] 新增: 处理右键点击的方法
    private void handleRightClick(MouseEvent e) {
        Point worldPoint = appController.convertScreenToWorld(e.getPoint());
        AbstractSlideObject clickedObject = findObjectAtPoint(worldPoint);
        
        // 只有当右键点击在某个对象上时，才显示菜单
        if (clickedObject != null) {
            // 如果点击的对象不是当前选中的，就先选中它
            if (clickedObject != selectedObject) {
                mousePressed(e); // 模拟一次左键点击的选中过程
            }
            
            // 创建弹出菜单
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem deleteItem = new JMenuItem("删除");
            
            // 为“删除”菜单项添加动作监听器
            deleteItem.addActionListener(actionEvent -> {
                if (selectedObject != null) {
                    presentation.getSlides().get(0).removeObject(selectedObject);
                    appController.setSelectedObject(null);
                    appController.updatePropertiesPanel();
                    mainFrame.getCanvasPanel().repaint();
                }
            });
            
            popupMenu.add(deleteItem);
            
            // 在鼠标点击的位置显示菜单
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}