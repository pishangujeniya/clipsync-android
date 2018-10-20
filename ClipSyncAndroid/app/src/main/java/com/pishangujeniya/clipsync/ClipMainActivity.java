package com.pishangujeniya.clipsync;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.gdacciaro.iOSDialog.iOSDialog;
import com.gdacciaro.iOSDialog.iOSDialogBuilder;
import com.gdacciaro.iOSDialog.iOSDialogClickListener;
import com.pishangujeniya.clipsync.helper.DataHolder;
import com.pishangujeniya.clipsync.helper.Utility;
import com.pishangujeniya.clipsync.networking.CustomRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

public class ClipMainActivity extends Activity {
    private String TAG = ClipMainActivity.class.getSimpleName();

    protected EditText clip_content;
    protected FloatingActionButton copy_button;
    protected EditText clip_title;
    public ConstraintLayout constraintLayout;
    private FloatingActionButton save_clip;
    private FloatingActionButton edit_clip;
    RequestQueue queue;


    boolean isNew;
    boolean isTextChanged = false;

    private Utility utility;
    private DataHolder.ClipSyncClipData clipSyncClipData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clip_main);

        utility = new Utility(this);

        constraintLayout = findViewById(R.id.activity_clip_main_layout);
        clip_content = findViewById(R.id.recycler_clip_card_clip_content);
        clip_title = findViewById(R.id.recycler_clip_card_clip_title);
        copy_button = findViewById(R.id.copy_button);
        save_clip = findViewById(R.id.save_clip);
        edit_clip = findViewById(R.id.edit_clip);

        queue = Volley.newRequestQueue(this);

        setupClipMainActivity();
    }

    private void setupClipMainActivity() {

        isNew = getIntent().getBooleanExtra("ISNEW", false);
        clipSyncClipData = new DataHolder.ClipSyncClipData();
        if (!isNew) {
            clipSyncClipData.setClip_content(getIntent().getStringExtra("CONTENT"));
            clipSyncClipData.setClip_title(getIntent().getStringExtra("TITLE"));
            clipSyncClipData.setClip_id(getIntent().getIntExtra("ID", 0));
            clip_title.setText(clipSyncClipData.getClip_title());
            clip_content.setText(clipSyncClipData.getClip_content());
            save_clip.setVisibility(View.INVISIBLE);

        } else {
            clip_content.setEnabled(true);
            clip_title.setEnabled(true);
            clip_content.setText(pasteClipBoard());
            edit_clip.setVisibility(View.GONE);
            save_clip.setVisibility(View.VISIBLE);
        }

        edit_clip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clip_content.setEnabled(true);
                clip_title.setEnabled(true);
                clip_title.setFocusable(true);
                clip_content.setFocusable(true);
                clip_content.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.showSoftInput(clip_content, InputMethodManager.SHOW_IMPLICIT);
                save_clip.setVisibility(View.VISIBLE);
                isTextChanged = true;
                edit_clip.setVisibility(View.GONE);
            }
        });

        copy_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyToClipBoard(clip_title.getText().toString(), clip_content.getText().toString());
                Snackbar.make(constraintLayout, "Copied", Snackbar.LENGTH_SHORT).show();
            }
        });

        save_clip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNew) {
                    saveClip(clip_title.getText().toString(), clip_content.getText().toString(), utility.getUid());
                } else {
                    updateClip(clipSyncClipData.getClip_id(), clip_title.getText().toString(), clip_content.getText().toString(), utility.getUid());
                }
            }
        });

    }

    public void saveClip(String title, String content, int uid) {

        if (content.length() == 0) {
            Snackbar.make(constraintLayout, "Please Enter some Text", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (title.length() == 0) {
            if (content.length() < 36) {
                Snackbar.make(constraintLayout, "Please Enter Title", Snackbar.LENGTH_SHORT).show();
                return;
            }
            title = content.substring(0, 35);
        }

        String url = getResources().getString(R.string.apiInsertClip);
        Log.e(TAG, "URL  : " + url);
        // Request a string response from the provided URL.

        Map<String, String> params = new HashMap<String, String>();
        params.put("UID", String.valueOf(uid));
        params.put("CLIP_TITLE", title);
        params.put("CLIP_DATA", content);
        save_clip.setVisibility(View.INVISIBLE);
        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.e("Response: ", response.toString());
                    JSONObject jsonObject = new JSONObject(response.toString());
                    boolean success = jsonObject.getBoolean("success");
                    if (success) {
                        Toast.makeText(getApplicationContext(), "Added", Toast.LENGTH_SHORT).show();
                        if (getIntent().getBooleanExtra("FROM_NOTI", false)) {
                            moveTaskToBack(true);
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Failed to Add Clip", Toast.LENGTH_SHORT).show();
                        save_clip.setVisibility(View.VISIBLE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    save_clip.setVisibility(View.VISIBLE);
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError response) {
                Log.d("Response: ", response.toString());
                save_clip.setVisibility(View.VISIBLE);
            }
        });
        // Add the request to the RequestQueue.
        queue.add(jsObjRequest);
        setResult(1);
        isTextChanged = false;
        onBackPressed();
    }

    public void updateClip(int clip_id, String title, String content, int uid) {

        if (content.length() == 0) {
            Snackbar.make(constraintLayout, "Please Enter some Text", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (title.length() == 0) {
            if (content.length() < 36) {
                Snackbar.make(constraintLayout, "Please Enter Title", Snackbar.LENGTH_SHORT).show();
                return;
            }
            title = content.substring(0, 35);
        }

        String url = getResources().getString(R.string.apiUpdateClip);
        Log.e(TAG, "URL  : " + url);
        // Request a string response from the provided URL.

        Map<String, String> params = new HashMap<String, String>();
        params.put("UID", String.valueOf(uid));
        params.put("CLIP_ID", String.valueOf(clip_id));
        params.put("CLIP_TITLE", title);
        params.put("CLIP_DATA", content);

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.e(TAG, "Response :" + response.toString());
                    JSONObject jsonObject = new JSONObject(response.toString());
                    boolean success = jsonObject.getBoolean("success");
                    if (success) {
                        Toast.makeText(getApplicationContext(), "Updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Failed to Update Clip", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError response) {
                Log.e(TAG, "Response :" + response.toString());
            }
        });
        // Add the request to the RequestQueue.
        queue.add(jsObjRequest);
        setResult(1);
        isTextChanged = false;
        onBackPressed();

    }


    private void copyToClipBoard(String title, String content) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(title, content);
        assert clipboard != null;
        clipboard.setPrimaryClip(clip);
    }

    private String pasteClipBoard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        String pasteData = null;

        // If it does contain data, decide if you can handle the data.
        assert clipboard != null;
        if (!(clipboard.hasPrimaryClip())) {

        } else if (!(clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN))) {

            // since the clipboard has data but it is not plain text

        } else {

            //since the clipboard contains plain text.
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);

            // Gets the clipboard as text.
            pasteData = item.getText().toString();
        }

        return pasteData;
    }

    @Override
    public void onBackPressed() {
        if (isTextChanged) {
            new iOSDialogBuilder(ClipMainActivity.this)
                    .setTitle(getString(R.string.warning_title))
                    .setSubtitle(getString(R.string.update_changes_or_discard))
                    .setBoldPositiveLabel(true)
                    .setCancelable(false)
                    .setPositiveListener(getString(R.string.update), new iOSDialogClickListener() {
                        @Override
                        public void onClick(iOSDialog dialog) {
                            updateClip(clipSyncClipData.getClip_id(), clip_title.getText().toString(), clip_content.getText().toString(), utility.getUid());
                            dialog.dismiss();
                            ClipMainActivity.super.onBackPressed();

                        }
                    })
                    .setNegativeListener(getString(R.string.discard), new iOSDialogClickListener() {
                        @Override
                        public void onClick(iOSDialog dialog) {
                            dialog.dismiss();
                            ClipMainActivity.super.onBackPressed();
                        }
                    })
                    .build().show();
        } else {
            super.onBackPressed();
        }
    }
}
