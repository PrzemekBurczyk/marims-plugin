package pl.edu.agh.marims.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;

public class Dashboard implements ToolWindowFactory {
    private JList buildsList;
    private JPanel contentPanel;
    private JLabel title;
    private JButton chooseFileButton;
    private JLabel chooseFileLabel;

    private Project project;
    private ToolWindow toolWindow;
    private File selectedFile;

    public Dashboard() {
        chooseFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(project.getBasePath());
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Android APKs", "apk");
            fileChooser.setFileFilter(filter);
            int selectedOption = fileChooser.showOpenDialog(contentPanel);
            if (selectedOption == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                try {
                    chooseFileLabel.setText(selectedFile.getCanonicalPath());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(contentPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

}
