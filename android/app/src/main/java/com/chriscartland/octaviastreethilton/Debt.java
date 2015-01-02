package com.chriscartland.octaviastreethilton;

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
