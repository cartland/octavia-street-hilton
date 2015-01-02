package com.chriscartland.octaviastreethilton;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.firebase.client.DataSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A transaction in the database.
 */
public class Transaction implements Parcelable {

    private static final String TAG = Transaction.class.getSimpleName();
    public static final String EXTRA = "com.chriscartland.octaviastreethilton.TRANSACTION_EXTRA";

    private String date;
    private String amount;
    private String purchaser;
    private String description;
    private String notes;
    private List debts;

    private Transaction(String date, String amount, String purchaser, String description,
                        String notes, List debts) {
        this.date = date;
        this.amount = amount;
        this.purchaser = purchaser;
        this.description = description;
        this.notes = notes;
        this.debts = debts;
    }

    public static Transaction newFromSnapshot(DataSnapshot dataSnapshot) {
        Transaction.Builder builder = new Builder();

        for (DataSnapshot child : dataSnapshot.getChildren()) {
            String key = child.getKey();
            String value = child.getValue().toString();
            switch (key) {
                case "date":
                    builder.setDate(value);
                    break;
                case "amount":
                    builder.setAmount(value);
                    break;
                case "purchaser":
                    builder.setPurchaser(value);
                    break;
                case "description":
                    builder.setDescription(value);
                    break;
                case "notes":
                    builder.setNotes(value);
                    break;
                case "debts":
                    List<Debt> debts = parseDebtsFromSnapshot(child);
                    builder.setDebts(debts);
                    break;
                default:
                    // Do nothing
                    break;
            }
        }
        return builder.build();
    }

    private static List<Debt> parseDebtsFromSnapshot(DataSnapshot value) {
        ArrayList<Debt> result = new ArrayList<>();
        for (DataSnapshot debtData : value.getChildren()) {
            Debt.Builder builder = new Debt.Builder();
            for (DataSnapshot debtField : debtData.getChildren()) {
                switch (debtField.getKey()) {
                    case "amount":
                        builder.setAmount(debtField.getValue().toString());
                        break;
                    case "debtor":
                        builder.setDebtor(debtField.getValue().toString());
                        break;
                    default:
                        Log.e(TAG, "Unknown field in debt: " + debtField.getKey());
                        break;
                }
            }
            result.add(builder.build());
        }
        return result;
    }

    public static class Builder {

        private String date;
        private String amount;
        private String purchaser;
        private String description;
        private String notes;
        private List<Debt> debts;

        public Builder() {}

        public Transaction build() {
            return new Transaction(date, amount, purchaser, description, notes, debts);
        }

        public void setDate(String date) {
            this.date = date;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public void setPurchaser(String purchaser) {
            this.purchaser =  purchaser;
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

    }

    @Override
    public String toString() {
        return date + " " + amount + " " + purchaser + " " + description + " " + notes + " " + debts;
    }

    // Implement Parcelable.

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
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
                    Transaction.Builder builder = new Builder();
                    builder.setDate(source.readString());
                    builder.setAmount(source.readString());
                    builder.setPurchaser(source.readString());
                    builder.setDescription(source.readString());
                    builder.setNotes(source.readString());
                    ArrayList<Debt> debtArray = new ArrayList<>();
                    source.readList(debtArray, Debt.class.getClassLoader());
                    builder.setDebts(debtArray);
                    return builder.build();
                }

                @Override
                public Transaction[] newArray(int size) {
                    return new Transaction[size];
                }
            };
}
