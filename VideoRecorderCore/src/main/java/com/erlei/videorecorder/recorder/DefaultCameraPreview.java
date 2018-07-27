package com.erlei.videorecorder.recorder;

import android.content.Context;
import android.graphics.Rect;
import android.view.SurfaceView;
import android.view.TextureView;

import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.gles.EglCore;

public class DefaultCameraPreview implements ICameraPreview {

    protected SurfaceView mSurfaceView;
    protected TextureView mTextureView;
    protected Context mContext;

    public DefaultCameraPreview(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
        mContext = surfaceView.getContext();
    }

    public DefaultCameraPreview(TextureView textureView) {
        mTextureView = textureView;
        mContext = textureView.getContext();
    }


    @Override
    public Size getSurfaceSize() {
        if (mSurfaceView != null) {
            Rect surfaceFrame = mSurfaceView.getHolder().getSurfaceFrame();
            return new Size(surfaceFrame.width(), surfaceFrame.height());
        } else if (mTextureView != null) {
            return new Size(mTextureView.getWidth(), mTextureView.getHeight());
        }
        return new Size(0, 0);
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    @Override
    public Object getSurface(EglCore eglCore) {
        if (mSurfaceView != null) {
            return mSurfaceView.getHolder().getSurface();
        } else if (mTextureView != null) {
            return mTextureView.getSurfaceTexture();
        }
        return null;
    }

}
