package com.erlei.videorecorder.camera.annotations;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@StringDef({
        android.hardware.Camera.Parameters.FLASH_MODE_AUTO,
        android.hardware.Camera.Parameters.FLASH_MODE_OFF,
        android.hardware.Camera.Parameters.FLASH_MODE_ON,
        android.hardware.Camera.Parameters.FLASH_MODE_RED_EYE,
        android.hardware.Camera.Parameters.FLASH_MODE_TORCH,})
@Retention(RetentionPolicy.SOURCE)
public @interface FlashModel {
}