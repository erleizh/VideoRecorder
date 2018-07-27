package com.erlei.videorecorder.encoder1;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaAudioEncoder.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 */

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.erlei.videorecorder.recorder.VideoRecorder;
import com.erlei.videorecorder.util.LogUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaAudioEncoder extends MediaEncoder {
    private static final boolean DEBUG = LogUtil.LOG_ENABLE;    // TODO set false on release
    private static final String TAG = LogUtil.TAG;
    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int DEFAULT_SAMPLE_RATE = 44100;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int DEFAULT_BIT_RATE = 64000;
    private static final int DEFAULT_NUMBER_OF_CHANNELS = 1;
    private int mSampleRate;
    private int mBitRate;
    private int mChannelCount;
    private AudioThread mAudioThread = null;
    private VideoRecorder.Config mConfig;

    public MediaAudioEncoder(MediaMuxerWrapper muxer, VideoRecorder.Config config) {
        super(muxer, config);
        mConfig = config;
        mSampleRate = config.getAudioSampleRate();
        mBitRate = config.getAudioBitRate();
        mChannelCount = config.getAudioChannelCount();
    }

    @Override
    protected void prepare() throws IOException {
        if (DEBUG) Log.v(TAG, "prepare:");
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;
        // prepare MediaCodec for AAC encoding of audio data from inernal mic.
        final MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if (audioCodecInfo == null) {
            LogUtil.loge(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        if (DEBUG) LogUtil.loge(TAG, "selected codec: " + audioCodecInfo.getName());

        final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, mSampleRate, mChannelCount);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, mChannelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);
        if (DEBUG) LogUtil.loge(TAG, "format: " + audioFormat);
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        if (DEBUG) LogUtil.loge(TAG, "prepare finishing");
    }

    @Override
    protected void startRecording() {
        super.startRecording();
        // create and execute audio capturing thread using internal mic
        if (mAudioThread == null) {
            mAudioThread = new AudioThread();
            mAudioThread.start();
        }
    }

    @Override
    protected void release() {
        mAudioThread = null;
        super.release();
    }

    private static final int[] AUDIO_SOURCES = new int[]{
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
    };

    /**
     * Thread to capture audio data from internal mic as uncompressed 16bit PCM data
     * and write them to the MediaCodec encoder
     */
    private class AudioThread extends Thread {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            try {
                final int minBufferSize = AudioRecord.getMinBufferSize(
                        mSampleRate,
                        mChannelCount,
                        AudioFormat.ENCODING_PCM_16BIT);


                AudioRecord audioRecord = null;
                for (final int source : AUDIO_SOURCES) {
                    try {
                        audioRecord = new AudioRecord(
                                source, mSampleRate,
                                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize * 2);
                        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
                            audioRecord = null;
                    } catch (final Exception e) {
                        audioRecord = null;
                    }
                    if (audioRecord != null) break;
                }
                if (audioRecord != null) {
                    try {
                        if (mIsCapturing) {
                            if (DEBUG) Log.v(TAG, "AudioThread:start audio recording");
                            final ByteBuffer buf = ByteBuffer.allocateDirect(minBufferSize / 2);
                            int readBytes;
                            audioRecord.startRecording();
                            try {
                                for (; mIsCapturing && !mRequestStop && !mIsEOS; ) {
                                    // read audio data from internal mic
                                    buf.clear();
                                    readBytes = audioRecord.read(buf, minBufferSize / 2);
                                    if (readBytes > 0) {
                                        // set audio data to encoder
                                        buf.position(readBytes);
                                        buf.flip();
                                        encode(buf, readBytes, getPTSUs());
                                        frameAvailableSoon();
                                    }
                                }
                                frameAvailableSoon();
                            } finally {
                                audioRecord.stop();
                            }
                        }
                    } finally {
                        audioRecord.release();
                    }
                } else {
                    Log.e(TAG, "failed to initialize AudioRecord");
                }
            } catch (final Exception e) {
                Log.e(TAG, "AudioThread#run", e);
            }
            if (DEBUG) Log.v(TAG, "AudioThread:finished");
        }
    }

    /**
     * select the first codec that match a specific MIME type
     *
     * @param mimeType
     * @return
     */
    private static final MediaCodecInfo selectAudioCodec(final String mimeType) {
        if (DEBUG) Log.v(TAG, "selectAudioCodec:");

        MediaCodecInfo result = null;
        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        LOOP:
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (DEBUG)
                    LogUtil.loge(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (result == null) {
                        result = codecInfo;
                        break LOOP;
                    }
                }
            }
        }
        return result;
    }

}
