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

import com.csipsimple.utils.Log;

import org.pjsip.pjsua.pj_pool_t;
import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjmedia_port;
import org.pjsip.pjsua.pjmedia_tone_desc;
import org.pjsip.pjsua.pjmedia_tone_digit;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsua_call_info;

/**
 * DTMF In band tone generator for a given call object
 * It creates it's own pool, media port, and can stream in. 
 *
 */
public class PjStreamDialtoneGenerator {

    
    private static final String THIS_FILE = "PjStreamDialtoneGenerator";
    private static String SUPPORTED_DTMF = "0123456789abcd*#";
    private final int callId;
    private final boolean streamAsMicro;
	private pj_pool_t dialtonePool;
	private pjmedia_port dialtoneGen;
	private int dialtoneSlot = -1;
	private Thread mLoopingThread = null;
	private Boolean mContinueLooping = false;
	
	public PjStreamDialtoneGenerator(int aCallId) {
        this(aCallId, true);
    }
	
    public PjStreamDialtoneGenerator(int aCallId, boolean onMicro) {
        callId = aCallId;
        streamAsMicro = onMicro;
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
		if(streamAsMicro) {
    		int callConfSlot = info.getConf_slot();
    		status = pjsua.conf_connect(dialtoneSlot, callConfSlot);
		}else {
		    status = pjsua.conf_connect(dialtoneSlot, 0);
		}
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
	    stopSending();
		// Destroy the port
		if (dialtoneSlot != -1) {
			pjsua.conf_remove_port(dialtoneSlot);
			dialtoneSlot = -1;
		}
		
        if (dialtoneGen != null) {
            pjsua.pjmedia_port_destroy(dialtoneGen);
            dialtoneGen = null;
        }

		if (dialtonePool != null) {
			pjsua.pj_pool_release(dialtonePool);
			dialtonePool = null;
		}
	}
	
	/**
	 * Send multiple tones.
	 * @param dtmfChars tones list to send. 
	 * @return the pjsip status
	 */
	public synchronized int sendPjMediaDialTone(String dtmfChars) {
	    int status = ensureDialtoneGen();
	    if(status != pjsua.PJ_SUCCESS) {
	        return status;
	    }
		stopSending();
		
		for(int i = 0 ; i < dtmfChars.length(); i++ ) {
		    char d = dtmfChars.charAt(i);
		    if(SUPPORTED_DTMF.indexOf(d) == -1) {
		        Log.w(THIS_FILE, "Unsupported DTMF char " + d);
		    } else {
		        // Found dtmf char, use digit api
		        pjmedia_tone_digit[] tone = new pjmedia_tone_digit[1];
		        tone[0] = new pjmedia_tone_digit();
                tone[0].setVolume((short) 0);
                tone[0].setOn_msec((short) 100);
                tone[0].setOff_msec((short) 200);
                tone[0].setDigit(d);
                pjsua.pjmedia_tonegen_play_digits(dialtoneGen, 1, tone, 0);
		    }
		}

		return status;
	}
	/**
	 * Start playback of a waiting tone.
	 * This will create a thread looping until {@link #stopDialtoneGenerator()} called
	 *  
	 * @return #PJ_SUCCESS if start done correctly
	 */
    public synchronized int startPjMediaWaitingTone() {
        int status = ensureDialtoneGen();
        if (status != pjsua.PJ_SUCCESS) {
            return status;
        }
        stopSending();
        mContinueLooping = true;
        mLoopingThread = new Thread() {
            @Override
            public void run() {
                while(mContinueLooping) {
                    try {
                        // Found dtmf char, use digit api
                        pjmedia_tone_desc[] tone = new pjmedia_tone_desc[1];
                        tone[0] = new pjmedia_tone_desc();
                        tone[0].setVolume((short) 0);  // 0 means default
                        tone[0].setOn_msec((short) 100);
                        tone[0].setOff_msec((short) 200);
                        tone[0].setFreq1((short)440);
                        tone[0].setFreq2((short)350); // Not sure about this one
                        pjsua.pjmedia_tonegen_play(dialtoneGen, 1, tone, 0);
                        mContinueLooping.wait(3000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } 
        };
        mLoopingThread.start();
        return status;
    }
	
	private int ensureDialtoneGen() {
        if (dialtoneGen == null) {
            int status = startDialtoneGenerator();
            if (status != pjsua.PJ_SUCCESS) {
                return -1;
            }
        }
        return pjsua.PJ_SUCCESS;
	}
	
	private void stopSending() {
	    if(mLoopingThread != null) {
	        mContinueLooping = false;
	        mContinueLooping.notify();
	        mLoopingThread.interrupt();
	    }
        if (dialtoneGen != null) {
            pjsua.pjmedia_tonegen_stop(dialtoneGen);
        }
        if(mLoopingThread != null) {
            try {
                mLoopingThread.join(100);
            } catch (InterruptedException e) {
                Log.e(THIS_FILE, "Problem joining looping thread", e);
            }finally {
                mLoopingThread = null;
            }
        }
	}

}
