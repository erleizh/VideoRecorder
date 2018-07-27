package com.erlei.videorecorder.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by lll on 2017/12/7.
 * 视频工具类
 */
public class MediaUtil {

    private static final String TAG = "MediaUtil";

    public static long getVideoDuration(String path) {
        if (TextUtils.isEmpty(path)) return 0;
        android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
        try {
            mmr.setDataSource(path);
            String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            try {
                return Long.parseLong(duration);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                mmr.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public static MediaInfo getMediaInfo(String path) {
        if (TextUtils.isEmpty(path)) return null;
        android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
        MediaInfo mediaInfo = new MediaInfo();
        try {
            mmr.setDataSource(path);
            mediaInfo.setPath(path);
            mediaInfo.setDuration(mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION));
            mediaInfo.setWriter(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER));
            mediaInfo.setBitrate(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
            mediaInfo.setRotation(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
            mediaInfo.setDate(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE));
            mediaInfo.setTitle(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
            mediaInfo.setHasVideo(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO));
            mediaInfo.setMimeType(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE));
            mediaInfo.setLocation(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION));
//            //这种方式获取的信息不准确
//            mediaInfo.setHeight(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
//            mediaInfo.setWidth(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            Bitmap bitmap = mmr.getFrameAtTime();
            mediaInfo.setWidth(String.valueOf(bitmap.getWidth()));
            mediaInfo.setHeight(String.valueOf(bitmap.getHeight()));
            bitmap.recycle();
        } catch (Exception e) {
            Log.e(TAG, "MediaMetadataRetriever exception " + e);
        } finally {
            try {
                mmr.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
                Log.e(TAG, "MediaMetadataRetriever release exception " + e);
            }
        }
        return mediaInfo;
    }

    public static int[] getImageSize(String path) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeFile(path, options); // 此时返回的bitmap为null
            return new int[]{options.outWidth, options.outHeight};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new int[2];
    }

    public static Bitmap getVideoThumbnail(String filePath) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    public static class MediaInfo {
        private String mDuration;
        private String mWriter;
        private String mBitrate;
        private String mHeight;
        private String mWidth;
        private String mRotation;
        private String mDate;
        private String mTitle;
        private String mHasVideo;
        private String mMimeType;
        private String mLocation;
        private String mPath;

        public void setDuration(String duration) {
            mDuration = duration;
        }

        public int getDuration() {
            try {
                return Integer.parseInt(mDuration);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        public void setWriter(String writer) {
            mWriter = writer;
        }

        public String getWriter() {
            return mWriter;
        }

        public void setBitrate(String bitrate) {
            mBitrate = bitrate;
        }

        public int getBitrate() {
            try {
                return Integer.parseInt(mBitrate);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        public void setHeight(String height) {
            mHeight = height;
        }

        public int getHeight() {
            try {
                return Integer.parseInt(mHeight);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        public void setWidth(String width) {
            mWidth = width;
        }

        /**
         * @return 长的那一边永远是 宽 , 明明是
         */
        public float getWidth() {
            try {
                return Float.parseFloat(mWidth);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        public void setRotation(String rotation) {
            mRotation = rotation;
        }

        public float getRotation() {
            try {
                return Float.parseFloat(mRotation);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        public void setDate(String date) {
            mDate = date;
        }

        public String getDate() {
            return mDate;
        }

        public void setTitle(String title) {
            mTitle = title;
        }

        public String getTitle() {
            return mTitle;
        }

        public void setHasVideo(String hasVideo) {
            mHasVideo = hasVideo;
        }

        public String getHasVideo() {
            return mHasVideo;
        }

        public void setMimeType(String mimeType) {
            mMimeType = mimeType;
        }

        public String getMimeType() {
            return mMimeType;
        }

        public void setLocation(String location) {
            mLocation = location;
        }

        public String getLocation() {
            return mLocation;
        }

        public float getRatio() {
            try {
                return getWidth() / getHeight();
            } catch (Exception e) {
                return 0;
            }
        }

        public void setPath(String path) {
            mPath = path;
        }

        public String getPath() {
            return mPath;
        }

        @Override
        public String toString() {
            return "MediaInfo{" +
                    "mDuration='" + mDuration + '\'' +
                    ", mWriter='" + mWriter + '\'' +
                    ", mPath='" + mPath + '\'' +
                    ", mBitrate='" + mBitrate + '\'' +
                    ", mHeight='" + mHeight + '\'' +
                    ", mWidth='" + mWidth + '\'' +
                    ", mRotation='" + mRotation + '\'' +
                    ", mDate='" + mDate + '\'' +
                    ", mTitle='" + mTitle + '\'' +
                    ", mHasVideo='" + mHasVideo + '\'' +
                    ", mMimeType='" + mMimeType + '\'' +
                    ", mLocation='" + mLocation + '\'' +
                    ", mRatio='" + Float.toString(getRatio()) + '\'' +
                    '}';
        }

    }
}
