package com.erlei.videorecorder.recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.MediaRecorder;

import com.erlei.videorecorder.encoder.AudioEncoder;
import com.erlei.videorecorder.encoder.MediaEncoder;
import com.erlei.videorecorder.util.LogUtil;

import java.nio.ByteBuffer;

public class AudioCaptureThread extends Thread {

    private static final String TAG = "AudioCaptureThread";

    private static final int SAMPLES_PER_FRAME = 1024;    // AAC, bytes/frame/channel
    private static final int FRAMES = 25;    // AAC, frame/buffer/sec
    private static final int SAMPLE_RATE = 44100;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private int mSampleRate = SAMPLE_RATE;
    private int mFrames = FRAMES;
    private int mChannelConfig;
    private final AudioEncoder mAudioEncoder;
    private volatile boolean mCapture = true;


    public AudioCaptureThread(AudioEncoder audioEncoder, int frames) {
        this(audioEncoder, SAMPLE_RATE, frames, 1);
    }

    public AudioCaptureThread(AudioEncoder encoder, int sampleRate, int frames, int channelCount) {
        mAudioEncoder = encoder;
        mSampleRate = sampleRate;
        mFrames = frames;
        mChannelConfig = channelCount == 2 ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
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
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        try {
            final int minBufferSize = AudioRecord.getMinBufferSize(
                    mSampleRate,
                    mChannelConfig,
                    AudioFormat.ENCODING_PCM_16BIT);

            AudioRecord audioRecord = null;
            for (final int source : AUDIO_SOURCES) {
                try {
                    audioRecord = new AudioRecord(
                            source,
                            mSampleRate,
                            mChannelConfig,
                            AudioFormat.ENCODING_PCM_16BIT,
                            minBufferSize * 2);

                    if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                        audioRecord.release();
                        audioRecord = null;
                    }

                } catch (final Exception e) {
                    e.printStackTrace();
                    audioRecord = null;
                    LogUtil.loge(TAG, "new AudioRecord with source " + source + "error " + e);
                }
                if (audioRecord != null) break;
            }
            if (audioRecord != null) {
                try {
                    if (!isInterrupted() && mCapture) {
                        LogUtil.logd(TAG, "AudioThread:startRecord audio recording");
                        final ByteBuffer buf = ByteBuffer.allocateDirect(minBufferSize / 2);
                        int readBytes;
                        audioRecord.startRecording();
                        try {
                            MediaEncoder.MediaEncoderHandler handler = mAudioEncoder.getHandler();
                            long lastMillis = System.currentTimeMillis();
                            while (!isInterrupted() && mCapture) {
                                // read audio data from internal mic
                                buf.clear();
                                readBytes = audioRecord.read(buf, minBufferSize / 2);
                                if (readBytes > 0) {
                                    // set audio data to encoder
                                    buf.position(readBytes);
                                    buf.flip();
                                    LogUtil.logd(TAG, "AudioCapture send count " + ++mFrameCount + "\t\t readBytes = " + readBytes + "\t " + (System.currentTimeMillis() - lastMillis) + "ms");
                                    handler.encode(buf, readBytes, System.nanoTime() / 1000L);
                                    lastMillis = System.currentTimeMillis();
                                    handler.frameAvailableSoon();
                                }
                            }
                            handler.encode(null, 0, System.nanoTime() / 1000L);
                        } finally {
                            audioRecord.stop();
                        }
                    }
                } finally {
                    audioRecord.release();
                }
            } else {
                LogUtil.loge(TAG, "failed to initialize AudioRecord");
            }
        } catch (final Exception e) {
            LogUtil.loge(TAG, "AudioThread#run" + e);
        }
        LogUtil.loge(TAG, "AudioThread:finished");
    }

    public void setCapture(boolean capture) {
        mCapture = capture;
    }

    public boolean isCapture() {
        return mCapture;
    }
}
