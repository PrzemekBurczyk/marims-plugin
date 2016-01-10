package pl.edu.agh.marims.plugin.network.models;

public class ApplicationFile {

    private String packageName;
    private String fileName;

    public ApplicationFile() {
    }

    public ApplicationFile(String applicationFileString) {
        String[] applicationFileData = applicationFileString.split("(\\[)|(\\])");
        packageName = applicationFileData[1];
        fileName = applicationFileData[2];
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

    public String toApplicationFileString() {
        return "[" + packageName + "]" + fileName;
    }

    @Override
    public String toString() {
        return fileName;
    }
}

