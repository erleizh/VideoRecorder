package com.erlei.videorecorder.camera.annotations;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({
        android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK,
        android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT})
@Retention(RetentionPolicy.SOURCE)
public @interface Facing {
}