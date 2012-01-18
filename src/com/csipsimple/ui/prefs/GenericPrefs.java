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

package com.csipsimple.ui.prefs;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.utils.Log;

public abstract class GenericPrefs extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    /**
     * Get the xml preference resource for this screen
     * 
     * @return the resource reference
     */
    protected abstract int getXmlPreferences();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        beforeBuildPrefs();
        addPreferencesFromResource(getXmlPreferences());
        afterBuildPrefs();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        updateDescriptions();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateDescriptions();
    }

    /**
     * Process update of description of each preference field
     */
    protected abstract void updateDescriptions();

    /**
     * Optional hook for doing stuff before preference xml is loaded
     */
    protected void beforeBuildPrefs() {
        // By default, nothing to do
    };

    /**
     * Optional hook for doing stuff just after preference xml is loaded
     */
    protected void afterBuildPrefs() {
        // By default, nothing to do
    };

    // Utilities for update Descriptions
    /**
     * Get field summary if nothing set. By default it will try to add _summary
     * to name of the current field
     * 
     * @param field_name Name of the current field
     * @return Translated summary for this field
     */
    protected String getDefaultFieldSummary(String field_name) {
        try {
            String keyid = R.string.class.getField(field_name + "_summary").get(null).toString();
            return getString(Integer.parseInt(keyid));
        } catch (SecurityException e) {
            // Nothing to do : desc is null
        } catch (NoSuchFieldException e) {
            // Nothing to do : desc is null
        } catch (IllegalArgumentException e) {
            // Nothing to do : desc is null
        } catch (IllegalAccessException e) {
            // Nothing to do : desc is null
        }

        return "";
    }

    /**
     * Set summary of a standard string field If empty will display the default
     * summary Else it displays the preference value
     * 
     * @param fieldName the preference key name
     */
    public void setStringFieldSummary(String fieldName) {
        PreferenceScreen pfs = getPreferenceScreen();
        SharedPreferences sp = pfs.getSharedPreferences();
        Preference pref = pfs.findPreference(fieldName);

        String val = sp.getString(fieldName, null);
        if (TextUtils.isEmpty(val)) {
            val = getDefaultFieldSummary(fieldName);
        }
        setPreferenceSummary(pref, val);
    }

    /**
     * Set summary of a password field If empty will display default summary If
     * password will display a * char for each letter of password
     * 
     * @param fieldName the preference key name
     */
    public void setPasswordFieldSummary(String fieldName) {
        PreferenceScreen pfs = getPreferenceScreen();
        SharedPreferences sp = pfs.getSharedPreferences();
        Preference pref = pfs.findPreference(fieldName);

        String val = sp.getString(fieldName, null);

        if (TextUtils.isEmpty(val)) {
            val = getDefaultFieldSummary(fieldName);
        } else {
            val = val.replaceAll(".", "*");
        }
        setPreferenceSummary(pref, val);
    }

    /**
     * Set summary of a list field If empty will display default summary If one
     * item selected will display item name
     * 
     * @param fieldName the preference key name
     */
    public void setListFieldSummary(String fieldName) {
        PreferenceScreen pfs = getPreferenceScreen();
        ListPreference pref = (ListPreference) pfs.findPreference(fieldName);

        CharSequence val = pref.getEntry();
        if (TextUtils.isEmpty(val)) {
            val = getDefaultFieldSummary(fieldName);
        }
        setPreferenceSummary(pref, val);
    }

    /**
     * Safe setSummary on a Preference object that make sure that the preference
     * exists before doing anything
     * 
     * @param pref the preference to change summary of
     * @param val the string to set as preference summary
     */
    protected void setPreferenceSummary(Preference pref, CharSequence val) {
        if (pref != null) {
            pref.setSummary(val);
        }
    }

    /**
     * Hide a preference from the screen so that user can't see and modify it
     * 
     * @param parent the parent group preference if any, leave null if
     *            preference is a root pref
     * @param fieldName the preference key name to hide
     */
    protected void hidePreference(String parent, String fieldName) {
        PreferenceScreen pfs = getPreferenceScreen();
        PreferenceGroup parentPref = pfs;
        if (parent != null) {
            parentPref = (PreferenceGroup) pfs.findPreference(parent);
        }

        Preference toRemovePref = pfs.findPreference(fieldName);

        if (toRemovePref != null && parentPref != null) {
            boolean rem = parentPref.removePreference(toRemovePref);
            Log.d("Generic prefs", "Has removed it : " + rem);
        } else {
            Log.d("Generic prefs", "Not able to find" + parent + " " + fieldName);
        }
    }

}
