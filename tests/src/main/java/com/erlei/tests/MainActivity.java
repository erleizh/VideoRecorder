package com.erlei.tests;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private String[] mPermissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        if (!checkSelfPermissions(mPermissions)) {
            ActivityCompat.requestPermissions(MainActivity.this, mPermissions, REQUEST_CAMERA);
        } else {
            setContent();
        }

    }

    public boolean checkSelfPermissions(String... permission) {
        for (String s : permission) {
            if (ActivityCompat.checkSelfPermission(this, s) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (REQUEST_CAMERA == requestCode && grantResults.length == mPermissions.length) {
            setContent();
        }
    }

    private void setContent() {
        Fragment fragment = GdxTestFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commitAllowingStateLoss();
    }

}
