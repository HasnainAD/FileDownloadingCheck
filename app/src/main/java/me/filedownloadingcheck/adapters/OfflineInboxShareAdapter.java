package me.filedownloadingcheck.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.CardView;
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

public class OfflineInboxShareAdapter extends RecyclerView.Adapter<OfflineInboxShareAdapter.MyViewHolder> {

    private Context mContext;

    public ArrayList<File> data;
    public ArrayList<Integer> selectedData;

    // Constructor
    public OfflineInboxShareAdapter(Context c, ArrayList<File> data, ArrayList<Integer> selectedData) {
        mContext = c;
        this.data = data;
        this.selectedData = selectedData;

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);

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
        holder.avl.hide();





        if(Utils.currentType.equals("pdf")){
            holder.item_image.setImageResource(R.drawable.pdf_icon);
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
            holder.item_image.setImageResource(R.drawable.doc_icon);
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
            holder.item_image.setImageBitmap(BitmapFactory.decodeFile(data.get(position).getAbsolutePath()));
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
            holder.item_image.setImageBitmap(ThumbnailUtils.createVideoThumbnail(data.get(position).getAbsolutePath(), MediaStore.Video.Thumbnails.MICRO_KIND));
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

        String[] fileName = data.get(position).getName().split("\\|\\$");

        holder.upper_text.setText(fileName[0]);
        holder.lower_text.setText(fileName[1]);
        holder.item_name.setText(fileName[2]);

    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView item_image;
        TextView upper_text;
        ImageView downloadImage;
        TextView lower_text;
        TextView item_name;
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

}