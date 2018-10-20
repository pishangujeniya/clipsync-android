package com.pishangujeniya.clipsync;

import android.app.ActivityManager;
import android.content.ClipboardManager;
import android.content.Intent;
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

    private TextView status_text_veiw;
    private Button start_service_button;
    private Button stop_service_button;
    private Button logout_button;
    private Button refresh_button;

    private Utility utility;
    private final String TAG = ControlsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controls);

        start_service_button = findViewById(R.id.activity_controls_service_start_button);
        stop_service_button = findViewById(R.id.activity_controls_service_stop_button);
        logout_button = findViewById(R.id.activity_controls_log_out_button);
        status_text_veiw = findViewById(R.id.activity_controls_status_text_view);
        refresh_button = findViewById(R.id.activity_controls_status_refresh_button);

        refresh_button.setVisibility(View.INVISIBLE);
        status_text_veiw.setVisibility(View.INVISIBLE);
        utility = new Utility(this);

        start_service_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent signalRServiceIntent = new Intent(ControlsActivity.this, SignalRService.class);
                startService(signalRServiceIntent);

                // Always Call after SignalR Service Started
                startService(new Intent(ControlsActivity.this, ClipBoardMonitor.class));

                updateStatusText();

                start_service_button.setVisibility(View.INVISIBLE);

            }
        });

        stop_service_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopServices();

                updateStatusText();

                start_service_button.setVisibility(View.VISIBLE);
            }
        });

        refresh_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateStatusText();
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
            Log.e(TAG,service.service.getClassName());
            Log.e(TAG,"ClassName" +service.service.getClassName());
            if (classgetName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void stopServices() {
        Intent signalRServiceIntent = new Intent(ControlsActivity.this, SignalRService.class);
        stopService(signalRServiceIntent);

        // Always Call after SignalR Service Started
        stopService(new Intent(ControlsActivity.this, ClipBoardMonitor.class));
    }

    private void updateStatusText(){
        String text_to_display = "Status : ";

        if (isServiceRunning(SignalRService.class.getName()) && isServiceRunning(ClipboardManager.class.getName())) {
            text_to_display = text_to_display + "Yes";
        } else {
            text_to_display = text_to_display + "No";
        }

        status_text_veiw.setText(text_to_display);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusText();
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
