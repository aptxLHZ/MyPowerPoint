package com.myppt.controller.strategies;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.awt.Rectangle;
import java.awt.Cursor;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.myppt.controller.AppController;
import com.myppt.model.AbstractSlideObject;
import com.myppt.model.LineShape;
import com.myppt.model.Slide;
import com.myppt.view.MainFrame;
import com.myppt.commands.DeleteObjectCommand;
import com.myppt.commands.ChangeZOrderCommand;
import com.myppt.commands.Command;
import com.myppt.commands.TransformCommand;

/**
 * 选择策略类。
 * 负责处理鼠标的“选择模式”下的交互，包括：
 * 1. 选中对象、取消选中。
 * 2. 移动对象。
 * 3. 缩放对象（包括通用形状和直线）。
 * 4. 右键上下文菜单（删除、层级调整）。
 */
public class SelectStrategy implements InteractionStrategy {
    private AppController appController;
    private MainFrame mainFrame;
    
    // 当前选中的对象
    private AbstractSlideObject selectedObject = null;
    
    // 拖拽相关的状态记录
    private Point dragStartPoint = null;  // 鼠标按下的屏幕坐标
    private Point objectStartPoint = null; // 对象移动前的坐标
    private ResizeHandle activeResizeHandle = null; // 当前激活的缩放控制手柄
    private Rectangle originalBounds = null; // 缩放前的对象边界
    private double aspectRatio = 1.0; // 宽高比，用于Shift等比缩放
    private boolean wasDragged = false; // 标记是否发生了拖拽动作

    public SelectStrategy(AppController appController) {
        this.appController = appController;
        this.mainFrame = appController.getMainFrame();
    }
    
    /**
     * 鼠标按下事件：
     * 处理右键菜单、检测是否点击控制点（缩放）、检测是否点击对象（选中）、准备移动。
     */
    @Override
    public void mousePressed(MouseEvent e) {
        wasDragged = false;
        
        // 1. 如果是右键点击，处理上下文菜单
        if (SwingUtilities.isRightMouseButton(e)) {
            handleRightClick(e);
            return;
        }

        Point worldPoint = appController.convertScreenToWorld(e.getPoint());
        
        // 2. 检查是否点中了【已选中】对象的缩放控制点
        if (selectedObject != null) {
            for (Map.Entry<ResizeHandle, Rectangle> entry : selectedObject.getResizeHandles().entrySet()) {
                if (entry.getValue().contains(worldPoint)) {
                    // 进入缩放模式
                    activeResizeHandle = entry.getKey();
                    originalBounds = selectedObject.getBounds();
                    dragStartPoint = worldPoint;
                    if (originalBounds.height != 0) {
                        aspectRatio = (double) originalBounds.width / originalBounds.height;
                    }
                    return; // 结束方法，后续逻辑是移动或选择，缩放优先
                }
            }
        }
        
        // 3. 如果没点中控制点，则进行对象命中测试（选取逻辑）
        AbstractSlideObject clickedObject = findObjectAtPoint(worldPoint);
        
        // 如果点击的对象不是当前选中的对象
        if (clickedObject != selectedObject) {
            deselectAllObjects(); // 先取消之前的选中
            selectedObject = clickedObject;
            if (selectedObject != null) {
                selectedObject.setSelected(true);
            }
            // 更新控制器状态
            appController.setSelectedObject(selectedObject);
            appController.updatePropertiesPanel();
            
            // 刷新视图
            mainFrame.getCanvasPanel().repaint();
            appController.repaintThumbnails();
        }

        // 4. 如果【当前】有选中的对象，记录状态为【移动】做准备
        if (selectedObject != null) {
            originalBounds = selectedObject.getBounds();
            dragStartPoint = worldPoint;
            objectStartPoint = new Point(selectedObject.getX(), selectedObject.getY());
        }
        
        mainFrame.getCanvasPanel().repaint();
        appController.repaintThumbnails();
    }

    /**
     * 鼠标拖拽事件：
     * 根据当前状态执行 缩放 或 移动 操作。
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (selectedObject == null || dragStartPoint == null) return;
        wasDragged = true;
        Point worldPoint = appController.convertScreenToWorld(e.getPoint());
        
        if (activeResizeHandle != null) {
            // 处理缩放
            handleResize(worldPoint, e.isShiftDown());
        } else {
            // 处理移动
            int dx = worldPoint.x - dragStartPoint.x;
            int dy = worldPoint.y - dragStartPoint.y;
            selectedObject.setX(objectStartPoint.x + dx);
            selectedObject.setY(objectStartPoint.y + dy);
        }
        
        mainFrame.getCanvasPanel().repaint();
        appController.repaintThumbnails();
    }

    /**
     * 鼠标释放事件：
     * 判断是否发生了实质性的改变，如果是，则生成 Undo 命令并推入撤销栈。
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (wasDragged && selectedObject != null && originalBounds != null) {
            Rectangle newBounds = selectedObject.getBounds();
            // 只有当位置或大小真的改变了才创建命令
            if (!originalBounds.equals(newBounds)) {
                // 先恢复到原始状态，以便命令能够记录从 旧->新 的变化
                selectedObject.setBounds(originalBounds);
                
                Command command = new TransformCommand(selectedObject, originalBounds, newBounds);
                appController.getUndoManager().executeCommand(command);
                appController.markAsDirty();
            }
        }
        
        // 重置临时状态
        wasDragged = false;
        activeResizeHandle = null;
        originalBounds = null;
        dragStartPoint = null;
        objectStartPoint = null;
    }
    
    /**
     * 核心缩放计算方法，支持通用形状的等比例缩放。
     * @param currentPoint 当前鼠标的世界坐标
     * @param isShiftDown 是否按下Shift键（用于等比例缩放）
     */
    private void handleResize(Point currentPoint, boolean isShiftDown) {
        // 直线有特殊的缩放逻辑（修改端点）
        if (selectedObject instanceof LineShape) {
            handleLineResize(currentPoint);
            return;
        }

        appController.markAsDirty();

        int dx = currentPoint.x - dragStartPoint.x;
        int dy = currentPoint.y - dragStartPoint.y;

        int newX = originalBounds.x;
        int newY = originalBounds.y;
        int newWidth = originalBounds.width;
        int newHeight = originalBounds.height;

        // 根据不同的控制点计算新的边界
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

        // 处理 Shift 键等比例缩放逻辑
        if (isShiftDown) {
            boolean isCornerHandle = activeResizeHandle == ResizeHandle.TOP_LEFT ||
                                    activeResizeHandle == ResizeHandle.TOP_RIGHT ||
                                    activeResizeHandle == ResizeHandle.BOTTOM_LEFT ||
                                    activeResizeHandle == ResizeHandle.BOTTOM_RIGHT;
            
            if (isCornerHandle) {
                // 如果拖动的是角点，强制保持宽高比
                if (newWidth / aspectRatio > newHeight) {
                    newHeight = (int) (newWidth / aspectRatio);
                } else {
                    newWidth = (int) (newHeight * aspectRatio);
                }

                // 根据固定的角点，重新计算 x 和 y
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
                        // 右下角拖动，x和y不需要调整
                        break;
                    default:
                        break;
                }
            }
        }

        // 处理反向拖拽（宽度或高度变为负数）的情况
        if (newWidth < 0) {
            newX += newWidth;
            newWidth = -newWidth;
        }
        if (newHeight < 0) {
            newY += newHeight;
            newHeight = -newHeight;
        }
        
        // 应用新的边界
        selectedObject.setBounds(new Rectangle(newX, newY, newWidth, newHeight));
    }

    /**
     * 专门处理直线的缩放逻辑（实际上是调整起点或终点）。
     */
    private void handleLineResize(Point currentPoint) {
        appController.markAsDirty();
        LineShape line = (LineShape) selectedObject;
        
        if (activeResizeHandle == ResizeHandle.TOP_LEFT) {
            // 对于直线，TOP_LEFT 代表起点
            line.x = currentPoint.x;
            line.y = currentPoint.y;
        } else if (activeResizeHandle == ResizeHandle.BOTTOM_RIGHT) {
            // 对于直线，BOTTOM_RIGHT 代表终点
            line.x2 = currentPoint.x;
            line.y2 = currentPoint.y;
        }
    }

    /**
     * 鼠标移动事件：
     * 用于根据鼠标位置更新光标样式（例如悬停在缩放点上时）。
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        Point worldPoint = appController.convertScreenToWorld(e.getPoint());
        
        int cursorType = Cursor.DEFAULT_CURSOR;
        
        // 只有选中对象时才检测控制点
        if (selectedObject != null) {
            for (java.util.Map.Entry<ResizeHandle, java.awt.Rectangle> entry : selectedObject.getResizeHandles().entrySet()) {
                if (entry.getValue().contains(worldPoint)) {
                    cursorType = entry.getKey().getCursor();
                    break;
                }
            }
        }
        
        mainFrame.getCanvasPanel().setCursor(Cursor.getPredefinedCursor(cursorType));
    }

    // --- 内部辅助方法 ---

    /**
     * 根据坐标查找对象，考虑Z轴顺序（从上到下查找）。
     */
    private AbstractSlideObject findObjectAtPoint(Point worldPoint) {
        Slide currentSlide = appController.getPresentation().getCurrentSlide();
        List<AbstractSlideObject> objects = currentSlide.getSlideObjects();
        // 倒序遍历，确保先选中最上层的对象
        for (int i = objects.size() - 1; i >= 0; i--) {
            AbstractSlideObject object = objects.get(i);
            if (object.contains(worldPoint)) {
                return object;
            }
        }
        return null;
    }
    
    /**
     * 取消所有对象的选中状态。
     */
    private void deselectAllObjects() {
        Slide currentSlide = appController.getPresentation().getCurrentSlide();
        for (AbstractSlideObject object : currentSlide.getSlideObjects()) {
            object.setSelected(false);
        }
        selectedObject = null;
        appController.setSelectedObject(null);
        appController.repaintThumbnails();
    }

    /**
     * 处理右键点击逻辑：选中对象并弹出上下文菜单。
     */
    private void handleRightClick(MouseEvent e) {
        Point worldPoint = appController.convertScreenToWorld(e.getPoint());
        AbstractSlideObject clickedObject = findObjectAtPoint(worldPoint);
        
        if (clickedObject != null) {
            // 如果右键点击了非当前选中对象，则切换选中状态
            if (clickedObject != selectedObject) {
                deselectAllObjects();
                selectedObject = clickedObject;
                selectedObject.setSelected(true);
                appController.setSelectedObject(selectedObject);
                appController.updatePropertiesPanel();
                mainFrame.getCanvasPanel().repaint();
                appController.repaintThumbnails();
            }
            
            // 构建弹出菜单
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem deleteItem = new JMenuItem("删除");
            JMenuItem bringToFrontItem = new JMenuItem("置于顶层");
            JMenuItem sendToBackItem = new JMenuItem("置于底层");
            JMenuItem bringForwardItem = new JMenuItem("上移一层");
            JMenuItem sendBackwardItem = new JMenuItem("下移一层");
            
            // 层级调整的通用监听器
            java.awt.event.ActionListener zOrderListener = actionEvent -> {
                if (selectedObject == null) return;
                
                Slide slide = appController.getPresentation().getCurrentSlide();
                
                // 1. 记录操作前的顺序
                List<AbstractSlideObject> beforeOrder = new java.util.ArrayList<>(slide.getSlideObjects());
                
                // 2. 执行具体操作
                Object source = actionEvent.getSource();
                if (source == bringToFrontItem) slide.bringToFront(selectedObject);
                else if (source == sendToBackItem) slide.sendToBack(selectedObject);
                else if (source == bringForwardItem) slide.bringForward(selectedObject);
                else if (source == sendBackwardItem) slide.sendBackward(selectedObject);
                
                // 3. 记录操作后的顺序
                List<AbstractSlideObject> afterOrder = new java.util.ArrayList<>(slide.getSlideObjects());

                // 4. 为了支持撤销，先恢复到旧状态，让 Command 负责执行变更
                slide.setSlideObjects(beforeOrder);

                // 5. 创建并执行命令
                Command command = new ChangeZOrderCommand(slide, beforeOrder, afterOrder);
                appController.getUndoManager().executeCommand(command);
                
                appController.markAsDirty();
                appController.updateUI();
            };

            // 绑定事件
            bringToFrontItem.addActionListener(zOrderListener);
            sendToBackItem.addActionListener(zOrderListener);
            bringForwardItem.addActionListener(zOrderListener);
            sendBackwardItem.addActionListener(zOrderListener);

            // 删除事件
            deleteItem.addActionListener(actionEvent -> {
                if (selectedObject != null) {
                    Command deleteCmd = new DeleteObjectCommand(appController.getPresentation().getCurrentSlide(), selectedObject);
                    appController.getUndoManager().executeCommand(deleteCmd);
                    appController.markAsDirty();
                    appController.setSelectedObject(null); // 删除后清空选择
                    appController.updateUI();
                }
            });
            
            // 添加菜单项
            popupMenu.add(bringToFrontItem);
            popupMenu.add(sendToBackItem);
            popupMenu.add(bringForwardItem);
            popupMenu.add(sendBackwardItem);
            popupMenu.addSeparator();
            popupMenu.add(deleteItem);
            
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}