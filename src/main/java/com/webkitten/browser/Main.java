package com.webkitten.browser;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Main extends Application {
    private static final String DEFAULT_HOME = "https://example.com";
    private static final String FALLBACK_ERROR_IMAGE = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString((
            "<svg xmlns='http://www.w3.org/2000/svg' width='600' height='400' viewBox='0 0 600 400'>" +
                    "<defs><linearGradient id='bg' x1='0' x2='1'><stop offset='0%' stop-color='#0f172a'/><stop offset='100%' stop-color='#1e293b'/></linearGradient></defs>" +
                    "<rect width='600' height='400' rx='24' fill='url(#bg)'/>" +
                    "<circle cx='300' cy='150' r='72' fill='#f97316' fill-opacity='0.18'/>" +
                    "<path d='M300 90 L430 290 A30 30 0 0 1 370 320 H230 A30 30 0 0 1 170 290 Z' fill='#f97316' opacity='0.9'/>" +
                    "<circle cx='300' cy='210' r='12' fill='#fff7ed'/>" +
                    "<rect x='288' y='150' width='24' height='52' rx='10' fill='#fff7ed'/>" +
                    "<text x='300' y='330' text-anchor='middle' font-family='Arial, sans-serif' font-size='32' fill='#f8fafc' font-weight='700'>Page unavailable</text>" +
                    "</svg>"
    ).getBytes(StandardCharsets.UTF_8));

    private WebView webView;
    private WebEngine engine;
    private StackPane errorPane;
    private Label errorMessageLabel;

    @Override
    public void start(Stage stage) {
        webView = new WebView();
        engine = webView.getEngine();

        TextField addressBar = new TextField(DEFAULT_HOME);
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
            engine.load(DEFAULT_HOME);
            addressBar.setText(DEFAULT_HOME);
        });

        engine.locationProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                addressBar.setText(newValue);
            }
        });

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.FAILED) {
                showErrorOverlay("Unable to load this page.");
            } else if (newState == Worker.State.SUCCEEDED) {
                hideErrorOverlay();
            }
        });

        engine.setOnError((WebErrorEvent event) -> {
            showErrorOverlay(event.getMessage() != null ? event.getMessage() : "Unable to load this page.");
        });

        errorPane = buildErrorPane();
        StackPane contentPane = new StackPane(webView, errorPane);

        HBox toolbar = new HBox(10, backButton, forwardButton, reloadButton, homeButton, addressBar);
        toolbar.setPadding(new Insets(8));

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(contentPane);

        Scene scene = new Scene(root, 1280, 860);
        stage.setTitle("Webkitten");
        stage.setScene(scene);
        stage.show();

        engine.load(DEFAULT_HOME);
    }

    private StackPane buildErrorPane() {
        ImageView errorImageView = createErrorImageView();

        Label titleLabel = new Label("Page unavailable");
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: white;");

        errorMessageLabel = new Label();
        errorMessageLabel.setWrapText(true);
        errorMessageLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: bold;");

        VBox errorBox = new VBox(12, errorImageView, titleLabel, errorMessageLabel);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPadding(new Insets(20));

        StackPane pane = new StackPane(errorBox);
        pane.setVisible(false);
        pane.setStyle("-fx-background-color: rgba(15, 23, 42, 0.82);");
        return pane;
    }

    private ImageView createErrorImageView() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(420);
        imageView.setPreserveRatio(true);

        var resource = getClass().getResource("/error.png");
        if (resource != null) {
            imageView.setImage(new Image(resource.toExternalForm()));
        } else {
            imageView.setImage(new Image(FALLBACK_ERROR_IMAGE));
        }

        return imageView;
    }

    private void showErrorOverlay(String message) {
        Platform.runLater(() -> {
            errorMessageLabel.setText(message);
            errorPane.setVisible(true);
            errorPane.toFront();
        });
    }

    private void hideErrorOverlay() {
        Platform.runLater(() -> errorPane.setVisible(false));
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

        hideErrorOverlay();
        engine.load(normalized);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
