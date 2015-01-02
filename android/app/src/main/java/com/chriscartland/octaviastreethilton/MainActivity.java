package com.chriscartland.octaviastreethilton;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.SignInButton;

import java.util.ArrayList;


public class MainActivity extends GoogleOAuthActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ListView mListView;
    private SignInButton mGoogleSignInButton;
    private Firebase firebaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Resources res = getResources();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(res.getColor(R.color.color_primary));
        setSupportActionBar(toolbar);

        // Now retrieve the DrawerLayout so that we can set the status bar color.
        // This only takes effect on Lollipop, or when using translucentStatusBar
        // on KitKat.
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_main);
        drawerLayout.setStatusBarBackgroundColor(res.getColor(R.color.color_primary_dark));

        ArrayAdapter adapter = new ArrayAdapter(this, R.layout.list_room_item, new ArrayList<>());
        mListView = (ListView) findViewById(R.id.room_list);
        mListView.setAdapter(adapter);

        setupFirebase();

        setupGoogleSignIn();
    }

    private void setupFirebase() {
        Firebase.setAndroidContext(this);
        firebaseRef = new Firebase(getString(R.string.firebase_url));

        firebaseRef.child("room_names").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int i = 0;
                ArrayList<String> items = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    items.add(i, child.getValue().toString());
                    i++;
                }
                ArrayAdapter adapter = new ArrayAdapter(MainActivity.this,
                        R.layout.list_room_item, items);
                mListView.setAdapter(adapter);
            }
            @Override public void onCancelled(FirebaseError error) {
                Log.d(TAG, "onCancelled");
            }
        });

        firebaseRef.addAuthStateListener(new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if (authData != null) {
                    // user is logged in
                    Log.d(TAG, "User is logged in, Uid: " + authData.getUid());
                } else {
                    // user is not logged in
                    Log.d(TAG, "User is not logged in");
                }
            }
        });
    }

    private void setupGoogleSignIn() {
        /* Load the Google Sign-In button */
        mGoogleSignInButton = (SignInButton) findViewById(R.id.login_with_google);
        mGoogleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignInButtonClicked();
            }
        });
    }

    @Override
    protected void onReceivedGoogleOAuthToken(String token, String error) {
        super.onReceivedGoogleOAuthToken(token, error);
        if (token != null) {
            authGoogleFirebase(token);
        }
    }

    private void authGoogleFirebase(String token) {
        firebaseRef.authWithOAuthToken("google", token, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                // the Google user is now authenticated with Firebase
                Log.d(TAG, "Google user authenticated: " + authData.getUid());
            }
            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                // there was an error
                Log.d(TAG, "Firebase authentication error with Google: " + firebaseError);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
