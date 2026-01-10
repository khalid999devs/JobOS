package com.jobos.desktop.controller.seeker;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.ApplicationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ApplicationsController implements Initializable {

    @FXML private VBox applicationsList;
    @FXML private VBox loadingContainer;
    @FXML private VBox emptyContainer;
    @FXML private Label countLabel;
    @FXML private Label pageLabel;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private HBox paginationContainer;

    private final ApplicationService applicationService = new ApplicationService();
    private int currentPage = 0;
    private int totalPages = 0;
    private static final int PAGE_SIZE = 15;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupFilters();
        loadApplications();
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList(
                "All Status", "Pending", "Reviewed", "Shortlisted", "Accepted", "Rejected"
        ));
        statusFilter.setValue("All Status");
    }

    @FXML
    private void onFilterChange() {
        currentPage = 0;
        loadApplications();
    }

    @FXML
    private void onRefresh() {
        loadApplications();
    }

    @FXML
    private void onBrowseJobs() {
        Router.getInstance().navigate(Route.SEEKER_JOBS);
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            loadApplications();
        }
    }

    @FXML
    private void onNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadApplications();
        }
    }

    private void loadApplications() {
        showLoading(true);
        
        applicationService.getMyApplications(currentPage, PAGE_SIZE).whenComplete((response, error) -> {
            Platform.runLater(() -> {
                showLoading(false);
                if (error != null) {
                    Toast.error("Failed to load applications");
                    showEmpty(true);
                    return;
                }
                renderApplications(response);
            });
        });
    }

    @SuppressWarnings("unchecked")
    private void renderApplications(Map<String, Object> response) {
        applicationsList.getChildren().clear();
        
        List<Map<String, Object>> applications = (List<Map<String, Object>>) response.get("applications");
        
        if (applications == null || applications.isEmpty()) {
            showEmpty(true);
            countLabel.setText("No applications yet");
            updatePagination(0, 0);
            return;
        }

        showEmpty(false);
        
        Integer total = (Integer) response.get("totalPages");
        Long totalElements = response.get("totalElements") instanceof Long ? 
                (Long) response.get("totalElements") : 
                ((Number) response.get("totalElements")).longValue();
        
        totalPages = total != null ? total : 1;
        countLabel.setText(totalElements + " application" + (totalElements != 1 ? "s" : ""));
        updatePagination(currentPage + 1, totalPages);
        
        String selectedStatus = statusFilter.getValue();
        
        for (Map<String, Object> app : applications) {
            String status = (String) app.get("status");
            if (!"All Status".equals(selectedStatus) && !selectedStatus.equalsIgnoreCase(status)) {
                continue;
            }
            applicationsList.getChildren().add(createApplicationCard(app));
        }
        
        if (applicationsList.getChildren().isEmpty()) {
            showEmpty(true);
        }
    }

    private VBox createApplicationCard(Map<String, Object> app) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 10; -fx-cursor: hand;");
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        StackPane icon = new StackPane();
        icon.setStyle("-fx-background-color: #F0FDFA; -fx-background-radius: 8; -fx-min-width: 44; -fx-min-height: 44;");
        FontIcon briefcase = new FontIcon("fas-briefcase");
        briefcase.setIconSize(18);
        briefcase.setIconColor(Color.web("#0F766E"));
        icon.getChildren().add(briefcase);
        
        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        
        Label titleLabel = new Label((String) app.get("jobTitle"));
        titleLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 15px; -fx-font-weight: 600;");
        
        String company = (String) app.get("company");
        String location = (String) app.get("location");
        Label companyLabel = new Label(company + (location != null ? " • " + location : ""));
        companyLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        titleBox.getChildren().addAll(titleLabel, companyLabel);
        
        String status = (String) app.get("status");
        HBox statusBadge = createStatusBadge(status);
        
        header.getChildren().addAll(icon, titleBox, statusBadge);
        
        HBox footer = new HBox(16);
        footer.setAlignment(Pos.CENTER_LEFT);
        
        String appliedAt = (String) app.get("appliedAt");
        Label dateLabel = new Label("Applied " + formatDate(appliedAt));
        dateLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");
        
        footer.getChildren().add(dateLabel);
        
        card.getChildren().addAll(header, footer);
        
        String applicationId = (String) app.get("id");
        card.setOnMouseClicked(e -> {
            if (applicationId != null) {
                Router.getInstance().navigate(Route.SEEKER_APPLICATION_DETAIL, applicationId);
            }
        });
        
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 10; -fx-border-color: #0F766E; -fx-border-width: 1; -fx-border-radius: 10; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 10; -fx-cursor: hand;"));
        
        return card;
    }

    private HBox createStatusBadge(String status) {
        HBox badge = new HBox();
        badge.setAlignment(Pos.CENTER);
        
        String bgColor;
        String textColor;
        
        switch (status != null ? status.toUpperCase() : "") {
            case "PENDING" -> { bgColor = "#FEF3C7"; textColor = "#D97706"; }
            case "REVIEWED" -> { bgColor = "#E0F2FE"; textColor = "#0284C7"; }
            case "SHORTLISTED" -> { bgColor = "#F3E8FF"; textColor = "#9333EA"; }
            case "ACCEPTED" -> { bgColor = "#DCFCE7"; textColor = "#16A34A"; }
            case "REJECTED" -> { bgColor = "#FEE2E2"; textColor = "#DC2626"; }
            default -> { bgColor = "#F3F4F6"; textColor = "#6B7280"; }
        }
        
        badge.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12; -fx-padding: 4 10;");
        
        Label label = new Label(formatStatus(status));
        label.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 11px; -fx-font-weight: 600;");
        
        badge.getChildren().add(label);
        return badge;
    }

    private String formatStatus(String status) {
        if (status == null) return "Unknown";
        return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
    }

    private String formatDate(String dateStr) {
        if (dateStr == null) return "recently";
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateStr.replace("Z", "").split("\\.")[0]);
            LocalDateTime now = LocalDateTime.now();
            long days = ChronoUnit.DAYS.between(dateTime, now);
            if (days == 0) return "today";
            if (days == 1) return "yesterday";
            if (days < 7) return days + " days ago";
            return dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        } catch (Exception e) {
            return "recently";
        }
    }

    private void showLoading(boolean show) {
        loadingContainer.setVisible(show);
        loadingContainer.setManaged(show);
    }

    private void showEmpty(boolean show) {
        emptyContainer.setVisible(show);
        emptyContainer.setManaged(show);
    }

    private void updatePagination(int current, int total) {
        if (total <= 1) {
            paginationContainer.setVisible(false);
            paginationContainer.setManaged(false);
            return;
        }
        paginationContainer.setVisible(true);
        paginationContainer.setManaged(true);
        pageLabel.setText("Page " + current + " of " + total);
        prevBtn.setDisable(currentPage <= 0);
        nextBtn.setDisable(currentPage >= total - 1);
    }
}
