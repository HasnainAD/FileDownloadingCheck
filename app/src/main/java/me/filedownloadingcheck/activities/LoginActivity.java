package me.filedownloadingcheck.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;

import me.filedownloadingcheck.Profile;
import me.filedownloadingcheck.R;
import me.filedownloadingcheck.Utils;

import static me.filedownloadingcheck.Utils.mypreference;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private FirebaseAuth auth;
    private ProgressBar progressBar;
    private Button btnSignup, btnLogin, btnReset;
    private AVLoadingIndicatorView avl;
    private TextView smsLogo;

    FirebaseDatabase database;
    DatabaseReference myRef;

    private SharedPreferences sharedpreferences;

    private boolean faceRecognitionFlag;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set the view now
        setContentView(R.layout.activity_login);

        inputEmail = (EditText) findViewById(R.id.email);
        inputPassword = (EditText) findViewById(R.id.password);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btnSignup = (Button) findViewById(R.id.btn_signup);
        btnLogin = (Button) findViewById(R.id.btn_login);
        btnReset = (Button) findViewById(R.id.btn_reset_password);

        smsLogo =  findViewById(R.id.smsLogo);
        smsLogo.setTypeface(Typeface.createFromAsset(getAssets(),"Lobster-Regular.ttf"));

        CreateFolders();
        database = FirebaseDatabase.getInstance();

        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);

        faceRecognitionFlag = sharedpreferences.getBoolean("face_recognition_pref_key", true);


        sharedpreferences = getSharedPreferences(mypreference,
                Context.MODE_PRIVATE);

        if (!sharedpreferences.getString("profileEmail", "").isEmpty()) {

            Utils.userId = sharedpreferences.getString("userId", "");
            Utils.userPassword = sharedpreferences.getString("userPassword", "");

            Profile profile = new Profile( sharedpreferences.getString("profileName", ""),
                    sharedpreferences.getString("profileEmail", ""),
                    sharedpreferences.getString("profileNumnber", ""),
                    sharedpreferences.getString("profileImageUrl", ""),
                    sharedpreferences.getString("profileFacePassword", ""));

            Utils.profile = profile;

            if (faceRecognitionFlag)
                startActivity(new Intent(LoginActivity.this, RecognitionActivity.class));
            else
                startActivity(new Intent(LoginActivity.this, UploadActivity.class));


            finish();
        }



        int internetPermissionCheck = ActivityCompat.checkSelfPermission(this, INTERNET);
        int writePermissionCheck = ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE);
        int cameraPermissionCheck = ActivityCompat.checkSelfPermission(this, CAMERA);

        if (internetPermissionCheck == PackageManager.PERMISSION_GRANTED &&
                writePermissionCheck == PackageManager.PERMISSION_GRANTED &&
                cameraPermissionCheck == PackageManager.PERMISSION_GRANTED) {


            makeListeners();

        }

        else {
            ActivityCompat.requestPermissions(LoginActivity.this, Utils.permissions, Utils.REQUEST_PERMISSION_CODE);
        }


    }

    private void makeListeners() {

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
                finish();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = inputEmail.getText().toString();
                final String password = inputPassword.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);

                //authenticate user
                auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                progressBar.setVisibility(View.GONE);
                                if (!task.isSuccessful()) {
                                    // there was an error
                                    if (password.length() < 6) {
                                        inputPassword.setError("Password too short, enter minimum 6 characters!");
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Authentication failed, check your email and password or sign up", Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    CreateFolders();
                                    Utils.userId = auth.getCurrentUser().getUid();
                                    Utils.userPassword = password;
                                    myRef = database.getReference("user/");


                                    myRef.addChildEventListener(new ChildEventListener() {
                                        @Override
                                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                           if(dataSnapshot.getKey().equals(Utils.userId)){
                                               Utils.profile = dataSnapshot.getValue(Profile.class);
                                               myRef.removeEventListener(this);
                                           }
                                        }

                                        @Override
                                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                                        }

                                        @Override
                                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                                        }

                                        @Override
                                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });

                                    myRef.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {

                                            saveCredentialsLocally(Utils.userId, password, Utils.profile);

                                            if (faceRecognitionFlag)
                                                startActivity(new Intent(LoginActivity.this, RecognitionActivity.class));
                                            else
                                                startActivity(new Intent(LoginActivity.this, UploadActivity.class));


                                            finish();
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });

                                }
                            }
                        });
            }
        });
    }


    public void saveCredentialsLocally(String userId, String password, Profile profile) {
        SharedPreferences.Editor editor = sharedpreferences.edit();

        editor.putString("userId", userId);
        editor.putString("userPassword", password);
        editor.putString("profileName", profile.getName());
        editor.putString("profileEmail", profile.getEmail());
        editor.putString("profileNumnber", profile.getPhoneNumber());
        editor.putString("profileImageUrl", profile.getProfilePhotoUrl());
        editor.putString("profileFacePassword", profile.getRecognitionPassword());
        editor.commit();
    }


    private void CreateFolders() {
        //Making SMS Directorie
        File file = new File(Environment.getExternalStorageDirectory(), "SMS");
        if (!file.exists()) {
            file.mkdirs();
            Log.e("FOLDER", "SMS Created");
        }
        //Making Upload Directories
        file = new File(Environment.getExternalStorageDirectory() + "/SMS", "Upload");
        if (!file.exists()) {
            file.mkdir();
            Log.e("FOLDER", "Upload Created");
            file = new File(Environment.getExternalStorageDirectory() + "/SMS/Upload", "Videos");
            if (!file.exists()) {
                file.mkdir();
                Log.e("FOLDER", "Upload Videos Created");

            }
            file = new File(Environment.getExternalStorageDirectory() + "/SMS/Upload", "Images");
            if (!file.exists()) {
                file.mkdir();

                Log.e("FOLDER", "Upload Images Created");

            }
            file = new File(Environment.getExternalStorageDirectory() + "/SMS/Upload", "Pdf");
            if (!file.exists()) {
                file.mkdir();

                Log.e("FOLDER", "Upload PDF Created");


            }


            file = new File(Environment.getExternalStorageDirectory() + "/SMS/Upload", "Docs");
            if (!file.exists()) {
                file.mkdir();

                Log.e("FOLDER", "Upload Docs Created");

            }

        }

        //Making Inbox Directories

        file = new File(Environment.getExternalStorageDirectory() + "/SMS", "Inbox");
        if (!file.exists()) {
            file.mkdir();
            Log.e("FOLDER", "Inbox Created");


            file = new File(Environment.getExternalStorageDirectory() + "/SMS/Inbox", "Videos");
            if (!file.exists()) {
                file.mkdir();

                Log.e("FOLDER", "Inbox Videos Created");

            }


            file = new File(Environment.getExternalStorageDirectory() + "/SMS/Inbox", "Images");
            if (!file.exists()) {
                file.mkdir();

                Log.e("FOLDER", "Inbox Images Created");

            }


            file = new File(Environment.getExternalStorageDirectory() + "/SMS/Inbox", "Pdf");
            if (!file.exists()) {
                file.mkdir();

                Log.e("FOLDER", "Inbox PDF Created");

            }


            file = new File(Environment.getExternalStorageDirectory() + "/SMS/Inbox", "Docs");
            if (!file.exists()) {
                file.mkdir();

                Log.e("FOLDER", "Inbox Docs Created");

            }

        }


        //Making Share Directories
        file = new File(Environment.getExternalStorageDirectory() + "/SMS", "Share");
        if (!file.exists()) {
            file.mkdir();
            Log.e("FOLDER", "Share Created");



            file = new File(Environment.getExternalStorageDirectory() + "/SMS/Share", "Videos");
            if (!file.exists()) {
                file.mkdir();

                Log.e("FOLDER", "Share Videos Created");

            }


            file = new File(Environment.getExternalStorageDirectory() + "/SMS/Share", "Images");
            if (!file.exists()) {
                file.mkdir();

                Log.e("FOLDER", "Share Images Created");

            }


            file = new File(Environment.getExternalStorageDirectory() + "/SMS/Share", "Pdf");
            if (!file.exists()) {
                file.mkdir();

                Log.e("FOLDER", "Share Pdf Created");

            }


            file = new File(Environment.getExternalStorageDirectory() + "/SMS/Share", "Docs");
            if (!file.exists()) {
                file.mkdir();

                Log.e("FOLDER", "Share Docs Created");

            }

        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Utils.REQUEST_PERMISSION_CODE: {
                // If request is cancelled, the result arrays are empty.

                if (grantResults.length > 0) {

                    boolean cameraResult = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeResult = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean internetResult = grantResults[2] == PackageManager.PERMISSION_GRANTED;

                    Log.e("camera", Boolean.toString(cameraResult));
                    Log.e("write", Boolean.toString(writeResult));
                    Log.e("internet", Boolean.toString(internetResult));

                    if ( cameraResult && writeResult && internetResult) {
                        makeListeners();
                    }
                    else {

                        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("SMS")
                                .setMessage("Not granted all the permissions")
                                .setPositiveButton("Ok", listener)
                                .show();
                    }


                }

            }
        }
    }

}
