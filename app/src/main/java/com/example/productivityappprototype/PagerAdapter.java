package com.example.productivityappprototype;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter{
    private int mNumTabs;

    //Constructor for the class
    public PagerAdapter(FragmentManager fragmentManager, int numTabs) {
        super(fragmentManager);
        this.mNumTabs = numTabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch(position) {
            case(0):
                return new ScheduleFragment();
            case(1):
                return new ItemListFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumTabs;
    }
}
