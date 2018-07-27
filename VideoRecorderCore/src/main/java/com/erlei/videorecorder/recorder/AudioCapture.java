package com.erlei.videorecorder.recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.erlei.videorecorder.encoder.AudioEncoder;
import com.erlei.videorecorder.encoder.MediaEncoder;
import com.erlei.videorecorder.util.LogUtil;

import java.nio.ByteBuffer;

public class AudioCapture extends HandlerThread {

    private static final String TAG = "AudioCapture";


    private static final int MSG_READ_SAMPLES = 1;
    private static final int MSG_STOP_CAPTURE = 2;


    private static final int SAMPLES_PER_FRAME = 1024;    // AAC, bytes/frame/channel
    private static final int FRAMES = 25;    // AAC, frame/buffer/sec
    private static final int SAMPLE_RATE = 44100;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private final Handler mHandler;
    private int mSampleRate = SAMPLE_RATE;
    private int mFrames = FRAMES;
    private int mChannelConfig;
    private final AudioEncoder mAudioEncoder;
    private ByteBuffer mByteBuffer;
    private AudioRecord mAudioRecord;
    private int mMinBufferSize;


    public AudioCapture(AudioEncoder audioEncoder, int frames) {
        this(audioEncoder, SAMPLE_RATE, frames, 1);
    }

    public AudioCapture(AudioEncoder encoder, int sampleRate, int frames, int channelCount) {
        super(TAG);
        mAudioEncoder = encoder;
        mSampleRate = sampleRate;
        mFrames = frames;
        mChannelConfig = channelCount == 2 ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
        start();
        mHandler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_READ_SAMPLES:
                        readSamples();
                        break;

                    case MSG_STOP_CAPTURE:
                        handleStopCapture();
                        break;
                }
            }
        };
    }

    private static final int[] AUDIO_SOURCES = new int[]{
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
    };

    private int mFrameCount;

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        initAudioRecord();
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        super.run();
    }

    private void initAudioRecord() {
        mMinBufferSize = AudioRecord.getMinBufferSize(
                mSampleRate,
                mChannelConfig,
                AudioFormat.ENCODING_PCM_16BIT);

        mAudioRecord = null;
        for (final int source : AUDIO_SOURCES) {
            try {
                mAudioRecord = new AudioRecord(
                        source,
                        mSampleRate,
                        mChannelConfig,
                        AudioFormat.ENCODING_PCM_16BIT,
                        mMinBufferSize * mFrames / 2);

                if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    mAudioRecord.release();
                    mAudioRecord = null;
                }

            } catch (final Exception e) {
                e.printStackTrace();
                mAudioRecord = null;
                LogUtil.loge(TAG, "new AudioRecord with source " + source + "error " + e);
            }
            if (mAudioRecord != null) break;
        }
        if (mAudioRecord != null) {
            LogUtil.logd(TAG, "mAudioRecord.startRecording();");
            mByteBuffer = ByteBuffer.allocateDirect(mMinBufferSize / 2);
            mAudioRecord.startRecording();
//            int framePeriod = mSampleRate * (1000 / mFrames) / 1000;
            int framePeriod = 160;
            LogUtil.loge(TAG, "setPositionNotificationPeriod" + framePeriod);
            mAudioRecord.setPositionNotificationPeriod(framePeriod);
            mAudioRecord.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
                @Override
                public void onMarkerReached(AudioRecord audioRecord) {
                    LogUtil.logd(TAG, "onMarkerReached");
                }
                private long last;
                @Override
                public void onPeriodicNotification(AudioRecord audioRecord) {
                    long l = System.currentTimeMillis();
                    LogUtil.logd(TAG, "onPeriodicNotification "+(l - last));
                    mHandler.sendEmptyMessage(MSG_READ_SAMPLES);
                    last = l;

                }
            }, mHandler);
            mHandler.sendEmptyMessage(MSG_READ_SAMPLES);
        }

    }

    private void readSamples() {
        MediaEncoder.MediaEncoderHandler handler = mAudioEncoder.getHandler();
        if (handler == null) return;
        mByteBuffer.clear();
        int readBytes = mAudioRecord.read(mByteBuffer, mMinBufferSize / 2);
        if (readBytes > 0) {
            // set audio data to encoder
            mByteBuffer.position(readBytes);
            mByteBuffer.flip();
            LogUtil.logd(TAG, "AudioCapture send count " + ++mFrameCount + "\t\t readBytes = " + readBytes + "\t ");
            handler.encode(mByteBuffer, readBytes, System.nanoTime() / 1000L);
            handler.frameAvailableSoon();
        }
    }


    public void stopCapture() {
        mHandler.sendEmptyMessage(MSG_STOP_CAPTURE);
    }


    private void handleStopCapture() {
        MediaEncoder.MediaEncoderHandler handler = mAudioEncoder.getHandler();
        if (handler == null) return;
        handler.encode(null, 0, System.nanoTime() / 1000L);
        try {
            mAudioRecord.stop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mAudioRecord.release();
        }
        quitSafely();
        LogUtil.loge(TAG, "quit");
    }

}
