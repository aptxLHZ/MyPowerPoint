package com.myppt.commands;

import java.awt.Rectangle;
import com.myppt.model.AbstractSlideObject;

public class TransformCommand implements Command {
    private AbstractSlideObject targetObject;
    private Rectangle oldBounds;
    private Rectangle newBounds;

    public TransformCommand(AbstractSlideObject targetObject, Rectangle oldBounds, Rectangle newBounds) {
        this.targetObject = targetObject;
        this.oldBounds = oldBounds;
        this.newBounds = newBounds;
    }

    @Override
    public void execute() {
        targetObject.setBounds(newBounds);
    }

    @Override
    public void undo() {
        targetObject.setBounds(oldBounds);
    }
}