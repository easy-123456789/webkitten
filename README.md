# Webkitten

A small Java browser built with JavaFX WebView, which uses the platform WebKit engine.

## Features

- Back, forward, reload, and home controls
- URL bar for loading pages
- Simple browser window sized for desktop use

## Requirements

- Java 17+
- Maven

## Run

```bash
mvn clean javafx:run
```

## Project layout

- `pom.xml` - Maven project configuration
- `src/main/java/com/webkitten/browser/Main.java` - Browser UI and loading logic

## Notes

The browser uses JavaFX's `WebView`, which depends on the native WebKit implementation available on the host OS. Some environments may need JavaFX runtime support or additional OS dependencies.
