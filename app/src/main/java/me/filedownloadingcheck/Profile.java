package me.filedownloadingcheck;

/**
 * Created by Abdullah on 11/12/2017.
 */

public class Profile {

    private String name;
    private String email;
    private String phoneNumber;
    private String profilePhotoUrl;
    private String recognitionPassword;

    public Profile(){}

    public Profile(String name, String email, String phoneNumber, String profilePhotoUrl, String recognitionPassword) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.profilePhotoUrl = profilePhotoUrl;
        this.recognitionPassword = recognitionPassword;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getRecognitionPassword() {
        return recognitionPassword;
    }

    public void setRecognitionPassword(String recognitionPassword) {
        this.recognitionPassword = recognitionPassword;
    }
}
