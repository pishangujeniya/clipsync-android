package com.pishangujeniya.clipsync;

import android.app.ActivityManager;
import android.content.ClipboardManager;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pishangujeniya.clipsync.helper.Utility;
import com.pishangujeniya.clipsync.service.ClipBoardMonitor;
import com.pishangujeniya.clipsync.service.SignalRService;

public class ControlsActivity extends AppCompatActivity {


    private FloatingActionButton start_service_button;
    private FloatingActionButton stop_service_button;
    private FloatingActionButton logout_button;


    private Utility utility;
    private final String TAG = ControlsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controls);

        utility = new Utility(this);

        start_service_button = findViewById(R.id.activity_controls_service_start_fab);
        stop_service_button = findViewById(R.id.activity_controls_service_stop_fab);
        logout_button = findViewById(R.id.activity_controls_log_out_fab);

        start_service_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent signalRServiceIntent = new Intent(ControlsActivity.this, SignalRService.class);
                signalRServiceIntent.setAction(GlobalValues.START_SERVICE);
                startService(signalRServiceIntent);

                // Always Call after SignalR Service Started
                startService(new Intent(ControlsActivity.this, ClipBoardMonitor.class));


                start_service_button.hide();
                stop_service_button.show();

            }
        });

        stop_service_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                stop_service_button.hide();
                start_service_button.show();

                stopServices();
            }
        });

        logout_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });


    }

    private boolean isServiceRunning(String classgetName) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            Log.e(TAG, service.service.getClassName());
            Log.e(TAG, "ClassName" + service.service.getClassName());
            if (classgetName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void stopServices() {
        Intent signalRServiceIntent = new Intent(ControlsActivity.this, SignalRService.class);
        signalRServiceIntent.setAction(GlobalValues.STOP_SERVICE);
        stopService(signalRServiceIntent);

        // Always Call after SignalR Service Started
        stopService(new Intent(ControlsActivity.this, ClipBoardMonitor.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void logout() {
        utility.clearUserPrefs();
        Toast.makeText(getApplicationContext(), "Logged Out", Toast.LENGTH_SHORT).show();
        finishAffinity();
    }

    @Override
    protected void onDestroy() {
        stopServices();
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}
