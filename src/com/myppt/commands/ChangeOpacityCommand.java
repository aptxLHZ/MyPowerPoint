package com.myppt.commands;

import com.myppt.model.ImageObject;

public class ChangeOpacityCommand implements Command {
    private ImageObject targetImage;
    private float oldOpacity;
    private float newOpacity;

    public ChangeOpacityCommand(ImageObject targetImage, float newOpacity) {
        this.targetImage = targetImage;
        this.newOpacity = newOpacity;
        this.oldOpacity = targetImage.getOpacity();
    }

    @Override
    public void execute() {
        targetImage.setOpacity(newOpacity);
    }

    @Override
    public void undo() {
        targetImage.setOpacity(oldOpacity);
    }
}