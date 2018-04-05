package me.filedownloadingcheck.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.util.Util;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

import de.mrapp.android.dialog.ProgressDialog;
import me.filedownloadingcheck.Profile;
import me.filedownloadingcheck.R;
import me.filedownloadingcheck.Utils;
import me.filedownloadingcheck.helper.ImageHelper;

import static me.filedownloadingcheck.Utils.mypreference;

public class RecognitionSignupActivity extends AppCompatActivity {

    private EditText email, password;
    private Button confirmProfileBut;
    private ProgressBar progressBar;

    private final int PIC_REQUEST = 200;
    private String recognitionImageUrl;


    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private FirebaseDatabase firebaseDatabase;

    private ProgressDialog.Builder dialogBuilder;
    private ProgressDialog dialog;

    private SharedPreferences sharedpreferences;

    private Uri mUriPhotoTaken;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition_signup);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        confirmProfileBut = findViewById(R.id.confirmProfileBut);
        progressBar = findViewById(R.id.progressBar);

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        firebaseStorage = FirebaseStorage.getInstance();
        sharedpreferences = getSharedPreferences(mypreference,
                Context.MODE_PRIVATE);

        dialogBuilder = new ProgressDialog.Builder(this);
        dialogBuilder.setMessage("Your Recognition Image is Processing");
        dialogBuilder.setProgressBarPosition(ProgressDialog.ProgressBarPosition.LEFT);
        dialog = dialogBuilder.create();


        confirmProfileBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String emailText = email.getText().toString().trim();
                String passwordText = password.getText().toString().trim();

                if (TextUtils.isEmpty(emailText)) {
                    Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(passwordText)) {
                    Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);

                if (emailText.equals(Utils.profile.getEmail()) && passwordText.equals(Utils.userPassword)) {

                    Intent intent = new Intent(RecognitionSignupActivity.this, SelectImageActivity.class);
                    startActivityForResult(intent,  PIC_REQUEST);

                }
                else {
                    Toast.makeText(RecognitionSignupActivity.this, "Enter valid credentials", Toast.LENGTH_SHORT).show();
                }

                progressBar.setVisibility(View.GONE);



            }
        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PIC_REQUEST) {

            if(resultCode == RESULT_OK) {

                Uri imageUri;
                if (data == null || data.getData() == null) {
                    imageUri = mUriPhotoTaken;
                } else {
                    imageUri = data.getData();
                }

                Bitmap bitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                        imageUri, getContentResolver());
                if (bitmap != null) {
                    dialog.show();


                    storageReference = firebaseStorage.getReference();

                    if (!Utils.profile.getProfilePhotoUrl().equals("empty")) {

                        storageReference = firebaseStorage.getReferenceFromUrl(Utils.profile.getProfilePhotoUrl());
                        storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                            }
                        });

                    }
                    uploadRecognitionImage(bitmap);



                }
                else {
                    Toast.makeText(RecognitionSignupActivity.this, "Try Another Image", Toast.LENGTH_SHORT).show();
                    dialog.hide();
                }

            }

        }

    }

    private void uploadRecognitionImage(Bitmap bitmap) {


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] byteData = baos.toByteArray();

        UploadTask uploadTask = storageReference.child("recognition_image.jpg").putBytes(byteData);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                recognitionImageUrl = taskSnapshot.getDownloadUrl().toString();
                saveRecognitionPassword();

            }
        });
    }

    private void saveRecognitionPassword() {

        final Profile profile = new Profile(Utils.profile.getName(),
                Utils.profile.getEmail(),
                Utils.profile.getPhoneNumber(),
                Utils.profile.getProfilePhotoUrl(),
                recognitionImageUrl);

        databaseReference = firebaseDatabase.getReference("user/");


        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(Utils.userId.equals(dataSnapshot.getKey())){
                    databaseReference.child(Utils.userId).setValue(profile).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            dialog.hide();
                            Utils.profile = profile;

                            saveCredentialsLocally(Utils.userId, Utils.userPassword, profile);

                            finish();
                        }
                    });
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





}
