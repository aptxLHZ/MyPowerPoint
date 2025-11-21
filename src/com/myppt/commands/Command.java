package com.myppt.commands;

/**
 * 命令接口，所有可撤销/重做的操作都必须实现此接口。
 */
public interface Command {
    /**
     * 执行命令。
     */
    void execute();

    /**
     * 撤销命令。
     */
    void undo();
}