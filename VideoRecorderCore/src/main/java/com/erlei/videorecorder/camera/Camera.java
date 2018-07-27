package com.erlei.videorecorder.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.erlei.videorecorder.camera.annotations.Antibanding;
import com.erlei.videorecorder.camera.annotations.ColorEffect;
import com.erlei.videorecorder.camera.annotations.Facing;
import com.erlei.videorecorder.camera.annotations.FlashModel;
import com.erlei.videorecorder.camera.annotations.FocusModel;
import com.erlei.videorecorder.camera.annotations.PictureFormat;
import com.erlei.videorecorder.camera.annotations.PreviewFormat;
import com.erlei.videorecorder.camera.annotations.SceneModel;
import com.erlei.videorecorder.camera.annotations.WhiteBalance;
import com.erlei.videorecorder.util.Config;
import com.erlei.videorecorder.util.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;


/**
 * Created by lll on 2018/1/18.
 * 相机工具类
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Camera {

    /**
     * android.hardware.Camera#getNumberOfCameras() == 0
     */
    public static final int NO_CAMERA = 1;
    /**
     * CameraBuilder == null , 不应该发生
     */
    public static final int NOT_CONFIGURED = 2;

    /**
     * 设置的相机朝向设备不支持
     *
     * @see CameraBuilder#setFacing(int)
     */
    public static final int UNSUPPORTED_FACING = 3;

    /**
     * 打开相机失败  (请检查权限)
     *
     * @see android.hardware.Camera#open()
     */
    public static final int OPEN_ERROR = 4;

    /**
     * 成功连接相机服务并开启预览
     */
    public static final int SUCCESS = 5;
    /**
     * 开启预览失败
     */
    public static final int PREVIEW_ERROR = 6;
    /**
     * 未知错误
     */
    public static final int UNKNOWN = 7;

    /**
     * 不支持的预览尺寸
     */
    public static final int UNSUPPORTED_PREVIEW_SIZE = 8;
    /**
     * 不支持的拍照尺寸
     */
    public static final int UNSUPPORTED_PICTURE_SIZE = 9;

    private static final String TAG = Config.TAG;
    private static final boolean LOG_ENABLE = Config.DEBUG;
    private final Context mContext;
    private CameraBuilder mBuilder;
    private int mCameraId = -1;
    private android.hardware.Camera mCamera;
    private android.hardware.Camera.Size mPreviewSize;
    private android.hardware.Camera.Size mPictureSize;
    private int mDisplayOrientation;
    private int mCameraOrientation;
    private boolean mOpened;


    private Camera(Context context, CameraBuilder builder) {
        mContext = context;
        mBuilder = builder;
    }

    /**
     * 获取相机的方向
     *
     * @return 0 , 90, 180 , 270
     */
    public int getCameraOrientation() {
        return mCameraOrientation;
    }

    public int getDisplayOrientation() {
        return mDisplayOrientation;
    }

    public CameraBuilder getBuilder() {
        return mBuilder;
    }

    /**
     * 打开摄像机
     *
     * @return 是否成功
     */
    @Nullable
    public synchronized Camera open() {
        int cameras = android.hardware.Camera.getNumberOfCameras();
        if (cameras == 0) {
            loge("该设备没有相机可用");
            handleCameraCallback(NO_CAMERA, "该设备没有相机可用");
            return null;
        }
        if (mBuilder == null) {
            loge("没有配置相机");
            handleCameraCallback(NOT_CONFIGURED, "没有配置相机");
            throw new IllegalStateException("没有配置相机");
        }
        android.hardware.Camera.CameraInfo cameraInfo = new android.hardware.Camera.CameraInfo();
        for (int i = 0; i < cameras; i++) {
            android.hardware.Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == mBuilder.mFacing) mCameraId = i;
        }
        if (mCameraId == -1) {
            loge("设置的相机方向该设备不支持");
            handleCameraCallback(UNSUPPORTED_FACING, "设置的相机方向该设备不支持");
            return null;
        }
        try {
            if (mCamera != null) close();
            mCamera = android.hardware.Camera.open(mCameraId);
            mOpened = true;
        } catch (Exception e) {
            loge(e.toString());
            e.printStackTrace();
            close();
            handleCameraCallback(OPEN_ERROR, e.toString());
            return null;
        }
        setCameraParameters();
        startPreview();
        return this;
    }

    private void handleCameraCallback(final int code, final String errMsg) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mBuilder == null) return;
                for (CameraCallback callback : mBuilder.mCameraCallbacks) {
                    if (TextUtils.isEmpty(errMsg)) {
                        callback.onSuccess(Camera.this);
                    } else {
                        callback.onFailure(code, errMsg);
                    }
                }
            }
        };
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            runnable.run();
        } else {
            new Handler(Looper.getMainLooper()).post(runnable);
        }


    }

    /**
     * @return 获取预览的尺寸 该方法需要在相机成功预览之后调用才会有正确结果
     */
    public Size getPreviewSize() {
        return new Size(mPreviewSize);
    }

    /**
     * @return activity is  landscape
     */
    public boolean isLandscape() {
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null && windowManager.getDefaultDisplay() != null) {
            int rotation = windowManager.getDefaultDisplay().getRotation();
            LogUtil.logd(TAG, "rotation=" + rotation);
            return rotation == ROTATION_90 || rotation == ROTATION_270;
        }
        return mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }


    private void setCameraParameters() {
        if (mCamera == null) return;
        android.hardware.Camera.Parameters cameraParameters = getParameters();
        if (cameraParameters == null) return;
        //设置对焦模式
        setFocusMode(cameraParameters, mBuilder.mFocusModes);

        //设置闪光灯模式
        setFlashMode(cameraParameters, mBuilder.mFlashModes);

        //设置场景模式
        setSceneMode(cameraParameters, mBuilder.mSceneModes);

        //色彩效果
        setColorEffect(cameraParameters, mBuilder.mColorEffects);

        //白平衡
        setWhiteBalance(cameraParameters, mBuilder.mWhiteBalances);

        //刷新率
        setAntibanding(cameraParameters, mBuilder.mAntibanding);

        //设置预览帧的像素格式
        int previewFormat = getSupportedModelOfInt(cameraParameters.getSupportedPreviewFormats(), mBuilder.mPreviewFormats);
        if (previewFormat != -1) cameraParameters.setPreviewFormat(previewFormat);

        //设置图片的像素格式
        int pictureFormat = getSupportedModelOfInt(cameraParameters.getSupportedPictureFormats(), mBuilder.mPictureFormats);
        if (pictureFormat != -1) cameraParameters.setPictureFormat(pictureFormat);

        //是否是录制模式
        cameraParameters.setRecordingHint(mBuilder.mRecordingHint);


        //设置预览大小
        if (mBuilder.mPreviewSizeSelector != null) {
            mPreviewSize = mBuilder.mPreviewSizeSelector.select(cameraParameters.getSupportedPreviewSizes(), mBuilder.mPreviewSize);
        } else {
            mPreviewSize = getOptimalSize("SupportedPreviewSizes", cameraParameters.getSupportedPreviewSizes(), mBuilder.mPreviewSize);
        }
        if (mPreviewSize == null) {
            handleCameraCallback(UNSUPPORTED_PREVIEW_SIZE, "没有找到合适的预览尺寸");
            throw new IllegalStateException("没有找到合适的预览尺寸");
        }
        log("requestPreviewSize ：" + mBuilder.mPreviewSize.toString() + "\t\t previewSize : " + getPreviewSize().toString());
        cameraParameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);


        //设置拍照的图片尺寸
        if (mBuilder.mPictureSize != null) {
            if (mBuilder.mPictureSizeSelector != null) {
                mPictureSize = mBuilder.mPictureSizeSelector.select(cameraParameters.getSupportedPreviewSizes(), mBuilder.mPictureSize);
            } else {
                mPictureSize = getOptimalSize("SupportedPictureSizes", cameraParameters.getSupportedPictureSizes(), mBuilder.mPictureSize);
            }
            if (mPictureSize == null) {
                handleCameraCallback(UNSUPPORTED_PICTURE_SIZE, "没有找到合适的图片尺寸");
                throw new IllegalStateException("没有找到合适的图片尺寸");
            }
            log("requestPictureSize ：" + mBuilder.mPictureSize.toString() + "\t\t pictureSize : " + getPictureSize().toString());
            cameraParameters.setPictureSize(mPictureSize.width, mPictureSize.height);
        }
        //拍照的图片缩放
        setZoom(cameraParameters, mBuilder.mZoom);


        mCamera.setParameters(cameraParameters);
        log(cameraParameters.flatten().replaceAll(";", ";\t"));
    }

    private void setZoom(android.hardware.Camera.Parameters cameraParameters, int zoom) {
        if (zoom != -1) {
            int target = clamp(zoom, 0, cameraParameters.getMaxZoom());
            if (cameraParameters.isZoomSupported()) cameraParameters.setZoom(target);
        }
    }

    @SuppressWarnings("all")
    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    private void setAntibanding(android.hardware.Camera.Parameters cameraParameters, List<String> antibandings) {
        String antibanding = getSupportedModelOfString(cameraParameters.getSupportedAntibanding(), antibandings);
        if (!TextUtils.isEmpty(antibanding)) cameraParameters.setAntibanding(antibanding);
    }

    private void setWhiteBalance(android.hardware.Camera.Parameters cameraParameters, List<String> whiteBalances) {
        String whiteBalance = getSupportedModelOfString(cameraParameters.getSupportedWhiteBalance(), whiteBalances);
        if (!TextUtils.isEmpty(whiteBalance)) cameraParameters.setWhiteBalance(whiteBalance);
    }

    private void setColorEffect(android.hardware.Camera.Parameters cameraParameters, List<String> colorEffects) {
        String colorEffect = getSupportedModelOfString(cameraParameters.getSupportedColorEffects(), colorEffects);
        if (!TextUtils.isEmpty(colorEffect)) cameraParameters.setColorEffect(colorEffect);
    }

    private void setSceneMode(android.hardware.Camera.Parameters cameraParameters, List<String> sceneModels) {
        String sceneModel = getSupportedModelOfString(cameraParameters.getSupportedSceneModes(), sceneModels);
        if (!TextUtils.isEmpty(sceneModel)) cameraParameters.setSceneMode(sceneModel);
    }

    private void setFlashMode(android.hardware.Camera.Parameters cameraParameters, List<String> flashModels) {
        String flashModel = getSupportedModelOfString(cameraParameters.getSupportedFlashModes(), flashModels);
        if (!TextUtils.isEmpty(flashModel)) cameraParameters.setFlashMode(flashModel);
    }

    private void setFocusMode(android.hardware.Camera.Parameters cameraParameters, List<String> focusModels) {
        String focusMode = getSupportedModelOfString(cameraParameters.getSupportedFocusModes(), focusModels);
        if (!TextUtils.isEmpty(focusMode)) cameraParameters.setFocusMode(focusMode);
    }

    /**
     * 切换闪光灯状态
     */
    public synchronized void toggleFlash() {
        if (mCamera == null) return;
        try {
            android.hardware.Camera.Parameters parameters = getParameters();
            if (parameters == null) return;
            if (flashIsOpen()) {
                //打开状态 - 需要关闭
                parameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_OFF);
            } else {
                //关闭状态 - 需要打开
                parameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_TORCH);
            }
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
            loge("切换闪光灯失败");
        }
    }

    /**
     * @return 闪光灯是否开启
     */
    public boolean flashIsOpen() {
        android.hardware.Camera.Parameters parameters = getParameters();
        return parameters != null && mCamera != null && android.hardware.Camera.Parameters.FLASH_MODE_TORCH.equals(parameters.getFlashMode());
    }

    /**
     * 切换摄像头朝向
     */
    public synchronized void toggleFacing() {
        if (mCamera == null) return;
        if (getFacing() == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK) {
            mBuilder.mFacing = android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mBuilder.mFacing = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        open();
    }

    /**
     * @return 获取当前摄像头的朝向
     */
    public int getFacing() {
        if (mCamera == null) return -1;
        return getCameraInfo(mCameraId).facing;
    }


    public Size getPictureSize() {
        return new Size(mPictureSize);
    }


    public void takePicture(android.hardware.Camera.ShutterCallback shutter, android.hardware.Camera.PictureCallback raw,
                            android.hardware.Camera.PictureCallback postview, android.hardware.Camera.PictureCallback jpeg) {
        if (mCamera == null) return;
        mCamera.takePicture(shutter, raw, postview, jpeg);
    }

    public void takePicture(android.hardware.Camera.ShutterCallback shutter, android.hardware.Camera.PictureCallback raw,
                            android.hardware.Camera.PictureCallback jpeg) {
        if (mCamera == null) return;
        takePicture(shutter, raw, null, jpeg);
    }


    /**
     * 设置焦点
     *
     * @param rect 焦点区域
     */
    public void setFocusAreas(Rect... rect) {
        if (mCamera == null || rect == null || rect.length == 0) return;
        final android.hardware.Camera.Parameters parameters = getParameters();
        if (parameters == null) return;

        int maxNumFocusAreas = parameters.getMaxNumFocusAreas();
        if (maxNumFocusAreas > 0) {
            ArrayList<android.hardware.Camera.Area> focusAreas = new ArrayList<>();
            for (int i = 0; i < maxNumFocusAreas; i++) {
                if (i < rect.length)
                    focusAreas.add(new android.hardware.Camera.Area(rect[i], 1000));
            }
            parameters.setFocusAreas(focusAreas);
        }
        final String currentFocusMode = parameters.getFocusMode();
        if (isSupported(parameters.getSupportedFocusModes(), android.hardware.Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(parameters);
            mCamera.autoFocus(new android.hardware.Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, android.hardware.Camera camera) {
                    //不能用上面的 . 如果对焦的这一段时间参数有更改 , 会丢失
                    android.hardware.Camera.Parameters cameraParameters = camera.getParameters();
                    cameraParameters.setFocusMode(currentFocusMode);
                    camera.setParameters(cameraParameters);
                    cancelAutoFocus();
                    if (mBuilder.mAutoFocusCallback != null) {
                        mBuilder.mAutoFocusCallback.onAutoFocus(b, camera);
                    }
                }
            });
        } else {
            mCamera.setParameters(parameters);
        }
    }

    @Nullable
    private android.hardware.Camera.Parameters getParameters() {
        try {
            return mCamera.getParameters();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private int getSupportedModelOfInt(List<Integer> supportedModel, List<Integer> requestModels) {
        if (supportedModel != null && !supportedModel.isEmpty() && requestModels != null && !requestModels.isEmpty()) {
            for (Integer model : requestModels) {
                if (supportedModel.contains(model)) return model;
            }
        }
        return -1;
    }

    @Nullable
    private String getSupportedModelOfString(List<String> supportedModel, List<String> requestModels) {
        if (supportedModel != null && !supportedModel.isEmpty() && requestModels != null && !requestModels.isEmpty()) {
            for (String model : requestModels) {
                if (isSupported(supportedModel, model)) {
                    return model;
                }
            }
        }
        return null;
    }

    private boolean isSupported(List<String> models, String model) {
        return !(models == null || models.isEmpty()) && models.contains(model);
    }

    /**
     * 必须成功打开相机之后才有正确的返回结果
     *
     * @return android.hardware.Camera.Parameters
     */
    @Nullable
    public android.hardware.Camera.Parameters getCameraParameters() {
        if (mCamera == null) return null;
        return getParameters();
    }

    /**
     * 开启相机预览
     */
    private synchronized void startPreview() {
        if (mCamera == null) {
            handleCameraCallback(UNKNOWN, "未知错误");
            return;
        }
        try {
            if (mBuilder.mSurfaceTexture != null) {
                mCamera.setPreviewTexture(mBuilder.mSurfaceTexture);
            } else if (mBuilder.mSurfaceHolder != null) {
                mCamera.setPreviewDisplay(mBuilder.mSurfaceHolder);
            }
            if (mBuilder.mDisplayOrientation == -1 && mContext != null) {
                mDisplayOrientation = getCameraDisplayOrientation(getActivityOrientation(mContext));
            } else {
                mDisplayOrientation = mBuilder.mDisplayOrientation;
            }
            mCamera.setDisplayOrientation(mDisplayOrientation);
            mCamera.startPreview();
            handleCameraCallback(SUCCESS, null);
        } catch (Exception e) {
            loge(e.toString());
            e.printStackTrace();
            handleCameraCallback(PREVIEW_ERROR, toString());
            close();
        }
    }

    private int getActivityOrientation(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null && windowManager.getDefaultDisplay() != null) {
            return windowManager.getDefaultDisplay().getRotation();
        }
        return 90;
    }

    /**
     * 根据屏幕当前角度计算Camera正确显示所需要的角度
     *
     * @param rotation 屏幕角度
     * @return CameraDisplayOrientation
     * @see android.view.Display#getRotation()
     */
    private int getCameraDisplayOrientation(int rotation) {
        android.hardware.Camera.CameraInfo info = getCameraInfo(mCameraId);
        mCameraOrientation = info.orientation;
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    @Nullable
    public android.hardware.Camera.CameraInfo getCameraInfo() {
        if (mCameraId == -1) return null;
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(mCameraId, info);
        return info;
    }

    @NonNull
    public android.hardware.Camera.CameraInfo getCameraInfo(int cameraId) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(mCameraId, info);
        return info;
    }

    /**
     * @param tag                   日志前綴
     * @param supportedPreviewSizes 设备支持的预览尺寸
     * @param request               请求的预览尺寸
     * @return 在设备支持的尺寸中 ， 根据请求的尺寸来寻找一个合适的预览尺寸 ，
     * 如果请求的尺寸是设备支持的 ， 那么直接返回该尺寸 ，否则根据指定的宽高来寻找最接近的尺寸
     */
    private android.hardware.Camera.Size getOptimalSize(String tag, List<android.hardware.Camera.Size> supportedPreviewSizes, Size request) {
        //排序
        Collections.sort(supportedPreviewSizes, new Comparator<android.hardware.Camera.Size>() {
            //            @Override
//            public int compare(android.hardware.Camera.Size o1, android.hardware.Camera.Size o2) {
//                return o1.width == o2.width ? o1.height - o2.height : o1.width - o2.width;
//            }
            @Override
            public int compare(android.hardware.Camera.Size pre, android.hardware.Camera.Size after) {
                return Long.signum((long) pre.width * pre.height - (long) after.width * after.height);
            }
        });
        if (LOG_ENABLE) {
            for (android.hardware.Camera.Size size : supportedPreviewSizes) {
                log(tag + "\t\twidth :" + size.width + "\t height :" + size.height + "\t ratio : " + ((float) size.width / (float) size.height));
            }
        }
        //如果设备支持请求的尺寸
        for (android.hardware.Camera.Size size : supportedPreviewSizes) {
            if (request.equals(size)) return size;
        }

        for (android.hardware.Camera.Size size : supportedPreviewSizes) {
            if (size.width >= request.width && size.height >= request.height) return size;
        }

        //最终还没找到 , 使用最大的
        return Collections.max(supportedPreviewSizes, new Comparator<android.hardware.Camera.Size>() {
            @Override
            public int compare(android.hardware.Camera.Size pre, android.hardware.Camera.Size after) {
                return Long.signum((long) pre.width * pre.height -
                        (long) after.width * after.height);
            }
        });
    }


    public synchronized void close() {
        if (mCamera == null) return;
        try {
            mOpened = false;
            mCamera.stopPreview();
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.setPreviewCallback(null);
            mCamera.setErrorCallback(null);
            mCamera.release();
            mCamera = null;
        } catch (Exception e) {
            loge(e.getMessage());
            e.printStackTrace();
        }
    }


    public Size getRequestPreviewSize() {
        return mBuilder.mPreviewSize;
    }

    public List<Size> getSupportedPreviewSizes() {
        ArrayList<Size> sizes = new ArrayList<>();
        if (mCamera == null) return sizes;
        android.hardware.Camera.Parameters cameraParameters = getCameraParameters();
        if (cameraParameters == null) return sizes;
        List<android.hardware.Camera.Size> supportedPreviewSizes = cameraParameters.getSupportedPreviewSizes();
        for (android.hardware.Camera.Size previewSize : supportedPreviewSizes) {
            sizes.add(new Size(previewSize));
        }
        return sizes;
    }

    public void setFacing(int facing) {
        mBuilder.mFacing = facing;
        open();
    }

    public void setFlashMode(String... flashModes) {
        if (mCamera == null) return;
        android.hardware.Camera.Parameters parameters = getParameters();
        if (parameters == null) return;
        setFlashMode(parameters, Arrays.asList(flashModes));
        mCamera.setParameters(parameters);
    }

    public void setWhiteBalance(String... whiteBalance) {
        if (mCamera == null) return;
        android.hardware.Camera.Parameters parameters = getParameters();
        if (parameters == null) return;
        setWhiteBalance(parameters, Arrays.asList(whiteBalance));
        mCamera.setParameters(parameters);
    }

    public void setMeteringAreas(Rect... rect) {
        if (mCamera == null || rect == null || rect.length == 0) return;
        android.hardware.Camera.Parameters parameters = getParameters();
        if (parameters != null) {
            int maxNumMeteringAreas = parameters.getMaxNumMeteringAreas();
            if (maxNumMeteringAreas > 0) {
                ArrayList<android.hardware.Camera.Area> focusAreas = new ArrayList<>();
                for (int i = 0; i < maxNumMeteringAreas; i++) {
                    if (i < rect.length)
                        focusAreas.add(new android.hardware.Camera.Area(rect[i], 1000));
                }
                parameters.setMeteringAreas(focusAreas);
                mCamera.setParameters(parameters);
            }
        } else {
            loge("设置测光区域失败");
        }
    }

    /**
     * 设置曝光补偿
     *
     * @param compensation compensation <= maxExposureCompensation && compensation >= minExposureCompensation
     * @see android.hardware.Camera.Parameters#getMaxExposureCompensation()
     * @see android.hardware.Camera.Parameters#getMinExposureCompensation()
     */
    public void setExposureCompensation(int compensation) {
        if (mCamera == null) return;
        android.hardware.Camera.Parameters parameters = getParameters();
        if (parameters == null) return;
        int maxExposureCompensation = parameters.getMaxExposureCompensation();
        int minExposureCompensation = parameters.getMinExposureCompensation();
        if (compensation <= maxExposureCompensation && compensation >= minExposureCompensation) {
            parameters.setExposureCompensation(compensation);
            mCamera.setParameters(parameters);
        }

    }

    public void setRecordingHint(boolean recording) {
        if (mCamera == null) return;
        android.hardware.Camera.Parameters parameters = getParameters();
        if (parameters == null) return;
        parameters.setRecordingHint(recording);
        mCamera.setParameters(parameters);
    }

    public void setAntibanding(String... antibanding) {
        if (mCamera == null) return;
        android.hardware.Camera.Parameters parameters = getParameters();
        if (parameters == null) return;
        setAntibanding(parameters, Arrays.asList(antibanding));
        mCamera.setParameters(parameters);
    }

    public void setFocusMode(String... focusModes) {
        if (mCamera == null) return;
        android.hardware.Camera.Parameters parameters = getParameters();
        if (parameters == null) return;
        setFocusMode(parameters, Arrays.asList(focusModes));
        mCamera.setParameters(parameters);
    }

    public void setColorEffects(String... colorEffects) {
        if (mCamera == null) return;
        android.hardware.Camera.Parameters parameters = getParameters();
        if (parameters == null) return;
        setColorEffect(parameters, Arrays.asList(colorEffects));
        mCamera.setParameters(parameters);
    }

    public void setSceneMode(String... sceneModels) {
        if (mCamera == null) return;
        android.hardware.Camera.Parameters parameters = getParameters();
        if (parameters == null) return;
        setSceneMode(parameters, Arrays.asList(sceneModels));
        mCamera.setParameters(parameters);
    }

    public void setZoom(int zoom) {
        if (mCamera == null) return;
        android.hardware.Camera.Parameters parameters = getParameters();
        if (parameters == null) return;
        setZoom(parameters, zoom);
        mCamera.setParameters(parameters);
    }

    public void smoothZoom(int zoom) {
        if (mCamera == null) return;
        android.hardware.Camera.Parameters parameters = getParameters();
        if (parameters == null) return;
        int maxZoom = parameters.getMaxZoom();
        int target = clamp(zoom, 0, maxZoom);
        if (parameters.isZoomSupported() && parameters.isSmoothZoomSupported()) {
            mCamera.startSmoothZoom(target);
        }
    }

    public void setCameraParameters(android.hardware.Camera.Parameters parameters) {
        if (mCamera == null || parameters == null) return;
        mCamera.setParameters(parameters);
    }

    public List<FpsRange> getSupportedPreviewFpsRange() {
        ArrayList<FpsRange> fpsRanges = new ArrayList<>();
        if (mCamera == null) return fpsRanges;
        android.hardware.Camera.Parameters parameters = getParameters();
        if (parameters == null) return fpsRanges;
        List<int[]> supportedPreviewFpsRange = parameters.getSupportedPreviewFpsRange();
        for (int[] ints : supportedPreviewFpsRange) {
            fpsRanges.add(new FpsRange(ints));
        }
        return fpsRanges;
    }

    public void setPreviewFpsRange(FpsRange fpsRange) {
        if (mCamera == null) return;
        android.hardware.Camera.Parameters parameters = getParameters();
        if (parameters == null) return;
        List<FpsRange> supportedPreviewFpsRange = getSupportedPreviewFpsRange();
        if (supportedPreviewFpsRange.contains(fpsRange)) {
            parameters.setPreviewFpsRange(fpsRange.min, fpsRange.max);
            mCamera.setParameters(parameters);
        }
    }

    public void cancelAutoFocus() {
        if (mCamera == null) return;
        mCamera.cancelAutoFocus();
    }

    public boolean isOpen() {
        return mOpened;
    }

    @SuppressWarnings("unused")
    public static class CameraBuilder {

        private final Context mContext;
        private int mFacing;
        private Size mPictureSize;
        private Size mPreviewSize;
        private List<String> mFlashModes;
        private SurfaceTexture mSurfaceTexture;
        private SurfaceHolder mSurfaceHolder;
        private List<CameraCallback> mCameraCallbacks = new ArrayList<>();
        private List<String> mFocusModes;
        private List<String> mSceneModes;
        private List<Integer> mPreviewFormats;
        private List<String> mColorEffects;
        private List<Integer> mPictureFormats;
        private int mZoom = -1;
        private int mDisplayOrientation = -1;
        private android.hardware.Camera.AutoFocusCallback mAutoFocusCallback;
        private List<String> mAntibanding;
        private SizeSelector mPreviewSizeSelector;
        private SizeSelector mPictureSizeSelector;
        private boolean mRecordingHint;
        private List<String> mWhiteBalances;

        public CameraBuilder(Context context) {
            mContext = context;
        }

        public void release() {
            mSurfaceHolder = null;
            mSurfaceTexture = null;
            if (mCameraCallbacks != null) mCameraCallbacks.clear();
            mCameraCallbacks = null;
        }


        public CameraBuilder useDefaultConfig() {
            return setFacing(android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK)
                    .setPreviewSize(new Size(1920, 1080))
                    .setSceneMode(android.hardware.Camera.Parameters.SCENE_MODE_AUTO)
                    .setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
                    .setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_AUTO)
                    .setPictureSize(new Size(1920, 1080));
        }

        public CameraBuilder setAutoFocusCallback(android.hardware.Camera.AutoFocusCallback autoFocusCallback) {
            mAutoFocusCallback = autoFocusCallback;
            return this;
        }

        /**
         * 设置缩放 如果设备支持的话
         * 最小值为0 ， 最大值为 getMaxZoom() ， 如果设置的值大于获取的最大值 ， 那么将设置为MaxZoom
         *
         * @param zoom 缩放级别
         */
        public CameraBuilder setZoom(@IntRange(from = 0, to = Integer.MAX_VALUE) int zoom) {
            mZoom = zoom;
            return this;
        }

        public CameraBuilder setDisplayOrientation(int displayOrientation) {
            mDisplayOrientation = displayOrientation;
            return this;
        }

        /**
         * @param size 图片大小
         */
        public CameraBuilder setPictureSize(@NonNull Size size) {
            mPictureSize = size;
            return this;
        }

        /**
         * 是否是录制模式
         *
         * @param recording 录制模式
         * @see android.hardware.Camera.Parameters#setRecordingHint(boolean)
         */
        public CameraBuilder setRecordingHint(boolean recording) {
            mRecordingHint = recording;
            return this;
        }

        /**
         * @param size 相机预览大小
         */
        public CameraBuilder setPreviewSize(@NonNull Size size) {
            mPreviewSize = size;
            return this;
        }

        /**
         * 摄像头朝向
         *
         * @see android.hardware.Camera.CameraInfo#CAMERA_FACING_BACK
         * @see android.hardware.Camera.CameraInfo#CAMERA_FACING_FRONT
         */
        public CameraBuilder setFacing(@Facing int facing) {
            mFacing = facing;
            return this;
        }

        /**
         * 场景模式
         *
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
        public CameraBuilder setSceneMode(@SceneModel String... sceneModes) {
            mSceneModes = Arrays.asList(sceneModes);
            return this;
        }

        /**
         * 设置色彩效果
         *
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
        public CameraBuilder setColorEffects(@ColorEffect String... colorEffects) {
            mColorEffects = Arrays.asList(colorEffects);
            return this;
        }

        /**
         * 对焦模式
         *
         * @see android.hardware.Camera.Parameters#FOCUS_MODE_AUTO
         * @see android.hardware.Camera.Parameters#FOCUS_MODE_CONTINUOUS_VIDEO
         * @see android.hardware.Camera.Parameters#FOCUS_MODE_CONTINUOUS_PICTURE
         * @see android.hardware.Camera.Parameters#FOCUS_MODE_EDOF
         * @see android.hardware.Camera.Parameters#FOCUS_MODE_FIXED
         * @see android.hardware.Camera.Parameters#FOCUS_MODE_INFINITY
         * @see android.hardware.Camera.Parameters#FOCUS_MODE_MACRO
         */
        public CameraBuilder setFocusMode(@FocusModel String... focusModes) {
            mFocusModes = Arrays.asList(focusModes);
            return this;
        }

        /**
         * 图片格式
         *
         * @see ImageFormat#NV21
         * @see ImageFormat#JPEG
         * @see ImageFormat#RGB_565
         */
        public CameraBuilder setPictureFormat(@PictureFormat Integer... pictureFormat) {
            mPictureFormats = Arrays.asList(pictureFormat);
            return this;
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
        public CameraBuilder setAntibanding(@Antibanding String... antibanding) {
            mAntibanding = Arrays.asList(antibanding);
            return this;
        }

        /**
         * 预览帧格式
         *
         * @see ImageFormat#NV21
         * @see ImageFormat#YV12
         */
        public CameraBuilder setPreviewFormat(@PreviewFormat Integer... previewFormat) {
            mPreviewFormats = Arrays.asList(previewFormat);
            return this;
        }

        /**
         * 因为某些设备不支持指定的模式 ， 所以请设置闪光灯模式优先级列表
         *
         * @see android.hardware.Camera.Parameters#FLASH_MODE_AUTO
         * @see android.hardware.Camera.Parameters#FLASH_MODE_OFF
         * @see android.hardware.Camera.Parameters#FLASH_MODE_ON
         * @see android.hardware.Camera.Parameters#FLASH_MODE_RED_EYE
         * @see android.hardware.Camera.Parameters#FLASH_MODE_TORCH
         */
        public CameraBuilder setFlashMode(@FlashModel String... flashModes) {
            mFlashModes = Arrays.asList(flashModes);
            return this;
        }

        /**
         * 设置比白平衡
         *
         * @param whiteBalance 白平衡
         */
        public CameraBuilder setWhiteBalance(@WhiteBalance String... whiteBalance) {
            mWhiteBalances = Arrays.asList(whiteBalance);
            return this;
        }

        /**
         * 设置使用 SurfaceHolder 预览
         */
        public CameraBuilder setSurfaceHolder(@NonNull SurfaceHolder surfaceHolder) {
            mSurfaceHolder = surfaceHolder;
            return this;
        }

        /**
         * 设置使用 surfaceTexture 预览
         */
        public CameraBuilder setSurfaceTexture(@NonNull SurfaceTexture surfaceTexture) {
            mSurfaceTexture = surfaceTexture;
            return this;
        }

        /**
         * 添加相机预览尺寸选择器
         */
        public CameraBuilder setPreviewSizeSelector(SizeSelector sizeSelector) {
            mPreviewSizeSelector = sizeSelector;
            return this;
        }

        /**
         * 添加相机拍照尺寸选择器
         */
        public CameraBuilder setPictureSizeSelector(SizeSelector sizeSelector) {
            mPictureSizeSelector = sizeSelector;
            return this;
        }

        /**
         * 添加相机预览的回调
         */
        public CameraBuilder addPreviewCallback(@NonNull CameraCallback callback) {
            mCameraCallbacks.add(callback);
            return this;
        }

        /**
         * 构建相机参数
         *
         * @return Camera
         */
        public Camera build() {
            return new Camera(this.mContext, this);
        }

        /**
         * 构建相机参数
         *
         * @return Camera
         */
        public Camera build(CameraBuilder builder) {
            mFacing = builder.mFacing;
            mPictureSize = builder.mPictureSize;
            mPreviewSize = builder.mPreviewSize;
            mFlashModes = builder.mFlashModes;
            mSurfaceTexture = builder.mSurfaceTexture;
            mSurfaceHolder = builder.mSurfaceHolder;
            mCameraCallbacks = new ArrayList<>();
            mCameraCallbacks.addAll(builder.mCameraCallbacks);
            mFocusModes = builder.mFocusModes;
            mSceneModes = builder.mSceneModes;
            mPreviewFormats = builder.mPreviewFormats;
            mColorEffects = builder.mColorEffects;
            mWhiteBalances = builder.mWhiteBalances;
            mPictureFormats = builder.mPictureFormats;
            mZoom = builder.mZoom;
            mDisplayOrientation = builder.mDisplayOrientation;
            mAutoFocusCallback = builder.mAutoFocusCallback;
            mAntibanding = builder.mAntibanding;
            return new Camera(mContext, this);
        }

    }


    public interface SizeSelector {
        /**
         * 选择合适的尺寸
         *
         * @param supportedSizes 支持的尺寸列表
         * @param requestSize    期望的尺寸
         * @return 最终的尺寸
         */
        android.hardware.Camera.Size select(List<android.hardware.Camera.Size> supportedSizes, Size requestSize);
    }

    public interface CameraCallback {
        /**
         * 打开相机成功
         * 如果使用的是 SurfaceView预览 , 可以在这个回调里根据camera.getPreviewSize() 重新调整SurfaceView的大小比例 ， 避免预览变形
         */
        void onSuccess(Camera camera);

        /**
         * 相机预览失败回调
         *
         * @see Camera#NO_CAMERA
         * @see Camera#NOT_CONFIGURED
         * @see Camera#UNSUPPORTED_FACING
         * @see Camera#OPEN_ERROR
         * @see Camera#SUCCESS
         * @see Camera#PREVIEW_ERROR
         * @see Camera#UNKNOWN
         * @see Camera#UNSUPPORTED_PREVIEW_SIZE
         * @see Camera#UNSUPPORTED_PICTURE_SIZE
         */
        void onFailure(int code, String msg);

    }

    public void log(String msg) {
        if (LOG_ENABLE) Log.d(TAG, msg);
    }

    public void loge(String msg) {
        if (LOG_ENABLE) Log.e(TAG, msg);
    }

}
