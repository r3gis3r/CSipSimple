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

import java.util.regex.Pattern;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.models.Account;
import com.csipsimple.service.ISipService;
import com.csipsimple.widgets.AccountChooserButton.OnAccountChangeListener;

public class EditSipUri extends LinearLayout implements TextWatcher {

	protected static final String THIS_FILE = "EditSipUri";
	private EditText dialUser;
	private AccountChooserButton accountChooserButtonText;
	private TextView domainTextHelper;
	
	
	public EditSipUri(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.edit_sip_uri, this, true);
		
		dialUser = (EditText) findViewById(R.id.dialtxt_user);
		accountChooserButtonText = (AccountChooserButton) findViewById(R.id.accountChooserButtonText);
		domainTextHelper = (TextView) findViewById(R.id.dialtxt_domain_helper);
		
		//Map events
		accountChooserButtonText.setOnAccountChangeListener(new OnAccountChangeListener() {
			@Override
			public void onChooseAccount(Account account) {
				updateDialTextHelper();
			}
		});
		dialUser.addTextChangedListener(this);
	}
	
	public class ToCall {
		private Integer accountId;
		private String callee;
		public ToCall(Integer acc, String uri) {
			accountId = acc;
			callee = uri;
		}
		
		/**
		 * @return the pjsipAccountId
		 */
		public Integer getAccountId() {
			return accountId;
		}
		/**
		 * @return the callee
		 */
		public String getCallee() {
			return callee;
		}
	};
	
	public void updateService(ISipService service) {
		accountChooserButtonText.updateService(service);
	}
	
	public void updateRegistration() {
		boolean canChangeIfValid = TextUtils.isEmpty(dialUser.getText().toString());
		accountChooserButtonText.updateRegistration(canChangeIfValid);
	}
	

	private void updateDialTextHelper() {

		String dialUserValue = dialUser.getText().toString();
		Account acc = accountChooserButtonText.getSelectedAccount();
		if(!Pattern.matches(".*@.*", dialUserValue) && acc != null) {
			domainTextHelper.setText("@"+acc.getDefaultDomain());
		}else {
			domainTextHelper.setText("");
		}
		
	}
	
	public ToCall getValue() {
		String userName = dialUser.getText().toString();
		String toCall = ""; 
		Integer accountToUse = null;
		if (TextUtils.isEmpty(userName)) {
			return null;
		}
		userName = userName.replaceAll("[ \t]", "");
		Account acc = accountChooserButtonText.getSelectedAccount();
		if (acc != null) {
			accountToUse = acc.id;
			//TODO : escape + and special char in username
			if(Pattern.matches(".*@.*", userName)) {
				toCall = "sip:" + userName +"";
			}else {
				toCall = "sip:" + userName + "@" + acc.getDefaultDomain();
			}
		}else {
			toCall = userName;
		}
		
		return new ToCall(accountToUse, toCall);
	}
	
	
	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// Nothing to do here

	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		updateDialTextHelper();

	}
	
	@Override
	public void afterTextChanged(Editable s) {
		updateDialTextHelper();
	}

	public void clear() {
		dialUser.getText().clear();
	}

	
}
