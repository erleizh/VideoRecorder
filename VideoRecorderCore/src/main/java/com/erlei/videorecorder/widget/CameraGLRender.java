package com.erlei.videorecorder.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.SurfaceHolder;

import com.erlei.gdx.Gdx;
import com.erlei.gdx.android.EglCore;
import com.erlei.gdx.android.EglSurfaceBase;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.videorecorder.camera.Camera;

class CameraGLRender extends Gdx {

    private Camera mCamera;

    CameraGLRender(Context context, IRenderView renderView) {
        super(context, renderView);
    }

    @Override
    public void create(EglCore egl, EglSurfaceBase eglSurface) {
        super.create(egl, eglSurface);
        Camera.CameraBuilder builder = new Camera.CameraBuilder(getContext());
        builder.useDefaultConfig();
        if (mRenderView.getViewType() == IRenderView.ViewType.SurfaceView) {
            builder.setSurfaceHolder((SurfaceHolder) mRenderView.getSurface());
        } else if (mRenderView.getViewType() == IRenderView.ViewType.TextureView) {
            builder.setSurfaceTexture((SurfaceTexture) mRenderView.getSurface());
        }
        Camera camera = builder.build();
        camera.open();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void resume() {
        super.resume();
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
