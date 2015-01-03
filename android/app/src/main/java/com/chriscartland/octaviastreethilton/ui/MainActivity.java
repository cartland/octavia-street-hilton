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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.chriscartland.octaviastreethilton.Application;
import com.chriscartland.octaviastreethilton.R;
import com.chriscartland.octaviastreethilton.auth.AuthManager;
import com.chriscartland.octaviastreethilton.model.Auth;
import com.chriscartland.octaviastreethilton.model.Transaction;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity implements
        AuthManager.AuthCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Firebase mFirebase;
    private ChildEventListener mTransactionListener;

    private DrawerLayout mDrawerLayout;
    private ListView mListView;
    private ListView mDrawerNavigation;

    private ArrayAdapter<CharSequence>  mSpinnerAdapter;
    private String mTransactionFilter;
    private String mRoomId;
    private ArrayList<Transaction> mTransactions;
    private Auth mAuth;
    private Error mError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AuthManager.onCreate(this); // setContentView must be called before AuthManager.onCreate()
        mFirebase = ((Application) getApplication()).getFirebase();

        // Get default room ID.
        mRoomId = getString(R.string.default_room_id);

        createToolbar();
        createTransactionFilter();
        createTransactionViews();
        createDrawer();

        createFirebase();

        updateTransactionFilter();
        updateAuthDependentListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        AuthManager.onStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AuthManager.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AuthManager.onPause(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AuthManager.onStop(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AuthManager.onDestroy(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        AuthManager.onActivityResult(this, requestCode, resultCode, data);
    }

    /* AuthManager.AuthCallback implementation. */
    @Override
    public void onAuthResult(Auth auth, Error error) {
        if (((mAuth == null) != (auth == null)) || ((mError == null) != (error == null))) {
            updateAuthDependentListeners();
        }
        mAuth = auth;
        mError = error;
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
        mTransactionListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                mTransactions.add(0, Transaction.newFromSnapshot(dataSnapshot));
                updateTransactionsUi();

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
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
                mTransactions.add(0, Transaction.newFromSnapshot(dataSnapshot));
                updateTransactionsUi();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.d(TAG, "transaction event canceled: " + firebaseError);
                mTransactions = new ArrayList<>();
                updateTransactionsUi();
            }
        };
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
        Log.d(TAG, "updateAuthDependentListeners()");
        // Firebase does not call value event listeners when the auth state changes.
        // In order for our event listeners to get data based on new auth information,
        // we must remove the event listener and add it again every time we detect that
        // the auth state has changed.

        mFirebase.child("transactions").child(mRoomId).removeEventListener(mTransactionListener);
        mFirebase.child("transactions").child(mRoomId).orderByKey().addChildEventListener(mTransactionListener);
    }
}
