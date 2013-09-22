/*
 * Copyright (C) 2010-2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class MSISDNEditPreference extends EditTextPreference {

    private static final String LOG_TAG = "MSISDNListPreference";
    public static final String PHONE_NUMBER = "phone_number";

    private final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private MyHandler mHandler = new MyHandler();

    private Phone mPhone;
    private Context mContext;

    private TimeConsumingPreferenceListener mTcpListener;

    public MSISDNEditPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPhone = PhoneFactory.getDefaultPhone();
        mContext = context;
    }

    public MSISDNEditPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            String alphaTag = mPhone.getLine1AlphaTag();
            if (TextUtils.isEmpty(alphaTag)) {
                // No tag, set it.
                alphaTag = getContext().getString(R.string.msisdn_alpha_tag);
            }

            mPhone.setLine1Number(alphaTag, getText(),
                    mHandler.obtainMessage(MyHandler.MESSAGE_SET_MSISDN));
            if (mTcpListener != null) {
                mTcpListener.onStarted(this, false);
            }

            // Save the number
            SharedPreferences prefs = getSharedPreferences();
            Editor editor = prefs.edit();

            String phoneNum = getText().trim();
            String savedNum = prefs.getString(PHONE_NUMBER, null);

            // If there is no string, treat it as null
            if (phoneNum.length() == 0) {
                phoneNum = null;
            }

            if (phoneNum == null && savedNum == null) {
                Log.d(LOG_TAG, "No phone number set yet");
            } else if (phoneNum == null && savedNum != null) {
                /* Remove saved number only if there is some saved and
                there is no number set */
                if (DBG) {
                    Log.d(LOG_TAG, "Removing phone number");
                }

                editor.remove(PHONE_NUMBER);
                editor.commit();
            } else if (!TextUtils.equals(phoneNum, savedNum)) {
                /* Save phone number only if there is some number set and
                   it is not equal to the already saved one */
                if (DBG) {
                    Log.d(LOG_TAG, "Saving phone number: " + phoneNum);
                }

                editor.putString(PHONE_NUMBER, phoneNum);
                editor.commit();
            } else if (DBG) {
                Log.d(LOG_TAG, "No change");
            }
        }
    }

    void init(TimeConsumingPreferenceListener listener, boolean skipReading) {
        mTcpListener = listener;
        if (!skipReading) {
            setText(mPhone.getLine1Number());
        }
    }

    private class MyHandler extends Handler {
        private static final int MESSAGE_SET_MSISDN = 0;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SET_MSISDN:
                    handleSetMSISDNResponse(msg);
                    break;
            }
        }

        private void handleSetMSISDNResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception != null) {
                if (DBG) {
                    Log.d(LOG_TAG, "handleSetMSISDNResponse: ar.exception=" + ar.exception);
                }
                // setEnabled(false);
            }
            if (DBG) {
                Log.d(LOG_TAG, "handleSetMSISDNResponse: re get");
            }

            mTcpListener.onFinished(MSISDNEditPreference.this, false);
        }
    }
}
