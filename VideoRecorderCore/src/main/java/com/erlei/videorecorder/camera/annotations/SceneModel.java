package com.erlei.videorecorder.camera.annotations;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@StringDef({
        android.hardware.Camera.Parameters.SCENE_MODE_ACTION,
        android.hardware.Camera.Parameters.SCENE_MODE_AUTO,
        android.hardware.Camera.Parameters.SCENE_MODE_BARCODE,
        android.hardware.Camera.Parameters.SCENE_MODE_BEACH,
        android.hardware.Camera.Parameters.SCENE_MODE_CANDLELIGHT,
        android.hardware.Camera.Parameters.SCENE_MODE_FIREWORKS,
        android.hardware.Camera.Parameters.SCENE_MODE_HDR,
        android.hardware.Camera.Parameters.SCENE_MODE_LANDSCAPE,
        android.hardware.Camera.Parameters.SCENE_MODE_NIGHT,
        android.hardware.Camera.Parameters.SCENE_MODE_NIGHT_PORTRAIT,
        android.hardware.Camera.Parameters.SCENE_MODE_PARTY,
        android.hardware.Camera.Parameters.SCENE_MODE_SNOW,
        android.hardware.Camera.Parameters.SCENE_MODE_PORTRAIT,
        android.hardware.Camera.Parameters.SCENE_MODE_SPORTS,
        android.hardware.Camera.Parameters.SCENE_MODE_THEATRE,
        android.hardware.Camera.Parameters.SCENE_MODE_SUNSET,
        android.hardware.Camera.Parameters.SCENE_MODE_STEADYPHOTO})
@Retention(RetentionPolicy.SOURCE)
public @interface SceneModel {
}