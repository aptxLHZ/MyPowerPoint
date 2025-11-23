package com.myppt.commands;

import java.awt.Color;
import com.myppt.model.EllipseShape;
import com.myppt.model.LineShape;
import com.myppt.model.RectangleShape;

/**
 * [已重构] 一个统一的命令，用于修改任何支持边框或线条的对象的属性（颜色、宽度、样式）。
 * 这个版本移除了复杂的适配器模式，改用更直接的 instanceof 判断，以提高代码的清晰度和健壮性。
 */
public class ChangeBorderCommand implements Command {

    private final Object targetObject; // 直接存储目标对象，类型为Object

    // 完整的保存“之前”和“之后”的状态
    private final Color oldColor, newColor;
    private final double oldWidth, newWidth;
    private final int oldStyle, newStyle;

    /**
     * 构造函数，用于创建一个边框/线条属性变更的命令。
     * @param target 目标对象 (必须是 RectangleShape, EllipseShape, 或 LineShape)
     * @param newColor 新的颜色
     * @param newWidth 新的宽度
     * @param newStyle 新的线型
     */
    public ChangeBorderCommand(Object target, Color newColor, double newWidth, int newStyle) {
        this.targetObject = target;

        // [DEBUG] 添加调试信息
        System.out.println("\n--- 【调试】ChangeBorderCommand: 构造函数被调用 ---");
        System.out.println("【调试】-> 传入的 newWidth 是: " + newWidth);

        // 明确地保存传入的“新状态”
        this.newColor = newColor;
        this.newWidth = newWidth;
        this.newStyle = newStyle;

        // 根据对象类型，从对象中安全地读取并保存“旧状态”
        if (target instanceof RectangleShape) {
            RectangleShape r = (RectangleShape) target;
            this.oldColor = r.getBorderColor();
            this.oldWidth = r.getBorderWidth();
            this.oldStyle = r.getBorderStyle();
        } else if (target instanceof EllipseShape) {
            EllipseShape e = (EllipseShape) target;
            this.oldColor = e.getBorderColor();
            this.oldWidth = e.getBorderWidth();
            this.oldStyle = e.getBorderStyle();
        } else if (target instanceof LineShape) {
            LineShape l = (LineShape) target;
            this.oldColor = l.getLineColor();
            this.oldWidth = l.getStrokeWidth(); // LineShape 使用 float, 但可以安全转为 double
            this.oldStyle = l.getBorderStyle();
        } else {
            throw new IllegalArgumentException("目标对象不支持边框/线条属性的修改。");
        }
        
        // [DEBUG] 添加调试信息
        System.out.println("【调试】-> 从对象读取并保存的 oldWidth 是: " + this.oldWidth);
        System.out.println("--- 【调试】ChangeBorderCommand: 构造完毕 ---\n");
    }

    @Override
    public void execute() {
        // [DEBUG] 添加调试信息
        System.out.println("【调试】ChangeBorderCommand: 执行 execute(), 将宽度设置为 " + this.newWidth);
        
        // 根据对象类型，应用“新状态”
        if (targetObject instanceof RectangleShape) {
            RectangleShape r = (RectangleShape) targetObject;
            r.setBorderColor(newColor);
            r.setBorderWidth(newWidth);
            r.setBorderStyle(newStyle);
        } else if (targetObject instanceof EllipseShape) {
            EllipseShape e = (EllipseShape) targetObject;
            e.setBorderColor(newColor);
            e.setBorderWidth(newWidth);
            e.setBorderStyle(newStyle);
        } else if (targetObject instanceof LineShape) {
            LineShape l = (LineShape) targetObject;
            l.setLineColor(newColor);
            l.setStrokeWidth((float) newWidth); // 从 double 转回 float
            l.setBorderStyle(newStyle);
        }
    }

    @Override
    public void undo() {
        // [DEBUG] 添加调试信息
        System.out.println("【调试】ChangeBorderCommand: 执行 undo(), 将宽度恢复为 " + this.oldWidth);

        // 根据对象类型，恢复“旧状态”
        if (targetObject instanceof RectangleShape) {
            RectangleShape r = (RectangleShape) targetObject;
            r.setBorderColor(oldColor);
            r.setBorderWidth(oldWidth);
            r.setBorderStyle(oldStyle);
        } else if (targetObject instanceof EllipseShape) {
            EllipseShape e = (EllipseShape) targetObject;
            e.setBorderColor(oldColor);
            e.setBorderWidth(oldWidth);
            e.setBorderStyle(oldStyle);
        } else if (targetObject instanceof LineShape) {
            LineShape l = (LineShape) targetObject;
            l.setLineColor(oldColor);
            l.setStrokeWidth((float) oldWidth); // 从 double 转回 float
            l.setBorderStyle(oldStyle);
        }
    }
}