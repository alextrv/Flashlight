package org.trv.alex.flashlight;


import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    private final List<FragmentTitleContainer> mContainerList = new ArrayList<>();

    public ViewPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return mContainerList.get(position).getFragment();
    }

    @Override
    public int getCount() {
        return mContainerList.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mContainerList.get(position).getTitle();
    }

    /**
     * Add fragment and title to {@code mContainerList}.
     * @param fragment the fragment
     * @param title the title for fragment
     */
    public void addFragment(Fragment fragment, String title) {
        mContainerList.add(new FragmentTitleContainer(fragment, title));
    }

    /**
     * Inner class container for fragment and its title. This class has
     * constant fields which can't be changed after object created.
     */
    private static class FragmentTitleContainer {
        private final Fragment mFragment;
        private final String mTitle;

        public FragmentTitleContainer(Fragment fragment, String title) {
            mFragment = fragment;
            mTitle = title;
        }

        public Fragment getFragment() {
            return mFragment;
        }

        public String getTitle() {
            return mTitle;
        }
    }

}
