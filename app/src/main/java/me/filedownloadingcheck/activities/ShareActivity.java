package me.filedownloadingcheck.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

import me.filedownloadingcheck.R;
import me.filedownloadingcheck.Utils;
import me.filedownloadingcheck.adapters.FolderAdapter;

import static me.filedownloadingcheck.Utils.mypreference;

public class ShareActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private NavigationView nav_view;

    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);

        auth = FirebaseAuth.getInstance();
        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    // user auth state is changed - user is null
                    // launch login activity
                    startActivity(new Intent(ShareActivity.this, LoginActivity.class));
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

        GridView gridview = (GridView) findViewById(R.id.folderGridView);
        gridview.setAdapter(new FolderAdapter(this));


        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(ShareActivity.this, ShareFilesActivity.class);

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


    }



    @Override
    protected void onResume() {
        super.onResume();

        nav_view.getMenu().getItem(2).setChecked(true);
        Utils.mainType = "share";

        if (Utils.profile != null) {
            View view = nav_view.getHeaderView(0);
            TextView profileName = (TextView) view.findViewById(R.id.profileName);
            profileName.setText(Utils.profile.getName());
            TextView profileEmail = (TextView) view.findViewById(R.id.profileEmail);
            profileEmail.setText(Utils.profile.getEmail());
            ImageView profileImage = (ImageView) view.findViewById(R.id.profileImage);
            if (!Utils.profile.getProfilePhotoUrl().equals("empty")) {
                Log.e("ada", Utils.profile.getProfilePhotoUrl());
                Glide.with(ShareActivity.this).load(Utils.profile.getProfilePhotoUrl()).apply(RequestOptions.circleCropTransform()).into(profileImage);
            }
        }

    }

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
            Intent intent = new Intent(ShareActivity.this, InboxActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_shared) {

        } else if (id == R.id.nav_uploaded) {
            Intent intent = new Intent(ShareActivity.this, UploadActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        else if (id == R.id.nav_profile) {
            Intent intent = new Intent(ShareActivity.this, Profile_Activity.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(ShareActivity.this, SettingActivity.class);
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
            Intent intent = new Intent(ShareActivity.this, HelpActivity.class);
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
