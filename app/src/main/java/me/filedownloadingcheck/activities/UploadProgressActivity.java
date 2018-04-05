package me.filedownloadingcheck.activities;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import me.filedownloadingcheck.Item;
import me.filedownloadingcheck.R;
import me.filedownloadingcheck.Services.UploadService;
import me.filedownloadingcheck.Utils;

public class UploadProgressActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView output;
    private Integer count = 1;

    private Uri uri;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private String fileName;
    private File encFile;

    private boolean uploadedFlag = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_progress);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setMax(100);
        output = (TextView) findViewById(R.id.output);


        uri = (Uri) getIntent().getExtras().get("URI");
        fileName = (String) getIntent().getStringExtra("fileName");
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("upload");

        //Ininalizing Progress bar and start uploading
        count =1;
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        new MyTask().execute(100);


    }

    class MyTask extends AsyncTask<Integer, Integer, String> {
        @Override
        protected String doInBackground(Integer... params) {


            byte[] arr = convertFileToByteArray(uri);

            if (null != arr) {

                encrypt(arr, getJustFileName(fileName), Utils.encryptionPassword);

                final StorageReference storageReference = storageRef.child(uri.getLastPathSegment());
                final String name = fileName;
                storageReference.putFile(Uri.fromFile(encFile)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                        //deleting encrypted copy
                        encFile.delete();
                        String fileName = fileExt(name);
                        if (fileName.endsWith("jpg") || fileName.endsWith("png") || fileName.endsWith("jpeg")
                                || fileName.endsWith("bmp")) {
                            Utils.currentType = "images";
                        } else if (fileName.endsWith("pdf")) {
                            Utils.currentType = "pdf";
                        } else if (fileName.endsWith("doc") || fileName.endsWith("docx") || fileName.endsWith("ppt")
                                || fileName.endsWith("pptx") || fileName.endsWith("xls") || fileName.endsWith("txt")
                                || fileName.endsWith("xlsx") || fileName.endsWith("csv")) {
                            Utils.currentType = "docs";
                        } else if (fileName.endsWith("mp4") || fileName.endsWith("mov") || fileName.endsWith("mkv")
                                || fileName.endsWith("avi") || fileName.endsWith("3gp") || fileName.endsWith("flv")) {
                            Utils.currentType = "videos";
                        }
                        Item item = new Item(fileName, taskSnapshot.getDownloadUrl().toString());
                        myRef = database.getReference(Utils.mainType + "/" + Utils.userId + "/" + Utils.currentType);
                        myRef.push().setValue(item, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    StorageReference referenceFromUrl = storage.getReferenceFromUrl(taskSnapshot.getDownloadUrl().toString());
                                    referenceFromUrl.delete();
                                    Toast.makeText(UploadProgressActivity.this, "Upload Database Failed", Toast.LENGTH_LONG).show();
                                }
                                uploadedFlag = true;
                                finish();
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(UploadProgressActivity.this, "Upload Failed", Toast.LENGTH_LONG).show();
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        count = (int) progress;
                        publishProgress(count);
                    }
                });
                return "ok";
            }
            else {
                Log.e("jhjj", "in else");
                return "notok";

            }

        }
        @Override
        protected void onPostExecute(String result) {
            if (result.equalsIgnoreCase("notok")) {
                Toast.makeText(UploadProgressActivity.this, "File not Found", Toast.LENGTH_SHORT).show();
                finish();
            }


        }
        @Override
        protected void onPreExecute() {
            output.setText("Uploading Started...");
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            output.setText("Uploading..."+ values[0] + "%");
            progressBar.setProgress(values[0]);
        }
    }

    @Override
    public void onBackPressed() {

        if (uploadedFlag)
            super.onBackPressed();
        else {
            Toast.makeText(UploadProgressActivity.this, "Uploading in Progress", Toast.LENGTH_SHORT).show();
        }

    }

//    public byte[] convertFileToByteArray(Uri uri) {
//        byte[] byteArray = null;
//        try
//        {
//            InputStream inputStream = this.getContentResolver().openInputStream(uri);
//
//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            byte[] b = new byte[1024*8];
//            int bytesRead =0;
//
//            while ((bytesRead = inputStream.read(b)) != -1)
//            {
//                bos.write(b, 0, bytesRead);
//            }
//
//            byteArray = bos.toByteArray();
//        }
//        catch (IOException e)
//        {
//            e.printStackTrace();
//        }
//
//        return byteArray;
//    }

    public byte[] convertFileToByteArray(Uri uri) {
        byte[] byteArray = null;
        try
        {
            InputStream inputStream ;
            String uripath = uri.toString();
            if(uripath.contains("content://com.android.providers.downloads")){
                //File From download
                File file = new File("/sdcard/Download/"+fileName);
                inputStream = new FileInputStream(file);
            }else if(uripath.contains("content://com.google.android.apps.docs")){
                //File From gooogle drive
                return null;

            }else {
                //Not from downloads
                inputStream = this.getContentResolver().openInputStream(uri);
            }


            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024*8];
            int bytesRead =0;
            while ((bytesRead = inputStream.read(b)) != -1){
                bos.write(b, 0, bytesRead);
            }
            byteArray = bos.toByteArray();
        }
        catch (IOException e)
        {
            Log.e("ToByteCatch", e.getMessage());
        }

        return byteArray;
    }

    private void encrypt(byte[] text, String encFileName,  String password) {
        try {
            SecretKeySpec key = generateKey(password);
            Cipher cipher = Cipher.getInstance(Utils.AES);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encVal = cipher.doFinal(text);

            byte[] encryptedValByte = Base64.encode(encVal, Base64.NO_PADDING);

            encFile = new File(Environment.getExternalStorageDirectory() + "/SMS/", encFileName + ".enc");

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(encFile));
            bos.write(encryptedValByte);
            bos.flush();
            bos.close();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private SecretKeySpec generateKey(String password ) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] bytes = password.getBytes("UTF-8");

        digest.update(bytes, 0 ,  bytes.length);
        byte[] key = digest.digest();
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        return secretKeySpec;

    }

    private String fileExt(String url) {

        String[] tokens = url.split("/");
        return tokens[tokens.length-1];


    }

    private String getJustFileName(String name) {
        String[] tokenFile = name.split("\\.");
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i< tokenFile.length-1; i++) {
            stringBuilder.append(tokenFile[i]);
        }

        return stringBuilder.toString();
    }


}
