package com.myppt.controller;

import java.util.Stack;
import com.myppt.commands.Command;

/**
 * 管理所有命令的执行、撤销和重做。
 * 这是一个纯粹的数据结构管理者，不涉及任何UI更新。
 */
public class UndoManager {
    private Stack<Command> undoStack = new Stack<>();
    private Stack<Command> redoStack = new Stack<>();

    public void executeCommand(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
        System.out.println(">>> Command executed. Undo stack size: " + undoStack.size());
    }

    public void undo() {
        System.out.println(">>> Undo called. Can undo? " + canUndo());
        if (canUndo()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
        }
    }

    public void redo() {
        System.out.println(">>> Redo called. Can redo? " + canRedo());
        if (canRedo()) {
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
}