package org.trv.alex.flashlight;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    private BottomNavigationView mBottomNavigationView;

    private OnBackPressedListener mOnBackPressedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        if (!Flashlight.getInstance(getApplicationContext()).isAvailable()) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.warning_string)
                    .setMessage(R.string.not_support_flashlight_string)
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();
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
        return AppPreferences.getPrefUseBottomNavigationBar(this) ? mBottomNavigationView : mTabLayout;
    }

    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
        mOnBackPressedListener = onBackPressedListener;
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

    private void setCurrentFragment(Fragment fragment, int resId, String tag) {
        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentByTag(tag) == null) {
            fm.beginTransaction().replace(resId, fragment, tag).commit();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(AppPreferences.PREF_USE_BOTTOM_NAVIGATION_BAR)) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
