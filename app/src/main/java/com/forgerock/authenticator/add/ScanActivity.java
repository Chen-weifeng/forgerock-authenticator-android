/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 *
 * Portions Copyright 2013 Nathaniel McCallum, Red Hat
 */

package com.forgerock.authenticator.add;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.forgerock.authenticator.R;
import com.forgerock.authenticator.baseactivities.BaseActivity;
import com.forgerock.authenticator.mechanisms.CoreMechanismFactory;
import com.forgerock.authenticator.mechanisms.DuplicateMechanismException;
import com.forgerock.authenticator.mechanisms.base.Mechanism;
import com.forgerock.authenticator.mechanisms.MechanismCreationException;
import com.forgerock.authenticator.mechanisms.URIMappingException;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Activity used for scanning QR codes. Provides feedback to the user when a QR code is scanned,
 * and if successful, creates the Mechanism that the QR code represents.
 */
public class ScanActivity extends BaseActivity implements SurfaceHolder.Callback {
    private static final CameraInfo    mCameraInfo  = new CameraInfo();
    private final ScanAsyncTask mScanAsyncTask;
    private final int           mCameraId;
    private Handler             mHandler;
    private Camera              mCamera;
    private Logger logger;

    /**
     * Creates a new ScanActivity. Never called directly, as instantiation is handled by an Intent.
     */
    public ScanActivity() {
        super();
        logger = LoggerFactory.getLogger(ScanActivity.class);

        mCameraId = findCamera();
        assert mCameraId >= 0;

        final Context context = this;

        final CreateMechanismFromUriTask.MechanismPostRunnable postScanRunnable = new CreateMechanismFromUriTask.MechanismPostRunnable() {
            @Override
            public void run(Mechanism mechanism) {
                if (mechanism == null || mechanism.getOwner().getImageURL() == null) {
                    finish();
                    return;
                }
                final ImageView image = (ImageView) findViewById(R.id.image);
                Picasso.with(context)
                        .load(mechanism.getOwner().getImageURL())
                        .placeholder(R.drawable.scan)
                        .into(image, new Callback() {
                            @Override
                            public void onSuccess() {
                                findViewById(R.id.progress).setVisibility(View.INVISIBLE);
                                image.setAlpha(0.9f);
                                image.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        finish();
                                    }
                                }, 2000);
                            }

                            @Override
                            public void onError() {
                                finish();
                            }
                        });
            }
        };
        final Activity thisActivity = this;

        // Create the decoder thread
        mScanAsyncTask = new ScanAsyncTask() {
            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                new CreateMechanismFromUriTask(thisActivity, postScanRunnable).execute(result);

            }
        };
    }

    /**
     * Determines if a suitable camera has been detected.
     * @return True if there is a camera available.
     */
    public static boolean haveCamera() {
        return findCamera() >= 0;
    }

    private static int findCamera() {
        int cameraId = Camera.getNumberOfCameras() - 1;

        // Find a back-facing camera, otherwise use the first camera.
        for (; cameraId > 0; cameraId--) {
            Camera.getCameraInfo(cameraId, mCameraInfo);
            if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_BACK)
                break;
        }

        return cameraId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan);
        mScanAsyncTask.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScanAsyncTask.cancel(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ((SurfaceView) findViewById(R.id.surfaceview)).getHolder().addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera == null)
            return;

        // The code in this section comes from the developer docs. See:
        // http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)

        int rotation = 0;
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
        case Surface.ROTATION_0:
            rotation = 0;
            break;
        case Surface.ROTATION_90:
            rotation = 90;
            break;
        case Surface.ROTATION_180:
            rotation = 180;
            break;
        case Surface.ROTATION_270:
            rotation = 270;
            break;
        }

        int result = 0;
        switch (mCameraInfo.facing) {
        case Camera.CameraInfo.CAMERA_FACING_FRONT:
            result = (mCameraInfo.orientation + rotation) % 360;
            result = (360 - result) % 360; // compensate the mirror
            break;

        case Camera.CameraInfo.CAMERA_FACING_BACK:
            result = (mCameraInfo.orientation - rotation + 360) % 360;
            break;
        }

        mCamera.setDisplayOrientation(result);
        mCamera.startPreview();

        if (mHandler != null)
            mHandler.sendEmptyMessageDelayed(0, 100);
    }

    @Override
    @TargetApi(14)
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceDestroyed(holder);

        try {
            // Open the camera
            mCamera = Camera.open(mCameraId);
            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(mScanAsyncTask);
        } catch (Exception e) {
            e.printStackTrace();
            surfaceDestroyed(holder);

            // Show error message
            findViewById(R.id.surfaceview).setVisibility(View.INVISIBLE);
            findViewById(R.id.progress).setVisibility(View.INVISIBLE);
            findViewById(R.id.window).setVisibility(View.INVISIBLE);
            findViewById(R.id.textview).setVisibility(View.VISIBLE);
            return;
        }

        // Set auto-focus mode
        Parameters params = mCamera.getParameters();
        List<String> modes = params.getSupportedFocusModes();
        if (modes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        else if (modes.contains(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
            params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        else if (modes.contains(Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Parameters.FOCUS_MODE_AUTO);
            mHandler = new AutoFocusHandler(mCamera);
        }
        mCamera.setParameters(params);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera == null)
            return;

        if (mHandler != null) {
            mCamera.cancelAutoFocus();
            mHandler.removeMessages(0);
            mHandler = null;
        }

        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
    }

    private static class AutoFocusHandler extends Handler implements Camera.AutoFocusCallback {
        private final Camera mCamera;

        /**
         * Creates a handler for autofocussing a given camera.
         * @param camera The camera to autofocus.
         */
        public AutoFocusHandler(Camera camera) {
            mCamera = camera;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mCamera.autoFocus(this);
        }

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            sendEmptyMessageDelayed(0, 1000);
        }
    }

}
