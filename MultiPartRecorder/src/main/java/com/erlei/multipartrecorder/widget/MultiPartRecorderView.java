package com.erlei.multipartrecorder.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.erlei.multipartrecorder.R;
import com.erlei.videorecorder.util.LogUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * 分段视频录制视图
 */
@SuppressWarnings("all")
public class MultiPartRecorderView extends View {

    private int mMinDuration;
    private int mMaxDuration;
    private int mDividerWidth;
    private int mBackgroundColor;
    private int mForegroundColor;
    private int mMinRecordColor;
    private int mPauseDividerColor;
    private int mDividerColor;
    private int mRemovePartColor;

    public void setRecordListener(RecordListener listener) {
        mListener = listener;
    }

    private RecordListener mListener;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            invalidate();
            if (mListener != null) {
                long duration = getDuration();
                if (duration > getMaxRecordTimeMillis()) mListener.onOvertakeMaxTime(mParts);
                if (duration > getMinRecordTimeMillis()) mListener.onOvertakeMinTime();
                mListener.onDurationChange(duration);
            }
            if (isRunning) postDelayed(mRunnable, 10);
        }
    };

    private ArrayList<Part> mParts = new ArrayList<>();
    private Paint mPaint;
    private boolean isRunning;

    public MultiPartRecorderView(Context context) {
        this(context, null);
    }

    public MultiPartRecorderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MultiPartRecorderView);
        mDividerWidth = typedArray.getDimensionPixelSize(R.styleable.MultiPartRecorderView_divider_width, dp2px(1));
        mBackgroundColor = typedArray.getColor(R.styleable.MultiPartRecorderView_background_color, Color.BLACK);
        mForegroundColor = typedArray.getColor(R.styleable.MultiPartRecorderView_foreground_color, Color.WHITE);
        mMinRecordColor = typedArray.getColor(R.styleable.MultiPartRecorderView_min_record_time_divider_color, Color.YELLOW);
        mDividerColor = typedArray.getColor(R.styleable.MultiPartRecorderView_divider_color, Color.BLACK);
        mPauseDividerColor = typedArray.getColor(R.styleable.MultiPartRecorderView_pause_divider_color, Color.BLACK);
        mRemovePartColor = typedArray.getColor(R.styleable.MultiPartRecorderView_remove_part_color, Color.RED);
        mMinDuration = typedArray.getInteger(R.styleable.MultiPartRecorderView_min_record_time, 0);
        mMaxDuration = typedArray.getInteger(R.styleable.MultiPartRecorderView_max_record_time, -1);

        typedArray.recycle();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }

    public int getMinDuration() {
        return mMinDuration;
    }

    public void setMinDuration(int minDuration) {
        mMinDuration = minDuration;
    }

    public int getMaxDuration() {
        return mMaxDuration;
    }

    public void setMaxDuration(int maxDuration) {
        mMaxDuration = maxDuration;
    }

    public int getDividerWidth() {
        return mDividerWidth;
    }

    public void setDividerWidth(int dividerWidth) {
        mDividerWidth = dividerWidth;
    }

    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    @Override
    public void setBackgroundColor(int backgroundColor) {
        mBackgroundColor = backgroundColor;
    }

    public int getForegroundColor() {
        return mForegroundColor;
    }

    public void setForegroundColor(int foregroundColor) {
        mForegroundColor = foregroundColor;
    }

    public int getMinRecordColor() {
        return mMinRecordColor;
    }

    public void setMinRecordColor(int minRecordColor) {
        mMinRecordColor = minRecordColor;
    }

    public int getPauseDividerColor() {
        return mPauseDividerColor;
    }

    public void setPauseDividerColor(int pauseDividerColor) {
        mPauseDividerColor = pauseDividerColor;
    }

    public int getDividerColor() {
        return mDividerColor;
    }

    public void setDividerColor(int dividerColor) {
        mDividerColor = dividerColor;
    }

    public int getRemovePartColor() {
        return mRemovePartColor;
    }

    public void setRemovePartColor(int removePartColor) {
        mRemovePartColor = removePartColor;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long maxRecordTimeMillis = getMaxRecordTimeMillis();
        //画背景颜色
        canvas.drawColor(mBackgroundColor);

        drawMinRecordTimeDivider(canvas, maxRecordTimeMillis);

        if (mParts.isEmpty()) return;

        drawForegroundColor(canvas, maxRecordTimeMillis);

        drawPartDivider(canvas, maxRecordTimeMillis);

        drawRemovePart(canvas, maxRecordTimeMillis);
    }

    /**
     * 要刪除的视频块
     */
    private void drawRemovePart(Canvas canvas, long maxRecordTimeMillis) {
        for (int i = 0; i < mParts.size(); i++) {
            Part part = mParts.get(i);
            if (part.remove) {
                mPaint.setStrokeWidth(getMeasuredHeight());
                mPaint.setColor(mRemovePartColor);
                if (i == 0) {
                    int right = (int) mapValue(getDuration(i), 0, maxRecordTimeMillis, getLeft(), getRight());
                    canvas.drawLine(0, getMeasuredHeight() / 2, right, getMeasuredHeight() / 2, mPaint);
                } else {
                    int right = (int) mapValue(getDuration(i), 0, maxRecordTimeMillis, getLeft(), getRight());
                    int left = (int) mapValue(getDuration(i - 1), 0, maxRecordTimeMillis, getLeft(), getRight());
                    canvas.drawLine(left, getMeasuredHeight() / 2, right, getMeasuredHeight() / 2, mPaint);
                }
            }
        }
    }

    /**
     * 画前景色 , 随着视频录制时长不断增加
     */
    private void drawForegroundColor(Canvas canvas, long maxRecordTimeMillis) {
        mPaint.setStrokeWidth(getMeasuredHeight() + 1);//有些手机没盖住背景色 , 导致有底部背景色漏出一条缝
        mPaint.setColor(mForegroundColor);
        int right = (int) mapValue(getDuration(), 0, maxRecordTimeMillis, getLeft(), getRight());
        canvas.drawLine(0, getMeasuredHeight() / 2, right, getMeasuredHeight() / 2, mPaint);
        drawPartDivider(canvas, maxRecordTimeMillis);
    }

    /**
     * 画视频段分割线
     */
    private void drawPartDivider(Canvas canvas, long maxRecordTimeMillis) {
        mPaint.setStrokeWidth(mDividerWidth);
        for (int i = 0; i < mParts.size(); i++) {
            int position = (int) mapValue(getDuration(i), 0, maxRecordTimeMillis, getLeft(), getRight());
            if (i == mParts.size() - 1) {
                mPaint.setColor(mPauseDividerColor);
                canvas.drawLine(position, 0, position, getMeasuredHeight(), mPaint);
            } else {
                mPaint.setColor(mDividerColor);
                canvas.drawLine(position, 0, position, getMeasuredHeight(), mPaint);
            }
        }
    }

    /**
     * 画最小视频时长分割线
     */
    private void drawMinRecordTimeDivider(Canvas canvas, long maxRecordTimeMillis) {
        mPaint.setColor(mMinRecordColor);
        mPaint.setStrokeWidth(mDividerWidth);
        int v = (int) mapValue(getMinRecordTimeMillis(), 0, maxRecordTimeMillis, getLeft(), getRight());
        canvas.drawLine(v, 0, v, getMeasuredHeight(), mPaint);
    }

    public long getMinRecordTimeMillis() {
        return mMinDuration * 1000;
    }

    /**
     * 如果没有设置最大录制时间 , 那么最大录制时间会是 getDuration() * 2
     *
     * @return 最大录制时间
     */
    public long getMaxRecordTimeMillis() {
        return mMaxDuration <= 0 ? getDuration() * 2 : mMaxDuration * 1000;
    }

    /**
     * @return 总录制时长
     */
    public long getDuration() {
        return getDuration(mParts.size() - 1);
    }

    /**
     * @param index 视频块index
     * @return 返回这个视频块之前的总时长
     */
    public long getDuration(int index) {
        long duration = 0;
        for (int i = 0; i < index + 1; i++) {
            duration += mParts.get(i).getDuration();
        }
        return duration;
    }

    /**
     * Map a value within a given range to another range.
     *
     * @param value    the value to map
     * @param fromLow  the low end of the range the value is within
     * @param fromHigh the high end of the range the value is within
     * @param toLow    the low end of the range to map to
     * @param toHigh   the high end of the range to map to
     * @return the mapped value
     */
    private static double mapValue(
            double value,
            double fromLow,
            double fromHigh,
            double toLow,
            double toHigh) {
        double fromRangeSize = fromHigh - fromLow;
        double toRangeSize = toHigh - toLow;
        double valueScale = (value - fromLow) / fromRangeSize;
        return toLow + (valueScale * toRangeSize);
    }

    /**
     * 继续录制的时候 , 如果有视频块标记为删除状态, 那么恢复
     */
    private void resetPartState() {
        for (int i = 0; i < mParts.size(); i++) {
            setPartStatus(i, false);
        }
    }

    /**
     * 继续录制的时候 , 如果上一个视频块标记为删除状态 , 那么恢复
     */
    private void resetLastPartState() {
        setPartStatus(mParts.size() - 1, false);
    }

    /**
     * @return 最后一个视频块是不是标记为待删除状态
     */
    public boolean lastPartRemoved() {
        return !mParts.isEmpty() && mParts.get(mParts.size() - 1).remove;
    }

    public Part removeLastPart() {
        return removePart(mParts.size() - 1);
    }

    /**
     * 删除进度块
     */
    public Part removePart(int index) {
        if (index > mParts.size() || index < 0) return null;
        Part remove = mParts.remove(index);
        invalidate();
        return remove;
    }

    /**
     * 删除进度块
     */
    public Part removePart(String path) {
        int i = mParts.indexOf(new Part(path, 0));
        if (i >= 0) {
            Part remove = mParts.remove(i);
            invalidate();
            return remove;
        }
        return null;
    }

    public void getPart() {

    }

    public int getPartCount() {
        return mParts == null ? 0 : mParts.size();
    }

    public ArrayList<Part> getParts() {
        return mParts;
    }

    /**
     * 标记最后一个分块为删除状态
     */
    public void markLastPartRemove() {
        setPartStatus(mParts.size() - 1, true);
    }

    public void removeAllPart() {
        if (mParts != null) mParts.clear();
        invalidate();
    }

    /**
     * 设置分块的状态
     *
     * @param remove 是否删除
     */
    public void setPartStatus(int index, boolean remove) {
        if (index > mParts.size() || index < 0) return;
        mParts.get(index).remove = remove;
        invalidate();
    }

    public void startRecord(File outputFile) {
        isRunning = true;
        resetLastPartState();
        mParts.add(new Part(outputFile.getAbsolutePath(), System.currentTimeMillis()));
        post(mRunnable);
        LogUtil.loge(outputFile.getAbsolutePath());
    }

    public void stopRecord() {
        isRunning = false;
        mParts.get(mParts.size() - 1).stopTime = System.currentTimeMillis();
        removeCallbacks(mRunnable);
    }


    public class Part {
        private long startTime, duration = -1, stopTime = -1;
        private boolean remove;
        private String file;

        Part(String output, long millis) {
            file = output;
            startTime = millis;
        }

        private long getDuration() {
            if (duration <= -1) {
                return (stopTime == -1 ? System.currentTimeMillis() : stopTime) - startTime;
            } else {
                return duration;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Part part = (Part) o;

            if (!file.equals(part.file)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }

        @Override
        public String toString() {
            return "Part{" +
                    "startTime=" + startTime +
                    ", stopTime=" + stopTime +
                    ", duration=" + getDuration() +
                    ", remove=" + remove +
                    '}';
        }
    }

    private int dp2px(int dp) {
        return (int) (getContext().getResources().getDisplayMetrics().density * dp + 0.5F);
    }

    private int px2dp(int px) {
        return (int) (px / getContext().getResources().getDisplayMetrics().density + 0.5F);
    }

    public static interface RecordListener {

        /**
         * 超过最小录制时间
         */
        void onOvertakeMinTime();

        /**
         * 超过最大录制时间
         *
         * @param parts 视频块
         */
        void onOvertakeMaxTime(ArrayList<Part> parts);

        /**
         * 视频时长改变
         *
         * @param duration 时长
         */
        void onDurationChange(long duration);
    }
}
