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

package com.csipsimple.wizards;

import android.content.Intent;

import java.util.List;

import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.PreferencesWrapper;

public interface WizardIface {

    /**
     * Set the parent preference container. This method may be used to store
     * parent context and use it.
     * 
     * @param parent The base preference container that is basically a
     *            preference activity.
     */
    void setParent(BasePrefsWizard parent);

    /**
     * Get the preference resource to be used for the preference view.
     * 
     * @return The preference resource identifier.
     */
    int getBasePreferenceResource();

    /**
     * Fill the layout once inflated with sip profile content.
     * 
     * @param account The account to fill information of.
     */
    void fillLayout(SipProfile account);

    /**
     * Update descriptions of fields. This is called each time something change.
     * It could update the description with content of the value.
     */
    void updateDescriptions();

    /**
     * Retrieve the default summary for a given field.
     * 
     * @param fieldName the name of the field to retrieve summary of.
     * @return The summary of the field.
     */
    String getDefaultFieldSummary(String fieldName);

    // Save
    /**
     * Build the account based on preference view contents.
     * 
     * @param account The sip profile already saved in database
     * @return the sip profile to save into databse based on fields contents.
     */
    SipProfile buildAccount(SipProfile account);

    /**
     * Set default global application preferences. This is a hook method to set
     * preference when an account is saved with this profile. It's useful for
     * sip providers that needs global settings hack.
     * 
     * @param prefs The preference wrapper interface.
     */
    void setDefaultParams(PreferencesWrapper prefs);

    boolean canSave();

    /**
     * Does the wizard changes something that requires to restart sip stack? If
     * so once saved, the wizard will also ask for a stack restart to take into
     * account any preference changed with
     * {@link #setDefaultParams(PreferencesWrapper)}
     * 
     * @return true if the wizard would like the sip stack to restart
     */
    boolean needRestart();

    List<Filter> getDefaultFilters(SipProfile acc);

    // Extras
    void onActivityResult(int requestCode, int resultCode, Intent data);

    void onStart();
    void onStop();
}
