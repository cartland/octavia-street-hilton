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

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.chriscartland.octaviastreethilton.R;
import com.chriscartland.octaviastreethilton.auth.AuthManager;
import com.chriscartland.octaviastreethilton.model.Auth;
import com.chriscartland.octaviastreethilton.model.User;

/**
 * Created by cartland on 1/2/15.
 */
public class AuthUi implements AuthManager.AuthUserInterface {

    private static final String TAG = AuthUi.class.getSimpleName();

    private int mGoogleSignInButtonId;
    private int mSignOutButtonId;
    private int mIdentityNameId;
    private int mIdentityImageId;

    public AuthUi(int signInButton, int signOutButton, int name, int image) {
        mGoogleSignInButtonId = signInButton;
        mSignOutButtonId = signOutButton;
        mIdentityNameId = name;
        mIdentityImageId = image;
    }

    @Override
    public void updateAuthUserInterface(Activity activity, Auth auth) {
        Log.d(TAG, "updateAuthUserInterface(auth=" + auth + ")");
        View mGoogleSignInButton = activity.findViewById(mGoogleSignInButtonId);
        View mSignOutButton = activity.findViewById(mSignOutButtonId);
        TextView mIdentityName = (TextView) activity.findViewById(mIdentityNameId);
        ImageView mIdentityImage = (ImageView) activity.findViewById(mIdentityImageId);

        String displayName;
        String image;
        User user = null;
        if (auth != null) {
            user = auth.getUser();
        }
        if (user != null) {
            displayName = user.getName();
            image = user.getImage();
            mGoogleSignInButton.setVisibility(View.GONE);
            mSignOutButton.setVisibility(View.VISIBLE);
        } else {
            displayName = null;
            image = null;
            mGoogleSignInButton.setVisibility(View.VISIBLE);
            mSignOutButton.setVisibility(View.GONE);
        }
        Log.d(TAG, "UI(displayName=" + displayName + ", image=" + image + ")");
        mIdentityName.setText(displayName);
        Glide.with(activity)
                .load(image)
                .error(R.drawable.ic_launcher)
                .into(mIdentityImage);
    }
}
