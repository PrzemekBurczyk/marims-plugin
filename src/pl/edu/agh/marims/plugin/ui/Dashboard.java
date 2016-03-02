package pl.edu.agh.marims.plugin.ui;

import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;
import net.dongliu.apk.parser.ApkParser;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import pl.edu.agh.marims.plugin.Config;
import pl.edu.agh.marims.plugin.network.FileRequestBody;
import pl.edu.agh.marims.plugin.network.MarimsApiClient;
import pl.edu.agh.marims.plugin.network.MarimsService;
import pl.edu.agh.marims.plugin.network.models.*;
import pl.edu.agh.marims.plugin.util.GsonUtil;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Dashboard implements ToolWindowFactory {
    private static final ApplicationFile DEFAULT_FILE = new ApplicationFile("[]Others");
    private DefaultListModel<ApplicationFile> filesListModel;
    private DefaultListModel<Session> sessionsListModel;
    private DefaultListModel<User> allUsersListModel;
    private DefaultListModel<User> fileUsersListModel;

    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    private List<Session> sessions;
    private List<User> users;
    private List<ApplicationFile> files;

    private JPanel cardPanel;

    private JPanel contentPanel;
    private JLabel title;
    private JButton chooseFileButton;
    private JLabel chooseFileLabel;
    private JButton sendFileButton;
    private JProgressBar sendFileProgressBar;
    private JLabel applicationNameTextField;
    private JLabel applicationVersionTextField;
    private JLabel applicationVersionCodeTextField;
    private JList<ApplicationFile> filesList;
    private JList<Session> sessionsList;
    private JList<User> allUsersList;
    private JList<User> fileUsersList;

    private JPanel dashboardPanel;
    private JButton backButton;
    private JButton logOutButton;
    private JPanel loginPanel;
    private JTextField logInEmail;
    private JPasswordField logInPassword;
    private JTextField registerEmail;
    private JPasswordField registerPassword;
    private JButton logInButton;
    private JButton registerButton;
    private BrowserPanel browserPanel;

    private Project project;
    private ToolWindow toolWindow;

    private File selectedFile;

    private String applicationName;
    private String applicationVersion;
    private Long applicationVersionCode;
    private String applicationPackage;

    private MarimsApiClient marimsApiClient = MarimsApiClient.getInstance();
    private MarimsService marimsService = marimsApiClient.getMarimsService();
    private Socket socket;

    private final JPopupMenu filesPopupMenu = new JPopupMenu();
    private final JPopupMenu sessionsPopupMenu = new JPopupMenu();
    private final JPopupMenu allUsersPopupMenu = new JPopupMenu();
    private final JPopupMenu fileUsersPopupMenu = new JPopupMenu();

    private final Type stringListType = new TypeToken<List<String>>() {
    }.getType();

    private final Type sessionListType = new TypeToken<List<Session>>() {
    }.getType();

    private final Type userListType = new TypeToken<List<User>>() {
    }.getType();

    public Dashboard() {
        initInterface();
        initListeners();
    }

    private void initInterface() {
        browserPanel = new BrowserPanel();

        filesListModel = new DefaultListModel<>();
        filesList.setModel(filesListModel);

        sessionsListModel = new DefaultListModel<>();
        sessionsList.setModel(sessionsListModel);

        allUsersListModel = new DefaultListModel<>();
        allUsersList.setModel(allUsersListModel);

        fileUsersListModel = new DefaultListModel<>();
        fileUsersList.setModel(fileUsersListModel);

        JMenuItem createSessionItem = new JMenuItem("Create session");
        JMenuItem removeFileItem = new JMenuItem("Remove file");
        JMenuItem addMemberItem = new JMenuItem("Add member");
        JMenuItem removeMemberItem = new JMenuItem("Remove member");

        createSessionItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                socket.emit("createSession", filesList.getSelectedValue().toApplicationFileString());
            }
        });

        removeFileItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                marimsService.deleteFile(filesList.getSelectedValue().toApplicationFileString()).enqueue(new Callback<Void>() {
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

        InputValidator emailValidator = new InputValidator() {
            @Override
            public boolean checkInput(String inputString) {
                return Pattern.matches(EMAIL_PATTERN, inputString);
            }

            @Override
            public boolean canClose(String inputString) {
                return Pattern.matches(EMAIL_PATTERN, inputString);
            }
        };

        addMemberItem.addActionListener(e -> {
            String email = Messages.showInputDialog(project, "Enter user email", "Add Member", null, null, null);
            String filename = filesList.getSelectedValue().toApplicationFileString();

            if (email != null && !email.equals("")) {
                socket.emit("addMember", email, filename);
            }
        });

        removeMemberItem.addActionListener(e -> {
            String email = Messages.showInputDialog(project, "Enter user email", "Remove Member", null, null, null);
            String filename = filesList.getSelectedValue().toApplicationFileString();

            if (email != null && !email.equals("")) {
                socket.emit("removeMember", email, filename);
            }
        });

        filesPopupMenu.add(createSessionItem);
        filesPopupMenu.add(removeFileItem);
        filesPopupMenu.add(addMemberItem);
        filesPopupMenu.add(removeMemberItem);

        JMenuItem connectToSessionItem = new JMenuItem("Connect to session");

        connectToSessionItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String sessionId = sessionsList.getSelectedValue().getId();
                browserPanel.loadSession(sessionId);
                contentPanel.remove(dashboardPanel);
                contentPanel.add(browserPanel, BorderLayout.CENTER);
                contentPanel.revalidate();
                backButton.setEnabled(true);
            }
        });

        sessionsPopupMenu.add(connectToSessionItem);

        JMenuItem addAsMemberItem = new JMenuItem("Add as member");
        JMenuItem removeFromMembersItem = new JMenuItem("Remove from members");

        addAsMemberItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String email = allUsersList.getSelectedValue().getEmail();
                String filename = filesList.getSelectedValue().toApplicationFileString();

                if (email != null && !email.equals("")) {
                    socket.emit("addMember", email, filename);
                }
            }
        });

        removeFromMembersItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String email = fileUsersList.getSelectedValue().getEmail();
                String filename = filesList.getSelectedValue().toApplicationFileString();

                if (email != null && !email.equals("")) {
                    socket.emit("removeMember", email, filename);
                }
            }
        });

        allUsersPopupMenu.add(addAsMemberItem);
        fileUsersPopupMenu.add(removeFromMembersItem);
    }

    private void loadApplicationData(File file) throws IOException {
        ApkParser apkParser = new ApkParser(file);
        ApkMeta apkMeta = apkParser.getApkMeta();
        applicationName = apkMeta.getLabel();
        applicationVersion = apkMeta.getVersionName();
        applicationVersionCode = apkMeta.getVersionCode();
        applicationPackage = apkMeta.getPackageName();

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
        logInButton.addActionListener(e -> {
            UserRequest user = new UserRequest();
            user.setEmail(logInEmail.getText());
            user.setPassword(new String(logInPassword.getPassword()));
            marimsService.logIn(user).enqueue(new Callback<LoggedUser>() {
                @Override
                public void onResponse(Response<LoggedUser> response, Retrofit retrofit) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (response.code() >= 200 && response.code() < 300) {
                            marimsApiClient.setLoggedUser(response.body());
                            ((CardLayout) cardPanel.getLayout()).show(cardPanel, "Dashboard");
                            initConnection();
                        } else {
                            try {
                                Messages.showErrorDialog(project, response.errorBody().string(), "Log in");
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void onFailure(Throwable throwable) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "Log in failed", "Log in");
                    });
                }
            });
        });

        logOutButton.addActionListener(e -> {
            marimsService.logOut().enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Response<Void> response, Retrofit retrofit) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (response.code() >= 200 && response.code() < 300) {
                            marimsApiClient.setLoggedUser(null);
                            ((CardLayout) cardPanel.getLayout()).show(cardPanel, "LogIn");
                            closeConnection();
                        } else {
                            try {
                                Messages.showErrorDialog(project, response.errorBody().string(), "Log out");
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void onFailure(Throwable throwable) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "Log out failed", "Log out");
                    });
                }
            });
        });

        registerButton.addActionListener(e -> {
            UserRequest user = new UserRequest();
            user.setEmail(registerEmail.getText());
            user.setPassword(new String(registerPassword.getPassword()));
            marimsService.register(user).enqueue(new Callback<LoggedUser>() {
                @Override
                public void onResponse(Response<LoggedUser> response, Retrofit retrofit) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (response.code() >= 200 && response.code() < 300) {
                            marimsApiClient.setLoggedUser(response.body());
                            ((CardLayout) cardPanel.getLayout()).show(cardPanel, "Dashboard");
                            initConnection();
                        } else {
                            try {
                                Messages.showErrorDialog(project, response.errorBody().string(), "Register");
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void onFailure(Throwable throwable) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "Registering failed", "Register");
                    });
                }
            });
        });

        chooseFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(project.getBasePath());
//            FileNameExtensionFilter filter = new FileNameExtensionFilter("Android APKs", "apk");
//            fileChooser.setFileFilter(filter);
            int selectedOption = fileChooser.showOpenDialog(contentPanel);
            if (selectedOption == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                sendFileButton.setEnabled(true);
                sendFileProgressBar.setValue(0);
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
            RequestBody applicationPackageParam = RequestBody.create(MediaType.parse("text/plain"), applicationPackage);
            marimsService.postFile(applicationPackageParam, applicationNameParam, applicationVersionParam, applicationVersionCodeParam, file).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Response<Void> response, Retrofit retrofit) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (response.code() >= 200 && response.code() < 300) {
                            Messages.showInfoMessage(project, "File upload successful", "File Upload");
                        } else {
                            Messages.showErrorDialog(project, "File upload failed", "File Upload");
                        }
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
                    if (!list.getSelectedValue().equals(DEFAULT_FILE)) { // you cannot remove "Others" item
                        filesPopupMenu.show(list, e.getX(), e.getY());
                    }
                }
            }
        });

        filesList.addListSelectionListener(e -> {
            refreshUsersList();
            refreshSessionsList();
        });

        sessionsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mouseClicked(e);
                JList list = (JList) e.getSource();
                int clickedIndex = list.locationToIndex(e.getPoint());
                if (SwingUtilities.isRightMouseButton(e) && !list.isSelectionEmpty()) {
                    list.setSelectedIndex(clickedIndex);
                    sessionsPopupMenu.show(list, e.getX(), e.getY());
                }
            }
        });

        allUsersList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mouseClicked(e);
                JList list = (JList) e.getSource();
                int clickedIndex = list.locationToIndex(e.getPoint());
                if (SwingUtilities.isRightMouseButton(e) && !list.isSelectionEmpty()) {
                    list.setSelectedIndex(clickedIndex);
                    allUsersPopupMenu.show(list, e.getX(), e.getY());
                }
            }
        });

        fileUsersList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mouseClicked(e);
                JList list = (JList) e.getSource();
                int clickedIndex = list.locationToIndex(e.getPoint());
                if (SwingUtilities.isRightMouseButton(e) && !list.isSelectionEmpty()) {
                    list.setSelectedIndex(clickedIndex);
                    if (!filesList.getSelectedValue().equals(DEFAULT_FILE)) {
                        fileUsersPopupMenu.show(list, e.getX(), e.getY());
                    }
                }
            }
        });

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browserPanel.closeSession();
                contentPanel.remove(browserPanel);
                contentPanel.add(dashboardPanel, BorderLayout.CENTER);
                contentPanel.revalidate();
                backButton.setEnabled(false);
            }
        });
    }

    private void initConnection() {
        try {
            socket = IO.socket(Config.SERVER_URL + Config.SOCKET_IO_ENDPOINT);
            socket.io().on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Transport transport = (Transport) args[0];

                    transport.on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            @SuppressWarnings("unchecked")
                            Map<String, List<String>> headers = (Map<String, List<String>>) args[0];
                            LoggedUser loggedUser = marimsApiClient.getLoggedUser();
                            if (loggedUser != null && loggedUser.getToken() != null) {
                                headers.put("Authorization", Collections.singletonList("Bearer " + loggedUser.getToken()));
                            }
                        }
                    });
                }
            });
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
                    List<String> filesStrings = GsonUtil.getGson().fromJson(filesJson.toString(), stringListType);
                    files = filesStrings.stream()
                            .map(ApplicationFile::new)
                            .collect(Collectors.toList());
                    ApplicationManager.getApplication().invokeLater(() -> {
                        refreshFilesList();
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
            }).on("users", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONArray usersJson = (JSONArray) args[0];
                    users = GsonUtil.getGson().fromJson(usersJson.toString(), userListType);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        refreshUsersList();
                        refreshFilesList();
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

    private void closeConnection() {
        socket.disconnect();
    }

    private void refreshFilesList() {
        filesListModel.clear();
        LoggedUser loggedUser = marimsApiClient.getLoggedUser();
        if (users != null) {
            User currentUser = users.stream()
                    .filter((user) -> user.getId().equals(loggedUser.getId()))
                    .findFirst()
                    .get();
            if (loggedUser != null) {
                files.stream()
                        .filter((file) -> currentUser.getAuthorOfFiles().contains(file.toApplicationFileString()))
                        .forEach((file) -> filesListModel.addElement(file));
                filesListModel.addElement(DEFAULT_FILE);
            }
        }
    }

    private void refreshSessionsList() {
        sessionsListModel.clear();
        ApplicationFile selectedFile = filesList.getSelectedValue();
        if (selectedFile != null) {
            sessions.stream()
                    .filter((session) -> selectedFile.equals(DEFAULT_FILE) ? session.getFile() == null : selectedFile.toApplicationFileString().equals(session.getFile()))
                    .forEach((session) -> sessionsListModel.addElement(session));
        }
    }

    private void refreshUsersList() {
        refreshAllUsersList();
        refreshFileUsersList();
    }

    private void refreshAllUsersList() {
        allUsersListModel.clear();
        ApplicationFile selectedFile = filesList.getSelectedValue();
        LoggedUser loggedUser = marimsApiClient.getLoggedUser();
        if (loggedUser != null && selectedFile != null) {
            if (!selectedFile.equals(DEFAULT_FILE)) {
                users.stream()
                        .filter((user) -> !user.getId().equals(loggedUser.getId()) && !user.getMemberOfFiles().contains(selectedFile.toApplicationFileString()))
                        .forEach((user) -> allUsersListModel.addElement(user));
            }
        }
    }

    private void refreshFileUsersList() {
        fileUsersListModel.clear();
        ApplicationFile selectedFile = filesList.getSelectedValue();
        LoggedUser loggedUser = marimsApiClient.getLoggedUser();
        if (loggedUser != null && selectedFile != null) {
            if (!selectedFile.equals(DEFAULT_FILE)) {
                users.stream()
                        .filter((user) -> !user.getId().equals(loggedUser.getId()) && user.getMemberOfFiles().contains(selectedFile.toApplicationFileString()))
                        .forEach((user) -> fileUsersListModel.addElement(user));
            } else {
                users.stream()
                        .filter((user) -> !user.getId().equals(loggedUser.getId()))
                        .forEach((user) -> fileUsersListModel.addElement(user));
            }
        }
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(cardPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
