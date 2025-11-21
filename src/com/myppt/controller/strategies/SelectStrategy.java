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
// import com.myppt.model.Presentation;
import com.myppt.model.Slide;
import com.myppt.view.MainFrame;
import com.myppt.commands.DeleteObjectCommand;
import com.myppt.commands.Command;
import com.myppt.commands.Command;
import com.myppt.commands.TransformCommand;

import javax.swing.JMenuItem;  // [!] 新增
import javax.swing.JPopupMenu; // [!] 新增
import javax.swing.SwingUtilities;


public class SelectStrategy implements InteractionStrategy {
    private AppController appController;
    private MainFrame mainFrame;
    // private Presentation presentation;
    
    private AbstractSlideObject selectedObject = null;
    private Point dragStartPoint = null;
    private Point objectStartPoint = null;

    private ResizeHandle activeResizeHandle = null; // 当前被拖动的控制点
    private Rectangle originalBounds = null; // 开始缩放前对象的边界
    private double aspectRatio = 1.0;

    public SelectStrategy(AppController appController) {
        this.appController = appController;
        this.mainFrame = appController.getMainFrame();
        // this.presentation = appController.getPresentation();
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            handleRightClick(e);
            return;
        }

        Point worldPoint = appController.convertScreenToWorld(e.getPoint());
        
        // 清理上一次操作可能遗留的状态
        activeResizeHandle = null;
        originalBounds = null;
        dragStartPoint = null;
        objectStartPoint = null;

        // 1. 检查是否点中已选中对象的控制点
        if (selectedObject != null) {
            for (Map.Entry<ResizeHandle, Rectangle> entry : selectedObject.getResizeHandles().entrySet()) {
                if (entry.getValue().contains(worldPoint)) {
                    activeResizeHandle = entry.getKey();
                    break; // 找到即可
                }
            }
        }
        
        // 2. 如果没有点中控制点，则尝试选取对象
        if (activeResizeHandle == null) {
            AbstractSlideObject clickedObject = findObjectAtPoint(worldPoint);
            if (clickedObject != selectedObject) {
                deselectAllObjects();
                selectedObject = clickedObject;
                if (selectedObject != null) {
                    selectedObject.setSelected(true);
                }
                appController.setSelectedObject(selectedObject);
                appController.updatePropertiesPanel();
            }
        }

        // 3. 如果最终有选中的对象（无论是新选中的还是之前就选中的），则为移动或缩放做准备
        if (selectedObject != null) {
            originalBounds = selectedObject.getBounds(); // 记录下【操作前】的边界
            dragStartPoint = worldPoint;
            objectStartPoint = new Point(selectedObject.getX(), selectedObject.getY());
            if (originalBounds.height != 0) {
                aspectRatio = (double) originalBounds.width / originalBounds.height;
            }
        }
        
        mainFrame.getCanvasPanel().repaint();
        appController.repaintThumbnails();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (selectedObject == null || dragStartPoint == null) return;
        
        Point worldPoint = appController.convertScreenToWorld(e.getPoint());
        
        // 实时预览，直接修改对象状态
        if (activeResizeHandle != null) {
            handleResize(worldPoint, e.isShiftDown());
        } else {
            int dx = worldPoint.x - dragStartPoint.x;
            int dy = worldPoint.y - dragStartPoint.y;
            selectedObject.setX(objectStartPoint.x + dx);
            selectedObject.setY(objectStartPoint.y + dy);
        }
        
        mainFrame.getCanvasPanel().repaint();
        appController.repaintThumbnails();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // 只有在刚刚进行了一次拖动或缩放操作后才需要创建命令
        if (selectedObject != null && originalBounds != null) {
            Rectangle newBounds = selectedObject.getBounds();

            // 只有当边界真的发生了变化时才创建命令
            if (!originalBounds.equals(newBounds)) {
                // 注意: 我们不再把对象状态恢复到oldBounds
                Command command = new TransformCommand(selectedObject, originalBounds, newBounds);
                
                // 我们需要一个新的方法来只入栈，不执行
                // 我们先改造 UndoManager
                appController.getUndoManager().addCommand(command);
                appController.markAsDirty();
            }
        }

        // 清理本次操作的状态
        activeResizeHandle = null;
        originalBounds = null;
        dragStartPoint = null;
        objectStartPoint = null;
    }
    

    /**
     * 核心的缩放计算方法，现在支持对所有对象的通用等比例缩放。
     * @param currentPoint 当前鼠标的世界坐标
     * @param isShiftDown Shift键是否被按下
     */
    private void handleResize(Point currentPoint, boolean isShiftDown) {
        // [!] 核心修复: 如果是直线，走特殊逻辑
        if (selectedObject instanceof LineShape) {
            handleLineResize(currentPoint);
            return; // 处理完直接返回
        }

        appController.markAsDirty();

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

    // [!] 新增: 专门处理直线缩放的方法
    private void handleLineResize(Point currentPoint) {
        appController.markAsDirty();
        LineShape line = (LineShape) selectedObject;
        // 我们需要知道拖动前起点和终点的原始位置
        // 我们可以在 mousePressed 时就把它们存起来
        // 为了简化，我们暂时用一种变通的方法：
        // activeResizeHandle 告诉我们拖的是哪个点
        
        if (activeResizeHandle == ResizeHandle.TOP_LEFT) {
            // 拖动的是起点 (x, y)
            line.x = currentPoint.x;
            line.y = currentPoint.y;
        } else if (activeResizeHandle == ResizeHandle.BOTTOM_RIGHT) {
            // 拖动的是终点 (x2, y2)
            line.x2 = currentPoint.x;
            line.y2 = currentPoint.y;
        }
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
        // [!] 核心修复: 总是从 AppController 获取最新的 presentation
        Slide currentSlide = appController.getPresentation().getCurrentSlide();
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
        // [!] 核心修复: 总是从 AppController 获取最新的 presentation
        Slide currentSlide = appController.getPresentation().getCurrentSlide();
        for (AbstractSlideObject object : currentSlide.getSlideObjects()) {
            object.setSelected(false);
        }
        selectedObject = null;
        appController.setSelectedObject(null);
        appController.repaintThumbnails();
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
                    Command command = new DeleteObjectCommand(appController.getPresentation().getCurrentSlide(), selectedObject);
                    appController.getUndoManager().executeCommand(command);
                    
                    appController.markAsDirty();
                    appController.setSelectedObject(null);
                    appController.updateUI();
                }
            });
            
            popupMenu.add(deleteItem);
            
            // 在鼠标点击的位置显示菜单
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
            appController.repaintThumbnails();
        }
    }
}