package com.android.phone;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.Telephony;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.util.HashSet;

/**
 * This class used to handle the blacklist data. Its
 * only remaining purpose is legacy data migration
 */
class Blacklist {
    private static class PhoneNumber implements Externalizable {
        static final long serialVersionUID = 32847013274L;
        String phone;

        public PhoneNumber() {
        }

        public void writeExternal(ObjectOutput out) throws IOException {
        }

        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            phone = (String) in.readObject();
        }

        @Override
        public int hashCode() {
            return phone != null ? phone.hashCode() : 0;
        }
    }

    private static final String BLFILE = "blacklist.dat";
    private static final int BLFILE_VER = 1;

    public static void migrateOldDataIfPresent(Context context) {
        ObjectInputStream ois = null;
        HashSet<PhoneNumber> data = null;

        try {
            ois = new ObjectInputStream(context.openFileInput(BLFILE));
            Object o = ois.readObject();
            if (o != null && o instanceof Integer) {
                // check the version
                Integer version = (Integer) o;
                if (version == BLFILE_VER) {
                    Object numbers = ois.readObject();
                    if (numbers instanceof HashSet) {
                        data = (HashSet<PhoneNumber>) numbers;
                    }
                }
            }
        } catch (IOException e) {
            // Do nothing
        } catch (ClassNotFoundException e) {
            // Do nothing
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    // Do nothing
                }
                context.deleteFile(BLFILE);
            }
        }
        if (data != null) {
            ContentResolver cr = context.getContentResolver();
            ContentValues cv = new ContentValues();
            cv.put(Telephony.Blacklist.PHONE_MODE, 1);

            for (PhoneNumber number : data) {
                Uri uri = Uri.withAppendedPath(
                        Telephony.Blacklist.CONTENT_FILTER_BYNUMBER_URI, number.phone);
                cv.put(Telephony.Blacklist.NUMBER, number.phone);
                cr.update(uri, cv, null, null);
            }
        }
    }
}
