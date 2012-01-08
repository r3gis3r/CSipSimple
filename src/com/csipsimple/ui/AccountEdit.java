/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.csipsimple.ui;




import com.csipsimple.wizards.BasePrefsWizard;

public class AccountEdit extends BasePrefsWizard /*implements OnQuitListener */ {
	//private AccountEditFragment detailFragment;

/*
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // If the screen is now in landscape mode, we can show the
            // dialog in-line with the list so we don't need this activity.
            finish();
            return;
        }

        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
            detailFragment = new AccountEditFragment();
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
	         // app icon in Action Bar clicked; go home
	         Intent intent = new Intent(this, AccountsEditList.class);
	         intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	         startActivity(intent);
	         return true;
	    }

        return super.onOptionsItemSelected(item);
	}
	*/

	/*
	@Override
	public void onQuit() {
		finish();
	}

	@Override
	public void onShowProfile(long profileId) {
		
	}
	*/
	
}
