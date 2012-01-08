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
package com.csipsimple.ui.prefs;

import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.csipsimple.api.SipProfile;
import com.csipsimple.ui.AccountFilters;
import com.csipsimple.ui.AccountsChooserListActivity;
import com.csipsimple.utils.Log;


public class PrefsFilters  extends AccountsChooserListActivity implements OnItemClickListener, OnClickListener {
	

	private static final String THIS_FILE = "PrefsFilters";

	
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		if(id != SipProfile.INVALID_ID){
			Intent it = new Intent(this, AccountFilters.class);
			it.putExtra(SipProfile.FIELD_ID,  id);
			startActivity(it);
		}
	}
	
	@Override
	public void onClick(View v) {
		Long accId = (Long) v.getTag();
		if(accId != null) {
			Intent it = new Intent(this, AccountFilters.class);
			it.putExtra(SipProfile.FIELD_ID,  accId);
			startActivity(it);
		}else {
			Log.w(THIS_FILE, "Hey something is wrong here...");
		}
	}
	
	@Override
	protected boolean showInternalAccounts() {
		return true;
	}
}
