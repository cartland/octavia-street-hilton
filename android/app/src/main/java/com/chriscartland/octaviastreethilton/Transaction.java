package com.chriscartland.octaviastreethilton;

import com.firebase.client.DataSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A transaction in the database.
 */
public class Transaction {

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

    @Override
    public String toString() {
        return date + " " + amount + " " + purchaser + " " + description + " " + notes + " " + debts;
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
                    // TODO(cartland): Implement real Debt values.
                    builder.setDebts(new ArrayList<Debt>());
                    break;
                default:
                    // Do nothing
                    break;
            }
        }
        return builder.build();
    }
}
