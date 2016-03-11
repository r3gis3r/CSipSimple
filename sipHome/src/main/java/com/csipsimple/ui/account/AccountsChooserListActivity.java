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

package com.csipsimple.ui.account;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.csipsimple.R;
import com.csipsimple.ui.account.AccountsChooserListFragment.OnAccountClickListener;

public abstract class AccountsChooserListActivity extends SherlockFragmentActivity implements OnAccountClickListener {
    
    private AccountsChooserListFragment listFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accounts_chooser_view);
        
        listFragment = (AccountsChooserListFragment) getSupportFragmentManager().findFragmentById(R.id.listFragment);
        
        listFragment.setShowCallHandlerPlugins(showInternalAccounts());
        listFragment.setOnAccountClickListener(this);
    }

    /**
     * Should this activity propose external accounts?
     * @return true if the activity should show external plugin handlers
     */
    protected boolean showInternalAccounts() {
        return false;
    }

}
