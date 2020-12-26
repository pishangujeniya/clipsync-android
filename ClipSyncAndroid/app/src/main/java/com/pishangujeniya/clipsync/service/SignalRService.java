package com.pishangujeniya.clipsync.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.google.gson.JsonElement;
import com.pishangujeniya.clipsync.ControlsActivity;
import com.pishangujeniya.clipsync.GlobalValues;
import com.pishangujeniya.clipsync.R;
import com.pishangujeniya.clipsync.helper.Utility;

import java.util.UUID;

import microsoft.aspnet.signalr.client.Action;
import microsoft.aspnet.signalr.client.ConnectionState;
import microsoft.aspnet.signalr.client.ErrorCallback;
import microsoft.aspnet.signalr.client.LogLevel;
import microsoft.aspnet.signalr.client.Logger;
import microsoft.aspnet.signalr.client.MessageReceivedHandler;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.Subscription;


public class SignalRService extends Service {
    private final String TAG = SignalRService.class.getSimpleName();
    private HubConnection conn;
    private HubProxy mHubProxy;
    private Handler mHandler; // to display Toast message
    private final LocalBinder mBinder = new LocalBinder();
    public Boolean is_service_connected = false;

    private Context context;
    private NotificationManager mNotificationManager;
    private Notification notification;
    private String CHANNEL_ID = "ClipSyncServer";// The id of the channel.
    private CharSequence name = "ClipSyncServer";// The user-visible name of the channel.
    private String NOTIFICATION_TITLE = "ClipSync Working";
    private String NOTIFICATION_CONTENT_TEXT = "Copy Paste";
    private PendingIntent pStopSelf;
    private Bitmap icon;
    private PendingIntent pendingIntent;

    private NotificationChannel mChannel = null;

    private Utility utility;

    boolean looperThreadCreated = false;

    public SignalRService() {
//        conn = new HubConnection("YOUR CONNECTION NAME");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("service", "Inside oncreate  - service");

        // context = this.getApplicationContext();
        context = getBaseContext();
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        utility = new Utility(context);

        mHandler = new Handler(Looper.getMainLooper());

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (GlobalValues.STOP_SERVICE.equals(intent.getAction())) {
            Log.d(TAG, "called to cancel service");
            stopForeground(true);
            stopSelf();
            mNotificationManager.cancel(GlobalValues.SIGNALR_SERVICE_NOTIFICATION_ID);
        } else if (GlobalValues.START_SERVICE.equals(intent.getAction())) {
            showNotification();
            startSignalR();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (conn != null) {

            try {
                ConnectionState state = conn.getState();
                if (state.compareTo(ConnectionState.Connected) > -1) {
                    conn.stop();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        if (mNotificationManager != null) {
            mNotificationManager.cancel(GlobalValues.SIGNALR_SERVICE_NOTIFICATION_ID);
        }
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("Unbounding", "SignalRservice Service unbound");
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return the communication channel to the service.
        Log.d("service", "onBind  - service");
//        startSignalR();
        return mBinder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public SignalRService getService() {
            // Return this instance of SignalRService so clients can call public methods
            return SignalRService.this;
        }
    }

    private void showNotification() {


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mNotificationManager.createNotificationChannel(mChannel);
        }

        Intent notificationIntent = new Intent(this, ControlsActivity.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.clip_sync_logo_2);

        Intent stop_self_intent = new Intent(SignalRService.this, SignalRService.class);
        stop_self_intent.setAction(GlobalValues.STOP_SERVICE);

        pStopSelf = PendingIntent.getService(context, GlobalValues.SIGNALR_SERVICE_NOTIFICATION_ID, stop_self_intent, PendingIntent.FLAG_CANCEL_CURRENT);

        notification = new NotificationCompat.Builder(this)
                .setContentTitle(NOTIFICATION_TITLE)
                .setTicker(NOTIFICATION_TITLE)
                .setContentText(NOTIFICATION_CONTENT_TEXT)
                .setSmallIcon(R.drawable.clip_sync_logo_2)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setChannelId(CHANNEL_ID)
                .addAction(android.R.drawable.ic_media_previous, "Stop", pStopSelf)
                .build();


        startForeground(GlobalValues.SIGNALR_SERVICE_NOTIFICATION_ID, notification);

    }

    public void sendCopiedText(String text) {
        if (is_service_connected) {
            Log.e(TAG, "Sending Copied Text to SignalR");
            mHubProxy.invoke(GlobalValues.send_copied_text_signalr_method_name, text);
        } else {
            Log.e(TAG, "Service is not connected so not sending copied text");
        }
    }


    private void startSignalR() {
        // Create a new console logger
        final Logger logger = new Logger() {
            @Override
            public void log(String message, LogLevel level) {
                Log.d("SignalR : ", message);
            }
        };
        // Connect to the server
        String parameters = "&uid=" + utility.getUid() + "&platform=ANDROID" + "&device_id=" + UUID.randomUUID().toString();
        String server_address = "http://" + utility.getServerAddress() + ":" + utility.getServerPort();
        conn = new HubConnection(server_address, parameters, true, logger);

        // Create the hub proxy
        HubProxy proxy = conn.createHubProxy(GlobalValues.SignalHubName);

        mHubProxy = proxy;

        Subscription subscription = proxy.subscribe(GlobalValues.receive_copied_text_signalr_method_name);
        subscription.addReceivedHandler(new Action<JsonElement[]>() {
            public void run(JsonElement[] eventParameters) {
                if (eventParameters == null || eventParameters.length <= 0) {
                    return;
                }

                System.out.println("Received Copied Text : " + eventParameters[0]);

//                HandlerThread uiThread = new HandlerThread("UIHandler");
                if (!looperThreadCreated) {
                    Looper.prepare();
                    looperThreadCreated = true;
                }
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

                String received_text = eventParameters[0].toString();
                if (received_text.length() > 2) {
                    received_text = received_text.substring(1, received_text.length() - 1);
                }
                if (!received_text.contains(GlobalValues.copied_water_mark) && !utility.getLastClipboardText().equalsIgnoreCase(received_text)) {
                    ClipData clip = ClipData.newPlainText(String.valueOf(System.currentTimeMillis()), received_text + GlobalValues.copied_water_mark);
                    assert clipboard != null;
                    clipboard.setPrimaryClip(clip);
                }

            }
        });




        /*proxy.subscribe(new Object() {
            @SuppressWarnings("unused")
            public void recieveIncomingChat(RecieveIncomingchats recieveIncomingchats) {
                MainFragment.receivedincommingchats(recieveIncomingchats);
                Log.d("hit:", "Hit on receive Incoming chats");
            }
            @SuppressWarnings("unused")
            public void serviceStatus(boolean temp){
                Log.d("service_status", "status called");
            }
        });*/


        // Subscribe to the error event
        conn.error(new ErrorCallback() {
            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }
        });

        // Subscribe to the connected event
        conn.connected(new Runnable() {

            @Override
            public void run() {
                System.out.println("Connecting...");
                is_service_connected = true;

                if (mNotificationManager != null && notification != null) {
                    notification = new NotificationCompat.Builder(context)
                            .setContentTitle(NOTIFICATION_TITLE)
                            .setTicker(NOTIFICATION_TITLE)
                            .setContentText("Connecting...")
                            .setSmallIcon(R.drawable.clip_sync_logo_2)
                            .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                            .setContentIntent(pendingIntent)
                            .setOngoing(true)
                            .setOnlyAlertOnce(true)
                            .setChannelId(CHANNEL_ID)
                            .addAction(android.R.drawable.ic_media_previous, "Stop", pStopSelf)
                            .build();

                    mNotificationManager.notify(GlobalValues.SIGNALR_SERVICE_NOTIFICATION_ID, notification);
                }
            }
        });

        // Subscribe to the closed event
        conn.closed(new Runnable() {

            @Override
            public void run() {
                System.out.println("DISCONNECTED");
                if (mNotificationManager != null && notification != null) {
                    notification = new NotificationCompat.Builder(context)
                            .setContentTitle(NOTIFICATION_TITLE)
                            .setTicker(NOTIFICATION_TITLE)
                            .setContentText("Disconnected")
                            .setSmallIcon(R.drawable.clip_sync_logo_2)
                            .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .setOnlyAlertOnce(true)
                            .setChannelId(CHANNEL_ID)
                            .addAction(android.R.drawable.ic_media_previous, "Stop", pStopSelf)
                            .build();

                    mNotificationManager.notify(GlobalValues.SIGNALR_SERVICE_NOTIFICATION_ID, notification);
                }
            }
        });

        // Start the connection
        conn.start().done(new Action<Void>() {
            @Override
            public void run(Void obj) {
                System.out.println("Connected");
                if (mNotificationManager != null && notification != null) {
                    notification = new NotificationCompat.Builder(context)
                            .setContentTitle(NOTIFICATION_TITLE)
                            .setTicker(NOTIFICATION_TITLE)
                            .setContentText("Connected")
                            .setSmallIcon(R.drawable.clip_sync_logo_2)
                            .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                            .setContentIntent(pendingIntent)
                            .setOngoing(true)
                            .setOnlyAlertOnce(true)
                            .setChannelId(CHANNEL_ID)
                            .addAction(android.R.drawable.ic_media_previous, "Stop", pStopSelf)
                            .build();

                    mNotificationManager.notify(GlobalValues.SIGNALR_SERVICE_NOTIFICATION_ID, notification);
                }
            }
        });

        // Subscribe to the received event
        conn.received(new MessageReceivedHandler() {
            @Override
            public void onMessageReceived(JsonElement json) {
                System.out.println("RAW received message: " + json.toString());
            }
        });

    }

}