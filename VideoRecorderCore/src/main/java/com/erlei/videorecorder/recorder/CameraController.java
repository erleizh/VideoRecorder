package com.erlei.videorecorder.recorder;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.support.annotation.IntRange;
import android.view.MotionEvent;

import com.erlei.videorecorder.camera.Camera;
import com.erlei.videorecorder.camera.FpsRange;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.camera.annotations.Antibanding;
import com.erlei.videorecorder.camera.annotations.ColorEffect;
import com.erlei.videorecorder.camera.annotations.Facing;
import com.erlei.videorecorder.camera.annotations.FlashModel;
import com.erlei.videorecorder.camera.annotations.FocusModel;
import com.erlei.videorecorder.camera.annotations.SceneModel;
import com.erlei.videorecorder.camera.annotations.WhiteBalance;

import java.util.List;

public interface CameraController {


    void setCameraBuilder(Camera.CameraBuilder cameraBuilder);

    List<Size> getSupportedPreviewSizes();

    Camera.CameraBuilder getCameraBuilder();

    void setPreviewFpsRange(FpsRange fpsRange);

    void setZoomOnTouch(MotionEvent event);

    /**
     * @return 相机是否打开
     */
    boolean isOpen();

    int getCameraOrientation();

    int getDisplayOrientation();

    void setFocusAreaOnTouch(MotionEvent event);

    void setMeteringAreaOnTouch(MotionEvent event);

    android.hardware.Camera.Parameters getCameraParameters();

    Camera getCamera();

    void setCameraParameters(android.hardware.Camera.Parameters parameters);

    Context getContext();

    Size getCameraSize();

    Size getSurfaceSize();

    boolean openCamera(SurfaceTexture texture);

    void closeCamera();

    /**
     * @return camera is front
     */
    boolean isFront();

    /**
     * 设置缩放 如果设备支持的话
     * 最小值为0 ， 最大值为 getMaxZoom() ， 如果设置的值大于获取的最大值 ， 那么将设置为MaxZoom
     *
     * @param zoom 缩放级别
     */
    void setZoom(@IntRange(from = 0, to = Integer.MAX_VALUE) int zoom);

    /**
     * 平滑缩放 如果设备支持的话
     * 最小值为0 ， 最大值为 getMaxZoom() ， 如果设置的值大于获取的最大值 ， 那么将设置为MaxZoom
     *
     * @param zoom 缩放级别
     */
    void startSmoothZoom(@IntRange(from = 0, to = Integer.MAX_VALUE) int zoom);

    /**
     * @param sceneModels 场景模式
     * @see android.hardware.Camera.Parameters#SCENE_MODE_ACTION
     * @see android.hardware.Camera.Parameters#SCENE_MODE_AUTO
     * @see android.hardware.Camera.Parameters#SCENE_MODE_BARCODE
     * @see android.hardware.Camera.Parameters#SCENE_MODE_BEACH
     * @see android.hardware.Camera.Parameters#SCENE_MODE_CANDLELIGHT
     * @see android.hardware.Camera.Parameters#SCENE_MODE_FIREWORKS
     * @see android.hardware.Camera.Parameters#SCENE_MODE_HDR
     * @see android.hardware.Camera.Parameters#SCENE_MODE_LANDSCAPE
     * @see android.hardware.Camera.Parameters#SCENE_MODE_NIGHT
     * @see android.hardware.Camera.Parameters#SCENE_MODE_NIGHT_PORTRAIT
     * @see android.hardware.Camera.Parameters#SCENE_MODE_PARTY
     * @see android.hardware.Camera.Parameters#SCENE_MODE_SNOW
     * @see android.hardware.Camera.Parameters#SCENE_MODE_PORTRAIT
     * @see android.hardware.Camera.Parameters#SCENE_MODE_SPORTS
     * @see android.hardware.Camera.Parameters#SCENE_MODE_THEATRE
     * @see android.hardware.Camera.Parameters#SCENE_MODE_SUNSET
     * @see android.hardware.Camera.Parameters#SCENE_MODE_STEADYPHOTO
     */
    void setSceneMode(@SceneModel String... sceneModels);

    /**
     * @param colorEffects 设置色彩效果
     * @see android.hardware.Camera.Parameters#EFFECT_SEPIA
     * @see android.hardware.Camera.Parameters#EFFECT_AQUA
     * @see android.hardware.Camera.Parameters#EFFECT_BLACKBOARD
     * @see android.hardware.Camera.Parameters#EFFECT_MONO
     * @see android.hardware.Camera.Parameters#EFFECT_NEGATIVE
     * @see android.hardware.Camera.Parameters#EFFECT_NONE
     * @see android.hardware.Camera.Parameters#EFFECT_POSTERIZE
     * @see android.hardware.Camera.Parameters#EFFECT_SOLARIZE
     * @see android.hardware.Camera.Parameters#EFFECT_WHITEBOARD
     */
    void setColorEffects(@ColorEffect String... colorEffects);

    /**
     * @param focusModels 对焦模式
     * @see android.hardware.Camera.Parameters#FOCUS_MODE_AUTO
     * @see android.hardware.Camera.Parameters#FOCUS_MODE_CONTINUOUS_VIDEO
     * @see android.hardware.Camera.Parameters#FOCUS_MODE_CONTINUOUS_PICTURE
     * @see android.hardware.Camera.Parameters#FOCUS_MODE_EDOF
     * @see android.hardware.Camera.Parameters#FOCUS_MODE_FIXED
     * @see android.hardware.Camera.Parameters#FOCUS_MODE_INFINITY
     * @see android.hardware.Camera.Parameters#FOCUS_MODE_MACRO
     */
    void setFocusMode(@FocusModel String... focusModels);

    /**
     * 设置防闪烁参数 ,(由于灯光频率(50HZ或者60HZ)影响的数字相机曝光，进而产生的条纹。)
     *
     * @param antibanding 防闪烁值
     * @see android.hardware.Camera.Parameters#ANTIBANDING_50HZ
     * @see android.hardware.Camera.Parameters#ANTIBANDING_60HZ
     * @see android.hardware.Camera.Parameters#ANTIBANDING_AUTO
     * @see android.hardware.Camera.Parameters#ANTIBANDING_OFF
     */
    void setAntibanding(@Antibanding String... antibanding);

    /**
     * @param flashModels 闪光灯模式
     * @see android.hardware.Camera.Parameters#FLASH_MODE_AUTO
     * @see android.hardware.Camera.Parameters#FLASH_MODE_OFF
     * @see android.hardware.Camera.Parameters#FLASH_MODE_ON
     * @see android.hardware.Camera.Parameters#FLASH_MODE_RED_EYE
     * @see android.hardware.Camera.Parameters#FLASH_MODE_TORCH
     */
    void setFlashMode(@FlashModel String... flashModels);

    /**
     * 是否是录制模式
     *
     * @param recording 录制模式
     * @see android.hardware.Camera.Parameters#setRecordingHint(boolean)
     */
    void setRecordingHint(boolean recording);

    /**
     * 设置使用的摄像头朝向
     *
     * @param facing 摄像头朝向
     * @see android.hardware.Camera.CameraInfo#CAMERA_FACING_BACK
     * @see android.hardware.Camera.CameraInfo#CAMERA_FACING_FRONT
     */
    void setFacing(@Facing int facing);

    /**
     * 设置比白平衡
     *
     * @param whiteBalance 白平衡
     */
    void setWhiteBalance(@WhiteBalance String... whiteBalance);

    /**
     * /**
     * 设置曝光补偿
     *
     * @param compensation 曝光补偿
     */
    void setExposureCompensation(int compensation);

    /**
     * 设置测光区域
     *
     * @param rect 测光区域列表
     */
    void setMeteringAreas(Rect... rect);

    /**
     * @return 获取支持的预览帧率区间
     */
    List<FpsRange> getSupportedPreviewFpsRange();

    /**
     * 设置焦点
     *
     * @param rect 焦点区域列表
     */
    void setFocusAreas(Rect... rect);

    /**
     * 切换摄像头朝向
     */
    void toggleFacing();

    /**
     * 获取相机支持的模式,相机打开之后才能调用
     *
     * @param modes    modes
     */
    List<String> getSupportedModes(String... modes);


}
