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

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import com.chriscartland.octaviastreethilton.Application;
import com.chriscartland.octaviastreethilton.R;
import com.chriscartland.octaviastreethilton.auth.AuthManager;
import com.chriscartland.octaviastreethilton.model.Auth;
import com.chriscartland.octaviastreethilton.model.Transaction;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.Calendar;

/**
 * View for viewing and editing transactions.
 */
public class TransactionActivity extends ActionBarActivity implements
        AuthManager.AuthCallback {

    private static final String TAG = TransactionActivity.class.getSimpleName();

    private Firebase mFirebase;
    private ChildEventListener mTransactionListener;
    private TextView mView;

    private String mRoomId;
    private Transaction mTransaction;
    private Firebase mTransactionReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);
        AuthManager.onCreate(this); // setContentView must be called before AuthManager.onCreate()
        mFirebase = ((Application) getApplication()).getFirebase();

        // Get default room ID.
        mRoomId = getString(R.string.default_room_id);

        createToolbar();

        mTransaction = getIntent().getParcelableExtra(Transaction.EXTRA);
        mView = (TextView) findViewById(R.id.transaction);
        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(v);
            }
        });
        createFirebase();

        updateUI();
    }

    public void showDatePickerDialog(View v) {
        DatePickerFragment newFragment = new DatePickerFragment();
        String date = mTransaction.getDate();
        if (date.length() == 10) {
            int year = Integer.parseInt(date.substring(0, 4));
            int month = Integer.parseInt(date.substring(5, 7));
            int day = Integer.parseInt(date.substring(8, 10));
            newFragment.setDate(year, month, day);
        }
        newFragment.setCallback(new DatePickerFragment.DatePickedCallback() {
            @Override
            public void datePicked(int year, int month, int day) {
                String date = String.format("%04d-%02d-%02d", year, month + 1, day);
                mTransactionReference.child(Transaction.KEY_DATE).setValue(date);
                Log.d(TAG, date);
                Log.d(TAG, mTransactionReference.child(Transaction.KEY_DATE).toString());
            }
        });
        newFragment.show(getFragmentManager(), "datePicker");
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
        Log.d(TAG, "onAuthResult(auth=" + auth + ", error=" + error + ")");
        updateAuthDependentListeners();
    }


    private void createToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.color_primary));
        setSupportActionBar(toolbar);
    }

    private void createFirebase() {
        String id = mTransaction.getId();
        mTransactionReference = mFirebase.child("transactions").child(mRoomId).child(id);
        mTransactionReference.child("date").setValue("12341289734");    

        mTransactionListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                mTransaction.updateFieldInSnapshot(dataSnapshot);
                updateUI();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "onChildChanged");
                mTransaction.updateFieldInSnapshot(dataSnapshot);
                updateUI();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved: " + dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "onChildMoved: " + dataSnapshot.getKey());
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                mTransaction = null;
                updateUI();
            }
        };
    }

    private void updateUI() {
        if (mTransaction != null) {
            mView.setText(mTransaction.toString());
        } else {
            mView.setText("");
        }
    }

    private void updateAuthDependentListeners() {
        Log.d(TAG, "updateAuthDependentListeners()");
        // Firebase does not call value event listeners when the auth state changes.
        // In order for our event listeners to get data based on new auth information,
        // we must remove the event listener and add it again every time we detect that
        // the auth state has changed.

        mTransactionReference.removeEventListener(mTransactionListener);
        mTransactionReference.addChildEventListener(mTransactionListener);
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        private int mYear = -1;
        private int mMonth = -1;
        private int mDay = -1;
        private DatePickedCallback mCallback;

        public void setDate(int year, int month, int day) {
            mYear = year;
            mMonth = month;
            mDay = day;
        }

        public void setCallback(DatePickedCallback callback) {
            mCallback = callback;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            Calendar c = Calendar.getInstance();
            int year = mYear;
            int month = mMonth - 1;
            int day = mDay;
            if (year < 0) {
                year = c.get(Calendar.YEAR);
            }
            if (month < 0) {
                month = c.get(Calendar.MONTH);
            }
            if (day < 0) {
                day = c.get(Calendar.DAY_OF_MONTH);
            }

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            if (mCallback != null) {
                mCallback.datePicked(year, month, day);
            }
        }

        public interface DatePickedCallback {
            void datePicked(int year, int month, int day);
        }
    }
}
