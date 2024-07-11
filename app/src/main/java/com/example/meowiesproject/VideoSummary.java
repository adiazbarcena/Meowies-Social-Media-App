package com.example.meowiesproject;

import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.meowiesproject.R;

import java.util.HashMap;
import java.util.Map;

public class VideoSummary {

    private String videoId;
    private String thumbnailUri;
    private Long watchCount;

    private String videoUrl;

    public String getVideoUrl() {
        return videoUrl;
    }

    public VideoSummary(String videoId, String thumbnailUri, String videoUrl) {
        this.videoId = videoId;
        this.thumbnailUri = thumbnailUri;
        this.videoUrl = videoUrl;

    }

    public VideoSummary() {
    }

    public String getVideoId() {
        return videoId;
    }

    public String getThumbnailUri() {
        return thumbnailUri;
    }

    public Long getWatchCount() {
        return watchCount;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("videoId", videoId);
        result.put("thumbnailUri", thumbnailUri);


        return result;
    }
}