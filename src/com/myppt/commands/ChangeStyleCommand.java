package com.myppt.commands;

import com.myppt.model.AbstractSlideObject;
import com.myppt.model.Style;

public class ChangeStyleCommand implements Command {
    private AbstractSlideObject targetObject;
    private Style oldStyle;
    private Style newStyle;

    public ChangeStyleCommand(AbstractSlideObject target, Style newStyle) {
        this.targetObject = target;
        this.newStyle = newStyle;
        this.oldStyle = target.getStyle(); // 记录旧样式
    }
    
    @Override
    public void execute() {
        targetObject.setStyle(newStyle);
    }

    @Override
    public void undo() {
        targetObject.setStyle(oldStyle);
    }
}