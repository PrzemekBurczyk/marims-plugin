package pl.edu.agh.marims.plugin.ui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;

public class Browser extends JPanel {

    public Browser() {
        Platform.runLater(this::initWebView);
    }

    private void initWebView() {
        this.add(createWebView(), BorderLayout.CENTER);
    }

    private JFXPanel createWebView() {
        JFXPanel jfxPanel = new JFXPanel();
        jfxPanel.setScene(createScene());
        return jfxPanel;
    }

    private Scene createScene() {
        StackPane root = new StackPane();
        Scene scene = new Scene(root);
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webEngine.load("http://marims.pl");
        root.getChildren().add(webView);
        return scene;
    }
}
