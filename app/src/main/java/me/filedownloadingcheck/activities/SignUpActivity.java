package me.filedownloadingcheck.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.Image;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileNotFoundException;
import java.io.InputStream;

import me.filedownloadingcheck.Profile;
import me.filedownloadingcheck.R;
import me.filedownloadingcheck.Utils;

import static me.filedownloadingcheck.Utils.mypreference;

public class SignUpActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword, inputUserName, inputPhone;
    private ImageView profileImage;
    private Button btnSignIn, btnSignUp, btnResetPassword;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    SharedPreferences sharedpreferences;


    private String profileImageUrl = "empty";
    private Uri imageUri;


    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private FirebaseDatabase database;
    private DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sigup_activity_layout);

      //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        firebaseStorage = FirebaseStorage.getInstance();

        sharedpreferences = getSharedPreferences(mypreference,
                Context.MODE_PRIVATE);

        profileImage = (ImageView) findViewById(R.id.profileImage);
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, Utils.RC_PHOTO_PICKER);
            }
        });
        btnSignIn = (Button) findViewById(R.id.sign_in_button);
        btnSignUp = (Button) findViewById(R.id.sign_up_button);
        inputEmail = (EditText) findViewById(R.id.email);
        inputUserName = (EditText) findViewById(R.id.userName);
        inputPhone = (EditText) findViewById(R.id.phone);
        inputPassword = (EditText) findViewById(R.id.password);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btnResetPassword = (Button) findViewById(R.id.btn_reset_password);

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignUpActivity.this, ResetPasswordActivity.class));
            }
        });

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String email = inputEmail.getText().toString().trim();
                final String password = inputPassword.getText().toString().trim();
                String userName = inputUserName.getText().toString().trim();
                String phone = inputPhone.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(userName)) {
                    Toast.makeText(getApplicationContext(), "Enter UserName!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(phone)) {
                    Toast.makeText(getApplicationContext(), "Enter Phone!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(getApplicationContext(), "Password too short, enter minimum 6 characters!", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                //create user
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                //Toast.makeText(SignUpActivity.this, "createUserWithEmail:onComplete:" + task.isSuccessful(), Toast.LENGTH_SHORT).show();
                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                if (!task.isSuccessful()) {
                                    Toast.makeText(SignUpActivity.this, "Authentication failed." + task.getException(),
                                            Toast.LENGTH_SHORT).show();
                                } else {

                                    if (imageUri != null) {
                                        storageReference = firebaseStorage.getReference();

                                        StorageReference myRef = storageReference.child(imageUri.getLastPathSegment());

                                        myRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                profileImageUrl = taskSnapshot.getDownloadUrl().toString();
                                                saveProfile(email, password, profileImageUrl);
                                            }
                                        });


                                    }
                                    else {
                                        Toast.makeText(SignUpActivity.this, "Profile photo not Set", Toast.LENGTH_SHORT).show();
                                        saveProfile(email, password, "empty");
                                    }



                                    String token =FirebaseInstanceId.getInstance().getToken();

                                    myRef = database.getReference("inbox/"+ Utils.userId+"/token/" + token);


                                    myRef.setValue(true, new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                            //Toast.makeText(SignUpActivity.this, "Token Make", Toast.LENGTH_SHORT).show();
                                            Log.e("TOKEN", "Token Make");
                                        }
                                    });

                                }
                            }
                        });

            }
        });
    }

    private void saveProfile(String email, final String password, String Url) {

        progressBar.setVisibility(View.GONE);

        Utils.userId = auth.getCurrentUser().getUid();
        Utils.userPassword = password;

        myRef = database.getReference("user/" + Utils.userId);
        Profile profile = new Profile(inputUserName.getText().toString(), email, inputPhone.getText().toString(), Url, "empty");

        Utils.profile = profile;

        myRef.setValue(profile, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                saveCredentialsLocally(Utils.userId, password, Utils.profile);
                startActivity(new Intent(SignUpActivity.this, RecognitionActivity.class));
                finish();

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

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case Utils.RC_PHOTO_PICKER:
                if(resultCode == RESULT_OK){
                    try {
                        imageUri = imageReturnedIntent.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        Bitmap profileImageBmp = BitmapFactory.decodeStream(imageStream);
                        profileImage.setImageBitmap(profileImageBmp);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }
        }
    }

}
