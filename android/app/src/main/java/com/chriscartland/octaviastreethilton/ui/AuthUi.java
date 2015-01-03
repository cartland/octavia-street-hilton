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

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.chriscartland.octaviastreethilton.R;
import com.chriscartland.octaviastreethilton.auth.AuthManager;
import com.chriscartland.octaviastreethilton.model.Auth;
import com.chriscartland.octaviastreethilton.model.User;
import com.firebase.client.AuthData;
import com.google.android.gms.common.SignInButton;

import java.util.Map;

/**
 * Created by cartland on 1/2/15.
 */
public class AuthUi implements AuthManager.AuthUserInterface {

    private static final String TAG = AuthUi.class.getSimpleName();

    private Context mContext;
    private View mGoogleSignInButton;
    private View mSignOutButton;
    private TextView mIdentityName;
    private ImageView mIdentityImage;

    public AuthUi(Context context, View signInButton, View signOutButton, TextView name, ImageView image) {
        mContext = context;
        mGoogleSignInButton = signInButton;
        mSignOutButton = signOutButton;
        mIdentityName = name;
        mIdentityImage = image;
    }

    @Override
    public void updateAuthUserInterface(Auth auth) {
        Log.d(TAG, "updateAuthUserInterface(auth=" + auth + ")");
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
        Glide.with(mContext)
                .load(image)
                .error(R.drawable.ic_launcher)
                .into(mIdentityImage);
    }
}
