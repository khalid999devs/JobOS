package com.jobos.desktop.controller.seeker;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.CVService;
import com.jobos.shared.dto.cv.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class CvEditorController implements Initializable {

    @FXML private TextField titleField;
    @FXML private ComboBox<String> visibilityCombo;
    @FXML private VBox sectionsList;
    @FXML private VBox editorContent;
    @FXML private VBox propertiesPanel;
    @FXML private Label templateLabel;
    @FXML private Label lastSavedLabel;
    @FXML private Button saveBtn;
    @FXML private Button previewBtn;
    @FXML private VBox loadingContainer;
    @FXML private StackPane previewContainer;
    @FXML private VBox previewContent;

    private final CVService cvService = new CVService();
    private String cvId;
    private CVResponse currentCV;
    private CVSectionResponse selectedSection;
    private boolean isDirty = false;
    private static final DataFormat SECTION_FORMAT = new DataFormat("application/x-cv-section");

    // Section types
    private static final String[] SECTION_TYPES = {
        "PERSONAL_INFO", "SUMMARY", "EXPERIENCE", "EDUCATION", 
        "SKILLS", "PROJECTS", "CERTIFICATIONS", "LANGUAGES", "REFERENCES", "CUSTOM"
    };

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupVisibilityCombo();
        setupAutoSave();
        
        // Get CV ID from route params
        cvId = Router.getInstance().getParam("cvId");
        if (cvId != null) {
            loadCV();
        }
    }

    private void setupVisibilityCombo() {
        visibilityCombo.setItems(FXCollections.observableArrayList(
            "Private", "Public", "Link Only"
        ));
        visibilityCombo.setValue("Private");
        visibilityCombo.setOnAction(e -> markDirty());
    }

    private void setupAutoSave() {
        titleField.textProperty().addListener((obs, old, newVal) -> markDirty());
    }

    private void markDirty() {
        isDirty = true;
        lastSavedLabel.setText("Unsaved changes");
        lastSavedLabel.setStyle("-fx-text-fill: #D97706; -fx-font-size: 12px;");
    }

    private void markClean() {
        isDirty = false;
        lastSavedLabel.setText("All changes saved");
        lastSavedLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 12px;");
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
        templateLabel.setText(currentCV.getTemplateName() != null ? currentCV.getTemplateName() : "Custom Template");
        
        String visibility = currentCV.getVisibility();
        if (visibility != null) {
            switch (visibility.toUpperCase()) {
                case "PUBLIC" -> visibilityCombo.setValue("Public");
                case "LINK_ONLY" -> visibilityCombo.setValue("Link Only");
                default -> visibilityCombo.setValue("Private");
            }
        }
        
        renderSections();
        markClean();
    }

    private void renderSections() {
        sectionsList.getChildren().clear();
        
        if (currentCV.getSections() == null || currentCV.getSections().isEmpty()) {
            Label emptyLabel = new Label("No sections yet");
            emptyLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-padding: 16;");
            sectionsList.getChildren().add(emptyLabel);
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
        item.setPadding(new Insets(10, 12, 10, 12));
        item.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");
        
        // Drag handle
        FontIcon dragHandle = new FontIcon("fas-grip-vertical");
        dragHandle.setIconSize(12);
        dragHandle.setIconColor(Color.web("#9CA3AF"));
        
        // Section icon
        FontIcon sectionIcon = getSectionIcon(section.getSectionType());
        
        // Section title
        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        
        Label titleLabel = new Label(section.getTitle() != null ? section.getTitle() : formatSectionType(section.getSectionType()));
        titleLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 13px; -fx-font-weight: 500;");
        
        Label typeLabel = new Label(formatSectionType(section.getSectionType()));
        typeLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
        
        titleBox.getChildren().addAll(titleLabel, typeLabel);
        
        // Visibility toggle
        FontIcon visibilityIcon = new FontIcon(section.getIsVisible() != null && section.getIsVisible() ? "fas-eye" : "fas-eye-slash");
        visibilityIcon.setIconSize(12);
        visibilityIcon.setIconColor(Color.web(section.getIsVisible() != null && section.getIsVisible() ? "#059669" : "#9CA3AF"));
        visibilityIcon.setOnMouseClicked(e -> toggleSectionVisibility(section));
        
        item.getChildren().addAll(dragHandle, sectionIcon, titleBox, visibilityIcon);
        
        // Click to select
        item.setOnMouseClicked(e -> selectSection(section, item));
        
        // Highlight if selected
        if (selectedSection != null && selectedSection.getId().equals(section.getId())) {
            item.setStyle("-fx-background-color: #F0FDFA; -fx-background-radius: 8; -fx-border-color: #0F766E; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;");
        }
        
        item.setOnMouseEntered(e -> {
            if (selectedSection == null || !selectedSection.getId().equals(section.getId())) {
                item.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8; -fx-cursor: hand;");
            }
        });
        item.setOnMouseExited(e -> {
            if (selectedSection == null || !selectedSection.getId().equals(section.getId())) {
                item.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");
            }
        });
        
        // Drag and drop
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
                item.setStyle("-fx-background-color: #F0FDFA; -fx-background-radius: 8; -fx-border-color: #0F766E; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;");
            }
        });

        item.setOnDragExited(event -> {
            if (selectedSection == null || !selectedSection.getId().equals(section.getId())) {
                item.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");
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
            .filter(s -> s.getId().equals(draggedId))
            .findFirst().orElse(null);
        CVSectionResponse targetSection = sections.stream()
            .filter(s -> s.getId().equals(targetId))
            .findFirst().orElse(null);
        
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
                Toast.info("Sections reordered");
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
        icon.setIconSize(14);
        icon.setIconColor(Color.web("#0F766E"));
        return icon;
    }

    private String formatSectionType(String type) {
        if (type == null) return "Custom";
        return type.replace("_", " ")
            .toLowerCase()
            .replaceFirst(".", String.valueOf(Character.toUpperCase(type.charAt(0))));
    }

    private void selectSection(CVSectionResponse section, HBox item) {
        selectedSection = section;
        renderSections(); // Re-render to update selection highlight
        renderSectionEditor(section);
    }

    private void renderSectionEditor(CVSectionResponse section) {
        editorContent.getChildren().clear();
        
        VBox editor = new VBox(16);
        editor.setPadding(new Insets(20));
        
        // Section title
        Label headerLabel = new Label("Edit Section");
        headerLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 16px; -fx-font-weight: 600;");
        
        // Title field
        VBox titleGroup = new VBox(6);
        Label titleLabel = new Label("Section Title");
        titleLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 13px; -fx-font-weight: 500;");
        TextField sectionTitleField = new TextField(section.getTitle());
        sectionTitleField.setPromptText("Enter section title");
        sectionTitleField.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #E5E7EB; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 12;");
        titleGroup.getChildren().addAll(titleLabel, sectionTitleField);
        
        // Content area
        VBox contentGroup = new VBox(6);
        Label contentLabel = new Label("Content");
        contentLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 13px; -fx-font-weight: 500;");
        TextArea contentArea = new TextArea(section.getContent());
        contentArea.setPromptText("Enter section content...");
        contentArea.setWrapText(true);
        contentArea.setPrefRowCount(10);
        contentArea.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #E5E7EB; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        // Formatting toolbar
        HBox toolbar = createFormattingToolbar(contentArea);
        
        contentGroup.getChildren().addAll(contentLabel, toolbar, contentArea);
        
        // Actions
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(16, 0, 0, 0));
        
        Button updateBtn = new Button("Update Section");
        updateBtn.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        updateBtn.setOnAction(e -> updateSection(section.getId(), sectionTitleField.getText(), contentArea.getText()));
        
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DC2626; -fx-padding: 10 16; -fx-border-color: #DC2626; -fx-border-radius: 8; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deleteSection(section.getId()));
        
        actions.getChildren().addAll(updateBtn, deleteBtn);
        
        editor.getChildren().addAll(headerLabel, titleGroup, contentGroup, actions);
        editorContent.getChildren().add(editor);
    }

    private HBox createFormattingToolbar(TextArea contentArea) {
        HBox toolbar = new HBox(4);
        toolbar.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 6; -fx-padding: 4;");
        
        String[][] buttons = {
            {"fas-bold", "Bold"},
            {"fas-italic", "Italic"},
            {"fas-underline", "Underline"},
            {"fas-list-ul", "Bullet List"},
            {"fas-list-ol", "Numbered List"},
            {"fas-link", "Link"}
        };
        
        for (String[] btn : buttons) {
            Button formatBtn = new Button();
            FontIcon icon = new FontIcon(btn[0]);
            icon.setIconSize(12);
            icon.setIconColor(Color.web("#6B7280"));
            formatBtn.setGraphic(icon);
            formatBtn.setTooltip(new Tooltip(btn[1]));
            formatBtn.setStyle("-fx-background-color: transparent; -fx-padding: 6; -fx-cursor: hand;");
            formatBtn.setOnMouseEntered(e -> formatBtn.setStyle("-fx-background-color: #E5E7EB; -fx-background-radius: 4; -fx-padding: 6; -fx-cursor: hand;"));
            formatBtn.setOnMouseExited(e -> formatBtn.setStyle("-fx-background-color: transparent; -fx-padding: 6; -fx-cursor: hand;"));
            toolbar.getChildren().add(formatBtn);
        }
        
        return toolbar;
    }

    private void toggleSectionVisibility(CVSectionResponse section) {
        CVSectionRequest request = new CVSectionRequest();
        request.setSectionType(section.getSectionType());
        request.setTitle(section.getTitle());
        request.setContent(section.getContent());
        request.setIsVisible(!(section.getIsVisible() != null && section.getIsVisible()));
        
        cvService.updateSection(cvId, section.getId(), request).whenComplete((cv, error) -> {
            Platform.runLater(() -> {
                if (error != null) {
                    Toast.error("Failed to update visibility");
                    return;
                }
                currentCV = cv;
                renderSections();
            });
        });
    }

    private void updateSection(String sectionId, String title, String content) {
        CVSectionRequest request = new CVSectionRequest();
        CVSectionResponse section = currentCV.getSections().stream()
            .filter(s -> s.getId().equals(sectionId))
            .findFirst().orElse(null);
        
        if (section == null) return;
        
        request.setSectionType(section.getSectionType());
        request.setTitle(title);
        request.setContent(content);
        request.setIsVisible(section.getIsVisible());
        
        cvService.updateSection(cvId, sectionId, request).whenComplete((cv, error) -> {
            Platform.runLater(() -> {
                if (error != null) {
                    Toast.error("Failed to update section");
                    return;
                }
                currentCV = cv;
                renderSections();
                Toast.success("Section updated");
            });
        });
    }

    private void deleteSection(String sectionId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Section");
        alert.setHeaderText("Delete this section?");
        alert.setContentText("This action cannot be undone.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                cvService.deleteSection(cvId, sectionId).whenComplete((cv, error) -> {
                    Platform.runLater(() -> {
                        if (error != null) {
                            Toast.error("Failed to delete section");
                            return;
                        }
                        currentCV = cv;
                        selectedSection = null;
                        editorContent.getChildren().clear();
                        renderSections();
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
        
        dialog.showAndWait().ifPresent(type -> {
            CVSectionRequest request = new CVSectionRequest();
            request.setSectionType(type);
            request.setTitle(formatSectionType(type));
            request.setContent("");
            request.setIsVisible(true);
            
            cvService.addSection(cvId, request).whenComplete((cv, error) -> {
                Platform.runLater(() -> {
                    if (error != null) {
                        Toast.error("Failed to add section");
                        return;
                    }
                    currentCV = cv;
                    renderSections();
                    Toast.success("Section added");
                });
            });
        });
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
        request.setVisibility(mapVisibility(visibilityCombo.getValue()));
        
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

    private String mapVisibility(String display) {
        return switch (display) {
            case "Public" -> "PUBLIC";
            case "Link Only" -> "LINK_ONLY";
            default -> "PRIVATE";
        };
    }

    @FXML
    private void onPreview() {
        previewContainer.setVisible(true);
        previewContainer.setManaged(true);
        generatePreview();
    }

    @FXML
    private void onClosePreview() {
        previewContainer.setVisible(false);
        previewContainer.setManaged(false);
    }

    private void generatePreview() {
        previewContent.getChildren().clear();
        
        // Title
        Label titleLabel = new Label(currentCV.getTitle());
        titleLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 24px; -fx-font-weight: 700;");
        titleLabel.setWrapText(true);
        previewContent.getChildren().add(titleLabel);
        
        if (currentCV.getSections() != null) {
            for (CVSectionResponse section : currentCV.getSections()) {
                if (section.getIsVisible() != null && section.getIsVisible()) {
                    VBox sectionBox = new VBox(8);
                    sectionBox.setPadding(new Insets(16, 0, 0, 0));
                    
                    // Section header with border
                    Label sectionTitle = new Label(section.getTitle());
                    sectionTitle.setStyle("-fx-text-fill: #0F766E; -fx-font-size: 16px; -fx-font-weight: 600; -fx-border-color: #0F766E; -fx-border-width: 0 0 2 0; -fx-padding: 0 0 8 0;");
                    sectionTitle.setMaxWidth(Double.MAX_VALUE);
                    
                    // Section content
                    Label contentLabel = new Label(section.getContent());
                    contentLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px; -fx-line-spacing: 4;");
                    contentLabel.setWrapText(true);
                    
                    sectionBox.getChildren().addAll(sectionTitle, contentLabel);
                    previewContent.getChildren().add(sectionBox);
                }
            }
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("\n", "<br>");
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
            
            alert.showAndWait().ifPresent(response -> {
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
