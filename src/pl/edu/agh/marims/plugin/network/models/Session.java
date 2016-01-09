package pl.edu.agh.marims.plugin.network.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Session {

    private String id;
    private String file;
    private String username;
    private long creationTimestamp;

    public Session() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    @Override
    public String toString() {
        Date creationDate = new Date(creationTimestamp);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String creationDateString = simpleDateFormat.format(creationDate);
        return creationDateString + "   " + id;
    }
}
