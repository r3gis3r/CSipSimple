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
package com.csipsimple.pjsip;

import java.util.HashMap;
import java.util.Map;

import org.pjsip.pjsua.pj_pool_t;
import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjmedia_port;
import org.pjsip.pjsua.pjmedia_tone_desc;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsua_call_info;

public class PjStreamDialtoneGenerator {

	private pj_pool_t dialtonePool;
	private pjmedia_port dialtoneGen;
	private int dialtoneSlot = -1;
	private Object dialtoneMutext = new Object();
	
	private static final Map<String, short[]> DIGIT_MAP = new HashMap<String, short[]>() {
		private static final long serialVersionUID = -6656807954448449227L;

		{
			put("0", new short[] { 941, 1336 });
			put("1", new short[] { 697, 1209 });
			put("2", new short[] { 697, 1336 });
			put("3", new short[] { 697, 1477 });
			put("4", new short[] { 770, 1209 });
			put("5", new short[] { 770, 1336 });
			put("6", new short[] { 770, 1477 });
			put("7", new short[] { 852, 1209 });
			put("8", new short[] { 852, 1336 });
			put("9", new short[] { 852, 1477 });
			put("a", new short[] { 697, 1633 });
			put("b", new short[] { 770, 1633 });
			put("c", new short[] { 852, 1633 });
			put("d", new short[] { 941, 1633 });
			put("*", new short[] { 941, 1209 });
			put("#", new short[] { 941, 1477 });
		}
	};
	
	

	private int startDialtoneGenerator(int callId) {
		synchronized (dialtoneMutext) {
			pjsua_call_info info = new pjsua_call_info();
			pjsua.call_get_info(callId, info);
			int status;

			dialtonePool = pjsua.pjsua_pool_create("mycall", 512, 512);
			pj_str_t name = pjsua.pj_str_copy("dialtoneGen");
			long clockRate = 8000;
			long channelCount = 1;
			long samplesPerFrame = 160;
			long bitsPerSample = 16;
			long options = 0;
			int[] dialtoneSlotPtr = new int[1];
			dialtoneGen = new pjmedia_port();
			status = pjsua.pjmedia_tonegen_create2(dialtonePool, name, clockRate, channelCount, samplesPerFrame, bitsPerSample, options, dialtoneGen);
			if (status != pjsua.PJ_SUCCESS) {
				stopDialtoneGenerator();
				return status;
			}
			status = pjsua.conf_add_port(dialtonePool, dialtoneGen, dialtoneSlotPtr);
			if (status != pjsua.PJ_SUCCESS) {
				stopDialtoneGenerator();
				return status;
			}
			dialtoneSlot = dialtoneSlotPtr[0];
			status = pjsua.conf_connect(dialtoneSlot, info.getConf_slot());
			if (status != pjsua.PJ_SUCCESS) {
				dialtoneSlot = -1;
				stopDialtoneGenerator();
				return status;
			}
			return pjsua.PJ_SUCCESS;
		}
	}

	public void stopDialtoneGenerator() {
		synchronized (dialtoneMutext) {
			// Destroy the port
			if (dialtoneSlot != -1) {
				pjsua.conf_remove_port(dialtoneSlot);
				dialtoneSlot = -1;
			}

			dialtoneGen = null;
			// pjsua.port_destroy(dialtoneGen);

			if (dialtonePool != null) {
				pjsua.pj_pool_release(dialtonePool);
				dialtonePool = null;
			}
		}
	}
	
	
	public int sendPjMediaDialTone(int callId, String character) {
		if (!DIGIT_MAP.containsKey(character)) {
			return -1;
		}
		if (dialtoneGen == null) {
			int status = startDialtoneGenerator(callId);
			if (status != pjsua.PJ_SUCCESS) {
				return -1;
			}
		}

		short freq1 = DIGIT_MAP.get(character)[0];
		short freq2 = DIGIT_MAP.get(character)[1];

		// Play the tone
		pjmedia_tone_desc[] d = new pjmedia_tone_desc[1];
		d[0] = new pjmedia_tone_desc();
		d[0].setVolume((short) 0);
		d[0].setOn_msec((short) 100);
		d[0].setOff_msec((short) 200);
		d[0].setFreq1(freq1);
		d[0].setFreq2(freq2);
		return pjsua.pjmedia_tonegen_play(dialtoneGen, 1, d, 0);
	}

}
