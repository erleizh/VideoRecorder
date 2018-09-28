/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.erlei.gdx.utils;

import android.util.Log;

import com.erlei.gdx.Application;

/**
 * Simple logger that uses the {@link Application} logging facilities to output messages. The log level set with
 *  overrides the log level set here.
 *
 * @author mzechner
 * @author Nathan Sweet
 */
public class Logger {
    public static final int NONE = 0;
    public static final int ERROR = 1;
    public static final int WARN = 2;
    public static final int INFO = 3;
    public static final int DEBUG = 4;
    public static int LOG_LEVEL = DEBUG;

    private final String tag;
    private int level = INFO;

    public Logger(String tag) {
        this(tag, ERROR);
    }

    public Logger(String tag, int level) {
        this.tag = tag;
        this.level = level;
    }

    public void debug(String message) {
        if (level >= DEBUG) debug(tag, message);
    }

    public void debug(String message, Exception exception) {
        if (level >= DEBUG) debug(tag, message, exception);
    }

    public void info(String message) {
        if (level >= INFO) info(tag, message);
    }

    public void info(String message, Exception exception) {
        if (level >= INFO) info(tag, message, exception);
    }

    public void warn(String message) {
        if (level >= WARN) warn(tag, message);
    }

    public void warn(String message, Exception exception) {
        if (level >= WARN) warn(tag, message, exception);
    }

    public void error(String message) {
        if (level >= ERROR) error(tag, message);
    }

    public void error(String message, Throwable exception) {
        if (level >= ERROR) error(tag, message, exception);
    }

    public static void debug(String tag, String message) {
        if (LOG_LEVEL >= DEBUG) Log.d(tag, message);
    }

    public static void debug(String tag, String message, Throwable exception) {
        if (LOG_LEVEL >= DEBUG) Log.d(tag, message, exception);
    }

    public static void info(String tag, String message, Throwable exception) {
        if (LOG_LEVEL >= INFO) Log.i(tag, message, exception);
    }

    public static void info(String tag, String message) {
        if (LOG_LEVEL >= INFO) Log.i(tag, message);
    }

    public static void warn(String tag, String message) {
        if (LOG_LEVEL >= WARN) Log.w(tag, message);
    }

    public static void warn(String tag, String message, Throwable exception) {
        if (LOG_LEVEL >= WARN) Log.w(tag, message, exception);
    }

    public static void error(String tag, String message) {
        if (LOG_LEVEL >= ERROR) Log.e(tag, message);
    }

    public static void error(String tag, String message, Throwable exception) {
        if (LOG_LEVEL >= ERROR) Log.e(tag, message, exception);
    }


    /**
     * Sets the log level. {@link #NONE} will mute all log output. {@link #ERROR} will only let error messages through.
     * {@link #INFO} will let all non-debug messages through, and {@link #DEBUG} will let all messages through.
     *
     * @param level {@link #NONE}, {@link #ERROR}, {@link #INFO}, {@link #DEBUG}.
     */
    public void setLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
