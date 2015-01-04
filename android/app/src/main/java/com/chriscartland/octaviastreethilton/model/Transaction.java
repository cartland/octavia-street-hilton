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

package com.chriscartland.octaviastreethilton.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.firebase.client.DataSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A transaction in the database.
 */
public class Transaction implements Parcelable, Comparable<Transaction> {

    private static final String TAG = Transaction.class.getSimpleName();
    public static final String EXTRA = "com.chriscartland.octaviastreethilton.TRANSACTION_EXTRA";
    public static final String KEY_DATE = "date";
    public static final String KEY_AMOUNT = "amount";
    public static final String KEY_PURCHASER = "purchaser";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_NOTES = "notes";
    public static final String KEY_DEBTS = "debts";

    private String id;
    private String date;
    private String amount;
    private String purchaser;
    private String description;
    private String notes;
    private List<Debt> debts;

    public Transaction() {}

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getAmount() {
        return amount;
    }

    public String getPurchaser() {
        return purchaser;
    }

    public String getDescription() {
        return description;
    }

    public String getNotes() {
        return notes;
    }

    public List<Debt> getDebts() {
        return debts;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public void setPurchaser(String purchaser) {
        this.purchaser = purchaser;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setDebts(List<Debt> debts) {
        this.debts = debts;
    }

    public static Transaction newFromSnapshot(DataSnapshot dataSnapshot) {
        Transaction transaction = new Transaction();

        String id = dataSnapshot.getKey();
        transaction.setId(id);
        for (DataSnapshot child : dataSnapshot.getChildren()) {
            transaction.updateFieldInSnapshot(child);
        }
        return transaction;
    }

    public void updateFieldInSnapshot(DataSnapshot dataSnapshot) {
        String value = dataSnapshot.getValue().toString();
        switch (dataSnapshot.getKey()) {
            case Transaction.KEY_DATE:
                setDate(value);
                break;
            case Transaction.KEY_AMOUNT:
                setAmount(value);
                break;
            case Transaction.KEY_PURCHASER:
                setPurchaser(value);
                break;
            case Transaction.KEY_DESCRIPTION:
                setDescription(value);
                break;
            case Transaction.KEY_NOTES:
                setNotes(value);
                break;
            case Transaction.KEY_DEBTS:
                setDebts(parseDebtsFromSnapshot(dataSnapshot));
                break;
            default:
                Log.e(TAG, "Unknown field in transaction: " + dataSnapshot.getKey());
                break;
        }
    }

    private static List<Debt> parseDebtsFromSnapshot(DataSnapshot value) {
        ArrayList<Debt> result = new ArrayList<>();
        for (DataSnapshot debtData : value.getChildren()) {
            Debt debt = Debt.newFromSnapshot(debtData);
            result.add(debt);
        }
        return result;
    }

    @Override
    public String toString() {
        return id + " " + date + " " + amount + " " + purchaser + " " + description
                + " " + notes + " " + debts;
    }

    // Implement Parcelable.

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(date);
        dest.writeString(amount);
        dest.writeString(purchaser);
        dest.writeString(description);
        dest.writeString(notes);
        dest.writeArray(debts.toArray());
    }

    public static final Creator<Transaction> CREATOR =
            new Creator<Transaction>() {
                @Override
                public Transaction createFromParcel(Parcel source) {
                    Transaction transaction = new Transaction();
                    transaction.setId(source.readString());
                    transaction.setDate(source.readString());
                    transaction.setAmount(source.readString());
                    transaction.setPurchaser(source.readString());
                    transaction.setDescription(source.readString());
                    transaction.setNotes(source.readString());
                    ArrayList<Debt> debtArray = new ArrayList<>();
                    source.readList(debtArray, Debt.class.getClassLoader());
                    transaction.setDebts(debtArray);
                    return transaction;
                }

                @Override
                public Transaction[] newArray(int size) {
                    return new Transaction[size];
                }
            };


    // Implements Comparable and consistent with equals.

    @Override
    public int compareTo(Transaction another) {
        if (this.equals(another)) {
            // Two Transaction objects with the same ID are always "equal" even if the other
            // values are different.
            return 0;
        }
        if (another == null) {
            return 1;
        }
        int result = date.compareTo(another.date);
        if (result != 0) {
            return result;
        }
        result = amount.compareTo(another.amount);
        if (result != 0) {
            return result;
        }
        result = purchaser.compareTo(another.purchaser);
        if (result != 0) {
            return result;
        }
        result = description.compareTo(another.description);
        if (result != 0) {
            return result;
        }
        result = notes.compareTo(another.notes);
        if (result != 0) {
            return result;
        }
        // If all the values are equal, then compare the IDs.
        // This statement should never return 0.
        return id.compareTo(another.id);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof Transaction)) {
            return false;
        }
        Transaction t = (Transaction)o;
        return id.equals(t.id);
    }
}
