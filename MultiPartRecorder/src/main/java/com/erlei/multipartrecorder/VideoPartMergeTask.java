package com.erlei.multipartrecorder;

import android.os.AsyncTask;
import android.os.SystemClock;

import com.coremedia.iso.boxes.Container;
import com.erlei.videorecorder.util.LogUtil;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class VideoPartMergeTask extends AsyncTask<MultiPartRecorder.Part, Float, File> {

    private MultiPartRecorder.VideoMergeListener mMergeListener;
    private File mOutPutFile;
    private boolean mDeletePartFile;
    private final long mTimeout;

    public VideoPartMergeTask(File outPutFile) {
        this(outPutFile, null, false, 2000);
    }

    /**
     * 视频合并任务
     *
     * @param outPutFile     最重输出文件
     * @param mergeListener  监听器
     * @param deletePartFile 合并完成后是否删除视频块 (发生错误时不会删除视频块)
     * @param timeout        由于从调用 stopRecord 到视频录制成功需要一段时间.这取决于编码速度 ,文件读写速度
     *                       如果用户在点击结束录制之后立即进行合并 , 那么可能由于最后一个视频块还没有编码完成 (handleVideoMuxerStopped) ,
     *                       有一定可能导致合并失败
     *                       设置超时时间会等待编码器完成视频块的编码工作 (最后一个视频块 , 通常不会太久)
     */
    public VideoPartMergeTask(File outPutFile, MultiPartRecorder.VideoMergeListener mergeListener, boolean deletePartFile, long timeout) {
        mOutPutFile = outPutFile;
        mMergeListener = mergeListener;
        mDeletePartFile = deletePartFile;
        mTimeout = timeout;
    }


    @Override
    protected void onProgressUpdate(Float... values) {
        super.onProgressUpdate(values);
        if (mMergeListener != null) {
            mMergeListener.onProgress(values[0]);
        }
    }

    @Override
    protected File doInBackground(MultiPartRecorder.Part... lists) {
        if (LogUtil.LOG_ENABLE) {
            for (MultiPartRecorder.Part list : lists) {
                LogUtil.logd("VideoPartMergeTask", list.toString());
            }
        }
        try {
            if (lists.length == 0) throw new IllegalArgumentException("传入的视频块列表是空的");

            if (mMergeListener != null) mMergeListener.onStart();
            //检查视频块是否全都录制结束了
            int timeout = 0;
            while (!checkVideoPartRecordFinish(lists) && timeout < mTimeout) {
                timeout += 50;
                SystemClock.sleep(50);
            }
            if (timeout > mTimeout) {
                throw new IllegalStateException("有视频块还没有结束录制 , 不能开始合并视频");
            }

            if (lists.length == 1) {//只有一个视频片段
                if (lists[0].file.renameTo(mOutPutFile)) {
                    if (mMergeListener != null) mMergeListener.onSuccess(mOutPutFile);
                } else {
                    if (mMergeListener != null)
                        mMergeListener.onError(new IllegalStateException("只有一个视频片段,在将这个视频片段移动到目标位置时出现了错误"));
                }

            } else {
                String[] videoUris = new String[lists.length];
                for (int i = 0; i < lists.length; i++) {
                    videoUris[i] = lists[i].file.getAbsolutePath();
                }

                List<Movie> inMovies = new ArrayList<>(videoUris.length);
                for (String videoUri : videoUris) {
                    inMovies.add(MovieCreator.build(videoUri));
                }

                List<Track> videoTracks = new LinkedList<Track>();
                List<Track> audioTracks = new LinkedList<Track>();

                for (Movie m : inMovies) {
                    for (Track t : m.getTracks()) {
                        if (t.getHandler().equals("soun")) {
                            audioTracks.add(t);
                        }
                        if (t.getHandler().equals("vide")) {
                            videoTracks.add(t);
                        }
                    }
                }

                Movie result = new Movie();
                if (!audioTracks.isEmpty()) {
                    result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
                }
                if (!videoTracks.isEmpty()) {
                    result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
                }
                Container out = new DefaultMp4Builder().build(result);

                startCheckProgressThread(out);

                FileChannel fc = new RandomAccessFile(mOutPutFile, "rw").getChannel();
                out.writeContainer(fc);
                fc.close();
                if (mMergeListener != null) mMergeListener.onSuccess(mOutPutFile);
            }
        } catch (Exception e) {
            mDeletePartFile = false;//发生错误时不删除视频
            if (mMergeListener != null) mMergeListener.onError(e);
        }
        deleteTempPartFile(lists);
        return mOutPutFile;
    }

    private void startCheckProgressThread(Container out) {
        long finalVideoFileSize = 0;
        for (int i = 0; i < out.getBoxes().size(); i++) {
            finalVideoFileSize += out.getBoxes().get(i).getSize();
        }
        final long finalVideoFileSize1 = finalVideoFileSize;
        new Thread(new Runnable() {
            @Override
            public void run() {
                long currentFileSize = 0;
                if (mOutPutFile.exists())
                    currentFileSize = mOutPutFile.length();
                while (finalVideoFileSize1 != currentFileSize && !isCancelled() && getStatus() == Status.RUNNING) {
                    if (mOutPutFile.exists()) {
                        currentFileSize = mOutPutFile.length();
                        publishProgress((float) currentFileSize / (float) finalVideoFileSize1);
                    }
                    SystemClock.sleep(10);
                }
            }
        }).start();
    }

    private void deleteTempPartFile(MultiPartRecorder.Part[] lists) {
        if (mDeletePartFile) {
            for (MultiPartRecorder.Part list : lists) {
                //noinspection ResultOfMethodCallIgnored
                list.file.delete();
            }
        }
    }

    private boolean checkVideoPartRecordFinish(MultiPartRecorder.Part[] lists) {
        for (MultiPartRecorder.Part list : lists) {
            if (list.isRecording()) return false;
        }
        return true;
    }

}