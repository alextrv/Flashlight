package org.trv.alex.flashlight;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

public class MainFragment extends Fragment implements MainActivity.FlashlightCallbacks {

    public static final String TAG = "MainFragment";

    private ImageButton mToggleButton;
    private Switch mSwitchBlinking;
    private Flashlight mFlashlight;
    private Handler mFlashlightHandler;

    private MainActivity mActivity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFlashlight = Flashlight.getInstance(getActivity().getApplicationContext());

        mActivity = (MainActivity) getActivity();

        mActivity.setFlashlightCallbacks(this);

        mFlashlightHandler = mActivity.getFlashlightHandler();

        // Works only for API 23+
        mFlashlight.registerStateChanged(new Flashlight.StateChanged() {
            @Override
            public void onStateChanged() {
                if (mActivity != null && !mActivity.isDestroyed()) {
                    setToggleButtonState();
                    mActivity.setKeepScreenOn();
                }
            }
        });
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
                    mFlashlightHandler.sendEmptyMessage(MainActivity.TURN_OFF);
                } else {
                    mFlashlightHandler.sendEmptyMessage(
                            mSwitchBlinking.isChecked() ? MainActivity.START_BLINKING : MainActivity.TURN_ON);
                }
            }
        });

        mSwitchBlinking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AppPreferences.setPrefEnableFlashlightBlinking(getActivity(), isChecked);
                if (mFlashlight.isTurnedOn()) {
                    mFlashlightHandler.sendEmptyMessage(
                            isChecked ? MainActivity.START_BLINKING : MainActivity.TURN_ON);
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
    public void onDestroyView() {
        mActivity.setFlashlightCallbacks(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mFlashlight.unregisterStateChanged();
        mActivity = null;
        super.onDestroy();
    }

    public void setToggleButtonState() {
        setToggleButtonState(mFlashlight.isTurnedOn());
    }

    public void setToggleButtonState(boolean enabled) {
        mToggleButton.setImageResource(enabled ? R.drawable.button_on_state : R.drawable.button_off_state);
    }

    @Override
    public void beforeTaskExecute() {
        setToggleButtonState(!mFlashlight.isTurnedOn());
    }

    @Override
    public void afterTaskExecute(boolean ok) {
        if (mActivity != null) {
            setToggleButtonState();
            mActivity.setKeepScreenOn();
            if (!ok && mFlashlight.isAvailable()) {
                Toast.makeText(getActivity(), R.string.error_string, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
