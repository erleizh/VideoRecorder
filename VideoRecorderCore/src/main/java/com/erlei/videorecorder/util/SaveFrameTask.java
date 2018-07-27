package com.erlei.videorecorder.util;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.erlei.videorecorder.recorder.IVideoRecorder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SaveFrameTask extends AsyncTask<ByteBuffer, Void, File> {

    private int mWidth;
    private int mHeight;
    private IVideoRecorder.TakePictureCallback mPictureCallback;

    public SaveFrameTask(int width, int height, IVideoRecorder.TakePictureCallback pictureCallback) {
        mWidth = width;
        mHeight = height;
        mPictureCallback = pictureCallback;
    }

    @Override
    protected File doInBackground(ByteBuffer... buffers) {
        ByteBuffer buffer = buffers[0];

        reverseByteBuffer(buffer, mWidth, mHeight);
        Bitmap bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        buffer.rewind();
        bmp.copyPixelsFromBuffer(buffer);

        File file = mPictureCallback.onPictureTaken(bmp);
        if (file != null) {
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(file));
                bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
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
        }
        return file;
    }

    private void reverseByteBuffer(ByteBuffer buf, int width, int height) {
        long ts = System.currentTimeMillis();
        int i = 0;
        byte[] tmp = new byte[width * 4];
        while (i++ < height / 2) {
            buf.get(tmp);
            System.arraycopy(buf.array(), buf.limit() - buf.position(), buf.array(), buf.position() - width * 4, width * 4);
            System.arraycopy(tmp, 0, buf.array(), buf.limit() - buf.position(), width * 4);
        }
        buf.rewind();
        LogUtil.logd("reverseByteBuffer took " + (System.currentTimeMillis() - ts) + "ms");
    }

    @Override
    protected void onPostExecute(File file) {
        super.onPostExecute(file);
        if (file != null) {
            mPictureCallback.onPictureTaken(file);
        }
    }
}