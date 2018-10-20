package com.pishangujeniya.clipsync;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

public class SignUpActivity extends Activity implements EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {

    private static final String TAG = SignUpActivity.class.getSimpleName();

    private String[] CLIPSYNC_PERMISSIONS;
    private static int CLIPSYNC_PERMISSIONS_REQUEST_CODE = 1000;

    Utility utility;

    EditText _nameText;
    EditText _emailText;
    EditText _passwordText;
    Button _signupButton;
    TextView _loginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        utility = new Utility(getApplicationContext());
        CLIPSYNC_PERMISSIONS = utility.getPermissions();
//        EasyPermissions.requestPermissions(this, "You need to allow few permissions for better working of features in this app", CLIPSYNC_PERMISSIONS_REQUEST_CODE, CLIPSYNC_PERMISSIONS);
        if (!hasClipSyncPermissions()) {
            EasyPermissions.requestPermissions(this, "You need to allow few permissions for better working of features in this app", CLIPSYNC_PERMISSIONS_REQUEST_CODE, CLIPSYNC_PERMISSIONS);
        }

        _nameText = findViewById(R.id.input_name);
        _emailText = findViewById(R.id.input_email);
        _passwordText = findViewById(R.id.input_password);
        _signupButton = findViewById(R.id.btn_signup);
        _loginLink = findViewById(R.id.link_login);

        _signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        _loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                onBackPressed();
            }
        });
    }

    public void signup() {
        Log.d(TAG, "Signup");

        if (!validate()) {
            onSignupFailed();
            return;
        }

        _signupButton.setEnabled(false);

        final String name = _nameText.getText().toString();
        final String email = _emailText.getText().toString();
        final String password = _passwordText.getText().toString();


        final ProgressDialog progressDialog = new ProgressDialog(SignUpActivity.this,
                R.style.Theme_AppCompat_Light_Dialog_Alert);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Baking up your cake ... ");
        progressDialog.show();

        // Instantiate the RequestQueue.
        final RequestQueue queue = Volley.newRequestQueue(this);

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // On complete call either onSignupSuccess or onSignupFailed
                        // depending on success
                        String url = getResources().getString(R.string.apiSignUp);
                        Log.e(TAG, "URL  : " + url);
                        // Request a string response from the provided URL.

                        Map<String, String> params = new HashMap<String, String>();
                        params.put("userName", name);
                        params.put("userEmail", email);
                        params.put("userPassword", password);

                        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    Log.e("Response: ", response.toString());
                                    JSONObject jsonObject = new JSONObject(response.toString());
                                    boolean success = jsonObject.getBoolean("success");
                                    if (success) {
                                        onSignupSuccess(jsonObject.getInt("uid"), jsonObject.getString("username"), jsonObject.getString("email"));
                                    }else{
                                        onSignupFailed();
                                        progressDialog.dismiss();
                                    }

                                } catch (JSONException e) {

                                    e.printStackTrace();
                                    onSignupFailed();
                                    progressDialog.dismiss();
                                }

                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError response) {
                                Log.d("Response: ", response.toString());
                                onSignupFailed();
                                progressDialog.dismiss();
                            }
                        });


                        // Add the request to the RequestQueue.
                        queue.add(jsObjRequest);

                        // onSignupFailed();

                    }
                }, 3000);
    }


    public void onSignupSuccess(int uid, String username, String email) {
        _signupButton.setEnabled(true);
        Intent data = new Intent();
        data.putExtra("USERNAME", username);
        data.putExtra("UID", uid);
        data.putExtra("EMAIL", email);
        setResult(RESULT_OK, data);
        finish();
    }

    public void onSignupFailed() {
        Toast.makeText(getBaseContext(), "Sign failed", Toast.LENGTH_SHORT).show();
        Toast.makeText(getBaseContext(), "Check your email id is correct?", Toast.LENGTH_LONG).show();

        _signupButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String name = _nameText.getText().toString();
        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (name.isEmpty() || name.length() < 3) {
            _nameText.setError("at least 3 characters");
            valid = false;
        } else {
            _nameText.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
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
