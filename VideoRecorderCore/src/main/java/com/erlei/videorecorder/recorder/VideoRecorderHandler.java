package com.erlei.videorecorder.recorder;

import android.os.Handler;
import android.os.Message;

import com.erlei.videorecorder.encoder.MuxerCallback;

public class VideoRecorderHandler extends Handler implements MuxerCallback {

    protected static final int MSG_UPDATE_FPS = 1;
    protected static final int MSG_MEDIA_MUXER_STOPPED = 2;
    protected static final int MSG_MEDIA_MUXER_START = 3;
    protected static final int MSG_MEDIA_CAPTURE_START = 4;
    protected static final int MSG_MEDIA_CAPTURE_STOPPED = 5;


    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case MSG_UPDATE_FPS:
                handleUpdateFPS((float) msg.obj);
                break;
            case MSG_MEDIA_MUXER_STOPPED:
                handleVideoMuxerStopped((String) msg.obj);
                break;
            case MSG_MEDIA_MUXER_START:
                handleVideoMuxerStarted((String) msg.obj);
                break;
            case MSG_MEDIA_CAPTURE_START:
                handleMediaCaptureStarted((String) msg.obj);
                break;
            case MSG_MEDIA_CAPTURE_STOPPED:
                handleMediaCaptureStopped((String) msg.obj);
                break;
        }
    }

    /**
     * 停止捕捉音视频数据
     *
     * @param output 本次录制的视频文件路径
     * @see VideoRecorderHandler#handleMediaCaptureStarted(java.lang.String)
     */
    protected void handleMediaCaptureStopped(String output) {

    }

    /**
     * 开始捕捉音视频数据
     *
     * @param output 本次录制的视频文件路径
     * @see VideoRecorderHandler#handleMediaCaptureStopped(java.lang.String)
     */
    protected void handleMediaCaptureStarted(String output) {

    }

    /**
     * 开始合并音视频编码数据
     *
     * @param output 本次录制的视频文件路径
     *               <p>
     *               注意 这个方法和 handleVideoMuxerStopped 并不总是成对的 , 有一种异常情况是
     *               调用了startRecord 之后立刻调用 stopRecord ,会导致不会调用handleVideoMuxerStarted
     *               这种情况是因为调用了startRecord 之后并不能立马开始混合音视频编码数据 ,
     *               需要等待混合添加跟踪轨道之后才能开启开启混合器 , 所以如果在开始录制之后立即停止录制,
     *               导致接收不到VideoRecorderHandler.MSG_MEDIA_MUXER_START
     *               消息 , 导致没有调用 handleVideoMuxerStarted()
     * @see VideoRecorderHandler#handleVideoMuxerStopped(java.lang.String)
     */
    protected void handleVideoMuxerStarted(String output) {

    }


    /**
     * 停止合并音视频编码数据
     *
     * @param output 本次录制的视频文件路径
     *               <p>
     *               注意 这个方法和 handleVideoMuxerStarted 并不总是成对的 , 有一种异常情况是
     *               调用了startRecord 之后立刻调用 stopRecord ,会导致不会调用handleVideoMuxerStarted
     *               这种情况是因为调用了startRecord 之后并不能立马开始混合音视频编码数据 ,
     *               需要等待混合添加跟踪轨道之后才能开启开启混合器 , 所以如果在开始录制之后立即停止录制,
     *               导致接收不到VideoRecorderHandler.MSG_MEDIA_MUXER_START
     *               消息 , 导致没有调用 handleVideoMuxerStarted()
     * @see VideoRecorderHandler#handleVideoMuxerStarted(java.lang.String)
     */
    protected void handleVideoMuxerStopped(String output) {

    }

    protected void handleUpdateFPS(float obj) {

    }


    protected void updateFPS(float fps) {
        sendMessage(obtainMessage(MSG_UPDATE_FPS, fps));
    }

    @Override
    public void onPrepared() {

    }

    @Override
    public void onMuxerStarted(String output) {
        sendMessage(obtainMessage(MSG_MEDIA_MUXER_START, output));
    }

    @Override
    public void onMuxerStopped(String output) {
        sendMessage(obtainMessage(MSG_MEDIA_MUXER_STOPPED, output));
    }

    public void onCaptureStarted(String output) {
        sendMessage(obtainMessage(MSG_MEDIA_CAPTURE_START, output));
    }

    public void onCaptureStopped(String output) {
        sendMessage(obtainMessage(MSG_MEDIA_CAPTURE_STOPPED, output));
    }
}
