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

import android.content.Intent;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.PreferencesWrapper;


public class PrefsMedia extends GenericPrefs {


	private static final String MISC_KEY = "misc";
	private static final String AUDIO_VOLUME_KEY = "audio_volume";
	private static final String AUDIO_QUALITY_KEY = "audio_quality";

	@Override
	protected int getXmlPreferences() {
		return R.xml.prefs_media;
	}
	
	/*
	@Override
	protected void beforeBuildPrefs() {
		super.beforeBuildPrefs();
		ActionBar ab = getActionBar();
		if(ab != null) {
			ab.setDisplayHomeAsUpEnabled(true);
		}
	}
	*/
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int selId = item.getItemId();
		if(selId == Compatibility.getHomeMenuId()) {
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void afterBuildPrefs() {
		super.afterBuildPrefs();
		PreferencesWrapper pfw = new PreferencesWrapper(this);
		if(!pfw.isAdvancedUser()) {
			
			
			hidePreference(AUDIO_QUALITY_KEY, SipConfigManager.SND_MEDIA_QUALITY);
			hidePreference(AUDIO_QUALITY_KEY, SipConfigManager.ECHO_CANCELLATION_TAIL);
			hidePreference(AUDIO_QUALITY_KEY, SipConfigManager.ECHO_MODE);
			hidePreference(AUDIO_QUALITY_KEY, SipConfigManager.SND_PTIME);
			hidePreference(AUDIO_QUALITY_KEY, SipConfigManager.HAS_IO_QUEUE);
			
			
			hidePreference(null, "band_types");
			
			hidePreference(AUDIO_VOLUME_KEY, SipConfigManager.SND_MIC_LEVEL);
			hidePreference(AUDIO_VOLUME_KEY, SipConfigManager.SND_SPEAKER_LEVEL);
			
			hidePreference(AUDIO_VOLUME_KEY, SipConfigManager.SND_BT_MIC_LEVEL);
			hidePreference(AUDIO_VOLUME_KEY, SipConfigManager.SND_BT_SPEAKER_LEVEL);
			
			hidePreference(AUDIO_VOLUME_KEY, SipConfigManager.USE_SOFT_VOLUME);
			
			//hidePreference("perfs", SipConfigManager.THREAD_COUNT);
			//hidePreference(null, "perfs");
			
			hidePreference(MISC_KEY, SipConfigManager.SND_AUTO_CLOSE_TIME);
			hidePreference(MISC_KEY, SipConfigManager.USE_ROUTING_API);
			hidePreference(MISC_KEY, SipConfigManager.USE_MODE_API);
			hidePreference(MISC_KEY, SipConfigManager.SET_AUDIO_GENERATE_TONE);
			hidePreference(MISC_KEY, SipConfigManager.SIP_AUDIO_MODE);
			hidePreference(MISC_KEY, SipConfigManager.USE_SGS_CALL_HACK);
			hidePreference(MISC_KEY, SipConfigManager.USE_WEBRTC_HACK);
			hidePreference(MISC_KEY, SipConfigManager.DO_FOCUS_AUDIO);
			hidePreference(MISC_KEY, SipConfigManager.MICRO_SOURCE);
			
		}
		
		PreferenceScreen pfs = getPreferenceScreen();
		Preference codecsPrefs = pfs.findPreference("codecs_list");
		codecsPrefs.setIntent(new Intent(this, Codecs.class));
		

		ListPreference lp = (ListPreference) findPreference(SipConfigManager.AUDIO_IMPLEMENTATION);
		boolean isGinger = Compatibility.isCompatible(9);
		CharSequence[] entries = new CharSequence[isGinger ? 2 : 1];
		CharSequence[] values = new CharSequence[isGinger ? 2 : 1];
		
		values[0] = "0";
		entries[0] = "Java";
		if(isGinger) {
			values[1] = "1";
			entries[1] = "OpenSL-ES";
		}
		
		lp.setEntries(entries);
		lp.setEntryValues(values);
	}

	@Override
	protected void updateDescriptions() {
		// TODO Auto-generated method stub
		
	}


	
}
