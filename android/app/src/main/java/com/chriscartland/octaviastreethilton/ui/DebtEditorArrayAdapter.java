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

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.chriscartland.octaviastreethilton.Application;
import com.chriscartland.octaviastreethilton.R;
import com.chriscartland.octaviastreethilton.model.Debt;
import com.chriscartland.octaviastreethilton.model.Transaction;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Adapter for Debt editor view.
 */
public class DebtEditorArrayAdapter extends ArrayAdapter<Debt> {

    private static final String TAG = DebtEditorArrayAdapter.class.getSimpleName();

    private final Firebase mFirebase;
    private final String mRoomId;
    private Transaction mTransaction;
    private final List<Debt> mDebts;

    private Firebase mDebtsReference;
    private EditText mAmountView;

    public DebtEditorArrayAdapter(Activity context, Transaction transaction) {
        super(context, 0, new ArrayList<Debt>());

        mTransaction = transaction;
        mDebts = mTransaction.getDebts();
        Log.d(TAG, "debts: " + mDebts);

//        List members = Arrays.asList(context.getResources().getStringArray(R.array.osh_members));
//        ArrayList<String> names = new ArrayList<String>(members);
//        for (Debt debt : transaction.getDebts()) {
//            names.remove(debt.getDebtor());
//        }
//        for (String name : names) {
//            Debt emptyDebt = new Debt();
//            emptyDebt.setDebtor(name);
//            // Add empty debt so user can create new expenses.
//            this.add(emptyDebt);
//        }
        mFirebase = ((Application) context.getApplication()).getFirebase();
        // Get default room ID.
        mRoomId = context.getString(R.string.default_room_id);

        createFirebase();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Debt debt = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.edit_debt_list_item, parent, false);
        }

        TextView name = (TextView) convertView.findViewById(R.id.debtor_name);
        name.setText(debt.getDebtor());

        mAmountView = (EditText) convertView.findViewById(R.id.debt_amount_editor);
        mAmountView.setText(debt.getAmount());


        mAmountListener = new TextWatcher() {
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
                String debtId = debt.getId();
                mDebtsReference.child(debtId).child(Debt.KEY_AMOUNT)
                        .setValue(cleanString);
            }
        });

        mAmountView.addTextChangedListener(

        return convertView;
    }

    private void createFirebase() {
        String id = mTransaction.getId();
        mDebtsReference = mFirebase.child("transactions").child(mRoomId).child(id).child("debts");

        ChildEventListener debtsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Debt debt = Debt.newFromSnapshot(dataSnapshot);
                DebtEditorArrayAdapter.this.add(debt);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "onChildChanged");
                Debt changedDebt = Debt.newFromSnapshot(dataSnapshot);
                for (Debt debt : DebtEditorArrayAdapter.this.mDebts) {
                    if (changedDebt.equals(debt)) {
                        debt.updateFieldInSnapshot(dataSnapshot);
                        DebtEditorArrayAdapter.this.notifyDataSetChanged();
                        return;
                    }
                }
                Log.e(TAG, "Could not find existing child for debt: " + changedDebt);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved: " + dataSnapshot.getKey());
                Debt toRemove = Debt.newFromSnapshot(dataSnapshot);
                DebtEditorArrayAdapter.this.remove(toRemove);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "onChildMoved: " + dataSnapshot.getKey());
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                mTransaction = null;
            }
        };
        mDebtsReference.addChildEventListener(debtsListener);
    }
}
