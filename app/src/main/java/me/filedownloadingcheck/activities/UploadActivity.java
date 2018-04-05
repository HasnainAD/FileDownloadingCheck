package me.filedownloadingcheck.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.File;

import de.mrapp.android.dialog.ProgressDialog;
import me.filedownloadingcheck.MyTestReceiver;
import me.filedownloadingcheck.R;
import me.filedownloadingcheck.Services.UploadService;
import me.filedownloadingcheck.Utils;
import me.filedownloadingcheck.adapters.FolderAdapter;

import static me.filedownloadingcheck.Utils.mypreference;

public class UploadActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private NavigationView nav_view;

    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseAuth auth;

    public MyTestReceiver receiverForTest;

//    ProgressDialog.Builder dialogBuilder;
//    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_activity_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //get firebase auth instance
        auth = FirebaseAuth.getInstance();
        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    // user auth state is changed - user is null
                    // launch login activity
                    startActivity(new Intent(UploadActivity.this, LoginActivity.class));
                    finish();
                }
                else {
                    Utils.userId = user.getUid();
                }
            }
        };



        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        nav_view = (NavigationView) findViewById(R.id.nav_view);
        nav_view.setNavigationItemSelectedListener(this);


//        dialogBuilder = new ProgressDialog.Builder(this);
//        //dialogBuilder.setTitle("Uploading Files");
//        dialogBuilder.setMessage("Your File is Uploading");
//        dialogBuilder.setProgressBarPosition(ProgressDialog.ProgressBarPosition.LEFT);
//        dialog = dialogBuilder.create();


        GridView gridview = (GridView) findViewById(R.id.folderGridView);
        gridview.setAdapter(new FolderAdapter(this));


        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(UploadActivity.this, UploadFilesActivity.class);

                intent.putExtra("mainType", "upload");

                if (i == 0) {
                    intent.putExtra("dataType", "pdf");
                }
                else if (i == 1) {
                    intent.putExtra("dataType", "videos");
                }
                else if (i == 2) {
                    intent.putExtra("dataType", "images");
                }
                else if (i == 3) {
                    intent.putExtra("dataType", "docs");
                }
                startActivity(intent);

            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), Utils.RC_PHOTO_PICKER);
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();

        nav_view.getMenu().getItem(0).setChecked(true);
        Utils.mainType = "upload";


        if (Utils.profile != null) {
            View view = nav_view.getHeaderView(0);
            TextView profileName = (TextView) view.findViewById(R.id.profileName);
            profileName.setText(Utils.profile.getName());
            TextView profileEmail = (TextView) view.findViewById(R.id.profileEmail);
            profileEmail.setText(Utils.profile.getEmail());
            ImageView profileImage = (ImageView) view.findViewById(R.id.profileImage);
            if (!Utils.profile.getProfilePhotoUrl().equals("empty")) {
                Glide.with(UploadActivity.this).load(Utils.profile.getProfilePhotoUrl()).apply(RequestOptions.circleCropTransform()).into(profileImage);
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(authListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authListener != null) {
            auth.removeAuthStateListener(authListener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == Utils.RC_PHOTO_PICKER && resultCode == RESULT_OK)
        {

//            setupServiceReceiver();
            final Uri uri = data.getData();
            Intent intent = new Intent(UploadActivity.this, UploadProgressActivity.class);
            String uriString = uri.toString();
            File myFile = new File(uriString);
            String displayName = null;

            if (uriString.startsWith("content://")) {
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            } else if (uriString.startsWith("file://")) {
                displayName = myFile.getName();
            }
            intent.putExtra("URI", uri);
            intent.putExtra("fileName", displayName);
            intent.putExtra("receiver", receiverForTest);
            //dialog.show();

            startActivity(intent);
        }

    }

//    public void setupServiceReceiver() {
//        receiverForTest = new MyTestReceiver(new Handler());
//        // This is where we specify what happens when data is received from the service
//        receiverForTest.setReceiver(new MyTestReceiver.Receiver() {
//            @Override
//            public void onReceiveResult(int resultCode, Bundle resultData) {
//                if (resultCode == RESULT_OK) {
//                    Boolean resultValue = resultData.getBoolean("resultValue");
//
//                    if(resultValue){
//                        dialog.hide();
//                        Toast.makeText(UploadActivity.this, "File Uploaded", Toast.LENGTH_SHORT).show();
//
//                    }
//                    else{
//                        dialog.hide();
//                        Toast.makeText(UploadActivity.this, "Upload Fail", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            }
//        });
//    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_inbox) {
            Intent intent = new Intent(UploadActivity.this, InboxActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

        } else if (id == R.id.nav_shared) {
            Intent intent = new Intent(UploadActivity.this, ShareActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_uploaded) {
        }
        else if (id == R.id.nav_profile) {
            Intent intent = new Intent(UploadActivity.this, Profile_Activity.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(UploadActivity.this, SettingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {

            auth.signOut();
            File file = new File(Environment.getExternalStorageDirectory()+"/SMS");
            Log.e("File", file.getAbsolutePath());
            deleteDirectory(file);
            SharedPreferences sharedpreferences = getSharedPreferences(mypreference,
                    Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString("userId","");
            editor.putString("profileName", "");
            editor.putString("profileEmail", "");
            editor.putString("profileNumnber", "");
            editor.putString("profileImageUrl", "");
            editor.putString("profileFacePassword", "");

            editor.commit();

            finish();

        } else if (id == R.id.nav_help) {
            Intent intent = new Intent(UploadActivity.this, HelpActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    private void deleteDirectory(File file){
        File[] list;
        if(!file.isDirectory())
        {
            file.delete();
        }
        else{
            list = file.listFiles();
            for(int i =0; i < list.length; i++){
                deleteDirectory(list[i]);
            }
        }

    }


}
