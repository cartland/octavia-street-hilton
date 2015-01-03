/*
 * Copyright 2015 Chris Cartland. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chriscartland.octaviastreethilton.ui;

import android.content.Intent;
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
import android.widget.ListView;
import android.widget.Spinner;

import com.chriscartland.octaviastreethilton.FirebaseAuthManager;
import com.chriscartland.octaviastreethilton.GoogleOAuthManager;
import com.chriscartland.octaviastreethilton.R;
import com.chriscartland.octaviastreethilton.model.Transaction;
import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.ArrayList;
import java.util.Map;


public class MainActivity extends ActionBarActivity implements
        FirebaseAuthManager.FirebaseAuthCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    private GoogleOAuthManager mGoogleOAuthManager;
    private FirebaseAuthManager mFirebaseAuthManager;

    private Firebase mFirebase;
    private ChildEventListener mTransactionListener;

    private DrawerLayout mDrawerLayout;
    private ListView mListView;
    private ListView mDrawerNavigation;

    private ArrayAdapter<CharSequence>  mSpinnerAdapter;
    private String mTransactionFilter;
    private String mRoomId;
    private ArrayList<Transaction> mTransactions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get default room ID.
        mRoomId = getString(R.string.default_room_id);

        createToolbar();
        createTransactionFilter();
        createTransactionViews();
        createDrawer();

        createFirebase();
        createAuth();
        startFirebase();
        startAuth();

        mGoogleOAuthManager.signIn();

        updateTransactionFilter();
        updateAuthDependentListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateAuthDependentListeners();
    }

    private void createToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.color_primary));
        setSupportActionBar(toolbar);
    }

    private void createTransactionFilter() {
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
    }

    private void createTransactionViews() {
        mTransactions = new ArrayList<>();
        mListView = (ListView) findViewById(R.id.transaction_list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Transaction clicked: " + position);
                Intent intent = new Intent(MainActivity.this, TransactionActivity.class);
                intent.putExtra(Transaction.EXTRA, (Transaction) parent.getItemAtPosition(position));
                startActivity(intent);
            }
        });
    }

    private void createDrawer() {
        // Now retrieve the DrawerLayout so that we can set the status bar color.
        // This only takes effect on Lollipop, or when using translucentStatusBar
        // on KitKat.
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_main);
        drawerLayout.setStatusBarBackgroundColor(getResources()
                .getColor(R.color.color_primary_dark));

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_main);
        ArrayAdapter drawerAdapter = ArrayAdapter.createFromResource(this,
                R.array.navigation_array, R.layout.drawer_list_item);
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
    }

    private void createFirebase() {
        Firebase.setAndroidContext(this);
        mFirebase = new Firebase(getString(R.string.firebase_url));

        mTransactionListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "childS: " + s);
                mTransactions.add(0, Transaction.newFromSnapshot(dataSnapshot));
                updateTransactionsUi();

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "childS: " + s);
                mTransactions.add(0, Transaction.newFromSnapshot(dataSnapshot));
                updateTransactionsUi();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Transaction t = Transaction.newFromSnapshot(dataSnapshot);
                mTransactions.remove(t);
                updateTransactionsUi();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "childS: " + s);
                mTransactions.add(0, Transaction.newFromSnapshot(dataSnapshot));
                updateTransactionsUi();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.d(TAG, "transaction event canceled: " + firebaseError);
            }
        };
    }

    private void createAuth() {
        if (mFirebaseAuthManager == null) {
            mFirebaseAuthManager = new FirebaseAuthManager(mFirebase);
        }

        if (mGoogleOAuthManager == null) {
            mGoogleOAuthManager = new GoogleOAuthManager();
        }
    }

    private void startFirebase() {
        Firebase.setAndroidContext(this);
    }

    private void startAuth() {
        mFirebaseAuthManager.setCallback(this);
        mGoogleOAuthManager.setActivity(this);
        mGoogleOAuthManager.setCallback(mFirebaseAuthManager);
        mGoogleOAuthManager.start();
    }

    private void updateTransactionFilter() {
        if (mTransactionFilter == null) {
            mTransactionFilter = mSpinnerAdapter.getItem(0).toString();
        }
        // TODO(cartland): Update the transaction filter.
    }

    private void updateTransactionsUi() {
        TransactionArrayAdapter adapter = new TransactionArrayAdapter(this, mTransactions);
        mListView.setAdapter(adapter);
    }

    private void updateAuthDependentListeners() {
        // Firebase does not call value event listeners when the auth state changes.
        // In order for our event listeners to get data based on new auth information,
        // we must remove the event listener and add it again every time we detect that
        // the auth state has changed.

        mFirebase.child("transactions").child(mRoomId).removeEventListener(mTransactionListener);
        mFirebase.child("transactions").child(mRoomId).orderByKey().addChildEventListener(mTransactionListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Don't forget to call GoogleOAuthManager.onActivityResult()
        mGoogleOAuthManager.onActivityResult(requestCode, resultCode, data);
    }

    // Implement interface.
    @Override
    public void onReceivedFirebaseAuth(AuthData authData, FirebaseError error) {
        if (error != null) {
            mTransactions = new ArrayList<>();
            mGoogleOAuthManager.updateIdentityUi(null);
        } else {
            Map<String, Object> data = authData.getProviderData();
            Map<String, String> userProfile = (Map<String, String>) data.get("cachedUserProfile");
            mGoogleOAuthManager.updateIdentityUi(userProfile);
        }
        updateAuthDependentListeners();
        updateTransactionsUi();
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
