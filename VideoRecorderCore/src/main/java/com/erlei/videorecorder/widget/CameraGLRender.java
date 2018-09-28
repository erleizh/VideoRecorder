package com.erlei.videorecorder.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.erlei.gdx.Gdx;
import com.erlei.gdx.android.EglCore;
import com.erlei.gdx.android.EglSurfaceBase;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.gdx.utils.Logger;
import com.erlei.videorecorder.camera.Camera;
import com.erlei.videorecorder.recorder.CameraController;

class CameraGLRender extends Gdx implements Camera.CameraCallback, SurfaceTexture.OnFrameAvailableListener {

    private CameraController mCamera;
    private Logger mLogger = new Logger("CameraGLRender");

    private SurfaceTexture mTexture;
    private int mOESTexture;

    CameraGLRender(Context context, IRenderView renderView, CameraController cameraController) {
        super(context, renderView);
        mCamera = cameraController;

    }

    @Override
    public void create(EglCore egl, EglSurfaceBase eglSurface) {
        super.create(egl, eglSurface);
        mLogger.debug("create");
        initSurfaceTexture();
        mCamera.openCamera(mTexture);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        mLogger.debug("resize(" + width + "x" + height + ")");
    }

    @Override
    public void resume() {
        super.resume();
        mLogger.debug("resume");
    }

    @Override
    public void pause() {
        super.pause();
        mLogger.debug("pause");
    }

    @Override
    public void dispose() {
        super.dispose();
        mLogger.debug("dispose");
    }

    @Override
    public void onSuccess(Camera camera) {
        mLogger.info("open camera success");
    }

    @Override
    public void onFailure(int code, String msg) {
        mLogger.error("open camera failure : " + msg);
    }


    private void deleteSurfaceTexture() {
        mLogger.debug("deleteSurfaceTexture");
        if (mTexture != null) {
            mTexture.release();
            mTexture = null;
            gl.glDeleteTexture(mOESTexture);
        }
    }

    protected void initSurfaceTexture() {
        mLogger.debug("initSurfaceTexture");
        deleteSurfaceTexture();
        mOESTexture = gl.glGenTexture();
        gl.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mOESTexture);
        gl.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        gl.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        mTexture = new SurfaceTexture(mOESTexture);
        mTexture.setOnFrameAvailableListener(this);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mRenderView.requestRender();
    }

    public void setCameraController(CameraController cameraController) {
        mCamera = cameraController;
    }
}
