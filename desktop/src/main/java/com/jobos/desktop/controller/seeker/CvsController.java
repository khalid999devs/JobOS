package com.jobos.desktop.controller.seeker;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
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
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * Google Docs-style CV management interface
 */
public class CvsController implements Initializable {

    @FXML private VBox templatesSection;
    @FXML private HBox templatesList;
    @FXML private VBox myCvsSection;
    @FXML private FlowPane cvsList;
    @FXML private VBox loadingContainer;
    @FXML private VBox emptyContainer;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Label cvsCountLabel;

    private final CVService cvService = new CVService();
    private final Router router = Router.getInstance();
    private List<CVTemplateResponse> templates = new ArrayList<>();
    private List<CVListResponse> myCVs = new ArrayList<>();
    
    // Thumbnail cache
    private final Map<String, WritableImage> thumbnailCache = new HashMap<>();
    
    // Constants for thumbnail sizes
    private static final int TEMPLATE_THUMB_WIDTH = 150;
    private static final int TEMPLATE_THUMB_HEIGHT = 195;
    private static final int CV_THUMB_WIDTH = 180;
    private static final int CV_THUMB_HEIGHT = 230;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupCategoryFilter();
        loadTemplates();
        loadMyCVs();
    }

    private void setupCategoryFilter() {
        categoryFilter.getItems().addAll(
            "All Templates", "Professional", "Creative", "Modern", "Simple", "Academic"
        );
        categoryFilter.setValue("All Templates");
        categoryFilter.setOnAction(e -> loadTemplates());
    }

    private void loadTemplates() {
        String category = categoryFilter.getValue();
        String categoryParam = "All Templates".equals(category) ? null : category.toUpperCase();
        
        cvService.getTemplates(categoryParam).whenComplete((result, error) -> {
            Platform.runLater(() -> {
                if (error != null) {
                    System.err.println("Failed to load templates: " + error.getMessage());
                    renderTemplates(List.of());
                    return;
                }
                templates = result != null ? result : List.of();
                renderTemplates(templates);
            });
        });
    }

    private void renderTemplates(List<CVTemplateResponse> templates) {
        templatesList.getChildren().clear();
        
        // Add "Blank" template first (like Google Docs' blank document)
        templatesList.getChildren().add(createBlankTemplateCard());
        
        // Add other templates
        for (CVTemplateResponse template : templates) {
            templatesList.getChildren().add(createTemplateCard(template));
        }
    }

    private VBox createBlankTemplateCard() {
        VBox card = new VBox(8);
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle("-fx-cursor: hand;");
        card.setPrefWidth(TEMPLATE_THUMB_WIDTH);
        
        // Preview area with blank thumbnail
        StackPane preview = new StackPane();
        preview.setPrefSize(TEMPLATE_THUMB_WIDTH, TEMPLATE_THUMB_HEIGHT);
        preview.setStyle("-fx-background-color: white; -fx-background-radius: 4; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 4;");
        
        // Generate blank thumbnail
        WritableImage blankThumb = CvPdfGenerator.generateBlankThumbnail(TEMPLATE_THUMB_WIDTH, TEMPLATE_THUMB_HEIGHT);
        ImageView imageView = new ImageView(blankThumb);
        imageView.setFitWidth(TEMPLATE_THUMB_WIDTH - 2);
        imageView.setFitHeight(TEMPLATE_THUMB_HEIGHT - 2);
        preview.getChildren().add(imageView);
        
        // Label
        Label nameLabel = new Label("Blank document");
        nameLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");
        
        card.getChildren().addAll(preview, nameLabel);
        
        // Click to create new blank CV
        card.setOnMouseClicked(e -> createNewCV(null, "Blank"));
        
        // Hover effects
        card.setOnMouseEntered(e -> preview.setStyle("-fx-background-color: white; -fx-background-radius: 4; -fx-border-color: #0F766E; -fx-border-width: 2; -fx-border-radius: 4;"));
        card.setOnMouseExited(e -> preview.setStyle("-fx-background-color: white; -fx-background-radius: 4; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 4;"));
        
        return card;
    }

    private VBox createTemplateCard(CVTemplateResponse template) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle("-fx-cursor: hand;");
        card.setPrefWidth(TEMPLATE_THUMB_WIDTH);
        
        // Preview area
        StackPane preview = new StackPane();
        preview.setPrefSize(TEMPLATE_THUMB_WIDTH, TEMPLATE_THUMB_HEIGHT);
        preview.setStyle("-fx-background-color: white; -fx-background-radius: 4; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 4;");
        
        // Simple template visualization
        VBox templatePreview = createTemplatePreviewContent(template);
        preview.getChildren().add(templatePreview);
        
        // Premium badge
        if (template.getIsPremium() != null && template.getIsPremium()) {
            HBox badge = new HBox(4);
            badge.setAlignment(Pos.CENTER);
            badge.setPadding(new Insets(3, 6, 3, 6));
            badge.setStyle("-fx-background-color: #FEF3C7; -fx-background-radius: 4;");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(6, 6, 0, 0));
            
            FontIcon crownIcon = new FontIcon("fas-crown");
            crownIcon.setIconSize(10);
            crownIcon.setIconColor(Color.web("#D97706"));
            
            Label costLabel = new Label(template.getCreditCost() != null ? template.getCreditCost().toString() : "5");
            costLabel.setStyle("-fx-text-fill: #D97706; -fx-font-size: 10px; -fx-font-weight: 600;");
            
            badge.getChildren().addAll(crownIcon, costLabel);
            preview.getChildren().add(badge);
            
            // Lock overlay if not unlocked
            if (template.getIsUnlocked() == null || !template.getIsUnlocked()) {
                StackPane lockOverlay = new StackPane();
                lockOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.25); -fx-background-radius: 4;");
                FontIcon lockIcon = new FontIcon("fas-lock");
                lockIcon.setIconSize(20);
                lockIcon.setIconColor(Color.WHITE);
                lockOverlay.getChildren().add(lockIcon);
                preview.getChildren().add(lockOverlay);
            }
        }
        
        // Template name
        Label nameLabel = new Label(template.getName());
        nameLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");
        nameLabel.setMaxWidth(TEMPLATE_THUMB_WIDTH);
        
        // Category label
        Label categoryLabel = new Label(template.getCategory() != null ? formatCategory(template.getCategory()) : "");
        categoryLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 10px;");
        
        card.getChildren().addAll(preview, nameLabel, categoryLabel);
        
        // Click handler
        card.setOnMouseClicked(e -> handleTemplateClick(template));
        
        // Hover effects
        card.setOnMouseEntered(e -> preview.setStyle("-fx-background-color: white; -fx-background-radius: 4; -fx-border-color: #0F766E; -fx-border-width: 2; -fx-border-radius: 4;"));
        card.setOnMouseExited(e -> preview.setStyle("-fx-background-color: white; -fx-background-radius: 4; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 4;"));
        
        return card;
    }
    
    private VBox createTemplatePreviewContent(CVTemplateResponse template) {
        VBox content = new VBox(4);
        content.setPadding(new Insets(12, 10, 12, 10));
        content.setMaxSize(TEMPLATE_THUMB_WIDTH - 4, TEMPLATE_THUMB_HEIGHT - 4);
        content.setStyle("-fx-background-color: white;");
        
        // Header bar (colored based on template category)
        String headerColor = getTemplateHeaderColor(template.getCategory());
        Region header = new Region();
        header.setPrefHeight(25);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setStyle("-fx-background-color: " + headerColor + ";");
        
        // Name placeholder
        Region nameLine = new Region();
        nameLine.setPrefHeight(8);
        nameLine.setPrefWidth(80);
        nameLine.setStyle("-fx-background-color: #E5E7EB;");
        VBox.setMargin(nameLine, new Insets(8, 0, 0, 0));
        
        // Content lines
        VBox lines = new VBox(4);
        VBox.setMargin(lines, new Insets(8, 0, 0, 0));
        for (int i = 0; i < 5; i++) {
            Region line = new Region();
            line.setPrefHeight(4);
            line.setMaxWidth(Double.MAX_VALUE);
            line.setStyle("-fx-background-color: #F3F4F6;");
            lines.getChildren().add(line);
        }
        
        content.getChildren().addAll(header, nameLine, lines);
        return content;
    }
    
    private String getTemplateHeaderColor(String category) {
        if (category == null) return "#0F766E";
        return switch (category.toUpperCase()) {
            case "CREATIVE" -> "#9333EA";
            case "MODERN" -> "#0284C7";
            case "SIMPLE" -> "#374151";
            case "ACADEMIC" -> "#059669";
            default -> "#0F766E";
        };
    }
    
    private String formatCategory(String category) {
        if (category == null) return "";
        return category.charAt(0) + category.substring(1).toLowerCase();
    }

    private void handleTemplateClick(CVTemplateResponse template) {
        if (template.getIsPremium() != null && template.getIsPremium() && 
            (template.getIsUnlocked() == null || !template.getIsUnlocked())) {
            showUnlockDialog(template);
        } else {
            createNewCV(template.getId(), template.getName());
        }
    }

    private void showUnlockDialog(CVTemplateResponse template) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unlock Template");
        alert.setHeaderText("Unlock \"" + template.getName() + "\"");
        alert.setContentText("This template costs " + template.getCreditCost() + " credits. Unlock it?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                unlockTemplate(template);
            }
        });
    }

    private void unlockTemplate(CVTemplateResponse template) {
        cvService.unlockTemplate(template.getId()).whenComplete((result, error) -> {
            Platform.runLater(() -> {
                if (error != null) {
                    Toast.error("Failed to unlock. Check your credits.");
                    return;
                }
                Toast.success("Template unlocked!");
                loadTemplates();
            });
        });
    }

    private void createNewCV(String templateId, String templateName) {
        TextInputDialog dialog = new TextInputDialog("Untitled CV");
        dialog.setTitle("Create New CV");
        dialog.setHeaderText(templateName != null ? "Using template: " + templateName : "Create a blank CV");
        dialog.setContentText("CV Name:");
        
        dialog.showAndWait().ifPresent(title -> {
            CVCreateRequest request = new CVCreateRequest();
            request.setTitle(title);
            if (templateId != null) {
                request.setTemplateId(templateId);
            }
            request.setVisibility("PRIVATE");
            
            cvService.createCV(request).whenComplete((cv, error) -> {
                Platform.runLater(() -> {
                    if (error != null) {
                        Toast.error("Failed to create CV");
                        return;
                    }
                    Toast.success("CV created!");
                    Map<String, Object> params = new HashMap<>();
                    params.put("cvId", cv.getId());
                    params.put("isNew", true);
                    router.navigate(Route.SEEKER_CV_EDITOR, params);
                });
            });
        });
    }

    private void loadMyCVs() {
        showLoading(true);
        
        cvService.getAllCVs().whenComplete((cvs, error) -> {
            Platform.runLater(() -> {
                showLoading(false);
                if (error != null) {
                    System.err.println("Failed to load CVs: " + error.getMessage());
                    showEmpty(true);
                    return;
                }
                myCVs = cvs != null ? cvs : List.of();
                renderMyCVs(myCVs);
            });
        });
    }

    private void renderMyCVs(List<CVListResponse> cvs) {
        cvsList.getChildren().clear();
        
        if (cvs == null || cvs.isEmpty()) {
            showEmpty(true);
            cvsCountLabel.setText("0 CVs");
            return;
        }
        
        showEmpty(false);
        cvsCountLabel.setText(cvs.size() + " CV" + (cvs.size() != 1 ? "s" : ""));
        
        for (CVListResponse cv : cvs) {
            cvsList.getChildren().add(createCVCard(cv));
        }
    }

    private VBox createCVCard(CVListResponse cv) {
        VBox card = new VBox(0);
        card.setPrefWidth(CV_THUMB_WIDTH);
        card.setStyle("-fx-cursor: hand;");
        
        // Thumbnail container
        StackPane thumbnailContainer = new StackPane();
        thumbnailContainer.setPrefSize(CV_THUMB_WIDTH, CV_THUMB_HEIGHT);
        thumbnailContainer.setStyle("-fx-background-color: white; -fx-background-radius: 4 4 0 0; -fx-border-color: #E5E7EB; -fx-border-width: 1 1 0 1; -fx-border-radius: 4 4 0 0;");
        
        // Generate or get cached thumbnail
        generateCVThumbnail(cv, thumbnailContainer);
        
        // Default badge
        if (cv.getIsDefault() != null && cv.getIsDefault()) {
            HBox defaultBadge = new HBox();
            defaultBadge.setAlignment(Pos.CENTER);
            defaultBadge.setPadding(new Insets(2, 6, 2, 6));
            defaultBadge.setStyle("-fx-background-color: #0F766E; -fx-background-radius: 4;");
            StackPane.setAlignment(defaultBadge, Pos.TOP_LEFT);
            StackPane.setMargin(defaultBadge, new Insets(8, 0, 0, 8));
            
            Label defaultLabel = new Label("Default");
            defaultLabel.setStyle("-fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: 600;");
            defaultBadge.getChildren().add(defaultLabel);
            thumbnailContainer.getChildren().add(defaultBadge);
        }
        
        // Info section
        VBox infoSection = new VBox(4);
        infoSection.setPadding(new Insets(10, 12, 10, 12));
        infoSection.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 4 4; -fx-border-color: #E5E7EB; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 4 4;");
        
        // Title with menu
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(cv.getTitle());
        titleLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 13px; -fx-font-weight: 500;");
        titleLabel.setMaxWidth(CV_THUMB_WIDTH - 50);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        MenuButton menuBtn = new MenuButton();
        menuBtn.setGraphic(createIcon("fas-ellipsis-v", 12, "#6B7280"));
        menuBtn.setStyle("-fx-background-color: transparent; -fx-padding: 4;");
        
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> openCVEditor(cv.getId()));
        
        MenuItem previewItem = new MenuItem("Preview");
        previewItem.setOnAction(e -> showCVPreview(cv.getId()));
        
        MenuItem downloadItem = new MenuItem("Download PDF");
        downloadItem.setOnAction(e -> downloadAsPdf(cv.getId()));
        
        MenuItem setDefaultItem = new MenuItem("Set as Default");
        setDefaultItem.setOnAction(e -> setDefaultCV(cv.getId()));
        
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setStyle("-fx-text-fill: #DC2626;");
        deleteItem.setOnAction(e -> deleteCV(cv.getId(), cv.getTitle()));
        
        menuBtn.getItems().addAll(editItem, previewItem, downloadItem, new SeparatorMenuItem(), setDefaultItem, new SeparatorMenuItem(), deleteItem);
        
        titleRow.getChildren().addAll(titleLabel, menuBtn);
        
        // Meta info
        HBox metaRow = new HBox(6);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        
        Label templateLabel = new Label(cv.getTemplateName() != null ? cv.getTemplateName() : "Custom");
        templateLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");
        
        Label dotLabel = new Label("•");
        dotLabel.setStyle("-fx-text-fill: #D1D5DB;");
        
        Label sectionsLabel = new Label((cv.getSectionCount() != null ? cv.getSectionCount() : 0) + " sections");
        sectionsLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
        
        metaRow.getChildren().addAll(templateLabel, dotLabel, sectionsLabel);
        
        infoSection.getChildren().addAll(titleRow, metaRow);
        card.getChildren().addAll(thumbnailContainer, infoSection);
        
        // Click to edit
        thumbnailContainer.setOnMouseClicked(e -> openCVEditor(cv.getId()));
        
        // Hover effects
        card.setOnMouseEntered(e -> {
            thumbnailContainer.setStyle("-fx-background-color: white; -fx-background-radius: 4 4 0 0; -fx-border-color: #0F766E; -fx-border-width: 2 2 0 2; -fx-border-radius: 4 4 0 0;");
            infoSection.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 0 0 4 4; -fx-border-color: #0F766E; -fx-border-width: 0 2 2 2; -fx-border-radius: 0 0 4 4;");
        });
        card.setOnMouseExited(e -> {
            thumbnailContainer.setStyle("-fx-background-color: white; -fx-background-radius: 4 4 0 0; -fx-border-color: #E5E7EB; -fx-border-width: 1 1 0 1; -fx-border-radius: 4 4 0 0;");
            infoSection.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 4 4; -fx-border-color: #E5E7EB; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 4 4;");
        });
        
        return card;
    }
    
    private void generateCVThumbnail(CVListResponse cvInfo, StackPane container) {
        // Check cache first
        if (thumbnailCache.containsKey(cvInfo.getId())) {
            ImageView imageView = new ImageView(thumbnailCache.get(cvInfo.getId()));
            imageView.setFitWidth(CV_THUMB_WIDTH - 2);
            imageView.setFitHeight(CV_THUMB_HEIGHT - 2);
            container.getChildren().add(0, imageView);
            return;
        }
        
        // Show loading placeholder
        VBox placeholder = new VBox(8);
        placeholder.setAlignment(Pos.CENTER);
        FontIcon docIcon = new FontIcon("fas-file-alt");
        docIcon.setIconSize(32);
        docIcon.setIconColor(Color.web("#D1D5DB"));
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(20, 20);
        placeholder.getChildren().addAll(docIcon, progress);
        container.getChildren().add(0, placeholder);
        
        // Load full CV data and generate thumbnail
        cvService.getCVById(cvInfo.getId()).whenComplete((cv, error) -> {
            Platform.runLater(() -> {
                container.getChildren().remove(placeholder);
                
                WritableImage thumbnail;
                if (error != null || cv == null) {
                    thumbnail = createErrorThumbnail();
                } else {
                    thumbnail = CvPdfGenerator.generateThumbnail(cv, CV_THUMB_WIDTH, CV_THUMB_HEIGHT);
                    thumbnailCache.put(cvInfo.getId(), thumbnail);
                }
                
                ImageView imageView = new ImageView(thumbnail);
                imageView.setFitWidth(CV_THUMB_WIDTH - 2);
                imageView.setFitHeight(CV_THUMB_HEIGHT - 2);
                container.getChildren().add(0, imageView);
            });
        });
    }
    
    private WritableImage createErrorThumbnail() {
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(CV_THUMB_WIDTH, CV_THUMB_HEIGHT);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        
        gc.setFill(Color.web("#F9FAFB"));
        gc.fillRect(0, 0, CV_THUMB_WIDTH, CV_THUMB_HEIGHT);
        
        gc.setFill(Color.web("#D1D5DB"));
        gc.setFont(javafx.scene.text.Font.font("System", 40));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText("📄", CV_THUMB_WIDTH / 2.0, CV_THUMB_HEIGHT / 2.0);
        
        return canvas.snapshot(new javafx.scene.SnapshotParameters(), null);
    }

    private FontIcon createIcon(String iconCode, int size, String color) {
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(size);
        icon.setIconColor(Color.web(color));
        return icon;
    }

    private void openCVEditor(String cvId) {
        Map<String, Object> params = new HashMap<>();
        params.put("cvId", cvId);
        router.navigate(Route.SEEKER_CV_EDITOR, params);
    }
    
    private void showCVPreview(String cvId) {
        Map<String, Object> params = new HashMap<>();
        params.put("cvId", cvId);
        params.put("mode", "preview");
        router.navigate(Route.SEEKER_CV_EDITOR, params);
    }
    
    private void downloadAsPdf(String cvId) {
        cvService.getCVById(cvId).whenComplete((cv, error) -> {
            Platform.runLater(() -> {
                if (error != null || cv == null) {
                    Toast.error("Failed to load CV data");
                    return;
                }
                
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save CV as PDF");
                fileChooser.setInitialFileName(cv.getTitle() + ".pdf");
                fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
                );
                
                File file = fileChooser.showSaveDialog(cvsList.getScene().getWindow());
                if (file != null) {
                    try {
                        CvPdfGenerator.generatePdf(cv, file);
                        Toast.success("PDF saved successfully!");
                        java.awt.Desktop.getDesktop().open(file);
                    } catch (Exception e) {
                        Toast.error("Failed to generate PDF: " + e.getMessage());
                    }
                }
            });
        });
    }

    private void setDefaultCV(String cvId) {
        cvService.setDefaultCV(cvId).whenComplete((cv, error) -> {
            Platform.runLater(() -> {
                if (error != null) {
                    Toast.error("Failed to set default CV");
                    return;
                }
                Toast.success("Default CV updated!");
                loadMyCVs();
            });
        });
    }

    private void deleteCV(String cvId, String title) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete CV");
        alert.setHeaderText("Delete \"" + title + "\"?");
        alert.setContentText("This action cannot be undone.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                cvService.deleteCV(cvId).whenComplete((v, error) -> {
                    Platform.runLater(() -> {
                        if (error != null) {
                            Toast.error("Failed to delete CV");
                            return;
                        }
                        Toast.success("CV deleted");
                        thumbnailCache.remove(cvId);
                        loadMyCVs();
                    });
                });
            }
        });
    }

    @FXML
    private void onRefresh() {
        thumbnailCache.clear();
        loadTemplates();
        loadMyCVs();
    }

    private void showLoading(boolean show) {
        loadingContainer.setVisible(show);
        loadingContainer.setManaged(show);
    }

    private void showEmpty(boolean show) {
        emptyContainer.setVisible(show);
        emptyContainer.setManaged(show);
        cvsList.setVisible(!show);
        cvsList.setManaged(!show);
    }
}
