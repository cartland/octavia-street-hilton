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
import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;


public class MainActivity extends ActionBarActivity implements
        AuthManager.AuthCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Firebase mFirebase;
    private ChildEventListener mTransactionListener;

    private DrawerLayout mDrawerLayout;
    private ListView mListView;
    private ListView mDrawerNavigation;

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
        createTransactionViews();
        createDrawer();

        createFirebase();

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

    private void createTransactionViews() {
        mTransactions = new ArrayList<>();
        mListView = (ListView) findViewById(R.id.transaction_list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Transaction clicked: " + position);
                Log.d(TAG, "UIDEBTS transaction before intent=" + parent.getItemAtPosition(position));
                Intent intent = new Intent(MainActivity.this, TransactionActivity.class);
                intent.putExtra(Transaction.EXTRA, (Transaction) parent.getItemAtPosition(position));
                startActivity(intent);
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToListView(mListView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "TODO(cartland): Launch activity to create transaction");

                Firebase newItem = mFirebase.child("transactions").push();
                Transaction newTransaction = new Transaction();
                newTransaction.setId(newItem.getKey());

                Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);
                String date = String.format("%04d-%02d-%02d", year, month + 1, day);
                newTransaction.setDate(date);

                newItem.setValue(newTransaction);

                Intent intent = new Intent(MainActivity.this, TransactionActivity.class);
                intent.putExtra(Transaction.EXTRA, newTransaction);
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
                Transaction newTransaction = Transaction.newFromSnapshot(dataSnapshot);
                int index = mTransactions.indexOf(newTransaction);
                mTransactions.remove(index);
                mTransactions.add(index, newTransaction);
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
                Log.d(TAG, "onChildMoved(): TODO(cartland): Implement.");
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

    private void updateTransactionsUi() {
        Collections.sort(mTransactions, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction lhs, Transaction rhs) {
                return -lhs.compareTo(rhs);
            }
        });
        TransactionArrayAdapter adapter = new TransactionArrayAdapter(this, mTransactions);
        mListView.setAdapter(adapter);
    }

    private void updateAuthDependentListeners() {
        // Firebase does not call value event listeners when the auth state changes.
        // In order for our event listeners to get data based on new auth information,
        // we must remove the event listener and add it again every time we detect that
        // the auth state has changed.

        mFirebase.child("transactions").child(mRoomId).removeEventListener(mTransactionListener);
        mTransactions = new ArrayList<>();
        mFirebase.child("transactions").child(mRoomId).orderByKey().addChildEventListener(mTransactionListener);
    }
}
