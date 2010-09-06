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
package com.csipsimple.utils.audio;

import android.media.AudioManager;

import com.csipsimple.service.SipService;
import com.csipsimple.utils.Compatibility;


public class AudioFocusWrapper {
	private AudioFocus3 audio3;
	private AudioFocus8 audio8;

	/* class initialization fails when this throws an exception */
	static {
		try {
			Class.forName("com.csipsimple.utils.audio.AudioFocus8");
		} catch (Exception ex) {
			try {
				Class.forName("com.csipsimple.utils.audio.AudioFocus3");
			}catch (Exception ex2) {
				throw new RuntimeException(ex);
			}
		}
	}
	
	public AudioFocusWrapper(SipService service, AudioManager manager) {
		if(Compatibility.isCompatible(8)) {
			audio8 = new AudioFocus8(service, manager);
		}else {
			audio3 = new AudioFocus3(service, manager);
		}
	}
	
	public void focus() {
		if(audio8 != null) {
			audio8.focus();
		}else if(audio3 != null) {
			audio3.focus();
		}
	}
	
	public void unFocus() {
		if(audio8 != null) {
			audio8.unFocus();
		}else if(audio3 != null) {
			audio3.unFocus();
		}
	}
	
	
	
}
