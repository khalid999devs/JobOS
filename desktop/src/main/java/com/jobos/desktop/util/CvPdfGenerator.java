package com.jobos.desktop.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

public class CvPdfGenerator {

    private static final float PAGE_WIDTH = 595.28f;
    private static final float PAGE_HEIGHT = 841.89f;
    private static final float MARGIN = 50;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    
    private static final float[] TEXT_RGB = {0.067f, 0.094f, 0.153f};
    private static final float[] SECONDARY_RGB = {0.216f, 0.255f, 0.318f};
    private static final float[] MUTED_RGB = {0.420f, 0.447f, 0.502f};
    private static final float[] BORDER_RGB = {0.898f, 0.906f, 0.922f};

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static File generatePdf(CVResponse cv, File outputFile) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDPageContentStream content = new PDPageContentStream(document, page);
            float yPosition = PAGE_HEIGHT - MARGIN;
            
            Map<String, Object> personalInfo = getPersonalInfoData(cv);
            yPosition = drawHeader(content, personalInfo, yPosition);
            if (cv.getSections() != null) {
                List<CVSectionResponse> sortedSections = cv.getSections().stream()
                    .filter(s -> s.getIsVisible() == null || s.getIsVisible())
                    .filter(s -> !"PERSONAL_INFO".equalsIgnoreCase(s.getSectionType()))
                    .sorted(Comparator.comparingInt(s -> s.getOrderIndex() != null ? s.getOrderIndex() : 0))
                    .toList();
                
                for (CVSectionResponse section : sortedSections) {
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
            
            drawFooter(content);
            
            content.close();
            document.save(outputFile);
        }
        
        return outputFile;
    }
    
    private static Map<String, Object> getPersonalInfoData(CVResponse cv) {
        if (cv.getSections() != null) {
            CVSectionResponse personalInfo = cv.getSections().stream()
                .filter(s -> "PERSONAL_INFO".equalsIgnoreCase(s.getSectionType()))
                .findFirst().orElse(null);
            if (personalInfo != null && personalInfo.getContent() != null) {
                try {
                    return objectMapper.readValue(personalInfo.getContent(), new TypeReference<>() {});
                } catch (Exception e) { }
            }
        }
        return Map.of();
    }
    
    private static float drawHeader(PDPageContentStream content, Map<String, Object> info, float yPosition) throws Exception {
        PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        String fullName = getStringVal(info, "fullName");
        String title = getStringVal(info, "title");
        String email = getStringVal(info, "email");
        String phone = getStringVal(info, "phone");
        String linkedIn = getStringVal(info, "linkedIn");
        String location = getStringVal(info, "location");
        if (location == null) location = getStringVal(info, "address");
        
        content.setFont(boldFont, 28);
        content.setNonStrokingColor(TEXT_RGB[0], TEXT_RGB[1], TEXT_RGB[2]);
        content.beginText();
        content.newLineAtOffset(MARGIN, yPosition);
        content.showText(sanitizeText(fullName != null ? fullName : "Your Name"));
        content.endText();
        yPosition -= 24;
        
        if (title != null) {
            content.setFont(regularFont, 12);
            content.setNonStrokingColor(SECONDARY_RGB[0], SECONDARY_RGB[1], SECONDARY_RGB[2]);
            content.beginText();
            content.newLineAtOffset(MARGIN, yPosition);
            content.showText(sanitizeText(title));
            content.endText();
            yPosition -= 20;
        }
        
        List<String[]> contacts = new ArrayList<>();
        if (phone != null) contacts.add(new String[]{"Phone", phone});
        if (linkedIn != null) contacts.add(new String[]{"LinkedIn", linkedIn});
        if (email != null) contacts.add(new String[]{"E-mail", email});
        if (location != null) contacts.add(new String[]{"Location", location});
        
        if (!contacts.isEmpty()) {
            yPosition -= 8;
            float xPos = MARGIN;
            float columnWidth = CONTENT_WIDTH / contacts.size();
            
            for (String[] contact : contacts) {
                content.setFont(boldFont, 9);
                content.setNonStrokingColor(TEXT_RGB[0], TEXT_RGB[1], TEXT_RGB[2]);
                content.beginText();
                content.newLineAtOffset(xPos, yPosition);
                content.showText(contact[0]);
                content.endText();
                
                content.setFont(regularFont, 9);
                content.setNonStrokingColor(SECONDARY_RGB[0], SECONDARY_RGB[1], SECONDARY_RGB[2]);
                content.beginText();
                content.newLineAtOffset(xPos, yPosition - 12);
                String val = contact[1].length() > 30 ? contact[1].substring(0, 27) + "..." : contact[1];
                content.showText(sanitizeText(val));
                content.endText();
                
                xPos += columnWidth;
            }
            yPosition -= 30;
        }
        
        yPosition -= 8;
        content.setStrokingColor(BORDER_RGB[0], BORDER_RGB[1], BORDER_RGB[2]);
        content.setLineWidth(0.5f);
        content.moveTo(MARGIN, yPosition);
        content.lineTo(PAGE_WIDTH - MARGIN, yPosition);
        content.stroke();
        
        return yPosition - 20;
    }
    
    private static float drawSection(PDPageContentStream content, CVSectionResponse section, float yPosition) throws Exception {
        PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        String title = section.getTitle() != null ? section.getTitle() : formatSectionType(section.getSectionType());
        content.setFont(boldFont, 14);
        content.setNonStrokingColor(TEXT_RGB[0], TEXT_RGB[1], TEXT_RGB[2]);
        content.beginText();
        content.newLineAtOffset(MARGIN, yPosition);
        content.showText(sanitizeText(title));
        content.endText();
        
        yPosition -= 4;
        
        content.setStrokingColor(TEXT_RGB[0], TEXT_RGB[1], TEXT_RGB[2]);
        content.setLineWidth(0.75f);
        content.moveTo(MARGIN, yPosition);
        content.lineTo(PAGE_WIDTH - MARGIN, yPosition);
        content.stroke();
        
        yPosition -= 14;
        
        String sectionContent = section.getContent();
        if (sectionContent != null && !sectionContent.isEmpty()) {
            yPosition = renderSectionContent(content, sectionContent, section.getSectionType(), yPosition, regularFont, boldFont);
        }
        
        return yPosition - 12;
    }
    
    private static float renderSectionContent(PDPageContentStream content, String sectionContent, 
            String sectionType, float yPosition, PDType1Font regularFont, PDType1Font boldFont) throws Exception {
        
        try {
            Map<String, Object> jsonContent = null;
            if (sectionContent.trim().startsWith("{")) {
                jsonContent = objectMapper.readValue(sectionContent, new TypeReference<>() {});
            }
            
            if (jsonContent != null) {
                String customContent = getStringVal(jsonContent, "customContent");
                if (customContent != null) {
                    content.setFont(regularFont, 10);
                    content.setNonStrokingColor(SECONDARY_RGB[0], SECONDARY_RGB[1], SECONDARY_RGB[2]);
                    List<String> lines = wrapText(sanitizeText(customContent), CONTENT_WIDTH, regularFont, 10);
                    for (String line : lines) {
                        if (yPosition < MARGIN + 50) break;
                        content.beginText();
                        content.newLineAtOffset(MARGIN, yPosition);
                        content.showText(line);
                        content.endText();
                        yPosition -= 14;
                    }
                    return yPosition;
                }
                
                yPosition = renderProfessionalContent(content, jsonContent, sectionType, yPosition, regularFont, boldFont);
            } else {
                // Plain text
                content.setFont(regularFont, 10);
                content.setNonStrokingColor(SECONDARY_RGB[0], SECONDARY_RGB[1], SECONDARY_RGB[2]);
                List<String> lines = wrapText(sanitizeText(sectionContent), CONTENT_WIDTH, regularFont, 10);
                for (String line : lines) {
                    if (yPosition < MARGIN + 50) break;
                    content.beginText();
                    content.newLineAtOffset(MARGIN, yPosition);
                    content.showText(line);
                    content.endText();
                    yPosition -= 14;
                }
            }
        } catch (Exception e) {
            content.setFont(regularFont, 10);
            content.setNonStrokingColor(SECONDARY_RGB[0], SECONDARY_RGB[1], SECONDARY_RGB[2]);
            List<String> lines = wrapText(sanitizeText(sectionContent), CONTENT_WIDTH, regularFont, 10);
            for (String line : lines) {
                if (yPosition < MARGIN + 50) break;
                content.beginText();
                content.newLineAtOffset(MARGIN, yPosition);
                content.showText(line);
                content.endText();
                yPosition -= 14;
            }
        }
        
        return yPosition;
    }
    
    private static float renderProfessionalContent(PDPageContentStream content, Map<String, Object> json, 
            String sectionType, float yPosition, PDType1Font regularFont, PDType1Font boldFont) throws Exception {
        
        if (sectionType == null) sectionType = "";
        String type = sectionType.toUpperCase();
        
        boolean isSummaryType = type.contains("SUMMARY") || type.contains("OBJECTIVE") || 
                               type.contains("PROFILE") || type.contains("ABOUT");
        
        if (isSummaryType) {
            String textContent = getStringVal(json, "title");
            if (textContent == null) textContent = getStringVal(json, "summary");
            if (textContent == null) textContent = getStringVal(json, "text");
            if (textContent == null) textContent = getStringVal(json, "content");
            if (textContent == null) textContent = getStringVal(json, "description");
            
            if (textContent != null) {
                content.setFont(regularFont, 10);
                content.setNonStrokingColor(SECONDARY_RGB[0], SECONDARY_RGB[1], SECONDARY_RGB[2]);
                List<String> lines = wrapText(sanitizeText(textContent), CONTENT_WIDTH, regularFont, 10);
                for (String line : lines) {
                    if (yPosition < MARGIN + 50) break;
                    content.beginText();
                    content.newLineAtOffset(MARGIN, yPosition);
                    content.showText(line);
                    content.endText();
                    yPosition -= 14;
                }
                return yPosition;
            }
        }
        
        boolean hasFieldValues = false;
        if (json.containsKey("fields") && json.get("fields") instanceof List) {
            List<?> fields = (List<?>) json.get("fields");
            for (Object field : fields) {
                String fieldName = field.toString();
                if (!fieldName.equals("customContent")) {
                    String val = getStringVal(json, fieldName);
                    if (val != null) {
                        hasFieldValues = true;
                        break;
                    }
                }
            }
        }
        
        if (!hasFieldValues) {
            if (json.containsKey("fields") && json.get("fields") instanceof List) {
                List<?> fields = (List<?>) json.get("fields");
                StringBuilder tagLine = new StringBuilder();
                for (Object field : fields) {
                    String fieldName = field.toString();
                    if (!fieldName.equals("customContent")) {
                        if (tagLine.length() > 0) tagLine.append("  |  ");
                        tagLine.append(formatFieldName(fieldName));
                    }
                }
                if (tagLine.length() > 0) {
                    content.setFont(regularFont, 9);
                    content.setNonStrokingColor(MUTED_RGB[0], MUTED_RGB[1], MUTED_RGB[2]);
                    content.beginText();
                    content.newLineAtOffset(MARGIN, yPosition);
                    content.showText(sanitizeText(tagLine.toString()));
                    content.endText();
                    yPosition -= 14;
                }
            }
            return yPosition;
        }
        
        switch (type) {
            case "EXPERIENCE", "CAREER_HISTORY", "WORK_EXPERIENCE" -> {
                yPosition = renderExperienceItem(content, json, yPosition, regularFont, boldFont);
            }
            case "EDUCATION", "EDUCATION_AND_EXECUTIVE_DEVELOPMENT" -> {
                yPosition = renderEducationItem(content, json, yPosition, regularFont, boldFont);
            }
            case "SKILLS", "CORE_COMPETENCIES", "KEY_ACHIEVEMENTS" -> {
                yPosition = renderSkillsItem(content, json, yPosition, regularFont, boldFont);
            }
            case "CERTIFICATIONS", "PROFESSIONAL_CERTIFICATIONS" -> {
                yPosition = renderCertificationItem(content, json, yPosition, regularFont, boldFont);
            }
            case "PROJECTS" -> {
                yPosition = renderProjectItem(content, json, yPosition, regularFont, boldFont);
            }
            default -> {
                yPosition = renderGenericItem(content, json, yPosition, regularFont, boldFont);
            }
        }
        
        return yPosition;
    }
    
    private static float renderExperienceItem(PDPageContentStream content, Map<String, Object> json, 
            float yPosition, PDType1Font regularFont, PDType1Font boldFont) throws Exception {
        String duration = getStringVal(json, "duration");
        if (duration == null) {
            String start = getStringVal(json, "startDate");
            String end = getStringVal(json, "endDate");
            if (start != null) duration = start + (end != null ? " - " + end : " - Present");
        }
        String jobTitle = getStringVal(json, "jobTitle");
        if (jobTitle == null) jobTitle = getStringVal(json, "title");
        String company = getStringVal(json, "company");
        String description = getStringVal(json, "description");
        
        StringBuilder line = new StringBuilder();
        if (duration != null) line.append(duration);
        if (jobTitle != null) {
            if (line.length() > 0) line.append("  |  ");
            line.append(jobTitle);
        }
        if (company != null) {
            if (line.length() > 0) line.append("  |  ");
            line.append(company);
        }
        
        if (line.length() > 0) {
            content.setFont(boldFont, 10);
            content.setNonStrokingColor(TEXT_RGB[0], TEXT_RGB[1], TEXT_RGB[2]);
            content.beginText();
            content.newLineAtOffset(MARGIN, yPosition);
            content.showText(sanitizeText(line.toString()));
            content.endText();
            yPosition -= 14;
        }
        
        if (description != null) {
            content.setFont(regularFont, 9);
            content.setNonStrokingColor(SECONDARY_RGB[0], SECONDARY_RGB[1], SECONDARY_RGB[2]);
            List<String> lines = wrapText(sanitizeText(description), CONTENT_WIDTH, regularFont, 9);
            for (String l : lines) {
                content.beginText();
                content.newLineAtOffset(MARGIN, yPosition);
                content.showText(l);
                content.endText();
                yPosition -= 12;
            }
        }
        
        return yPosition;
    }
    
    private static float renderEducationItem(PDPageContentStream content, Map<String, Object> json, 
            float yPosition, PDType1Font regularFont, PDType1Font boldFont) throws Exception {
        String year = getStringVal(json, "year");
        if (year == null) year = getStringVal(json, "graduationDate");
        String degree = getStringVal(json, "degree");
        String institution = getStringVal(json, "institution");
        
        StringBuilder line = new StringBuilder();
        if (year != null) line.append(year);
        if (degree != null) {
            if (line.length() > 0) line.append("  |  ");
            line.append(degree);
        }
        if (institution != null) {
            if (line.length() > 0) line.append("  |  ");
            line.append(institution);
        }
        
        if (line.length() > 0) {
            content.setFont(boldFont, 10);
            content.setNonStrokingColor(TEXT_RGB[0], TEXT_RGB[1], TEXT_RGB[2]);
            content.beginText();
            content.newLineAtOffset(MARGIN, yPosition);
            content.showText(sanitizeText(line.toString()));
            content.endText();
            yPosition -= 14;
        }
        
        return yPosition;
    }
    
    private static float renderSkillsItem(PDPageContentStream content, Map<String, Object> json, 
            float yPosition, PDType1Font regularFont, PDType1Font boldFont) throws Exception {
        if (json.containsKey("categories") && json.get("categories") instanceof List) {
            List<?> categories = (List<?>) json.get("categories");
            for (Object cat : categories) {
                content.setFont(regularFont, 9);
                content.setNonStrokingColor(SECONDARY_RGB[0], SECONDARY_RGB[1], SECONDARY_RGB[2]);
                content.beginText();
                content.newLineAtOffset(MARGIN, yPosition);
                content.showText("* " + sanitizeText(cat.toString()));
                content.endText();
                yPosition -= 12;
            }
        }
        return yPosition;
    }
    
    private static float renderCertificationItem(PDPageContentStream content, Map<String, Object> json, 
            float yPosition, PDType1Font regularFont, PDType1Font boldFont) throws Exception {
        String year = getStringVal(json, "year");
        String certName = getStringVal(json, "certName");
        if (certName == null) certName = getStringVal(json, "certification");
        String org = getStringVal(json, "issuingOrg");
        if (org == null) org = getStringVal(json, "organization");
        
        StringBuilder line = new StringBuilder();
        if (year != null) line.append(year);
        if (certName != null) {
            if (line.length() > 0) line.append("  |  ");
            line.append(certName);
        }
        if (org != null) {
            if (line.length() > 0) line.append("  |  ");
            line.append(org);
        }
        
        if (line.length() > 0) {
            content.setFont(boldFont, 10);
            content.setNonStrokingColor(TEXT_RGB[0], TEXT_RGB[1], TEXT_RGB[2]);
            content.beginText();
            content.newLineAtOffset(MARGIN, yPosition);
            content.showText(sanitizeText(line.toString()));
            content.endText();
            yPosition -= 14;
        }
        
        return yPosition;
    }
    
    private static float renderProjectItem(PDPageContentStream content, Map<String, Object> json, 
            float yPosition, PDType1Font regularFont, PDType1Font boldFont) throws Exception {
        String name = getStringVal(json, "projectName");
        if (name == null) name = getStringVal(json, "name");
        String tech = getStringVal(json, "technologies");
        String desc = getStringVal(json, "description");
        
        StringBuilder line = new StringBuilder();
        if (name != null) line.append(name);
        if (tech != null) {
            if (line.length() > 0) line.append("  |  ");
            line.append(tech);
        }
        
        if (line.length() > 0) {
            content.setFont(boldFont, 10);
            content.setNonStrokingColor(TEXT_RGB[0], TEXT_RGB[1], TEXT_RGB[2]);
            content.beginText();
            content.newLineAtOffset(MARGIN, yPosition);
            content.showText(sanitizeText(line.toString()));
            content.endText();
            yPosition -= 14;
        }
        
        if (desc != null) {
            content.setFont(regularFont, 9);
            content.setNonStrokingColor(SECONDARY_RGB[0], SECONDARY_RGB[1], SECONDARY_RGB[2]);
            List<String> lines = wrapText(sanitizeText(desc), CONTENT_WIDTH, regularFont, 9);
            for (String l : lines) {
                content.beginText();
                content.newLineAtOffset(MARGIN, yPosition);
                content.showText(l);
                content.endText();
                yPosition -= 12;
            }
        }
        
        return yPosition;
    }
    
    private static float renderGenericItem(PDPageContentStream content, Map<String, Object> json, 
            float yPosition, PDType1Font regularFont, PDType1Font boldFont) throws Exception {
        // Get fields and render their values
        if (json.containsKey("fields") && json.get("fields") instanceof List) {
            List<?> fields = (List<?>) json.get("fields");
            for (Object field : fields) {
                String val = getStringVal(json, field.toString());
                if (val != null) {
                    content.setFont(regularFont, 9);
                    content.setNonStrokingColor(SECONDARY_RGB[0], SECONDARY_RGB[1], SECONDARY_RGB[2]);
                    content.beginText();
                    content.newLineAtOffset(MARGIN, yPosition);
                    content.showText(sanitizeText(formatFieldName(field.toString()) + ": " + val));
                    content.endText();
                    yPosition -= 12;
                }
            }
        }
        return yPosition;
    }
    
    private static String getStringVal(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) != null) {
            String val = map.get(key).toString().trim();
            return val.isEmpty() ? null : val;
        }
        return null;
    }
    
    private static String formatFieldName(String field) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (i == 0) result.append(Character.toUpperCase(c));
            else if (Character.isUpperCase(c)) result.append(' ').append(c);
            else result.append(c);
        }
        return result.toString();
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
    
    private static String sanitizeText(String text) {
        if (text == null) return "";
        return text
            .replace("\r", "")
            .replace("\t", "    ")
            .replaceAll("[^\\x00-\\x7F]", "");
    }
    
    private static String formatSectionType(String type) {
        if (type == null) return "Section";
        String formatted = type.replace("_", " ");
        if (formatted.length() > 1) {
            return formatted.substring(0, 1).toUpperCase() + formatted.substring(1).toLowerCase();
        }
        return formatted;
    }
    
    public static WritableImage generateThumbnail(CVResponse cv, int width, int height) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Background - white
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
        
        double scale = (double) width / PAGE_WIDTH;
        double margin = Math.max(MARGIN * scale, 12);
        double contentWidth = width - 2 * margin;
        
        double yPos = margin;
        
        String headerColor = getHeaderColorForTemplate(cv.getTemplateName());
        gc.setFill(Color.web(headerColor));
        gc.fillRect(0, 0, width, 28);
        
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 11));
        gc.setTextAlign(TextAlignment.LEFT);
        String title = cv.getTitle() != null ? cv.getTitle() : "Curriculum Vitae";
        if (title.length() > 22) title = title.substring(0, 19) + "...";
        gc.fillText(title, margin, 18);
        
        yPos = 38;
        
        gc.setFill(Color.web("#374151"));
        gc.fillRoundRect(margin, yPos, contentWidth * 0.6, 8, 2, 2);
        yPos += 14;
        
        gc.setFill(Color.web("#D1D5DB"));
        gc.fillRoundRect(margin, yPos, contentWidth * 0.4, 4, 1, 1);
        yPos += 18;
        
        gc.setStroke(Color.web("#E5E7EB"));
        gc.setLineWidth(1);
        gc.strokeLine(margin, yPos, width - margin, yPos);
        yPos += 10;
        
        if (cv.getSections() != null) {
            List<CVSectionResponse> visibleSections = cv.getSections().stream()
                .filter(s -> s.getIsVisible() == null || s.getIsVisible())
                .sorted(Comparator.comparingInt(s -> s.getOrderIndex() != null ? s.getOrderIndex() : 0))
                .limit(4)
                .toList();
            
            for (CVSectionResponse section : visibleSections) {
                if (yPos > height - margin - 25) break;
                
                gc.setFill(Color.web(headerColor));
                gc.fillRoundRect(margin, yPos, contentWidth * 0.35, 6, 1, 1);
                yPos += 12;
                
                gc.setFill(Color.web("#F3F4F6"));
                int contentLines = Math.min(3, (int)((height - yPos - margin) / 8));
                for (int i = 0; i < contentLines; i++) {
                    double lineWidth = contentWidth * (0.95 - (i * 0.1));
                    gc.fillRoundRect(margin, yPos, lineWidth, 4, 1, 1);
                    yPos += 7;
                }
                
                yPos += 10;
            }
        } else {
            gc.setFill(Color.web("#E5E7EB"));
            gc.fillRoundRect(margin, yPos, contentWidth * 0.35, 6, 1, 1);
            yPos += 14;
            
            for (int i = 0; i < 5; i++) {
                gc.setFill(Color.web("#F3F4F6"));
                gc.fillRoundRect(margin, yPos, contentWidth * (0.9 - (i % 3) * 0.15), 4, 1, 1);
                yPos += 7;
            }
        }
        
        gc.setStroke(Color.web("#E5E7EB"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(0.5, 0.5, width - 1, height - 1, 4, 4);
        
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, null);
    }
    
    private static String getHeaderColorForTemplate(String templateName) {
        if (templateName == null) return "#0F766E";
        String lower = templateName.toLowerCase();
        if (lower.contains("creative")) return "#9333EA";
        if (lower.contains("modern")) return "#0284C7";
        if (lower.contains("simple") || lower.contains("minimal")) return "#374151";
        if (lower.contains("academic") || lower.contains("scholar")) return "#059669";
        if (lower.contains("executive") || lower.contains("elite")) return "#B45309";
        if (lower.contains("tech") || lower.contains("developer")) return "#0F766E";
        return "#0F766E";
    }
    
    public static WritableImage generateBlankThumbnail(int width, int height) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
        
        gc.setStroke(Color.web("#D1D5DB"));
        gc.setLineWidth(2);
        gc.setLineDashes(8, 4);
        gc.strokeRect(3, 3, width - 6, height - 6);
        
        gc.setLineDashes(0);
        
        double centerX = width / 2.0;
        double centerY = height / 2.0 - 12;
        double iconSize = Math.min(width, height) * 0.15;
        
        gc.setStroke(Color.web("#9CA3AF"));
        gc.setLineWidth(3);
        gc.strokeLine(centerX - iconSize, centerY, centerX + iconSize, centerY);
        gc.strokeLine(centerX, centerY - iconSize, centerX, centerY + iconSize);
        
        gc.setFill(Color.web("#6B7280"));
        gc.setFont(Font.font("System", FontWeight.MEDIUM, 11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Blank", centerX, centerY + 40);
        
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, null);
    }
    
    public static WritableImage generateTemplatePreview(String templateName, String category, int width, int height) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
        
        Color headerColor = getCategoryColor(category);
        gc.setFill(headerColor);
        gc.fillRect(0, 0, width, 35);
        
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 10));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(templateName != null ? templateName : "Template", 10, 22);
        
        double margin = 15;
        double yPos = 50;
        
        gc.setFill(Color.web("#374151"));
        gc.fillRect(margin, yPos, width * 0.5, 12);
        
        yPos += 22;
        
        gc.setFill(Color.web("#9CA3AF"));
        gc.fillRect(margin, yPos, width * 0.35, 6);
        
        yPos += 20;
        
        gc.setFill(headerColor.deriveColor(0, 1, 1, 0.7));
        gc.fillRect(margin, yPos, width * 0.25, 3);
        
        yPos += 15;
        
        gc.setFill(Color.web("#E5E7EB"));
        for (int i = 0; i < 4; i++) {
            double lineWidth = width - 2 * margin - (i % 2 == 0 ? 0 : 20);
            gc.fillRect(margin, yPos, lineWidth, 4);
            yPos += 10;
        }
        
        yPos += 10;
        
        gc.setFill(headerColor.deriveColor(0, 1, 1, 0.7));
        gc.fillRect(margin, yPos, width * 0.2, 3);
        
        yPos += 15;
        
        gc.setFill(Color.web("#E5E7EB"));
        for (int i = 0; i < 3; i++) {
            double lineWidth = width - 2 * margin - (i % 2 == 0 ? 10 : 0);
            gc.fillRect(margin, yPos, lineWidth, 4);
            yPos += 10;
        }
        
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
            case "PROFESSIONAL" -> Color.web("#1E40AF");
            case "CREATIVE" -> Color.web("#7C3AED");
            case "MODERN" -> Color.web("#0F766E");
            case "SIMPLE" -> Color.web("#374151");
            case "ACADEMIC" -> Color.web("#B45309");
            case "EXECUTIVE" -> Color.web("#0F172A");
            default -> Color.web("#0F766E");
        };
    }
}
