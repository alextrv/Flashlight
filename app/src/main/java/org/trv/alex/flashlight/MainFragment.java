package org.trv.alex.flashlight;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

public class MainFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int TURN_ON = 1;
    private static final int TURN_OFF = 2;
    private static final int START_BLINKING = 3;

    private ImageButton mToggleButton;
    private Switch mSwitchBlinking;
    private Flashlight mFlashlight;

    private HandlerThread mFlashlightHanderThread;
    private Handler mFlashlightHandler;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFlashlight = new Flashlight(getActivity());

        // Works only for API 23+
        mFlashlight.registerStateChanged(new Flashlight.StateChanged() {
            @Override
            public void onStateChanged() {
                setToggleButtonState();
                setKeepScreenOn();
            }
        });

        mFlashlightHanderThread = new HandlerThread("Flashlight");
        mFlashlightHanderThread.start();

        mFlashlightHandler = new Handler(mFlashlightHanderThread.getLooper()) {
            @Override
            public void handleMessage(final Message msg) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setToggleButtonState(!mFlashlight.isTurnedOn());

                    }
                });
                final boolean ok;
                switch (msg.what) {
                    case MainFragment.TURN_ON:
                        ok = mFlashlight.turnOn();
                        break;
                    case MainFragment.TURN_OFF:
                        ok = mFlashlight.turnOff();
                        break;
                    case MainFragment.START_BLINKING:
                        int onMs = AppPreferences.getPrefEnabledDurationFlashlight(getActivity()) * 1000;
                        int offMs = AppPreferences.getPrefDisabledDurationFlashlight(getActivity()) * 1000;
                        mFlashlight.startBlinking(onMs, offMs);
                        ok = true;
                        break;
                    default:
                        ok = true;
                        super.handleMessage(msg);
                }
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() != null) {
                            setToggleButtonState();
                            setKeepScreenOn();
                            if (!ok) {
                                Toast.makeText(getActivity(), R.string.error_string, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }
        };

        if (!mFlashlight.isAvailable()) {
            new AlertDialog.Builder(getActivity())
                    .setCancelable(false)
                    .setTitle(R.string.warning_string)
                    .setMessage(R.string.not_support_flashlight_string)
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();
        }

        if (AppPreferences.getPrefTurnOnFlashlightOnStart(getActivity())) {
            mFlashlightHandler.sendEmptyMessage(TURN_ON);
        }

        PreferenceManager
                .getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        mToggleButton = view.findViewById(R.id.toggle_flashlight);

        mToggleButton.setEnabled(mFlashlight.isAvailable());

        boolean enableBlinking = AppPreferences.getPrefEnableFlashlightBlinking(getActivity());

        mSwitchBlinking = view.findViewById(R.id.switch_blinking);
        mSwitchBlinking.setChecked(enableBlinking);

        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFlashlight.isTurnedOn()) {
                    mFlashlightHandler.sendEmptyMessage(TURN_OFF);
                } else {
                    mFlashlightHandler.sendEmptyMessage(mSwitchBlinking.isChecked() ? START_BLINKING : TURN_ON);
                }
            }
        });

        mSwitchBlinking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AppPreferences.setPrefEnableFlashlightBlinking(getActivity(), isChecked);
                if (mFlashlight.isTurnedOn()) {
                    mFlashlightHandler.sendEmptyMessage(isChecked ? START_BLINKING : TURN_ON);
                }
            }
        });

        return view;

    }

    @Override
    public void onResume() {
        super.onResume();
        setToggleButtonState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (AppPreferences.getPrefTurnOffFlashlightOnMinimize(getActivity())) {
            mFlashlightHandler.sendEmptyMessage(TURN_OFF);
        }
        PreferenceManager
                .getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
        mFlashlight.unregisterStateChanged();
    }

    public void setToggleButtonState() {
        setToggleButtonState(mFlashlight.isTurnedOn());
    }

    public void setToggleButtonState(boolean enabled) {
        mToggleButton.setImageResource(enabled ? R.drawable.button_on_state : R.drawable.button_off_state);
    }

    private void setKeepScreenOn() {
        boolean preventFromSleeping = AppPreferences.getPrefPreventScreenFromSleeping(getActivity().getApplicationContext());
        TabLayout tabLayout = ((MainActivity) getActivity()).getTabLayout();
        if (preventFromSleeping) {
            if (mFlashlight.isTurnedOn()) {
                tabLayout.setKeepScreenOn(true);
            } else {
                tabLayout.setKeepScreenOn(false);
            }
        } else {
            tabLayout.setKeepScreenOn(false);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(AppPreferences.PREF_PREVENT_SCREEN_FROM_SLEEPING)) {
            setKeepScreenOn();
        }
    }
}
