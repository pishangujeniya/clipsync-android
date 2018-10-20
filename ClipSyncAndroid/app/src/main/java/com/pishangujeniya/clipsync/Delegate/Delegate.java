package com.pishangujeniya.clipsync.Delegate;

public class Delegate {

    public interface ClipRecyclerClickListener {
        public void copyClick(int position);

        public void clipClick(int position);

        public void OnLongClick(int position);

    }
}
