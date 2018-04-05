package me.filedownloadingcheck.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.VerifyResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.UUID;

import de.mrapp.android.dialog.ProgressDialog;
import me.filedownloadingcheck.Profile;
import me.filedownloadingcheck.R;
import me.filedownloadingcheck.Utils;
import me.filedownloadingcheck.helper.ImageHelper;
import me.filedownloadingcheck.helper.SampleApp;

import static me.filedownloadingcheck.Utils.mypreference;

public class RecognitionActivity extends AppCompatActivity {

    private final String TAG = "RecognitionActivity";
    private Uri mUriPhotoTaken;

    private static final int REQUEST_TAKE_PHOTO_SIGNUP = 0;
    private static final int REQUEST_TAKE_PHOTO_SIGNIN = 1;

    private ProgressDialog.Builder dialogBuilder;
    private ProgressDialog dialog;

    private SharedPreferences sharedpreferences;

    private String recognitionImageUrl;

    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private FirebaseDatabase firebaseDatabase;

    private UUID firstFace;
    private UUID secondFace;

    private File file;

    private boolean fromCapture = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);


        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        firebaseStorage = FirebaseStorage.getInstance();
        sharedpreferences = getSharedPreferences(mypreference,
                Context.MODE_PRIVATE);

        dialogBuilder = new ProgressDialog.Builder(this);
        dialogBuilder.setMessage("Your SignUp Image is Processing");
        dialogBuilder.setProgressBarPosition(ProgressDialog.ProgressBarPosition.LEFT);
        dialog = dialogBuilder.create();



    }

    public void signUpAction(View view) {
//        startActivity(new Intent(RecognitionActivity.this, RecognitionSignupActivity.class));

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(getPackageManager()) != null) {
            // Save the photo taken to a temporary file.
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            try {
                File file = File.createTempFile("IMG_", ".jpg", storageDir);
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // only for M and newer versions
                    mUriPhotoTaken = FileProvider.getUriForFile(
                            RecognitionActivity.this,
                            RecognitionActivity.this.getPackageName() + ".provider", file);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                else {
                    mUriPhotoTaken = Uri.fromFile(file);
                }


                intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO_SIGNUP);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }


    }

    public void signInAction(View view) {
//        startActivity(new Intent(RecognitionActivity.this, RecognitionSignInActivity.class));

        if (Utils.profile.getRecognitionPassword().equalsIgnoreCase("empty")) {
            Toast.makeText(RecognitionActivity.this, "First Sign up for Recognition", Toast.LENGTH_SHORT).show();
            finish();
        }
        else {

            dialogBuilder.setMessage("Checking");
            dialog.show();

            firebaseStorage = FirebaseStorage.getInstance();

            file = new File(Environment.getExternalStorageDirectory() + "/SMS", "recognition_image.jpg");

            storageReference = firebaseStorage.getReferenceFromUrl(Utils.profile.getRecognitionPassword());

            storageReference.getFile(file).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    detect(bitmap);


                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(RecognitionActivity.this, "SignIn Failed", Toast.LENGTH_SHORT).show();
                }
            });

        }


    }


    // Deal with the result of selection of the photos and faces.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode)
        {
            case REQUEST_TAKE_PHOTO_SIGNUP:
                if (resultCode == RESULT_OK) {
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
                        Toast.makeText(RecognitionActivity.this, "Try Another Image", Toast.LENGTH_SHORT).show();
                        dialog.hide();
                    }


                }
                break;
            case REQUEST_TAKE_PHOTO_SIGNIN:
                if (resultCode == RESULT_OK) {

                    Uri imageUri;
                    if (data == null || data.getData() == null) {
                        imageUri = mUriPhotoTaken;
                    } else {
                        imageUri = data.getData();
                    }

                    Bitmap bitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            imageUri, getContentResolver());
                    if (bitmap != null) {
                        // Start detecting in image.
                        detect(bitmap);
                    }
                    else {
                        Toast.makeText(RecognitionActivity.this, "Please Select Another Picture", Toast.LENGTH_SHORT).show();
                    }
                }

                break;
            default:
                break;
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

    // Background task for face verification.
    private class VerificationTask extends AsyncTask<Void, String, VerifyResult> {
        // The IDs of two face to verify.
        private UUID mFaceId0;
        private UUID mFaceId1;

        VerificationTask (UUID faceId0, UUID faceId1) {
            mFaceId0 = faceId0;
            mFaceId1 = faceId1;
        }

        @Override
        protected VerifyResult doInBackground(Void... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try{
                publishProgress("Verifying...");

                // Start verification.
                return faceServiceClient.verify(
                        mFaceId0,      /* The first face ID to verify */
                        mFaceId1);     /* The second face ID to verify */
            }  catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            dialogBuilder.setMessage("Recognition is in Progress");
            dialog.show();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            //progressDialog.setMessage(progress[0]);
        }

        @Override
        protected void onPostExecute(VerifyResult result) {

            dialog.hide();
            // Show the result on screen when verification is done.
            setUiAfterVerification(result);
        }
    }

    // Background task of face detection.
    private class DetectionTask extends AsyncTask<InputStream, String, Face[]> {

        private boolean mSucceed = true;

        DetectionTask() {

        }

        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try{
                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        null);
            }  catch (Exception e) {
                mSucceed = false;
                Log.e(TAG, e.getMessage());
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            dialogBuilder.setMessage("Detection is in Progress");
            dialog.show();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            //progressDialog.setMessage(progress[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {
            // Show the result on screen when detection is done.


            //detection successful (mSucceed)

            if (!fromCapture && mSucceed) {
                //face detected on firebase image

                fromCapture = true;
                dialog.hide();
                firstFace = result[0].faceId;

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(intent.resolveActivity(getPackageManager()) != null) {
                    // Save the photo taken to a temporary file.
                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    try {
                        File file = File.createTempFile("IMG_", ".jpg", storageDir);
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // only for M and newer versions
                            mUriPhotoTaken = FileProvider.getUriForFile(
                                    RecognitionActivity.this,
                                    RecognitionActivity.this.getPackageName() + ".provider", file);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                        else {
                            mUriPhotoTaken = Uri.fromFile(file);
                        }

                        intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);
                        startActivityForResult(intent, REQUEST_TAKE_PHOTO_SIGNIN);
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }

            }
            else if (!fromCapture && !mSucceed) {
                //face not detected on firebase image
                Toast.makeText(RecognitionActivity.this, "SignUp Image is incorrect, Sign up Again", Toast.LENGTH_SHORT).show();
                dialog.hide();

            }
            else if (fromCapture && mSucceed) {

                secondFace = result[0].faceId;
                dialog.hide();

                new RecognitionActivity.VerificationTask(firstFace, secondFace).execute();

            }
            else if (fromCapture && !mSucceed) {
                dialog.hide();
                Toast.makeText(RecognitionActivity.this, "Please Select Another Picture", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(RecognitionActivity.this, "Something Wrong", Toast.LENGTH_SHORT).show();
                finish();
            }

        }
    }

    // Start detecting in image specified by index.
    private void detect(Bitmap bitmap) {
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        // Start a background task to detect faces in the image.
        new RecognitionActivity.DetectionTask().execute(inputStream);

    }

    // Show the result on screen when verification is done.
    private void setUiAfterVerification(VerifyResult result) {
        // Verification is done, hide the progress dialog.


        // Show verification result.
        if (result != null) {
            DecimalFormat formatter = new DecimalFormat("#0.00");
            String verificationResult = (result.isIdentical ? "The same person": "Different persons")
                    + ". The confidence is " + formatter.format(result.confidence);


            if (verificationResult.contains("The same person")) {
                file.delete();
                Toast.makeText(RecognitionActivity.this, verificationResult, Toast.LENGTH_SHORT).show();

                startActivity(new Intent(RecognitionActivity.this, UploadActivity.class));
            }
            else {
                Toast.makeText(RecognitionActivity.this, "Invalid Person", Toast.LENGTH_SHORT).show();
                finish();
            }


        }
    }

    // Save the activity state when it's going to stop.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("ImageUri", mUriPhotoTaken);
    }

    // Recover the saved state when the activity is recreated.
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mUriPhotoTaken = savedInstanceState.getParcelable("ImageUri");
    }


}
