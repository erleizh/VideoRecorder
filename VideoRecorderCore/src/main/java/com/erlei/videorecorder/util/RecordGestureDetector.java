package com.erlei.videorecorder.util;


import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * 点击拍摄 , 长按录制手势处理器
 */
public class RecordGestureDetector {

    private static final int LONG_PRESS = 1;
    private static final long LONG_PRESS_TIMEOUT = 500;
    private final GestureHandler mHandler;
    private final OnGestureListener mListener;
    private MotionEvent mCurrentDownEvent;
    private boolean mInLongPress;

    public interface OnGestureListener {

        void onLongPressDown(View view, MotionEvent e);

        void onLongPressUp(View view, MotionEvent e);

        void onSingleTap(View view, MotionEvent e);

    }


    public static class SimpleOnGestureListener implements RecordGestureDetector.OnGestureListener {

        @Override
        public void onLongPressDown(View view, MotionEvent e) {

        }

        @Override
        public void onLongPressUp(View view, MotionEvent e) {

        }

        @Override
        public void onSingleTap(View view, MotionEvent e) {

        }
    }

    private static class GestureHandler extends Handler {

        private WeakReference<RecordGestureDetector> mWeakReference;

        GestureHandler(RecordGestureDetector gestureDetector) {
            super();
            mWeakReference = new WeakReference<>(gestureDetector);
        }

        GestureHandler(RecordGestureDetector gestureDetector, Handler handler) {
            super(handler.getLooper());
            mWeakReference = new WeakReference<>(gestureDetector);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LONG_PRESS:
                    if (mWeakReference.get() != null) {
                        mWeakReference.get().dispatchLongPress((View) msg.obj);
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown message " + msg); //never
            }
        }
    }

    public RecordGestureDetector(RecordGestureDetector.OnGestureListener listener) {
        this(listener, null);
    }


    public RecordGestureDetector(RecordGestureDetector.OnGestureListener listener, Handler handler) {
        if (handler != null) {
            mHandler = new GestureHandler(this, handler);
        } else {
            mHandler = new GestureHandler(this);
        }
        mListener = listener;

        if (mListener == null) {
            throw new NullPointerException("OnGestureListener must not be null");
        }
    }


    public boolean onTouchEvent(View v, MotionEvent ev) {
        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mCurrentDownEvent = MotionEvent.obtain(ev);
                mHandler.removeMessages(LONG_PRESS);
                mHandler.sendMessageAtTime(Message.obtain(mHandler, LONG_PRESS, v), mCurrentDownEvent.getDownTime() + LONG_PRESS_TIMEOUT);
                break;
            case MotionEvent.ACTION_UP:
                if (mInLongPress) {
                    mInLongPress = false;
                    mListener.onLongPressUp(v, ev);
                } else {
                    mListener.onSingleTap(v, ev);
                }
                mHandler.removeMessages(LONG_PRESS);
                break;
            case MotionEvent.ACTION_CANCEL:
                cancel();
                break;
        }
        return false;
    }

    private void cancel() {
        mHandler.removeMessages(LONG_PRESS);
    }


    private void dispatchLongPress(View view) {
        mInLongPress = true;
        mListener.onLongPressDown(view, mCurrentDownEvent);
    }
}
