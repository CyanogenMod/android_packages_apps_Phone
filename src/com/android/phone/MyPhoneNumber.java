package com.android.phone;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;


public class MyPhoneNumber extends BroadcastReceiver {
    private final String LOG_TAG = "MyPhoneNumber";
    private final boolean DBG = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        TelephonyManager mTelephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        SharedPreferences prefs = context.getSharedPreferences(MyPhoneNumber.class.getPackage().getName() + "_preferences", Context.MODE_PRIVATE);

        String phoneNum = mTelephonyMgr.getLine1Number();
        String savedNum = prefs.getString(MSISDNEditPreference.PHONE_NUMBER, null);
        boolean airplaneModeOn = intent.getBooleanExtra("state", false);
        int simState = intent.getIntExtra("ss", -1);

        if (airplaneModeOn) {
            if (DBG)
                Log.d(LOG_TAG, "Airplane Mode On.  No modification to phone number.");
        }
        else if (phoneNum == null && 
                         (simState == 4 || simState == 6 || simState < 0) ) {
            /* If we were triggered by a SIM_STATE change, it has to be
             * 4 (SIM_READY) or 6 (RUIM_READY) */
            if (DBG)
                Log.d(LOG_TAG, "Trying to read the phone number from file");

            if (savedNum != null) {
                Phone mPhone = PhoneFactory.getDefaultPhone();
                String alphaTag = mPhone.getLine1AlphaTag();

                if (alphaTag == null || "".equals(alphaTag)) {
                    // No tag, set it.
                    alphaTag = "Voice Line 1";
                }

                mPhone.setLine1Number(alphaTag, savedNum, null);

                if (DBG)
                    Log.d(LOG_TAG, "Phone number set to: " + savedNum);
            } else if (DBG) {
                    Log.d(LOG_TAG, "No phone number set yet");
            }
        } else if (DBG) {
            Log.d(LOG_TAG, "Phone number exists. No need to read it from file.");
        }
    }
}
