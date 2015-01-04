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

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.chriscartland.octaviastreethilton.R;
import com.chriscartland.octaviastreethilton.model.Debt;
import com.chriscartland.octaviastreethilton.model.Transaction;

import java.util.List;

/**
 * Adapter for Transaction list view.
 */
public class TransactionArrayAdapter extends ArrayAdapter<Transaction> {

    public TransactionArrayAdapter(Context context, List<Transaction>transactions) {
        super(context, 0, transactions);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Transaction transaction = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.transaction_list_item, parent, false);
        }
        TextView date = (TextView) convertView.findViewById(R.id.transaction_date);
        TextView amount = (TextView) convertView.findViewById(R.id.transaction_amount);
        TextView purchaser = (TextView) convertView.findViewById(R.id.transaction_purchaser);
        TextView description = (TextView) convertView.findViewById(R.id.transaction_description);
        TextView debts = (TextView) convertView.findViewById(R.id.transaction_debts);
        date.setText(transaction.getDate());
        amount.setText(transaction.getAmount());
        purchaser.setText(transaction.getPurchaser());
        description.setText(transaction.getDescription());

        StringBuilder s = new StringBuilder();
        for (Debt debt : transaction.getDebts()) {
            if (!TextUtils.isEmpty(debt.getAmount())) {
                s.append(debt.getDebtor());
                s.append(": ");
                s.append(debt.getAmount());
                s.append("  ");
            }
        }
        debts.setText(s.toString());
        return convertView;
    }
}
