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
package com.csipsimple.ui.account;

import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.CallHandler;
import com.csipsimple.utils.CallHandler.onLoadListener;
import com.csipsimple.wizards.WizardUtils;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

public abstract class AccountsChooserListActivity extends Activity implements OnItemClickListener, OnClickListener {

	private AccountAdapter adapter;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Build window
		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accounts_list);
		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_list_accounts);

		// Fill accounts with currently avalaible accounts
    	Cursor c = managedQuery(SipProfile.ACCOUNT_URI, new String[] {
    			SipProfile.FIELD_ID + " AS " + BaseColumns._ID,
    			SipProfile.FIELD_ID,
    			SipProfile.FIELD_DISPLAY_NAME,
    			SipProfile.FIELD_WIZARD
    	}, null, null, null);
    	
    	adapter = new AccountAdapter(this, c);
    	
		addExternalRows();
		
		ListView accountsListView = (ListView) findViewById(R.id.account_list);
		
		accountsListView.setAdapter(adapter);
		accountsListView.setOnItemClickListener(this);
		
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
	

	private void addExternalRows() {

		Map<String, String> callHandlers = CallHandler.getAvailableCallHandlers(this);
		for(String packageName : callHandlers.keySet()) {
			CallHandler ch = new CallHandler(this);
			ch.loadFrom(packageName, null, new onLoadListener() {
				@Override
				public void onLoad(final CallHandler ch) {
					addRow(ch.getLabel(), ch.getIconDrawable(), ch.getFakeProfile());
				}
			});
		}
		
	}


	private static final class AccountListItemViews {
		public TextView labelView;
		public ImageView icon;
	}
	
	protected class AccountAdapter extends SimpleCursorAdapter {
		

		private int wizardColIndex = -1;
		private int nameColIndex = -1;
		
		AccountAdapter(Context context, Cursor c) {
			super(context, R.layout.choose_account_row, c,
					new String[] {},
	                new int[] {});
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
		
		/**
		 * Bind the view to the content datas
		 * @param view The view to bind infos to
		 * @param position Position of the content to bind
		 */
		public void bindView(View view, int position) {
			AccountListItemViews tagView = (AccountListItemViews) view.getTag();
			view.setTag(tagView);
			
			
			// Get the view object and account object for the row
	        final Cursor c = (Cursor) getItem(position);
	        if (c == null){
	        	return;
	        }
	        

			if(nameColIndex == -1) {
				nameColIndex = c.getColumnIndex(SipProfile.FIELD_DISPLAY_NAME);
				wizardColIndex = c.getColumnIndex(SipProfile.FIELD_WIZARD);
			}
	        String wizardName = c.getString(nameColIndex);
			
			tagView.labelView.setText(wizardName);
			tagView.icon.setContentDescription(wizardName);
            //Update account image
            final WizardInfo wizardInfos = WizardUtils.getWizardClass(c.getString(wizardColIndex));
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
