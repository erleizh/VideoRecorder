package com.erlei.videorecorder.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;

import com.erlei.gdx.android.widget.GLSurfaceView;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.recorder.CameraController;

public class CameraGLSurfaceView extends GLSurfaceView implements ICameraPreview, IRenderView.SurfaceSizeChangeListener {

    private CameraGLRender mCameraGLRender;
    private CameraController mCameraController;
    private Size mSurfaceSize = new Size(0, 0);

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
        mCameraGLRender = new CameraGLRender(getContext(), this);
        setRenderer(mCameraGLRender);
        setRenderMode(RenderMode.WHEN_DIRTY);
        addSurfaceSizeChangeListener(this);
    }

    @Override
    public void startPreview(SurfaceTexture texture) {
        checkCameraControllerState();
        if (mCameraController.isOpen()) return;
        mCameraController.openCamera(texture);
    }

    private void checkCameraControllerState() {
        if (mCameraController == null) throw new IllegalStateException("Camera controller not set");
    }


    @Override
    public void stopPreview() {
        checkCameraControllerState();
        mCameraController.closeCamera();
    }


    @Override
    public Size getSurfaceSize() {
        return mSurfaceSize;
    }

    public CameraController getCameraController() {
        return mCameraController;
    }

    public void setCameraController(CameraController cameraController) {
        mCameraController = cameraController;
    }

    @Override
    public void onSizeChanged(int w, int h) {
        mSurfaceSize.setWidth(w);
        mSurfaceSize.setHeight(h);
    }

}
