package org.trv.alex.flashlight;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public interface OnBackPressedListener {
        void onPressedBack();
        boolean condition();
    }

    public interface FlashlightCallbacks {
        void beforeTaskExecute();
        void afterTaskExecute(boolean ok);
    }

    public static final int TURN_ON = 1;
    public static final int TURN_OFF = 2;
    public static final int START_BLINKING = 3;

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private BottomNavigationView mBottomNavigationView;

    private OnBackPressedListener mOnBackPressedListener;

    private FlashlightCallbacks mFlashlightCallbacks;

    private HandlerThread mFlashlightHandlerThread;
    private Handler mFlashlightHandler;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    private Flashlight mFlashlight;

    private boolean mIsRestarting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFlashlight = Flashlight.getInstance(getApplicationContext());

        boolean useBottomNavBar = AppPreferences.getPrefUseBottomNavigationBar(this);
        if (useBottomNavBar) {
            setContentView(R.layout.activity_main_bottom_nav_bar);
            setCurrentFragment(new MainFragment(), R.id.fragment_container_btm_nav_bar, MainFragment.TAG);
            mBottomNavigationView = findViewById(R.id.bottom_navigation);
            mBottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.nav_flashlight:
                            setCurrentFragment(new MainFragment(), R.id.fragment_container_btm_nav_bar, MainFragment.TAG);
                            break;

                        case R.id.nav_screen:
                            setCurrentFragment(new ScreenFragment(), R.id.fragment_container_btm_nav_bar, ScreenFragment.TAG);
                            break;

                        case R.id.nav_preferences:
                            setCurrentFragment(new SettingsFragment(), R.id.fragment_container_btm_nav_bar, SettingsFragment.TAG);
                            break;
                    }
                    return true;
                }
            });
        } else {
            setContentView(R.layout.activity_main);
            mViewPager = findViewById(R.id.view_pager);
            setupViewPager();
            mTabLayout = findViewById(R.id.tab_layout);
            mTabLayout.setupWithViewPager(mViewPager);
        }

        if (!mFlashlight.isAvailable()) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.warning_string)
                    .setMessage(R.string.not_support_flashlight_string)
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();
        }

        mFlashlightHandlerThread = new HandlerThread("Flashlight", Process.THREAD_PRIORITY_BACKGROUND);
        mFlashlightHandlerThread.start();

        mFlashlightHandler = new Handler(mFlashlightHandlerThread.getLooper()) {
            @Override
            public void handleMessage(final Message msg) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mFlashlightCallbacks != null) {
                            mFlashlightCallbacks.beforeTaskExecute();
                        }
                    }
                });
                final boolean ok;
                switch (msg.what) {
                    case TURN_ON:
                        ok = mFlashlight.turnOn();
                        break;
                    case TURN_OFF:
                        ok = mFlashlight.turnOff();
                        break;
                    case START_BLINKING:
                        int onMs = AppPreferences.getPrefEnabledDurationFlashlight(getApplicationContext()) * 1000;
                        int offMs = AppPreferences.getPrefDisabledDurationFlashlight(getApplicationContext()) * 1000;
                        mFlashlight.blink(onMs, offMs);
                        ok = true;
                        break;
                    default:
                        ok = true;
                        super.handleMessage(msg);
                }
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mFlashlightCallbacks != null) {
                            mFlashlightCallbacks.afterTaskExecute(ok);
                        }
                    }
                });
            }
        };

        if (AppPreferences.getPrefTurnOnFlashlightOnStart(getApplicationContext())) {
            mFlashlightHandler.sendEmptyMessage(TURN_ON);
        }
    }

    /**
     * Sets up ViewPager on the main screen and adds fragment for each tab
     */
    private void setupViewPager() {
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getFragmentManager());
        viewPagerAdapter.addFragment(new MainFragment(), getString(R.string.pref_flashlight_category_string));
        viewPagerAdapter.addFragment(new ScreenFragment(), getString(R.string.pref_screen_category_string));
        viewPagerAdapter.addFragment(new SettingsFragment(), getString(R.string.settings_string));
        mViewPager.setAdapter(viewPagerAdapter);
    }

    public ViewPager getViewPager() {
        return mViewPager;
    }

    public View getBarLayout() {
        return mBottomNavigationView != null ? mBottomNavigationView : mTabLayout;
    }

    public Handler getFlashlightHandler() {
        return mFlashlightHandler;
    }

    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
        mOnBackPressedListener = onBackPressedListener;
    }

    public void setFlashlightCallbacks(FlashlightCallbacks flashlightCallbacks) {
        mFlashlightCallbacks = flashlightCallbacks;
    }

    public void setKeepScreenOn() {
        boolean preventFromSleeping = AppPreferences.getPrefPreventScreenFromSleeping(getApplicationContext());
        View barView = getBarLayout();
        if (preventFromSleeping) {
            if (mFlashlight.isTurnedOn()) {
                barView.setKeepScreenOn(true);
            } else {
                barView.setKeepScreenOn(false);
            }
        } else {
            barView.setKeepScreenOn(false);
        }
    }

    @Override
    public void onBackPressed() {
        if (mOnBackPressedListener != null && mOnBackPressedListener.condition()) {
            mOnBackPressedListener.onPressedBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        if (AppPreferences.getPrefTurnOffFlashlightOnMinimize(getApplicationContext())
                && !mIsRestarting) {
            mFlashlightHandler.sendEmptyMessage(MainActivity.TURN_OFF);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mFlashlightHandlerThread.quitSafely();
        } else {
            mFlashlightHandlerThread.quit();
        }
        mFlashlight.close();
        super.onDestroy();
    }

    private void setCurrentFragment(Fragment fragment, int resId, String tag) {
        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentByTag(tag) == null) {
            fm.beginTransaction().replace(resId, fragment, tag).commit();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(AppPreferences.PREF_USE_BOTTOM_NAVIGATION_BAR)) {
            mFlashlightHandler.sendEmptyMessage(MainActivity.TURN_OFF);
            mIsRestarting = true;
            Intent intent = new Intent(this, MainActivity.class);
            finish();
            startActivity(intent);
        } else if (key.equals(AppPreferences.PREF_PREVENT_SCREEN_FROM_SLEEPING)) {
            setKeepScreenOn();
        }
    }
}
