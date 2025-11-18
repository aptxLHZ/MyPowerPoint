package com.myppt.controller.strategies;

import java.awt.Cursor;

/**
 * 定义8个缩放控制点的类型及其对应的鼠标光标。
 */
public enum ResizeHandle {
    TOP_LEFT(Cursor.NW_RESIZE_CURSOR),
    TOP_CENTER(Cursor.N_RESIZE_CURSOR),
    TOP_RIGHT(Cursor.NE_RESIZE_CURSOR),
    MIDDLE_LEFT(Cursor.W_RESIZE_CURSOR),
    MIDDLE_RIGHT(Cursor.E_RESIZE_CURSOR),
    BOTTOM_LEFT(Cursor.SW_RESIZE_CURSOR),
    BOTTOM_CENTER(Cursor.S_RESIZE_CURSOR),
    BOTTOM_RIGHT(Cursor.SE_RESIZE_CURSOR);

    private final int cursor;

    ResizeHandle(int cursor) {
        this.cursor = cursor;
    }

    public int getCursor() {
        return cursor;
    }
    
}