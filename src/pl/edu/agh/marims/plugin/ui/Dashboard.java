package pl.edu.agh.marims.plugin.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import org.jetbrains.annotations.NotNull;
import pl.edu.agh.marims.plugin.network.FileRequestBody;
import pl.edu.agh.marims.plugin.network.MarimsApiClient;
import pl.edu.agh.marims.plugin.network.MarimsService;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Dashboard implements ToolWindowFactory {
    private DefaultListModel<String> listModel;

    private JList<String> filesList;
    private JPanel contentPanel;
    private JLabel title;
    private JButton chooseFileButton;
    private JLabel chooseFileLabel;
    private JButton sendFileButton;
    private JProgressBar sendFileProgressBar;

    private Project project;
    private ToolWindow toolWindow;
    private File selectedFile;

    private MarimsService marimsService = MarimsApiClient.getInstance().getMarimsService();

    public Dashboard() {
        initInterface();
        initListeners();
        fetchData();
    }

    private void initInterface() {
        listModel = new DefaultListModel<>();
        filesList.setModel(listModel);
    }

    private void initListeners() {
        chooseFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(project.getBasePath());
//            FileNameExtensionFilter filter = new FileNameExtensionFilter("Android APKs", "apk");
//            fileChooser.setFileFilter(filter);
            int selectedOption = fileChooser.showOpenDialog(contentPanel);
            if (selectedOption == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                sendFileButton.setEnabled(true);
                try {
                    chooseFileLabel.setText(selectedFile.getCanonicalPath());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        sendFileButton.addActionListener(e -> {
            RequestBody file = new FileRequestBody(selectedFile, (current, max) -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    sendFileProgressBar.setValue((int) (current * 100 / max));
                });
            });
            RequestBody applicationName = RequestBody.create(MediaType.parse("text/plain"), "test");
            RequestBody applicarionVersion = RequestBody.create(MediaType.parse("text/plain"), "0.7");
            marimsService.postFile(applicationName, applicarionVersion, file).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Response<Void> response, Retrofit retrofit) {
                    fetchData();
                }

                @Override
                public void onFailure(Throwable throwable) {

                }
            });
        });
    }

    private void fetchData() {
        marimsService.getFiles().enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Response<List<String>> response, Retrofit retrofit) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    listModel.clear();
                    response.body().forEach((file) -> listModel.addElement(file));
                });
            }

            @Override
            public void onFailure(Throwable throwable) {

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
