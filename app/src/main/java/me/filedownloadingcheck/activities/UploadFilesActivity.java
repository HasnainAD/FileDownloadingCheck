package me.filedownloadingcheck.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import me.filedownloadingcheck.Item;
import me.filedownloadingcheck.R;
import me.filedownloadingcheck.RecyclerTouchListener;
import me.filedownloadingcheck.Utils;
import me.filedownloadingcheck.adapters.OfflineUploadAdapter;
import me.filedownloadingcheck.adapters.OnlineUploadAdapter;

public class UploadFilesActivity extends AppCompatActivity {


    private TextView noDataTextView;
    RecyclerView recyclerView;

    ArrayList<Item> dataList;
    ArrayList<File> fileArrayList;


    ArrayList<Integer> multiselect_list = new ArrayList<>();

    OnlineUploadAdapter onlineUploadAdapter;
    OfflineUploadAdapter offlineUploadAdapter;

    FirebaseDatabase database;
    DatabaseReference myRef;
    ChildEventListener childEventListener;
    ValueEventListener valueEventListener;
    FirebaseStorage  firebaseStorage;

    ActionMode mActionMode;
    Menu context_menu;
    StorageReference  fileRef ;
    ArrayList<String> databaseKeyToDelete;

     DatabaseReference databaseReference;

    boolean netFlag = false;
    boolean isMultiSelect = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_files);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.parseColor("#ffffff"));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        databaseKeyToDelete = new ArrayList<>();


        noDataTextView = (TextView) findViewById(R.id.no_dataTextView);
        recyclerView = (RecyclerView) findViewById(R.id.filesRecyclerView);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(UploadFilesActivity.this, 2, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        Utils.currentType = getIntent().getStringExtra("dataType");

        dataList = new ArrayList<>();

        firebaseStorage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();

       myRef = database.getReference(Utils.mainType + "/" + Utils.userId + "/" + Utils.currentType);



        if (isWifiConn(UploadFilesActivity.this) || isMobileConn(UploadFilesActivity.this)) {
           Log.e("NetStatus","connected");
           netFlag = true;
           getValuesFromDataBase();

       }
       else {
           Log.e("NetStatus","not connected");

           netFlag = false;

           getValuesFromLocalStorage();
       }
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                if (isMultiSelect)
                    multi_select(position);
            }

            @Override
            public void onLongClick(View view, int position) {
                if (!isMultiSelect) {
                    multiselect_list = new ArrayList<>();
                    isMultiSelect = true;


                    if (mActionMode == null) {
                        mActionMode = startActionMode(mActionModeCallback);
                    }
                }
                multi_select(position);
            }
        }));


    }

    public void multi_select(int position) {
        if (mActionMode != null) {
            if (multiselect_list.contains(position)) {
                Integer a = position;
                multiselect_list.remove(a);
            }
            else {
                multiselect_list.add(position);
            }

            if (multiselect_list.size() > 0)
                mActionMode.setTitle("" + multiselect_list.size());
            else {
                mActionMode.setTitle("");
                mActionMode.finish();

            }

            refreshAdapter();
        }
    }

    public void refreshAdapter()
        {

        if (netFlag) {
            onlineUploadAdapter.selectedData = multiselect_list;
            onlineUploadAdapter.data = dataList;
            onlineUploadAdapter.notifyDataSetChanged();
        }
        else {
            offlineUploadAdapter.selectedData = multiselect_list;
            offlineUploadAdapter.data = fileArrayList;
            offlineUploadAdapter.notifyDataSetChanged();
        }
    }


    private void getValuesFromDataBase() {

        if (childEventListener == null) {
            childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Item item = dataSnapshot.getValue(Item.class);
                    try {
                      dataList.add(item);
                    }catch (Exception e){
                        Log.e("ExceptionChildListener", item.getName());

                    }

                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };


            valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    Log.e("Delete","TRue");
                    if(dataList.isEmpty())
                        noDataTextView.setVisibility(View.VISIBLE);
                    else{

                        onlineUploadAdapter = new OnlineUploadAdapter(UploadFilesActivity.this, dataList, multiselect_list);
                        recyclerView.setAdapter(onlineUploadAdapter);
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };

            myRef.addChildEventListener(childEventListener);
            myRef.addValueEventListener(valueEventListener);

        }

    }

    private void getValuesFromLocalStorage() {

        File file = new File(Environment.getExternalStorageDirectory() + "/" + "SMS" + "/" + Utils.mainType + "/" + Utils.currentType);

        fileArrayList = new ArrayList<>(Arrays.asList(file.listFiles()));
        offlineUploadAdapter = new OfflineUploadAdapter(UploadFilesActivity.this, fileArrayList, multiselect_list);
        recyclerView.setAdapter(offlineUploadAdapter);
    }

    public static boolean isWifiConn(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo.isConnected();
    }

    public static boolean isMobileConn(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return networkInfo.isConnected();
    }


    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_main, menu);
            context_menu = menu;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    Collections.sort(multiselect_list);
                    Collections.reverse(multiselect_list);
                    if(netFlag){
                        databaseReference = database.getReference(Utils.mainType+"/"+Utils.userId+"/"+Utils.currentType);
                        databaseReference.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                final Item  value = dataSnapshot.getValue(Item.class);
                                final DataSnapshot snapshot = dataSnapshot;

                                for(Integer a : multiselect_list){

                                    if( dataList.get(a).getUrl().equals(value.getUrl())){
                                        databaseKeyToDelete.add(snapshot.getKey());
                                        break;
                                    }
                                }
                            }

                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onChildRemoved(DataSnapshot dataSnapshot) {

                            }

                            @Override
                            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                        databaseReference.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                deleteFromDatabase();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                    else {
                        //Offline Delete from local storage
                        for (Integer a: multiselect_list) {
                            File file = fileArrayList.get(multiselect_list.get(a.intValue()));
                            file.delete();
                            fileArrayList.remove(file);

                        }
                        multiselect_list = new ArrayList<>();
                        refreshAdapter();
                        mActionMode.setTitle("");
                    }

                    return true;
                case R.id.action_share:

                    if  (netFlag) {

                        ArrayList<Item> shareItems = new ArrayList<>();

                        for (Integer a: multiselect_list) {
                            shareItems.add(dataList.get(a.intValue()));
                        }

                        Intent intent = new Intent(UploadFilesActivity.this, ContactsActivity.class);
                        intent.putExtra("items", shareItems);
                        startActivity(intent);


                    }
                    else {
                        Toast.makeText(UploadFilesActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            isMultiSelect = false;
            multiselect_list = new ArrayList<>();
            refreshAdapter();
        }
    };

    private  void deleteFromDatabase(){
        //databaseReference.addValueEventListener(null);
        int size = databaseKeyToDelete.size();
        for(int i = 0; i < size; i++){
            databaseReference.child(databaseKeyToDelete.get(i)).removeValue();
        }
        removeFromStorage();
    }

    private void removeFromStorage() {
        int size = multiselect_list.size();
//Log.e("Size",multiselect_list.toString());
        for(int i = 0; i < size; i++){

            File file = new File(Environment.getExternalStorageDirectory() + "/SMS/"+Utils.mainType+
                    "/"+Utils.currentType+"/"+dataList.get(multiselect_list.get(i).intValue()).getName());
            file.delete();


            fileRef = firebaseStorage.getReferenceFromUrl(dataList.get(multiselect_list.get(i).intValue()).getUrl());
            dataList.remove(multiselect_list.get(i).intValue());

            fileRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // File deleted successfully
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.e("Deletion Failed", exception.getMessage());
                    Toast.makeText(UploadFilesActivity.this,"Cannot Delete from storage", Toast.LENGTH_SHORT).show();
                }
            });

        }
        multiselect_list = new ArrayList<>();
        refreshAdapter();
        if(mActionMode != null)
            mActionMode.setTitle("");
    }


}
