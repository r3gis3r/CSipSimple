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


package com.csipsimple.ui.filters;

import android.os.Bundle;
import android.text.TextUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;
import com.csipsimple.wizards.WizardUtils;

public class AccountFilters extends SherlockFragmentActivity {

    private static final String THIS_FILE = "AccountFilters";
    private long accountId = SipProfile.INVALID_ID;
    private AccountFiltersListFragment listFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String accountName = null;
        String wizard = null;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            accountId = extras.getLong(SipProfile.FIELD_ID, -1);
            accountName = extras.getString(SipProfile.FIELD_DISPLAY_NAME);
            wizard = extras.getString(SipProfile.FIELD_WIZARD);
        }

        if (accountId == -1) {
            Log.e(THIS_FILE, "You provide an empty account id....");
            finish();
        }
        if(!TextUtils.isEmpty(accountName)) {
            setTitle(getResources().getString(R.string.filters) + " : " + accountName);
        }
        if(!TextUtils.isEmpty(wizard)) {
            ActionBar ab = getSupportActionBar();
            if(ab != null) {
                ab.setIcon(WizardUtils.getWizardIconRes(wizard));
            }
        }

        setContentView(R.layout.account_filters_view);
        listFragment = (AccountFiltersListFragment) getSupportFragmentManager().findFragmentById(R.id.list);
        listFragment.setAccountId(accountId);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == Compatibility.getHomeMenuId()) {
            finish();
            return true;
        }
        return false;
    }
}
