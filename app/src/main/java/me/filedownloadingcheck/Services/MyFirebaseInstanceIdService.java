package me.filedownloadingcheck.Services;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by Abdullah on 11/19/2017.
 */

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {


    @Override
    public void onTokenRefresh() {

        String refreshToken = FirebaseInstanceId.getInstance().getToken();


    }
}
