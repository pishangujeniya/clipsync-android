package com.pishangujeniya.clipsync.helper;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.pishangujeniya.clipsync.GlobalValues;

import java.util.Objects;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;
import static android.content.Context.CLIPBOARD_SERVICE;

public class Utility {

    private final String TAG = Utility.class.getSimpleName();

    Context context;
    SharedPreferences userSharedPref;

    private final String USER = "USER";

    public Utility(Context context) {
        this.context = context;
        userSharedPref = context.getSharedPreferences(USER, Context.MODE_PRIVATE);
    }

    public String[] getPermissions() {
        return new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
        };
    }

    public boolean isDataAvailable() {

        final ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            // notify user you are online
            return true;
        } else {
            // notify user you are not online
            return false;
        }
    }

    public void setUid(int uid) {
        SharedPreferences.Editor editor = userSharedPref.edit();
        editor.putInt("UID", uid);
        editor.apply();

    }

    public int getUid() {
        return userSharedPref.getInt("UID", 0);
    }

    public void setServerAddress(String address) {
        SharedPreferences.Editor editor = userSharedPref.edit();
        editor.putString("SERVER_ADDRESS", address);
        editor.apply();
    }

    public String getServerAddress() {
        return userSharedPref.getString("SERVER_ADDRESS", null);
    }

    public void setServerPort(int port) {
        SharedPreferences.Editor editor = userSharedPref.edit();
        editor.putInt("SERVER_PORT", port);
        editor.apply();
    }

    public int getServerPort() {
        return userSharedPref.getInt("SERVER_PORT", 0);
    }


    public void setuserName(String userName) {
        SharedPreferences.Editor editor = userSharedPref.edit();
        editor.putString("USERNAME", userName);
        editor.apply();
    }

    public String getuserName() {
        return userSharedPref.getString("USERNAME", null);
    }

    public void setEmail(String email) {
        SharedPreferences.Editor editor = userSharedPref.edit();
        editor.putString("EMAIL", email);
        editor.apply();
    }

    public String getEmail() {
        return userSharedPref.getString("EMAIL", null);
    }

    public void clearUserPrefs() {
        SharedPreferences.Editor editor = userSharedPref.edit();
        editor.clear();
        editor.apply();
    }

    public String getLastClipboardText() {

        ClipboardManager mClipboardManager = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);

        assert mClipboardManager != null;
        if (!(mClipboardManager.hasPrimaryClip())) {

        } else if (!(Objects.requireNonNull(mClipboardManager.getPrimaryClipDescription()).hasMimeType(MIMETYPE_TEXT_PLAIN))) {

            // since the clipboard has data but it is not plain text
            //since the clipboard contains plain text.
            ClipData clip = mClipboardManager.getPrimaryClip();
            assert clip != null;
            return clip.getItemAt(0).getText().toString();


        } else {

            //since the clipboard contains plain text.
            ClipData clip = mClipboardManager.getPrimaryClip();
            assert clip != null;
            return clip.getItemAt(0).getText().toString();
        }

        return "";
    }


}
