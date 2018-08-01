package com.erlei.videorecorder.fragment;


import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import com.erlei.videorecorder.R;
import com.erlei.videorecorder.camera.FpsRange;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.recorder.CameraController;

import java.util.List;

public class SettingsDialogFragment extends DialogFragment {

    private CameraController mCameraController;
    private ArrayAdapter<Size> mResolutionAdapter;
    private CameraControllerView mCameraControllerView;
    private AppCompatSpinner mSpResolution;
    private AppCompatSpinner mSpFlash;
    private AppCompatSpinner mSpAntibanding;
    private AppCompatSpinner mSpColorEffects;
    private AppCompatSpinner mSpFocus;
    private AppCompatSpinner mSpWhiteBalance;
    private AppCompatSpinner mSpSceneMode;
    private AppCompatSpinner mSpFPS;
    private AppCompatSpinner mSpISOMode;
    private AppCompatSeekBar mSbZoom;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(getParentFragment() instanceof CameraControllerView)) {
            throw new IllegalStateException("Attached getParentFragment are not implemented SettingsDialogFragment.CameraControllerView");
        }
        mCameraControllerView = ((CameraControllerView) getParentFragment());
        mCameraController = mCameraControllerView.getCameraController();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Window window = getDialog().getWindow();
        if (window == null) return;
        window.requestFeature(Window.FEATURE_NO_TITLE);
        super.onActivityCreated(savedInstanceState);
//        window.setBackgroundDrawable(new ColorDrawable(0x00000000));
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_multi_part_recorder, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findViews(view);

        mResolutionAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, mCameraController.getSupportedPreviewSizes());
        mSpResolution.setAdapter(mResolutionAdapter);
        mSpResolution.setSelection(mResolutionAdapter.getPosition(mCameraController.getCameraSize()), false);
        mSpResolution.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCameraControllerView.setPreviewSize(mResolutionAdapter.getItem(position));
            }
        });

        Camera.Parameters cameraParameters = mCameraController.getCameraParameters();


        initSpinner(mSpFlash, cameraParameters.getSupportedFlashModes(), "flash-mode", new Callback() {
            @Override
            public void set(String key) {
                mCameraController.setFlashMode(key);
            }
        });
        initSpinner(mSpAntibanding, cameraParameters.getSupportedAntibanding(), "antibanding", new Callback() {
            @Override
            public void set(String key) {
                mCameraController.setAntibanding(key);
            }
        });
        initSpinner(mSpColorEffects, cameraParameters.getSupportedColorEffects(), "effect", new Callback() {
            @Override
            public void set(String key) {
                mCameraController.setColorEffects(key);
            }
        });

        initSpinner(mSpFocus, cameraParameters.getSupportedFocusModes(), "focus-mode", new Callback() {
            @Override
            public void set(String key) {
                mCameraController.setFocusMode(key);
            }
        });
        initSpinner(mSpISOMode,mCameraController.getSupportedModes("iso-values") , "iso", new Callback() {
            @Override
            public void set(String key) {
                mCameraController.setFocusMode(key);
            }
        });


        initSpinner(mSpSceneMode, cameraParameters.getSupportedSceneModes(), "scene-mode", new Callback() {
            @Override
            public void set(String key) {
                mCameraController.setSceneMode(key);
            }
        });

        initSpinner(mSpWhiteBalance, cameraParameters.getSupportedWhiteBalance(), "whitebalance", new Callback() {
            @Override
            public void set(String key) {
                mCameraController.setWhiteBalance(key);
            }
        });

        initFPSRangeSpinner(cameraParameters);


        initZoomSeekBar();
    }

    private void initZoomSeekBar() {
        Camera.Parameters cameraParameters = mCameraController.getCameraParameters();
        int maxZoom = cameraParameters.getMaxZoom();
        mSbZoom.setMax(maxZoom);
        mSbZoom.setProgress(cameraParameters.getZoom());
        mSbZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mCameraController.startSmoothZoom(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }

    private void initFPSRangeSpinner(Camera.Parameters cameraParameters) {
        final ArrayAdapter<FpsRange> fpsRangeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, mCameraController.getSupportedPreviewFpsRange());
        mSpFPS.setAdapter(fpsRangeAdapter);
        mSpFPS.setSelection(fpsRangeAdapter.getPosition(new FpsRange(cameraParameters.get("preview-fps-range"))), false);
        mSpFPS.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                FpsRange item = fpsRangeAdapter.getItem(position);
                mCameraController.setPreviewFpsRange(item);
            }
        });

    }

    private void initSpinner(final AppCompatSpinner sp, List<String> list, String key, final Callback callback) {
        if (list == null) return;
        Camera.Parameters cameraParameters = mCameraController.getCameraParameters();
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, list);
        sp.setAdapter(adapter);
        sp.setSelection(adapter.getPosition(cameraParameters.get(key)), false);
        sp.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                callback.set(adapter.getItem(position));
            }
        });
    }

    private interface Callback {
        void set(String key);
    }

    private void findViews(View view) {
        mSpResolution = view.findViewById(R.id.spResolution);
        mSpFlash = view.findViewById(R.id.spFlash);
        mSpAntibanding = view.findViewById(R.id.spAntibanding);
        mSpColorEffects = view.findViewById(R.id.spColorEffects);
        mSpFocus = view.findViewById(R.id.spFocus);
        mSpWhiteBalance = view.findViewById(R.id.spWhiteBalance);
        mSpSceneMode = view.findViewById(R.id.spSceneMode);
        mSpISOMode = view.findViewById(R.id.spISOMode);
        mSbZoom = view.findViewById(R.id.sbZoom);
        mSpFPS = view.findViewById(R.id.spFPS);
    }

    public static SettingsDialogFragment newInstance() {
        return new SettingsDialogFragment();
    }


    public interface CameraControllerView {

        CameraController getCameraController();

        void setPreviewSize(Size item);
    }


    public class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }
}
