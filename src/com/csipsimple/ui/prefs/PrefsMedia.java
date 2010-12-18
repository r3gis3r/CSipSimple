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
			
			
			hidePreference("audio_quality", "snd_media_quality");
			hidePreference("audio_quality", "echo_cancellation_tail");
			hidePreference("audio_quality", "echo_mode");
			hidePreference("audio_quality", "snd_ptime");
			hidePreference("audio_quality", "has_io_queue");
			
			
			hidePreference("audio_volume", "snd_mic_level");
			hidePreference("audio_volume", "snd_speaker_level");
			hidePreference("audio_volume", "use_soft_volume");
		//	hidePreference("audio_volume", "snd_stream_level");
			
			hidePreference("perfs", "thread_count");
			hidePreference(null, "perfs");
			
			hidePreference("misc", "snd_auto_close_time");
			hidePreference("misc", "use_routing_api");
			hidePreference("misc", "use_mode_api");
			hidePreference("misc", "set_audio_generate_tone");
			hidePreference("misc", "sip_audio_mode");
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
