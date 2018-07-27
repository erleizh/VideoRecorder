package com.erlei.videorecorder.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

public interface MediaEncoderCallBack {

    String getOutPutPath();

    void onPrepared(MediaEncoder mediaEncoder);
    void onStopped(MediaEncoder mediaEncoder);

    void sendEncodedData(int mediaTrack, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo);

    int addMediaTrack(MediaEncoder encoder, MediaFormat format);
}