package com.erlei.videorecorder.encoder;

import android.support.annotation.Nullable;

public interface MuxerCallback {

    void onPrepared();

    void onMuxerStarted(String output);

    void onMuxerStopped(String outPutPath);
}