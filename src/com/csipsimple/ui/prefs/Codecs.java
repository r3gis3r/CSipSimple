/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.csipsimple.ui.prefs;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.Log;

import java.util.ArrayList;
import java.util.List;

public class Codecs extends FragmentActivity {

	protected static final String THIS_FILE = "Codecs";
    private ViewPager mViewPager;
    private boolean useCodecsPerSpeed = true;
    private boolean showVideoCodecs = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.codecs_pager);

        final ActionBar ab = getSupportActionBar();
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        ab.setDisplayShowHomeEnabled(true);
        ab.setDisplayShowTitleEnabled(true);
        
        mViewPager = (ViewPager) findViewById(R.id.pager);
        TabsAdapter tabAdapter = new TabsAdapter(this, ab, mViewPager);
        useCodecsPerSpeed = SipConfigManager.getPreferenceBooleanValue(this, SipConfigManager.CODECS_PER_BANDWIDTH);
        showVideoCodecs   = SipConfigManager.getPreferenceBooleanValue(this, SipConfigManager.USE_VIDEO);
        if(useCodecsPerSpeed) {
            Tab audioNb = ab.newTab().setText( R.string.slow ).setIcon(R.drawable.ic_prefs_media);
            Tab audioWb = ab.newTab().setText( R.string.fast ).setIcon(R.drawable.ic_prefs_media);
            tabAdapter.addTab(audioWb, CodecsFragment.class);
            tabAdapter.addTab(audioNb, CodecsFragment.class);
            if(showVideoCodecs) {
                Tab videoNb = ab.newTab().setText( R.string.slow ).setIcon(R.drawable.ic_prefs_media_video);
                Tab videoWb = ab.newTab().setText( R.string.fast ).setIcon(R.drawable.ic_prefs_media_video);
                
                tabAdapter.addTab(videoWb, CodecsFragment.class);
                tabAdapter.addTab(videoNb, CodecsFragment.class);
            }
        }else {
            Tab audioTab = ab.newTab().setIcon(R.drawable.ic_prefs_media);
            tabAdapter.addTab(audioTab, CodecsFragment.class);
            
            if(showVideoCodecs) {
                Tab videoTab = ab.newTab().setIcon(R.drawable.ic_prefs_media_video);
                tabAdapter.addTab(videoTab, CodecsFragment.class);
            }
        }
	}

    private class TabsAdapter extends FragmentPagerAdapter implements
            ViewPager.OnPageChangeListener, ActionBar.TabListener {

        private final Context mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final List<String> mTabs = new ArrayList<String>();
        
        private int mCurrentPosition = -1;
        /**
         * Used during page migration, to remember the next position
         * {@link #onPageSelected(int)} specified.
         */
        private int mNextPosition = -1;
        
        public TabsAdapter(FragmentActivity activity, ActionBar actionBar, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mActionBar = actionBar;
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }
        

        public void addTab(ActionBar.Tab tab, Class<?> clss) {
            mTabs.add(clss.getName());
            mActionBar.addTab(tab.setTabListener(this));
            notifyDataSetChanged();
        }
        

        @Override
        public Fragment getItem(int position) {
            Bundle args = new Bundle();
            if(useCodecsPerSpeed) {
                args.putString(CodecsFragment.BAND_TYPE, (position % 2 == 0)? SipConfigManager.CODEC_WB : SipConfigManager.CODEC_NB );
                args.putInt(CodecsFragment.MEDIA_TYPE, (position < 2)? CodecsFragment.MEDIA_AUDIO : CodecsFragment.MEDIA_VIDEO );
            }else {
                args.putString(CodecsFragment.BAND_TYPE, SipConfigManager.CODEC_WB );
                args.putInt(CodecsFragment.MEDIA_TYPE, (position < 1)? CodecsFragment.MEDIA_AUDIO : CodecsFragment.MEDIA_VIDEO );
            }
            return Fragment.instantiate(mContext, mTabs.get(position), args);
            
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }



        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // Nothing to do
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mViewPager.getCurrentItem() != tab.getPosition()) {
                mViewPager.setCurrentItem(tab.getPosition(), true);
            }
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            // Nothing to do
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Nothing to do
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);

            if (mCurrentPosition == position) {
                Log.w(THIS_FILE, "Previous position and next position became same (" + position
                        + ")");
            }

            mNextPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            switch (state) {
                case ViewPager.SCROLL_STATE_IDLE: {
                    invalidateOptionsMenu();

                    mCurrentPosition = mNextPosition;
                    break;
                }
                case ViewPager.SCROLL_STATE_DRAGGING:
                case ViewPager.SCROLL_STATE_SETTLING:
                default:
                    break;
            }
        }
        
    }
	
	
}
