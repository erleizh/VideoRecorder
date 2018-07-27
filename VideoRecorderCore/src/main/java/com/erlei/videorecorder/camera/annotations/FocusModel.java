package com.erlei.videorecorder.camera.annotations;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@StringDef({
        android.hardware.Camera.Parameters.FOCUS_MODE_AUTO,
        android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
        android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
        android.hardware.Camera.Parameters.FOCUS_MODE_EDOF,
        android.hardware.Camera.Parameters.FOCUS_MODE_FIXED,
        android.hardware.Camera.Parameters.FOCUS_MODE_INFINITY,
        android.hardware.Camera.Parameters.FOCUS_MODE_MACRO,})
@Retention(RetentionPolicy.SOURCE)
public @interface FocusModel {
}