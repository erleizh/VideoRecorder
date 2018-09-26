package com.erlei.videorecorder.util;

import android.util.Log;

public class LogUtil {
    public static final int LOG_NONE = 0;
    public static final int LOG_DEBUG = 3;
    public static final int LOG_INFO = 2;
    public static final int LOG_ERROR = 1;

    public static String TAG = Config.TAG;
    public static final boolean LOG_ENABLE = Config.DEBUG;

    public static void logd(String msg) {
        if (LOG_ENABLE) Log.d(TAG, msg);
    }

    public static void logd(String tag, String msg) {
        if (LOG_ENABLE) Log.d(TAG + "-" + tag, msg);
    }

    public static void loge(String msg) {
        Log.e(TAG, msg);
    }

    public static void loge(String tag, String msg) {
        if (LOG_ENABLE) Log.e(TAG + "-" + tag, msg);
    }

    public static void logi(String msg) {
        if (LOG_ENABLE) if (LOG_ENABLE) Log.i(TAG, msg);
    }

    public static void logi(String tag, String msg) {
        if (LOG_ENABLE) Log.i(TAG + "-" + tag, msg);
    }

    public static void logw(String msg) {
        if (LOG_ENABLE) Log.w(TAG, msg);
    }

    public static void logw(String tag, String msg) {
        if (LOG_ENABLE) Log.w(TAG + "-" + tag, msg);
    }

    public static void loge(String tag, String msg, Throwable throwable) {
        if (LOG_ENABLE) Log.e(TAG + "-" + tag, msg, throwable);
    }
}
