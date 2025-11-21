package com.myppt.commands;

import java.awt.Font;
import com.myppt.model.TextBox;

public class ChangeFontCommand implements Command {
    private TextBox targetTextBox;
    private Font oldFont;
    private Font newFont;

    public ChangeFontCommand(TextBox targetTextBox, Font newFont) {
        this.targetTextBox = targetTextBox;
        this.newFont = newFont;
        this.oldFont = targetTextBox.getFont(); // 在创建时记录旧字体
    }

    @Override
    public void execute() {
        targetTextBox.setFont(newFont);
    }

    @Override
    public void undo() {
        targetTextBox.setFont(oldFont);
    }
}