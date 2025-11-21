package com.myppt.commands;

import com.myppt.model.TextBox;

public class ChangeTextCommand implements Command {
    private TextBox targetTextBox;
    private String oldText;
    private String newText;

    public ChangeTextCommand(TextBox targetTextBox, String newText) {
        this.targetTextBox = targetTextBox;
        this.newText = newText;
        this.oldText = targetTextBox.getText(); // 在创建时记录旧文本
    }

    @Override
    public void execute() {
        targetTextBox.setText(newText);
    }

    @Override
    public void undo() {
        targetTextBox.setText(oldText);
    }
}