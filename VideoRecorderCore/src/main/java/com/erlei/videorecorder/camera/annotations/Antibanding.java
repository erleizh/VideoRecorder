package com.erlei.videorecorder.camera.annotations;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@StringDef({
        android.hardware.Camera.Parameters.ANTIBANDING_50HZ,
        android.hardware.Camera.Parameters.ANTIBANDING_60HZ,
        android.hardware.Camera.Parameters.ANTIBANDING_AUTO,
        android.hardware.Camera.Parameters.ANTIBANDING_OFF})
@Retention(RetentionPolicy.SOURCE)
public @interface Antibanding {
}