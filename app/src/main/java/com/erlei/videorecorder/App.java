package com.erlei.videorecorder;

import android.app.Application;

import com.erlei.videorecorder.util.Config;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Config.app = this;
    }
}
