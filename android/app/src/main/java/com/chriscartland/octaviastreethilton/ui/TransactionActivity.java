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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
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

import java.text.NumberFormat;
import java.util.Calendar;

/**
 * View for viewing and editing transactions.
 */
public class TransactionActivity extends ActionBarActivity implements
        AuthManager.AuthCallback {

    private static final String TAG = TransactionActivity.class.getSimpleName();

    private Firebase mFirebase;
    private ChildEventListener mTransactionListener;

    private String mRoomId;
    private Transaction mTransaction;
    private Firebase mTransactionReference;

    private View mTransactionView;
    private TextView mDateView;
    private EditText mAmountView;
    private Spinner mPurchaserView;
    private EditText mDescriptionView;
    private EditText mNotesView;
    private ArrayAdapter<CharSequence> mSpinnerAdapter;

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

        createViews();
        createFirebase();

        updateUI();
    }

    private void createViews() {
        mTransactionView = findViewById(R.id.transaction);

        mDateView = (TextView) findViewById(R.id.transaction_date_editor);
        mDateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(v);
            }
        });

        mAmountView = (EditText) findViewById(R.id.transaction_amount_editor);
        mAmountView.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!s.toString().equals(current)){
                    mAmountView.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[$,.]", "");

                    try {
                        double parsed = Double.parseDouble(cleanString);
                        String formatted = NumberFormat.getCurrencyInstance().format((parsed / 100));

                        current = formatted;
                        mAmountView.setText(formatted);
                        mAmountView.setSelection(formatted.length());
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Could not parse double from string: " + cleanString);
                    }

                    mAmountView.addTextChangedListener(this);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String cleanString = mAmountView.getText().toString().replaceAll("[$,]", "");
                Log.d(TAG, "Update amount: " + cleanString);
                mTransactionReference.child(Transaction.KEY_AMOUNT).setValue(cleanString);
            }
        });

        mPurchaserView = (Spinner) findViewById(R.id.transaction_purchaser_editor);
        mSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.osh_members, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPurchaserView.setAdapter(mSpinnerAdapter);

        mDescriptionView = (EditText) findViewById(R.id.transaction_description_editor);
        mDescriptionView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mTransactionReference.child(Transaction.KEY_DESCRIPTION)
                            .setValue(mDescriptionView.getText().toString());
                }
            }
        });
        mDescriptionView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_DONE){
                    mTransactionReference.child(Transaction.KEY_DESCRIPTION)
                            .setValue(mDescriptionView.getText().toString());
                }
                return false;
            }
        });

        mNotesView = (EditText) findViewById(R.id.transaction_notes_editor);
        mNotesView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mTransactionReference.child(Transaction.KEY_NOTES)
                            .setValue(mNotesView.getText().toString());
                }
            }
        });
        mNotesView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_DONE){
                    mTransactionReference.child(Transaction.KEY_NOTES)
                            .setValue(mNotesView.getText().toString());
                }
                return false;
            }
        });
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
        Log.d(TAG, "updateUI()");
        if (mTransaction != null) {
            mDateView.setText(mTransaction.getDate());
            mAmountView.setText(mTransaction.getAmount());
            mPurchaserView.setSelection(mSpinnerAdapter.getPosition(mTransaction.getPurchaser()));
            mDescriptionView.setText(mTransaction.getDescription());
            mNotesView.setText(mTransaction.getNotes());
            mTransactionView.setVisibility(View.VISIBLE);
        } else {
            mTransactionView.setVisibility(View.GONE);
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
