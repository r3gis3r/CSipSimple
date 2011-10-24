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
package com.csipsimple.ui.prefs;

import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.PreferencesWrapper;


public class PrefsMedia extends GenericPrefs {


	@Override
	protected int getXmlPreferences() {
		return R.xml.prefs_media;
	}
	
	@Override
	protected void afterBuildPrefs() {
		super.afterBuildPrefs();
		PreferencesWrapper pfw = new PreferencesWrapper(this);
		if(!pfw.isAdvancedUser()) {
			
			
			hidePreference("audio_quality", SipConfigManager.SND_MEDIA_QUALITY);
			hidePreference("audio_quality", SipConfigManager.ECHO_CANCELLATION_TAIL);
			hidePreference("audio_quality", SipConfigManager.ECHO_MODE);
			hidePreference("audio_quality", SipConfigManager.SND_PTIME);
			hidePreference("audio_quality", SipConfigManager.HAS_IO_QUEUE);
			
			
			hidePreference(null, "band_types");
			
			hidePreference("audio_volume", SipConfigManager.SND_MIC_LEVEL);
			hidePreference("audio_volume", SipConfigManager.SND_SPEAKER_LEVEL);
			
			hidePreference("audio_volume", SipConfigManager.SND_BT_MIC_LEVEL);
			hidePreference("audio_volume", SipConfigManager.SND_BT_SPEAKER_LEVEL);
			
			hidePreference("audio_volume", SipConfigManager.USE_SOFT_VOLUME);
			
			//hidePreference("perfs", SipConfigManager.THREAD_COUNT);
			//hidePreference(null, "perfs");
			
			hidePreference("misc", SipConfigManager.SND_AUTO_CLOSE_TIME);
			hidePreference("misc", SipConfigManager.USE_ROUTING_API);
			hidePreference("misc", SipConfigManager.USE_MODE_API);
			hidePreference("misc", SipConfigManager.SET_AUDIO_GENERATE_TONE);
			hidePreference("misc", SipConfigManager.SIP_AUDIO_MODE);
			hidePreference("misc", SipConfigManager.USE_SGS_CALL_HACK);
			hidePreference("misc", SipConfigManager.USE_WEBRTC_HACK);
			hidePreference("misc", SipConfigManager.DO_FOCUS_AUDIO);
			hidePreference("misc", SipConfigManager.MICRO_SOURCE);
			
		}
		
		PreferenceScreen pfs = getPreferenceScreen();
		Preference codecsPrefs = pfs.findPreference("codecs_list");
		codecsPrefs.setIntent(new Intent(this, Codecs.class));
		
	}

	@Override
	protected void updateDescriptions() {
		// TODO Auto-generated method stub
		
	}


	
}
