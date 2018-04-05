package me.filedownloadingcheck.activities;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.util.Util;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import de.mrapp.android.dialog.ProgressDialog;
import me.filedownloadingcheck.Item;
import me.filedownloadingcheck.MyShareReciever;
import me.filedownloadingcheck.MyTestReceiver;
import me.filedownloadingcheck.Profile;
import me.filedownloadingcheck.R;
import me.filedownloadingcheck.RecyclerTouchListener;
import me.filedownloadingcheck.Services.ShareService;
import me.filedownloadingcheck.Utils;
import me.filedownloadingcheck.adapters.ContactsAdapter;

public class ContactsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<Profile> allProfiles;
    private ArrayList<Item> shareItems;
    private ArrayList<String> userIDList;

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private MyShareReciever myShareReciever;

    private ProgressDialog.Builder dialogBuilder;
    private ProgressDialog dialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        database = FirebaseDatabase.getInstance();

        shareItems =  getIntent().getParcelableArrayListExtra("items");

        allProfiles = new ArrayList<>();
        userIDList = new ArrayList<>();

        dialogBuilder = new ProgressDialog.Builder(this);
        //dialogBuilder.setTitle("Uploading Files");
        dialogBuilder.setMessage("Sharing Files");
        dialogBuilder.setProgressBarPosition(ProgressDialog.ProgressBarPosition.LEFT);
        dialog = dialogBuilder.create();

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());


        setupAdapter();


        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                setupServiceReceiver();

                Intent intent = new Intent(ContactsActivity.this, ShareService.class);
                intent.putExtra("items", shareItems);
                intent.putExtra("receiver", myShareReciever);
                intent.putExtra("sendermail", allProfiles.get(position).getEmail());
                intent.putExtra("sendername", allProfiles.get(position).getName());
                intent.putExtra("receiverID", userIDList.get(position));

                dialog.show();

                startService(intent);



            }

            @Override
            public void onLongClick(View view, int position) {
            }
        }));
    }

    private void setupAdapter() {
        myRef = database.getReference("user");

        myRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Profile profile = dataSnapshot.getValue(Profile.class);
                userIDList.add(dataSnapshot.getKey());
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


        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                int size = allProfiles.size();
                for (int i = 0; i< size;i++) {

                    String userId = userIDList.get(i);
                    if (userId.equals(Utils.userId)) {
                        userIDList.remove(userId);
                        allProfiles.remove(i);
                        break;
                    }


                }

                ContactsAdapter contactsAdapter = new ContactsAdapter(ContactsActivity.this, allProfiles, userIDList);

                recyclerView.setAdapter(contactsAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void setupServiceReceiver() {
        myShareReciever = new MyShareReciever(new Handler());
        // This is where we specify what happens when data is received from the service
        myShareReciever.setReceiver(new MyShareReciever.Receiver() {
            @Override
            public void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == RESULT_OK) {
                    Boolean resultValue = resultData.getBoolean("resultValue");

                    if(resultValue){
                        dialog.hide();
                        Toast.makeText(ContactsActivity.this, "Successfully Shared", Toast.LENGTH_SHORT).show();

                    }
                    else{
                        dialog.hide();
                        Toast.makeText(ContactsActivity.this, "Sharing Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }


}
