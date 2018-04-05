package me.filedownloadingcheck;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;

/**
 * Created by Abdullah on 10/31/2017.
 */

public class Item implements Parcelable {

    private String name;
    private String url;
    public  Item(){}

    public Item(String name, String url){
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(url);
    }


    // Creator
    public static final Parcelable.Creator CREATOR
            = new Parcelable.Creator() {
        public Item createFromParcel(Parcel in) {
            return new Item(in);
        }

        public Item[] newArray(int size) {
            return new Item[size];
        }
    };


    public Item(Parcel in){
        this.name = in.readString();
        this.url = in.readString();
    }

}
