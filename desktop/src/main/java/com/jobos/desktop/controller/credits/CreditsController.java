package com.jobos.desktop.controller.credits;

import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.Dialogs;
import com.jobos.desktop.core.ui.SkeletonLoader;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.CreditService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CreditsController implements Initializable {
    
    @FXML private Label creditsBalance;
    @FXML private Label usedCredits;
    @FXML private Label planLabel;
    @FXML private VBox transactionsList;
    @FXML private HBox paginationContainer;
    @FXML private HBox plansContainer;
    
    private final Router router = Router.getInstance();
    
    private final CreditService creditService = new CreditService();
    private int currentPage = 0;
    private int totalPages = 1;
    private String currentPlanId = null;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadCreditsData();
        loadPlans();
        loadTransactions();
    }
    
    private void loadPlans() {
        creditService.getPlans()
            .thenAccept(response -> {
                Platform.runLater(() -> renderPlans(response));
            })
            .exceptionally(e -> {
                Platform.runLater(() -> renderFallbackPlans());
                return null;
            });
    }
    
    @SuppressWarnings("unchecked")
    private void renderPlans(Map<String, Object> response) {
        if (plansContainer == null) return;
        plansContainer.getChildren().clear();
        
        Object resultObj = response.get("result");
        List<Map<String, Object>> plans = null;
        
        if (resultObj instanceof List) {
            plans = (List<Map<String, Object>>) resultObj;
        } else if (response.containsKey("plans")) {
            plans = (List<Map<String, Object>>) response.get("plans");
        }
        
        if (plans == null || plans.isEmpty()) {
            renderFallbackPlans();
            return;
        }
        
        for (Map<String, Object> plan : plans) {
            boolean isCurrentPlan = Boolean.TRUE.equals(plan.get("isCurrentPlan"));
            if (isCurrentPlan) {
                currentPlanId = getString(plan, "id");
            }
            plansContainer.getChildren().add(createPlanCard(plan, isCurrentPlan));
        }
    }
    
    private void renderFallbackPlans() {
        if (plansContainer == null) return;
        plansContainer.getChildren().clear();
        
        // Free Plan
        plansContainer.getChildren().add(createStaticPlanCard("FREE", "Free", "$0", "50 credits/month", 
            Arrays.asList("3 CVs", "10 Job Applications", "Basic Templates"), false, "FREE".equals(planLabel.getText())));
        
        // Pro Plan
        plansContainer.getChildren().add(createStaticPlanCard("PRO", "Pro", "$9.99", "200 credits/month",
            Arrays.asList("10 CVs", "50 Job Applications", "Premium Templates", "AI Assistance"), true, "PRO".equals(planLabel.getText())));
        
        // Enterprise Plan
        plansContainer.getChildren().add(createStaticPlanCard("ENTERPRISE", "Enterprise", "$29.99", "Unlimited credits",
            Arrays.asList("Unlimited CVs", "Unlimited Applications", "All Templates", "Priority Support"), false, "ENTERPRISE".equals(planLabel.getText())));
    }
    
    private VBox createPlanCard(Map<String, Object> plan, boolean isCurrentPlan) {
        String planType = getString(plan, "planType");
        String name = getString(plan, "name");
        Object priceObj = plan.get("monthlyPrice");
        String price = priceObj != null ? "$" + priceObj.toString() : "Free";
        Object creditsObj = plan.get("monthlyCredits");
        String credits = creditsObj != null ? creditsObj + " credits/month" : "Limited";
        
        boolean isPopular = "PRO".equalsIgnoreCase(planType);
        
        VBox card = new VBox(12);
        card.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPadding(new Insets(20));
        
        if (isPopular) {
            card.setStyle("-fx-background-color: #F0FDFA; -fx-background-radius: 12; -fx-border-color: #0F766E; -fx-border-radius: 12; -fx-border-width: 2;");
        } else if (isCurrentPlan) {
            card.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 12; -fx-border-color: #D1D5DB; -fx-border-radius: 12; -fx-border-width: 1;");
        } else {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-border-width: 1;");
        }
        
        if (isPopular) {
            Label popular = new Label("MOST POPULAR");
            popular.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
            card.getChildren().add(popular);
        }
        
        Label nameLabel = new Label(name != null ? name : planType);
        nameLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-weight: bold; -fx-font-size: 12px;");
        
        Label priceLabel = new Label(price.equals("$0") || price.equals("$0.00") ? "Free" : price + "/mo");
        priceLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        
        Label creditsLabel = new Label(credits);
        creditsLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        // Features
        VBox features = new VBox(8);
        features.setPadding(new Insets(12, 0, 12, 0));
        
        Integer maxCVs = getInteger(plan, "maxCVs");
        Integer maxApps = getInteger(plan, "maxJobApplications");
        Boolean hasAI = getBoolean(plan, "hasAIAssistance");
        Boolean hasPremiumTemplates = getBoolean(plan, "hasPremiumTemplates");
        Boolean hasPrioritySupport = getBoolean(plan, "hasPrioritySupport");
        
        if (maxCVs != null) features.getChildren().add(createFeatureRow(maxCVs == -1 ? "Unlimited CVs" : maxCVs + " CVs"));
        if (maxApps != null) features.getChildren().add(createFeatureRow(maxApps == -1 ? "Unlimited Applications" : maxApps + " Job Applications"));
        if (Boolean.TRUE.equals(hasPremiumTemplates)) features.getChildren().add(createFeatureRow("Premium Templates"));
        if (Boolean.TRUE.equals(hasAI)) features.getChildren().add(createFeatureRow("AI Assistance"));
        if (Boolean.TRUE.equals(hasPrioritySupport)) features.getChildren().add(createFeatureRow("Priority Support"));
        
        Region buttonSpacer = new Region();
        VBox.setVgrow(buttonSpacer, Priority.ALWAYS);
        
        Button actionBtn;
        if (isCurrentPlan) {
            actionBtn = new Button("Current Plan");
            actionBtn.setStyle("-fx-background-color: #D1D5DB; -fx-text-fill: #6B7280; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 24; -fx-font-size: 13px;");
            actionBtn.setDisable(true);
        } else {
            actionBtn = new Button(isPopular ? "Upgrade Now" : "Select Plan");
            actionBtn.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 24; -fx-cursor: hand; -fx-font-size: 13px;");
            String planId = getString(plan, "id");
            actionBtn.setOnAction(e -> subscribeToPlan(planId, name));
        }
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setPrefHeight(42);
        
        card.getChildren().addAll(nameLabel, priceLabel, creditsLabel, features, buttonSpacer, actionBtn);
        return card;
    }
    
    private VBox createStaticPlanCard(String id, String name, String price, String credits, List<String> featuresList, boolean isPopular, boolean isCurrentPlan) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPadding(new Insets(20));
        
        if (isPopular) {
            card.setStyle("-fx-background-color: #F0FDFA; -fx-background-radius: 12; -fx-border-color: #0F766E; -fx-border-radius: 12; -fx-border-width: 2;");
        } else if (isCurrentPlan) {
            card.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 12; -fx-border-color: #D1D5DB; -fx-border-radius: 12; -fx-border-width: 1;");
        } else {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-border-width: 1;");
        }
        
        if (isPopular) {
            Label popular = new Label("MOST POPULAR");
            popular.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
            card.getChildren().add(popular);
        }
        
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-weight: bold; -fx-font-size: 12px;");
        
        Label priceLabel = new Label(price.equals("$0") ? "Free" : price + "/mo");
        priceLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        
        Label creditsLabel = new Label(credits);
        creditsLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        VBox features = new VBox(8);
        features.setPadding(new Insets(12, 0, 12, 0));
        for (String feature : featuresList) {
            features.getChildren().add(createFeatureRow(feature));
        }
        
        Region buttonSpacer2 = new Region();
        VBox.setVgrow(buttonSpacer2, Priority.ALWAYS);
        
        Button actionBtn;
        if (isCurrentPlan) {
            actionBtn = new Button("Current Plan");
            actionBtn.setStyle("-fx-background-color: #D1D5DB; -fx-text-fill: #6B7280; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 24; -fx-font-size: 13px;");
            actionBtn.setDisable(true);
        } else {
            actionBtn = new Button(isPopular ? "Upgrade Now" : "Select Plan");
            actionBtn.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 12 24; -fx-cursor: hand; -fx-font-size: 13px;");
            actionBtn.setOnAction(e -> subscribeToPlan(id, name));
        }
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setPrefHeight(42);
        
        card.getChildren().addAll(nameLabel, priceLabel, creditsLabel, features, buttonSpacer2, actionBtn);
        return card;
    }
    
    private HBox createFeatureRow(String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        FontIcon check = new FontIcon("fas-check");
        check.setIconSize(12);
        check.setIconColor(javafx.scene.paint.Color.web("#10B981"));
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");
        row.getChildren().addAll(check, label);
        return row;
    }
    
    private void subscribeToPlan(String planId, String planName) {
        showPlanPaymentDialog(planId, planName);
    }
    
    private void showPlanPaymentDialog(String planId, String planName) {
        String price = switch (planId.toUpperCase()) {
            case "FREE" -> "$0.00";
            case "PRO" -> "$9.99";
            case "PREMIUM", "ENTERPRISE" -> "$19.99";
            default -> "$9.99";
        };
        
        if ("FREE".equalsIgnoreCase(planId)) {
            processSubscription(planId, planName);
            return;
        }
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Subscribe to " + planName);
        dialog.setHeaderText("Complete Your Subscription");
        
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);
        
        VBox summary = new VBox(8);
        summary.setStyle("-fx-background-color: #F3F4F6; -fx-padding: 16; -fx-background-radius: 8;");
        Label summaryTitle = new Label("Subscription Summary");
        summaryTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        HBox planRow = new HBox();
        planRow.setAlignment(Pos.CENTER_LEFT);
        Label planText = new Label(planName + " Plan (Monthly)");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label priceText = new Label(price + "/mo");
        priceText.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F766E;");
        planRow.getChildren().addAll(planText, spacer, priceText);
        summary.getChildren().addAll(summaryTitle, planRow);
        
        VBox cardSection = new VBox(12);
        Label cardLabel = new Label("Payment Details");
        cardLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        TextField cardNumber = new TextField();
        cardNumber.setPromptText("4242 4242 4242 4242");
        cardNumber.setStyle("-fx-pref-height: 40;");
        
        HBox cardExtras = new HBox(12);
        TextField expiry = new TextField();
        expiry.setPromptText("MM/YY");
        expiry.setPrefWidth(100);
        TextField cvv = new TextField();
        cvv.setPromptText("CVV");
        cvv.setPrefWidth(80);
        cardExtras.getChildren().addAll(expiry, cvv);
        
        Label secureNote = new Label("🔒 Your payment is secure and encrypted");
        secureNote.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");
        
        cardSection.getChildren().addAll(cardLabel, cardNumber, cardExtras, secureNote);
        
        content.getChildren().addAll(summary, cardSection);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Subscribe " + price + "/mo");
        okButton.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-font-weight: bold;");
        
        Dialogs.prepare(dialog).showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                processSubscription(planId, planName);
            }
        });
    }
    
    private void processSubscription(String planId, String planName) {
        Toast.info("Processing subscription...");
        
        creditService.subscribeToPlan(planId)
            .thenAccept(response -> Platform.runLater(() -> {
                Toast.success("Successfully subscribed to " + planName + "!");
                loadCreditsData();
                loadPlans();
                router.refreshShellCredits();
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    Toast.error("Subscription failed: " + e.getMessage());
                });
                return null;
            });
    }
    
    private Integer getInteger(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return null;
    }
    
    private Boolean getBoolean(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return null;
    }
    
    private void loadCreditsData() {
        CompletableFuture.runAsync(() -> {
            try {
                var balance = creditService.getBalance();
                Platform.runLater(() -> {
                    if (balance != null) {
                        creditsBalance.setText(String.valueOf(balance.getCredits()));
                        usedCredits.setText(String.valueOf(balance.getUsedCredits()));
                        String plan = balance.getPlan();
                        if (plan != null && !plan.isEmpty()) {
                            planLabel.setText(plan.toUpperCase());
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    creditsBalance.setText("0");
                    usedCredits.setText("0");
                    planLabel.setText("FREE");
                });
            }
        });
    }

    private void loadTransactions() {
        if (transactionsList == null) return;
        
        // Show skeleton loading in transactions list
        transactionsList.getChildren().setAll(SkeletonLoader.createSkeletonList(5));
        
        creditService.getTransactions(currentPage, 10)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    renderTransactions(response);
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    showEmptyTransactions("Unable to load transactions");
                });
                return null;
            });
    }

    @SuppressWarnings("unchecked")
    private void renderTransactions(Map<String, Object> response) {
        transactionsList.getChildren().clear();
        
        Object resultObj = response.get("result");
        Map<String, Object> pageData = null;
        
        if (resultObj instanceof Map) {
            pageData = (Map<String, Object>) resultObj;
        } else {
            pageData = response;
        }
        
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageData.get("content");
        if (content == null) {
            content = (List<Map<String, Object>>) pageData.get("transactions");
        }
        
        totalPages = pageData.get("totalPages") != null ? ((Number) pageData.get("totalPages")).intValue() : 1;
        
        if (content == null || content.isEmpty()) {
            showEmptyTransactions("No transactions yet");
            return;
        }
        
        for (Map<String, Object> transaction : content) {
            transactionsList.getChildren().add(createTransactionRow(transaction));
        }
        
        renderPagination();
    }

    private HBox createTransactionRow(Map<String, Object> transaction) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #E5E7EB; -fx-border-radius: 8;");
        
        String type = getString(transaction, "type");
        String description = getString(transaction, "description");
        Object amountObj = transaction.get("amount");
        int amount = amountObj != null ? ((Number) amountObj).intValue() : 0;
        String createdAt = getString(transaction, "createdAt");
        
        Label icon = new Label(amount > 0 ? "📈" : "📉");
        icon.setStyle("-fx-font-size: 20px;");
        
        VBox details = new VBox(2);
        HBox.setHgrow(details, Priority.ALWAYS);
        
        Label descLabel = new Label(description != null ? description : (type != null ? type : "Transaction"));
        descLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
        
        Label dateLabel = new Label(createdAt != null ? formatDate(createdAt) : "");
        dateLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
        
        details.getChildren().addAll(descLabel, dateLabel);
        
        Label amountLabel = new Label((amount >= 0 ? "+" : "") + amount + " credits");
        amountLabel.setStyle("-fx-font-weight: bold; " + 
            (amount >= 0 ? "-fx-text-fill: #10B981;" : "-fx-text-fill: #EF4444;"));
        
        row.getChildren().addAll(icon, details, amountLabel);
        
        return row;
    }

    private void showEmptyTransactions(String message) {
        transactionsList.getChildren().clear();
        
        VBox emptyState = new VBox(12);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(32));
        
        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size: 32px;");
        
        Label text = new Label(message);
        text.setStyle("-fx-text-fill: #6B7280;");
        
        emptyState.getChildren().addAll(icon, text);
        transactionsList.getChildren().add(emptyState);
    }

    private void renderPagination() {
        if (paginationContainer == null) return;
        paginationContainer.getChildren().clear();
        
        if (totalPages <= 1) return;
        
        Button prevBtn = new Button("← Previous");
        prevBtn.getStyleClass().add("button-secondary");
        prevBtn.setDisable(currentPage == 0);
        prevBtn.setOnAction(e -> {
            currentPage--;
            loadTransactions();
        });
        
        Label pageLabel = new Label("Page " + (currentPage + 1) + " of " + totalPages);
        pageLabel.setStyle("-fx-text-fill: #6B7280;");
        
        Button nextBtn = new Button("Next →");
        nextBtn.getStyleClass().add("button-secondary");
        nextBtn.setDisable(currentPage >= totalPages - 1);
        nextBtn.setOnAction(e -> {
            currentPage++;
            loadTransactions();
        });
        
        paginationContainer.getChildren().addAll(prevBtn, pageLabel, nextBtn);
    }

    @FXML
    private void onPurchase200() {
        showPaymentGateway(200, "$5");
    }

    @FXML
    private void onPurchase800() {
        showPaymentGateway(800, "$10");
    }

    @FXML
    private void onPurchase2000() {
        showPaymentGateway(2000, "$20");
    }
    
    private void showPaymentGateway(int credits, String price) {
        // Create simulated payment dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Payment Gateway");
        dialog.setHeaderText("Complete Your Purchase");
        
        // Payment form
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);
        
        // Order summary
        VBox summary = new VBox(8);
        summary.setStyle("-fx-background-color: #F3F4F6; -fx-padding: 16; -fx-background-radius: 8;");
        Label summaryTitle = new Label("Order Summary");
        summaryTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        HBox creditsRow = new HBox();
        creditsRow.setAlignment(Pos.CENTER_LEFT);
        Label creditsText = new Label(credits + " Credits");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label priceText = new Label(price);
        priceText.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F766E;");
        creditsRow.getChildren().addAll(creditsText, spacer, priceText);
        summary.getChildren().addAll(summaryTitle, creditsRow);
        
        // Card details (simulated)
        VBox cardSection = new VBox(12);
        Label cardLabel = new Label("Card Details");
        cardLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        TextField cardNumber = new TextField();
        cardNumber.setPromptText("4242 4242 4242 4242");
        cardNumber.setStyle("-fx-pref-height: 40;");
        
        HBox cardExtras = new HBox(12);
        TextField expiry = new TextField();
        expiry.setPromptText("MM/YY");
        expiry.setPrefWidth(100);
        TextField cvv = new TextField();
        cvv.setPromptText("CVV");
        cvv.setPrefWidth(80);
        cardExtras.getChildren().addAll(expiry, cvv);
        
        Label secureNote = new Label("🔒 Your payment is secure and encrypted");
        secureNote.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");
        
        cardSection.getChildren().addAll(cardLabel, cardNumber, cardExtras, secureNote);
        
        content.getChildren().addAll(summary, cardSection);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Pay " + price);
        okButton.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-font-weight: bold;");
        
        Dialogs.prepare(dialog).showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                processPayment(credits, price);
            }
        });
    }
    
    private void processPayment(int credits, String price) {
        Toast.info("Processing payment...");
        
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {}
        }).thenRun(() -> {
            Platform.runLater(() -> {
                purchaseCredits(credits, price);
            });
        });
    }

    private void purchaseCredits(int amount, String price) {
        creditService.purchaseCredits(amount, "SIMULATED")
            .thenAccept(response -> Platform.runLater(() -> {
                Toast.success(amount + " credits added to your account!");
                loadCreditsData();
                loadTransactions();
                router.refreshShellCredits();
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    Toast.error("Purchase failed. Please try again.");
                });
                return null;
            });
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private String formatDate(String dateStr) {
        try {
            LocalDateTime dt = LocalDateTime.parse(dateStr.substring(0, 19));
            return dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        } catch (Exception e) {
            return dateStr;
        }
    }
}
