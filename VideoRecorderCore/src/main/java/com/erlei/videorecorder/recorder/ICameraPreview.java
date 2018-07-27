package com.erlei.videorecorder.recorder;

import android.content.Context;

import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.gles.EglCore;

public interface ICameraPreview {

    Size getSurfaceSize();

    Context getContext();

    /**
     * @return object
     * if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture)) {
     *  throw new RuntimeException("invalid surface: " + surface);
     * }
     * @param eglCore
     */
    Object getSurface(EglCore eglCore);
}
