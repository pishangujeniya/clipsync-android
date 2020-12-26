package com.pishangujeniya.clipsync;

import android.app.ActivityManager;
import android.content.Intent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.pishangujeniya.clipsync.helper.Utility;
import com.pishangujeniya.clipsync.service.ClipBoardMonitor;
import com.pishangujeniya.clipsync.service.SignalRService;

public class ControlsActivity extends AppCompatActivity {


    private FloatingActionButton start_service_button;
    private FloatingActionButton stop_service_button;
    private FloatingActionButton logout_button;
    private EditText server_address_edit_text;
    private EditText server_port_edit_text;
    private EditText server_uid;


    private Utility utility;
    private final String TAG = ControlsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controls);

        utility = new Utility(this);

        server_address_edit_text = findViewById(R.id.serverAddressEditText);
        server_port_edit_text = findViewById(R.id.serverPortEditText);
        server_uid = findViewById(R.id.serverUIDEditeText);

        server_address_edit_text.setText(utility.getServerAddress() == null ? "" : utility.getServerAddress());
        int server_port_value = utility.getServerPort();
        int server_uid_value = utility.getUid();
        server_port_edit_text.setText(server_port_value == 0 ? "" : String.valueOf(server_port_value));
        server_uid.setText(server_uid_value == 0 ? "" : String.valueOf(server_uid_value));


        start_service_button = findViewById(R.id.activity_controls_service_start_fab);
        stop_service_button = findViewById(R.id.activity_controls_service_stop_fab);
        logout_button = findViewById(R.id.activity_controls_log_out_fab);

        start_service_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (server_address_edit_text.getText() != null && server_address_edit_text.getText().toString().length() > 0 && server_port_edit_text.getText() != null && server_port_edit_text.getText().toString().length() > 1 && server_uid.getText() != null && server_uid.getText().toString().length() > 1) {

                    utility.setServerAddress(server_address_edit_text.getText().toString().trim());
                    utility.setServerPort(Integer.parseInt(server_port_edit_text.getText().toString().trim()));
                    utility.setUid(Integer.parseInt(server_uid.getText().toString().trim()));

                    Intent signalRServiceIntent = new Intent(ControlsActivity.this, SignalRService.class);
                    signalRServiceIntent.setAction(GlobalValues.START_SERVICE);
                    startService(signalRServiceIntent);

                    // Always Call after SignalR Service Started
                    startService(new Intent(ControlsActivity.this, ClipBoardMonitor.class));


                    start_service_button.hide();
                    stop_service_button.show();
                } else {
                    server_address_edit_text.setError("Enter Required Fields");
                    server_port_edit_text.setError("Enter Required Fields");
                    server_uid.setError("Enter Required Fields");
                }


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
