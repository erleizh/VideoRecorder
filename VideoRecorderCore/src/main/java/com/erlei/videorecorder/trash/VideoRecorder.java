package com.erlei.videorecorder.trash;

import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.os.AsyncTask;

import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.recorder.OnDrawTextureListener;
import com.erlei.videorecorder.util.LogUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static com.erlei.videorecorder.gles.GLUtil.checkGlError;

/**
 * pbo 异步 glReadPixels , 不成功
 */
class VideoRecorder  {


    private IntBuffer mPboIds;
    private static final int PBO_NUM = 2;
    private int mPboSize = 0;
    private int mPboIndex = 0;
    private int mNextPboIndex = 1;
    private Buffer mBuffer;
    private Size mSize;


    private int mRowStride;

    private void initPixelBuffer(Size size) {
        destroyPixelBuffers();
        final int align = 128;//128字节对齐
//        mRowStride = (size.getWidth() * 4 + (align - 1)) & ~(align - 1);
//
//        mPboSize = mRowStride * size.getHeight();
        mPboSize = size.getHeight() * size.getWidth() * 4;//RGBA
        mPboIds = IntBuffer.allocate(PBO_NUM);
        GLES30.glGenBuffers(PBO_NUM, mPboIds);
        checkGlError("glGenBuffers");
        for (int i = 0; i < PBO_NUM; i++) {
            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(i));
            checkGlError("glBindBuffer");
            GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mPboSize, null, GLES30.GL_STATIC_READ);
            checkGlError("glBufferData");
        }
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
        mBuffer = ByteBuffer.allocate(mPboSize).position(0);
    }

    public void onCameraViewStarted(Size size) {
        mSize = size;
        LogUtil.loge("onCameraViewStarted size = "+ size);
        initPixelBuffer(size);
    }

    public void onCameraViewStopped() {
        LogUtil.loge("onCameraViewStopped");
        destroyPixelBuffers();
    }

//    //
//    @Override
//    public boolean drawTexture(int texIn, int texOut, Size size) {
//        long l = System.currentTimeMillis();
//        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(0));
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            GLES30.glReadPixels(0, 0, size.getWidth(), size.getHeight(), GLES30.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, 0);
//            LogUtil.loge("glReadPixels : " + (System.currentTimeMillis() - l) + "ms");
//        }
//        l = System.currentTimeMillis();
//        ByteBuffer buffer = (ByteBuffer) GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER, 0, mPboSize, GLES30.GL_MAP_READ_BIT);
//        new SaveFrameTask(new File(mCameraGLView.getContext().getExternalCacheDir(), System.currentTimeMillis() + ".jpg"), size.getWidth(), size.getHeight()).execute(buffer);
//        LogUtil.loge("glMapBufferRange : " + (System.currentTimeMillis() - l) + "ms");
//        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER);
//        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
//        return false;
//    }


    long num_downloads;
    int index;

    public boolean drawTexture(int i, int i1) {
//        GLES30.glPixelStorei(GLES30.GL_PACK_ALIGNMENT, 4);
//        long l = System.currentTimeMillis();
//        if (num_downloads < PBO_NUM) {
//            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(index));
//            GLUtil.glReadPixels(0, 0, size.getWidth(), size.getHeight(), GLES30.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, 0);
//        } else {
//            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(index));
//            ByteBuffer buffer = (ByteBuffer) GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER, 0, mPboSize, GLES30.GL_MAP_READ_BIT);
//            if (buffer != null) {
//                boolean b = GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER);
//                LogUtil.loge("glUnmapBuffer : " + b);
//                checkGlError("unmap buffer range");
//            }
//            GLUtil.glReadPixels(0, 0, size.getWidth(), size.getHeight(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, 0);
//            LogUtil.loge("glReadPixels : " + (System.currentTimeMillis() - l) + "ms");
////            new SaveFrameTask(new File(mCameraGLView.getContext().getExternalCacheDir(), System.currentTimeMillis() + ".jpg"), size.getWidth(), size.getHeight()).execute(buffer);
//        }
//        ++index;
//        index = index % PBO_NUM;
//        num_downloads++;
//        if (num_downloads == Long.MAX_VALUE) {
//            num_downloads = PBO_NUM;
//        }
//        LogUtil.loge((System.currentTimeMillis() - l) + "ms" + "\t\t index = " + index + "\t\tnum_downloads = " + num_downloads);
//        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
        return false;
    }

    private void destroyPixelBuffers() {
        if (mPboIds != null) {
            GLES30.glDeleteBuffers(PBO_NUM, mPboIds);
            mPboIds = null;
        }
    }

    int count = 1;
//
//    @Override
//    public boolean drawTexture(int texIn, int texOut, Size size) {
//        LogUtil.loge("drawTexture");
//        LogUtil.loge("count : " + count++);
////        if (count < 60) return false;
//        long l = System.currentTimeMillis();
////        GLES20.glReadPixels(0, 0, size.getWidth(), size.getHeight(), GLES30.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mBuffer);
////        if (mBuffer != null) {
////            LogUtil.loge((System.currentTimeMillis() - l)+"ms\t\t"+mBuffer.toString());
////        }
//        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(mPboIndex));
//        checkGlError("bind pixel buffer object");
//        GLUtil.glReadPixels(0, 0, size.getWidth(), size.getHeight(), GLES30.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, 0);
//        LogUtil.loge("glReadPixels : " + (System.currentTimeMillis() - l) + "ms");
//        checkGlError("read pixels");
//        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(mNextPboIndex));
//        l = System.currentTimeMillis();
//        ByteBuffer buffer = (ByteBuffer) GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER, 0, mPboSize, GLES30.GL_MAP_READ_BIT);
//        LogUtil.loge("glMapBufferRange : " + (System.currentTimeMillis() - l) + "ms" + "\t\t " + buffer.toString());
//        checkGlError("map buffer range");
//        boolean b = GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER);
//        LogUtil.loge("glUnmapBuffer : " + b);
//        checkGlError("unmap buffer range");
//
//        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
//        LogUtil.loge((System.currentTimeMillis() - l) + "ms" + "\t\t mPboIndex = " + mPboIndex + "\t\tmNextPboIndex = " + mNextPboIndex);
//        mPboIndex = (mPboIndex + 1) % 2;
//        mNextPboIndex = (mNextPboIndex + 1) % 2;
////        new SaveFrameTask(new File(mCameraGLView.getContext().getExternalCacheDir(),System.currentTimeMillis()+".jpg"),size.getWidth(),size.getHeight()).execute(buffer);
//        return false;
//    }


    public static class SaveFrameTask extends AsyncTask<ByteBuffer, Void, File> {

        private File filename;
        private int mWidth;
        private int mHeight;

        public SaveFrameTask(File filename, int width, int height) {
            this.filename = filename;
            mWidth = width;
            mHeight = height;
        }

        @Override
        protected File doInBackground(ByteBuffer... byteBuffers) {
            ByteBuffer byteBuffer = byteBuffers[0];
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(filename));
                Bitmap bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                byteBuffer.rewind();
                bmp.copyPixelsFromBuffer(byteBuffer);
                bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
                bmp.recycle();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }

}
