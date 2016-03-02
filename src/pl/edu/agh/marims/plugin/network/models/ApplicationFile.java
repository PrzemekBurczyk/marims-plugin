package pl.edu.agh.marims.plugin.network.models;

public class ApplicationFile {

    private String packageName;
    private String fileName;
    private String userId;

    public ApplicationFile() {
    }

    public ApplicationFile(String applicationFileString) {
        String[] applicationFileData = applicationFileString.split("(\\[)|(\\])");
        userId = applicationFileData[1];
        packageName = applicationFileData[3];
        fileName = applicationFileData[4];
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String toApplicationFileString() {
        return "[" + userId + "]" + "[" + packageName + "]" + fileName;
    }

    @Override
    public String toString() {
        return fileName;
    }
}

