
package com.android.phone;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.util.Log;

public class MSISDNEditPreference extends EditTextPreference {

    private static final String LOG_TAG = "MSISDNListPreference";

    private final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);

    private MyHandler mHandler = new MyHandler();

    private Phone mPhone;

    private TimeConsumingPreferenceListener tcpListener;

    public MSISDNEditPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPhone = PhoneFactory.getDefaultPhone();
    }

    public MSISDNEditPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            String alphaTag = mPhone.getLine1AlphaTag();
            if (alphaTag == null || "".equals(alphaTag)) {
                // No tag, set it.
                alphaTag = "Voice Line 1";
            }
            
            mPhone.setLine1Number(alphaTag, getText(),
                    mHandler.obtainMessage(MyHandler.MESSAGE_SET_MSISDN));
            if (tcpListener != null) {
                tcpListener.onStarted(this, false);
            }
        }
    }

    void init(TimeConsumingPreferenceListener listener, boolean skipReading) {
        tcpListener = listener;
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
                if (DBG)
                    Log.d(LOG_TAG, "handleSetMSISDNResponse: ar.exception=" + ar.exception);
                // setEnabled(false);
            }
            if (DBG)
                Log.d(LOG_TAG, "handleSetMSISDNResponse: re get");
            
            tcpListener.onFinished(MSISDNEditPreference.this, false);
        }
    }
}
