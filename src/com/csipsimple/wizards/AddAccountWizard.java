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

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import com.csipsimple.R;

public class AddAccountWizard extends ListActivity {
	//private static final String THIS_FILE = "SIP ADD ACC W";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.add_account_wizard);
		
	    
        // Now build the list adapter
        SimpleAdapter adapter = new SimpleAdapter(
          // the Context
          this,
          // the data to display
          WizardUtils.getWizardsList(),
          // The layout to use for each item
            R.layout.wizard_row,
            // The list item attributes to display
            new String[] { WizardUtils.LABEL, WizardUtils.ICON },
            // And the ids of the views where they should be displayed (same order)
            new int[] { android.R.id.text1, R.id.icon }
     );

        setListAdapter( adapter ); 
		
        
        Button cancelBt = (Button) findViewById(R.id.cancel_bt);
        cancelBt.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				finish();
			}
        });
	}
	
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Class<?>[] wizards_classes = WizardUtils.getWizardsClasses();
		Class<?> aClass = wizards_classes[position];
	    startActivityForResult(new Intent(this, aClass), 0);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data); 
		switch(requestCode){
		case 0:
			if(resultCode == Activity.RESULT_OK){
				setResult(RESULT_OK, getIntent());
				finish();
			}
		}
	}
}
