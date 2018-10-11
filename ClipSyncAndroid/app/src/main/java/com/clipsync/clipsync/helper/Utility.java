package com.clipsync.clipsync.helper;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class Utility {
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
        if (activeNetwork != null && activeNetwork.isConnected())

        {
            // notify user you are online
            return true;
        } else

        {
            // notify user you are not online
            return false;
        }

    }

    public void setUid(int uid) {
        SharedPreferences.Editor editor = userSharedPref.edit();
        editor.putInt("UID", uid);
        editor.commit();
    }

    public int getUid() {
        return userSharedPref.getInt("UID", 0);
    }

    public void setuserName(String userName) {
        SharedPreferences.Editor editor = userSharedPref.edit();
        editor.putString("USERNAME", userName);
        editor.commit();
    }

    public String getuserName() {
        return userSharedPref.getString("USERNAME", null);
    }

    public void setEmail(String email) {
        SharedPreferences.Editor editor = userSharedPref.edit();
        editor.putString("EMAIL", email);
        editor.commit();
    }

    public String getEmail() {
        return userSharedPref.getString("EMAIL", null);
    }

    public void clearUserPrefs() {
        SharedPreferences.Editor editor = userSharedPref.edit();
        editor.clear();
        editor.commit();
    }

}
