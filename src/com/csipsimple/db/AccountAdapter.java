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
package com.csipsimple.db;

import java.util.List;

import com.csipsimple.R;
import com.csipsimple.models.Account;
import com.csipsimple.service.ISipService;
import com.csipsimple.utils.AccountListUtils;
import com.csipsimple.utils.AccountListUtils.AccountStatusDisplay;
import com.csipsimple.wizards.WizardUtils;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AccountAdapter extends ArrayAdapter<Account> {

	private ISipService service;

	public AccountAdapter(Activity aContext, List<Account> list) {
		super(aContext, R.layout.choose_account_row, list);
	}
	
	public void updateService(ISipService aService) {
		service = aService;
	}
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		
		//Log.d(THIS_FILE, "try to do convertView :: "+position+" / "+getCount());
		//View v = super.getView(position, convertView, parent);
		View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.choose_account_row, parent, false);
        }
        
        v.setClickable(true);

        TextView labelView = (TextView)v.findViewById(R.id.AccTextView);
        TextView statusView = (TextView)v.findViewById(R.id.AccTextStatusView);
        ImageView iconImage = (ImageView)v.findViewById(R.id.wizard_icon);
        
        Account account = getItem(position);
        //Log.d(THIS_FILE, "has account");
        if (account != null){
            
            AccountStatusDisplay accountStatusDisplay = AccountListUtils.getAccountDisplay(service, account.id);
            
            labelView.setText(account.display_name);
            //Update status label and color
            statusView.setText(accountStatusDisplay.statusLabel);
            labelView.setTextColor(accountStatusDisplay.statusColor);
            v.setClickable(!accountStatusDisplay.availableForCalls);
            
            //Update account image
            WizardInfo wizard_infos = WizardUtils.getWizardClass(account.wizard);
            if(wizard_infos != null) {
            	iconImage.setImageResource(wizard_infos.icon);
            }
        }
        
        
        return v;
    }
}
