package com.myppt.model;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics; // 用于精确测量文字尺寸
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;

public class TextBox extends AbstractSlideObject {
    private String text;
    private Font font;
    private Color textColor;

    // 用于碰撞检测的边界
    private int width;
    private int height;

    public TextBox(int x, int y, String text, Font font, Color color) {
        super(x, y);
        this.text = text;
        this.font = font;
        this.textColor = color;
    }

    public void setText(String text) {
        this.text = text;
    }
    
    public void setTextColor(Color color) {
        this.textColor = color;
    }
    
    public Color getTextColor() {
        return this.textColor;
    }

    @Override
    public boolean contains(Point p) {
        // 这里的width和height是在draw方法中计算的
        // 所以碰撞检测依赖于至少被绘制过一次
        return p.x >= this.x && p.x <= (this.x + this.width) &&
               p.y >= this.y && p.y <= (this.y + this.height);
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        // 1. 保存原始绘图状态
        Color originalColor = g2d.getColor();
        Font originalFont = g2d.getFont();
        Stroke originalStroke = g2d.getStroke();

        // 2. 设置当前文本框的字体和颜色
        g2d.setFont(this.font);
        g2d.setColor(this.textColor);
        
        // 3. 获取字体信息，用于计算尺寸和位置
        FontMetrics fm = g2d.getFontMetrics(this.font);
        int lineHeight = fm.getHeight();
        int textAscent = fm.getAscent();

        // 4. [核心] 处理换行逻辑
        String[] lines = this.text.split("\n"); // 按换行符分割文本
        int currentY = this.y;
        this.width = 0; // 重置宽度，准备计算最长行的宽度

        for (String line : lines) {
            // 逐行绘制文本
            g2d.drawString(line, this.x, currentY + textAscent);
            
            // 检查当前行是否是最长的，如果是，则更新文本框的宽度
            int currentLineWidth = fm.stringWidth(line);
            if (currentLineWidth > this.width) {
                this.width = currentLineWidth;
            }
            
            // y坐标下移一行的高度
            currentY += lineHeight;
        }
        
        // 5. 根据行数和行高，计算文本框的总高度
        this.height = lines.length * lineHeight;

        // 6. 如果被选中，绘制包围框
        if (this.selected) {
            g2d.setColor(Color.BLUE);
            Stroke dashed = new java.awt.BasicStroke(2, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_BEVEL, 0, new float[]{10, 5}, 0);
            g2d.setStroke(dashed);
            // 包围框的位置和尺寸基于我们刚刚计算出的width和height
            g2d.drawRect(this.x - 3, this.y - 3, this.width + 6, this.height + 6);
        }

        // 7. 恢复原始绘图状态
        g2d.setColor(originalColor);
        g2d.setFont(originalFont);
        g2d.setStroke(originalStroke);
    }

    public String getText() {
        return this.text;
    }

}