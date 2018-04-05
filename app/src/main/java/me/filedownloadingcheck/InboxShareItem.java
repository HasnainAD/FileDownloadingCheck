package me.filedownloadingcheck;

/**
 * Created by Abdullah on 11/17/2017.
 */

public class InboxShareItem {

    private String fileName;
    private String url;
    private String userID;

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public InboxShareItem() {}

    public InboxShareItem(String fileName, String url, String userID) {
        this.fileName = fileName;
        this.url = url;
        this.userID = userID;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
