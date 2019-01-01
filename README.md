# VideoRecorder
#### 警告
此项目是作为个人学习用，包含许多未解决的bug

#### Feature

- [x] 高性能任意尺寸视频录制 (以相机支持的最大尺寸预览)

- [X] 断点录制 (断点回删)

- [X] 离屏录制

- [X] 录制时的Canvas API支持

- [ ] 实时滤镜

- [ ] 后期滤镜

- [ ] 人脸贴纸

- [ ] 抖音特效

#### API
```java
    private void initRecorder() {
        ICameraPreview cameraPreview = new DefaultCameraPreview(mTextureView);
//        ICameraPreview cameraPreview = new OffscreenCameraPreview(getContext(), 1920, 1920); //离屏录制

        Camera.CameraBuilder cameraBuilder = new Camera.CameraBuilder(getActivity())
                .useDefaultConfig()
                .setFacing(android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT)
                .setPreviewSize(new Size(2048, 1536))
                .setRecordingHint(true)
                .setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        //视频效果管理器
        mEffectsManager = new EffectsManager();
        mEffectsManager.addEffect(new CanvasOverlayEffect() {
            private FPSCounterFactory.FPSCounter1 mCounter;
            Paint mPaint;

            @Override
            public void prepare(Size size) {
                super.prepare(size);
                mPaint = new Paint();
                mPaint.setColor(Color.YELLOW);
                mPaint.setAlpha(230);
                mPaint.setTextSize(40);
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setAntiAlias(true);

                mCounter = new FPSCounterFactory.FPSCounter1();
            }

            @Override
            protected void drawCanvas(Canvas canvas) {
                canvas.drawText(String.format(Locale.getDefault(), "%.2f", mCounter.getFPS()), canvas.getWidth() / 2, canvas.getHeight() / 2, mPaint);
            }
        });

        VideoRecorder.Builder builder = new VideoRecorder.Builder(cameraPreview)
                .setCallbackHandler(new CallbackHandler())
                .setLogFPSEnable(false)
                .setCameraBuilder(cameraBuilder)
                .setDrawTextureListener(mEffectsManager)
                .setOutPutPath(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), File.separator + "VideoRecorder").getAbsolutePath())
                .setFrameRate(30)
                .setChannelCount(1);

        //分段录制,回删支持
        MultiPartRecorder.Builder multiBuilder = new MultiPartRecorder.Builder(builder);
        mRecorder = multiBuilder
                .addPartListener(new MultiPartRecorder.VideoPartListener() {
                    @Override
                    public void onRecordVideoPartStarted(MultiPartRecorder.Part part) {
                        LogUtil.logd("onRecordVideoPartStarted \t" + part.toString());
                    }

                    @Override
                    public void onRecordVideoPartSuccess(MultiPartRecorder.Part part) {
                        LogUtil.logd("onRecordVideoPartSuccess \t" + part.toString());
                    }

                    @Override
                    public void onRecordVideoPartFailure(MultiPartRecorder.Part part) {
                        LogUtil.loge("onRecordVideoPartFailure \t" + part.file);
                        mRecorderIndicator.removePart(part.file.getAbsolutePath());
                        mRecorder.removePart(part.file.getAbsolutePath());
                    }
                })
                .setMergeListener(new MultiPartRecorder.VideoMergeListener() {
                    @Override
                    public void onStart() {
                        LogUtil.logd("merge onStart");
                    }

                    @Override
                    public void onSuccess(File outFile) {
                        LogUtil.logd("merge Success \t" + outFile);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        if (Build.VERSION.SDK_INT >= 24) {
                            intent.setDataAndType(FileProvider.getUriForFile(getContext().getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", outFile), "video/*");
                            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } else {
                            intent.setDataAndType(Uri.fromFile(outFile), "video/*");
                        }
                        startActivity(intent);
                    }

                    @Override
                    public void onError(Exception e) {
                        LogUtil.logd("merge Error \t" + e.toString());
                    }

                    /**
                     * 合并进度
                     *
                     * @param value 0 - 1
                     */
                    @Override
                    public void onProgress(float value) {
                        LogUtil.logd("merge onProgress \t" + value);
                    }

                })
                .setFileFilter(new MultiPartRecorder.FileFilter() {
                    @Override
                    public boolean filter(MultiPartRecorder.Part part) {
                        return part.duration > 1500;
                    }
                })
                .build();

        mCameraController = mRecorder.getCameraController();
    }


```
#### ScreenShort
   <div align="center">
   <img src="/screenshort/479649274876459714.jpg" width="45%"><img src="/screenshort/440800708950629658.png" width="45%">
   </div>


#### Download apk

  [VideoRecorder](https://fir.im/egmc)

#### 学习过程中记录下来的一些有价值的资料

  [相关资料.md](./app/doc/相关资料.md)
