package pl.edu.agh.marims.plugin.network.models;

import java.util.List;

public class User {
    private String id;
    private String email;
    private List<String> authorOfFiles;
    private List<String> memberOfFiles;

    public User() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getAuthorOfFiles() {
        return authorOfFiles;
    }

    public void setAuthorOfFiles(List<String> authorOfFiles) {
        this.authorOfFiles = authorOfFiles;
    }

    public List<String> getMemberOfFiles() {
        return memberOfFiles;
    }

    public void setMemberOfFiles(List<String> memberOfFiles) {
        this.memberOfFiles = memberOfFiles;
    }

    @Override
    public String toString() {
        return email;
    }
}
