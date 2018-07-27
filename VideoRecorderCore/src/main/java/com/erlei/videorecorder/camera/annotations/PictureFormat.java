package com.erlei.videorecorder.camera.annotations;

import android.graphics.ImageFormat;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({ImageFormat.NV21, ImageFormat.RGB_565, ImageFormat.JPEG})
@Retention(RetentionPolicy.SOURCE)
public @interface PictureFormat {
}