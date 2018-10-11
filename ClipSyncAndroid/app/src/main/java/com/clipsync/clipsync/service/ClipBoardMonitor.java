package com.clipsync.clipsync.service;

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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.clipsync.clipsync.ClipMainActivity;
import com.clipsync.clipsync.R;
import com.clipsync.clipsync.helper.Utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;
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
    public void onDestroy() {
        super.onDestroy();

        if (mClipboardManager != null) {
            mClipboardManager.removePrimaryClipChangedListener(
                    mOnPrimaryClipChangedListener);
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

    public void sendNotification(String content) {

        Intent intent = new Intent(this, ClipMainActivity.class);
        intent.putExtra("ISNEW", true);
        intent.putExtra("FROM_NOTI", true);
        intent.putExtra("CONTENT", "");
        intent.putExtra("TITLE", "'");
        intent.putExtra("ID", 0);

// Creating a pending intent and wrapping our intent
        PendingIntent pendingIntent = PendingIntent.getActivity(this, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Get an instance of NotificationManager//


        Notification.Builder mBuilder =
                new Notification.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.clip_sync_logo_2)
                        .setStyle(new Notification.BigTextStyle().bigText(content))
                        .setAutoCancel(true)
                        .setContentTitle("Want to save this clip?")
                        .setContentIntent(pendingIntent);


        // Gets an instance of the NotificationManager service//

        NotificationManager mNotificationManager =

                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // When you issue multiple notifications about the same type of event,
        // it’s best practice for your app to try to update an existing notification
        // with this new information, rather than immediately creating a new notification.
        // If you want to update this notification at a later date, you need to assign it an ID.
        // You can then use this ID whenever you issue a subsequent notification.
        // If the previous notification is still visible, the system will update this existing notification,
        // rather than create a new one. In this example, the notification’s ID is 001//
        if (utility.getUid() != 0) {
            Objects.requireNonNull(mNotificationManager).notify(NOTIFICATION_ID, mBuilder.build());
        }

    }

    private ClipboardManager.OnPrimaryClipChangedListener mOnPrimaryClipChangedListener =
            new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override
                public void onPrimaryClipChanged() {
                    Log.d(TAG, "onPrimaryClipChanged");
//                    mThreadPool.execute(new WriteHistoryRunnable(
//                            clip.getItemAt(0).getText()));
                    if (!(mClipboardManager.hasPrimaryClip())) {

                    } else if (!(mClipboardManager.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN))) {

                        // since the clipboard has data but it is not plain text

                    } else {

                        //since the clipboard contains plain text.
                        ClipData clip = mClipboardManager.getPrimaryClip();
                        String copied_content = clip.getItemAt(0).getText().toString();
                        sendNotification(copied_content);
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