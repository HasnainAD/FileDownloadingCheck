package me.filedownloadingcheck.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

import me.filedownloadingcheck.Item;
import me.filedownloadingcheck.Profile;
import me.filedownloadingcheck.R;

/**
 * Created by Abdullah on 11/17/2017.
 */

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.MyViewHolder> {

    private ArrayList<Profile> dataList;
    private ArrayList<String> userIdList;
    private Context mContext;


    public ContactsAdapter(Context context, ArrayList<Profile> dataList, ArrayList<String> userIdList) {
        this.dataList = dataList;
        this.mContext = context;
        this.userIdList = userIdList;
    }


    @Override
    public ContactsAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_item_layout, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ContactsAdapter.MyViewHolder holder, int position) {

        holder.nameText.setText(dataList.get(position).getName());
        holder.emailText.setText(dataList.get(position).getEmail());


        if (!dataList.get(position).getProfilePhotoUrl().equals("empty"))
            Glide.with(mContext).load(dataList.get(position).getProfilePhotoUrl()).apply(RequestOptions.circleCropTransform()).into(holder.profileImageView);


    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }


    class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView profileImageView;
        TextView nameText;
        TextView emailText;


        public MyViewHolder(View itemView) {
            super(itemView);

            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameText = itemView.findViewById(R.id.nameText);
            emailText = itemView.findViewById(R.id.emailText);


        }


    }

}
