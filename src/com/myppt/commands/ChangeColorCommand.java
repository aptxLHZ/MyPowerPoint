package com.myppt.commands;

import com.myppt.model.*;
import java.awt.Color;


public class ChangeColorCommand implements Command {
    private AbstractSlideObject targetObject;
    private Color oldColor;
    private Color newColor;

    public ChangeColorCommand(AbstractSlideObject targetObject, Color newColor) {
        this.targetObject = targetObject;
        this.newColor = newColor;
        // 在创建命令时，就记录下旧颜色
        if (targetObject instanceof RectangleShape) this.oldColor = ((RectangleShape) targetObject).getFillColor();
        else if (targetObject instanceof EllipseShape) this.oldColor = ((EllipseShape) targetObject).getFillColor();
        else if (targetObject instanceof LineShape) this.oldColor = ((LineShape) targetObject).getLineColor();
        else if (targetObject instanceof TextBox) this.oldColor = ((TextBox) targetObject).getTextColor();
    }
    
    @Override
    public void execute() {
        setColor(newColor);
    }

    @Override
    public void undo() {
        setColor(oldColor);
    }

    private void setColor(Color color) {
        if (targetObject instanceof RectangleShape) ((RectangleShape) targetObject).setFillColor(color);
        else if (targetObject instanceof EllipseShape) ((EllipseShape) targetObject).setFillColor(color);
        else if (targetObject instanceof LineShape) ((LineShape) targetObject).setLineColor(color);
        else if (targetObject instanceof TextBox) ((TextBox) targetObject).setTextColor(color);
    }
}