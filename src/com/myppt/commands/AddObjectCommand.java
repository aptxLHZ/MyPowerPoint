package com.myppt.commands;

import com.myppt.model.AbstractSlideObject;
import com.myppt.model.Slide;


public class AddObjectCommand implements Command {
    private Slide targetSlide;
    private AbstractSlideObject objectToAdd;

    public AddObjectCommand(Slide targetSlide, AbstractSlideObject objectToAdd) {
        this.targetSlide = targetSlide;
        this.objectToAdd = objectToAdd;
    }

    @Override
    public void execute() {
        targetSlide.addObject(objectToAdd);
    }

    @Override
    public void undo() {
        targetSlide.removeObject(objectToAdd);
    }
}