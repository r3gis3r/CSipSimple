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

package com.csipsimple.pjsip;

import org.pjsip.pjsua.pj_pool_t;
import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjmedia_port;
import org.pjsip.pjsua.pjmedia_tone_desc;
import org.pjsip.pjsua.pjmedia_tone_digit;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;
import org.pjsip.pjsua.pjsua_call_info;

/**
 * DTMF In band tone generator for a given call object
 * It creates it's own pool, media port, and can stream in. 
 * @author r3gis3r
 *
 */
public class PjStreamDialtoneGenerator {

    
    private static String SUPPORTED_DTMF = "0123456789abcd*#";
    private final int callId;
	private pj_pool_t dialtonePool;
	private pjmedia_port dialtoneGen;
	private int dialtoneSlot = -1;
	
	
	public PjStreamDialtoneGenerator(int aCallId) {
        callId = aCallId;
    }
	
	/**
	 * Start the tone generate.
	 * This is automatically done by the send dtmf
	 * @return the pjsip error code for creation
	 */
	private synchronized int startDialtoneGenerator() {
		
		pjsua_call_info info = new pjsua_call_info();
		pjsua.call_get_info(callId, info);
		int status;
		
		dialtonePool = pjsua.pjsua_pool_create("tonegen-"+callId, 512, 512);
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

	/**
	 * Stop the dialtone generator.
	 * This has to be called manually when no more DTMF codes are to be send for the associated call
	 */
	public synchronized void stopDialtoneGenerator() {
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
	
	/**
	 * Send multiple tones.
	 * @param dtmfChars tones list to send. Any unknown tones will be considered as pause (1sec). ";" will be considered as 3sec pause instead of wait tone. 
	 * @return the pjsip status
	 */
	public synchronized int sendPjMediaDialTone(String dtmfChars) {
		if (dialtoneGen == null) {
			int status = startDialtoneGenerator();
			if (status != pjsua.PJ_SUCCESS) {
				return -1;
			}
		}
		int status = pjsuaConstants.PJ_SUCCESS;
		
		for(int i = 0 ; i < dtmfChars.length(); i++ ) {
		    char d = dtmfChars.charAt(i);
		    if(SUPPORTED_DTMF.indexOf(d) == -1) {
		        // We should reach that only if first chars are unknown
                // Not supported char, use 3sec temp for a ";", 1 sec else
		        // We don't support wait for dialtone w since no way to get analog tone parsing
		        pjmedia_tone_desc[] tone = new pjmedia_tone_desc[1];
		        tone[0] = new pjmedia_tone_desc();
		        tone[0].setVolume((short) 0);
		        tone[0].setOn_msec((short) 0);
		        tone[0].setOff_msec((short) ((d == ';')? 3000 : 1000));
		        tone[0].setFreq1((short) 10);
		        tone[0].setFreq2((short) 10);
		        
		        pjsua.pjmedia_tonegen_play(dialtoneGen, 1, tone, 0);
		    } else {
		        // Found dtmf char, use digit api
		        pjmedia_tone_digit[] tone = new pjmedia_tone_digit[1];
		        tone[0] = new pjmedia_tone_digit();
                tone[0].setVolume((short) 0);
                tone[0].setOn_msec((short) 100);
                tone[0].setDigit(d);
                
                // Get offtime based on next invalid
                short offTime = 200;
                int unsupportedNext = 0;
                while( i+unsupportedNext+1 < dtmfChars.length() && SUPPORTED_DTMF.indexOf(dtmfChars.charAt(i+unsupportedNext+1)) == -1) {
                    if(dtmfChars.charAt(i+unsupportedNext+1) == ';') {
                        offTime += 3000;
                    }else {
                        offTime += 1000;
                    }
                    unsupportedNext ++;
                }
                i += unsupportedNext;
                
                tone[0].setOff_msec((short) offTime);
                pjsua.pjmedia_tonegen_play_digits(dialtoneGen, 1, tone, 0);
		    }
		}

		return status;
	}

}
