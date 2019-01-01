package org.trv.alex.flashlight;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScreenFragment extends Fragment implements MainActivity.OnBackPressedListener {

    private static final float MAX_BRIGHTNESS = 1F;

    private static final float DEFAULT_BRIGHTNESS = -1F;

    public static final String TAG = "ScreenFragment";

    private ImageButton mToggleButton;
    private LinearLayout mEmptyLayout;
    private FrameLayout mContentLayout;
    private Switch mSwitchBlinking;

    private boolean mFullScreenEnabled;
    private volatile boolean isScreenBlinking;

    private static ExecutorService mBlinkingExecutor = Executors.newSingleThreadExecutor();

    private View mDecorView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setOnBackPressedListener(this);
        }

        mDecorView = getActivity().getWindow().getDecorView();

        mDecorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    setFullScreenState(false);
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        mContentLayout = view.findViewById(R.id.content_layout);

        mToggleButton = view.findViewById(R.id.toggle_flashlight);
        mToggleButton.setImageResource(R.drawable.button_screen_off_state);

        mEmptyLayout = view.findViewById(R.id.empty_layout);

        mEmptyLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFullScreenState(false);
            }
        });

        boolean enableBlinking = AppPreferences.getPrefEnableScreenBlinking(getActivity());

        mSwitchBlinking = view.findViewById(R.id.switch_blinking);
        mSwitchBlinking.setChecked(enableBlinking);

        mSwitchBlinking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AppPreferences.setPrefEnableScreenBlinking(getActivity(), isChecked);
            }
        });

        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFullScreenState(true);
                boolean enableBlinking = AppPreferences.getPrefEnableScreenBlinking(getActivity());
                if (enableBlinking) {
                    int onMs = AppPreferences.getPrefEnabledDurationScreen(getActivity());
                    int offMs = AppPreferences.getPrefDisabledDurationScreen(getActivity());
                    int colorFirst = AppPreferences.getPrefFirstColor(getActivity());
                    int colorSecond = AppPreferences.getPrefSecondColor(getActivity());
                    blinkScreen(onMs, offMs, colorFirst, colorSecond);
                } else {
                    mEmptyLayout.setBackgroundColor(Color.WHITE);
                }
                setBrightness(MAX_BRIGHTNESS);
            }
        });

        return view;
    }

    public void setFullScreenState(boolean fullScreenEnable) {
        stopBlinking();
        mFullScreenEnabled = fullScreenEnable;
        int uiOptions;
        if (mFullScreenEnabled) {
            uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        } else {
            uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
        }
        MainActivity activity = (MainActivity) getActivity();
        mDecorView.setSystemUiVisibility(uiOptions);
        if (uiOptions == View.SYSTEM_UI_FLAG_VISIBLE) {
            mContentLayout.setVisibility(View.VISIBLE);
            mEmptyLayout.setVisibility(View.GONE);
            activity.getBarLayout().setVisibility(View.VISIBLE);
            setBrightness(DEFAULT_BRIGHTNESS);
        } else {
            mEmptyLayout.setVisibility(View.VISIBLE);
            mContentLayout.setVisibility(View.GONE);
            activity.getBarLayout().setVisibility(View.GONE);
            setBrightness(MAX_BRIGHTNESS);
        }
    }

    public void setBrightness(float brightness) {
        WindowManager.LayoutParams layoutParams = getActivity().getWindow().getAttributes();
        layoutParams.screenBrightness = brightness;
        getActivity().getWindow().setAttributes(layoutParams);
    }

    public void blinkScreen(int onMs, int offMs, final int colorOn, final int colorOff) {

        isScreenBlinking = true;

        final int minSleep = 10;

        final int sleepOnMs = Math.max(minSleep, onMs);
        final int sleepOffMs = Math.max(minSleep, offMs);

        if (mBlinkingExecutor.isTerminated()) {
            mBlinkingExecutor = Executors.newSingleThreadExecutor();
        }

        mBlinkingExecutor.submit(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; isScreenBlinking && !Thread.currentThread().isInterrupted(); ++i) {
                    if (i % 2 == 0) {
                        mEmptyLayout.post(new Runnable() {
                            @Override
                            public void run() {
                                mEmptyLayout.setBackgroundColor(colorOn);
                            }
                        });
                        try {
                            Thread.sleep(sleepOnMs);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                    } else {
                        mEmptyLayout.post(new Runnable() {
                            @Override
                            public void run() {
                                mEmptyLayout.setBackgroundColor(colorOff);
                            }
                        });
                        try {
                            Thread.sleep(sleepOffMs);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                }
            }
        });
    }

    public void stopBlinking() {
        isScreenBlinking = false;
    }

    private void exitFullScreenState() {
        if (mFullScreenEnabled) {
            setFullScreenState(false);
            setBrightness(DEFAULT_BRIGHTNESS);
        }
    }

    @Override
    public void onDestroyView() {
        exitFullScreenState();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mDecorView.setOnSystemUiVisibilityChangeListener(null);
        if (!mBlinkingExecutor.isShutdown()) {
            mBlinkingExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    @Override
    public void onPressedBack() {
        exitFullScreenState();
    }

    @Override
    public boolean condition() {
        return mFullScreenEnabled;
    }
}
