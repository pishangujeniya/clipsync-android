package com.pishangujeniya.clipsync.service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonElement;
import com.pishangujeniya.clipsync.GlobalValues;
import com.pishangujeniya.clipsync.helper.Utility;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import microsoft.aspnet.signalr.client.Action;
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

        utility = new Utility(context);

        mHandler = new Handler(Looper.getMainLooper());

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("service", "service start  - service");
        int result = super.onStartCommand(intent, flags, startId);
        startSignalR();
        return result;
    }

    @Override
    public void onDestroy() {
        conn.stop();
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
        startSignalR();
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

    /**
     * method for clients (activities)
     */
    private void getIncommingcht() {
        Log.d("Inside : ", "getIncommingcht - service - Method");
//        mHubProxy.invoke("addGroup", ProileId, Copanyid, "true", Token);
//        mHubProxy.invoke("GetChatQueue",ProileId, Token);
    }

    public void sendCopiedText(String text) {
        if (is_service_connected) {
            Log.e(TAG, "Sending Copied Text to SignalR");
            mHubProxy.invoke(GlobalValues.send_copied_text_signalr_method_name, text);
        } else {
            Log.e(TAG, "Service is not connected so not sending copied text");

        }
    }

    public void selectVisitor(String visitor_id, String CompanyID, String DisplayName, String startTime) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
        mHubProxy.invoke("seleVisitr", DisplayName, timeStamp, startTime);
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
        String parameters = "&uid=" + utility.getUid() + "&platform=ANDROID" + "&device_id=" + Build.SERIAL;
        conn = new HubConnection(GlobalValues.SignalRServerURL, parameters, true, logger);

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
                System.out.println("CONNECTED");
                is_service_connected = true;
            }
        });

        // Subscribe to the closed event
        conn.closed(new Runnable() {

            @Override
            public void run() {
                System.out.println("DISCONNECTED");
            }
        });

        // Start the connection
        conn.start().done(new Action<Void>() {
            @Override
            public void run(Void obj) {
                System.out.println("Done Connecting!");
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