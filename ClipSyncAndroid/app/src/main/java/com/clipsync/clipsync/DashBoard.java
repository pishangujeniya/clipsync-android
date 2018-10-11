package com.clipsync.clipsync;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.clipsync.clipsync.Delegate.Delegate;
import com.clipsync.clipsync.adapters.ClipContentRecyclerAdapter;
import com.clipsync.clipsync.helper.DataHolder;
import com.clipsync.clipsync.helper.Utility;
import com.clipsync.clipsync.networking.CustomRequest;
import com.clipsync.clipsync.service.ClipBoardMonitor;
import com.gdacciaro.iOSDialog.iOSDialog;
import com.gdacciaro.iOSDialog.iOSDialogBuilder;
import com.gdacciaro.iOSDialog.iOSDialogClickListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DashBoard extends Activity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = DashBoard.class.getSimpleName();
    private static final int REQUEST_CLIP_MAIN_ACTIVITY = 10001;

    private static final int ISUPDATED = 1;

    RecyclerView recyclerView;
    ArrayList<DataHolder.ClipSyncClipData> clipSyncClipDataArrayList;
    ClipContentRecyclerAdapter clipContentRecyclerAdapter;
    Utility utility;
    FloatingActionButton exit;
    FloatingActionButton new_clip;
    SwipeRefreshLayout swiper;
    TextView version_textView;

    int user_version_code;

    RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);
        utility = new Utility(this);

        recyclerView = findViewById(R.id.activity_dashboard_clip_recycler_view);
        swiper = findViewById(R.id.swiper);
        swiper.setOnRefreshListener(this);
        swiper.setRefreshing(true);

        exit = findViewById(R.id.exit);
        version_textView = findViewById(R.id.version);
        version_textView.setText(String.format("© ClipSync v%s", BuildConfig.VERSION_NAME));
        user_version_code = BuildConfig.VERSION_CODE;

        new_clip = findViewById(R.id.new_clip);

        queue = Volley.newRequestQueue(this);
        startService(new Intent(this, ClipBoardMonitor.class));
        setupDashboard();

        getLatestVersion();

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                utility.clearUserPrefs();
                Toast.makeText(getApplicationContext(), "Logged Out", Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        });

        new_clip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DashBoard.this, ClipMainActivity.class);
                intent.putExtra("ISNEW", true);
                intent.putExtra("CONTENT", "");
                intent.putExtra("TITLE", "'");
                intent.putExtra("ID", 0);
                startActivityForResult(intent, REQUEST_CLIP_MAIN_ACTIVITY);
            }
        });
    }

    private void getLatestVersion() {
        final String url = getResources().getString(R.string.apiCheckUpdate);
        Log.e(TAG, "URL  : " + url);
        // Request a string response from the provided URL.

        Map<String, String> params = new HashMap<String, String>();
        params.put("versioncode", String.valueOf(user_version_code));

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.e(TAG, "Response :" + response.toString());
                    final JSONObject jsonObject = new JSONObject(response.toString());
                    boolean success = jsonObject.getBoolean("success");
                    if (success) {
                        final String url = jsonObject.getString("link");
                        final int latest_version_code = jsonObject.getInt("versioncode");
                        if (user_version_code != latest_version_code) {
                            new iOSDialogBuilder(DashBoard.this)
                                    .setTitle(getString(R.string.info_title))
                                    .setSubtitle(getString(R.string.update_app))
                                    .setBoldPositiveLabel(true)
                                    .setCancelable(false)
                                    .setPositiveListener(getString(R.string.update), new iOSDialogClickListener() {
                                        @Override
                                        public void onClick(iOSDialog dialog) {
                                            Intent i = new Intent(Intent.ACTION_VIEW);
                                            i.setData(Uri.parse(url));
                                            startActivity(i);
                                            dialog.dismiss();
                                            finishAffinity();

                                        }
                                    })
                                    .setNegativeListener(getString(R.string.later), new iOSDialogClickListener() {
                                        @Override
                                        public void onClick(iOSDialog dialog) {
                                            dialog.dismiss();

                                        }
                                    })
                                    .build().show();
                        }
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

    }

    private void setupDashboard() {
        getClips(utility.getUid(), new getClipsResponse() {
            @Override
            public void response(final ArrayList<DataHolder.ClipSyncClipData> server_ClipSync_clipDataArrayList) {
                clipContentRecyclerAdapter = new ClipContentRecyclerAdapter(getApplicationContext(), server_ClipSync_clipDataArrayList, new Delegate.ClipRecyclerClickListener() {
                    @Override
                    public void copyClick(int position) {
                        copyToClipBoard(server_ClipSync_clipDataArrayList.get(position).getClip_title(), server_ClipSync_clipDataArrayList.get(position).getClip_content());
                        Toast.makeText(DashBoard.this, "Copied", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void clipClick(int position) {
                        Intent intent = new Intent(DashBoard.this, ClipMainActivity.class);
                        intent.putExtra("CONTENT", server_ClipSync_clipDataArrayList.get(position).getClip_content());
                        intent.putExtra("TITLE", server_ClipSync_clipDataArrayList.get(position).getClip_title());
                        intent.putExtra("ID", server_ClipSync_clipDataArrayList.get(position).getClip_id());
                        startActivityForResult(intent, REQUEST_CLIP_MAIN_ACTIVITY);
                    }

                    @Override
                    public void OnLongClick(final int position) {
                        new iOSDialogBuilder(DashBoard.this)
                                .setTitle(getString(R.string.warning_title))
                                .setSubtitle(getString(R.string.update_changes_or_discard))
                                .setBoldPositiveLabel(true)
                                .setCancelable(false)
                                .setPositiveListener(getString(R.string.delete), new iOSDialogClickListener() {
                                    @Override
                                    public void onClick(iOSDialog dialog) {
                                        deleteClip(server_ClipSync_clipDataArrayList.get(position).getClip_id(), utility.getUid());
                                        clipContentRecyclerAdapter.notifyItemRemoved(position);
                                        dialog.dismiss();

                                    }
                                })
                                .setNegativeListener(getString(R.string.cancel), new iOSDialogClickListener() {
                                    @Override
                                    public void onClick(iOSDialog dialog) {
                                        dialog.dismiss();

                                    }
                                })
                                .build().show();
                    }
                });
                clipSyncClipDataArrayList = server_ClipSync_clipDataArrayList;
                Log.e(TAG, "Size :" + clipSyncClipDataArrayList.size());
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
                recyclerView.setLayoutManager(mLayoutManager);
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                recyclerView.setAdapter(null);
                recyclerView.setAdapter(clipContentRecyclerAdapter);
                swiper.setRefreshing(false);

            }
        });


    }

    private void deleteClip(int clip_id, int uid) {
        String url = getResources().getString(R.string.apiDeleteClip);
        Log.e(TAG, "URL  : " + url);
        // Request a string response from the provided URL.

        Map<String, String> params = new HashMap<String, String>();
        params.put("UID", String.valueOf(uid));
        params.put("CLIP_ID", String.valueOf(clip_id));

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.e(TAG, "Response :" + response.toString());
                    JSONObject jsonObject = new JSONObject(response.toString());
                    boolean success = jsonObject.getBoolean("success");
                    if (success) {
                        Toast.makeText(getApplicationContext(), "Deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Failed to Delete Clip", Toast.LENGTH_SHORT).show();
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
    }

    private void copyToClipBoard(String title, String content) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(title, content);
        assert clipboard != null;
        clipboard.setPrimaryClip(clip);
    }

    public void getClips(int uid, final getClipsResponse getClipsResponse) {

        final ArrayList<DataHolder.ClipSyncClipData> serverClipArray = new ArrayList<DataHolder.ClipSyncClipData>();


        String url = getResources().getString(R.string.apiGetClips);
        Log.e(TAG, "URL  : " + url);
        // Request a string response from the provided URL.

        Map<String, String> params = new HashMap<String, String>();
        params.put("UID", String.valueOf(uid));
        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.e("Response: ", response.toString());
                    JSONObject jsonObject = new JSONObject(response.toString());
                    boolean success = jsonObject.getBoolean("success");
                    if (success) {
                        for (int i = 0; i < jsonObject.length() - 1; i++) {
                            JSONObject clip_data_json = jsonObject.getJSONObject(String.valueOf(i));
                            DataHolder.ClipSyncClipData clipSyncClipData = new DataHolder.ClipSyncClipData();
                            clipSyncClipData.setClip_id(clip_data_json.getInt("clip_id"));
                            clipSyncClipData.setClip_title(clip_data_json.getString("clip_title"));
                            clipSyncClipData.setClip_content(clip_data_json.getString("clip_data"));
                            serverClipArray.add(clipSyncClipData);
                        }
                        getClipsResponse.response(serverClipArray);
                    } else {
                        getClipsResponse.response(serverClipArray);
                        swiper.setRefreshing(false);
                    }

                } catch (JSONException e) {

                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError response) {
                Log.d("Response: ", response.toString());
            }
        });

        // Add the request to the RequestQueue.
        queue.add(jsObjRequest);


    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }

    @Override
    public void onRefresh() {
        swiper.setRefreshing(true);
        setupDashboard();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CLIP_MAIN_ACTIVITY) {
            if (resultCode == ISUPDATED) {
                onRefresh();
                // By default we just finish the Activity and log them in automatically

            }
        }
    }
}

interface getClipsResponse {
    void response(ArrayList<DataHolder.ClipSyncClipData> clipSyncClipDataArrayList);
}
