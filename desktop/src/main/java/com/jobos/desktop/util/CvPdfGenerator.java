package com.jobos.desktop.util;

import com.jobos.shared.dto.cv.CVResponse;
import com.jobos.shared.dto.cv.CVSectionResponse;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Generates PDF documents and thumbnails for CVs
 * Enterprise-grade CV document generation utility
 */
public class CvPdfGenerator {

    // A4 dimensions in points (72 points = 1 inch)
    private static final float PAGE_WIDTH = 595.28f;
    private static final float PAGE_HEIGHT = 841.89f;
    private static final float MARGIN = 50;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    
    // Primary theme color - teal
    private static final float[] PRIMARY_RGB = {0.059f, 0.463f, 0.431f}; // #0F766E
    private static final float[] TEXT_RGB = {0.067f, 0.094f, 0.153f}; // #111827
    private static final float[] SECONDARY_RGB = {0.420f, 0.447f, 0.502f}; // #6B7280
    private static final float[] BORDER_RGB = {0.898f, 0.906f, 0.922f}; // #E5E7EB

    /**
     * Generate a PDF file from CV data
     * @param cv The CV data to convert
     * @param outputFile The output file to write to
     * @return The generated file
     */
    public static File generatePdf(CVResponse cv, File outputFile) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDPageContentStream content = new PDPageContentStream(document, page);
            float yPosition = PAGE_HEIGHT - MARGIN;
            
            // Header section
            yPosition = drawHeader(content, cv, yPosition);
            
            // Draw sections
            if (cv.getSections() != null) {
                List<CVSectionResponse> sortedSections = cv.getSections().stream()
                    .filter(s -> s.getIsVisible() == null || s.getIsVisible())
                    .sorted(Comparator.comparingInt(s -> s.getOrderIndex() != null ? s.getOrderIndex() : 0))
                    .toList();
                
                for (CVSectionResponse section : sortedSections) {
                    // Check if we need a new page
                    if (yPosition < MARGIN + 100) {
                        content.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        yPosition = PAGE_HEIGHT - MARGIN;
                    }
                    
                    yPosition = drawSection(content, section, yPosition);
                }
            }
            
            // Footer
            drawFooter(content);
            
            content.close();
            document.save(outputFile);
        }
        
        return outputFile;
    }
    
    private static float drawHeader(PDPageContentStream content, CVResponse cv, float yPosition) throws Exception {
        PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        // CV Title - large and bold
        content.setFont(boldFont, 26);
        content.setNonStrokingColor(PRIMARY_RGB[0], PRIMARY_RGB[1], PRIMARY_RGB[2]);
        content.beginText();
        content.newLineAtOffset(MARGIN, yPosition);
        String title = sanitizeText(cv.getTitle() != null ? cv.getTitle() : "Curriculum Vitae");
        content.showText(title);
        content.endText();
        
        yPosition -= 30;
        
        // Template name - smaller subtitle
        content.setFont(regularFont, 11);
        content.setNonStrokingColor(SECONDARY_RGB[0], SECONDARY_RGB[1], SECONDARY_RGB[2]);
        content.beginText();
        content.newLineAtOffset(MARGIN, yPosition);
        String templateInfo = "Template: " + sanitizeText(cv.getTemplateName() != null ? cv.getTemplateName() : "Custom");
        content.showText(templateInfo);
        content.endText();
        
        yPosition -= 20;
        
        // Created date if available
        if (cv.getCreatedAt() != null) {
            content.setFont(regularFont, 9);
            content.beginText();
            content.newLineAtOffset(MARGIN, yPosition);
            content.showText("Created: " + cv.getCreatedAt().toString().substring(0, 10));
            content.endText();
            yPosition -= 15;
        }
        
        yPosition -= 5;
        
        // Divider line
        content.setStrokingColor(BORDER_RGB[0], BORDER_RGB[1], BORDER_RGB[2]);
        content.setLineWidth(1);
        content.moveTo(MARGIN, yPosition);
        content.lineTo(PAGE_WIDTH - MARGIN, yPosition);
        content.stroke();
        
        return yPosition - 25;
    }
    
    private static float drawSection(PDPageContentStream content, CVSectionResponse section, float yPosition) throws Exception {
        PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        // Section title
        String title = section.getTitle() != null ? section.getTitle() : formatSectionType(section.getSectionType());
        content.setFont(boldFont, 13);
        content.setNonStrokingColor(PRIMARY_RGB[0], PRIMARY_RGB[1], PRIMARY_RGB[2]);
        content.beginText();
        content.newLineAtOffset(MARGIN, yPosition);
        content.showText(sanitizeText(title.toUpperCase()));
        content.endText();
        
        yPosition -= 6;
        
        // Underline accent
        content.setStrokingColor(PRIMARY_RGB[0], PRIMARY_RGB[1], PRIMARY_RGB[2]);
        content.setLineWidth(2);
        content.moveTo(MARGIN, yPosition);
        content.lineTo(MARGIN + 80, yPosition);
        content.stroke();
        
        yPosition -= 18;
        
        // Section content
        String sectionContent = section.getContent();
        if (sectionContent != null && !sectionContent.isEmpty()) {
            content.setFont(regularFont, 11);
            content.setNonStrokingColor(TEXT_RGB[0], TEXT_RGB[1], TEXT_RGB[2]);
            
            // Word wrap the content
            List<String> lines = wrapText(sanitizeText(sectionContent), CONTENT_WIDTH, regularFont, 11);
            for (String line : lines) {
                if (yPosition < MARGIN + 50) {
                    break; // Would need new page (handled in caller)
                }
                content.beginText();
                content.newLineAtOffset(MARGIN, yPosition);
                content.showText(line);
                content.endText();
                yPosition -= 16;
            }
        }
        
        return yPosition - 20;
    }
    
    private static void drawFooter(PDPageContentStream content) throws Exception {
        PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        content.setFont(regularFont, 8);
        content.setNonStrokingColor(SECONDARY_RGB[0], SECONDARY_RGB[1], SECONDARY_RGB[2]);
        content.beginText();
        content.newLineAtOffset(MARGIN, 30);
        content.showText("Generated by JobOS");
        content.endText();
    }
    
    private static List<String> wrapText(String text, float maxWidth, PDType1Font font, float fontSize) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        
        String[] paragraphs = text.split("\n");
        
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                lines.add("");
                continue;
            }
            
            String[] words = paragraph.split("\\s+");
            StringBuilder currentLine = new StringBuilder();
            
            for (String word : words) {
                if (word.isEmpty()) continue;
                
                String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
                try {
                    float width = font.getStringWidth(testLine) / 1000 * fontSize;
                    if (width > maxWidth && currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    } else {
                        currentLine = new StringBuilder(testLine);
                    }
                } catch (Exception e) {
                    // If width calculation fails, just add the word
                    if (currentLine.length() > 0) {
                        currentLine.append(" ");
                    }
                    currentLine.append(word);
                }
            }
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        
        return lines;
    }
    
    /**
     * Sanitize text for PDF - remove characters not supported by PDF standard fonts
     */
    private static String sanitizeText(String text) {
        if (text == null) return "";
        // Replace common problematic characters
        return text
            .replace("\r", "")
            .replace("\t", "    ")
            // Remove or replace non-ASCII characters
            .replaceAll("[^\\x00-\\x7F]", "");
    }
    
    private static String formatSectionType(String type) {
        if (type == null) return "Section";
        // Convert EDUCATION -> Education, WORK_EXPERIENCE -> Work Experience
        String formatted = type.replace("_", " ");
        if (formatted.length() > 1) {
            return formatted.substring(0, 1).toUpperCase() + formatted.substring(1).toLowerCase();
        }
        return formatted;
    }
    
    /**
     * Generate a thumbnail image for CV preview using JavaFX Canvas
     * @param cv The CV data
     * @param width Thumbnail width in pixels
     * @param height Thumbnail height in pixels
     * @return WritableImage containing the thumbnail
     */
    public static WritableImage generateThumbnail(CVResponse cv, int width, int height) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Background - white with subtle shadow effect
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
        
        // Scale factor for A4 to thumbnail
        double scale = (double) width / PAGE_WIDTH;
        double margin = MARGIN * scale;
        double contentWidth = width - 2 * margin;
        
        double yPos = margin;
        
        // Header - CV Title
        gc.setFill(Color.web("#0F766E"));
        gc.setFont(Font.font("System", FontWeight.BOLD, Math.max(12 * scale, 10)));
        gc.setTextAlign(TextAlignment.LEFT);
        String title = cv.getTitle() != null ? cv.getTitle() : "Curriculum Vitae";
        if (title.length() > 25) title = title.substring(0, 22) + "...";
        gc.fillText(title, margin, yPos + 12 * scale);
        
        yPos += 22 * scale;
        
        // Template info - smaller text
        gc.setFill(Color.web("#6B7280"));
        gc.setFont(Font.font("System", Math.max(7 * scale, 6)));
        String templateName = cv.getTemplateName() != null ? cv.getTemplateName() : "Custom";
        gc.fillText(templateName, margin, yPos);
        
        yPos += 14 * scale;
        
        // Divider
        gc.setStroke(Color.web("#E5E7EB"));
        gc.setLineWidth(1);
        gc.strokeLine(margin, yPos, width - margin, yPos);
        
        yPos += 12 * scale;
        
        // Draw section previews (simplified)
        if (cv.getSections() != null) {
            List<CVSectionResponse> visibleSections = cv.getSections().stream()
                .filter(s -> s.getIsVisible() == null || s.getIsVisible())
                .sorted(Comparator.comparingInt(s -> s.getOrderIndex() != null ? s.getOrderIndex() : 0))
                .limit(4)
                .toList();
            
            for (CVSectionResponse section : visibleSections) {
                if (yPos > height - margin - 20) break;
                
                // Section title
                gc.setFill(Color.web("#0F766E"));
                gc.setFont(Font.font("System", FontWeight.BOLD, Math.max(8 * scale, 6)));
                String sectionTitle = section.getTitle() != null ? section.getTitle() : formatSectionType(section.getSectionType());
                if (sectionTitle.length() > 20) sectionTitle = sectionTitle.substring(0, 17) + "...";
                gc.fillText(sectionTitle.toUpperCase(), margin, yPos);
                
                yPos += 5 * scale;
                
                // Underline accent
                gc.setStroke(Color.web("#0F766E"));
                gc.setLineWidth(Math.max(1.5 * scale, 1));
                gc.strokeLine(margin, yPos, margin + 50 * scale, yPos);
                
                yPos += 8 * scale;
                
                // Content preview (truncated)
                String content = section.getContent();
                if (content != null && !content.isEmpty()) {
                    gc.setFill(Color.web("#374151"));
                    gc.setFont(Font.font("System", Math.max(6 * scale, 5)));
                    String preview = content.replace("\n", " ").replace("\r", "");
                    if (preview.length() > 60) preview = preview.substring(0, 57) + "...";
                    gc.fillText(preview, margin, yPos);
                    yPos += 6 * scale;
                }
                
                yPos += 14 * scale;
            }
        }
        
        // Border - subtle gray
        gc.setStroke(Color.web("#D1D5DB"));
        gc.setLineWidth(1);
        gc.strokeRect(0.5, 0.5, width - 1, height - 1);
        
        // Take snapshot
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, null);
    }
    
    /**
     * Generate a blank document placeholder thumbnail (like Google Docs)
     * @param width Thumbnail width in pixels
     * @param height Thumbnail height in pixels
     * @return WritableImage containing the blank thumbnail
     */
    public static WritableImage generateBlankThumbnail(int width, int height) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Background - white
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
        
        // Dashed border
        gc.setStroke(Color.web("#D1D5DB"));
        gc.setLineWidth(2);
        gc.setLineDashes(8, 4);
        gc.strokeRect(3, 3, width - 6, height - 6);
        
        // Reset dashes for plus icon
        gc.setLineDashes(0);
        
        // Plus icon - centered
        double centerX = width / 2.0;
        double centerY = height / 2.0 - 12;
        double iconSize = Math.min(width, height) * 0.15;
        
        gc.setStroke(Color.web("#9CA3AF"));
        gc.setLineWidth(3);
        gc.strokeLine(centerX - iconSize, centerY, centerX + iconSize, centerY);
        gc.strokeLine(centerX, centerY - iconSize, centerX, centerY + iconSize);
        
        // "Blank" text
        gc.setFill(Color.web("#6B7280"));
        gc.setFont(Font.font("System", FontWeight.MEDIUM, 11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Blank", centerX, centerY + 40);
        
        // Take snapshot
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, null);
    }
    
    /**
     * Generate a template preview thumbnail
     * @param templateName Name of the template
     * @param category Category color/style
     * @param width Thumbnail width
     * @param height Thumbnail height
     * @return WritableImage containing the template preview
     */
    public static WritableImage generateTemplatePreview(String templateName, String category, int width, int height) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
        
        // Category color header bar
        Color headerColor = getCategoryColor(category);
        gc.setFill(headerColor);
        gc.fillRect(0, 0, width, 35);
        
        // Template name in header
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 10));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(templateName != null ? templateName : "Template", 10, 22);
        
        // Simulate document structure
        double margin = 15;
        double yPos = 50;
        
        // Name placeholder
        gc.setFill(Color.web("#374151"));
        gc.fillRect(margin, yPos, width * 0.5, 12);
        
        yPos += 22;
        
        // Subtitle placeholder
        gc.setFill(Color.web("#9CA3AF"));
        gc.fillRect(margin, yPos, width * 0.35, 6);
        
        yPos += 20;
        
        // Section divider
        gc.setFill(headerColor.deriveColor(0, 1, 1, 0.7));
        gc.fillRect(margin, yPos, width * 0.25, 3);
        
        yPos += 15;
        
        // Content lines
        gc.setFill(Color.web("#E5E7EB"));
        for (int i = 0; i < 4; i++) {
            double lineWidth = width - 2 * margin - (i % 2 == 0 ? 0 : 20);
            gc.fillRect(margin, yPos, lineWidth, 4);
            yPos += 10;
        }
        
        yPos += 10;
        
        // Another section
        gc.setFill(headerColor.deriveColor(0, 1, 1, 0.7));
        gc.fillRect(margin, yPos, width * 0.2, 3);
        
        yPos += 15;
        
        // More content lines
        gc.setFill(Color.web("#E5E7EB"));
        for (int i = 0; i < 3; i++) {
            double lineWidth = width - 2 * margin - (i % 2 == 0 ? 10 : 0);
            gc.fillRect(margin, yPos, lineWidth, 4);
            yPos += 10;
        }
        
        // Border
        gc.setStroke(Color.web("#D1D5DB"));
        gc.setLineWidth(1);
        gc.strokeRect(0.5, 0.5, width - 1, height - 1);
        
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, null);
    }
    
    private static Color getCategoryColor(String category) {
        if (category == null) return Color.web("#0F766E");
        
        return switch (category.toUpperCase()) {
            case "PROFESSIONAL" -> Color.web("#1E40AF"); // Blue
            case "CREATIVE" -> Color.web("#7C3AED"); // Purple
            case "MODERN" -> Color.web("#0F766E"); // Teal
            case "SIMPLE" -> Color.web("#374151"); // Gray
            case "ACADEMIC" -> Color.web("#B45309"); // Amber
            case "EXECUTIVE" -> Color.web("#0F172A"); // Dark
            default -> Color.web("#0F766E");
        };
    }
}
