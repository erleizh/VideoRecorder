package com.erlei.videorecorder.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;

import com.erlei.videorecorder.camera.Camera;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.recorder.CameraController;

public interface CameraGLView extends CameraController {

    Size getCameraSize();

    Size getSurfaceSize();

    int getVisibility();

    boolean openCamera(SurfaceTexture texture);

    void closeCamera();

    void setCameraBuilder(Camera.CameraBuilder cameraBuilder);

    CameraTextureCallback getCameraTextureCallBack();

    void setCameraTextureCallBack(CameraTextureCallback callBack);

    void requestRender();

    int getCameraOrientation();

    int getDisplayOrientation();

    Context getContext();

    boolean isEnabled();

    /**
     * @return camera is front
     */
    boolean isFront();

    void updateCameraParams(Camera.CameraBuilder builder);
}
