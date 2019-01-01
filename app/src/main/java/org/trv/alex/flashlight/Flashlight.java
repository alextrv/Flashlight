package org.trv.alex.flashlight;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.support.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Flashlight implements Closeable {

    private static Flashlight sFlashlight;

    private boolean mTurnedOn = false;
    private volatile boolean mBlinking;
    private Context mContext;
    private static ExecutorService mBlinkingExecutor = Executors.newSingleThreadExecutor();
    private static Future<?> mBlinkingResult;

    // For SDK before 23
    private Camera mCamera;

    // For SDK 23+
    private CameraManager mCameraManager;
    private StateChanged mStateChanged;
    private CameraManager.TorchCallback mTorchCallback;

    @Override
    public void close() {
        if (isTurnedOn()) {
            turnOff();
        }
        if (!mBlinkingExecutor.isShutdown()) {
            mBlinkingExecutor.shutdownNow();
        }
    }

    public interface StateChanged {
        void onStateChanged();
    }

    public static Flashlight getInstance(Context context) {
        if (sFlashlight == null) {
            sFlashlight = new Flashlight(context.getApplicationContext());
        } else {
            sFlashlight.mContext = context.getApplicationContext();
        }
        return sFlashlight;
    }

    private Flashlight(Context context) {
        mContext = context;
        if (BuildConfig.FLAVOR.equals("minApi23")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
                mTorchCallback = new CameraManager.TorchCallback() {
                    @Override
                    public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
                        super.onTorchModeChanged(cameraId, enabled);
                        if (mBlinking) {
                            mTurnedOn = true;
                        } else {
                            mTurnedOn = enabled;
                            mStateChanged.onStateChanged();
                        }
                    }
                };
            }
        }
    }

    public boolean isAvailable() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public boolean turnOn() {
        if (!isAvailable()) {
            return false;
        }
        if (isBlinking()) {
            turnOff();
        }
        if (BuildConfig.API23) {
            try {
                String cameraId = getCameraIdWithFlashlight();
                if (cameraId != null) {
                    mCameraManager.setTorchMode(cameraId, true);
                    mTurnedOn = true;
                }
            } catch (CameraAccessException e) {
                return false;
            }
        } else {
            try {
                mCamera = Camera.open();
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(parameters);
                SurfaceTexture surfaceTexture = new SurfaceTexture(0);
                try {
                    mCamera.setPreviewTexture(surfaceTexture);
                } catch (IOException e) {
                    return false;
                }
                mCamera.startPreview();
                mTurnedOn = true;
            } catch (RuntimeException re) {
                return false;
            }
        }
        return true;
    }

    public boolean turnOff() {
        if (!isAvailable()) {
            return false;
        }
        mBlinking = false;
        try {
            if (mBlinkingResult != null) {
                mBlinkingResult.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (BuildConfig.API23) {
            try {
                String cameraId = getCameraIdWithFlashlight();
                if (cameraId != null) {
                    mCameraManager.setTorchMode(cameraId, false);
                }
            } catch (CameraAccessException e) {
                return false;
            } finally {
                mTurnedOn = false;
            }
        } else {
            if (mCamera != null) {
                try {
                    Camera.Parameters parameters = mCamera.getParameters();
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(parameters);
                    mCamera.stopPreview();
                    mCamera.release();
                } catch (RuntimeException re) {
                    // ignore this exception
                } finally {
                    mTurnedOn = false;
                    mCamera = null;
                }
            }
        }
        return true;
    }

    public void blink(final long lightOnMs, final long lightOffMs) {
        if (!isAvailable()) {
            return;
        }
        if (mTurnedOn) {
            turnOff();
        }
        mTurnedOn = true;
        if (mBlinkingExecutor.isTerminated()) {
            mBlinkingExecutor = Executors.newSingleThreadExecutor();
        }
        mBlinkingResult = mBlinkingExecutor.submit(new Blinking(lightOnMs, lightOffMs));
    }

    private class Blinking implements Runnable {

        private static final long MIN_DURATION = 100;

        private long lightOnMs;
        private long lightOffMs;

        public Blinking(long lightOnMs, long lightOffMs) {
            this.lightOnMs = Math.max(MIN_DURATION, lightOnMs);
            this.lightOffMs = Math.max(MIN_DURATION, lightOffMs);
        }

        @Override
        public void run() {
            mBlinking = true;
            if (BuildConfig.API23) {
                try {
                    final String cameraId = getCameraIdWithFlashlight();
                    if (cameraId == null) {
                        return;
                    }
                    for (int i = 0; mBlinking && !Thread.currentThread().isInterrupted(); ++i) {
                        if (i % 2 == 0) {
                            mCameraManager.setTorchMode(cameraId, true);
                            try {
                                Thread.sleep(lightOnMs);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                return;
                            }
                        } else {
                            mCameraManager.setTorchMode(cameraId, false);
                            try {
                                Thread.sleep(lightOffMs);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                return;
                            }
                        }
                    }
                } catch (CameraAccessException e) {
                    // ignore this exception
                }
            } else {
                mCamera = Camera.open();
                final Camera.Parameters params = mCamera.getParameters();
                SurfaceTexture surfaceTexture = new SurfaceTexture(0);
                try {
                    mCamera.setPreviewTexture(surfaceTexture);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.startPreview();
                for (int i = 0; mBlinking && !Thread.currentThread().isInterrupted(); ++i) {
                    if (i % 2 == 0) {
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        mCamera.setParameters(params);
                        try {
                            Thread.sleep(lightOnMs);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                    } else {
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        mCamera.setParameters(params);
                        try {
                            Thread.sleep(lightOffMs);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                }
            }
            mBlinking = false;
        }


    }
    

    public boolean isBlinking() {
        return mBlinkingResult != null && !mBlinkingResult.isDone();
    }

    public boolean isTurnedOn() {
        return mTurnedOn;
    }

    public void registerStateChanged(@NonNull StateChanged stateChanged) {
        if (BuildConfig.API23) {
            mStateChanged = stateChanged;
            mCameraManager.registerTorchCallback(mTorchCallback, null);
        }
    }

    public void unregisterStateChanged() {
        if (BuildConfig.API23) {
            mCameraManager.unregisterTorchCallback(mTorchCallback);
        }
    }

    private String getCameraIdWithFlashlight() {
        if (BuildConfig.API23) {
            try {
                String[] cameraIdArray = mCameraManager.getCameraIdList();
                if (cameraIdArray.length > 0) {
                    for (String cameraId : cameraIdArray) {
                        Boolean flashAvailable = mCameraManager.getCameraCharacteristics(cameraId)
                                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                        if (flashAvailable != null && flashAvailable) {
                            return cameraId;
                        }
                    }
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
