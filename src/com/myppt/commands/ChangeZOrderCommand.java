package com.myppt.commands;

import java.util.List;
import com.myppt.model.AbstractSlideObject;
import com.myppt.model.Slide;

public class ChangeZOrderCommand implements Command {
    private Slide targetSlide;
    private List<AbstractSlideObject> beforeOrder;
    private List<AbstractSlideObject> afterOrder;

    /**
     * @param targetSlide 被操作的幻灯片
     * @param beforeOrder 操作【之前】的对象列表状态
     * @param afterOrder 操作【之后】的对象列表状态
     */
    public ChangeZOrderCommand(Slide targetSlide, List<AbstractSlideObject> beforeOrder, List<AbstractSlideObject> afterOrder) {
        this.targetSlide = targetSlide;
        this.beforeOrder = new java.util.ArrayList<>(beforeOrder);
        this.afterOrder = new java.util.ArrayList<>(afterOrder);
    }

    @Override
    public void execute() {
        targetSlide.setSlideObjects(afterOrder);
    }

    @Override
    public void undo() {
        targetSlide.setSlideObjects(beforeOrder);
    }
}