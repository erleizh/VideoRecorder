package com.erlei.gdx.utils;

import java.util.ArrayList;

public class FPSCounter {


    private final FPSCounterStrategy mStrategy;

    public FPSCounter(FPSCounterStrategy counterStrategy) {
        mStrategy = counterStrategy;
    }

    public void update(){
        mStrategy.update();
    }

    public float getFPS() {
        return mStrategy.getFPS();
    }

    public interface FPSCounterStrategy {
        float getFPS();

        void update();
    }

    /**
     * 实时计算
     * 使用上一帧的时间间隔进行计算
     */
    public static class FPSCounter2 implements FPSCounterStrategy {
        private long lastFrame = System.nanoTime();
        private float FPS = 0;

        public float getFPS() {
            return FPS;
        }

        @Override
        public void update() {
            long time = (System.nanoTime() - lastFrame);
            FPS = 1 / (time / 1000000000.0f);
            lastFrame = System.nanoTime();
        }
    }

    /**
     * 精确采样
     * 采样前N个帧，然后计算平均值
     */
    public static class FPSCounter1 implements FPSCounterStrategy {
        ArrayList<Long> mList;
        private long l;
        int mFrame = 10;
        private long msPerFrame = 1;

        public FPSCounter1() {
            mList = new ArrayList<>(mFrame);
        }

        public FPSCounter1(int frame) {
            mFrame = frame;
            mList = new ArrayList<>(mFrame);
        }

        @Override
        public float getFPS() {
            long sum = 0;
            for (Long aLong : mList) {
                sum += aLong;
            }
            return (float) (1e9 * mList.size() / sum);
        }

        @Override
        public void update() {
            long currentTime = System.nanoTime();
            msPerFrame = currentTime - l;
            l = currentTime;
            synchronized (this) {
                mList.add(msPerFrame);
                if (mList.size() > mFrame) {
                    mList.remove(0);
                }
            }
        }
    }
}
