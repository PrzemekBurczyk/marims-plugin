package pl.edu.agh.marims.plugin.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import net.dongliu.apk.parser.ApkParser;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.jetbrains.annotations.NotNull;
import pl.edu.agh.marims.plugin.network.FileRequestBody;
import pl.edu.agh.marims.plugin.network.MarimsApiClient;
import pl.edu.agh.marims.plugin.network.MarimsService;
import pl.edu.agh.marims.plugin.util.Version;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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
    private JLabel applicationNameTextField;
    private JLabel applicationVersionTextField;
    private JLabel applicationVersionCodeTextField;
    private JList sessionsList;

    private Project project;
    private ToolWindow toolWindow;

    private File selectedFile;
    private String applicationName;
    private String applicationVersion;
    private Long applicationVersionCode;

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

    private VirtualFile getAaptFile() {
        Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (projectSdk != null) {
            VirtualFile sdkHomeDirectory = projectSdk.getHomeDirectory();
            if (sdkHomeDirectory != null && sdkHomeDirectory.exists()) {
                VirtualFile buildToolsDirectory = sdkHomeDirectory.findChild("build-tools");
                if (buildToolsDirectory != null && buildToolsDirectory.exists()) {
                    VirtualFile newestBuildToolsDirectory = Arrays.asList(buildToolsDirectory.getChildren())
                            .stream()
                            .filter(directory -> Version.isVersion(directory.getName()))
                            .max((left, right) -> new Version(left.getName()).compareTo(new Version(right.getName())))
                            .get();
                    if (newestBuildToolsDirectory != null) {
                        VirtualFile aaptFile = newestBuildToolsDirectory.findChild("aapt.exe");
                        if (aaptFile != null && aaptFile.exists()) {
                            return aaptFile;
//                            Runtime.getRuntime().exec(Arrays.asList(new String[]{aaptFile.getCanonicalPath(), "dump", "badging", file.getCanonicalPath()})
//                                    .stream()
//                                    .collect(Collectors.joining(" ")));

                        }
                    }
                }
            }
        }
        return null;
    }

    private void loadApplicationData(File file) throws IOException {
        ApkParser apkParser = new ApkParser(file);
        ApkMeta apkMeta = apkParser.getApkMeta();
        applicationName = apkMeta.getLabel();
        applicationVersion = apkMeta.getVersionName();
        applicationVersionCode = apkMeta.getVersionCode();

        applicationNameTextField.setText(applicationName);
        applicationVersionTextField.setText(applicationVersion);
        applicationVersionCodeTextField.setText(String.valueOf(applicationVersionCode));
    }

    private void clearApplicationMetaInformation() {
        applicationNameTextField.setText("none");
        applicationVersionTextField.setText("none");
        applicationVersionCodeTextField.setText(String.valueOf("none"));
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
                    loadApplicationData(selectedFile);
                } catch (IOException e1) {
                    e1.printStackTrace();
                    selectedFile = null;
                    sendFileButton.setEnabled(false);
                    clearApplicationMetaInformation();
                }
            }
        });

        sendFileButton.addActionListener(e -> {
            RequestBody file = new FileRequestBody(selectedFile, (current, max) -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    sendFileProgressBar.setValue((int) (current * 100 / max));
                });
            });

            RequestBody applicationNameBody = RequestBody.create(MediaType.parse("text/plain"), applicationName);
            RequestBody applicationVersionBody = RequestBody.create(MediaType.parse("text/plain"), applicationVersion);
            marimsService.postFile(applicationNameBody, applicationVersionBody, file).enqueue(new Callback<Void>() {
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
