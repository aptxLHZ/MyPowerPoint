package com.myppt.commands;

import com.myppt.model.AbstractSlideObject;
import com.myppt.model.Slide;
// import java.util.List;

public class DeleteObjectCommand implements Command {
    private Slide targetSlide;
    private AbstractSlideObject objectToDelete;
    private int originalIndex; // [!] 关键: 记录对象被删除前的位置

    public DeleteObjectCommand(Slide targetSlide, AbstractSlideObject objectToDelete) {
        this.targetSlide = targetSlide;
        this.objectToDelete = objectToDelete;
    }

    @Override
    public void execute() {
        // 在执行删除前，记录下它的原始位置
        // 这对于撤销后的“层次”恢复至关重要
        this.originalIndex = targetSlide.getSlideObjects().indexOf(objectToDelete);
        targetSlide.removeObject(objectToDelete);
    }

    @Override
    public void undo() {
        // 撤销删除，就是在原始位置把对象加回去
        targetSlide.getSlideObjects().add(originalIndex, objectToDelete);
    }
}