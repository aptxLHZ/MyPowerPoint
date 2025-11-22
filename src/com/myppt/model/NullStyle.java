package com.myppt.model;

/**
 * 表示一个空的、无具体内容的样式。
 * 用于对象没有可复制/粘贴的特定样式时。
 */
public class NullStyle implements Style {
    private static final long serialVersionUID = 1L; // 确保可序列化
    // 这是一个单例模式，避免重复创建实例
    private static NullStyle instance = new NullStyle();
    private NullStyle() {}
    public static NullStyle getInstance() {
        return instance;
    }
}