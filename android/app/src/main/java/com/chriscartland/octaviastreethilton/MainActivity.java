package com.chriscartland.octaviastreethilton;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.common.SignInButton;

import java.util.ArrayList;
import java.util.Map;


public class MainActivity extends ActionBarActivity implements
        GoogleOAuthManager.GoogleOAuthManagerCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    private GoogleOAuthManager mGoogleOAuthManager;
    private SignInButton mGoogleSignInButton;
    private Button mSignOutButton;

    private Firebase mFirebase;
    private ChildEventListener mTransactionListener;

    private DrawerLayout mDrawerLayout;
    private ListView mListView;
    private ListView mDrawerNavigation;
    private ImageView mIdentityImage;
    private TextView mIdentityName;

    private ArrayAdapter<CharSequence>  mSpinnerAdapter;
    private String mTransactionFilter;
    private String mRoomId;
    private Map<String, String> mCachedUserProfile;
    private ArrayList<Transaction> mTransactions;

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

        // Get default room ID.
        mRoomId = getString(R.string.default_room_id);

        mIdentityImage = (ImageView) findViewById(R.id.identity_image);
        mIdentityName = (TextView) findViewById(R.id.identity_name);

        // Create an ArrayAdapter using the string array and a default spinner layout
        mSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.transactions_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        Spinner spinner = (Spinner) findViewById(R.id.transaction_spinner);
        spinner.setAdapter(mSpinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mTransactionFilter = (String) parent.getItemAtPosition(position);
                updateTransactionFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mTransactionFilter = null;
                updateTransactionFilter();
            }
        });

        mTransactions = new ArrayList<>();
        ArrayAdapter adapter = new ArrayAdapter(this, R.layout.list_item, mTransactions);
        mListView = (ListView) findViewById(R.id.transaction_list);
        mListView.setAdapter(adapter);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_main);
        ArrayAdapter drawerAdapter = ArrayAdapter.createFromResource(this,
                R.array.navigation_array, R.layout.list_item);
        mDrawerNavigation = (ListView) findViewById(R.id.drawer_navigation);
        mDrawerNavigation.setAdapter(drawerAdapter);
        mDrawerNavigation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setTitle(parent.getItemAtPosition(position).toString());
                mDrawerLayout.closeDrawer(Gravity.START);
            }
        });
        setTitle(mDrawerNavigation.getItemAtPosition(0).toString());

        setupFirebase();
        setupGoogleSignIn();
        mGoogleOAuthManager.signIn();

        updateTransactionFilter();
        updateIdentityUi();
    }

    private void updateTransactionFilter() {
        if (mTransactionFilter == null) {
            mTransactionFilter = mSpinnerAdapter.getItem(0).toString();
        }
        // TODO(cartland): Update the transaction filter.
    }

    private void updateIdentityUi() {
        String displayName;
        String image;
        if (mCachedUserProfile != null) {
            displayName = mCachedUserProfile.get("name");
            image = mCachedUserProfile.get("picture");
            mGoogleSignInButton.setVisibility(View.GONE);
            mSignOutButton.setVisibility(View.VISIBLE);
        } else {
            displayName = "";
            image = null;
            mGoogleSignInButton.setVisibility(View.VISIBLE);
            mSignOutButton.setVisibility(View.GONE);
        }
        Log.d(TAG, "updateIdentityUi(displayName=" + displayName + ", image=" + image + ")");
        mIdentityName.setText(displayName);
        Glide.with(MainActivity.this)
                .load(image)
                .error(R.drawable.ic_launcher)
                .into(mIdentityImage);
    }

    private void updateTransactionsUi() {
        ArrayAdapter adapter = new ArrayAdapter(MainActivity.this,
                R.layout.list_item, mTransactions);
        mListView.setAdapter(adapter);
    }

    private void setupFirebase() {
        Firebase.setAndroidContext(this);
        mFirebase = new Firebase(getString(R.string.firebase_url));

        mTransactionListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                mTransactions.add(Transaction.newFromSnapshot(dataSnapshot));
                updateTransactionsUi();

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                mTransactions.add(Transaction.newFromSnapshot(dataSnapshot));
                updateTransactionsUi();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                mTransactions.add(Transaction.newFromSnapshot(dataSnapshot));
                updateTransactionsUi();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                mTransactions.add(Transaction.newFromSnapshot(dataSnapshot));
                updateTransactionsUi();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.d(TAG, "transaction event canceled: " + firebaseError);
            }
        };

        mFirebase.addAuthStateListener(new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if (authData != null) {
                    // user is logged in
                    Log.d(TAG, "User is logged in, Uid: " + authData.getUid());
                } else {
                    // user is not logged in
                    Log.d(TAG, "User is not logged in");
                }
                updateAuthDependentListeners();
            }
        });
        updateAuthDependentListeners();
    }

    private void updateAuthDependentListeners() {
        // Firebase does not call value event listeners when the auth state changes.
        // In order for our event listeners to get data based on new auth information,
        // we must remove the event listener and add it again every time we detect that
        // the auth state has changed.

        mFirebase.child("transactions").child(mRoomId).removeEventListener(mTransactionListener);
        mFirebase.child("transactions").child(mRoomId).addChildEventListener(mTransactionListener);
    }

    private void setupGoogleSignIn() {
        mGoogleOAuthManager = new GoogleOAuthManager();
        mGoogleOAuthManager.setActivity(this);
        mGoogleOAuthManager.connect();

        /* Load the Google Sign-In button */
        mGoogleSignInButton = (SignInButton) findViewById(R.id.sign_in_with_google);
        mGoogleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGoogleOAuthManager.signIn();
            }
        });
        /* Sign out button */
        mSignOutButton = (Button) findViewById(R.id.sign_out);
        mSignOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGoogleOAuthManager.signOut();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Don't forget to call GoogleOAuthManager.onActivityResult()
        mGoogleOAuthManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onReceivedGoogleOAuthToken(String token, String error) {
        Log.d(TAG, "onReceivedGoogleOAuthToken(token=" + token + ", error=" + error + ")");
        if (token != null) {
            authGoogleFirebase(token);
        } else {
            mFirebase.unauth();
            mCachedUserProfile = null;
            mTransactions = new ArrayList<>();
            updateIdentityUi();
            updateTransactionsUi();
        }
    }

    private void authGoogleFirebase(String token) {
        mFirebase.authWithOAuthToken("google", token, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                // the Google user is now authenticated with Firebase
                Log.d(TAG, "Google user authenticated: " + authData.getUid());
                Map<String, Object> data = authData.getProviderData();
                mCachedUserProfile = (Map<String, String>) data.get("cachedUserProfile");
                updateIdentityUi();
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
