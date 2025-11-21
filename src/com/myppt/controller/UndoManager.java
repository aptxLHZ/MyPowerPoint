package com.myppt.controller;

import java.util.Stack;
import com.myppt.commands.Command;

/**
 * 管理所有命令的执行、撤销和重做。
 */
public class UndoManager {
    private Stack<Command> undoStack = new Stack<>();
    private Stack<Command> redoStack = new Stack<>();

    /**
     * 执行一个新命令，并将其添加到撤销栈中。
     * @param command 要执行的命令
     */
    public void executeCommand(Command command) {
        command.execute();
        undoStack.push(command);
        // 当执行一个新命令时，所有之前的“重做”历史都应被清空
        redoStack.clear();
    }

    /**
     * 撤销上一个命令。
     */
    public void undo() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
        }
    }

    /**
     * 重做上一个被撤销的命令。
     */
    public void redo() {
        if (!redoStack.isEmpty()) {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * 将一个【已经执行过】的命令添加到撤销栈中。
     * 用于拖拽等实时预览的操作。
     * @param command 已经执行过的命令
     */
    public void addCommand(Command command) {
        undoStack.push(command);
        redoStack.clear(); // 同样需要清空重做栈
    }
}