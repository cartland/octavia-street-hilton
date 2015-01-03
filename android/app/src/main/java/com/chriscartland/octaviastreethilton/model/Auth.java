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

package com.chriscartland.octaviastreethilton.model;

import android.text.TextUtils;

import com.firebase.client.AuthData;

/**
 * Auth information.
 */
public class Auth {

    private User user;
    private AuthData authData;

    public Auth(User user, AuthData authData) {
        this.user = user;
        this.authData = authData;
    }

    public User getUser() {
        return user;
    }

    public AuthData getAuthData() {
        return authData;
    }

    @Override
    public String toString() {
        return "Auth(User=" + user + ", auth=" + authData + ")";
    }
}
