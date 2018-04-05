package me.filedownloadingcheck.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileNotFoundException;
import java.io.InputStream;

import de.mrapp.android.dialog.ProgressDialog;
import me.filedownloadingcheck.Profile;
import me.filedownloadingcheck.R;
import me.filedownloadingcheck.Utils;

public class Profile_Activity extends AppCompatActivity {

    ImageView profileImage;
    EditText profileNameText;
    EditText profileNumberText;
    TextView profileEmailText;
    Button updateBut;
    private String profileImageUrl = "empty";
    private Uri imageUri;
    private FirebaseStorage firebaseStorage;
    private FirebaseDatabase firebaseDatabase;
    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private ProgressDialog.Builder dialogBuilder;
    private ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_);

        profileImage = (ImageView) findViewById(R.id.profileImage);
        profileNameText = (EditText) findViewById(R.id.profileNameText);
        profileNumberText = (EditText) findViewById(R.id.profileNumberText);
        profileEmailText = (TextView) findViewById(R.id.profileEmailText);
        updateBut = findViewById(R.id.updateBut);

        profileNameText.setText(Utils.profile.getName());
        profileNumberText.setText(Utils.profile.getPhoneNumber());
        profileEmailText.setText(Utils.profile.getEmail());
        dialogBuilder = new ProgressDialog.Builder(this);
        dialogBuilder.setMessage("Your profile is uploading");
        dialogBuilder.setProgressBarPosition(ProgressDialog.ProgressBarPosition.LEFT);
        dialog = dialogBuilder.create();

        if (!Utils.profile.getProfilePhotoUrl().equals("empty")) {
            Glide.with(Profile_Activity.this).load(Utils.profile.getProfilePhotoUrl()).apply(RequestOptions.circleCropTransform()).into(profileImage);
        }

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, Utils.RC_PHOTO_PICKER);
            }
        });


        updateBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();

                if (!Utils.profile.getProfilePhotoUrl().equals("empty")) {

                    storageReference = firebaseStorage.getReferenceFromUrl(Utils.profile.getProfilePhotoUrl());
                    storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    });

                }
                uploadProfileImage();
            }
        });



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

    private void uploadProfileImage() {

        storageReference = firebaseStorage.getReference();

        if (imageUri != null) {
            StorageReference myRef = storageReference.child(imageUri.getLastPathSegment());

            myRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    profileImageUrl = taskSnapshot.getDownloadUrl().toString();
                    saveProfileData();
                }
            });


        }
        else{
            saveProfileData();
        }




    }

    private void saveProfileData() {
        final Profile profile = new Profile(profileNameText.getText().toString(),
                profileEmailText.getText().toString(),
                profileNumberText.getText().toString(),
                profileImageUrl,
                "empty");

        databaseReference = firebaseDatabase.getReference("user/");


        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(Utils.userId.equals(dataSnapshot.getKey())){
                    databaseReference.child(Utils.userId).setValue(profile).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            dialog.hide();
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



        Utils.profile = profile;


    }

}
