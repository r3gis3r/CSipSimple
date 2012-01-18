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

package com.csipsimple.utils;

import android.content.Context;

import com.csipsimple.R;
import com.csipsimple.api.SipCallSession;

public class CallsUtils {
	/**
	 * Get the corresponding string for a given state
	 * Can be used to translate or debug current state
	 * @return the string reprensenting this call info state
	 */
	public static final String getStringCallState(SipCallSession session, Context context) {

		int callState = session.getCallState();
		switch(callState) {
		case SipCallSession.InvState.CALLING:
			return context.getString(R.string.call_state_calling);
		case SipCallSession.InvState.CONFIRMED:
			return context.getString(R.string.call_state_confirmed);
		case SipCallSession.InvState.CONNECTING:
			return context.getString(R.string.call_state_connecting);
		case SipCallSession.InvState.DISCONNECTED:
			return context.getString(R.string.call_state_disconnected);
		case SipCallSession.InvState.EARLY:
			return context.getString(R.string.call_state_early);
		case SipCallSession.InvState.INCOMING:
			return context.getString(R.string.call_state_incoming);
		case SipCallSession.InvState.NULL:
			return context.getString(R.string.call_state_null);
		}
		
		return "";
	}
}
