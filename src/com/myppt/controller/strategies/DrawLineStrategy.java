package com.myppt.controller.strategies;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;

import com.myppt.controller.AppController;
import com.myppt.model.LineShape;
// import com.myppt.model.Presentation;
import com.myppt.view.MainFrame;

public class DrawLineStrategy implements InteractionStrategy {
    private AppController appController;
    private MainFrame mainFrame;
    // private Presentation presentation;
    private LineShape currentDrawingLine = null;

    public DrawLineStrategy(AppController appController) {
        this.appController = appController;
        this.mainFrame = appController.getMainFrame();
        // this.presentation = appController.getPresentation();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point worldPoint = appController.convertScreenToWorld(e.getPoint());
        if (appController.isPointInPage(worldPoint)) {
            currentDrawingLine = new LineShape(worldPoint.x, worldPoint.y, worldPoint.x, worldPoint.y, Color.BLACK, 2f);
            appController.getPresentation().getCurrentSlide().addObject(currentDrawingLine);
        }
        mainFrame.getCanvasPanel().repaint();
        appController.repaintThumbnails();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (currentDrawingLine != null) {
            Point worldPoint = appController.convertScreenToWorld(e.getPoint());
            currentDrawingLine.x2 = worldPoint.x;
            currentDrawingLine.y2 = worldPoint.y;
            mainFrame.getCanvasPanel().repaint();
        }
        appController.repaintThumbnails();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        appController.markAsDirty();
        currentDrawingLine = null;
        appController.setMode("SELECT");
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Do nothing
    }
}