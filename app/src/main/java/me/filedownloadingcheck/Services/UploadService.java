package me.filedownloadingcheck.Services;

import android.app.Activity;
import android.app.IntentService;

import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
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
import me.filedownloadingcheck.Utils;

/**
 * Created by Abdullah on 9/26/2017.
 */

public class UploadService extends IntentService {


    private Uri uri;
    FirebaseStorage storage;
    StorageReference storageRef;
    FirebaseDatabase database;
    DatabaseReference myRef;
    private String fileName;
    private File encFile;


    public UploadService(){
        super("UploadingService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {



        final ResultReceiver rec = intent.getParcelableExtra("receiver");

        uri = (Uri) intent.getExtras().get("URI");
        fileName = (String) intent.getStringExtra("fileName");
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("upload");

        Log.e("uri", uri.toString());

        encrypt(convertFileToByteArray(uri), getJustFileName(fileName), Utils.encryptionPassword);


        final StorageReference storageReference = storageRef.child(uri.getLastPathSegment());
        final String name = fileName;
        storageReference.putFile(Uri.fromFile(encFile)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                //deleting encrypted copy
                encFile.delete();
                String fileName = fileExt(name);
                if (fileName.endsWith("jpg")|| fileName.endsWith("png") || fileName.endsWith("jpeg")
                        || fileName.endsWith("bmp") ) {
                    Utils.currentType = "images";
                }
                else if (fileName.endsWith("pdf")) {
                    Utils.currentType = "pdf";
                }
                else if (fileName.endsWith("doc") || fileName.endsWith("docx") || fileName.endsWith("ppt")
                        || fileName.endsWith("pptx") ||fileName.endsWith("xls") || fileName.endsWith("txt")
                        ||fileName.endsWith("xlsx") || fileName.endsWith("csv") ) {
                    Utils.currentType = "docs";
                }
                else if (fileName.endsWith("mp4") || fileName.endsWith("mov") ||fileName.endsWith("mkv")
                        || fileName.endsWith("avi") || fileName.endsWith("3gp") || fileName.endsWith("flv")) {
                    Utils.currentType = "videos";
                }
                Item item = new Item( fileName , taskSnapshot.getDownloadUrl().toString());
                myRef = database.getReference(Utils.mainType + "/" + Utils.userId + "/" + Utils.currentType);
                myRef.push().setValue(item, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if(databaseError == null){
                            Bundle bundle = new Bundle();
                            bundle.putBoolean("resultValue", true);
                            rec.send(Activity.RESULT_OK, bundle);
                        }
                        else{
                            Bundle bundle = new Bundle();
                            bundle.putBoolean("resultValue", false);
                            rec.send(Activity.RESULT_OK, bundle);

                            StorageReference referenceFromUrl = storage.getReferenceFromUrl(taskSnapshot.getDownloadUrl().toString());
                            referenceFromUrl.delete();
                            Toast.makeText(UploadService.this, "Upload Database Failed", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("resultValue", false);
                rec.send(Activity.RESULT_OK, bundle);
                Toast.makeText(UploadService.this, "Upload Failed", Toast.LENGTH_LONG).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

            }
        });


    }


    public byte[] convertFileToByteArray(Uri uri) {
        byte[] byteArray = null;
        try
        {
            ParcelFileDescriptor pdf = this.getContentResolver().openFileDescriptor(uri, "r");

            InputStream inputStream = new FileInputStream(pdf.getFileDescriptor());
//            InputStream inputStream = this.getContentResolver().openInputStream(uri);

            Log.e("asdad", inputStream.toString());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024*8];
            int bytesRead =0;

            while ((bytesRead = inputStream.read(b)) != -1)
            {
                bos.write(b, 0, bytesRead);
            }

            byteArray = bos.toByteArray();
        }
        catch (IOException e)
        {
            e.printStackTrace();
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
