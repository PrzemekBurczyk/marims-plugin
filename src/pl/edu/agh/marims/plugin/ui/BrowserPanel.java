package pl.edu.agh.marims.plugin.ui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import pl.edu.agh.marims.plugin.Config;

import javax.swing.*;
import java.awt.*;

public class BrowserPanel extends JPanel {
    private JFXPanel jfxPanel;
    private WebView webView;
    private WebEngine webEngine;

    public BrowserPanel() {
        jfxPanel = new JFXPanel();
        Platform.setImplicitExit(false);
        this.setLayout(new BorderLayout());
        Platform.runLater(this::initWebView);
    }

    private void initWebView() {
        this.add(createWebView(), BorderLayout.CENTER);
    }

    private JFXPanel createWebView() {
        jfxPanel.setScene(createScene());
        return jfxPanel;
    }

    private Scene createScene() {
        StackPane root = new StackPane();
        Scene scene = new Scene(root);
        webView = new WebView();
        webEngine = webView.getEngine();
        root.getChildren().add(webView);
        return scene;
    }

    public void loadSession(String sessionId) {
        Platform.runLater(() -> webEngine.load(Config.SERVER_URL + sessionId));
    }

    public void closeSession() {
        Platform.runLater(() -> webEngine.load(null));
    }

}
