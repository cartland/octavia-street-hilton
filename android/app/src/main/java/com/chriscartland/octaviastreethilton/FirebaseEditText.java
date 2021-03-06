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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;

import com.firebase.client.Firebase;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Automatically saves text to Firebase after being edited by the user.
 */
public class FirebaseEditText extends EditText {

    private static final String TAG = FirebaseEditText.class.getSimpleName();

    protected static final long SAVE_TIME_DELAY_MS = 1000;

    protected Firebase mRef;
    protected Timer mClickTrackingTimer;
    protected int mSelStart;
    protected int mSelEnd;
    protected TextWatcher mTextWatcher;
    protected boolean mNoSave;

    public FirebaseEditText(Context context) {
        super(context);
        init();
    }

    public FirebaseEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FirebaseEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (mNoSave) {
            // Do not change the cursor selection when the text is programatically changed.
            try {
                setSelection(mSelStart, Math.min(mSelEnd, this.length()));
            } catch (IndexOutOfBoundsException e) {
                // TODO(cartland): Properly compute the selection bounds.
            }
        } else {
            mSelStart = selStart;
            mSelEnd = selEnd;
        }
    }

    protected void init() {
        mTextWatcher = new FirebaseTextWatcher();

        this.addTextChangedListener(mTextWatcher);
    }

    public void setFirebase(Firebase mRef) {
        this.mRef = mRef;
    }

    /**
     * Used instead of EditText.setText() in order to ensure that there isn't an infinite loop
     * when syncing text with Firebase.
     */
    public void setTextWithoutSaving(CharSequence text) {
        Log.d(TAG, "setTextWithoutSaving()");
        mNoSave = true;
        this.setText(text);
    }

    protected void delaySaveText() {
        Log.d(TAG, "delaySaveText()");
        if (null != mClickTrackingTimer) {
            mClickTrackingTimer.cancel();
        }
        mClickTrackingTimer = new Timer();
        mClickTrackingTimer.schedule(new SaveAfterDelayTask(), SAVE_TIME_DELAY_MS);
    }

    protected class SaveAfterDelayTask extends TimerTask {
        @Override
        public void run() {
            save();
        }
    }

    protected void save() {
        Log.d(TAG, "save()");
        if (mRef != null) {
            mRef.setValue(this.getText().toString());
        }
    }

    protected void maybeSaveLater() {
        if (mNoSave) {
            // Do not try to save text when is is programatically changed.
            mNoSave = false;
        } else {
            delaySaveText();
        }
    }

    protected class FirebaseTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            maybeSaveLater();
        }
    }
}
