package com.myppt.utils;

import com.myppt.model.*;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;

import org.apache.pdfbox.util.Matrix;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JPanel;

public class PdfExporter {

    private PDDocument document;
    private PDPage currentPage;
    private PDPageContentStream contentStream;
    private PDRectangle mediaBox;

    /** PDFBox 3.x 的标准字体（必须是 PDFont） **/
    private PDFont helvetica;
    private PDFont helveticaBold;
    private PDFont helveticaOblique;
    private PDFont helveticaBoldOblique;
    private PDFont chineseFontBold;

    /** 中文字体（TrueType） **/
    private PDFont chineseFont;

    public PdfExporter(PDDocument document, PDPage page) throws IOException {
        this.document = document;
        this.currentPage = page;
        this.mediaBox = page.getMediaBox();

        this.contentStream = new PDPageContentStream(document, page);

        /** ———————— PDFBox 3.x 标准字体正确加载方式 ———————— **/
        this.helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        this.helveticaBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        this.helveticaOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        this.helveticaBoldOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);

        /** ———————— 中文字体加载 ———————— **/
        // ===== 更健壮的中文字体加载（复制到构造函数里） =====
        try {
            // 首先尝试加载你提供的 TTC（msyh / msyhbd）
            File fontNormal = new File("lib\\Deng.ttf");
            File fontBold   = new File("lib\\Dengb.ttf");

            if (fontNormal.exists()) {
                try {
                    // true -> allow subsetting (对 TTC 更稳妥)
                    this.chineseFont = PDType0Font.load(document, fontNormal);
                    System.out.println("中文字体加载成功（TTC）：" + fontNormal.getAbsolutePath());
                } catch (Exception e) {
                    System.err.println("使用 TTC 加载 msyh.ttc 失败: " + e.getMessage());
                    this.chineseFont = null;
                }
            }

            if (fontBold.exists()) {
                try {
                    this.chineseFontBold = PDType0Font.load(document, fontBold);
                    System.out.println("中文粗体加载成功（TTC）：" + fontBold.getAbsolutePath());
                } catch (Exception e) {
                    System.err.println("使用 TTC 加载 msyhbd.ttc 失败: " + e.getMessage());
                    this.chineseFontBold = null;
                }
            }

            // 如果 above 失败，尝试同目录是否有 ttf（例如你手动下载的 Noto/SourceHan）
            if (this.chineseFont == null) {
                File fallback = new File("D:\\MyPowerPoint\\lib\\NotoSansSC-Regular.ttf"); // 你可按后面步骤下载并放置
                if (fallback.exists()) {
                    this.chineseFont = PDType0Font.load(document, fallback);
                    System.out.println("中文字体加载成功（TTF 备用）：" + fallback.getAbsolutePath());
                }
            }

            if (this.chineseFont == null) {
                System.err.println("中文字体加载失败：未能加载有效字体文件（尝试 msyh.ttc / msyhbd.ttc / NotoSansSC-Regular.ttf）");
            }
        } catch (Exception e) {
            System.err.println("中文字体加载发生异常：" + e.getMessage());
        }
    }


    /** ————————————————————————————————————————————
     *  入口：绘制单页幻灯片
     * ————————————————————————————————————————————— **/
    public void drawSlideToPdf(Slide slide) throws IOException {
        float slideWidth = Slide.PAGE_WIDTH;
        float slideHeight = Slide.PAGE_HEIGHT;

        float pdfWidth = mediaBox.getWidth();
        float pdfHeight = mediaBox.getHeight();

        float scale = Math.min(pdfWidth / slideWidth, pdfHeight / slideHeight);

        float offsetX = (pdfWidth - slideWidth * scale) / 2;
        float offsetY = (pdfHeight - slideHeight * scale) / 2;

        contentStream.saveGraphicsState();
        contentStream.transform(new Matrix(scale, 0, 0, scale, offsetX, offsetY));

        // 背景
        contentStream.setNonStrokingColor(1.0f, 1.0f, 1.0f);
        contentStream.addRect(0, 0, slideWidth, slideHeight);
        contentStream.fill();

        for (AbstractSlideObject obj : slide.getSlideObjects()) {
            drawObject(obj);
        }

        contentStream.restoreGraphicsState();
    }


    private void drawObject(AbstractSlideObject object) throws IOException {
        if (object instanceof RectangleShape) drawRectangleShape((RectangleShape) object);
        if (object instanceof EllipseShape) drawEllipseShape((EllipseShape) object);
        if (object instanceof LineShape) drawLineShape((LineShape) object);
        if (object instanceof TextBox) drawTextBox((TextBox) object);
        if (object instanceof ImageObject) drawImageObject((ImageObject) object);
    }


    /** ———————————————— 绘制矩形 ———————————————— **/
    private void drawRectangleShape(RectangleShape rect) throws IOException {
        contentStream.saveGraphicsState();

        float x = rect.getX();
        float y = mediaBox.getHeight() - rect.getY() - rect.getHeight();

        if (rect.getFillColor() != null) {
            contentStream.setNonStrokingColor(toPdfColor(rect.getFillColor()));
            contentStream.addRect(x, y, rect.getWidth(), rect.getHeight());
            contentStream.fill();
        }

        if (rect.getBorderWidth() > 0) {
            contentStream.setStrokingColor(toPdfColor(rect.getBorderColor()));
            contentStream.setLineWidth((float) rect.getBorderWidth());
            contentStream.addRect(x, y, rect.getWidth(), rect.getHeight());
            contentStream.stroke();
        }

        contentStream.restoreGraphicsState();
    }


    /** ———————————————— 绘制椭圆 ———————————————— **/
    private void drawEllipseShape(EllipseShape ellipse) throws IOException {
        contentStream.saveGraphicsState();

        float x = ellipse.getX();
        float y = mediaBox.getHeight() - ellipse.getY() - ellipse.getHeight();

        drawEllipsePath(contentStream, x, y, ellipse.getWidth(), ellipse.getHeight());
        if (ellipse.getFillColor() != null) {
            contentStream.setNonStrokingColor(toPdfColor(ellipse.getFillColor()));
            contentStream.fill();
        }
        if (ellipse.getBorderWidth() > 0) {
            contentStream.setStrokingColor(toPdfColor(ellipse.getBorderColor()));
            contentStream.setLineWidth((float) ellipse.getBorderWidth());
            contentStream.stroke();
        }

        contentStream.restoreGraphicsState();
    }


    private void drawEllipsePath(PDPageContentStream cs, float x, float y, float w, float h) throws IOException {
        float k = 0.552284749831f;
        float ox = (w / 2) * k;
        float oy = (h / 2) * k;

        float xe = x + w;
        float ye = y + h;
        float xm = x + w / 2;
        float ym = y + h / 2;

        cs.moveTo(x, ym);
        cs.curveTo(x, ym - oy, xm - ox, y, xm, y);
        cs.curveTo(xm + ox, y, xe, ym - oy, xe, ym);
        cs.curveTo(xe, ym + oy, xm + ox, ye, xm, ye);
        cs.curveTo(xm - ox, ye, x, ym + oy, x, ym);
        cs.closePath();
    }


    /** ———————————————— 绘制线条 ———————————————— **/
    private void drawLineShape(LineShape line) throws IOException {
        contentStream.saveGraphicsState();

        contentStream.setStrokingColor(toPdfColor(line.getLineColor()));
        contentStream.setLineWidth(line.getStrokeWidth());

        contentStream.moveTo(line.x, mediaBox.getHeight() - line.y);
        contentStream.lineTo(line.x2, mediaBox.getHeight() - line.y2);
        contentStream.stroke();

        contentStream.restoreGraphicsState();
    }


    /** ———————————————— 绘制文本 ———————————————— **/
    private void drawTextBox(TextBox textBox) throws IOException {
        if (textBox.getText() == null || textBox.getText().isEmpty()) return;

        contentStream.setNonStrokingColor(toPdfColor(textBox.getTextColor()));

        /** 字体选择逻辑 **/
        PDFont font = helvetica;
        Font awtFont = textBox.getFont();

        if (awtFont.isBold() && awtFont.isItalic()) font = helveticaBoldOblique;
        else if (awtFont.isBold()) font = helveticaBold;
        else if (awtFont.isItalic()) font = helveticaOblique;

        /** 文本换行 **/
        FontMetrics fm = new JPanel().getFontMetrics(awtFont);

        float x = textBox.getX();
        float y = mediaBox.getHeight() - textBox.getY();

        for (String paragraph : textBox.getText().split("\n", -1)) {
            StringBuilder current = new StringBuilder();
            for (char c : paragraph.toCharArray()) {
                if (fm.stringWidth(current.toString() + c) > textBox.getBounds().width) {
                    drawTextLine(font, awtFont.getSize2D(), x, y, current.toString());
                    y -= awtFont.getSize2D();
                    current = new StringBuilder();
                }
                current.append(c);
            }
            drawTextLine(font, awtFont.getSize2D(), x, y, current.toString());
            y -= awtFont.getSize2D();
        }
    }

    private void drawTextLine(PDFont latinFont, float size, float x, float y, String text) throws IOException {

        // 自动切换中文字体
        PDFont fontToUse = containsChinese(text) ? chineseFont : latinFont;

        contentStream.beginText();
        contentStream.setFont(fontToUse, size);
        contentStream.newLineAtOffset(x, y - size);
        contentStream.showText(text);
        contentStream.endText();
    }

    private boolean containsChinese(String text) {
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) return true;
        }
        return false;
    }


    /** ———————————————— 绘制图片 ———————————————— **/
    private void drawImageObject(ImageObject imgObj) throws IOException {
        if (imgObj.getImagePath() == null) return;

        File img = new File(imgObj.getImagePath());
        if (!img.exists()) return;

        PDImageXObject image = PDImageXObject.createFromFile(img.getAbsolutePath(), document);

        contentStream.saveGraphicsState();

        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(imgObj.getOpacity());
        contentStream.setGraphicsStateParameters(gs);

        float x = imgObj.getX();
        float y = mediaBox.getHeight() - imgObj.getY() - imgObj.getBounds().height;

        contentStream.drawImage(image, x, y, imgObj.getBounds().width, imgObj.getBounds().height);

        contentStream.restoreGraphicsState();
    }


    private PDColor toPdfColor(Color c) {
        return new PDColor(new float[] {
                c.getRed() / 255f,
                c.getGreen() / 255f,
                c.getBlue() / 255f
        }, PDDeviceRGB.INSTANCE);
    }


    public void close() throws IOException {
        contentStream.close();
    }
}
