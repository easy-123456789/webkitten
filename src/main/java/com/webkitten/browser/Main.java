package com.webkitten.browser;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        TextField addressBar = new TextField("https://example.com");
        addressBar.setPrefWidth(700);
        addressBar.setOnAction(event -> loadAddress(engine, addressBar.getText()));

        Button backButton = new Button("Back");
        backButton.setOnAction(event -> {
            if (engine.getHistory().getCurrentIndex() > 0) {
                engine.getHistory().go(-1);
            }
        });

        Button forwardButton = new Button("Forward");
        forwardButton.setOnAction(event -> {
            if (engine.getHistory().getCurrentIndex() < engine.getHistory().getEntries().size() - 1) {
                engine.getHistory().go(1);
            }
        });

        Button reloadButton = new Button("Reload");
        reloadButton.setOnAction(event -> engine.reload());

        Button homeButton = new Button("Home");
        homeButton.setOnAction(event -> {
            engine.load("https://example.com");
            addressBar.setText("https://example.com");
        });

        engine.locationProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                addressBar.setText(newValue);
            }
        });

        HBox toolbar = new HBox(10, backButton, forwardButton, reloadButton, homeButton, addressBar);
        toolbar.setPadding(new Insets(8));

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(webView);

        Scene scene = new Scene(root, 1280, 860);
        stage.setTitle("Webkitten");
        stage.setScene(scene);
        stage.show();

        engine.load("https://example.com");
    }

    private void loadAddress(WebEngine engine, String rawAddress) {
        String address = rawAddress == null ? "" : rawAddress.trim();
        if (address.isEmpty()) {
            return;
        }

        String normalized = address;
        if (!normalized.matches("(?i)^(https?|file)://.*")) {
            normalized = "https://" + normalized;
        }

        engine.load(normalized);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
