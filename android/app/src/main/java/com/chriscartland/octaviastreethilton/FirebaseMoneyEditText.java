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

import android.content.Context;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;

import java.text.NumberFormat;

/**
 * Automatically saves text to Firebase after being edited by the user.
 */
public class FirebaseMoneyEditText extends FirebaseEditText {

    private static final String TAG = FirebaseMoneyEditText.class.getSimpleName();

    protected TextWatcher mTextWatcher;

    public FirebaseMoneyEditText(Context context) {
        super(context);
    }

    public FirebaseMoneyEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FirebaseMoneyEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void init() {
        mTextWatcher = new FirebaseTextWatcher() {
            private String current = "";

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!s.toString().equals(current)){
                    removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[$,.]", "");

                    try {
                        double parsed = Double.parseDouble(cleanString);
                        String formatted = NumberFormat.getCurrencyInstance().format((parsed / 100));

                        current = formatted;
                        setText(formatted);
                        setSelection(formatted.length());
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Could not parse double from string: " + cleanString);
                    }

                    addTextChangedListener(this);
                }
            }
        };

        this.addTextChangedListener(this.mTextWatcher);
    }
}
