package com.t2drx.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;

public class PdfPageManager {
    private PDDocument document;
    private PDPage currentPage;
    private PDPageContentStream currentStream;
    private float currentY;
    private float margin;
    private float pageHeight;
    private float pageWidth;
    private float minY; // Minimum Y before page break
    private PDRectangle pageSize;

    private static final float DEFAULT_MARGIN = 50;
    private static final float DEFAULT_MIN_Y = 80; // Space at bottom before page break

    public PdfPageManager(PDDocument document) {
        this.document = document;
        this.margin = DEFAULT_MARGIN;
        this.minY = DEFAULT_MIN_Y;
        this.pageSize = PDRectangle.LETTER; // Standard letter size
        this.pageHeight = pageSize.getHeight();
        this.pageWidth = pageSize.getWidth();
        createNewPage();
    }

    public PdfPageManager(PDDocument document, PDRectangle pageSize) {
        this.document = document;
        this.pageSize = pageSize;
        this.pageHeight = pageSize.getHeight();
        this.pageWidth = pageSize.getWidth();
        this.margin = DEFAULT_MARGIN;
        this.minY = DEFAULT_MIN_Y;
        createNewPage();
    }

    private void createNewPage() {
        try {
            if (currentStream != null) {
                currentStream.close();
            }
            currentPage = new PDPage(pageSize);
            document.addPage(currentPage);
            currentStream = new PDPageContentStream(document, currentPage);
            currentY = pageHeight - margin;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create new page", e);
        }
    }

    public void checkAndCreateNewPageIfNeeded(float requiredSpace) throws IOException {
        if (currentY - requiredSpace < minY) {
            createNewPage();
        }
    }

    public void drawText(String text, PDFont font, float fontSize) throws IOException {
        checkAndCreateNewPageIfNeeded(fontSize + 5);

        currentStream.beginText();
        currentStream.newLineAtOffset(margin, currentY);
        currentStream.setFont(font, fontSize);
        currentStream.showText(text);
        currentStream.endText();

        currentY -= (fontSize + 5);
    }

    public void drawText(String text, float xOffset, PDFont font, float fontSize) throws IOException {
        checkAndCreateNewPageIfNeeded(fontSize + 5);

        currentStream.beginText();
        currentStream.newLineAtOffset(xOffset, currentY);
        currentStream.setFont(font, fontSize);
        currentStream.showText(text);
        currentStream.endText();

        currentY -= (fontSize + 5);
    }

    public void drawLine(float width) throws IOException {
        checkAndCreateNewPageIfNeeded(10);

        currentStream.setLineWidth(width);
        currentStream.moveTo(margin, currentY);
        currentStream.lineTo(margin + (pageWidth - 2 * margin), currentY);
        currentStream.stroke();

        currentY -= 10;
    }

    public void drawWrappedText(String text, float maxWidth, PDFont font, float fontSize, int maxLineLength) throws IOException {
        String[] lines = wrapText(text, maxLineLength);

        for (String line : lines) {
            checkAndCreateNewPageIfNeeded(fontSize + 4);

            currentStream.beginText();
            currentStream.newLineAtOffset(margin, currentY);
            currentStream.setFont(font, fontSize);
            currentStream.showText(line);
            currentStream.endText();

            currentY -= (fontSize + 4);
        }
    }

    public void addVerticalSpace(float space) throws IOException {
        checkAndCreateNewPageIfNeeded(space);
        currentY -= space;
    }

    public float getCurrentY() {
        return currentY;
    }

    public void setCurrentY(float y) {
        this.currentY = y;
    }

    public PDPageContentStream getCurrentStream() {
        return currentStream;
    }

    public void close() throws IOException {
        if (currentStream != null) {
            currentStream.close();
        }
    }

    public int getPageCount() {
        return document.getNumberOfPages();
    }

    private String[] wrapText(String text, int maxLineLength) {
        if (text.length() <= maxLineLength) {
            return new String[]{text};
        }

        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        java.util.List<String> lines = new java.util.ArrayList<>();

        for (String word : words) {
            if (line.length() + word.length() + 1 > maxLineLength) {
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder();
                }
            }
            if (line.length() > 0) {
                line.append(" ");
            }
            line.append(word);
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }

        return lines.toArray(new String[0]);
    }
}
