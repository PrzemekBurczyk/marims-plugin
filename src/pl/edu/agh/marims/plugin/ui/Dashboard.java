package pl.edu.agh.marims.plugin.ui;

import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import net.dongliu.apk.parser.ApkParser;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import pl.edu.agh.marims.plugin.Config;
import pl.edu.agh.marims.plugin.network.FileRequestBody;
import pl.edu.agh.marims.plugin.network.MarimsApiClient;
import pl.edu.agh.marims.plugin.network.MarimsService;
import pl.edu.agh.marims.plugin.network.models.Session;
import pl.edu.agh.marims.plugin.util.GsonUtil;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.List;

public class Dashboard implements ToolWindowFactory {
    private DefaultListModel<String> filesListModel;
    private DefaultListModel<Session> sessionsListModel;

    private List<Session> sessions;

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
    private JList<Session> sessionsList;

    private Project project;
    private ToolWindow toolWindow;

    private File selectedFile;
    private String applicationName;
    private String applicationVersion;
    private Long applicationVersionCode;

    private MarimsService marimsService = MarimsApiClient.getInstance().getMarimsService();
    private Socket socket;

    private final JPopupMenu filesPopupMenu = new JPopupMenu();

    private final Type stringListType = new TypeToken<List<String>>() {
    }.getType();

    private final Type sessionListType = new TypeToken<List<Session>>() {
    }.getType();

    public Dashboard() {
        initInterface();
        initListeners();
        initConnection();
    }

    private void initInterface() {
        filesListModel = new DefaultListModel<>();
        filesList.setModel(filesListModel);

        sessionsListModel = new DefaultListModel<>();
        sessionsList.setModel(sessionsListModel);

        JMenuItem createSessionItem = new JMenuItem("Create session");
        JMenuItem removeFileItem = new JMenuItem("Remove file");

        createSessionItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                socket.emit("createSession", filesList.getSelectedValue());
            }
        });

        removeFileItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                marimsService.deleteFile(filesList.getSelectedValue()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Response<Void> response, Retrofit retrofit) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (response.code() >= 200 && response.code() < 300) {
                                Messages.showInfoMessage(project, "File removal successful", "File Removal");
                            } else {
                                Messages.showErrorDialog(project, "File removal failed", "File Removal");
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showErrorDialog(project, "File removal failed", "File Removal");
                        });
                    }
                });
            }
        });

        filesPopupMenu.add(createSessionItem);
        filesPopupMenu.add(removeFileItem);
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

            RequestBody applicationNameParam = RequestBody.create(MediaType.parse("text/plain"), applicationName);
            RequestBody applicationVersionParam = RequestBody.create(MediaType.parse("text/plain"), applicationVersion);
            RequestBody applicationVersionCodeParam = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(applicationVersionCode));
            marimsService.postFile(applicationNameParam, applicationVersionParam, applicationVersionCodeParam, file).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Response<Void> response, Retrofit retrofit) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showInfoMessage(project, "File upload successful", "File Upload");
                    });
                }

                @Override
                public void onFailure(Throwable throwable) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "File upload failed", "File Upload");
                    });
                }
            });
        });

        filesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mouseClicked(e);
                JList list = (JList) e.getSource();
                int clickedIndex = list.locationToIndex(e.getPoint());
                if (SwingUtilities.isRightMouseButton(e) && !list.isSelectionEmpty()) {
                    list.setSelectedIndex(clickedIndex);
                    filesPopupMenu.show(list, e.getX(), e.getY());
                }
            }
        });

        filesList.addListSelectionListener(e -> refreshSessionsList());
    }

    private void initConnection() {
        try {
            socket = IO.socket(Config.SERVER_URL + Config.SOCKET_IO_ENDPOINT);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("Socket connected");
                }
            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("Socket disconnected");
                }
            }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("Socket connect error");
                }
            }).on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("Socket connect timeout");
                }
            }).on("files", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONArray filesJson = (JSONArray) args[0];
                    List<String> files = GsonUtil.getGson().fromJson(filesJson.toString(), stringListType);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        filesListModel.clear();
                        files.forEach((file) -> filesListModel.addElement(file));
                    });
                }
            }).on("sessions", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONArray sessionsJson = (JSONArray) args[0];
                    sessions = GsonUtil.getGson().fromJson(sessionsJson.toString(), sessionListType);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        refreshSessionsList();
                    });
                }
            }).on("sessionCreationFailed", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "Session creation failed", "Session Creation");
                    });
                }
            }).on("sessionRemovalFailed", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "Session removal failed", "Session Removal");
                    });
                }
            });
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void fetchFilesData() {
        marimsService.getFiles().enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Response<List<String>> response, Retrofit retrofit) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    filesListModel.clear();
                    response.body().forEach((file) -> filesListModel.addElement(file));
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

    private void refreshSessionsList() {
        sessionsListModel.clear();
        String selectedFile = filesList.getSelectedValue();
        sessions.stream()
                .filter((session) -> session.getFile().equals(selectedFile))
                .forEach((session) -> sessionsListModel.addElement(session));
    }

}
