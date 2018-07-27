package com.erlei.videorecorder.recorder;

import android.content.Context;

import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.gles.EglCore;
import com.erlei.videorecorder.gles.OffscreenSurface;

public class OffscreenCameraPreview implements ICameraPreview {

    private final Context mContext;
    private final int mWidth;
    private final int mHeight;
    private OffscreenSurface mOffscreenSurface;

    public OffscreenCameraPreview(Context context, int width, int height) {
        mContext = context;
        mWidth = width;
        mHeight = height;
    }

    @Override
    public Size getSurfaceSize() {
        return new Size(mWidth, mHeight);
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    /**
     * @param eglCore
     * @return object
     * if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture)) {
     * throw new RuntimeException("invalid surface: " + surface);
     * }
     */
    @Override
    public synchronized Object getSurface(EglCore eglCore) {
        if (mOffscreenSurface == null)
            mOffscreenSurface = new OffscreenSurface(eglCore, mWidth, mHeight);
        return mOffscreenSurface;
    }
}
