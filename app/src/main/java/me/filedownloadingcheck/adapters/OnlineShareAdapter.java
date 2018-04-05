package me.filedownloadingcheck.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

import me.filedownloadingcheck.InboxShareItem;
import me.filedownloadingcheck.Profile;
import me.filedownloadingcheck.R;
import me.filedownloadingcheck.Utils;

/**
 * Created by Abdullah on 11/18/2017.
 */

public class OnlineShareAdapter extends RecyclerView.Adapter<OnlineShareAdapter.MyViewHolder> {


    private Context mContext;

    public ArrayList<InboxShareItem> data;
    public ArrayList<Integer> selectedData;
    public ArrayList<Profile> allProfiles;

    FirebaseStorage storage;
    StorageReference storageRef;

    String folderPath;
    File filePath;


    // Constructor
    public OnlineShareAdapter(Context c, ArrayList<InboxShareItem> data,ArrayList<Profile> allProfiles, ArrayList<Integer> selectedData ) {
        mContext = c;
        this.data = data;
        this.selectedData = selectedData;
        this.allProfiles = allProfiles;

        folderPath = Environment.getExternalStorageDirectory().getPath();
        folderPath += "/SMS/" + Utils.mainType + "/" + Utils.currentType;
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();


    }


    @Override
    public OnlineShareAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);

        return new OnlineShareAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(OnlineShareAdapter.MyViewHolder holder, final int position) {



        if (selectedData.contains(position)) {

            holder.list_item_layout.setBackgroundColor(Color.parseColor("#00BAA9"));
        }
        else {
            holder.list_item_layout.setBackgroundColor(Color.WHITE);
        }

        final String filename = allProfiles.get(position).getName()+"|$"+allProfiles.get(position).getEmail()+"|$"+ data.get(position).getFileName();

        filePath = new File(folderPath, filename);

        if (filePath.exists()) {
            holder.downloadImage.setVisibility(View.GONE);
            holder.avl.hide();


            if(Utils.currentType.equals("pdf")){
                holder.item_image.setImageResource(R.drawable.pdf);

                holder.list_item_layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        filePath = new File(folderPath, filename);
                        Intent newIntent = new Intent(Intent.ACTION_VIEW);
                        newIntent.setDataAndType(Uri.fromFile(filePath),"application/pdf");
                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(newIntent);

                    }
                });
            }else if(Utils.currentType.equals("docs")){
                holder.item_image.setImageResource(R.drawable.doc);
                holder.list_item_layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        filePath = new File(folderPath, filename);
                        Intent newIntent = new Intent(Intent.ACTION_VIEW);
                        newIntent.setDataAndType(Uri.fromFile(filePath),"application/*");
                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(newIntent);

                    }
                });
            }
            else if(Utils.currentType.equals("images")){
                holder.item_image.setImageBitmap(BitmapFactory.decodeFile(filePath.getAbsolutePath()));
                holder.list_item_layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        filePath = new File(folderPath, filename);
                        Intent newIntent = new Intent(Intent.ACTION_VIEW);
                        newIntent.setDataAndType(Uri.fromFile(filePath),"image/*");
                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(newIntent);

                    }
                });
            }
            else if(Utils.currentType.equals("videos")){
                holder.item_image.setImageBitmap(ThumbnailUtils.createVideoThumbnail(filePath.getAbsolutePath(), MediaStore.Video.Thumbnails.MICRO_KIND));
                holder.list_item_layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        filePath = new File(folderPath, filename);
                        Intent newIntent = new Intent(Intent.ACTION_VIEW);
                        newIntent.setDataAndType(Uri.fromFile(filePath),"video/*");
                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(newIntent);

                    }
                });
            }
        }
        else {
            holder.downloadImage.setVisibility(View.VISIBLE);
            holder.avl.hide();

            if(Utils.currentType.equals("pdf")){
                holder.item_image.setImageResource(R.drawable.pdf);
            }else if(Utils.currentType.equals("docs")){
                holder.item_image.setImageResource(R.drawable.doc);
            }
            else if(Utils.currentType.equals("images")){
                holder.item_image.setImageResource(R.drawable.jpg);
            }
            else if(Utils.currentType.equals("videos")){
                holder.item_image.setImageResource(R.drawable.mp4);
            }

            final OnlineShareAdapter.MyViewHolder finalHolder = holder;
            final int finalPosition = position;
            holder.downloadImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    finalHolder.avl.show();
                    finalHolder.downloadImage.setVisibility(View.GONE);
                    InboxShareItem item = data.get(finalPosition);

                    storageRef =   storage.getReferenceFromUrl(item.getUrl());
                    File localFile = null;

                    localFile = new File(Environment.getExternalStorageDirectory() +"/SMS/" + getJustFileName(data.get(position).getFileName()) + ".enc");
                    if(!localFile.exists())
                        try {
                            localFile.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    final File finalLocalFile = localFile;
                    storageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                            //dec
                            final File decrptedFile =   decrypt(convertFileToByteArray(finalLocalFile), position, Utils.encryptionPassword);

                            //delete the enc file
                            finalLocalFile.delete();

                            finalHolder.avl.hide();


                            if(Utils.currentType.equals("pdf")){
                                finalHolder.item_image.setImageResource(R.drawable.pdf_icon);

                                finalHolder.list_item_layout.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        filePath = new File(folderPath, decrptedFile.getName());
                                        Intent newIntent = new Intent(Intent.ACTION_VIEW);
                                        newIntent.setDataAndType(Uri.fromFile(filePath),"application/pdf");
                                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        mContext.startActivity(newIntent);

                                    }
                                });
                            }else if(Utils.currentType.equals("docs")){
                                finalHolder.item_image.setImageResource(R.drawable.doc_icon);

                                finalHolder.list_item_layout.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        filePath = new File(folderPath, decrptedFile.getName());
                                        Intent newIntent = new Intent(Intent.ACTION_VIEW);
                                        newIntent.setDataAndType(Uri.fromFile(filePath),"application/*");
                                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        mContext.startActivity(newIntent);

                                    }
                                });
                            }
                            else if(Utils.currentType.equals("images")){
                                finalHolder.item_image.setImageBitmap(BitmapFactory.decodeFile(decrptedFile.getAbsolutePath()));

                                finalHolder.list_item_layout.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        filePath = new File(folderPath, decrptedFile.getName());
                                        Intent newIntent = new Intent(Intent.ACTION_VIEW);
                                        newIntent.setDataAndType(Uri.fromFile(filePath),"image/*");
                                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        mContext.startActivity(newIntent);

                                    }
                                });
                            }
                            else if(Utils.currentType.equals("videos")){
                                finalHolder.item_image.setImageBitmap(ThumbnailUtils.createVideoThumbnail(decrptedFile.getAbsolutePath(), MediaStore.Video.Thumbnails.MICRO_KIND));

                                finalHolder.list_item_layout.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        filePath = new File(folderPath, decrptedFile.getName());
                                        Intent newIntent = new Intent(Intent.ACTION_VIEW);
                                        newIntent.setDataAndType(Uri.fromFile(filePath),"video/*");
                                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        mContext.startActivity(newIntent);

                                    }
                                });
                            }

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.e("Status","not downloaded");
                            Toast.makeText(mContext, "Download Failed!", Toast.LENGTH_SHORT).show();
                            finalHolder.avl.hide();
                            finalHolder.downloadImage.setVisibility(View.VISIBLE);
                            finalLocalFile.delete();

                        }
                    });




                }
            });


        }

        holder.upper_text.setText(allProfiles.get(position).getName());
        holder.lower_text.setText(allProfiles.get(position).getEmail());
        holder.item_name.setText(data.get(position).getFileName());


    }


    @Override
    public int getItemCount() {
        return data.size();
    }


    class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView item_image;
        TextView upper_text;
        TextView item_name;
        ImageView downloadImage;
        TextView lower_text;
        AVLoadingIndicatorView avl;
        CardView list_item_layout;

        public MyViewHolder(View itemView) {
            super(itemView);
            item_image = (ImageView) itemView.findViewById(R.id.item_image);
            item_name = (TextView) itemView.findViewById(R.id.item_name);
            downloadImage = (ImageView) itemView.findViewById(R.id.downloadImageView);
            upper_text = (TextView) itemView.findViewById(R.id.upper_text);
            lower_text = (TextView) itemView.findViewById(R.id.lower_text);
            avl = (AVLoadingIndicatorView ) itemView.findViewById(R.id.loadingindicator);
            list_item_layout = (CardView) itemView;
        }
    }

    private String getJustFileName(String name) {
        String[] tokenFile = name.split("\\.");
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i< tokenFile.length-1; i++) {
            stringBuilder.append(tokenFile[i]);
        }

        return stringBuilder.toString();
    }

    private SecretKeySpec generateKey(String password ) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] bytes = password.getBytes("UTF-8");

        digest.update(bytes, 0 ,  bytes.length);
        byte[] key = digest.digest();
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        return secretKeySpec;

    }

    private File decrypt(byte[] text, int position, String password) {

        File myFile = null;
        try {
            SecretKeySpec key = generateKey(password);

            Cipher cipher = Cipher.getInstance(Utils.AES);
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decodedValue = Base64.decode(text, Base64.NO_PADDING);
            byte[] decValue = cipher.doFinal(decodedValue);

            final String filename = allProfiles.get(position).getName()+"|$"+allProfiles.get(position).getEmail()+"|$"+ data.get(position).getFileName();

            myFile = new File(folderPath, filename);

            FileOutputStream out = null;
            try {
                out = new FileOutputStream(myFile);
                out.write(decValue);
                out.flush();
                return myFile;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return myFile;
    }


    public byte[] convertFileToByteArray(File f)
    {
        byte[] byteArray = null;
        try
        {
            InputStream inputStream = new FileInputStream(f);
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

}
