package com.erlei.videorecorder.widget;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.erlei.videorecorder.camera.Camera;
import com.erlei.videorecorder.camera.FpsRange;
import com.erlei.videorecorder.camera.Size;

import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;


/**
 * Created by lll on 2018/4/12
 * Email : lllemail@foxmail.com
 * Describe : 使用TextureView进行相机预览
 */
public class CameraGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, CameraGLView {


    private Camera mCamera;
    private CameraGLRenderer mRenderer;
    private Camera.CameraBuilder mCameraBuilder;
    private CameraTextureCallback mCameraTextureCallBack;


    public CameraGLSurfaceView(Context context) {
        this(context, null);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        mRenderer = new CameraGLRenderer(this);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }


    @Override
    public Size getCameraSize() {
        return mCamera == null ? new Size(0, 0) : mCamera.getPreviewSize();
    }

    @Override
    public Size getSurfaceSize() {
        return new Size(getWidth(), getHeight());
    }

    @Override
    public boolean openCamera(SurfaceTexture texture) {
        if (mCameraBuilder == null) {
            mCamera = new Camera.CameraBuilder(getContext())
                    .useDefaultConfig()
                    .setSurfaceTexture(texture)
                    .build()
                    .open();
        } else {
            mCameraBuilder.setSurfaceTexture(texture);
            mCamera = new Camera.CameraBuilder(getContext()).build(mCameraBuilder).open();
        }
        return mCamera != null;
    }

    @Override
    public void closeCamera() {
        if (mCamera != null) mCamera.close();
    }

    @Override
    public void setCameraBuilder(Camera.CameraBuilder cameraBuilder) {
        mCameraBuilder = cameraBuilder;
    }

    @Override
    public List<Size> getSupportedPreviewSizes() {
        return mCamera.getSupportedPreviewSizes();
    }

    @Override
    public Camera.CameraBuilder getCameraBuilder() {
        return null;
    }

    @Override
    public void setPreviewFpsRange(FpsRange fpsRange) {

    }

    @Override
    public void setZoomOnTouch(MotionEvent event) {

    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mRenderer.updateState();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        mRenderer.updateState();
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

    }

    @Override
    public void setMeteringAreaOnTouch(MotionEvent event) {

    }

    @Override
    public android.hardware.Camera.Parameters getCameraParameters() {
        return null;
    }

    @Override
    public Camera getCamera() {
        return null;
    }

    @Override
    public void setCameraParameters(android.hardware.Camera.Parameters parameters) {

    }

    @Override
    public boolean isFront() {
        return mCamera != null && mCamera.getFacing() == CAMERA_FACING_FRONT;
    }

    @Override
    public void setZoom(int zoom) {

    }

    /**
     * 平滑缩放 如果设备支持的话
     * 最小值为0 ， 最大值为 getMaxZoom() ， 如果设置的值大于获取的最大值 ， 那么将设置为MaxZoom
     *
     * @param zoom 缩放级别
     */
    @Override
    public void startSmoothZoom(int zoom) {

    }


    @Override
    public void setSceneMode(String... sceneModels) {

    }

    @Override
    public void setColorEffects(String... colorEffects) {

    }

    @Override
    public void setFocusMode(String... focusModels) {

    }

    @Override
    public void setAntibanding(String... antibanding) {

    }

    @Override
    public void setFlashMode(String... flashModels) {

    }

    @Override
    public void setRecordingHint(boolean recording) {

    }

    @Override
    public void setFacing(int facing) {

    }

    @Override
    public void setWhiteBalance(String... whiteBalance) {

    }

    @Override
    public void setExposureCompensation(int compensation) {

    }

    @Override
    public void setMeteringAreas(Rect... rect) {

    }

    /**
     * @return 获取支持的预览帧率区间
     */
    @Override
    public List<FpsRange> getSupportedPreviewFpsRange() {
        return null;
    }

    @Override
    public void setFocusAreas(Rect... rect) {

    }

    @Override
    public void toggleFacing() {

    }

    @Override
    public void updateCameraParams(Camera.CameraBuilder builder) {
        mCameraBuilder = builder;
        setEnabled(false);
        setEnabled(true);
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mRenderer.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mRenderer.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mRenderer.onDrawFrame();
    }


    @Override
    public CameraTextureCallback getCameraTextureCallBack() {
        return mCameraTextureCallBack;
    }

    public void setCameraTextureCallBack(CameraTextureCallback cameraTextureCallBack) {
        mCameraTextureCallBack = cameraTextureCallBack;
    }

    @Override
    public void onResume() {
        super.onResume();
        mRenderer.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mRenderer.onPause();
    }
}
