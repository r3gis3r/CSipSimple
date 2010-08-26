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
package com.csipsimple.widgets;

import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;

import com.csipsimple.R;
import com.csipsimple.models.AccountInfo;
import com.csipsimple.wizards.WizardUtils;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

public class RegistrationNotification extends RemoteViews {

	private static final Integer[] cells = new Integer[] {
		R.id.cell1,
		R.id.cell2,
		R.id.cell3,
	};
	
	private static final Integer[] icons = new Integer[] {
		R.id.icon1,
		R.id.icon2,
		R.id.icon3,
	};
	
	private static final Integer[] texts = new Integer[] {
		R.id.account_label1,
		R.id.account_label2,
		R.id.account_label3,
	};
	
	public RegistrationNotification(String aPackageName) {
		super(aPackageName, R.layout.notification_registration_layout);
	}

	public void clearRegistrations() {
		for (Integer cellId : cells) {
			setViewVisibility(cellId, View.GONE);
		}
	}

	public void addAccountInfos(Context context, ArrayList<AccountInfo> activeAccountsInfos) {
		int i = 0;
		for(AccountInfo accountInfo : activeAccountsInfos ) {
			if(i<cells.length) {
				setViewVisibility(cells[i], View.VISIBLE);
				WizardInfo wizardInfos = WizardUtils.getWizardClass(accountInfo.getWizard());
				setImageViewResource(icons[i], wizardInfos.icon);
				setTextViewText(texts[i], accountInfo.getDisplayName());
				i++;
			}
		}
		
	}
	

}
