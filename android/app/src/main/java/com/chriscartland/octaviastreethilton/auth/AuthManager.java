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

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.chriscartland.octaviastreethilton.Application;
import com.chriscartland.octaviastreethilton.R;
import com.chriscartland.octaviastreethilton.model.Auth;
import com.chriscartland.octaviastreethilton.model.User;
import com.chriscartland.octaviastreethilton.ui.AuthUi;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.common.SignInButton;

import java.util.Map;

/**
 * AuthManager is a singleton class that handles authentication using Google Sign-In and Firebase.
 * Each activity that needs to be auth-aware should call the static methods corresponding to
 * the Android Activity lifecycle.
 *      AuthManager.onCreate(Activity)
 *      AuthManager.onStart(Activity)
 *      AuthManager.onResume(Activity)
 *      AuthManager.onPause(Activity)
 *      AuthManager.onStop(Activity)
 *      AuthManager.onDestroy(Activity)
 *
 * Each activity must pass activity results to AuthManager.
 *      AuthManager.onActivityResult(Activity, int requestCode, int resultCode, Intent data)
 *
 * Finally, each Activity must implement AuthManager.AuthCallback
 */
public class AuthManager implements FirebaseAuthManager.FirebaseAuthCallback {

    private static final String TAG = AuthManager.class.getSimpleName();

    private Firebase mFirebase;
    private Activity mActivity;
    private Auth mAuth;
    private Error mError;
    private AuthCallback mAuthCallback;
    private AuthUserInterface mUserInterface;
    private FirebaseAuthManager mFirebaseAuthManager;

    private GoogleOAuthManager mGoogleOAuthManager;

    public interface AuthCallback {
        /**
         * Called when Firebase and Google have authenticated the user, or on failure.
         * Called when auth data exists on update, onResume(), and when the callback is set.
         *
         * @param auth Basic user information and auth information from Firebase and Google.
         * @param error Null if the user is signed in.
         */
        void onAuthResult(Auth auth, Error error);
    }

    public interface AuthUserInterface {
        void updateAuthUserInterface(Activity activity, Auth auth);
    }


    /* Singleton. */

    private static AuthManager INSTANCE;

    public static AuthManager getInstance(Activity activity) {
        if (INSTANCE == null) {
            INSTANCE = new AuthManager();
        }
        return INSTANCE;
    }

    private AuthManager() {}


    /**
     * Called whenever activity lifecycle is taking focus. This allows a sign-in flow to start
     * in one activity and receive the final AuthCallback in a different activity.
     *
     * @param activity
     */
    private void setActivity(Activity activity) {
        this.mActivity = activity;
    }


    /* Lifecycle methods. */

    public static void onCreate(final Activity activity) {
        getInstance(activity).setActivity(activity);
        getInstance(activity).onCreate();
    }

    private void onCreate() {
        /* Load the Google Sign-In button */
        SignInButton signInButton = (SignInButton) mActivity.findViewById(R.id.sign_in_with_google);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthManager.this.signIn();
            }
        });
        /* Sign out button */
        Button signOutButton = (Button) mActivity.findViewById(R.id.sign_out);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthManager.this.signOut();
            }
        });
        TextView name = (TextView) mActivity.findViewById(R.id.identity_name);
        ImageView image = (ImageView) mActivity.findViewById(R.id.identity_image);
        setUserInterface(new AuthUi(R.id.sign_in_with_google, R.id.sign_out, R.id.identity_name, R.id.identity_image));

        mFirebase = ((Application) mActivity.getApplication()).getFirebase();
        setFirebaseAuthManager(new FirebaseAuthManager(mFirebase));
        setGoogleOAuthManager(new GoogleOAuthManager());
    }

    public static void onStart(Activity activity) {
        getInstance(activity).setActivity(activity);
        getInstance(activity).onStart();
    }

    private void onStart() {}

    public static void onResume(Activity activity) {
        getInstance(activity).setActivity(activity);
        getInstance(activity).onResume();
    }

    private void onResume() {
        AuthCallback callback = null;
        try {
            callback = (AuthCallback) mActivity;
        } catch (ClassCastException e) {
            Log.e(TAG, "Activity must implement AuthCallback interface");
            throw e;
        }
        setAuthCallback(callback); // Calls AuthCallback.
        updateUserInterfaceCallback();

        mFirebaseAuthManager.setCallback(this);
        mGoogleOAuthManager.setActivity(mActivity);
        mGoogleOAuthManager.setCallback(mFirebaseAuthManager);
        mGoogleOAuthManager.start();
    }

    public static void onPause(Activity activity) {
        getInstance(activity).onPause();
    }

    private void onPause() {}

    public static void onStop(Activity activity) {
        getInstance(activity).onStop();
    }

    private void onStop() {}

    public static void onDestroy(Activity activity) {
        getInstance(activity).onDestroy();
    }

    private void onDestroy() {}

    public static void onActivityResult(Activity activity, int requestCode, int resultCode,
                                        Intent data) {
        getInstance(activity).onActivityResult(requestCode, resultCode, data);
    }

    private void onActivityResult(int requestCode, int resultCode, Intent data) {
        mGoogleOAuthManager.onActivityResult(requestCode, resultCode, data);
    }


    /* Public methods */

    /**
     * Programmatically trigger sign-in.
     */
    public void signIn() {
        mGoogleOAuthManager.signIn();
    }

    /**
     * Programmatically trigger sign-out.
     */
    public void signOut() {
        mGoogleOAuthManager.signOut();
    }

    /**
     * By default, this is an Activity.
     *
     * @param callback Receives updates when Auth information changes.
     */
    private void setAuthCallback(AuthCallback callback) {
        mAuthCallback = callback;
        maybeUpdateAuthCallback();
    }

    /**
     * This user interface shows the sign-in information.
     *
     * @param callback Receives callback when UI should be updated.
     */
    private void setUserInterface(AuthUserInterface callback) {
        mUserInterface = callback;
        updateUserInterfaceCallback();
    }

    /**
     * @param mGoogleOAuthManager Handles Google Sign-In.
     */
    public void setGoogleOAuthManager(GoogleOAuthManager mGoogleOAuthManager) {
        this.mGoogleOAuthManager = mGoogleOAuthManager;
    }

    /**
     * @param mFirebaseAuthManager Handles Firebase auth using Google Sign-In.
     */
    public void setFirebaseAuthManager(FirebaseAuthManager mFirebaseAuthManager) {
        this.mFirebaseAuthManager = mFirebaseAuthManager;
    }

    /**
     * Calls AuthCallback if there is either data or an error.
     */
    private void maybeUpdateAuthCallback() {
        if (mAuthCallback != null) {
            if (mAuth != null || mError != null) {
                mAuthCallback.onAuthResult(mAuth, mError);
            }
        } else {
            Log.d(TAG, "Cannot update null auth callback");
        }
    }

    /**
     * Calls callback to update activity UI with Auth information.
     */
    private void updateUserInterfaceCallback() {
        Log.d(TAG, "updateUserInterfaceCallback()");
        if (mUserInterface != null) {
            mUserInterface.updateAuthUserInterface(mActivity, mAuth);
        } else {
            Log.d(TAG, "Cannot update null UI callback");
        }
    }


    /* FirebaseAuthManager interface. */

    /**
     * Update mAuth and mError with Firebase AuthData, then trigger callbacks.
     *
     * @param authData Data from Firebase authentication.
     * @param error
     */
    @Override
    public void onReceivedFirebaseAuth(AuthData authData, FirebaseError error) {
        if (error != null) {
            mAuth = null;
            mError = new Error(error.toString());
        } else {
            Map<String, Object> data = authData.getProviderData();
            Map<String, String> userProfile = (Map<String, String>) data.get("cachedUserProfile");

            User.Builder userBuilder = new User.Builder();
            userBuilder.setId(authData.getUid());
            userBuilder.setName(userProfile.get("name"));
            userBuilder.setImage(userProfile.get("picture"));

            User user = userBuilder.build();
            Log.d(TAG, "onReceivedFirebaseAuth() found User=" + user);
            mAuth = new Auth(user, authData);
            mError = null;
        }
        updateUserInterfaceCallback();
        maybeUpdateAuthCallback();
    }
}
