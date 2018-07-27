package com.erlei.videorecorder.camera.annotations;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@StringDef({
        android.hardware.Camera.Parameters.EFFECT_AQUA,
        android.hardware.Camera.Parameters.EFFECT_BLACKBOARD,
        android.hardware.Camera.Parameters.EFFECT_MONO,
        android.hardware.Camera.Parameters.EFFECT_NEGATIVE,
        android.hardware.Camera.Parameters.EFFECT_NONE,
        android.hardware.Camera.Parameters.EFFECT_POSTERIZE,
        android.hardware.Camera.Parameters.EFFECT_SEPIA,
        android.hardware.Camera.Parameters.EFFECT_SOLARIZE,
        android.hardware.Camera.Parameters.EFFECT_WHITEBOARD})
@Retention(RetentionPolicy.SOURCE)
public @interface ColorEffect {
}