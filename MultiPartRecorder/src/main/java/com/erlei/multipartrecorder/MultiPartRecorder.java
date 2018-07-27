package com.erlei.multipartrecorder;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Message;

import com.erlei.videorecorder.recorder.CameraController;
import com.erlei.videorecorder.recorder.IVideoRecorder;
import com.erlei.videorecorder.recorder.VideoRecorder;
import com.erlei.videorecorder.recorder.VideoRecorderHandler;
import com.erlei.videorecorder.util.LogUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 多段录制
 */
public class MultiPartRecorder extends VideoRecorderHandler implements IVideoRecorder {
    static {
        LogUtil.TAG = "MultiPartRecorder";
    }

    private static final String TAG = "MultiPartRecorder";
    private final VideoRecorder mRecorder;
    private File mOutputFile;
    private final Context mContext;
    private final VideoRecorder.Config mConfig;
    private final VideoRecorderHandler mDefaultViewHandler;
    private final List<Part> mParts;
    private VideoMergeListener mMergeListener;
    private List<VideoPartListener> mPartListeners;
    private boolean mDelPartEnable = true;
    private FileFilter mFileFilter;


    private MultiPartRecorder(VideoRecorder.Builder builder) {
        if (builder == null)
            throw new IllegalArgumentException("VideoRecorder.Builder must not null");
        mParts = new ArrayList<>();
        mConfig = builder.getConfig();
        mDefaultViewHandler = mConfig.getViewHandler();
        mContext = mConfig.getContext();
        setOutPut(builder);
        builder.setCallbackHandler(this);
        mRecorder = builder.build();
    }

    public File getCurrentPartFile() {
        return mRecorder.getOutputFile();
    }

    public File getOutputFile() {
        return mOutputFile;
    }

    @Override
    public SurfaceTexture getPreviewTexture() {
        return mRecorder.getPreviewTexture();
    }

    /**
     * 拍照
     */
    @Override
    public void takePicture(TakePictureCallback callback) {
        mRecorder.takePicture(callback);
    }

    /**
     * @param delPartEnable 是否开启视频块删除
     *                      初衷是考虑到把所有视频段放在录制结束的时候合并可能会存在过长的等待时间
     *                      所以考虑过录制完一个分段 ,就合并, 避免最终合并时间过长 , 但是经过不完全测试 ,
     *                      最后合并的时间消耗是可以接受的 , 暂时就这样处理吧 , 有需求再加
     */
    public void setDelPartEnable(boolean delPartEnable) {
        mDelPartEnable = delPartEnable;
    }

    /**
     * 根据文件路径删除视频块
     */
    public Part removePart(String path) {
        if (!mDelPartEnable || mParts.isEmpty()) return null;
        int index = mParts.indexOf(new Part(path));
        if (index >= 0) return mParts.remove(index);
        return null;

    }

    /**
     * 刪除最后的视频块
     * 警告 ，这个方法不要夹在onRecordVideoPartStarted 和 onRecordVideoPartSuccess 或 onRecordVideoPartFailure 之间调用
     * 会导致在快速切换录制状态时onRecordVideoPartSuccess 或 onRecordVideoPartFailure 回调丢失
     */
    public Part removeLastPart() {
        if (!mDelPartEnable || mParts.isEmpty()) return null;
        return mParts.remove(mParts.size() - 1);
    }

    public VideoMergeListener getMergeListener() {
        return mMergeListener;
    }

    public void setMergeListener(VideoMergeListener mergeListener) {
        mMergeListener = mergeListener;
    }


    public void addPartListener(VideoPartListener partListener) {
        if (mPartListeners == null) {
            mPartListeners = new ArrayList<>();
        }
        mPartListeners.add(partListener);
    }

    /**
     * 修改普通VideoRecorder的配置 , 让他将视频块输出到指定的文件夹下,
     * 让其支持多段录制
     */
    private void setOutPut(VideoRecorder.Builder builder) {
        mOutputFile = mConfig.getOutputFile();
        if (mOutputFile == null) {
            mOutputFile = getOutPut();
        }
        builder.setOutPutFile(null);
        File filesDir = mContext.getExternalCacheDir();
        if (filesDir == null) filesDir = mContext.getCacheDir();
        builder.setOutPutPath(new File(filesDir, TAG + File.separator).getAbsolutePath());

    }

    private File getOutPut() {
        //还是优先使用之前设置的文件存放路径
        String outputPath = mConfig.getOutputPath();
        if (outputPath == null) {
            File filesDir = mConfig.getContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            if (filesDir != null) outputPath = filesDir.getAbsolutePath();
            if (outputPath == null) {
                outputPath = new File(mConfig.getContext().getFilesDir(), TAG).getAbsolutePath();
            }
        }
        File path = new File(outputPath);
        if (!path.exists()) {
            //noinspection ResultOfMethodCallIgnored
            path.mkdirs();
        }
        if (path.isFile()) path = path.getParentFile();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        return new File(path, format.format(new Date()) + ".mp4");
    }

    /**
     * 合并视频片段
     */
    public AsyncTask<Part, Float, File> mergeVideoParts() {
        if (mParts == null || mParts.isEmpty()) return null;
        Part[] parts = new Part[mParts.size()];
        parts = mParts.toArray(parts);
        removeAllPart();
        return new VideoPartMergeTask(mOutputFile, mMergeListener, false, 2000).execute(parts);
    }

    @Override
    public void startPreview() {
        mRecorder.startPreview();
    }

    @Override
    public synchronized void startRecord() {
        mRecorder.setRecordEnabled(true);
    }

    @Override
    public synchronized void stopRecord() {
        mRecorder.setRecordEnabled(false);
    }

    public synchronized void setRecordEnabled(boolean enable) {
        mRecorder.setRecordEnabled(enable);
    }

    @Override
    public CameraController getCameraController() {
        return mRecorder.getCameraController();
    }

    @Override
    public boolean isRecordEnable() {
        return mRecorder.isRecordEnable();
    }

    /**
     * @return 混合器是否正在运行
     */
    @Override
    public boolean isMuxerRunning() {
        return mRecorder.isMuxerRunning();
    }

    @Override
    public void onSizeChanged(int width, int height) {
        mRecorder.onSizeChanged(width, height);
    }

    @Override
    public synchronized void stopPreview() {
        mRecorder.stopPreview();
    }

    @Override
    public void release() {
        if (mPartListeners != null) mPartListeners.clear();
    }


    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if (mDefaultViewHandler != null) {
            mDefaultViewHandler.handleMessage(msg);
        }
    }

    @Override
    protected void handleMediaCaptureStarted(String output) {
        LogUtil.logd("handleMediaCaptureStarted : " + output);
        Part part = new Part(output);
        mParts.add(part);
        if (mPartListeners != null) {
            for (VideoPartListener listener : mPartListeners) {
                listener.onRecordVideoPartStarted(part);
            }
        }
    }

    @Override
    protected void handleMediaCaptureStopped(String output) {
        super.handleMediaCaptureStopped(output);
        LogUtil.logd("handleMediaCaptureStopped : " + output);
    }

    @Override
    protected void handleVideoMuxerStarted(String output) {
        super.handleVideoMuxerStarted(output);
        LogUtil.logd("handleVideoMuxerStarted : " + output);
    }

    @Override
    protected void handleVideoMuxerStopped(String output) {
        int i = mParts.indexOf(new Part(output));
        LogUtil.logd("handleVideoMuxerStopped : " + "index = " + i + "\t\t" + output);
        if (i < 0) return;
        Part part = mParts.get(i);
        part.end();
        handleMuxerEnd(part);
    }

    private void handleMuxerEnd(Part part) {
        if (mPartListeners != null) {
            if (!part.file.exists()) {
                for (VideoPartListener listener : mPartListeners) {
                    listener.onRecordVideoPartFailure(part);
                }
            } else {
                if (mFileFilter != null) {
                    if (mFileFilter.filter(part)) {
                        for (VideoPartListener listener : mPartListeners) {
                            listener.onRecordVideoPartSuccess(part);
                        }
                    } else {
                        for (VideoPartListener listener : mPartListeners) {
                            listener.onRecordVideoPartFailure(part);
                        }
                    }
                } else {
                    if (part.duration > 1000 && part.file.length() > 1000) {
                        for (VideoPartListener listener : mPartListeners) {
                            listener.onRecordVideoPartSuccess(part);
                        }
                    } else {
                        for (VideoPartListener listener : mPartListeners) {
                            listener.onRecordVideoPartFailure(part);
                        }
                    }

                }

            }
        }
    }

    public void removeAllPart() {
        mParts.clear();
    }

    public interface VideoMergeListener {
        void onStart();

        void onSuccess(File outFile);

        void onError(Exception e);

        /**
         * 合并进度
         *
         * @param value 0 - 1
         */
        void onProgress(float value);
    }

    public interface FileFilter {

        boolean filter(Part part);

    }

    public interface VideoPartListener {
        /**
         * 一个视频块开始录制
         *
         * @param part 视频块
         */
        void onRecordVideoPartStarted(Part part);

        /**
         * 完成一个视频块的录制
         *
         * @param part 视频块
         *             判断是否完成是根据最终生成的文件大小 , 和录制时间决定的
         *             由于我没有找到一个好的判断条件 , 所以粗略的使用
         *             part.file.exists() && part.duration > 300 && part.file.length() > 1000
         *             判断了下 , 如果要求比较高可以自行使用
         *             MediaUtil.getVideoDuration(file.getAbsolutePath()); 但是获取这个信息大概耗时100毫秒
         */
        void onRecordVideoPartSuccess(Part part);

        /**
         * 一个损坏的视频
         *
         * @param part 视频块
         */
        void onRecordVideoPartFailure(Part part);
    }

    public static class Builder {

        private final VideoRecorder.Builder mBuilder;
        private final List<VideoPartListener> mVideoPartListeners = new ArrayList<>();
        private VideoMergeListener mMergeListener;
        private FileFilter mFileFilter;

        public Builder(VideoRecorder.Builder builder) {
            mBuilder = builder;
        }

        public MultiPartRecorder build() {
            MultiPartRecorder recorder = new MultiPartRecorder(mBuilder);
            for (VideoPartListener listener : mVideoPartListeners) {
                recorder.addPartListener(listener);
            }
            recorder.setMergeListener(mMergeListener);
            recorder.setFileFilter(mFileFilter);
            return recorder;
        }

        public Builder addPartListener(VideoPartListener videoPartListener) {
            mVideoPartListeners.add(videoPartListener);
            return this;
        }

        public Builder setFileFilter(FileFilter fileFilter) {
            mFileFilter = fileFilter;
            return this;
        }

        public Builder setMergeListener(VideoMergeListener mergeListener) {
            mMergeListener = mergeListener;
            return this;
        }
    }

    public void setFileFilter(FileFilter fileFilter) {
        mFileFilter = fileFilter;
    }


    public static class Part {
        /**
         * 视频段的大致时长 , 是用开始录制时间 , 结束录制时间来计算的 , 包含误差(编码性能),
         */
        public long duration;
        public long startTimeMillis;
        public long endTimeMillis = -1;
        public final File file;

        public Part(String output) {
            file = new File(output);
            startTimeMillis = System.currentTimeMillis();
        }

        public void end() {
            endTimeMillis = System.currentTimeMillis();
            duration = endTimeMillis - startTimeMillis;
//            duration = MediaUtil.getVideoDuration(file.getAbsolutePath()); 耗时100毫秒
        }

        /**
         * @return 是否正在录制中
         */
        public boolean isRecording() {
            return endTimeMillis == -1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Part part = (Part) o;
            return file.getPath().equals(part.file.getPath());
        }

        @Override
        public String toString() {
            return "Part{" +
                    "duration=" + duration +
                    ", startTimeMillis=" + startTimeMillis +
                    ", endTimeMillis=" + endTimeMillis +
                    ", file=" + file +
                    ", fileLength=" + file.length() +
                    '}';
        }

        @Override
        public int hashCode() {
            return file.getPath().hashCode();
//            return file.hashCode();似乎是相等的
        }
    }
}
