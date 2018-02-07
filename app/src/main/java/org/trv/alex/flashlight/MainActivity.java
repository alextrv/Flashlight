package org.trv.alex.flashlight;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public interface OnBackPressedListener {
        void onPressedBack();
        boolean condition();
    }

    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    private OnBackPressedListener mOnBackPressedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewPager = findViewById(R.id.view_pager);
        setupViewPager();
        mTabLayout = findViewById(R.id.tab_layout);
        mTabLayout.setupWithViewPager(mViewPager);
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

    public TabLayout getTabLayout() {
        return mTabLayout;
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
}
