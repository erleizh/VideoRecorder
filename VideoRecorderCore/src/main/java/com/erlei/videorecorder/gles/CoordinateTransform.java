package com.erlei.videorecorder.gles;

import android.content.Context;
import android.content.res.Configuration;
import android.opengl.Matrix;
import android.support.annotation.IntDef;
import android.view.WindowManager;

import com.erlei.videorecorder.recorder.CameraController;
import com.erlei.videorecorder.util.LogUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;


@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class CoordinateTransform {
    protected final CameraController mCameraController;
    protected static final String TAG = LogUtil.TAG;

    protected float sVertexCoords[] = {
            -1.0f, -1.0f,       // 0 bottom left
            1.0f, -1.0f,        // 1 bottom right
            -1.0f, 1.0f,        // 2 top left
            1.0f, 1.0f,         // 3 top right
    };

    protected float TEXTURE_ROTATED_0[] = {
            0.0f, 0.0f,     // bottom left
            1.0f, 0.0f,     // bottom right
            0.0f, 1.0f,     // top left
            1.0f, 1.0f,     // top right
    };
    protected float TEXTURE_ROTATED_90[] = {
            1.0f, 0.0f,     // bottom right
            1.0f, 1.0f,     // top right
            0.0f, 0.0f,     // bottom left
            0.0f, 1.0f,     // top left
    };
    protected float TEXTURE_ROTATED_180[] = {
            1.0f, 1.0f,     // top right
            0.0f, 1.0f,     // top left
            1.0f, 0.0f,     // bottom right
            0.0f, 0.0f,     // bottom left
    };
    protected float TEXTURE_ROTATED_270[] = {
            0.0f, 1.0f,     // top left
            0.0f, 0.0f,     // bottom left
            1.0f, 1.0f,     // top right
            1.0f, 0.0f,     // bottom right
    };

    protected float sTexCoord2D[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
    };

    @android.support.annotation.Size(max = 16, min = 16)
    public abstract float[] getMVPMatrixOES();

    @android.support.annotation.Size(max = 16, min = 16)
    public float[] getMVPMatrix2D(){
        float[] floats = new float[16];
        Matrix.setIdentityM(floats,0);
        return floats;
    }

    @android.support.annotation.Size(max = 16, min = 16)
    public float[] getTextureMatrix2D(){
        float[] floats = new float[16];
        Matrix.setIdentityM(floats,0);
        return floats;
    }

    @android.support.annotation.Size(max = 16, min = 16)
    public float[] getTextureMatrixOES(){
        float[] floats = new float[16];
        Matrix.setIdentityM(floats,0);
        return floats;
    }

    @android.support.annotation.Size(max = 8, min = 8)
    public abstract float[] getOESTextureCoordinate();

    @android.support.annotation.Size(max = 8, min = 8)
    public abstract float[] get2DTextureCoordinate();

    @android.support.annotation.Size(max = 8, min = 8)
    public abstract float[] getVertexCoordinate();

    public static final int CENTER_INSIDE = 0, CENTER_CROP = 1, FIT_XY = 2;
    public static final int FLIP_NONE = 0, FLIP_HORIZONTAL = 1, FLIP_VERTICAL = 2;

    @IntDef({CENTER_CROP, CENTER_INSIDE, FIT_XY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScaleType {
    }

    @IntDef({FLIP_NONE, FLIP_HORIZONTAL, FLIP_VERTICAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FlipType {
    }

    public CoordinateTransform(CameraController controller) {
        this.mCameraController = controller;
    }

    /**
     * @return activity is  landscape
     */
    public boolean isLandscape() {
        WindowManager windowManager = (WindowManager) mCameraController.getContext().getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null && windowManager.getDefaultDisplay() != null) {
            int rotation = windowManager.getDefaultDisplay().getRotation();
            LogUtil.logd(TAG, "rotation=" + rotation);
            return rotation == ROTATION_90 || rotation == ROTATION_270;
        }
        return mCameraController.getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * @param coordinate 需要翻转的坐标数组
     * @return 获取翻转之后的坐标
     */
    public float[] getFlip(float[] coordinate, @FlipType int flipType) {
        LogUtil.logd(TAG, "coordinate=" + Arrays.toString(coordinate));
        float[] dest = null;
        switch (flipType) {
            case FLIP_HORIZONTAL:
                dest = new float[]{
                        coordinate[0], flip(coordinate[1]),
                        coordinate[2], flip(coordinate[3]),
                        coordinate[4], flip(coordinate[5]),
                        coordinate[6], flip(coordinate[7]),
                };
                break;
            case FLIP_VERTICAL:
                dest = new float[]{
                        flip(coordinate[0]), coordinate[1],
                        flip(coordinate[2]), coordinate[3],
                        flip(coordinate[4]), coordinate[5],
                        flip(coordinate[6]), coordinate[7],
                };
                break;
            case FLIP_NONE:
                break;
        }
        LogUtil.logd(TAG, "coordinate=" + Arrays.toString(dest));
        return dest == null ? coordinate : dest;
    }

    private static float flip(final float i) {
        return i == 0.0f ? 1.0f : 0.0f;
    }

    /**
     * 不使用 mvpMatrix 的话 , 可以用这个 配合  getFlip 实现预览 ,
     */
    public float[] getRotate() {
        if (mCameraController.isFront()) {
            switch (mCameraController.getDisplayOrientation()) {
                case 0:
                    return TEXTURE_ROTATED_180;
                case 90:
                    return TEXTURE_ROTATED_270;
                case 180:
                    return TEXTURE_ROTATED_0;
                case 270:
                    return TEXTURE_ROTATED_90;
            }
        } else {
            switch (mCameraController.getDisplayOrientation()) {
                case 0:
                    return TEXTURE_ROTATED_0;
                case 90:
                    return TEXTURE_ROTATED_90;
                case 180:
                    return TEXTURE_ROTATED_180;
                case 270:
                    return TEXTURE_ROTATED_270;
            }

        }
        return TEXTURE_ROTATED_90;
    }

    /**
     * @param scale  缩放大小
     * @param coords 矩阵
     * @return 缩小矩阵
     */
    public float[] setScale(float scale, float[] coords) {
        for (int i = 0; i < coords.length; i++) {
            coords[i] = ((coords[i] - 0.5f) * scale) + 0.5f;
        }
        return coords;
    }


    /**
     * @param m     矩阵
     * @param angle 角度
     * @return 旋转矩阵
     */
    public static float[] rotate(float[] m, float angle) {
        Matrix.rotateM(m, 0, angle, 0, 0, 1);
        return m;
    }

    /**
     * @param m 矩阵
     * @param x x 是否翻转
     * @param y y 是否翻转
     * @return 镜面翻转矩阵
     */
    public static float[] flip(float[] m, boolean x, boolean y) {
        if (x || y) {
            Matrix.scaleM(m, 0, x ? -1 : 1, y ? -1 : 1, 1);
        }
        return m;
    }

}
