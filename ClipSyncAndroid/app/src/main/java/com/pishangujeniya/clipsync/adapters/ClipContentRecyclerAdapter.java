package com.pishangujeniya.clipsync.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.pishangujeniya.clipsync.Delegate.Delegate;
import com.pishangujeniya.clipsync.R;
import com.pishangujeniya.clipsync.helper.DataHolder;

import java.util.ArrayList;

public class ClipContentRecyclerAdapter extends RecyclerView.Adapter<ClipContentRecyclerAdapter.ViewHolder> {

    ArrayList<DataHolder.ClipSyncClipData> clipSyncClipDataArrayList;
    Context context;
    Delegate.ClipRecyclerClickListener clipRecyclerClickListener;

    public ClipContentRecyclerAdapter(Context context, ArrayList<DataHolder.ClipSyncClipData> clipSyncClipDataArrayList, Delegate.ClipRecyclerClickListener clipRecyclerClickListener) {
        this.clipSyncClipDataArrayList = clipSyncClipDataArrayList;
        this.context = context;
        this.clipRecyclerClickListener = clipRecyclerClickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements Delegate.ClipRecyclerClickListener {

        protected TextView clip_content;
        protected ImageButton copy_button;
        protected TextView clip_title;
        protected ScrollView scrollView;

        public ViewHolder(View itemView) {
            super(itemView);
            clip_content = itemView.findViewById(R.id.recycler_clip_card_clip_content);
            clip_content.setMaxLines(15);
            copy_button = itemView.findViewById(R.id.copy_button);
            clip_title = itemView.findViewById(R.id.recycler_clip_card_clip_title);
            copy_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    copyClick(getAdapterPosition());
                }
            });

            clip_content.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clipClick(getAdapterPosition());
                }
            });

            clip_content.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    OnLongClick(getAdapterPosition());
                    return true;
                }
            });
        }


        @Override
        public void copyClick(int position) {
            clipRecyclerClickListener.copyClick(position);
        }

        @Override
        public void clipClick(int position) {
            clipRecyclerClickListener.clipClick(position);
        }

        @Override
        public void OnLongClick(int position) {
            clipRecyclerClickListener.OnLongClick(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_clip_card, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataHolder.ClipSyncClipData clipSyncClipData = clipSyncClipDataArrayList.get(position);
        holder.clip_title.setText(clipSyncClipData.getClip_title());
        holder.clip_content.setText(clipSyncClipData.getClip_content());
    }

    @Override
    public int getItemCount() {
        return clipSyncClipDataArrayList.size();
    }


}
