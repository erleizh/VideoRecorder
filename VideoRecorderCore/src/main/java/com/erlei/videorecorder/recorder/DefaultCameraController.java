package com.erlei.videorecorder.recorder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.MotionEvent;

import com.erlei.videorecorder.camera.Camera;
import com.erlei.videorecorder.camera.FpsRange;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.util.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

public class DefaultCameraController implements CameraController {

    private final ICameraPreview mCameraPreview;
    protected final Context mContext;
    protected VideoRecorder.Config mConfig;
    protected Camera mCamera;
    protected Camera.CameraBuilder mCameraBuilder;
    protected OnDrawTextureListener mTextureCallBack;
    private ValueAnimator mSmoothZoomAnimator;
    private float mOldDist;

    public DefaultCameraController(ICameraPreview cameraPreview) {
        mCameraPreview = cameraPreview;
        mContext = cameraPreview.getContext();
    }


    public OnDrawTextureListener getTextureCallBack() {
        return mTextureCallBack;
    }

    public void setTextureCallBack(OnDrawTextureListener textureCallBack) {
        mTextureCallBack = textureCallBack;
    }

    public void setCameraBuilder(Camera.CameraBuilder cameraBuilder) {
        mCameraBuilder = cameraBuilder;
    }

    @Override
    public List<Size> getSupportedPreviewSizes() {
        checkCameraState();
        return mCamera.getSupportedPreviewSizes();
    }

    @Override
    public Camera.CameraBuilder getCameraBuilder() {
        checkCameraState();
        return mCamera.getBuilder();
    }

    @Override
    public void setPreviewFpsRange(FpsRange fpsRange) {
        checkCameraState();
        mCamera.setPreviewFpsRange(fpsRange);
    }

    @Override
    public void setZoomOnTouch(MotionEvent event) {
        if (event.getPointerCount() < 2 || !isOpen()) return;
        checkCameraState();
        android.hardware.Camera.Parameters cameraParameters = mCamera.getCameraParameters();
        if (cameraParameters == null) return;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mOldDist = getFingerSpacing(event);
                break;
            case MotionEvent.ACTION_MOVE:
                float newDist = getFingerSpacing(event);
                int maxZoom = cameraParameters.getMaxZoom();
                int zoom = cameraParameters.getZoom();
                if (newDist > mOldDist) {
                    setZoom((zoom + maxZoom / 30));
                } else if (newDist < mOldDist) {
                    setZoom((zoom - maxZoom / 30));
                }
                mOldDist = newDist;
                break;
        }
    }

    @Override
    public boolean isOpen() {
        return mCamera != null && mCamera.isOpen();
    }

    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    @Override
    public Context getContext() {
        return mContext;
    }


    @Override
    public Size getCameraSize() {
        return mCamera == null ? new Size(0, 0) : mCamera.getPreviewSize();
    }

    @Override
    public Size getSurfaceSize() {
        return mCameraPreview.getSurfaceSize();
    }

    @Override
    public synchronized boolean openCamera(SurfaceTexture texture) {
        if (mCameraBuilder == null) {
            Camera.CameraBuilder builder = new Camera.CameraBuilder(getContext());
            builder.useDefaultConfig()
                    .setPreviewSize(new Size(2048, 1536))
                    .setRecordingHint(true)
                    .setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
                    .setSurfaceTexture(texture);
            mCamera = builder.build().open();
        } else {
            mCameraBuilder.setSurfaceTexture(texture);
            mCamera = new Camera.CameraBuilder(getContext()).build(mCameraBuilder).open();
        }
        LogUtil.logd("openCamera " + System.currentTimeMillis());
        return mCamera != null;
    }

    @Override
    public synchronized void closeCamera() {
        stopSmoothZoom();
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
    }

    public void stopSmoothZoom() {
        if (mSmoothZoomAnimator != null && mSmoothZoomAnimator.isRunning()) {
            mSmoothZoomAnimator.cancel();
            mSmoothZoomAnimator = null;
        }
    }


    @Override
    public int getCameraOrientation() {
        return mCamera == null ? 90 : mCamera.getCameraOrientation();
    }

    @Override
    public int getDisplayOrientation() {
        return mCamera == null ? 90 : mCamera.getDisplayOrientation();
    }

    @Override
    public void setFocusAreaOnTouch(MotionEvent event) {
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN && isOpen()) {
            Size surfaceSize = getSurfaceSize();
            Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1F, surfaceSize.getWidth(), surfaceSize.getHeight());
            setFocusAreas(focusRect);
        }
    }

    public void cancelAutoFocus() {
        checkCameraState();
        mCamera.cancelAutoFocus();
    }

    /**
     * Convert touch position x:y to {@link android.hardware.Camera.Area} position -1000:-1000 to 1000:1000.
     */
    public Rect calculateTapArea(float x, float y, float coefficient, int width, int height) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(coefficient * focusAreaSize).intValue();
        int centerX = (int) (x / width * 2000 - 1000);
        int centerY = (int) (y / height * 2000 - 1000);

        int halfAreaSize = areaSize / 2;
        RectF rectF = new RectF(clamp(centerX - halfAreaSize, -1000, 1000)
                , clamp(centerY - halfAreaSize, -1000, 1000)
                , clamp(centerX + halfAreaSize, -1000, 1000)
                , clamp(centerY + halfAreaSize, -1000, 1000));
        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    @Override
    public void setMeteringAreaOnTouch(MotionEvent event) {
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN && isOpen()) {
            Size surfaceSize = getSurfaceSize();
            Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1F, surfaceSize.getWidth(), surfaceSize.getHeight());
            setMeteringAreas(focusRect);
        }
    }

    @Override
    public android.hardware.Camera.Parameters getCameraParameters() {
        checkCameraState();
        return mCamera.getCameraParameters();
    }

    @Override
    public Camera getCamera() {
        return mCamera;
    }

    @Override
    public void setCameraParameters(android.hardware.Camera.Parameters parameters) {
        checkCameraState();
        mCamera.setCameraParameters(parameters);
    }

    @Override
    public boolean isFront() {
        return mCamera != null && mCamera.getFacing() == CAMERA_FACING_FRONT;
    }

    /**
     * 设置缩放 如果设备支持的话
     * 最小值为0 ， 最大值为 getMaxZoom() ， 如果设置的值大于获取的最大值 ， 那么将设置为MaxZoom
     *
     * @param zoom 缩放级别
     */
    @Override
    public void setZoom(int zoom) {
        checkCameraState();
        mCamera.setZoom(zoom);
    }

    /**
     * 平滑缩放 如果设备支持的话
     * 最小值为0 ， 最大值为 getMaxZoom() ， 如果设置的值大于获取的最大值 ， 那么将设置为MaxZoom
     *
     * @param zoom 缩放级别
     */
    @Override
    public void startSmoothZoom(int zoom) {
        checkCameraState();
        android.hardware.Camera.Parameters cameraParameters = mCamera.getCameraParameters();
        if (cameraParameters == null) return;
        if (cameraParameters.isZoomSupported()) {
            int currentZoom = cameraParameters.getZoom();
            int maxZoom = cameraParameters.getMaxZoom();
            int target = clamp(zoom, 0, maxZoom);
            if (zoom == currentZoom) return;
            if (cameraParameters.isSmoothZoomSupported()) {
                //支持平滑缩放
                mCamera.smoothZoom(zoom);
            } else {
                //不支持平滑缩放,使用兼容模式
                stopSmoothZoom();
                mSmoothZoomAnimator = ValueAnimator.ofInt(currentZoom, target);
                mSmoothZoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        if (mCamera == null) return;
                        int animatedValue = (int) valueAnimator.getAnimatedValue();
                        mCamera.setZoom(animatedValue);
                    }
                });
                mSmoothZoomAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mSmoothZoomAnimator = null;
                    }
                });
                mSmoothZoomAnimator.setDuration(300);
                mSmoothZoomAnimator.start();

            }
        }
    }


    /**
     * @param sceneModes 场景模式
     */
    @Override
    public void setSceneMode(String... sceneModes) {
        checkCameraState();
        mCamera.setSceneMode(sceneModes);
    }

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
    @Override
    public void setColorEffects(String... colorEffects) {
        checkCameraState();
        mCamera.setColorEffects(colorEffects);
    }

    /**
     * @param focusModes 对焦模式
     * @see android.hardware.Camera.Parameters#FOCUS_MODE_AUTO
     * @see android.hardware.Camera.Parameters#FOCUS_MODE_CONTINUOUS_VIDEO
     * @see android.hardware.Camera.Parameters#FOCUS_MODE_CONTINUOUS_PICTURE
     * @see android.hardware.Camera.Parameters#FOCUS_MODE_EDOF
     * @see android.hardware.Camera.Parameters#FOCUS_MODE_FIXED
     * @see android.hardware.Camera.Parameters#FOCUS_MODE_INFINITY
     * @see android.hardware.Camera.Parameters#FOCUS_MODE_MACRO
     */
    @Override
    public void setFocusMode(String... focusModes) {
        checkCameraState();
        mCamera.setFocusMode(focusModes);
    }

    /**
     * 设置防闪烁参数 ,(由于灯光频率(50HZ或者60HZ)影响的数字相机曝光，进而产生的条纹。)
     *
     * @param antibanding 防闪烁值
     * @see android.hardware.Camera.Parameters#ANTIBANDING_50HZ
     * @see android.hardware.Camera.Parameters#ANTIBANDING_60HZ
     * @see android.hardware.Camera.Parameters#ANTIBANDING_AUTO
     * @see android.hardware.Camera.Parameters#ANTIBANDING_OFF
     */
    @Override
    public void setAntibanding(String... antibanding) {
        checkCameraState();
        mCamera.setAntibanding(antibanding);
    }


    /**
     * @param flashModes 闪光灯模式
     * @see android.hardware.Camera.Parameters#FLASH_MODE_AUTO
     * @see android.hardware.Camera.Parameters#FLASH_MODE_OFF
     * @see android.hardware.Camera.Parameters#FLASH_MODE_ON
     * @see android.hardware.Camera.Parameters#FLASH_MODE_RED_EYE
     * @see android.hardware.Camera.Parameters#FLASH_MODE_TORCH
     */
    @Override
    public void setFlashMode(String... flashModes) {
        checkCameraState();
        mCamera.setFlashMode(flashModes);
    }

    /**
     * 是否是录制模式
     *
     * @param recording 录制模式
     * @see android.hardware.Camera.Parameters#setRecordingHint(boolean)
     */
    @Override
    public void setRecordingHint(boolean recording) {
        checkCameraState();
        mCamera.setRecordingHint(recording);
    }

    /**
     * 设置使用的摄像头朝向
     *
     * @param facing 摄像头朝向
     * @see android.hardware.Camera.CameraInfo#CAMERA_FACING_BACK
     * @see android.hardware.Camera.CameraInfo#CAMERA_FACING_FRONT
     */
    @Override
    public void setFacing(int facing) {
        checkCameraState();
        mCamera.setFacing(facing);
    }

    /**
     * 设置比白平衡
     *
     * @param whiteBalance 白平衡
     */
    @Override
    public void setWhiteBalance(String... whiteBalance) {
        checkCameraState();
        mCamera.setWhiteBalance(whiteBalance);
    }

    /**
     * /**
     * 设置曝光补偿
     *
     * @param compensation 曝光补偿
     */
    @Override
    public void setExposureCompensation(int compensation) {
        checkCameraState();
        mCamera.setExposureCompensation(compensation);
    }

    /**
     * 设置测光区域
     *
     * @param rect 测光区域列表
     */
    @Override
    public void setMeteringAreas(Rect... rect) {
        checkCameraState();
        mCamera.setMeteringAreas(rect);
    }

    /**
     * @return 获取支持的预览帧率区间
     */
    @Override
    public List<FpsRange> getSupportedPreviewFpsRange() {
        checkCameraState();
        return mCamera.getSupportedPreviewFpsRange();
    }

    /**
     * 设置焦点
     *
     * @param rect 焦点区域列表
     */
    @Override
    public void setFocusAreas(Rect... rect) {
        checkCameraState();
        mCamera.setFocusAreas(rect);
    }

    /**
     * 切换摄像头朝向
     */
    @Override
    public void toggleFacing() {
        checkCameraState();
        mCamera.toggleFacing();
    }

    @Override
    public List<String> getSupportedModes(String... modes) {
        checkCameraState();
        return mCamera.getSupportedModes(modes);
    }

    @Override
    public void setMode(String key, String value) {
        checkCameraState();
        mCamera.setMode(key,value);
    }


    private void checkCameraState() {
        if (mCamera == null) {
            throw new IllegalStateException("camera == null");
        }
    }

}
