package pl.edu.agh.marims.plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MarimsToolWindowFactory implements ToolWindowFactory {
    private ToolWindow toolWindow;
    private JPanel contentPanel;
    private JFXPanel jfxPanel;
    private WebView webView;
    private WebEngine webEngine;

    public MarimsToolWindowFactory() {
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(contentPanel, "", false);
        toolWindow.getContentManager().addContent(content);

        Platform.runLater(this::initWebView);
    }

    private void initWebView() {
        jfxPanel.setScene(createScene());
    }

    private Scene createScene() {
        StackPane root = new StackPane();
        Scene scene = new Scene(root);
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.load("http://marims.pl");
        root.getChildren().add(webView);
        return scene;
    }
}
