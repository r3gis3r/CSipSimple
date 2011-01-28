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

import org.pjsip.pjsua.pj_time_val;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;
import org.pjsip.pjsua.pjsua_call_info;

import android.os.SystemClock;

import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Log;

public final class PjSipCalls {
	
	@SuppressWarnings("serial")
	public static class UnavailableException extends Exception {

		public UnavailableException() {
			super("Unable to find call infos from stack");
		}
	}

	private static final String THIS_FILE = "PjSipCalls";
	
	
	public static SipCallSession getCallInfo(int callId, PjSipService service) {
		SipCallSession session = new SipCallSession();
		session.setCallId(callId);
		
		try {
			session = updateSessionFromPj(session, service);
		} catch (UnavailableException e) {
			session.setCallState(SipCallSession.InvState.NULL);
			Log.e(THIS_FILE, "This account is not available anymore");
		}
		
		return session;
	}
	
	
	
	private static SipCallSession updateSession(SipCallSession session, pjsua_call_info pjCallInfo, PjSipService service) {
		session.setCallId(pjCallInfo.getId());
		
		try {
			int status_code = pjCallInfo.getLast_status().swigValue();
			session.setLastStatusCode(status_code);
			Log.d(THIS_FILE, "Last status code is "+status_code);
			//String status_text = pjCallInfo.getLast_status_text().getPtr();
		}catch(IllegalArgumentException e) {
			//The status code does not exist in enum ignore it
		}
		//Hey lucky man we have nothing to think about here cause we have a bijection between int / state
		session.setCallState( pjCallInfo.getState().swigValue() );
		session.setMediaStatus( pjCallInfo.getMedia_status().swigValue() );
		session.setRemoteContact( pjCallInfo.getRemote_info().getPtr() );
		session.setConfPort( pjCallInfo.getConf_slot() );
		
		SipProfile account = service.getAccountForPjsipId(pjCallInfo.getAcc_id());
		if(account != null) {
			session.setAccId( account.id );
		}
		pj_time_val duration = pjCallInfo.getConnect_duration();
		session.setConnectStart( SystemClock.elapsedRealtime () - duration.getSec() * 1000 - duration.getMsec() ); 
		
		return session;
	}
	
	public static SipCallSession updateSessionFromPj(SipCallSession session, PjSipService service) throws UnavailableException {
		Log.d(THIS_FILE, "Update call "+session.getCallId());
		pjsua_call_info pj_info = new pjsua_call_info();
		int status = pjsua.call_get_info(session.getCallId(), pj_info);
		
		if(status != pjsua.PJ_SUCCESS) {
			//Log.e(THIS_FILE, "Error while getting Call info from stack");
			throw new UnavailableException();
		}
		
		session = updateSession(session, pj_info, service);
		session.setMediaSecure(pjsua.is_call_secure(session.getCallId()) == pjsuaConstants.PJ_TRUE);
		
		return session;
	}
	
	public static String dumpCallInfo(int callId) {
		return pjsua.call_dump(callId, pjsua.PJ_TRUE, " ").getPtr();
	}
	
}
