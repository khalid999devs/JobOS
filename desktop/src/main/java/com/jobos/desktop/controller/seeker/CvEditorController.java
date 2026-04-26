package com.jobos.desktop.controller.seeker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.Dialogs;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.CVService;
import com.jobos.desktop.util.CvPdfGenerator;
import com.jobos.shared.dto.cv.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
public class CvEditorController implements Initializable {
    @FXML private TextField titleField;
    @FXML private Label lastSavedLabel;
    @FXML private Label zoomLabel;
    @FXML private Button saveBtn;
    @FXML private Button exportBtn;
    @FXML private Button previewBtn;
    @FXML private VBox sectionsList;
    @FXML private Label templateLabel;
    @FXML private ScrollPane previewScrollPane;
    @FXML private VBox previewContent;
    @FXML private VBox propertiesPanel;
    @FXML private Label propertiesTitle;
    @FXML private VBox propertiesContent;
    @FXML private VBox loadingContainer;
    private final CVService cvService = new CVService();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String cvId;
    private CVResponse currentCV;
    private CVSectionResponse selectedSection;
    private boolean isDirty = false;
    private double zoomLevel = 1.0;
    private static final DataFormat SECTION_FORMAT = new DataFormat("application/x-cv-section");
    private static final String[] SECTION_TYPES = {
        "PERSONAL_INFO", "SUMMARY", "EXPERIENCE", "EDUCATION", 
        "SKILLS", "PROJECTS", "CERTIFICATIONS", "LANGUAGES", "REFERENCES", "CUSTOM"
    };
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupAutoSave();
        cvId = Router.getInstance().getParam("cvId");
        if (cvId != null) {
            loadCV();
        }
    }
    private void setupAutoSave() {
        titleField.textProperty().addListener((obs, old, newVal) -> {
            if (currentCV != null) {
                currentCV.setTitle(newVal);
                renderLivePreview();
                markDirty();
            }
        });
    }
    private void markDirty() {
        isDirty = true;
        lastSavedLabel.setText("Unsaved changes");
        lastSavedLabel.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 11px;");
    }
    private void markClean() {
        isDirty = false;
        lastSavedLabel.setText("All changes saved");
        lastSavedLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px;");
    }
    private void refreshCV() {
        cvService.getCVById(cvId).whenComplete((cv, error) -> {
            Platform.runLater(() -> {
                if (error != null) return;
                currentCV = cv;
                renderSections();
                renderLivePreview();
            });
        });
    }
    private void loadCV() {
        showLoading(true);
        cvService.getCVById(cvId).whenComplete((cv, error) -> {
            Platform.runLater(() -> {
                showLoading(false);
                if (error != null) {
                    Toast.error("Failed to load CV");
                    return;
                }
                currentCV = cv;
                populateEditor();
            });
        });
    }
    private void populateEditor() {
        titleField.setText(currentCV.getTitle());
        templateLabel.setText(currentCV.getTemplateName() != null ? currentCV.getTemplateName() : "Professional");
        renderSections();
        renderLivePreview();
        renderDefaultPropertiesPanel();
        markClean();
    }
    private void renderSections() {
        sectionsList.getChildren().clear();
        if (currentCV.getSections() == null || currentCV.getSections().isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(20));
            FontIcon icon = new FontIcon("fas-inbox");
            icon.setIconSize(24);
            icon.setIconColor(Color.web("#9CA3AF"));
            Label label = new Label("No sections yet");
            label.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
            Label hint = new Label("Click + to add");
            hint.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 10px;");
            emptyState.getChildren().addAll(icon, label, hint);
            sectionsList.getChildren().add(emptyState);
            return;
        }
        List<CVSectionResponse> sortedSections = currentCV.getSections().stream()
            .sorted(Comparator.comparingInt(s -> s.getOrderIndex() != null ? s.getOrderIndex() : 0))
            .collect(Collectors.toList());
        for (CVSectionResponse section : sortedSections) {
            sectionsList.getChildren().add(createSectionItem(section));
        }
    }
    private HBox createSectionItem(CVSectionResponse section) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 14, 12, 14));
        item.setMinHeight(44);
        boolean isSelected = selectedSection != null && selectedSection.getId().equals(section.getId());
        String baseStyle = isSelected 
            ? "-fx-background-color: #0F766E; -fx-background-radius: 8; -fx-cursor: hand;"
            : "-fx-background-color: #F3F4F6; -fx-background-radius: 8; -fx-cursor: hand;";
        item.setStyle(baseStyle);
        FontIcon dragHandle = new FontIcon("fas-grip-vertical");
        dragHandle.setIconSize(12);
        dragHandle.setIconColor(Color.web(isSelected ? "#99F6E4" : "#9CA3AF"));
        FontIcon sectionIcon = getSectionIcon(section.getSectionType());
        sectionIcon.setIconSize(14);
        sectionIcon.setIconColor(Color.web(isSelected ? "white" : "#0F766E"));
        VBox titleBox = new VBox(1);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label titleLabel = new Label(section.getTitle() != null ? section.getTitle() : formatSectionType(section.getSectionType()));
        titleLabel.setStyle("-fx-text-fill: " + (isSelected ? "white" : "#111827") + "; -fx-font-size: 13px; -fx-font-weight: 500;");
        titleLabel.setMaxWidth(160);
        titleLabel.setEllipsisString("...");
        titleBox.getChildren().add(titleLabel);
        FontIcon visibilityIcon = new FontIcon(section.getIsVisible() != null && section.getIsVisible() ? "fas-eye" : "fas-eye-slash");
        visibilityIcon.setIconSize(12);
        visibilityIcon.setIconColor(Color.web(section.getIsVisible() != null && section.getIsVisible() ? "#10B981" : "#9CA3AF"));
        visibilityIcon.setOnMouseClicked(e -> {
            e.consume();
            toggleSectionVisibility(section);
        });
        item.getChildren().addAll(dragHandle, sectionIcon, titleBox, visibilityIcon);
        item.setOnMouseClicked(e -> selectSection(section));
        if (!isSelected) {
            item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #E5E7EB; -fx-background-radius: 8; -fx-cursor: hand;"));
            item.setOnMouseExited(e -> item.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8; -fx-cursor: hand;"));
        }
        setupDragAndDrop(item, section);
        return item;
    }
    private void setupDragAndDrop(HBox item, CVSectionResponse section) {
        item.setOnDragDetected(event -> {
            Dragboard db = item.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(SECTION_FORMAT, section.getId());
            db.setContent(content);
            event.consume();
        });
        item.setOnDragOver(event -> {
            if (event.getGestureSource() != item && event.getDragboard().hasContent(SECTION_FORMAT)) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        item.setOnDragEntered(event -> {
            if (event.getGestureSource() != item && event.getDragboard().hasContent(SECTION_FORMAT)) {
                item.setStyle("-fx-background-color: #0F766E; -fx-background-radius: 6; -fx-cursor: hand;");
            }
        });
        item.setOnDragExited(event -> {
            boolean isSelected = selectedSection != null && selectedSection.getId().equals(section.getId());
            if (!isSelected) {
                item.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 6; -fx-cursor: hand;");
            }
        });
        item.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasContent(SECTION_FORMAT)) {
                String draggedId = (String) db.getContent(SECTION_FORMAT);
                reorderSections(draggedId, section.getId());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }
    private void reorderSections(String draggedId, String targetId) {
        List<CVSectionResponse> sections = new ArrayList<>(currentCV.getSections());
        CVSectionResponse draggedSection = sections.stream()
            .filter(s -> s.getId().equals(draggedId)).findFirst().orElse(null);
        CVSectionResponse targetSection = sections.stream()
            .filter(s -> s.getId().equals(targetId)).findFirst().orElse(null);
        if (draggedSection == null || targetSection == null) return;
        sections.remove(draggedSection);
        int targetIndex = sections.indexOf(targetSection);
        sections.add(targetIndex, draggedSection);
        List<String> newOrder = sections.stream().map(CVSectionResponse::getId).collect(Collectors.toList());
        cvService.reorderSections(cvId, newOrder).whenComplete((cv, error) -> {
            Platform.runLater(() -> {
                if (error != null) {
                    Toast.error("Failed to reorder sections");
                    return;
                }
                currentCV = cv;
                renderSections();
                renderLivePreview();
            });
        });
    }
    private FontIcon getSectionIcon(String type) {
        String iconCode = switch (type != null ? type.toUpperCase() : "") {
            case "PERSONAL_INFO" -> "fas-user";
            case "SUMMARY" -> "fas-align-left";
            case "EXPERIENCE" -> "fas-briefcase";
            case "EDUCATION" -> "fas-graduation-cap";
            case "SKILLS" -> "fas-tools";
            case "PROJECTS" -> "fas-project-diagram";
            case "CERTIFICATIONS" -> "fas-certificate";
            case "LANGUAGES" -> "fas-globe";
            case "REFERENCES" -> "fas-users";
            default -> "fas-list";
        };
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(12);
        return icon;
    }
    private String formatSectionType(String type) {
        if (type == null) return "Custom";
        return Arrays.stream(type.toLowerCase().split("_"))
            .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
            .collect(Collectors.joining(" "));
    }
    private void selectSection(CVSectionResponse section) {
        selectedSection = section;
        renderSections();
        renderPropertiesPanel(section);
    }
    private void renderLivePreview() {
        previewContent.getChildren().clear();
        previewContent.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 3);");
        previewContent.setPadding(new Insets(40, 45, 40, 45));
        previewContent.setScaleX(zoomLevel);
        previewContent.setScaleY(zoomLevel);
        if (currentCV == null) {
            Label placeholder = new Label("Your CV preview will appear here");
            placeholder.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 14px;");
            previewContent.getChildren().add(placeholder);
            return;
        }
        VBox header = createPreviewHeader();
        previewContent.getChildren().add(header);
        if (currentCV.getSections() != null) {
            List<CVSectionResponse> sortedSections = currentCV.getSections().stream()
                .filter(s -> s.getIsVisible() != null && s.getIsVisible())
                .sorted(Comparator.comparingInt(s -> s.getOrderIndex() != null ? s.getOrderIndex() : 0))
                .collect(Collectors.toList());
            for (CVSectionResponse section : sortedSections) {
                previewContent.getChildren().add(createPreviewSection(section));
            }
        }
        if (previewContent.getChildren().size() <= 1) {
            Label emptyLabel = new Label("Add sections to build your CV");
            emptyLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");
            VBox.setMargin(emptyLabel, new Insets(20, 0, 0, 0));
            previewContent.getChildren().add(emptyLabel);
        }
    }
    private VBox createPreviewHeader() {
        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 16, 0));
        String fullName = null, title = null, email = null, phone = null, linkedIn = null, location = null;
        if (currentCV.getSections() != null) {
            CVSectionResponse personalInfo = currentCV.getSections().stream()
                .filter(s -> "PERSONAL_INFO".equalsIgnoreCase(s.getSectionType()))
                .findFirst().orElse(null);
            if (personalInfo != null && personalInfo.getContent() != null) {
                try {
                    Map<String, Object> content = objectMapper.readValue(personalInfo.getContent(), new TypeReference<>() {});
                    fullName = getStringValue(content, "fullName");
                    title = getStringValue(content, "title");
                    email = getStringValue(content, "email");
                    phone = getStringValue(content, "phone");
                    linkedIn = getStringValue(content, "linkedIn");
                    location = getStringValue(content, "location");
                    if (location == null) location = getStringValue(content, "address");
                } catch (Exception e) { }
            }
        }
        Label nameLabel = new Label(fullName != null ? fullName : "Your Name");
        nameLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 32px; -fx-font-weight: 700;");
        nameLabel.setWrapText(true);
        header.getChildren().add(nameLabel);
        if (title != null) {
            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px; -fx-font-weight: 400;");
            header.getChildren().add(titleLabel);
        }
        HBox contactRow = new HBox(20);
        contactRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(contactRow, new Insets(8, 0, 0, 0));
        if (phone != null) {
            contactRow.getChildren().add(createContactLabel("Phone", phone));
        }
        if (linkedIn != null) {
            contactRow.getChildren().add(createContactLabel("LinkedIn", linkedIn));
        }
        if (email != null) {
            contactRow.getChildren().add(createContactLabel("E-mail", email));
        }
        if (location != null) {
            contactRow.getChildren().add(createContactLabel("Location", location));
        }
        if (!contactRow.getChildren().isEmpty()) {
            header.getChildren().add(contactRow);
        }
        Region separator = new Region();
        separator.setStyle("-fx-background-color: #E5E7EB; -fx-min-height: 1; -fx-max-height: 1;");
        VBox.setMargin(separator, new Insets(8, 0, 0, 0));
        header.getChildren().add(separator);
        return header;
    }
    private String getStringValue(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) != null) {
            String val = map.get(key).toString().trim();
            return val.isEmpty() ? null : val;
        }
        return null;
    }
    private VBox createContactLabel(String label, String value) {
        VBox item = new VBox(0);
        Label labelText = new Label(label);
        labelText.setStyle("-fx-text-fill: #111827; -fx-font-size: 10px; -fx-font-weight: 600;");
        Label valueText = new Label(value);
        valueText.setStyle("-fx-text-fill: #374151; -fx-font-size: 10px;");
        item.getChildren().addAll(labelText, valueText);
        return item;
    }
    private VBox createPreviewSection(CVSectionResponse section) {
        VBox sectionBox = new VBox(4);
        VBox.setMargin(sectionBox, new Insets(10, 0, 0, 0));
        if ("PERSONAL_INFO".equalsIgnoreCase(section.getSectionType())) {
            return sectionBox;
        }
        boolean isSelected = selectedSection != null && selectedSection.getId().equals(section.getId());
        if (isSelected) {
            sectionBox.setStyle("-fx-background-color: #FAFAFA; -fx-padding: 8; -fx-background-radius: 4; -fx-border-color: #0F766E; -fx-border-width: 1; -fx-border-radius: 4;");
        }
        Label titleLabel = new Label(section.getTitle() != null ? section.getTitle() : formatSectionType(section.getSectionType()));
        titleLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 16px; -fx-font-weight: 700;");
        Region underline = new Region();
        underline.setStyle("-fx-background-color: #111827; -fx-min-height: 1; -fx-max-height: 1;");
        VBox.setMargin(underline, new Insets(2, 0, 8, 0));
        sectionBox.getChildren().addAll(titleLabel, underline);
        VBox contentBox = renderSectionContent(section);
        sectionBox.getChildren().add(contentBox);
        sectionBox.setOnMouseClicked(e -> selectSection(section));
        sectionBox.setCursor(javafx.scene.Cursor.HAND);
        return sectionBox;
    }
    private VBox renderSectionContent(CVSectionResponse section) {
        VBox contentBox = new VBox(4);
        String content = section.getContent();
        if (content == null || content.trim().isEmpty()) {
            Label emptyLabel = new Label("Click to add content...");
            emptyLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px; -fx-font-style: italic;");
            contentBox.getChildren().add(emptyLabel);
            return contentBox;
        }
        try {
            Map<String, Object> jsonContent = null;
            if (content.trim().startsWith("{")) {
                jsonContent = objectMapper.readValue(content, new TypeReference<>() {});
            }
            if (jsonContent != null) {
                String customContent = getStringValue(jsonContent, "customContent");
                if (customContent != null) {
                    Label textLabel = new Label(customContent);
                    textLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px; -fx-line-spacing: 2;");
                    textLabel.setWrapText(true);
                    contentBox.getChildren().add(textLabel);
                    return contentBox;
                }
                String sectionType = section.getSectionType();
                contentBox.getChildren().add(renderProfessionalContent(jsonContent, sectionType));
            } else if (content.trim().startsWith("[")) {
                List<Map<String, Object>> jsonList = objectMapper.readValue(content, new TypeReference<>() {});
                for (int i = 0; i < jsonList.size(); i++) {
                    if (i > 0) {
                        Region spacer = new Region();
                        spacer.setMinHeight(6);
                        contentBox.getChildren().add(spacer);
                    }
                    contentBox.getChildren().add(renderProfessionalContent(jsonList.get(i), section.getSectionType()));
                }
            } else {
                Label textLabel = new Label(content);
                textLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px;");
                textLabel.setWrapText(true);
                contentBox.getChildren().add(textLabel);
            }
        } catch (Exception e) {
            Label textLabel = new Label(content);
            textLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px;");
            textLabel.setWrapText(true);
            contentBox.getChildren().add(textLabel);
        }
        return contentBox;
    }
    private VBox renderProfessionalContent(Map<String, Object> json, String sectionType) {
        VBox box = new VBox(2);
        boolean hasValues = false;
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("fields") && !key.equals("categories") && !key.equals("style") &&
                !key.equals("type") && !key.equals("layout") && !key.equals("displayStyle") &&
                !key.equals("placeholder") && !key.equals("maxLength") && !key.equals("customContent") &&
                !key.equals("includePhoto") && entry.getValue() != null &&
                !entry.getValue().toString().trim().isEmpty()) {
                hasValues = true;
                break;
            }
        }
        String upperType = sectionType != null ? sectionType.toUpperCase() : "";
        boolean isSummaryType = upperType.contains("SUMMARY") || upperType.contains("OBJECTIVE") || 
                               upperType.contains("PROFILE") || upperType.contains("ABOUT");
        String titleVal = getStringValue(json, "title");
        if (isSummaryType && titleVal != null) {
            Label titleLabel = new Label(titleVal);
            titleLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px; -fx-line-spacing: 2;");
            titleLabel.setWrapText(true);
            box.getChildren().add(titleLabel);
            return box;
        }
        if (!hasValues) {
            if (json.containsKey("fields") && json.get("fields") instanceof List) {
                FlowPane fieldsPane = new FlowPane();
                fieldsPane.setHgap(6);
                fieldsPane.setVgap(4);
                for (Object field : (List<?>) json.get("fields")) {
                    Label tag = new Label(formatFieldName(field.toString()));
                    tag.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #9CA3AF; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10px;");
                    fieldsPane.getChildren().add(tag);
                }
                box.getChildren().add(fieldsPane);
            } else if (json.containsKey("categories") && json.get("categories") instanceof List) {
                for (Object cat : (List<?>) json.get("categories")) {
                    Label catLabel = new Label("• " + cat.toString());
                    catLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
                    box.getChildren().add(catLabel);
                }
            } else if (json.containsKey("placeholder")) {
                Label placeholder = new Label(json.get("placeholder").toString());
                placeholder.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px; -fx-font-style: italic;");
                placeholder.setWrapText(true);
                box.getChildren().add(placeholder);
            }
            return box;
        }
        switch (sectionType != null ? sectionType.toUpperCase() : "") {
            case "EXPERIENCE", "CAREER_HISTORY", "WORK_EXPERIENCE" -> renderExperienceItem(box, json);
            case "EDUCATION", "EDUCATION_AND_EXECUTIVE_DEVELOPMENT" -> renderEducationItem(box, json);
            case "SKILLS", "CORE_COMPETENCIES", "KEY_ACHIEVEMENTS" -> renderSkillsItem(box, json);
            case "CERTIFICATIONS", "PROFESSIONAL_CERTIFICATIONS" -> renderCertificationItem(box, json);
            case "PROJECTS" -> renderProjectItem(box, json);
            case "LANGUAGES" -> renderLanguageItem(box, json);
            case "SUMMARY", "EXECUTIVE_SUMMARY", "PROFESSIONAL_SUMMARY", "OBJECTIVE", "PROFILE" -> renderSummaryItem(box, json);
            default -> renderGenericItem(box, json);
        }
        return box;
    }
    private void renderExperienceItem(VBox box, Map<String, Object> json) {
        HBox mainRow = new HBox(16);
        String duration = getStringValue(json, "duration");
        String startDate = getStringValue(json, "startDate");
        String endDate = getStringValue(json, "endDate");
        String dateText = duration != null ? duration : 
                         (startDate != null ? startDate + " - " + (endDate != null ? endDate : "present") : null);
        if (dateText != null) {
            Label dateLabel = new Label(dateText);
            dateLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 10px; -fx-min-width: 80;");
            mainRow.getChildren().add(dateLabel);
        }
        VBox details = new VBox(2);
        String jobTitle = getStringValue(json, "jobTitle");
        if (jobTitle == null) jobTitle = getStringValue(json, "title");
        if (jobTitle != null) {
            Label titleLabel = new Label(jobTitle);
            titleLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 12px; -fx-font-weight: 600;");
            details.getChildren().add(titleLabel);
        }
        String company = getStringValue(json, "company");
        String location = getStringValue(json, "location");
        if (company != null) {
            String companyText = company + (location != null ? ", " + location : "");
            Label companyLabel = new Label(companyText);
            companyLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px; -fx-font-style: italic;");
            details.getChildren().add(companyLabel);
        }
        String responsibilities = getStringValue(json, "responsibilities");
        String description = getStringValue(json, "description");
        String text = responsibilities != null ? responsibilities : description;
        if (text != null) {
            Label descLabel = new Label("• " + text);
            descLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 10px;");
            descLabel.setWrapText(true);
            details.getChildren().add(descLabel);
        }
        mainRow.getChildren().add(details);
        HBox.setHgrow(details, Priority.ALWAYS);
        box.getChildren().add(mainRow);
    }
    private void renderEducationItem(VBox box, Map<String, Object> json) {
        HBox mainRow = new HBox(16);
        String year = getStringValue(json, "graduationDate");
        if (year == null) year = getStringValue(json, "year");
        if (year != null) {
            Label dateLabel = new Label(year);
            dateLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 10px; -fx-min-width: 80;");
            mainRow.getChildren().add(dateLabel);
        }
        VBox details = new VBox(2);
        String degree = getStringValue(json, "degree");
        if (degree != null) {
            Label degreeLabel = new Label(degree);
            degreeLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 12px; -fx-font-weight: 600;");
            details.getChildren().add(degreeLabel);
        }
        String institution = getStringValue(json, "institution");
        if (institution != null) {
            Label instLabel = new Label(institution);
            instLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px; -fx-font-style: italic;");
            details.getChildren().add(instLabel);
        }
        String gpa = getStringValue(json, "gpa");
        if (gpa != null) {
            Label gpaLabel = new Label("GPA: " + gpa);
            gpaLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 10px;");
            details.getChildren().add(gpaLabel);
        }
        mainRow.getChildren().add(details);
        HBox.setHgrow(details, Priority.ALWAYS);
        box.getChildren().add(mainRow);
    }
    private void renderSkillsItem(VBox box, Map<String, Object> json) {
        if (json.containsKey("categories") && json.get("categories") instanceof List) {
            for (Object cat : (List<?>) json.get("categories")) {
                HBox skillRow = new HBox(12);
                skillRow.setAlignment(Pos.CENTER_LEFT);
                Label skillLabel = new Label(cat.toString());
                skillLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px; -fx-min-width: 150;");
                Region progressBg = new Region();
                progressBg.setStyle("-fx-background-color: #E5E7EB; -fx-min-height: 6; -fx-max-height: 6; -fx-background-radius: 3;");
                progressBg.setPrefWidth(100);
                skillRow.getChildren().addAll(skillLabel, progressBg);
                box.getChildren().add(skillRow);
            }
        } else {
            renderGenericItem(box, json);
        }
    }
    private void renderCertificationItem(VBox box, Map<String, Object> json) {
        HBox mainRow = new HBox(16);
        String date = getStringValue(json, "issueDate");
        if (date == null) date = getStringValue(json, "year");
        if (date != null) {
            Label dateLabel = new Label(date);
            dateLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 10px; -fx-min-width: 80;");
            mainRow.getChildren().add(dateLabel);
        }
        String certName = getStringValue(json, "certName");
        if (certName == null) certName = getStringValue(json, "certification");
        String org = getStringValue(json, "issuingOrg");
        if (org == null) org = getStringValue(json, "organization");
        String certText = certName != null ? certName : "";
        if (org != null) certText += " (" + org + ")";
        if (!certText.isEmpty()) {
            Label certLabel = new Label(certText);
            certLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px;");
            mainRow.getChildren().add(certLabel);
        }
        box.getChildren().add(mainRow);
    }
    private void renderProjectItem(VBox box, Map<String, Object> json) {
        String name = getStringValue(json, "projectName");
        if (name == null) name = getStringValue(json, "name");
        if (name != null) {
            Label nameLabel = new Label(name);
            nameLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 12px; -fx-font-weight: 600;");
            box.getChildren().add(nameLabel);
        }
        String desc = getStringValue(json, "description");
        if (desc != null) {
            Label descLabel = new Label(desc);
            descLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 10px;");
            descLabel.setWrapText(true);
            box.getChildren().add(descLabel);
        }
        String tech = getStringValue(json, "technologies");
        if (tech != null) {
            Label techLabel = new Label("Technologies: " + tech);
            techLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 10px;");
            box.getChildren().add(techLabel);
        }
    }
    private void renderLanguageItem(VBox box, Map<String, Object> json) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        String language = getStringValue(json, "language");
        String proficiency = getStringValue(json, "proficiency");
        if (language != null) {
            Label langLabel = new Label(language);
            langLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px; -fx-min-width: 100;");
            row.getChildren().add(langLabel);
        }
        if (proficiency != null) {
            Label profLabel = new Label(proficiency);
            profLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 10px;");
            row.getChildren().add(profLabel);
        }
        box.getChildren().add(row);
    }
    private void renderSummaryItem(VBox box, Map<String, Object> json) {
        String content = getStringValue(json, "title");
        if (content == null) content = getStringValue(json, "summary");
        if (content == null) content = getStringValue(json, "text");
        if (content == null) content = getStringValue(json, "content");
        if (content == null) content = getStringValue(json, "description");
        if (content != null) {
            Label textLabel = new Label(content);
            textLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px; -fx-line-spacing: 2;");
            textLabel.setWrapText(true);
            box.getChildren().add(textLabel);
        }
    }
    private void renderGenericItem(VBox box, Map<String, Object> json) {
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("fields") && !key.equals("categories") && !key.equals("style") &&
                !key.equals("type") && !key.equals("layout") && !key.equals("displayStyle") &&
                !key.equals("placeholder") && !key.equals("maxLength") && !key.equals("customContent") &&
                !key.equals("includePhoto") && entry.getValue() != null) {
                String value = entry.getValue().toString().trim();
                if (!value.isEmpty()) {
                    Label label = new Label(value);
                    label.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px;");
                    label.setWrapText(true);
                    box.getChildren().add(label);
                }
            }
        }
    }
    private String formatFieldName(String field) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (i == 0) {
                result.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                result.append(' ').append(c);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    private void renderPropertiesPanel(CVSectionResponse section) {
        propertiesContent.getChildren().clear();
        propertiesTitle.setText("Edit: " + formatSectionType(section.getSectionType()));
        VBox titleGroup = createFormGroup("Section Title", section.getTitle(), "Enter section title...");
        TextField sectionTitleField = (TextField) titleGroup.getChildren().get(1);
        Map<String, Object> contentJson = parseContentJson(section.getContent());
        VBox propertiesSection = createPropertiesInfoSection(contentJson);
        VBox fieldsSection = createDynamicFieldsSection(contentJson, section);
        VBox customContentSection = createCustomContentSection(contentJson, section);
        HBox visibilityRow = createVisibilityToggle(section);
        HBox actions = new HBox(8);
        actions.setPadding(new Insets(16, 0, 0, 0));
        Button updateBtn = new Button("Save Changes");
        updateBtn.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;");
        updateBtn.setOnAction(e -> {
            String originalText = updateBtn.getText();
            updateBtn.setText("Saving...");
            updateBtn.setDisable(true);
            updateBtn.setStyle("-fx-background-color: #6B7280; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6; -fx-font-size: 12px;");
            String newContent = buildContentJson(fieldsSection, customContentSection, contentJson);
            updateSectionWithCallback(section.getId(), sectionTitleField.getText(), newContent, 
                section.getIsVisible() != null && section.getIsVisible(), updateBtn, originalText);
        });
        HBox.setHgrow(updateBtn, Priority.ALWAYS);
        updateBtn.setMaxWidth(Double.MAX_VALUE);
        Button deleteBtn = new Button();
        deleteBtn.setGraphic(new FontIcon("fas-trash"));
        ((FontIcon)deleteBtn.getGraphic()).setIconSize(12);
        ((FontIcon)deleteBtn.getGraphic()).setIconColor(Color.web("#EF4444"));
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #EF4444; -fx-border-radius: 6; -fx-padding: 8; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deleteSection(section.getId()));
        actions.getChildren().addAll(updateBtn, deleteBtn);
        propertiesContent.getChildren().addAll(
            titleGroup,
            new Separator(),
            propertiesSection,
            new Separator(),
            fieldsSection,
            customContentSection,
            new Separator(),
            visibilityRow,
            actions
        );
        sectionTitleField.textProperty().addListener((obs, old, newVal) -> {
            section.setTitle(newVal);
            renderSections();
            renderLivePreview();
            markDirty();
        });
    }
    private Map<String, Object> parseContentJson(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            if (content.trim().startsWith("{")) {
                return objectMapper.readValue(content, new TypeReference<>() {});
            }
        } catch (Exception e) {
        }
        return new HashMap<>();
    }
    private VBox createPropertiesInfoSection(Map<String, Object> contentJson) {
        VBox section = new VBox(8);
        Label header = new Label("PROPERTIES");
        header.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 10px; -fx-font-weight: 600;");
        section.getChildren().add(header);
        if (contentJson.containsKey("style")) {
            addPropertyRow(section, "Style", contentJson.get("style").toString());
        }
        if (contentJson.containsKey("type")) {
            addPropertyRow(section, "Type", contentJson.get("type").toString());
        }
        if (contentJson.containsKey("layout")) {
            addPropertyRow(section, "Layout", contentJson.get("layout").toString());
        }
        if (contentJson.containsKey("displayStyle")) {
            addPropertyRow(section, "Display", contentJson.get("displayStyle").toString());
        }
        if (contentJson.containsKey("categories") && contentJson.get("categories") instanceof List) {
            List<?> categories = (List<?>) contentJson.get("categories");
            FlowPane catPane = new FlowPane();
            catPane.setHgap(4);
            catPane.setVgap(4);
            for (Object cat : categories) {
                Label tag = new Label(cat.toString());
                tag.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10px;");
                catPane.getChildren().add(tag);
            }
            VBox catBox = new VBox(4);
            Label catLabel = new Label("Categories");
            catLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 10px;");
            catBox.getChildren().addAll(catLabel, catPane);
            section.getChildren().add(catBox);
        }
        if (contentJson.containsKey("fields") && contentJson.get("fields") instanceof List) {
            List<?> fields = (List<?>) contentJson.get("fields");
            FlowPane fieldsPane = new FlowPane();
            fieldsPane.setHgap(4);
            fieldsPane.setVgap(4);
            for (Object field : fields) {
                Label tag = new Label(formatFieldName(field.toString()));
                tag.setStyle("-fx-background-color: #E0E7FF; -fx-text-fill: #4338CA; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10px;");
                fieldsPane.getChildren().add(tag);
            }
            VBox fieldsBox = new VBox(4);
            Label fieldsLabel = new Label("Fields");
            fieldsLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 10px;");
            fieldsBox.getChildren().addAll(fieldsLabel, fieldsPane);
            section.getChildren().add(fieldsBox);
        }
        return section;
    }
    private void addPropertyRow(VBox container, String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label keyLabel = new Label(label + ":");
        keyLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px; -fx-min-width: 60;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 11px; -fx-font-weight: 500;");
        row.getChildren().addAll(keyLabel, valueLabel);
        container.getChildren().add(row);
    }
    private VBox createDynamicFieldsSection(Map<String, Object> contentJson, CVSectionResponse section) {
        VBox fieldsSection = new VBox(10);
        fieldsSection.setUserData(new HashMap<String, TextField>());
        Label header = new Label("FIELD VALUES");
        header.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 10px; -fx-font-weight: 600;");
        fieldsSection.getChildren().add(header);
        List<String> fieldNames = new ArrayList<>();
        if (contentJson.containsKey("fields") && contentJson.get("fields") instanceof List) {
            for (Object f : (List<?>) contentJson.get("fields")) {
                String fname = f.toString();
                if (!fname.equals("customContent")) {
                    fieldNames.add(fname);
                }
            }
        } else {
            for (String key : contentJson.keySet()) {
                if (!key.equals("style") && !key.equals("type") && !key.equals("layout") && 
                    !key.equals("displayStyle") && !key.equals("categories") && !key.equals("fields") &&
                    !key.equals("placeholder") && !key.equals("maxLength") && !key.equals("customContent") &&
                    !key.equals("includePhoto")) {
                    fieldNames.add(key);
                }
            }
        }
        @SuppressWarnings("unchecked")
        Map<String, TextField> fieldMap = (Map<String, TextField>) fieldsSection.getUserData();
        for (String fieldName : fieldNames) {
            VBox fieldGroup = new VBox(4);
            Label fieldLabel = new Label(formatFieldName(fieldName));
            fieldLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px; -fx-font-weight: 500;");
            TextField fieldInput = new TextField();
            fieldInput.setPromptText("Enter " + formatFieldName(fieldName).toLowerCase() + "...");
            fieldInput.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #E5E7EB; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8;");
            if (contentJson.containsKey(fieldName) && contentJson.get(fieldName) != null) {
                fieldInput.setText(contentJson.get(fieldName).toString());
            }
            fieldInput.textProperty().addListener((obs, old, newVal) -> {
                contentJson.put(fieldName, newVal);
                updateSectionContentInMemory(section, contentJson);
                renderLivePreview();
                markDirty();
            });
            fieldMap.put(fieldName, fieldInput);
            fieldGroup.getChildren().addAll(fieldLabel, fieldInput);
            fieldsSection.getChildren().add(fieldGroup);
        }
        return fieldsSection;
    }
    private void updateSectionContentInMemory(CVSectionResponse section, Map<String, Object> contentJson) {
        try {
            section.setContent(objectMapper.writeValueAsString(contentJson));
        } catch (Exception e) {
        }
    }
    private VBox createCustomContentSection(Map<String, Object> contentJson, CVSectionResponse section) {
        VBox sectionBox = new VBox(8);
        sectionBox.setPadding(new Insets(10, 0, 0, 0));
        Label header = new Label("CUSTOM CONTENT");
        header.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 10px; -fx-font-weight: 600;");
        TextArea customArea = new TextArea();
        customArea.setPromptText("Add any additional custom content here...");
        customArea.setPrefRowCount(3);
        customArea.setWrapText(true);
        customArea.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #E5E7EB; -fx-border-radius: 6; -fx-background-radius: 6;");
        if (contentJson.containsKey("customContent")) {
            customArea.setText(contentJson.get("customContent").toString());
        } else if (contentJson.containsKey("placeholder") && !contentJson.containsKey("fields")) {
            String placeholder = contentJson.get("placeholder").toString();
            customArea.setPromptText(placeholder);
        }
        customArea.textProperty().addListener((obs, old, newVal) -> {
            String text = newVal != null ? newVal : "";
            int lineCount = text.isEmpty() ? 1 : text.split("\n", -1).length;
            int estimatedWrappedLines = (int) Math.ceil(text.length() / 80.0);
            int totalLines = Math.max(lineCount, estimatedWrappedLines);
            int rows = Math.max(3, Math.min(20, totalLines));
            customArea.setPrefRowCount(rows);
            contentJson.put("customContent", newVal);
            updateSectionContentInMemory(section, contentJson);
            renderLivePreview();
            markDirty();
        });
        sectionBox.setUserData(customArea);
        sectionBox.getChildren().addAll(header, customArea);
        return sectionBox;
    }
    private HBox createVisibilityToggle(CVSectionResponse section) {
        HBox visibilityRow = new HBox(10);
        visibilityRow.setAlignment(Pos.CENTER_LEFT);
        Label visLabel = new Label("Visible in CV");
        visLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        ToggleButton visibilityToggle = new ToggleButton();
        visibilityToggle.setSelected(section.getIsVisible() != null && section.getIsVisible());
        visibilityToggle.setText(visibilityToggle.isSelected() ? "ON" : "OFF");
        visibilityToggle.setStyle(visibilityToggle.isSelected() 
            ? "-fx-background-color: #0F766E; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 12;"
            : "-fx-background-color: #E5E7EB; -fx-text-fill: #6B7280; -fx-padding: 4 12; -fx-background-radius: 12;");
        visibilityToggle.setOnAction(e -> {
            boolean isVisible = visibilityToggle.isSelected();
            visibilityToggle.setText(isVisible ? "ON" : "OFF");
            visibilityToggle.setStyle(isVisible 
                ? "-fx-background-color: #0F766E; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 12;"
                : "-fx-background-color: #E5E7EB; -fx-text-fill: #6B7280; -fx-padding: 4 12; -fx-background-radius: 12;");
            section.setIsVisible(isVisible);
            renderSections();
            renderLivePreview();
            markDirty();
        });
        visibilityRow.getChildren().addAll(visLabel, spacer, visibilityToggle);
        return visibilityRow;
    }
    private String buildContentJson(VBox fieldsSection, VBox customContentSection, Map<String, Object> originalContent) {
        try {
            Map<String, Object> newContent = new HashMap<>(originalContent);
            @SuppressWarnings("unchecked")
            Map<String, TextField> fieldMap = (Map<String, TextField>) fieldsSection.getUserData();
            if (fieldMap != null) {
                for (Map.Entry<String, TextField> entry : fieldMap.entrySet()) {
                    String value = entry.getValue().getText();
                    if (value != null && !value.isEmpty()) {
                        newContent.put(entry.getKey(), value);
                    }
                }
            }
            TextArea customArea = (TextArea) customContentSection.getUserData();
            if (customArea != null && !customArea.getText().isEmpty()) {
                newContent.put("customContent", customArea.getText());
            }
            return objectMapper.writeValueAsString(newContent);
        } catch (Exception e) {
            return originalContent.toString();
        }
    }
    private VBox createFormGroup(String labelText, String value, String placeholder) {
        VBox group = new VBox(6);
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px; -fx-font-weight: 600;");
        TextField field = new TextField(value);
        field.setPromptText(placeholder);
        field.setStyle("-fx-background-color: #F9FAFB; -fx-text-fill: #111827; -fx-border-color: #E5E7EB; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12;");
        group.getChildren().addAll(label, field);
        return group;
    }
    private void renderDefaultPropertiesPanel() {
        propertiesContent.getChildren().clear();
        propertiesTitle.setText("Properties");
        VBox placeholder = new VBox(12);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPadding(new Insets(40, 20, 40, 20));
        FontIcon icon = new FontIcon("fas-hand-pointer");
        icon.setIconSize(32);
        icon.setIconColor(Color.web("#4B5563"));
        Label label = new Label("Select a section");
        label.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px;");
        Label hint = new Label("Click on a section from the left panel to edit its properties and content");
        hint.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px; -fx-text-alignment: center;");
        hint.setWrapText(true);
        placeholder.getChildren().addAll(icon, label, hint);
        propertiesContent.getChildren().add(placeholder);
    }
    private void toggleSectionVisibility(CVSectionResponse section) {
        CVSectionRequest request = new CVSectionRequest();
        request.setSectionType(section.getSectionType());
        request.setTitle(section.getTitle());
        request.setContent(section.getContent());
        request.setIsVisible(!(section.getIsVisible() != null && section.getIsVisible()));
        cvService.updateSection(cvId, section.getId(), request).whenComplete((updatedSection, error) -> {
            Platform.runLater(() -> {
                if (error != null) {
                    Toast.error("Failed to update visibility");
                    return;
                }
                refreshCV();
            });
        });
    }
    private void updateSection(String sectionId, String title, String content, boolean isVisible) {
        updateSectionWithCallback(sectionId, title, content, isVisible, null, null);
    }
    private void updateSectionWithCallback(String sectionId, String title, String content, boolean isVisible, 
                                           Button updateBtn, String originalText) {
        CVSectionRequest request = new CVSectionRequest();
        CVSectionResponse section = currentCV.getSections().stream()
            .filter(s -> s.getId().equals(sectionId))
            .findFirst().orElse(null);
        if (section == null) {
            resetSaveButton(updateBtn, originalText, false);
            return;
        }
        request.setSectionType(section.getSectionType());
        request.setTitle(title);
        request.setContent(content);
        request.setIsVisible(isVisible);
        cvService.updateSection(cvId, sectionId, request).whenComplete((updatedSection, error) -> {
            Platform.runLater(() -> {
                if (error != null) {
                    Toast.error("Failed to update section");
                    resetSaveButton(updateBtn, originalText, false);
                    return;
                }
                refreshCV();
                selectedSection = null;
                renderLivePreview();
                if (updateBtn != null) {
                    updateBtn.setText("✓ Saved");
                    updateBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6; -fx-font-size: 12px;");
                    new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> resetSaveButton(updateBtn, originalText, true));
                        }
                    }, 1500);
                }
                Toast.success("Section updated");
                markClean();
            });
        });
    }
    private void resetSaveButton(Button btn, String originalText, boolean success) {
        if (btn == null) return;
        btn.setText(originalText != null ? originalText : "Save Changes");
        btn.setDisable(false);
        btn.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;");
    }
    private void deleteSection(String sectionId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Section");
        alert.setHeaderText("Delete this section?");
        alert.setContentText("This action cannot be undone.");
        Dialogs.prepare(alert).showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                cvService.deleteSection(cvId, sectionId).whenComplete((result, error) -> {
                    Platform.runLater(() -> {
                        if (error != null) {
                            Toast.error("Failed to delete section");
                            return;
                        }
                        selectedSection = null;
                        renderDefaultPropertiesPanel();
                        refreshCV();
                        Toast.success("Section deleted");
                    });
                });
            }
        });
    }
    @FXML
    private void onAddSection() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("SUMMARY", SECTION_TYPES);
        dialog.setTitle("Add Section");
        dialog.setHeaderText("Select section type");
        dialog.setContentText("Type:");
        Dialogs.prepare(dialog).showAndWait().ifPresent(type -> {
            CVSectionRequest request = new CVSectionRequest();
            request.setSectionType(type);
            request.setTitle(formatSectionType(type));
            request.setContent(getDefaultContentForType(type));
            request.setIsVisible(true);
            cvService.addSection(cvId, request).whenComplete((newSection, error) -> {
                Platform.runLater(() -> {
                    if (error != null) {
                        Toast.error("Failed to add section");
                        return;
                    }
                    refreshCV();
                    Toast.success("Section added");
                });
            });
        });
    }
    private String getDefaultContentForType(String type) {
        return switch (type.toUpperCase()) {
            case "PERSONAL_INFO" -> "{\"fields\": [\"fullName\", \"email\", \"phone\", \"address\", \"linkedIn\"]}";
            case "SUMMARY" -> "{\"placeholder\": \"Write a brief professional summary...\"}";
            case "EXPERIENCE" -> "{\"fields\": [\"jobTitle\", \"company\", \"duration\", \"responsibilities\"]}";
            case "EDUCATION" -> "{\"fields\": [\"degree\", \"institution\", \"graduationDate\", \"gpa\"]}";
            case "SKILLS" -> "{\"categories\": [\"Technical Skills\", \"Soft Skills\"]}";
            case "PROJECTS" -> "{\"fields\": [\"projectName\", \"description\", \"technologies\", \"link\"]}";
            case "CERTIFICATIONS" -> "{\"fields\": [\"certName\", \"issuingOrg\", \"issueDate\"]}";
            case "LANGUAGES" -> "{\"fields\": [\"language\", \"proficiency\"]}";
            case "REFERENCES" -> "{\"fields\": [\"name\", \"title\", \"company\", \"email\", \"phone\"]}";
            default -> "{}";
        };
    }
    @FXML
    private void onSave() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            Toast.error("Title is required");
            return;
        }
        CVUpdateRequest request = new CVUpdateRequest();
        request.setTitle(title);
        request.setVisibility("PRIVATE");
        saveBtn.setDisable(true);
        cvService.updateCV(cvId, request).whenComplete((cv, error) -> {
            Platform.runLater(() -> {
                saveBtn.setDisable(false);
                if (error != null) {
                    Toast.error("Failed to save CV");
                    return;
                }
                currentCV = cv;
                markClean();
                Toast.success("CV saved!");
            });
        });
    }
    @FXML
    private void onZoomIn() {
        if (zoomLevel < 1.5) {
            zoomLevel += 0.1;
            updateZoom();
        }
    }
    @FXML
    private void onZoomOut() {
        if (zoomLevel > 0.5) {
            zoomLevel -= 0.1;
            updateZoom();
        }
    }
    private void updateZoom() {
        zoomLabel.setText(Math.round(zoomLevel * 100) + "%");
        previewContent.setScaleX(zoomLevel);
        previewContent.setScaleY(zoomLevel);
    }
    @FXML
    private void onPreview() {
        if (currentCV == null) {
            Toast.error("No CV to preview");
            return;
        }
        previewBtn.setDisable(true);
        String originalStyle = previewBtn.getStyle();
        previewBtn.setStyle("-fx-background-color: #9CA3AF; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6; -fx-min-height: 36; -fx-max-height: 36;");
        Toast.info("Generating PDF preview...");
        new Thread(() -> {
            try {
                File tempFile = File.createTempFile("cv_preview_", ".pdf");
                tempFile.deleteOnExit();
                CvPdfGenerator.generatePdf(currentCV, tempFile);
                Platform.runLater(() -> {
                    previewBtn.setDisable(false);
                    previewBtn.setStyle(originalStyle);
                    if (java.awt.Desktop.isDesktopSupported()) {
                        try {
                            java.awt.Desktop.getDesktop().open(tempFile);
                            Toast.success("PDF preview opened");
                        } catch (Exception ex) {
                            Toast.info("PDF saved to: " + tempFile.getAbsolutePath());
                        }
                    } else {
                        Toast.info("PDF saved to: " + tempFile.getAbsolutePath());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    previewBtn.setDisable(false);
                    previewBtn.setStyle(originalStyle);
                    Toast.error("Failed to generate preview");
                });
            }
        }).start();
    }
    @FXML
    private void onExport() {
        if (currentCV == null) {
            Toast.error("No CV to export");
            return;
        }
        String fileName = (currentCV.getTitle() != null ? 
            currentCV.getTitle().replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", "_") : "CV") + ".pdf";
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save CV as PDF");
        fileChooser.setInitialFileName(fileName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File downloadsDir = new File(System.getProperty("user.home"), "Downloads");
        if (downloadsDir.exists() && downloadsDir.isDirectory()) {
            fileChooser.setInitialDirectory(downloadsDir);
        }
        File saveFile = fileChooser.showSaveDialog(exportBtn.getScene().getWindow());
        if (saveFile == null) {
            return;
        }
        if (!saveFile.getName().toLowerCase().endsWith(".pdf")) {
            saveFile = new File(saveFile.getAbsolutePath() + ".pdf");
        }
        final File finalFile = saveFile;
        exportBtn.setDisable(true);
        String originalStyle = exportBtn.getStyle();
        exportBtn.setStyle("-fx-background-color: #9CA3AF; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6; -fx-min-height: 36; -fx-max-height: 36;");
        Toast.info("Generating PDF...");
        new Thread(() -> {
            try {
                CvPdfGenerator.generatePdf(currentCV, finalFile);
                Platform.runLater(() -> {
                    exportBtn.setDisable(false);
                    exportBtn.setStyle(originalStyle);
                    Toast.success("CV exported to " + finalFile.getName());
                    if (java.awt.Desktop.isDesktopSupported()) {
                        try {
                            java.awt.Desktop.getDesktop().open(finalFile);
                        } catch (Exception ex) {
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    exportBtn.setDisable(false);
                    exportBtn.setStyle(originalStyle);
                    Toast.error("Failed to export CV");
                });
            }
        }).start();
    }
    @FXML
    private void onChangeTemplate() {
        Toast.info("Template gallery coming soon!");
    }
    @FXML
    private void onDuplicate() {
        if (selectedSection == null) {
            Toast.info("Select a section to duplicate");
            return;
        }
        CVSectionRequest request = new CVSectionRequest();
        request.setSectionType(selectedSection.getSectionType());
        request.setTitle(selectedSection.getTitle() + " (Copy)");
        request.setContent(selectedSection.getContent());
        request.setIsVisible(true);
        cvService.addSection(cvId, request).whenComplete((newSection, error) -> {
            Platform.runLater(() -> {
                if (error != null) {
                    Toast.error("Failed to duplicate section");
                    return;
                }
                refreshCV();
                Toast.success("Section duplicated");
            });
        });
    }
    @FXML
    private void onBack() {
        if (isDirty) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes");
            alert.setContentText("Do you want to save before leaving?");
            ButtonType saveButton = new ButtonType("Save & Exit");
            ButtonType discardButton = new ButtonType("Discard");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);
            Dialogs.prepare(alert).showAndWait().ifPresent(response -> {
                if (response == saveButton) {
                    onSave();
                    Router.getInstance().navigate(Route.SEEKER_CVS);
                } else if (response == discardButton) {
                    Router.getInstance().navigate(Route.SEEKER_CVS);
                }
            });
        } else {
            Router.getInstance().navigate(Route.SEEKER_CVS);
        }
    }
    private void showLoading(boolean show) {
        loadingContainer.setVisible(show);
        loadingContainer.setManaged(show);
    }
}
