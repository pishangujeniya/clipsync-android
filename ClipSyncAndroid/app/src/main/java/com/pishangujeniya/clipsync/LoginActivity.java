package com.pishangujeniya.clipsync;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.pishangujeniya.clipsync.helper.Utility;
import com.pishangujeniya.clipsync.networking.CustomRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class LoginActivity extends Activity implements EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {


    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final int REQUEST_SIGNUP = 0;
    private String[] CLIPSYNC_PERMISSIONS;
    private static int CLIPSYNC_PERMISSIONS_REQUEST_CODE = 1000;

    private Utility utility;


    EditText _emailText;

    EditText _passwordText;

    Button _loginButton;

    TextView _signupLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        utility = new Utility(getApplicationContext());
        CLIPSYNC_PERMISSIONS = utility.getPermissions();
//        EasyPermissions.requestPermissions(this, "You need to allow few permissions for better working of features in this app", CLIPSYNC_PERMISSIONS_REQUEST_CODE, CLIPSYNC_PERMISSIONS);
        if (!hasClipSyncPermissions()) {
            EasyPermissions.requestPermissions(this, "You need to allow few permissions for better working of features in this app", CLIPSYNC_PERMISSIONS_REQUEST_CODE, CLIPSYNC_PERMISSIONS);
        }

        CardView place_holder = findViewById(R.id.place_holder);
        CardView cardView = findViewById(R.id.activity_login_cardview);
        if (!utility.isDataAvailable()) {
            cardView.setVisibility(View.GONE);
            place_holder.setVisibility(View.VISIBLE);
            Button activity_login_restart_button = findViewById(R.id.activity_login_restart_button);
            activity_login_restart_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        } else {
            cardView.setVisibility(View.VISIBLE);
            place_holder.setVisibility(View.GONE);
            if (utility.getUid() != 0) {
//                startDashBoard();

                startControlsActivity();
            }
        }

        _emailText = findViewById(R.id.input_email);
        _passwordText = findViewById(R.id.input_password);
        _loginButton = findViewById(R.id.btn_login);
        _signupLink = findViewById(R.id.link_signup);

        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

        _signupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), SignUpActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
            }
        });
    }

    public void login() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }

        _loginButton.setEnabled(true);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.Theme_AppCompat_Light_Dialog_Alert);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        final String email = _emailText.getText().toString();
        final String password = _passwordText.getText().toString();
        // Instantiate the RequestQueue.
        final RequestQueue queue = Volley.newRequestQueue(this);


        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // On complete call either onLoginSuccess or onLoginFailed


                        String url = getResources().getString(R.string.apiLogin);
                        Log.e(TAG, "URL  : " + url);
                        // Request a string response from the provided URL.

                        Map<String, String> params = new HashMap<String, String>();
                        params.put("userName", email);
                        params.put("userPassword", password);

                        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    Log.e("Response: ", response.toString());
                                    JSONObject jsonObject = new JSONObject(response.toString());
                                    boolean success = jsonObject.getBoolean("success");
                                    if (success) {
                                        onLoginSuccess(jsonObject.getInt("uid"), jsonObject.getString("username"), jsonObject.getString("email"));
                                    }
                                    progressDialog.dismiss();

                                } catch (JSONException e) {

                                    e.printStackTrace();
                                    onLoginFailed();
                                    progressDialog.dismiss();
                                }

                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError response) {
                                Log.d("Response: ", response.toString());
                                onLoginFailed();
                                progressDialog.dismiss();
                            }
                        });


                        // Add the request to the RequestQueue.
                        queue.add(jsObjRequest);


                    }
                }, 3000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {

                // TODO: Implement successful signup logic here
                onLoginSuccess(data.getIntExtra("UID", 0), data.getStringExtra("USERNAME"), data.getStringExtra("EMAIL"));
                // By default we just finish the Activity and log them in automatically

            }
        }
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess(int uid, String username, String email) {
        utility.setEmail(email);
        utility.setUid(uid);
        utility.setuserName(username);
        _loginButton.setEnabled(true);

//        startDashBoard();

        startControlsActivity();
    }

    public void startControlsActivity() {
        Intent intent = new Intent(LoginActivity.this, ControlsActivity.class);
        startActivity(intent);
    }

    public void startDashBoard() {
        Intent intent = new Intent(LoginActivity.this, DashBoard.class);
        startActivity(intent);
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();
        _loginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

//        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        if (email.isEmpty()) {
            _emailText.setError("enter an email or username");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty()) {
            _passwordText.setError("please enter password");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

    private boolean hasClipSyncPermissions() {
        return EasyPermissions.hasPermissions(this, CLIPSYNC_PERMISSIONS);
    }


    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onRationaleAccepted(int requestCode) {
        Log.d(TAG, "onRationaleAccepted:" + requestCode);
    }

    @Override
    public void onRationaleDenied(int requestCode) {
        Log.d(TAG, "onRationaleDenied:" + requestCode);
    }
}
