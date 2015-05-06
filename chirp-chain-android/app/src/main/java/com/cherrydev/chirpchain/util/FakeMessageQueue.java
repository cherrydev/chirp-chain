package com.cherrydev.chirpchain.util;

import android.os.Handler;

import com.cherrydev.chirpchain.message.ChirpMessage;

import java.util.Date;

/**
 * Created by alannon on 2015-02-28.
 */
public class FakeMessageQueue {

    private Handler handler = new Handler();

    private int totalTime = 10000;
    private int updateInterval = 500;
    private Mode currentMode;
    private Date startTime;
    private ProgressCallback progressCallback;
    private CompleteCallback completeCallback;
    private ChirpMessage currentMessage;

    public enum Mode {
        send,
        receive
    }
    public void enqueueMessageForSend(ChirpMessage message) {
        currentMode = Mode.send;
        startTime = new Date();
        currentMessage = message;
        postProgress();
    }

    public void setProgressCallback(ProgressCallback progressCallback){
        this.progressCallback = progressCallback;
    }

    public void setCompleteCallback(CompleteCallback completeCallback) {
        this.completeCallback = completeCallback;
    }

    private void postProgress() {
        long now = new Date().getTime();
        int progressTime = (int) (now - startTime.getTime());
        float progress = ((float)progressTime) / totalTime;
        if (progress > 1) progress = 1;
        progressCallback.onProgress(currentMessage, currentMode, progress);
        if (progress < 1) {
            handler.postDelayed(this::postProgress, updateInterval);
        }
        else {
            completeCallback.onComplete(currentMessage, currentMode);
        }
    }

    public interface ProgressCallback {
        public void onProgress(ChirpMessage message, Mode mode, float progress);
    }

    public interface CompleteCallback {
        public void onComplete(ChirpMessage message, Mode mode);
    }
}
