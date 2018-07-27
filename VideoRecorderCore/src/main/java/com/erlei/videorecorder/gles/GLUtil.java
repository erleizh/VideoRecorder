package com.erlei.videorecorder.gles;

import android.opengl.GLES20;

import com.erlei.videorecorder.util.LogUtil;

public class GLUtil {

    static {
        System.loadLibrary("glutil-lib");
    }

    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            LogUtil.logd(msg);
            throw new RuntimeException(msg);
        }
    }
    public static void checkLocation(int location, String label) {
        if (location < 0) {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }

    public static native void glReadPixels(
            int x,
            int y,
            int width,
            int height,
            int format,
            int type,
            int offset
    );
}
