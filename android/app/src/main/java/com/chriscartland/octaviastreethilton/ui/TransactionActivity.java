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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.chriscartland.octaviastreethilton.R;
import com.chriscartland.octaviastreethilton.auth.AuthManager;
import com.chriscartland.octaviastreethilton.model.Auth;
import com.chriscartland.octaviastreethilton.model.Transaction;

/**
 * View for viewing and editing transactions.
 */
public class TransactionActivity extends ActionBarActivity implements
        AuthManager.AuthCallback {

    private static final String TAG = TransactionActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        createToolbar();

        Transaction transaction = getIntent().getParcelableExtra(Transaction.EXTRA);
        TextView view = (TextView) findViewById(R.id.transaction);
        view.setText(transaction.toString());

        AuthManager.onCreate(this); // setContentView must be called before AuthManager.onCreate()
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
        AuthManager.getInstance(this).onActivityResult(requestCode, resultCode, data);
    }

    /* AuthManager.AuthCallback implementation. */
    @Override
    public void onAuthResult(Auth auth, Error error) {
        Log.d(TAG, "onAuthResult(auth=" + auth + ", error=" + error + ")");
    }

    private void createToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.color_primary));
        setSupportActionBar(toolbar);
    }
}
