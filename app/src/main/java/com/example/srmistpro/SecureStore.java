package com.example.srmistpro;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecureStore {

    private static final String FILE_NAME = "secure_attendance_data";

    public static void saveData(Context context, String key, String value) {
        try {
            getPrefs(context).edit().putString(key, value).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String loadData(Context context, String key) {
        try {
            return getPrefs(context).getString(key, null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static SharedPreferences getPrefs(Context context) throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
}