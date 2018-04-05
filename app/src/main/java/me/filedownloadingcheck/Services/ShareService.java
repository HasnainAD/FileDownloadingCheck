package me.filedownloadingcheck.Services;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import me.filedownloadingcheck.Item;
import me.filedownloadingcheck.InboxShareItem;
import me.filedownloadingcheck.Utils;

/**
 * Created by Abdullah on 11/17/2017.
 */

public class ShareService  extends IntentService {


    private Uri uri;
    FirebaseStorage storage;
    StorageReference storageRef;
    FirebaseDatabase database;
    DatabaseReference myRef;
    private String fileName;
    private File encFile;

    private ArrayList<Item> items;
    private String receiverID;

    public ShareService(){
        super("SharingService");
    }

    @Override
    protected void onHandleIntent(@Nullable final Intent intent) {

        final ResultReceiver rec = intent.getParcelableExtra("receiver");
        items = intent.getParcelableArrayListExtra("items");
        receiverID = intent.getStringExtra("receiverID");
        final String sendername = intent.getStringExtra("sendername");
        final String sendermail = intent.getStringExtra("sendermail");

        database = FirebaseDatabase.getInstance();

        for (final Item item: items) {
            final InboxShareItem inboxItem = new InboxShareItem(item.getName(), item.getUrl(), Utils.userId);
            Log.e("name", item.getName() + item.getUrl());

            myRef = database.getReference("inbox/" + receiverID + "/" + Utils.currentType);
            myRef.push().setValue(inboxItem, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if(databaseError == null){

                        InboxShareItem inboxShareItem = new InboxShareItem(item.getName(), item.getUrl(), receiverID);

                        myRef = database.getReference("share/" +  Utils.userId+ "/" + Utils.currentType);
                        myRef.push().setValue(inboxShareItem, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError != null){
                                    Bundle bundle = new Bundle();
                                    bundle.putBoolean("resultValue", false);
                                    rec.send(Activity.RESULT_OK, bundle);

                                }
                            }
                        });

                    }
                    else{
                        Bundle bundle = new Bundle();
                        bundle.putBoolean("resultValue", false);
                        rec.send(Activity.RESULT_OK, bundle);
                    }
                }
            });



        }

        Bundle bundle = new Bundle();
        bundle.putBoolean("resultValue", true);
        rec.send(Activity.RESULT_OK, bundle);

    }



    public byte[] convertFileToByteArray(Uri uri)
    {
        byte[] byteArray = null;
        try
        {
            InputStream inputStream = this.getContentResolver().openInputStream(uri);
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
