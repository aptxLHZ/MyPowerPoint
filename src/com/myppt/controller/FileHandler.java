package com.myppt.controller;

import com.myppt.model.Presentation;
import com.myppt.model.Slide;
import com.myppt.utils.PdfExporter;
import java.io.*;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * 负责处理所有文件I/O操作，如新建、打开、保存、导出等。
 */
public class FileHandler {
    private final AppController controller;
    private File currentFile = null;
    private boolean isDirty = false;

    public FileHandler(AppController controller) {
        this.controller = controller;
    }

    public void newFile() {
        if (!promptToSave()) return;

        controller.setPresentation(new Presentation());
        this.currentFile = null;
        setDirty(false);

        controller.getMainFrame().getCanvasPanel().setPresentation(controller.getPresentation());
        // [FIX] Removed call to non-existent UndoManager.clear() method
        controller.getUiUpdater().updateUI();
        SwingUtilities.invokeLater(controller::fitToWindow);
    }

    public boolean saveToFile() {
        if (currentFile == null) {
            return saveAsToFile();
        } else {
            return doSave(currentFile);
        }
    }

    public boolean saveAsToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("另存为...");
        fileChooser.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".myppt");
            }
            public String getDescription() {
                return "MyPPT 幻灯片 (*.myppt)";
            }
        });

        if (fileChooser.showSaveDialog(controller.getMainFrame()) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".myppt")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".myppt");
            }
            
            if (doSave(fileToSave)) {
                this.currentFile = fileToSave;
                controller.getUiUpdater().updateTitle();
                return true;
            }
        }
        return false;
    }

    private boolean doSave(File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(controller.getPresentation());
            setDirty(false);
            JOptionPane.showMessageDialog(controller.getMainFrame(), "保存成功！");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(controller.getMainFrame(), "保存失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public void openFromFile() {
        if (!promptToSave()) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("打开幻灯片");
        fileChooser.setFileFilter(new FileFilter() {
             public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".myppt");
            }
            public String getDescription() {
                return "MyPPT 幻灯片 (*.myppt)";
            }
        });

        if (fileChooser.showOpenDialog(controller.getMainFrame()) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(currentFile))) {
                Presentation loadedPresentation = (Presentation) ois.readObject();
                controller.setPresentation(loadedPresentation);
                setDirty(false);
                
                controller.getMainFrame().getCanvasPanel().setPresentation(loadedPresentation);
                // [FIX] Removed call to non-existent UndoManager.clear() method
                controller.getUiUpdater().updateUI();
                SwingUtilities.invokeLater(controller::fitToWindow);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(controller.getMainFrame(), "打开失败: 文件可能已损坏或格式不兼容。", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void exportToPdf() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出为PDF");
        fileChooser.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".pdf");
            }
            public String getDescription() {
                return "PDF 文件 (*.pdf)";
            }
        });

        if (fileChooser.showSaveDialog(controller.getMainFrame()) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
            }
            
            try (PDDocument document = new PDDocument()) {
                for (Slide slide : controller.getPresentation().getSlides()) {
                    PDRectangle pageSize = PDRectangle.A4;
                    PDPage page = new PDPage(new PDRectangle(pageSize.getHeight(), pageSize.getWidth()));
                    document.addPage(page);

                    PdfExporter exporter = new PdfExporter(document, page);
                    exporter.drawSlideToPdf(slide);
                    exporter.close();
                }
                document.save(fileToSave);
                JOptionPane.showMessageDialog(controller.getMainFrame(), "PDF导出成功！", "导出完成", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(controller.getMainFrame(), "PDF导出失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public boolean promptToSave() {
        if (!isDirty) return true;

        int result = JOptionPane.showConfirmDialog(
            controller.getMainFrame(), "当前文件已修改，是否保存？", "保存文件",
            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE
        );

        switch (result) {
            case JOptionPane.YES_OPTION: return saveToFile();
            case JOptionPane.NO_OPTION: return true;
            default: return false;
        }
    }

    public boolean isDirty() { return isDirty; }
    public void markAsDirty() {
        if (!this.isDirty) {
            this.isDirty = true;
            controller.getUiUpdater().updateTitle();
        }
    }
    public void setDirty(boolean dirty) {
        this.isDirty = dirty;
        controller.getUiUpdater().updateTitle();
    }
    public File getCurrentFile() { return currentFile; }
}