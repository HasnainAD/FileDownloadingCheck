package me.filedownloadingcheck.activities;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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

import me.filedownloadingcheck.InboxShareItem;
import me.filedownloadingcheck.Profile;
import me.filedownloadingcheck.R;
import me.filedownloadingcheck.RecyclerTouchListener;
import me.filedownloadingcheck.Utils;
import me.filedownloadingcheck.adapters.OfflineInboxShareAdapter;
import me.filedownloadingcheck.adapters.OnlineInboxAdapter;
import me.filedownloadingcheck.adapters.OnlineShareAdapter;

public class ShareFilesActivity extends AppCompatActivity {


    private TextView noDataTextView;
    RecyclerView recyclerView;

    ArrayList<InboxShareItem> dataList;
    ArrayList<File> fileArrayList;
    ArrayList<Profile> allProfiles;
    ArrayList<Profile> selectedProfiles;

    ArrayList<String> userIdList;



    ArrayList<Integer> multiselect_list = new ArrayList<>();

    OnlineShareAdapter onlineShareAdapter;
    OfflineInboxShareAdapter offlineInboxShareAdapter;

    FirebaseDatabase database;
    DatabaseReference myRef;
    ChildEventListener childEventListener;
    ValueEventListener valueEventListener;
    FirebaseStorage  firebaseStorage;
    DatabaseReference inboxreference;
    ActionMode mActionMode;
    Menu context_menu;
    StorageReference  fileRef ;
    ArrayList<String> shareDatabaseKeyToDelete;
    ArrayList<String> inboxDatabaseKeyToDelete;
    DatabaseReference databaseReference;

    boolean netFlag = false;
    boolean isMultiSelect = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_files);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.parseColor("#ffffff"));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        shareDatabaseKeyToDelete = new ArrayList<>();
        inboxDatabaseKeyToDelete = new ArrayList<>();
        allProfiles = new ArrayList<>();
        selectedProfiles = new ArrayList<>();

        userIdList = new ArrayList<>();


        noDataTextView = (TextView) findViewById(R.id.no_dataTextView);
        recyclerView = (RecyclerView) findViewById(R.id.filesRecyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        Utils.currentType = getIntent().getStringExtra("dataType");

        dataList = new ArrayList<>();

        firebaseStorage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();

        myRef = database.getReference(Utils.mainType + "/" + Utils.userId + "/" + Utils.currentType);



        if (isWifiConn(ShareFilesActivity.this) || isMobileConn(ShareFilesActivity.this)) {
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
            onlineShareAdapter.selectedData = multiselect_list;
            onlineShareAdapter.data = dataList;
            onlineShareAdapter.notifyDataSetChanged();
        }
        else {
            offlineInboxShareAdapter.selectedData = multiselect_list;
            offlineInboxShareAdapter.data = fileArrayList;
            offlineInboxShareAdapter.notifyDataSetChanged();
        }
    }


    private void getValuesFromDataBase() {

        if (childEventListener == null) {
            childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    InboxShareItem item = dataSnapshot.getValue(InboxShareItem.class);
                    try {
                        dataList.add(item);
                    }catch (Exception e){
                        Log.e("ExceptionChildListener", item.getFileName());

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

                        Collections.reverse(dataList);

                        DatabaseReference databaseReference = database.getReference("user");

                        databaseReference.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                Profile profile = dataSnapshot.getValue(Profile.class);
                                userIdList.add(dataSnapshot.getKey());
                                allProfiles.add(profile);
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
                                for (int i = 0; i<dataList.size();i++) {
                                    String userId = dataList.get(i).getUserID();

                                    for (int j = 0; j< userIdList.size(); j++) {

                                        String id = userIdList.get(j);

                                        if (userId.equals(id)) {
                                            Profile profile = allProfiles.get(j);
                                            selectedProfiles.add(profile);
                                            break;
                                        }

                                    }

                                }

                                onlineShareAdapter = new OnlineShareAdapter(ShareFilesActivity.this, dataList, selectedProfiles, multiselect_list);
                                recyclerView.setAdapter(onlineShareAdapter);

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

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

        Collections.reverse(fileArrayList);

        offlineInboxShareAdapter = new OfflineInboxShareAdapter(ShareFilesActivity.this, fileArrayList, multiselect_list);
        recyclerView.setAdapter(offlineInboxShareAdapter);
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
            inflater.inflate(R.menu.menu_main2, menu);
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
                                final InboxShareItem value = dataSnapshot.getValue(InboxShareItem.class);
                                final DataSnapshot snapshot = dataSnapshot;

                                for(Integer a : multiselect_list){

                                    if( dataList.get(a).getUrl().equals(value.getUrl())){
                                        shareDatabaseKeyToDelete.add(snapshot.getKey());
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
//                case R.id.action_share:
//
//                    if  (netFlag) {
//
//                        ArrayList<InboxShareItem> shareItems = new ArrayList<>();
//
//                        for (Integer a: multiselect_list) {
//                            shareItems.add(dataList.get(a.intValue()));
//                        }
//
//                        Intent intent = new Intent(ShareFilesActivity.this, ContactsActivity.class);
//                        intent.putExtra("items", shareItems);
//                        startActivity(intent);
//
//
//                    }
//                    else {
//                        Toast.makeText(ShareFilesActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
//                    }
//                    return true;
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
        int size = shareDatabaseKeyToDelete.size();
        for(int i = 0; i < size; i++){
            databaseReference.child(shareDatabaseKeyToDelete.get(i)).removeValue();
        }
         Log.e("Size", Integer.toString(multiselect_list.size()));
        for(Integer a: multiselect_list){
            Log.e("ForLoop", Integer.toString(a.intValue()));
            InboxShareItem item = dataList.get(a.intValue());
            deleteItemfromInbox(item);
        }
        removeFromStorage();
    }

    private synchronized void removeFromInbox(String key, DatabaseReference inboxreference){

        DatabaseReference databaseReference = inboxreference;
        Log.e("Remove From Inbox" , key);
        databaseReference.child(key).removeValue();

    }


    private synchronized void deleteItemfromInbox(InboxShareItem item){


        inboxreference = database.getReference("inbox"+"/"+item.getUserID()+"/"+Utils.currentType);
        Log.e("Inbox reference", inboxreference.toString());
        final InboxShareItem shareitem = item;
        inboxreference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final InboxShareItem value = dataSnapshot.getValue(InboxShareItem.class);
                final DataSnapshot snapshot = dataSnapshot;

                Log.e("Child added", dataSnapshot.getKey());
                if( shareitem.getUrl().equals(value.getUrl())){
                        Log.e("Matched with ID", snapshot.getKey());
                        removeFromInbox(snapshot.getKey(), inboxreference);

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



    }


    private void removeFromStorage() {
        int size = multiselect_list.size();

        for(int i = 0; i < size; i++){

            String filename = allProfiles.get(multiselect_list.get(i).intValue()).getName() +"|$"+
                    allProfiles.get(multiselect_list.get(i).intValue()).getEmail() +"|$"+
                    dataList.get(multiselect_list.get(i).intValue()).getFileName();

            File file = new File(Environment.getExternalStorageDirectory() + "/SMS/"+Utils.mainType+
                    "/"+Utils.currentType+"/"+filename);
            file.delete();

            dataList.remove(multiselect_list.get(i).intValue());


        }
        multiselect_list = new ArrayList<>();
        refreshAdapter();
        if(mActionMode != null)
            mActionMode.setTitle("");
    }

}
