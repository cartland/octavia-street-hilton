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

/**
 * The part of a transaction representing a debt to the purchaser.
 */
public class Debt implements Parcelable {

    private final String amount;
    private final String debtor;

    public Debt(String amount, String debtor) {
        this.amount = amount;
        this.debtor = debtor;
    }

    public String getAmount() {
        return amount;
    }

    public String getDebtor() {
        return debtor;
    }

    public static class Builder {

        private String amount;
        private String debtor;

        public Builder() {}

        public Debt build() {
            return new Debt(amount, debtor);
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public void setDebtor(String debtor) {
            this.debtor = debtor;
        }

    }

    @Override
    public String toString() {
        return debtor + ": " + amount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(amount);
        dest.writeString(debtor);
    }

    public static final Creator<Debt> CREATOR =
            new Creator<Debt>() {
                @Override
                public Debt createFromParcel(Parcel source) {
                    String amount = source.readString();
                    String debtor = source.readString();
                    return new Debt(amount, debtor);
                }

                @Override
                public Debt[] newArray(int size) {
                    return new Debt[size];
                }
            };
}
