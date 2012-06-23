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

package com.csipsimple.ui.prefs.hc;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.ui.prefs.PrefsFilters;
import com.csipsimple.ui.prefs.PrefsLogic;
import com.csipsimple.utils.PreferencesWrapper;

import java.util.List;

@TargetApi(11)
public class MainPrefs extends SherlockPreferenceActivity {
    private PreferencesWrapper prefsWrapper;
    private List<Header> mFragments;

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.prefs_headers, target);
        for(Header header : target) {
            // Well not the cleanest way to do that...
            if(header.iconRes == R.drawable.ic_prefs_fast) {
                header.intent = new Intent(SipManager.ACTION_UI_PREFS_FAST);
            }else if(header.iconRes == R.drawable.ic_prefs_filter) {
                header.intent = new Intent(this, PrefsFilters.class);
            }
        }
        mFragments = target;
    }
    
    @Override
    public Header onGetInitialHeader() {
        for(Header h : mFragments) {
            if(!TextUtils.isEmpty(h.fragment)) {
                return h;
            }
        }
        return super.onGetInitialHeader();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsWrapper = new PreferencesWrapper(this);
        // TODO -- enable display home as up
        //getActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.main_prefs, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        PrefsLogic.onMainActivityPrepareOptionMenu(menu, this, prefsWrapper);
        return super.onPrepareOptionsMenu(menu);
    }
    

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(PrefsLogic.onMainActivityOptionsItemSelected(item, this, prefsWrapper)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
}
