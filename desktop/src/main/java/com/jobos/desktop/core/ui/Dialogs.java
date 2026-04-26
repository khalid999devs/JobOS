package com.jobos.desktop.core.ui;

import javafx.application.Platform;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Objects;

public final class Dialogs {

    private static final double MAX_DIALOG_WIDTH = 560;
    private static StackPane rootContainer;

    private Dialogs() {
    }

    public static void setContainer(StackPane root) {
        rootContainer = root;
    }

    public static <T> Dialog<T> prepare(Dialog<T> dialog) {
        DialogPane pane = dialog.getDialogPane();
        applyTheme(pane);
        clampDialogPane(pane);
        attachToMainWindow(dialog);
        dialog.setResizable(false);
        dialog.setOnShown(e -> Platform.runLater(() -> normalizeWindow(dialog)));
        return dialog;
    }

    private static void applyTheme(DialogPane pane) {
        String theme = Objects.requireNonNull(Dialogs.class.getResource("/css/theme.css")).toExternalForm();
        String components = Objects.requireNonNull(Dialogs.class.getResource("/css/components.css")).toExternalForm();

        if (!pane.getStylesheets().contains(theme)) {
            pane.getStylesheets().add(theme);
        }
        if (!pane.getStylesheets().contains(components)) {
            pane.getStylesheets().add(components);
        }
        if (!pane.getStyleClass().contains("jobos-dialog-pane")) {
            pane.getStyleClass().add("jobos-dialog-pane");
        }
    }

    private static void clampDialogPane(DialogPane pane) {
        pane.setPrefWidth(MAX_DIALOG_WIDTH);
        pane.setMaxWidth(MAX_DIALOG_WIDTH);
        pane.setMaxHeight(760);
    }

    private static <T> void attachToMainWindow(Dialog<T> dialog) {
        Window owner = getOwner();
        if (owner == null) {
            return;
        }

        try {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        } catch (IllegalStateException ignored) {
            // Dialog was already initialized or shown.
        }
    }

    private static <T> void normalizeWindow(Dialog<T> dialog) {
        Window window = dialog.getDialogPane().getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setResizable(false);
            stage.sizeToScene();
        }

        Window owner = getOwner();
        if (owner != null && window != null) {
            centerOnOwner(window, owner);
        }
    }

    private static void centerOnOwner(Window window, Window owner) {
        double ownerWidth = owner.getWidth();
        double ownerHeight = owner.getHeight();
        if (ownerWidth <= 0 || ownerHeight <= 0) {
            return;
        }

        window.setX(owner.getX() + (ownerWidth - window.getWidth()) / 2);
        window.setY(owner.getY() + (ownerHeight - window.getHeight()) / 2);
    }

    private static Window getOwner() {
        if (rootContainer == null || rootContainer.getScene() == null) {
            return null;
        }
        return rootContainer.getScene().getWindow();
    }
}
