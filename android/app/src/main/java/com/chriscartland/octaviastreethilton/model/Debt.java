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

/**
 * The part of a transaction representing a debt to the purchaser.
 */
public class Debt implements Parcelable {

    private static final String TAG = Debt.class.getSimpleName();

    public static final String KEY_AMOUNT = "amount";
    public static final String KEY_DEBTOR = "debtor";
    private String id;
    private String amount;
    private String debtor;

    public Debt() {}

    public String getId() {
        return id;
    }

    public String getAmount() {
        return amount;
    }

    public String getDebtor() {
        return debtor;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public void setDebtor(String debtor) {
        this.debtor = debtor;
    }

    public static Debt newFromSnapshot(DataSnapshot debtData) {
        Debt debt = new Debt();
        debt.setId(debtData.getKey());
        for (DataSnapshot debtField : debtData.getChildren()) {
            debt.updateFieldInSnapshot(debtField);
        }
        return debt;
    }

    public void updateFieldInSnapshot(DataSnapshot dataSnapshot) {
        String value = dataSnapshot.getValue().toString();
        switch (dataSnapshot.getKey()) {
            case Debt.KEY_AMOUNT:
                setAmount(value);
                break;
            case Debt.KEY_DEBTOR:
                setDebtor(value);
                break;
            default:
                Log.e(TAG, "Unknown field in debt: " + dataSnapshot.getKey());
                break;
        }
    }

    @Override
    public String toString() {
        return id + "-" + debtor + ": " + amount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(amount);
        dest.writeString(debtor);
    }

    public static final Creator<Debt> CREATOR =
            new Creator<Debt>() {
                @Override
                public Debt createFromParcel(Parcel source) {
                    Debt debt = new Debt();
                    debt.setId(source.readString());
                    debt.setAmount(source.readString());
                    debt.setDebtor(source.readString());
                    return debt;
                }

                @Override
                public Debt[] newArray(int size) {
                    return new Debt[size];
                }
            };

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof Debt)) {
            return false;
        }
        Debt d = (Debt)o;
        return id.equals(d.id);
    }
}
