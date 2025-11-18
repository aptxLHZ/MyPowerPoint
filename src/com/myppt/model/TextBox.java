package com.myppt.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics; // 用于精确测量文字尺寸
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.Rectangle;
import javax.swing.JPanel;

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
        //计算边界
        updateBounds(); 
    }

    public void setText(String text) {
        this.text = text;
    }
    
    // [!] 核心修改: 新增一个私有方法来计算边界
    private void updateBounds() {
        // 创建一个临时的JPanel来获取FontMetrics
        // 这是一个在非GUI线程中获取字体信息的标准技巧
        JPanel tempPanel = new JPanel();
        FontMetrics fm = tempPanel.getFontMetrics(this.font);
        
        // 我们需要模拟draw方法中的换行逻辑来计算正确的width和height
        String[] lines = this.text.split("\n");
        int maxWidth = 0;
        for (String line : lines) {
            int currentWidth = fm.stringWidth(line);
            if (currentWidth > maxWidth) {
                maxWidth = currentWidth;
            }
        }
        this.width = maxWidth;
        this.height = lines.length * fm.getHeight();

        System.out.println(">>> updateBounds: width 被设置为 " + this.width);
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
        
        // 1. 保存状态
        Color originalColor = g2d.getColor();
        Font originalFont = g2d.getFont();
        Stroke originalStroke = g2d.getStroke();

        g2d.setFont(this.font);
        g2d.setColor(this.textColor);
        
        FontMetrics fm = g2d.getFontMetrics(this.font);
        int lineHeight = fm.getHeight();
        int textAscent = fm.getAscent();

        // 2. [核心] 全新的、基于字符的 Word Wrap 算法
        java.util.List<String> linesToDraw = new java.util.ArrayList<>();
        String[] paragraphs = this.text.split("\n", -1);

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                linesToDraw.add("");
                continue;
            }

            StringBuilder currentLine = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                char ch = paragraph.charAt(i);
                
                // 检查如果加上下一个字符，是否会超过宽度
                if (fm.stringWidth(currentLine.toString() + ch) > this.width) {
                    // 如果会超宽，就把当前行存起来
                    linesToDraw.add(currentLine.toString());
                    // 开始新的一行，并把当前字符作为新行的第一个字符
                    currentLine = new StringBuilder();
                    currentLine.append(ch);
                } else {
                    // 如果没超宽，就把字符加到当前行
                    currentLine.append(ch);
                }
            }
            // 把最后剩余的部分也加进去
            linesToDraw.add(currentLine.toString());
        }

        // 3. 绘制所有计算好的行
        int currentY = this.y;
        for (String line : linesToDraw) {
            g2d.drawString(line, this.x, currentY + textAscent);
            currentY += lineHeight;
        }
        
        // 4. 根据计算出的行数，更新文本框的实际高度
        this.height = Math.max(lineHeight, linesToDraw.size() * lineHeight);

        // 5. 绘制选中效果 (虚线框 + 控制点)
        if (this.selected) {
            Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0);
            g2d.setStroke(dashed);
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawRect(this.x - 3, this.y - 3, this.width + 6, this.height + 6);
            
            g2d.setStroke(originalStroke);
            g2d.setColor(Color.BLUE);
            for (Rectangle handle : getResizeHandles().values()) {
                g2d.fill(handle);
            }
        }

        // 6. 恢复状态
        g2d.setColor(originalColor);
        g2d.setFont(originalFont);
        g2d.setStroke(originalStroke);
    }

    public String getText() {
        return this.text;
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }

    @Override
    public void setBounds(Rectangle bounds) {
        this.x = bounds.x;
        this.y = bounds.y;
        this.width = bounds.width;
        // 注意：我们只更新width，height将由draw方法根据自动换行重新计算
        
        // System.out.println(">>> setBounds: width 被设置为 " + this.width);
    }
}