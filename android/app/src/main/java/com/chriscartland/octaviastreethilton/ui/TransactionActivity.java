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

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.chriscartland.octaviastreethilton.auth.FirebaseAuthManager;
import com.chriscartland.octaviastreethilton.auth.GoogleOAuthManager;
import com.chriscartland.octaviastreethilton.R;
import com.chriscartland.octaviastreethilton.model.Transaction;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.Map;

/**
 * View for viewing and editing transactions.
 */
public class TransactionActivity extends ActionBarActivity implements
        GoogleOAuthManager.GoogleOAuthManagerCallback, FirebaseAuthManager.FirebaseAuthCallback {

    private static final String TAG = TransactionActivity.class.getSimpleName();

    private Firebase mFirebase;
    private GoogleOAuthManager mGoogleOAuthManager;
    private FirebaseAuthManager mFirebaseAuthManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        setupToolbar();

        Transaction transaction = getIntent().getParcelableExtra(Transaction.EXTRA);
        TextView view = (TextView) findViewById(R.id.transaction);
        view.setText(transaction.toString());

        setupFirebase();
        setupAuth();
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.color_primary));
        setSupportActionBar(toolbar);
    }

    private void setupFirebase() {
        Firebase.setAndroidContext(this);
        mFirebase = new Firebase(getString(R.string.firebase_url));
    }

    private void setupAuth() {
        mFirebaseAuthManager = new FirebaseAuthManager(mFirebase);
        mFirebaseAuthManager.setCallback(this);

        mGoogleOAuthManager = new GoogleOAuthManager();
        mGoogleOAuthManager.setActivity(this);
        mGoogleOAuthManager.setCallback(mFirebaseAuthManager);
        mGoogleOAuthManager.start();
    }

    @Override
    public void onReceivedGoogleOAuthToken(String token, String error) {
        String logToken = token;
        if (logToken != null) {
            logToken = logToken.substring(0, 10);
        }
        Log.d(TAG, "onReceivedGoogleOAuthToken(token=" + logToken + "..., error="
                + error + ")");
    }

    // Implement interface.
    @Override
    public void onReceivedFirebaseAuth(AuthData authData, FirebaseError error) {
//        if (error != null) {
//            mGoogleOAuthManager.updateIdentityUi(null);
//        } else {
//            Map<String, Object> data = authData.getProviderData();
//            Map<String, String> userProfile = (Map<String, String>) data.get("cachedUserProfile");
//            mGoogleOAuthManager.updateIdentityUi(userProfile);
//        }
    }

}
