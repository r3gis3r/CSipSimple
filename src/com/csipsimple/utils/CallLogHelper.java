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
package com.csipsimple.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.provider.CallLog;

import com.csipsimple.api.SipCallSession;
import com.csipsimple.models.CallerInfo;

public class CallLogHelper {


	public static void addCallLog(Context context, ContentValues values) {
		ContentResolver contentResolver = context.getContentResolver();
		contentResolver.insert(CallLog.Calls.CONTENT_URI, values);
	}
	
	
	public static ContentValues logValuesForCall(Context context, SipCallSession call, long callStart) {
		ContentValues cv = new ContentValues();
		String remoteContact = call.getRemoteContact();
		
		cv.put(CallLog.Calls.NUMBER, remoteContact);
		
		cv.put(CallLog.Calls.NEW, (callStart > 0)?1:0);
		cv.put(CallLog.Calls.DATE, (callStart>0 )?callStart:System.currentTimeMillis());
		int type = CallLog.Calls.OUTGOING_TYPE;
		int nonAcknowledge = 0; 
		if(call.isIncoming()) {
			type = CallLog.Calls.MISSED_TYPE;
			nonAcknowledge = 1;
			if(callStart>0) {
				nonAcknowledge = 0;
				type = CallLog.Calls.INCOMING_TYPE;
			}
		}
		cv.put(CallLog.Calls.TYPE, type);
		cv.put(CallLog.Calls.NEW, nonAcknowledge);
		cv.put(CallLog.Calls.DURATION, (callStart>0)?(System.currentTimeMillis()-callStart)/1000:0);
		
		CallerInfo callerInfo = CallerInfo.getCallerInfoFromSipUri(context, remoteContact);
		if(callerInfo != null) {
			cv.put(CallLog.Calls.CACHED_NAME, callerInfo.name);
			cv.put(CallLog.Calls.CACHED_NUMBER_LABEL, callerInfo.numberLabel);
			cv.put(CallLog.Calls.CACHED_NUMBER_TYPE, callerInfo.numberType);
		}
		
		return cv;
	}
}
