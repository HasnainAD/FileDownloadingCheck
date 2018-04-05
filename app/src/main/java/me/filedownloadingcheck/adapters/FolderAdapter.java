package me.filedownloadingcheck.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import me.filedownloadingcheck.R;

/**
 * Created by Abdullah on 11/7/2017.
 */

public class FolderAdapter extends BaseAdapter{

    private Context mContext;
    private int icons[] = {R.drawable.pdf_icon, R.drawable.video_icon, R.drawable.image_icon ,R.drawable.doc_icon};

    // Constructor
    public FolderAdapter(Context c) {
        mContext = c;
    }

    public int getCount() {
        return icons.length;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).
                    inflate(R.layout.folder_item_layout, parent, false);;
        }
        ImageView typeicon = (ImageView) convertView.findViewById(R.id.typeicon);
        typeicon.setImageResource(icons[position]);

        return convertView;
    }
}
