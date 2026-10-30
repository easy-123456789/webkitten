package com.webkitten.browser;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Main extends Application {
    private static final String DEFAULT_HOME = "https://example.com";
    private static final String DOWNLOAD_DIR = "webkitten-downloads";

    private final Map<String, String> bookmarks = new LinkedHashMap<>();
    private final ObservableList<String> historyList = FXCollections.observableArrayList();
    private final ObservableList<String> downloadList = FXCollections.observableArrayList();
    private final TextArea devToolsArea = new TextArea();
    private final TabPane tabPane = new TabPane();
    private final AtomicInteger tabCounter = new AtomicInteger(1);

    @Override
    public void start(Stage stage) {
        initBookmarks();

        devToolsArea.setEditable(false);
        devToolsArea.setWrapText(true);
        devToolsArea.setPrefRowCount(8);
        appendDevTool("Webkitten dev tools ready.");

        Button newTabButton = new Button("+ Tab");
        newTabButton.setOnAction(event -> createTab(DEFAULT_HOME));

        Button closeTabButton = new Button("Close");
        closeTabButton.setOnAction(event -> {
            Tab current = tabPane.getSelectionModel().getSelectedItem();
            if (current != null) {
                tabPane.getTabs().remove(current);
            }
            if (tabPane.getTabs().isEmpty()) {
                createTab(DEFAULT_HOME);
            }
        });

        Label titleLabel = new Label("Webkitten");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        HBox chrome = new HBox(10, titleLabel, newTabButton, closeTabButton);
        chrome.setPadding(new Insets(10));
        chrome.setAlignment(Pos.CENTER_LEFT);

        VBox sidebar = buildSidebar();
        SplitPane contentArea = new SplitPane(tabPane, sidebar);
        contentArea.setDividerPositions(0.78);

        BorderPane root = new BorderPane();
        root.setTop(chrome);
        root.setCenter(contentArea);
        root.setBottom(devToolsArea);

        Scene scene = new Scene(root, 1400, 900);
        stage.setTitle("Webkitten Browser");
        stage.setScene(scene);
        stage.show();

        createTab(DEFAULT_HOME);
    }

    private VBox buildSidebar() {
        ListView<String> bookmarkView = new ListView<>();
        ListView<String> historyView = new ListView<>(historyList);
        ListView<String> downloadsView = new ListView<>(downloadList);

        bookmarkView.setItems(FXCollections.observableArrayList(bookmarks.keySet()));
        bookmarkView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String item = bookmarkView.getSelectionModel().getSelectedItem();
                if (item != null) {
                    String url = bookmarks.get(item);
                    if (url != null) {
                        openInCurrentTab(url);
                    }
                }
            }
        });

        historyView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String url = historyView.getSelectionModel().getSelectedItem();
                if (url != null) {
                    openInCurrentTab(url);
                }
            }
        });

        downloadsView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String entry = downloadsView.getSelectionModel().getSelectedItem();
                if (entry != null) {
                    appendDevTool("Selected download: " + entry);
                }
            }
        });

        Label bookmarksLabel = new Label("Bookmarks");
        bookmarksLabel.setStyle("-fx-font-weight: bold;");
        Label historyLabel = new Label("History");
        historyLabel.setStyle("-fx-font-weight: bold;");
        Label downloadsLabel = new Label("Downloads");
        downloadsLabel.setStyle("-fx-font-weight: bold;");

        VBox sidebar = new VBox(8,
                bookmarksLabel,
                bookmarkView,
                historyLabel,
                historyView,
                downloadsLabel,
                downloadsView);
        sidebar.setPadding(new Insets(8));
        sidebar.setPrefWidth(320);
        VBox.setVgrow(bookmarkView, Priority.ALWAYS);
        VBox.setVgrow(historyView, Priority.ALWAYS);
        VBox.setVgrow(downloadsView, Priority.ALWAYS);

        return sidebar;
    }

    private void openInCurrentTab(String url) {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected == null) {
            createTab(url);
            return;
        }

        WebEngine engine = (WebEngine) selected.getProperties().get("engine");
        TextField addressField = (TextField) selected.getProperties().get("address");
        if (engine != null) {
            loadUrl(engine, addressField, url);
        }
    }

    private void createTab(String url) {
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        TextField addressField = new TextField(url);
        addressField.setPrefWidth(700);
        addressField.setOnAction(event -> loadUrl(engine, addressField, addressField.getText()));

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
        homeButton.setOnAction(event -> loadUrl(engine, addressField, DEFAULT_HOME));

        Button bookmarkButton = new Button("Bookmark");
        bookmarkButton.setOnAction(event -> {
            String currentUrl = engine.getLocation();
            if (currentUrl != null && !currentUrl.isBlank()) {
                String title = engine.getTitle() != null && !engine.getTitle().isBlank()
                        ? engine.getTitle() : "Untitled";
                bookmarks.put(title, currentUrl);
                appendDevTool("Saved bookmark: " + title + " -> " + currentUrl);
                refreshBookmarksSidebar();
            }
        });

        Button downloadButton = new Button("Download");
        downloadButton.setOnAction(event -> downloadPage(engine.getLocation()));

        Button toolsButton = new Button("DevTools");
        toolsButton.setOnAction(event -> toggleDevTools());

        MenuButton bookmarkMenu = new MenuButton("Quick Bookmarks");
        for (Map.Entry<String, String> entry : bookmarks.entrySet()) {
            MenuItem item = new MenuItem(entry.getKey());
            item.setOnAction(e -> loadUrl(engine, addressField, entry.getValue()));
            bookmarkMenu.getItems().add(item);
        }

        HBox toolbar = new HBox(8, backButton, forwardButton, reloadButton, homeButton, bookmarkButton, downloadButton, toolsButton, addressField, bookmarkMenu);
        toolbar.setPadding(new Insets(8));
        HBox.setHgrow(addressField, Priority.ALWAYS);

        BorderPane tabContent = new BorderPane();
        tabContent.setTop(toolbar);
        tabContent.setCenter(webView);

        Tab tab = new Tab("Tab " + tabCounter.getAndIncrement());
        tab.setContent(tabContent);
        tab.getProperties().put("engine", engine);
        tab.getProperties().put("address", addressField);
        tab.setOnCloseRequest(event -> {
            if (tabPane.getTabs().size() == 1) {
                createTab(DEFAULT_HOME);
            }
        });

        engine.locationProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                addressField.setText(newValue);
                addHistory(newValue);
            }
        });

        engine.titleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                tab.setText(newValue.length() > 24 ? newValue.substring(0, 24) + "..." : newValue);
            }
        });

        engine.setOnStatusChanged(event -> appendDevTool("Status: " + event.getData()));
        engine.setOnAlert(event -> appendDevTool("Alert: " + event.getData()));

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        loadUrl(engine, addressField, url);
    }

    private void loadUrl(WebEngine engine, TextField addressField, String rawAddress) {
        String value = rawAddress == null ? "" : rawAddress.trim();
        if (value.isEmpty()) {
            return;
        }

        String normalized = value;
        if (!normalized.matches("(?i)^(https?|file)://.*")) {
            normalized = "https://" + normalized;
        }

        addressField.setText(normalized);
        engine.load(normalized);
    }

    private void addHistory(String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        historyList.remove(url);
        historyList.add(0, url);
        while (historyList.size() > 100) {
            historyList.remove(historyList.size() - 1);
        }
    }

    private void refreshBookmarksSidebar() {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getProperties().containsKey("bookmarkMenu")) {
                // no-op: bookmarks are refreshed on next tab creation.
            }
        }
    }

    private void toggleDevTools() {
        devToolsArea.setVisible(!devToolsArea.isVisible());
        devToolsArea.setManaged(devToolsArea.isVisible());
    }

    private void appendDevTool(String entry) {
        Platform.runLater(() -> {
            String text = devToolsArea.getText();
            String value = (text == null || text.isBlank() ? "" : text + "\n") + entry;
            devToolsArea.setText(value);
            devToolsArea.positionCaret(value.length());
        });
    }

    private void downloadPage(String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        String safeUrl = url;
        if (!safeUrl.startsWith("http://") && !safeUrl.startsWith("https://")) {
            safeUrl = "https://" + safeUrl;
        }

        Path downloadsRoot = Paths.get(System.getProperty("user.home"), DOWNLOAD_DIR);
        try {
            Files.createDirectories(downloadsRoot);
        } catch (IOException e) {
            appendDevTool("Could not create downloads folder: " + e.getMessage());
            return;
        }

        String fileName = sanitizeFileName(safeUrl);
        Path target = downloadsRoot.resolve(fileName + ".html");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(safeUrl)).GET().build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    try {
                        Files.writeString(target, response.body(), StandardCharsets.UTF_8);
                        Platform.runLater(() -> {
                            downloadList.add(0, "Saved: " + safeUrl + " -> " + target);
                            appendDevTool("Download complete: " + target);
                        });
                    } catch (IOException ex) {
                        Platform.runLater(() -> appendDevTool("Download failed: " + ex.getMessage()));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> appendDevTool("Download request failed: " + ex.getCause()));
                    return null;
                });
    }

    private String sanitizeFileName(String url) {
        String cleaned = url.replaceFirst("https?://", "");
        cleaned = cleaned.replaceAll("[^A-Za-z0-9._-]", "-");
        cleaned = cleaned.replaceAll("-+", "-");
        return cleaned.isBlank() ? "download" : cleaned;
    }

    private void initBookmarks() {
        bookmarks.put("Example", DEFAULT_HOME);
        bookmarks.put("GitHub", "https://github.com");
        bookmarks.put("Google", "https://google.com");
        bookmarks.put("MDN", "https://developer.mozilla.org");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
