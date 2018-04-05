package me.filedownloadingcheck.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.VerifyResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.UUID;

import de.mrapp.android.dialog.ProgressDialog;
import me.filedownloadingcheck.R;
import me.filedownloadingcheck.Utils;
import me.filedownloadingcheck.helper.ImageHelper;
import me.filedownloadingcheck.helper.SampleApp;

public class RecognitionSignInActivity extends AppCompatActivity {

    private final int PIC_REQUEST = 200;

    // Progress dialog popped up when communicating with server.
    ProgressDialog progressDialog;

    private boolean fromCapture = false;

    private ProgressDialog.Builder dialogBuilder;
    private ProgressDialog dialog;


    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    private UUID firstFace;
    private UUID secondFace;

    private ImageView imageView;

    private File file;

    private Uri mUriPhotoTaken;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition_sign_in);

        if (Utils.profile.getRecognitionPassword().equalsIgnoreCase("empty")) {
            Toast.makeText(RecognitionSignInActivity.this, "First Sign up for Recognition", Toast.LENGTH_SHORT).show();
            finish();
        }
        else {

            imageView = findViewById(R.id.imageView);

            dialogBuilder = new ProgressDialog.Builder(this);
            dialogBuilder.setProgressBarPosition(ProgressDialog.ProgressBarPosition.LEFT);
            dialog = dialogBuilder.create();
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
                    finish();
                }
            });

        }
    }

    public void captureAction(View view) {
        Intent intent = new Intent(RecognitionSignInActivity.this, SelectImageActivity.class);
        startActivityForResult(intent,  PIC_REQUEST);
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
                Log.e("asdasda", e.getMessage());
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

            Log.e("asdasda", Boolean.toString(fromCapture));
            Log.e("asdasda", Boolean.toString(mSucceed));
            if (!fromCapture && mSucceed) {
                //face detected on firebase image

                fromCapture = true;
                dialog.hide();
                firstFace = result[0].faceId;

            }
            else if (!fromCapture && !mSucceed) {
                //face not detected on firebase image
                Toast.makeText(RecognitionSignInActivity.this, "SignUp Image is incorrect, Sign up Again", Toast.LENGTH_SHORT).show();
                dialog.hide();
                finish();

            }
            else if (fromCapture && mSucceed) {

               secondFace = result[0].faceId;
                dialog.hide();

                new RecognitionSignInActivity.VerificationTask(firstFace, secondFace).execute();

            }
            else if (fromCapture && !mSucceed) {
                dialog.hide();
                Toast.makeText(RecognitionSignInActivity.this, "Please Select Another Picture", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(RecognitionSignInActivity.this, "Something Wrong", Toast.LENGTH_SHORT).show();
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
        new RecognitionSignInActivity.DetectionTask().execute(inputStream);

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
                Toast.makeText(RecognitionSignInActivity.this, verificationResult, Toast.LENGTH_SHORT).show();

                startActivity(new Intent(RecognitionSignInActivity.this, UploadActivity.class));
            }
            else {
                Toast.makeText(RecognitionSignInActivity.this, "Invalid Person", Toast.LENGTH_SHORT).show();
                finish();
            }


        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PIC_REQUEST && resultCode == RESULT_OK) {
            Uri imageUri;
            if (data == null || data.getData() == null) {
                imageUri = mUriPhotoTaken;
            } else {
                imageUri = data.getData();
            }

            Bitmap bitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                    imageUri, getContentResolver());
            if (bitmap != null) {

                imageView.setImageBitmap(bitmap);

                // Start detecting in image.
                detect(bitmap);
            }
            else {
                Toast.makeText(RecognitionSignInActivity.this, "Please Select Another Picture", Toast.LENGTH_SHORT).show();
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
