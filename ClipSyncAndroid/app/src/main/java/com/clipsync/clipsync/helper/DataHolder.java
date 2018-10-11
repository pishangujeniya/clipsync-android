package com.clipsync.clipsync.helper;

public class DataHolder {

    public static class ClipSyncClipData {
        String clip_title;
        String clip_content;
        int clip_id;


        public String getClip_title() {
            return clip_title;
        }

        public void setClip_title(String clip_title) {
            this.clip_title = clip_title;
        }

        public String getClip_content() {
            return clip_content;
        }

        public void setClip_content(String clip_content) {
            this.clip_content = clip_content;
        }

        public int getClip_id() {
            return clip_id;
        }

        public void setClip_id(int clip_id) {
            this.clip_id = clip_id;
        }
    }
}
