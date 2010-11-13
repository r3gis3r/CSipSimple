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
package com.csipsimple.wizards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.csipsimple.R;

public class WizardChooser extends ExpandableListActivity {
	private String[] childFrom;
	private int[] childTo;
	private ArrayList<ArrayList<Map<String, Object>>> childDatas;

	// private static final String THIS_FILE = "SIP ADD ACC W";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.add_account_wizard);

		Context context = getApplicationContext();
		
		// Now build the list adapter
		childFrom = new String[] { WizardUtils.LABEL, WizardUtils.ICON };
		childTo = new int[] { android.R.id.text1, R.id.icon };
		childDatas = WizardUtils.getWizardsGroupedList();
		
		WizardsListAdapter adapter = new WizardsListAdapter(
				this,
				// Groups
				WizardUtils.getWizardsGroups(context),
				android.R.layout.simple_expandable_list_item_1,
				new String[] { WizardUtils.LANG_DISPLAY },
                new int[] { android.R.id.text1 },
				// Child
                childDatas,
				R.layout.wizard_row,
				childFrom, childTo );
		
		
		setListAdapter(adapter);

		Button cancelBt = (Button) findViewById(R.id.cancel_bt);
		cancelBt.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		getExpandableListView().expandGroup(0);
		getExpandableListView().expandGroup(1);
		
	}
	
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Map<String, Object> data = childDatas.get(groupPosition).get(childPosition);
		String wizard_id = (String) data.get(WizardUtils.ID);
		
		Intent result = getIntent();
		result.putExtra(WizardUtils.ID, wizard_id);
		
		setResult(RESULT_OK, result);
		finish();

		return true;
	}

	
	private class WizardsListAdapter extends SimpleExpandableListAdapter {

		public WizardsListAdapter(Context context, List<? extends Map<String, ?>> groupData, int groupLayout, String[] groupFrom, int[] groupTo,
				List<? extends List<? extends Map<String, ?>>> childData, int childLayout, String[] childFrom, int[] childTo) {
			super(context, groupData, groupLayout, groupFrom, groupTo, childData, childLayout, childFrom, childTo);
		}
		
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

			View v;
			if (convertView == null) {
				v = newChildView(isLastChild, parent);
			} else {
				v = convertView;
			}
			bindView(v, childDatas.get(groupPosition).get(childPosition), childFrom, childTo, groupPosition, childPosition);
			return v;
		}


		private void bindView(View view, Map<String, ?> data, String[] from, int[] to, int groupPosition, int childPosition) {
			// Apply TextViews
			TextView v = (TextView) view.findViewById(to[0]);
			if (v != null) {
				v.setText((String) data.get(from[0]));
			}
			// Apply ImageView
			ImageView imgV = (ImageView) view.findViewById(to[1]);
			if (imgV != null) {
				imgV.setImageResource((Integer) data.get(from[1]));
			}
		}
	}

	
}
