package com.myppt.controller;

import com.myppt.model.Presentation;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JOptionPane;

/**
 * 负责所有自动保存相关的功能。
 */
public class AutosaveManager {
    private final AppController controller;
    private Timer autosaveTimer;
    
    private static final String AUTOSAVE_DIR_NAME = ".autosave";
    private static final String AUTOSAVE_FILE_NAME = AUTOSAVE_DIR_NAME + File.separator + "current.myppt.tmp";
    private static final long AUTOSAVE_INTERVAL_MS = 2000; // 2 seconds

    public AutosaveManager(AppController controller) {
        this.controller = controller;
    }

    public void start() {
        autosaveTimer = new Timer(true); // Daemon thread
        autosaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                performAutosave();
            }
        }, AUTOSAVE_INTERVAL_MS, AUTOSAVE_INTERVAL_MS);
        System.out.println("自动保存开始计时，两秒一次。");
    }
    
    private void performAutosave() {
        File autosaveDir = new File(AUTOSAVE_DIR_NAME);
        if (!autosaveDir.exists()) {
            autosaveDir.mkdirs();
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(AUTOSAVE_FILE_NAME))) {
            oos.writeObject(controller.getPresentation());
        } catch (IOException e) {
            System.err.println("自动保存失败：" + e.getMessage());
        }
    }

    public void checkAndRestore() {
        File autosaveFile = new File(AUTOSAVE_FILE_NAME);
        if (autosaveFile.exists()) {
            int result = JOptionPane.showConfirmDialog(controller.getMainFrame(),
                "检测到上次意外关闭，是否恢复未保存的工作？", "自动恢复",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(autosaveFile))) {
                    Presentation restored = (Presentation) ois.readObject();
                    controller.setPresentation(restored);
                    controller.getMainFrame().getCanvasPanel().setPresentation(restored);
                    controller.getFileHandler().setDirty(true);
                    System.out.println("已从自动保存的备份文件夹回复原文件");
                } catch (IOException | ClassNotFoundException e) {
                    JOptionPane.showMessageDialog(controller.getMainFrame(), "自动恢复文件已损坏。", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
            deleteFile();
        }
    }
    
    public void deleteFile() {
        File autosaveFile = new File(AUTOSAVE_FILE_NAME);
        if (autosaveFile.exists() && autosaveFile.delete()) {
            System.out.println("已删除原自动保存备份文件");
            File autosaveDir = new File(AUTOSAVE_DIR_NAME);
            if (autosaveDir.exists() && autosaveDir.isDirectory() && autosaveDir.list().length == 0) {
                autosaveDir.delete();
            }
        }
    }
}