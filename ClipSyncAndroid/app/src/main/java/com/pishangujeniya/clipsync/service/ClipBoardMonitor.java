package com.pishangujeniya.clipsync.service;

/*
 * Copyright 2013 Tristan Waddington
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */


//Thanks to the code author

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.pishangujeniya.clipsync.GlobalValues;
import com.pishangujeniya.clipsync.helper.Utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

/**
 * Monitors the {@link ClipboardManager} for changes and logs the text to a file.
 */
public class ClipBoardMonitor extends Service {
    private static final String TAG = ClipBoardMonitor.class.getSimpleName();
    private static final String FILENAME = "clipboard-history.txt";
    private static final int NOTIFICATION_ID = 777;

    private Utility utility;

    private File mHistoryFile;
    private ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
    private ClipboardManager mClipboardManager;


    private boolean mBound = false;
    private SignalRService mService;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.e(TAG, "Inside service connected - Activity ");
            // We've bound to SignalRService, cast the IBinder and get SignalRService instance
            SignalRService.LocalBinder binder = (SignalRService.LocalBinder) service;
            mService = (SignalRService) binder.getService();
            mBound = true;
            Log.e(TAG, "bound status - " + mBound);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            mBound = false;
            Log.e(TAG, "bound disconnected - status - " + mBound);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // TODO: Show an ongoing notification when this service is running.
        utility = new Utility(getApplicationContext());
        mHistoryFile = new File(getExternalFilesDir(null), FILENAME);
        mClipboardManager =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        mClipboardManager.addPrimaryClipChangedListener(
                mOnPrimaryClipChangedListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent mIntent = new Intent(this, SignalRService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mClipboardManager != null) {
            mClipboardManager.removePrimaryClipChangedListener(
                    mOnPrimaryClipChangedListener);
        }

        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
            Log.e(TAG, "bound disconnecting - status - " + mBound);

        }
        if (mService != null) {
            mService.onDestroy();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private ClipboardManager.OnPrimaryClipChangedListener mOnPrimaryClipChangedListener =
            new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override
                public void onPrimaryClipChanged() {
                    Log.d(TAG, "onPrimaryClipChanged");
//                    mThreadPool.execute(new WriteHistoryRunnable(
//                            clip.getItemAt(0).getText()));
                    if (!(mClipboardManager.hasPrimaryClip())) {
                        Log.e(TAG,"no Primary Clip");
                    } else if (!(mClipboardManager.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN))) {

                        // since the clipboard has data but it is not plain text
                        //since the clipboard contains plain text.
                        ClipData clip = mClipboardManager.getPrimaryClip();
                        String copied_content = clip.getItemAt(0).getText().toString();
                        Log.e(TAG,"Content at 0 "+copied_content);
                        if(copied_content.contains(GlobalValues.copied_water_mark)){
                            // Means Copied text already copied by ClipSync and came back again so don't send again
                        }else{
                            Log.e(TAG, "Copied Text : " + copied_content);
                            if(mService != null){
//                            sendNotification(copied_content);
                                mService.sendCopiedText(copied_content);
                            }
                        }
                    } else {

                        //since the clipboard contains plain text.
                        ClipData clip = mClipboardManager.getPrimaryClip();
                        String copied_content = clip.getItemAt(0).getText().toString();
                        Log.e(TAG,"Content at 0 "+copied_content);
                        if(copied_content.contains(GlobalValues.copied_water_mark)){
                            // Means Copied text already copied by ClipSync and came back again so don't send again
                        }else{
                            Log.e(TAG, "Copied Text : " + copied_content);
                            if(mService != null){
//                            sendNotification(copied_content);
                            mService.sendCopiedText(copied_content);
                            }
                        }

                    }

                }
            };

    private class WriteHistoryRunnable implements Runnable {
        private final Date mNow;
        private final CharSequence mTextToWrite;

        public WriteHistoryRunnable(CharSequence text) {
            mNow = new Date(System.currentTimeMillis());
            mTextToWrite = text;
        }

        @Override
        public void run() {
            if (TextUtils.isEmpty(mTextToWrite)) {
                // Don't write empty text to the file
                return;
            }

            if (isExternalStorageWritable()) {
                try {
                    Log.i(TAG, "Writing new clip to history:");
                    Log.i(TAG, mTextToWrite.toString());
                    BufferedWriter writer =
                            new BufferedWriter(new FileWriter(mHistoryFile, true));
                    writer.write(String.format("[%s]: ", mNow.toString()));
                    writer.write(mTextToWrite.toString());
                    writer.newLine();
                    writer.close();
                } catch (IOException e) {
                    Log.w(TAG, String.format("Failed to open file %s for writing!",
                            mHistoryFile.getAbsoluteFile()));
                }
            } else {
                Log.w(TAG, "External storage is not writable!");
            }
        }
    }
}