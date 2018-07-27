package com.erlei.videorecorder.encoder;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.erlei.videorecorder.util.LogUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public abstract class MediaEncoder extends HandlerThread {
    private static final int TIMEOUT_USEC = 5000;
    protected MediaCodec mEncoder;
    private final String TAG;
    protected long mPrevOutputPTSUs = 0;
    protected MediaCodec.BufferInfo mBufferInfo;
    protected MediaEncoderHandler mHandler;
    protected MediaEncoderCallBack mMediaEncoderCallBack;
    protected int mMediaTrack;

    public MediaEncoder(MediaEncoderCallBack callBack, String name) {
        super(name);
        mMediaEncoderCallBack = callBack;
        TAG = name;
    }

    public synchronized MediaEncoderHandler getHandler() {
        if (!isAlive()) return null;
        if (mHandler == null) {
            mHandler = initHandler(getLooper(), this);
        }
        return mHandler;
    }

    protected synchronized MediaEncoderHandler initHandler(Looper looper, MediaEncoder encoder) {
        return new MediaEncoderHandler(looper, encoder);
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        LogUtil.logd(TAG, TAG + " thread prepared");
        mBufferInfo = new MediaCodec.BufferInfo();
        try {
            mEncoder = createEncoder();
        } catch (IOException e) {
            e.printStackTrace();
            LogUtil.logd(TAG, "createEncoder error " + e.toString());
        }
        if (mMediaEncoderCallBack != null) mMediaEncoderCallBack.onPrepared(this);
    }

    protected void release() {
        LogUtil.logd(TAG, "release encoder");
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
    }

    protected void stopMediaEncoder() {
        signalEndOfInputStream();
        release();
        if (mMediaEncoderCallBack != null) mMediaEncoderCallBack.onStopped(this);
        quit();
    }

    private int mFrameIndex = 0;

    protected void encode(ByteBuffer buffer, int length, long presentationTimeUs) {
        final ByteBuffer[] inputBuffers = mEncoder.getInputBuffers();
        while (true) {
            final int inputBufferIndex = mEncoder.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }
                if (length <= 0) {
                    // send EOS
                    LogUtil.logi(TAG, "send BUFFER_FLAG_END_OF_STREAM");

                } else {
                    mEncoder.queueInputBuffer(inputBufferIndex, 0, length, presentationTimeUs, 0);
                }
                break;
            }
        }
    }

    protected void frameAvailableSoon() {
        drain();
    }

    /**
     * 发送结束信号
     * 如果视频编码器使用的是 COLOR_FormatSurface , 那么必须使用 mEncoder.signalEndOfInputStream();
     * 所以此处VideoEncoder 重载了 signalEndOfInputStream() 方法
     */
    protected void signalEndOfInputStream() {
        LogUtil.logd(TAG, "sending EOS to encoder");
        encode(null, 0, getSafePTSUs(System.nanoTime() / 1000L));
    }

    private void drain() {
//        LogUtil.logd(TAG, "drainEncoder()");
        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
//        while (true) {
        int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
            // no output available yet
//            LogUtil.logd(TAG, "no output available, spinning to await EOS");
        } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            LogUtil.logd(TAG, "info output buffers changed");
            // not expected for an encoder
            encoderOutputBuffers = mEncoder.getOutputBuffers();
        } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            //在这里获取的 MediaFormat 包含了 csd-0 csd-1 的数据
            LogUtil.logd(TAG, "info output format changed");
            MediaFormat format = mEncoder.getOutputFormat();
            mMediaTrack = mMediaEncoderCallBack.addMediaTrack(this, format);
            LogUtil.loge(TAG, "OutputFormat = " + format.toString());
        } else if (encoderStatus < 0) {
            Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
            // let's ignore it
        } else {
            ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
            if (encodedData == null) {
                throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
            }

            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                // The codec config data was pulled out and fed to the muxer when we got
                // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                LogUtil.logd(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                mBufferInfo.size = 0;
            }

            if (mBufferInfo.size != 0) {
                // adjust the ByteBuffer values to match BufferInfo (not needed?)
                encodedData.position(mBufferInfo.offset);
                encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
//                mBufferInfo.presentationTimeUs = getSafePTSUs(mBufferInfo.presentationTimeUs);
                mMediaEncoderCallBack.sendEncodedData(mMediaTrack, encodedData, mBufferInfo);
                mPrevOutputPTSUs = mBufferInfo.presentationTimeUs;
                LogUtil.logd(TAG, "sent  ts " + mBufferInfo.presentationTimeUs + " ,\t\t\tframeIndex = " + ++mFrameCount + "\t\t\t bytes = " + mBufferInfo.size);
            }

            mEncoder.releaseOutputBuffer(encoderStatus, false);

            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                LogUtil.logd(TAG, "end of stream reached");
//                    break;      // out of while
            }
//            }
        }
    }

    private int mFrameCount;


    protected long getSafePTSUs(long presentationTimeUs) {
        long result = System.nanoTime() / 1000L;
        if (result < mPrevOutputPTSUs)
            result = (mPrevOutputPTSUs - result) + result;
        return result;
    }

    protected abstract MediaCodec createEncoder() throws IOException;

    public class MediaEncoderHandler extends Handler {
        protected static final int MSG_FRAME_AVAILABLE_SOON = 1;
        protected static final int MSG_STOP_MEDIA_ENCODER = 2;
        protected static final int MSG_ENCODE_FRAME = 3;

        private final WeakReference<MediaEncoder> mReference;

        protected MediaEncoderHandler(Looper looper, MediaEncoder encoder) {
            super(looper);
            mReference = new WeakReference<>(encoder);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MediaEncoder encoder = mReference.get();
            if (encoder == null) {
                LogUtil.loge(TAG, "handleMessage: weak ref is null");
                return;
            }
            switch (msg.what) {
                case MSG_FRAME_AVAILABLE_SOON:
                    encoder.frameAvailableSoon();
                    break;
                case MSG_STOP_MEDIA_ENCODER:
                    encoder.stopMediaEncoder();
                    break;
                case MSG_ENCODE_FRAME:
                    encoder.encode((ByteBuffer) msg.obj, msg.arg1, msg.getData().getLong("presentationTimeUs"));
                    break;
                default:
                    handleMessage(encoder, msg);
                    break;
            }
        }

        protected void handleMessage(MediaEncoder encoder, Message msg) {

        }

        public void stopMediaEncoder() {
            sendMessage(obtainMessage(MSG_STOP_MEDIA_ENCODER));
        }

        public void frameAvailableSoon() {
            sendMessage(obtainMessage(MSG_FRAME_AVAILABLE_SOON));
        }

        public void encode(ByteBuffer buf, int readBytes, long presentationTimeUs) {
            Message msg = obtainMessage(MSG_ENCODE_FRAME, readBytes, 0, buf);
            Bundle data = new Bundle();
            data.putLong("presentationTimeUs",presentationTimeUs);
            msg.setData(data);
            sendMessage(msg);
        }
    }
}
