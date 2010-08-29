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

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.models.Account;
import com.csipsimple.service.ISipService;
import com.csipsimple.utils.AccountListUtils;
import com.csipsimple.utils.AccountListUtils.AccountStatusDisplay;

public class AccountAdapter extends ArrayAdapter<Account> implements OnClickListener {

	private static final String THIS_FILE = "Account adapter";
	private ISipService service;
	private HashMap<Integer, AccountStatusDisplay> cacheStatusDisplay;
	Activity context;
	String forNumber = null;
	private DBAdapter db;
	
	public static final class AccountListItemViews {
		TextView labelView;
		TextView statusView;
		ImageView iconImage;
		View refreshView;
	}

	public AccountAdapter(Activity aContext, List<Account> list) {
		super(aContext, R.layout.choose_account_row, list);
		this.context= aContext;
		cacheStatusDisplay = new HashMap<Integer, AccountStatusDisplay>();
	}
	
	public AccountAdapter(Activity aContext, List<Account> list, String aForNumber, DBAdapter database) {
		super(aContext, R.layout.choose_account_row, list);
		this.context= aContext;
		cacheStatusDisplay = new HashMap<Integer, AccountStatusDisplay>();
		forNumber = aForNumber;
		db = database;
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
            
            AccountListItemViews tagView = new AccountListItemViews();
            tagView.iconImage = (ImageView)v.findViewById(R.id.wizard_icon);
            tagView.labelView = (TextView)v.findViewById(R.id.AccTextView);
            tagView.statusView = (TextView)v.findViewById(R.id.AccTextStatusView);
            tagView.refreshView = v.findViewById(R.id.refresh_button);
            tagView.refreshView.setOnClickListener(this);
            
            v.setTag(tagView);
        }
        
        bindView(v, position);
        
        return v;
    }
	
	public void bindView(View v, int position) {
		
		final AccountListItemViews tagView = (AccountListItemViews) v.getTag();
		v.setClickable(true);

		Account account = getItem(position);
		// Log.d(THIS_FILE, "has account");
		if (account != null) {
			AccountStatusDisplay accountStatusDisplay = null;
			accountStatusDisplay = (AccountStatusDisplay) cacheStatusDisplay.get(position);
			if(accountStatusDisplay == null) {
				accountStatusDisplay = AccountListUtils.getAccountDisplay(context, service, account.id);
				cacheStatusDisplay.put(position, accountStatusDisplay);
			}

			tagView.labelView.setText(account.display_name);
			// Update status label and color
			if(!accountStatusDisplay.availableForCalls || forNumber == null || db == null) {
				tagView.statusView.setText(accountStatusDisplay.statusLabel);
			}else {
				tagView.statusView.setText("Call : "+account.rewritePhoneNumber(forNumber, db));
			}
			tagView.labelView.setTextColor(accountStatusDisplay.statusColor);
			v.setClickable(!accountStatusDisplay.availableForCalls);
			tagView.refreshView.setVisibility(accountStatusDisplay.availableForCalls?View.GONE:View.VISIBLE);

			// Update account image
			tagView.iconImage.setImageResource(account.getIconResource());
			tagView.refreshView.setTag(account.id);
		}
	}
	

	@Override
	public void onClick(View v) {
		if(service != null) {
			try {
				service.setAccountRegistration((Integer)v.getTag(), 1);
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Unable to contact service", e);
			}
		}
	}
	
	@Override
	public void notifyDataSetChanged() {
		cacheStatusDisplay.clear();
		super.notifyDataSetChanged();
	}
}
