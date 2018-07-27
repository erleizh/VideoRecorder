package com.erlei.videorecorder.encoder;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntRange;

import com.erlei.videorecorder.util.LogUtil;

import java.io.IOException;

public class AudioEncoder extends MediaEncoder {
    private static final String TAG = "AudioEncoder";
    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int DEFAULT_SAMPLE_RATE = 44100;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int DEFAULT_BIT_RATE = 64000;
    private static final int DEFAULT_NUMBER_OF_CHANNELS = 1;
    private int mSampleRate;
    private int mBitRate;
    private int mChannelCount;


    public AudioEncoder(MediaEncoderCallBack callBack) {
        this(callBack, DEFAULT_SAMPLE_RATE, DEFAULT_BIT_RATE, DEFAULT_NUMBER_OF_CHANNELS);
    }

    public AudioEncoder(MediaEncoderCallBack callBack, int sampleRate, int bitRate, @IntRange(from = 1, to = 2) int channelCount) {
        super(callBack, TAG);
        mSampleRate = sampleRate;
        mBitRate = bitRate;
        mChannelCount = channelCount;
    }

    @Override
    protected long getSafePTSUs(long presentationTimeUs) {
//        return getJitterFreePTS(presentationTimeUs,3552);
        long result = System.nanoTime() / 1000L;
        if (result < mPrevOutputPTSUs)
            result = (mPrevOutputPTSUs - result) + result;
        return result;
    }
    long startPTS = 0;
    long totalSamplesNum = 0;
    private long getJitterFreePTS(long bufferPts, long bufferSamplesNum) {
        long correctedPts = 0;
        long bufferDuration = (1000000 * bufferSamplesNum) / (mSampleRate);
        bufferPts -= bufferDuration; // accounts for the delay of acquiring the audio buffer
        if (totalSamplesNum == 0) {
            // reset
            startPTS = bufferPts;
            totalSamplesNum = 0;
        }
        correctedPts = startPTS +  (1000000 * totalSamplesNum) / (mSampleRate);
        if(bufferPts - correctedPts >= 2*bufferDuration) {
            // reset
            startPTS = bufferPts;
            totalSamplesNum = 0;
            correctedPts = startPTS;
        }
        totalSamplesNum += bufferSamplesNum;
        return correctedPts;
    }

    @Override
    protected synchronized MediaEncoderHandler initHandler(Looper looper, MediaEncoder encoder) {
        return new AudioEncoderHandler(looper, encoder);
    }


    @Override
    protected MediaCodec createEncoder() throws IOException {
        LogUtil.logd(TAG, "createEncoder");
        final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, mSampleRate, mChannelCount);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, mChannelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);
        LogUtil.loge(TAG, "format: " + audioFormat);
        MediaCodec encoder = MediaCodec.createEncoderByType(MIME_TYPE);
        encoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();
        LogUtil.logd(TAG, "createEncoder finishing");
        return encoder;
    }

    private class AudioEncoderHandler extends MediaEncoderHandler {
        AudioEncoderHandler(Looper looper, MediaEncoder encoder) {
            super(looper, encoder);
        }

        @Override
        protected void handleMessage(MediaEncoder encoder, Message msg) {

        }
    }
}
