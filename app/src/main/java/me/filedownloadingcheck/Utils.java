package me.filedownloadingcheck;

import java.util.ArrayList;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * Created by Abdullah on 11/9/2017.
 */

public class Utils {

    public static String currentType;
    public static String mainType;

    public static final String encryptionPassword = "MyPassword";
    public static final String mypreference = "mypref";

    public static final String AES = "AES";

    public static volatile boolean fileUploadingFlag = false;

    public static String userId;
    public static String userPassword;
    public static Profile profile;


    public final static int RC_PHOTO_PICKER =382;
    public static String[] permissions = {CAMERA, WRITE_EXTERNAL_STORAGE, INTERNET};
    public static final int REQUEST_PERMISSION_CODE = 84;


}
