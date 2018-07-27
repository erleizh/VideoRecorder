package com.erlei.videorecorder.gles;


import android.opengl.Matrix;

import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.recorder.CameraController;
import com.erlei.videorecorder.util.LogUtil;

@SuppressWarnings({"WeakerAccess", "unused"})
public class DefaultCoordinateTransform extends CoordinateTransform {

    private float[] mVertexCoordinate;
    private float[] mTextureCoordinate;

    public DefaultCoordinateTransform(CameraController view) {
        super(view);
        mTextureCoordinate = TEXTURE_ROTATED_0;
        mVertexCoordinate = sVertexCoords;
    }

    @Override
    public float[] getMVPMatrixOES() {
        Size cameraSize = mCameraController.getCameraSize();
        Size surfaceSize = mCameraController.getSurfaceSize();

        //忘了当初为啥这样写的了 T-T , 不判断横竖屏的话竖屏预览会变形 , 横屏没毛病 ,
        //大概是跟Camera的物理方向是横屏的有关 , 用矩阵应该也行 ,我还是简单粗暴的判断一下算了
        if (!isLandscape()) cameraSize = new Size(cameraSize.getHeight(), cameraSize.getWidth());

        float cameraWidth = cameraSize.getWidth();
        float cameraHeight = cameraSize.getHeight();
        LogUtil.logd(TAG, "cameraSize = " + cameraWidth + "x" + cameraHeight);
        float surfaceWidth = surfaceSize.getWidth();
        float surfaceHeight = surfaceSize.getHeight();
        LogUtil.logd(TAG, "surfaceSize = " + surfaceWidth + "x" + surfaceHeight);
        float cameraAspectRatio = cameraWidth / cameraHeight;
        float surfaceAspectRatio = surfaceWidth / surfaceHeight;
        LogUtil.logd(TAG, "cameraAspectRatio = " + cameraAspectRatio + "\t\t surfaceAspectRatio = " + surfaceAspectRatio);
        // 模型矩阵
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);

        //由于mTexture.getTransformMatrix(mTexMatrix); 已经处理了纹理矩阵 , 所以不用考虑纹理方向
        //所以只需要处理下纹理变形的问题 , 需要将纹理的比例恢复到原比例 (cameraSize)
        //1 . 恢复纹理比例
        Matrix.scaleM(modelMatrix, 0, cameraWidth / surfaceWidth, cameraHeight / surfaceHeight, 0f);
        LogUtil.logd(TAG, "scalex = " + (cameraWidth / surfaceWidth) + "\t\t scaley = " + (cameraHeight / surfaceHeight));

        //2 . CENTER_CROP (see ImageView CENTER_CROP)
        float scale;
        float dx = 0, dy = 0;
        if (cameraWidth * surfaceHeight > surfaceWidth * cameraHeight) {
            scale = surfaceHeight / cameraHeight;
            dx = (surfaceWidth - cameraWidth * scale) * 0.5f;
        } else {
            scale = surfaceWidth / cameraWidth;
            dy = (surfaceHeight - cameraHeight * scale) * 0.5f;
        }
        LogUtil.logd(TAG, "scale = " + scale + "\t\t dx = " + (dx / cameraWidth) + "\t\t dy = " + (dy / cameraHeight));
        Matrix.scaleM(modelMatrix, 0, scale, scale, 0f);
        // TODO: 2018/5/22 这个地方最好需要平移一下 ， 不过不平移也看不出来 ，
        // 算法错误 ，需要重新大量测试一下 ， 在高分辨率下平移没问题 ， 低分辨率平移有毛病
        //Matrix.translateM(modelMatrix, 0, dx / cameraWidth,  dy / cameraHeight, 0f);
        return modelMatrix;

    }


    /**
     * @return 获取纹理坐标
     */
    @Override
    public float[] getOESTextureCoordinate() {
        return mTextureCoordinate;
    }

    @Override
    public float[] get2DTextureCoordinate() {
        return mTextureCoordinate;
    }

    @Override
    public float[] getVertexCoordinate() {
        return mVertexCoordinate;
    }
}
