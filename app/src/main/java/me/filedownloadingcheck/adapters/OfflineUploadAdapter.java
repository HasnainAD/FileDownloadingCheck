package me.filedownloadingcheck.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;
import java.util.ArrayList;

import me.filedownloadingcheck.R;
import me.filedownloadingcheck.Utils;

/**
 * Created by Abdullah on 11/11/2017.
 */

public class OfflineUploadAdapter extends RecyclerView.Adapter<OfflineUploadAdapter.MyViewHolder> {

    private Context mContext;

    public ArrayList<File> data;
    public ArrayList<Integer> selectedData;

    // Constructor
    public OfflineUploadAdapter(Context c, ArrayList<File> data, ArrayList<Integer> selectedData) {
        mContext = c;
        this.data = data;
        this.selectedData = selectedData;

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.upload_item, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {


        if (selectedData.contains(position)) {

            holder.list_item_layout.setBackgroundColor(Color.parseColor("#00BAA9"));
        }
        else {
            holder.list_item_layout.setBackgroundColor(Color.WHITE);
        }

        holder.downloadImage.setVisibility(View.GONE);
        holder.loadingIndicatorView.hide();



        if(Utils.currentType.equals("pdf")){
            holder.file.setImageResource(R.drawable.pdf_icon);
            holder.list_item_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent newIntent = new Intent(Intent.ACTION_VIEW);
                    newIntent.setDataAndType(Uri.fromFile(data.get(position)),"application/pdf");
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(newIntent);

                }
            });
        }else if(Utils.currentType.equals("docs")){
            holder.file.setImageResource(R.drawable.doc_icon);
            holder.list_item_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent newIntent = new Intent(Intent.ACTION_VIEW);
                    newIntent.setDataAndType(Uri.fromFile(data.get(position)),"application/*");
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(newIntent);

                }
            });
        }
        else if(Utils.currentType.equals("images")){
            holder.file.setImageBitmap(BitmapFactory.decodeFile(data.get(position).getAbsolutePath()));
            holder.list_item_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent newIntent = new Intent(Intent.ACTION_VIEW);
                    newIntent.setDataAndType(Uri.fromFile(data.get(position)),"image/*");
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(newIntent);

                }
            });
        }
        else if(Utils.currentType.equals("videos")){
            holder.file.setImageBitmap(ThumbnailUtils.createVideoThumbnail(data.get(position).getAbsolutePath(), MediaStore.Video.Thumbnails.MICRO_KIND));
            holder.list_item_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent newIntent = new Intent(Intent.ACTION_VIEW);
                    newIntent.setDataAndType(Uri.fromFile(data.get(position)),"video/*");
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(newIntent);

                }
            });
        }

        holder.fileTitle.setText(data.get(position).getName());


    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView file;
        ImageView downloadImage;
        TextView fileTitle;
        AVLoadingIndicatorView loadingIndicatorView;
        ConstraintLayout list_item_layout;

        public MyViewHolder(View itemView) {
            super(itemView);
            file = (ImageView) itemView.findViewById(R.id.file);
            downloadImage = (ImageView) itemView.findViewById(R.id.downloadImageView);
            fileTitle = (TextView) itemView.findViewById(R.id.fileTitle);
            loadingIndicatorView = (AVLoadingIndicatorView ) itemView.findViewById(R.id.loadingindicator);
            list_item_layout = (ConstraintLayout) itemView;
        }
    }

}