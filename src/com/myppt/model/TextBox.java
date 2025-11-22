package com.myppt.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import javax.swing.JPanel;

public class TextBox extends AbstractSlideObject {
    private static final long serialVersionUID = 1L;
    
    private String text;
    private Font font;
    private Color textColor;

    private int width;
    private int height;

    public TextBox(int x, int y, String text, Font font, Color color) {
        super(x, y);
        this.text = text;
        this.font = font;
        this.textColor = color;
        
        // 在创建时就立即计算一个初始的、自然的边界
        calculateNaturalBounds();
    }

    public void setText(String text) {
        this.text = text;
        // 文本内容改变时，重新计算自然边界
        calculateNaturalBounds();
    }
    
    /**
     * 计算文本在不换行或只按`\n`换行时的自然宽度和高度。
     * 这个方法在创建或文本内容更新时调用。
     */
    private void calculateNaturalBounds() {
        JPanel tempPanel = new JPanel();
        FontMetrics fm = tempPanel.getFontMetrics(this.font);
        
        String[] lines = this.text.split("\n");
        int maxWidth = 0;
        for (String line : lines) {
            int currentWidth = fm.stringWidth(line);
            if (currentWidth > maxWidth) {
                maxWidth = currentWidth;
            }
        }
        this.width = maxWidth > 0 ? maxWidth : 30; // 保证有个最小宽度
        this.height = lines.length * fm.getHeight();
    }

    /**
     * 根据给定的宽度，重新计算文本需要的高度。
     * 这个方法在缩放时(setBounds)调用。
     */
    private void updateHeightForWidth(int targetWidth) {
        JPanel tempPanel = new JPanel();
        FontMetrics fm = tempPanel.getFontMetrics(this.font);
        int lineHeight = fm.getHeight();

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
                if (fm.stringWidth(currentLine.toString() + ch) > targetWidth && currentLine.length() > 0) {
                    linesToDraw.add(currentLine.toString());
                    currentLine = new StringBuilder();
                    currentLine.append(ch);
                } else {
                    currentLine.append(ch);
                }
            }
            linesToDraw.add(currentLine.toString());
        }
        
        this.height = Math.max(lineHeight, linesToDraw.size() * lineHeight);
    }
    
    // --- Getters and Setters ---
    public String getText() {
        return this.text;
    }

    public void setTextColor(Color color) {
        this.textColor = color;
    }
    
    public Color getTextColor() {
        return this.textColor;
    }
    
    // --- Overridden methods ---

    @Override
    public boolean contains(Point p) {
        // width 和 height 现在总是最新的，所以这个方法是可靠的
        return p.x >= this.x && p.x <= (this.x + this.width) &&
               p.y >= this.y && p.y <= (this.y + this.height);
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
        
        // 宽度改变后，立即根据新宽度重新计算所需的高度
        updateHeightForWidth(this.width);
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        Color originalColor = g2d.getColor();
        Font originalFont = g2d.getFont();
        Stroke originalStroke = g2d.getStroke();

        g2d.setFont(this.font);
        g2d.setColor(this.textColor);
        
        FontMetrics fm = g2d.getFontMetrics(this.font);
        int lineHeight = fm.getHeight();
        int textAscent = fm.getAscent();

        // 核心: 基于字符的 Word Wrap 算法
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
                if (fm.stringWidth(currentLine.toString() + ch) > this.width && currentLine.length() > 0) {
                    linesToDraw.add(currentLine.toString());
                    currentLine = new StringBuilder();
                    currentLine.append(ch);
                } else {
                    currentLine.append(ch);
                }
            }
            linesToDraw.add(currentLine.toString());
        }

        // 绘制所有计算好的行
        int currentY = this.y;
        for (String line : linesToDraw) {
            g2d.drawString(line, this.x, currentY + textAscent);
            currentY += lineHeight;
        }
        
        // 根据绘制的行数，再次确认高度 (作为双重保险)
        this.height = Math.max(lineHeight, linesToDraw.size() * lineHeight);

        // 绘制选中效果 (虚线框 + 控制点)
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

        // 恢复状态
        g2d.setColor(originalColor);
        g2d.setFont(originalFont);
        g2d.setStroke(originalStroke);
    }

    public Font getFont() {
        return this.font;
    }

    public void setFont(Font font) {
        this.font = font;
        // [!] 核心修复: 字体改变后，立即重新计算自然边界
        calculateNaturalBounds();
        // 并且，如果之前有缩放，要确保新的高度也符合缩放的宽度
        // 所以，这里还需要再次调用 updateHeightForWidth，确保高度匹配当前宽度
        updateHeightForWidth(this.width);
    }

    @Override
    public Style getStyle() {
        return new TextStyle(this.font, this.textColor);
    }

    @Override
    public void setStyle(Style style) {
        if (style instanceof TextStyle) {
            TextStyle ts = (TextStyle) style;
            this.setFont(ts.getFont()); // 使用setFont以确保边界更新
            this.setTextColor(ts.getColor());
        }
    }
}