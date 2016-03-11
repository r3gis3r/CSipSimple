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

package com.csipsimple.wizards.impl;

import android.content.Intent;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;
import com.csipsimple.ui.prefs.GenericPrefs;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.wizards.BasePrefsWizard;
import com.csipsimple.wizards.WizardIface;

import java.util.List;
import java.util.regex.Pattern;

public abstract class BaseImplementation implements WizardIface {
	protected BasePrefsWizard parent;
	
	public void setParent(BasePrefsWizard aParent) {
		parent = aParent;
	}
	
	

	//Utilities functions
	protected boolean isEmpty(EditTextPreference edt){
		if(edt.getText() == null){
			return true;
		}
		if(edt.getText().equals("")){
			return true;
		}
		return false;
	}
	
	protected boolean isMatching(EditTextPreference edt, String regex) {
		if(edt.getText() == null){
			return false;
		}
		return Pattern.matches(regex, edt.getText());
	}

    /**
     * @see EditTextPreference#getText()
     * @param edt
     */
	protected String getText(EditTextPreference edt){
		return edt.getText();
	}
	

    /**
     * @see GenericPrefs#setStringFieldSummary(String)
     * @param fieldName
     */
	protected void setStringFieldSummary(String fieldName){
		parent.setStringFieldSummary(fieldName);
	}

    /**
     * @see GenericPrefs#setPasswordFieldSummary(String)
     * @param fieldName
     */
	protected void setPasswordFieldSummary(String fieldName){
		parent.setPasswordFieldSummary(fieldName);
	}
	
	/**
	 * @see GenericPrefs#setListFieldSummary(String)
	 * @param fieldName
	 */
    protected void setListFieldSummary(String fieldName){
        parent.setListFieldSummary(fieldName);
    }
	
    /**
     * @see PreferenceScreen#findPreference(CharSequence)
     */
    @SuppressWarnings("deprecation")
    protected Preference findPreference(String fieldName) {
        return parent.findPreference(fieldName);
    }
    /**
     * @see PreferenceScreen#addPreference(Preference)
     */
    @SuppressWarnings("deprecation")
    protected void addPreference(Preference pref) {
        parent.getPreferenceScreen().addPreference(pref);
        markFieldValid(pref);
    }
    
    /**
     * Hide a preference from the preference screen.
     * @param parentGroup key for parent group if any. If null no parent group are searched
     * @param fieldName key for the field to remove
     */
	@SuppressWarnings("deprecation")
    protected void hidePreference(String parentGroup, String fieldName) {
		PreferenceScreen pfs = parent.getPreferenceScreen();
		PreferenceGroup parentPref = pfs; 
		if (parentGroup != null) {
			parentPref = (PreferenceGroup) pfs.findPreference(parentGroup);
		}

		Preference toRemovePref = pfs.findPreference(fieldName);
		
		if (toRemovePref != null && parentPref != null) {
			boolean rem = parentPref.removePreference(toRemovePref);
			Log.d("Generic prefs", "Has removed it : " + rem);
		} else {
			Log.d("Generic prefs", "Not able to find" + parent + " " + fieldName);
		}
	}
	

	private void markFieldInvalid(Preference field) {
		field.setLayoutResource(R.layout.invalid_preference_row);
	}

	private void markFieldValid(Preference field) {
		field.setLayoutResource(R.layout.valid_preference_row);
	}

	/**
	 * Check the validity of a field and if invalid mark it as invalid
	 * 
	 * @param field
	 *            field to check
	 * @param isNotValid
	 *            if true this field is considered as invalid
	 * @return if the field is valid (!isNotValid) This is convenient for &=
	 *         from a true variable over multiple fields
	 */
	protected boolean checkField(Preference field, boolean isNotValid) {
		if (isNotValid) {
			markFieldInvalid(field);
		} else {
			markFieldValid(field);
		}
		return !isNotValid;
	}
	
	/**
	 * Set global preferences for this wizard
	 * If some preference that need restart are modified here
	 * Do not forget to return true in need restart
	 */
	public void setDefaultParams(PreferencesWrapper prefs) {
		// By default empty implementation
	}
	
	@Override
	public boolean needRestart() {
		return false;
	}
	
	public List<Filter> getDefaultFilters(SipProfile acc) {
		return null;
	}

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // By default empty implementation
    }
    
    public void onStart() {}
    public void onStop() {}
    
}
