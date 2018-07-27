package com.erlei.videorecorder.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.util.LogUtil;

import java.io.IOException;
import java.util.Locale;

public class VideoEncoder extends MediaEncoder {

    private static final String MIME_TYPE = "video/avc";
    private static final String TAG = "VideoEncoder";
    private static final int I_FRAME_INTERVAL = 10;
    private static final int FRAME_RATE = 30;
    private static final float BPP = 0.25f;
    private final Size mVideoSize;
    private final int mBitRate;
    private final int mIFrameInterval;
    private Surface mInputSurface;

    public VideoEncoder(MediaEncoderCallBack callBack, Size size, int bitRate,int iFrameInterval) {
        super(callBack, TAG);
        mVideoSize = size;
        mIFrameInterval = iFrameInterval;
        mBitRate = bitRate <= 0 ? calcBitRate() : bitRate;
    }


    @Override
    protected synchronized MediaEncoderHandler initHandler(Looper looper, MediaEncoder encoder) {
        return new VideoEncoderHandler(getLooper(), this);
    }


    @Override
    protected MediaCodec createEncoder() throws IOException {
        LogUtil.logd(TAG, "createEncoder");
        MediaFormat videoFormat = MediaFormat.createVideoFormat(MIME_TYPE, mVideoSize.getWidth(), mVideoSize.getHeight());
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameInterval);
        LogUtil.logd(TAG, "format: " + videoFormat);


        MediaCodec encoder = MediaCodec.createEncoderByType(MIME_TYPE);
        encoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = encoder.createInputSurface();
        encoder.start();
        LogUtil.logd(TAG, "createEncoder finishing");
        return encoder;
    }

    @Override
    protected void signalEndOfInputStream() {
        mEncoder.signalEndOfInputStream();
    }

    public Surface getInputSurface() {
        return mInputSurface;
    }

    private int calcBitRate() {
        final int bitrate = (int) (BPP * FRAME_RATE * mVideoSize.getWidth() * mVideoSize.getHeight());
        LogUtil.logd(TAG, String.format(Locale.getDefault(), "bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
    }

    private class VideoEncoderHandler extends MediaEncoderHandler {

        VideoEncoderHandler(Looper looper, VideoEncoder videoEncoder) {
            super(looper, videoEncoder);
        }

        @Override
        protected void handleMessage(MediaEncoder encoder, Message msg) {

        }
    }
}
