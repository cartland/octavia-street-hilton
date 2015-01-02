package com.chriscartland.octaviastreethilton;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;

/**
 * View for viewing and editing transactions.
 */
public class TransactionActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        Transaction transaction = getIntent().getParcelableExtra(Transaction.EXTRA);
        TextView view = (TextView) findViewById(R.id.transaction);
        view.setText(transaction.toString());
    }
}
