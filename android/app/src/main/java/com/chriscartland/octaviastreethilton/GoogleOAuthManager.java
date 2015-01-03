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

package com.chriscartland.octaviastreethilton;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import java.io.IOException;
import java.util.Map;

public class GoogleOAuthManager implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = GoogleOAuthManager.class.getSimpleName();

    private Activity mActivity;

    private GoogleOAuthManagerCallback mCallback;

    private static final int RC_GOOGLE_SIGN_IN = 1;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* A flag indicating that a PendingIntent is in progress and prevents us from starting further intents. */
    private boolean mGoogleIntentInProgress;

    /* Track whether the sign-in button has been clicked so that we know to resolve all issues preventing sign-in
     * without waiting. */
    private boolean mGoogleSignInClicked;

    /* Store the connection result from onConnectionFailed callbacks so that we can resolve them when the user clicks
     * sign-in. */
    private ConnectionResult mGoogleConnectionResult;

    /* Drawer widgets. */
    private SignInButton mGoogleSignInButton;
    private Button mSignOutButton;
    private ImageView mIdentityImage;
    private TextView mIdentityName;

    public interface GoogleOAuthManagerCallback {
        void onReceivedGoogleOAuthToken(String token, String error);
    }

    public void setActivity(Activity activity) {
        Log.d(TAG, "setActivity(activity=" + activity + ")");
        mActivity = activity;
        try {
            mCallback= (GoogleOAuthManagerCallback) activity;
        } catch (ClassCastException e) {
            Log.e(TAG, "Activity must implement required interface: " +
                    GoogleOAuthManagerCallback.class.getSimpleName());
            throw e;
        }
    }

    public void start() {
        Log.d(TAG, "start()");
        if (mActivity == null) {
            throw new IllegalStateException("GoogleOAuthManager: Must setActivity() before start()");
        }

        mIdentityImage = (ImageView) mActivity.findViewById(R.id.identity_image);
        mIdentityName = (TextView) mActivity.findViewById(R.id.identity_name);

        /* Load the Google Sign-In button */
        mGoogleSignInButton = (SignInButton) mActivity.findViewById(R.id.sign_in_with_google);
        mGoogleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GoogleOAuthManager.this.signIn();
            }
        });
        /* Sign out button */
        mSignOutButton = (Button) mActivity.findViewById(R.id.sign_out);
        mSignOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GoogleOAuthManager.this.signOut();
            }
        });
        connect();
    }

    private void connect() {
        Log.d(TAG, "connect()");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Plus.API)
                    .addScope(Plus.SCOPE_PLUS_PROFILE)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    public void signIn() {
        Log.d(TAG, "signIn()");
        mGoogleSignInClicked = true;
        if (!mGoogleApiClient.isConnecting()) {
            if (mGoogleConnectionResult != null) {
                resolveSignInError();
            } else if (mGoogleApiClient.isConnected()) {
                getGoogleOAuthTokenAndSignIn();
            } else {
                    /* connect API now */
                Log.d(TAG, "Trying to connect to Google API");
                mGoogleApiClient.connect();
            }
        }
    }

    public void signOut() {
        Log.d(TAG, "signOut()");

        mGoogleSignInClicked = false;
        mGoogleConnectionResult = null;
        mGoogleIntentInProgress = false;

        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
            mCallback.onReceivedGoogleOAuthToken(null, "Successfully signed out.");
        } else {
            mCallback.onReceivedGoogleOAuthToken(null, "Already signed out.");
        }
    }

    // Activity must call this method in onActivityResult.
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(requestCode=" + requestCode + ", resultCode=" + resultCode +
                ", data=" + data + ") {");
        if (resultCode != mActivity.RESULT_OK) {
            mGoogleSignInClicked = false;
        }

        mGoogleIntentInProgress = false;
        mGoogleConnectionResult = null;

        if (!mGoogleApiClient.isConnecting()) {
            Log.d(TAG, "!mGoogleApiClient.isConnecting()");
            Log.d(TAG, "mGoogleApiClient.connect()");
            mGoogleApiClient.connect();
        }
    }

    public void updateIdentityUi(Map<String, String> userProfile) {
        String displayName;
        String image;
        if (userProfile != null) {
            displayName = userProfile.get("name");
            image = userProfile.get("picture");
            mGoogleSignInButton.setVisibility(View.GONE);
            mSignOutButton.setVisibility(View.VISIBLE);
        } else {
            displayName = "";
            image = null;
            mGoogleSignInButton.setVisibility(View.VISIBLE);
            mSignOutButton.setVisibility(View.GONE);
        }
        Log.d(TAG, "updateIdentityUi(displayName=" + displayName + ", image=" + image + ")");
        mIdentityName.setText(displayName);
        Glide.with(mActivity)
                .load(image)
                .error(R.drawable.ic_launcher)
                .into(mIdentityImage);
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    /* A helper method to resolve the current ConnectionResult error. */
    private void resolveSignInError() {
        Log.d(TAG, "resolveSignInError()");
        if (mGoogleConnectionResult.hasResolution()) {
            Log.d(TAG, "mGoogleConnectionResult.hasResolution()==true");
            Log.d(TAG, "resolution=" + mGoogleConnectionResult);
            try {
                mGoogleIntentInProgress = true;
                mGoogleConnectionResult.startResolutionForResult(mActivity, GoogleOAuthManager.RC_GOOGLE_SIGN_IN);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                Log.d(TAG, "SendIntentException");
                mGoogleIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    private void getGoogleOAuthTokenAndSignIn() {
        Log.d(TAG, "getGoogleOAuthTokenAndSignIn()");
        /* Get OAuth token in Background */
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            String errorMessage = null;

            @Override
            protected String doInBackground(Void... params) {
                String token = null;

                try {
                    String scope = String.format("oauth2:%s", Scopes.PROFILE);
                    Log.d(TAG, "GoogleAuthUtil.getToken scope=" + scope);
                    String name = Plus.AccountApi.getAccountName(mGoogleApiClient);
                    Log.d(TAG, "getAccountName(mGoogleApiClient): " + name);
                    token = GoogleAuthUtil.getToken(mActivity, name, scope);
                } catch (IOException transientEx) {
                    /* Network or server error */
                    Log.e(TAG, "Error authenticating with Google: " + transientEx);
                    errorMessage = "Network error: " + transientEx.getMessage();
                } catch (UserRecoverableAuthException e) {
                    Log.w(TAG, "Recoverable Google OAuth error: " + e.toString());
                    /* We probably need to ask for permissions, so start the intent if there is none pending */
                    if (!mGoogleIntentInProgress) {
                        mGoogleIntentInProgress = true;
                        Intent recover = e.getIntent();
                        mActivity.startActivityForResult(recover, GoogleOAuthManager.RC_GOOGLE_SIGN_IN);
                    }
                } catch (GoogleAuthException authEx) {
                    /* The call is not ever expected to succeed assuming you have already verified that
                     * Google Play services is installed. */
                    Log.e(TAG, "Error authenticating with Google: " + authEx.getMessage(), authEx);
                    errorMessage = "Error authenticating with Google: " + authEx.getMessage();
                }

                return token;
            }

            @Override
            protected void onPostExecute(String token) {
                mGoogleSignInClicked = false;
                if (token != null) {
                    Log.d(TAG, "Google Auth Successful");
                    mCallback.onReceivedGoogleOAuthToken(token, null);
                } else if (errorMessage != null) {
                    Log.d(TAG, "Google Auth Failed: " + errorMessage);
                    mCallback.onReceivedGoogleOAuthToken(null, errorMessage);
                }
            }
        };
        task.execute();
    }

    @Override
    public void onConnected(final Bundle bundle) {
        Log.d(TAG, "onConnected()");
        /* Connected with Google API, use this to authenticate with Firebase */
        getGoogleOAuthTokenAndSignIn();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended(i=" + i + ")");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "onConnectedionSuspended()");
        if (!mGoogleIntentInProgress) {
            /* Store the ConnectionResult so that we can use it later when the user clicks on the Google+ Sign-In button */
            mGoogleConnectionResult = result;

            if (mGoogleSignInClicked) {
                /* The user has already clicked the sign-in button so we attempt to resolve all errors until the user is signed in,
                 * or they cancel. */
                resolveSignInError();
            } else {
                Log.e(TAG, result.toString());
            }
        }
    }
}