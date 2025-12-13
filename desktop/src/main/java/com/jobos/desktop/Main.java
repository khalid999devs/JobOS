package com.jobos.desktop;

import com.jobos.desktop.ui.AppView;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        AppView appView = new AppView();
        appView.show(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
