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

import java.io.IOException;

public class Flashlight {

    private boolean mTurnedOn = false;
    private boolean mBlinkingStarted;
    private Context mContext;
    private Thread mBlinkingThread;

    // For SDK before 23
    private Camera mCamera;

    // For SDK 23+
    private CameraManager mCameraManager;
    private StateChanged mStateChanged;
    private CameraManager.TorchCallback mTorchCallback;

    public interface StateChanged {
        void onStateChanged();
    }

    public Flashlight(Context context) {
        mContext = context;
        if (BuildConfig.FLAVOR.equals("minApi23")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
                mTorchCallback = new CameraManager.TorchCallback() {
                    @Override
                    public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
                        super.onTorchModeChanged(cameraId, enabled);
                        if (mBlinkingStarted) {
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
        if (mBlinkingStarted) {
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
        mBlinkingStarted = false;
        if (mBlinkingThread != null) {
            try {
                mBlinkingThread.interrupt();
                mBlinkingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mBlinkingThread = null;
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

    public void startBlinking(final long lightOnMs, final long lightOffMs) {
        if (!isAvailable()) {
            return;
        }
        if (mTurnedOn) {
            turnOff();
        }
        mBlinkingStarted = true;
        mTurnedOn = true;
        if (BuildConfig.API23) {
            mBlinkingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String cameraId = getCameraIdWithFlashlight();
                        if (cameraId == null) {
                            return;
                        }
                        for (int i = 0; mBlinkingStarted; ++i) {
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
                }
            });
        } else {
            mBlinkingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mCamera = Camera.open();
                    final Camera.Parameters params = mCamera.getParameters();
                    SurfaceTexture surfaceTexture = new SurfaceTexture(0);
                    try {
                        mCamera.setPreviewTexture(surfaceTexture);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mCamera.startPreview();
                    for (int i = 0; mBlinkingStarted; ++i) {
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
            });
        }
        mBlinkingThread.start();
    }

    public boolean isBlinkingStarted() {
        return mBlinkingStarted;
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
                        if (mCameraManager.getCameraCharacteristics(cameraId)
                                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
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
