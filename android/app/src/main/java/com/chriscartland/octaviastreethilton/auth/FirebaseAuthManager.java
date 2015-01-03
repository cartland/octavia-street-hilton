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

package com.chriscartland.octaviastreethilton.auth;

import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

/**
 * Created by cartland on 1/2/15.
 */
public class FirebaseAuthManager implements GoogleOAuthManager.GoogleOAuthManagerCallback {

    private static final String TAG = FirebaseAuthManager.class.getSimpleName();

    private final Firebase mFirebase;
    private FirebaseAuthCallback mFirebaseAuthCallback;

    public interface FirebaseAuthCallback {
        void onReceivedFirebaseAuth(AuthData authData, FirebaseError error);
    }

    public FirebaseAuthManager(Firebase reference) {
        mFirebase = reference;
    }

    public void setCallback(FirebaseAuthCallback callback) {
        mFirebaseAuthCallback = callback;
    }
    
    @Override
    public void onReceivedGoogleOAuthToken(String token, String error) {
        Log.d(TAG, "onReceivedGoogleOAuthToken(token=..., error=" + error + ")");
        if (token != null) {
            authGoogleFirebase(token);
        } else {
            mFirebase.unauth();
            mFirebaseAuthCallback.onReceivedFirebaseAuth(null,
                    new FirebaseError(FirebaseError.PROVIDER_ERROR, error));
        }
    }

    private void authGoogleFirebase(String token) {
        mFirebase.authWithOAuthToken("google", token, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                // the Google user is now authenticated with Firebase
                Log.d(TAG, "Google user authenticated: " + authData.getUid());
                mFirebaseAuthCallback.onReceivedFirebaseAuth(authData, null);
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                // there was an error
                Log.d(TAG, "Firebase authentication error with Google: " + firebaseError);
                mFirebaseAuthCallback.onReceivedFirebaseAuth(null, firebaseError);
            }
        });
    }
}
