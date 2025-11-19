package com.myppt.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.myppt.model.Presentation;
import com.myppt.model.Slide;

public class PlayerFrame extends JFrame {
    private Presentation presentation;
    private PlayerPanel playerPanel;
    private boolean isEndScreen = false;

    public PlayerFrame(Presentation presentation) {
        this.presentation = presentation;
        
        setTitle("幻灯片放映");
        setUndecorated(true); // [!] 核心: 无边框窗口
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // 关闭时只销毁此窗口

        playerPanel = new PlayerPanel();
        add(playerPanel);

        // --- 事件监听 ---
        // 1. 键盘监听
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_PAGE_DOWN:
                        nextPage();
                        break;
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_PAGE_UP:
                        prevPage();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        exitPlayer();
                        break;
                }
            }
        });

        // 2. 鼠标监听
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                nextPage(); // 默认单击鼠标是下一页
            }
        });
    }

    public void start() {
        // [!] 核心: 进入全屏独占模式
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (device.isFullScreenSupported()) {
            device.setFullScreenWindow(this);
        } else {
            // 如果不支持全屏独占，就最大化窗口
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setVisible(true);
        }
    }

    private void nextPage() {
        // 如果已经在结束页，则退出
        if (isEndScreen) {
            exitPlayer();
            return;
        }

        int currentIndex = presentation.getCurrentSlideIndex();
        if (currentIndex < presentation.getSlides().size() - 1) {
            presentation.setCurrentSlideIndex(currentIndex + 1);
            playerPanel.repaint();
        } else {
            // [!] 核心修改: 到达最后一页后，进入结束页模式
            isEndScreen = true;
            playerPanel.repaint();
        }
    }

    private void prevPage() {
        int currentIndex = presentation.getCurrentSlideIndex();
        if (currentIndex > 0) {
            presentation.setCurrentSlideIndex(currentIndex - 1);
            playerPanel.repaint();
        }
    }
    
    private void exitPlayer() {
        // [!] 核心: 退出全屏模式
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        device.setFullScreenWindow(null);
        dispose(); // 销毁此窗口
    }

    /**
     * 内部类，专门用于在播放器中绘制幻灯片。
     */
    private class PlayerPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            // 1. 黑色背景
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // [!] 核心修改: 如果是结束页，则只绘制提示文字
            if (isEndScreen) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
                String msg = "放映结束，单击鼠标退出。";
                FontMetrics fm = g2d.getFontMetrics();
                int msgWidth = fm.stringWidth(msg);
                int msgHeight = fm.getAscent();
                g2d.drawString(msg, (getWidth() - msgWidth) / 2, 20 + msgHeight);
                return; // 绘制完直接返回，不执行后续的页面绘制
            }

            Slide currentSlide = presentation.getCurrentSlide();
            
            // 2. 计算缩放比例和居中位置
            int screenWidth = getWidth();
            int screenHeight = getHeight();
            
            double scaleX = (double) screenWidth / Slide.PAGE_WIDTH;
            double scaleY = (double) screenHeight / Slide.PAGE_HEIGHT;
            double scale = Math.min(scaleX, scaleY); // 取较小的比例以保证完整显示
            
            int scaledWidth = (int) (Slide.PAGE_WIDTH * scale);
            int scaledHeight = (int) (Slide.PAGE_HEIGHT * scale);
            
            int tx = (screenWidth - scaledWidth) / 2;
            int ty = (screenHeight - scaledHeight) / 2;

            // 3. 创建副本并应用变换
            Graphics2D g2dCopy = (Graphics2D) g2d.create();
            g2dCopy.translate(tx, ty);
            g2dCopy.scale(scale, scale);

            // 绘制白色页面背景
            g2dCopy.setColor(Color.WHITE);
            g2dCopy.fillRect(0, 0, Slide.PAGE_WIDTH, Slide.PAGE_HEIGHT);
            //设置剪裁区域
            g2dCopy.setClip(0, 0, Slide.PAGE_WIDTH, Slide.PAGE_HEIGHT);

            // 4. 绘制所有对象
            for (com.myppt.model.AbstractSlideObject object : currentSlide.getSlideObjects()) {
                object.draw(g2dCopy);
            }
            g2dCopy.dispose();
        }
    }
}