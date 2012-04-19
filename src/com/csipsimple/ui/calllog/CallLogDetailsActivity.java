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

package com.csipsimple.ui.calllog;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.csipsimple.R;
import com.csipsimple.ui.SipHome;
import com.csipsimple.ui.calllog.CallLogDetailsFragment.OnQuitListener;
import com.csipsimple.utils.Compatibility;

import android.os.Bundle;

public class CallLogDetailsActivity extends SherlockFragmentActivity implements OnQuitListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        if (SipHome.USE_LIGHT_THEME) {
            setTheme(R.style.LightTheme_noTopActionBar);
        }

        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
            CallLogDetailsFragment detailFragment = new CallLogDetailsFragment();
            detailFragment.setArguments(getIntent().getExtras());
            detailFragment.setOnQuitListener(this);
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, detailFragment).commit();
        }
	}
	
	@Override
	protected void onStart() {
		super.onStart();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    if(item.getItemId() == Compatibility.getHomeMenuId()) {
	         finish();
	         return true;
	    }

        return super.onOptionsItemSelected(item);
	}

	@Override
	public void onQuit() {
		finish();
	}

	@Override
	public void onShowCallLog(long[] callsId) {
		
	}
}
