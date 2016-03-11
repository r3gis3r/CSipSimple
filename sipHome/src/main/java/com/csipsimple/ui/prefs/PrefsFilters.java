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

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.view.MenuItem;
import com.csipsimple.api.SipProfile;
import com.csipsimple.ui.account.AccountsChooserListActivity;
import com.csipsimple.ui.filters.AccountFilters;
import com.csipsimple.utils.Compatibility;


public class PrefsFilters extends AccountsChooserListActivity {
	
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getSherlock().getActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
	@Override
	protected boolean showInternalAccounts() {
		return true;
	}

    @Override
    public void onAccountClicked(long id, String displayName, String wizard) {
        if(id != SipProfile.INVALID_ID){
            Intent it = new Intent(this, AccountFilters.class);
            it.putExtra(SipProfile.FIELD_ID,  id);
            it.putExtra(SipProfile.FIELD_DISPLAY_NAME, displayName);
            it.putExtra(SipProfile.FIELD_WIZARD, wizard);
            startActivity(it);
        }
    }
    

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == Compatibility.getHomeMenuId()) {
            finish();
            return true;
        }
        return false;
    }
}
