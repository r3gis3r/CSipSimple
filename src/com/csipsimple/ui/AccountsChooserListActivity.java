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

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.wizards.WizardUtils;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

public abstract class AccountsChooserListActivity extends Activity implements OnItemClickListener, OnClickListener {

	private DBAdapter database;
	private AccountAdapter adapter;
	
	private List<SipProfile> accountsList;
	private ListView accountsListView;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Build window
		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accounts_list);
		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_list_accounts);

	    //	Log.d(THIS_FILE, "We are updating the list");
    	if(database == null) {
    		database = new DBAdapter(this);
    	}
    	
		// Fill accounts with currently avalaible accounts
		updateList();
		bindAddedRows();
		
		accountsListView = (ListView) findViewById(R.id.account_list);
		
		accountsListView.setAdapter(adapter);
		accountsListView.setOnItemClickListener(this);
		

		//Add add row
		LinearLayout add_row = (LinearLayout) findViewById(R.id.add_account);
		add_row.setVisibility(View.GONE);
		
		
	}
	
	protected boolean showInternalAccounts() {
		return false;
	}
	

	private void addRow(CharSequence label, Drawable dr, SipProfile acc) {
		// Get attr
		
		TypedArray a = obtainStyledAttributes(android.R.style.Theme, new int[]{android.R.attr.listPreferredItemHeight});
		int sListItemHeight = a.getDimensionPixelSize(0, 0);
		a.recycle();
		
		// Add line
		LinearLayout root = (LinearLayout) findViewById(R.id.acc_list_chooser_wrapper);
		
		ImageView separator = new ImageView(this);
		separator.setImageResource(R.drawable.divider_horizontal_dark);
		separator.setScaleType(ScaleType.FIT_XY);
		root.addView(separator, new LayoutParams(LayoutParams.FILL_PARENT, 1));
		
		LinearLayout line = new LinearLayout(this);
		line.setFocusable(true);
		line.setClickable(true);
		line.setOrientation(LinearLayout.HORIZONTAL);
		line.setGravity(Gravity.CENTER_VERTICAL);
		line.setBackgroundResource(android.R.drawable.menuitem_background);
		
		ImageView icon = new ImageView(this);
		icon.setImageDrawable(dr);
		icon.setScaleType(ScaleType.FIT_XY);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(48, 48);
		lp.setMargins(6, 6, 6, 6);
		line.addView(icon, lp);
		
		TextView tv = new TextView(this);
		tv.setText(label);
		tv.setTextAppearance(this, android.R.style.TextAppearance_Medium);
		tv.setTypeface(Typeface.DEFAULT_BOLD);
		line.addView(tv, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		line.setTag(acc.id);
		line.setOnClickListener(this);
		
		root.addView(line, new LayoutParams(LayoutParams.FILL_PARENT,  sListItemHeight));
		
		
	}
	

	private void bindAddedRows() {
		PackageManager pm = getPackageManager();
		List<ResolveInfo> callers = Compatibility.getIntentsForCall(this);
		
		int index = 1; 
		for(final ResolveInfo caller : callers) {
			// Add row if possible
			// Exclude GSM
			SipProfile gsmProfile = new SipProfile();
			gsmProfile.id = SipProfile.INVALID_ID - index;
		
			final SipProfile acc = gsmProfile;
			addRow(caller.loadLabel(pm), caller.loadIcon(pm), acc);
			
			index ++;
		}
	}

    private synchronized void updateList() {
    	
    	
    	database.open();
		accountsList = database.getListAccounts();
		database.close();
    	
    	if(adapter == null) {
    		adapter = new AccountAdapter(this, accountsList);
    		adapter.setNotifyOnChange(false);
    	}else {
    		adapter.clear();
    		for(SipProfile acc : accountsList){
    			adapter.add(acc);
    		}
    		adapter.notifyDataSetChanged();
    	}
    }

	private static final class AccountListItemViews {
		TextView labelView;
		ImageView icon;
	}
	
	protected class AccountAdapter extends ArrayAdapter<SipProfile> {
		Activity context;
		
		AccountAdapter(Activity context, List<SipProfile> list) {
			super(context, R.layout.choose_account_row, list);
			this.context = context;
		}
		
		
		
		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {
			//Create view if not existant
			View view = convertView;
            if (view == null) {
                LayoutInflater viewInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = viewInflater.inflate(R.layout.choose_account_row, parent, false);
                
                AccountListItemViews tagView = new AccountListItemViews();
                tagView.labelView = (TextView)view.findViewById(R.id.AccTextView);
                tagView.icon = (ImageView)view.findViewById(R.id.wizard_icon);
                
                View viewToHide;
                viewToHide = view.findViewById(R.id.refresh_button);
                viewToHide.setVisibility(View.GONE);
                viewToHide = view.findViewById(R.id.AccTextStatusView);
                viewToHide.setVisibility(View.GONE);
                viewToHide = view.findViewById(R.id.useLabel);
                viewToHide.setVisibility(View.GONE);
                
                view.setTag(tagView);
            }
            
            
            bindView(view, position);
           
	        return view;
	        
	    }
		
		
		public void bindView(View view, int position) {
			AccountListItemViews tagView = (AccountListItemViews) view.getTag();
			view.setTag(tagView);
			
			
			// Get the view object and account object for the row
	        final SipProfile account = getItem(position);
	        if (account == null){
	        	return;
	        }
			tagView.labelView.setText(account.display_name);
            //Update account image
            final WizardInfo wizardInfos = WizardUtils.getWizardClass(account.wizard);
            if(wizardInfos != null) {
            	tagView.icon.setImageResource(wizardInfos.icon);
            }
		}

	}

	protected AccountAdapter getAdapter() {
		return adapter;
	}
	

	@Override
	public void onClick(View v) {
		// Nothing to do
	}
	
}
