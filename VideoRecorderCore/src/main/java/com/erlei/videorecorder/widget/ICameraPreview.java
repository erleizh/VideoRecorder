package com.erlei.videorecorder.widget;

import android.graphics.SurfaceTexture;

import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.videorecorder.camera.Size;

interface ICameraPreview extends IRenderView {


    void startPreview(SurfaceTexture texture);

    void stopPreview();

    Size getSurfaceSize();
}
