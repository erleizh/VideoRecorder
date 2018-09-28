package com.erlei.videorecorder.widget;

import android.content.Context;
import android.util.AttributeSet;

import com.erlei.gdx.android.widget.GLSurfaceView;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.gles.EglCore;
import com.erlei.videorecorder.recorder.CameraController;
import com.erlei.videorecorder.recorder.ICameraPreview;

public class CameraGLSurfaceView extends GLSurfaceView implements ICameraPreview {

    private CameraController mCameraController;
    private CameraGLRender mCameraGLRender;

    public CameraGLSurfaceView(Context context) {
        super(context);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        super.init();
        mCameraGLRender = new CameraGLRender(getContext(), this, mCameraController);
        setRenderer(mCameraGLRender);
        setRenderMode(RenderMode.WHEN_DIRTY);
    }


    public CameraController getCameraController() {
        return mCameraController;
    }

    public void setCameraController(CameraController cameraController) {
        mCameraController = cameraController;
        mCameraGLRender.setCameraController(cameraController);
    }

    @Override
    public Size getSurfaceSize() {
        return new Size(getWidth(), getHeight());
    }

    @Override
    public Object getSurface(EglCore eglCore) {
        return getSurface();
    }
}
