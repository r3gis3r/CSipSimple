/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2010 Chris McCormick (aka mccormix - chris@mccormick.cx) 
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

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pjsip.pjsua.Callback;
import org.pjsip.pjsua.SWIGTYPE_p_p_pjmedia_port;
import org.pjsip.pjsua.SWIGTYPE_p_pjmedia_session;
import org.pjsip.pjsua.SWIGTYPE_p_pjsip_rx_data;
import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjsip_event;
import org.pjsip.pjsua.pjsip_status_code;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;

import com.csipsimple.R;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.SipMessage;
import com.csipsimple.service.SipNotifications;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.CallLogHelper;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.utils.Threading;

public class UAStateReceiver extends Callback {
	static String THIS_FILE = "SIP UA Receiver";

	final static String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
	

	private SipNotifications notificationManager;
	private PjSipService pjService;
//	private ComponentName remoteControlResponder;

	private void lockCpu(){
		if(eventLock != null) {
			Log.d(THIS_FILE, "< LOCK CPU");
			eventLock.acquire();
		}
	}
	private void unlockCpu() {
		if(eventLock != null && eventLock.isHeld()) {
			Log.d(THIS_FILE, "> UNLOCK CPU");
			eventLock.release();
		}
	}
	
	
	@Override
	public void on_incoming_call(final int acc_id, final int callId, SWIGTYPE_p_pjsip_rx_data rdata) {

		
		Thread t = new Thread() {
			public void run() {
				lockCpu();
				
				//Check if we have not already an ongoing call
				/*
				SipCallSession existingOngoingCall = getActiveCallInProgress();
				if(existingOngoingCall != null) {
					if(existingOngoingCall.getCallState() == SipCallSession.InvState.CONFIRMED) {
						Log.e(THIS_FILE, "For now we do not support two call at the same time !!!");
						//If there is an ongoing call... For now decline TODO : should here manage multiple calls
						//Send busy here
						pjsua.call_hangup(callId, 486, null, null);
						unlockCpu();
						return;
					}
				}
				*/
				
				
				SipCallSession callInfo = getCallInfo(callId, true);
				Log.d(THIS_FILE, "Incoming call <<");
				treatIncomingCall(acc_id, callInfo);
				msgHandler.sendMessage(msgHandler.obtainMessage(ON_INCOMING_CALL, callInfo));
				Log.d(THIS_FILE, "Incoming call >>");
				unlockCpu();
			};
		};
		t.start();
		
	}
	
	
	@Override
	public void on_call_state(final int callId, pjsip_event e) {
		
		Thread t = new Thread() {
			public void run() {
				lockCpu();
				Log.d(THIS_FILE, "Call state <<");
				//Get current infos
				SipCallSession callInfo = getCallInfo(callId, true);
				int callState = callInfo.getCallState();
				if (callState == SipCallSession.InvState.DISCONNECTED) {
					if(pjService.mediaManager != null) {
						pjService.mediaManager.stopAnnoucing();
						pjService.mediaManager.resetSettings();
					}
					if(incomingCallLock != null && incomingCallLock.isHeld()) {
						incomingCallLock.release();
					}
					// Call is now ended
					pjService.stopDialtoneGenerator();
					//TODO : should be stopped only if it's the current call.
					stopRecording();
				}
				msgHandler.sendMessage(msgHandler.obtainMessage(ON_CALL_STATE, callInfo));
				Log.d(THIS_FILE, "Call state >>");
				unlockCpu();
			}
		};
		t.start();
	}

	@Override
	public void on_buddy_state(int buddy_id) {
		lockCpu();
		Log.d(THIS_FILE, "On buddy state");
		// buddy_info = pjsua.buddy_get_info(buddy_id, new pjsua_buddy_info());
		
		unlockCpu();
	}

	@Override
	public void on_pager(int call_id, pj_str_t from, pj_str_t to, pj_str_t contact, pj_str_t mime_type, pj_str_t body) {
		lockCpu();
		
		long date = System.currentTimeMillis();
		String sFrom = SipUri.getCanonicalSipContact(from.getPtr());
		
		String contactString = "";
		if(contact != null && contact.getSlen() > 0) {
			Log.d(THIS_FILE, "Contact is present");
			contactString = contact.getPtr();
		}else {
			Log.d(THIS_FILE, "EMPTY CONTACT !!!");
		}
		
		SipMessage msg = new SipMessage(sFrom, to.getPtr(), contactString, body.getPtr(), mime_type.getPtr(), date, SipMessage.MESSAGE_TYPE_INBOX);
		
		//Insert the message to the DB 
		DBAdapter database = new DBAdapter(pjService.service);
		database.open();
		database.insertMessage(msg);
		database.close();
		
		//Broadcast the message
		Intent intent = new Intent(SipManager.ACTION_SIP_MESSAGE_RECEIVED);
		//TODO : could be parcelable !
		intent.putExtra(SipMessage.FIELD_FROM, msg.getFrom());
		intent.putExtra(SipMessage.FIELD_BODY, msg.getBody());
		pjService.service.sendBroadcast(intent);
		
		//Notify android os of the new message
		notificationManager.showNotificationForMessage(msg);
		unlockCpu();
	}

	@Override
	public void on_pager_status(int call_id, pj_str_t to, pj_str_t body, pjsip_status_code status, pj_str_t reason) {
		lockCpu();
		//TODO : treat error / acknowledge of messages
		int messageType = (status.equals(pjsip_status_code.PJSIP_SC_OK) 
				|| status.equals(pjsip_status_code.PJSIP_SC_ACCEPTED))? SipMessage.MESSAGE_TYPE_SENT : SipMessage.MESSAGE_TYPE_FAILED;
		String sTo = SipUri.getCanonicalSipContact(to.getPtr());

		Log.d(THIS_FILE, "SipMessage in on pager status "+status.toString()+" / "+reason.getPtr());
		
		//Update the db
		DBAdapter database = new DBAdapter(pjService.service);
		database.open();
		database.updateMessageStatus(sTo, body.getPtr(), messageType, status.swigValue(), reason.getPtr());
		database.close();
		
		//Broadcast the information
		Intent intent = new Intent(SipManager.ACTION_SIP_MESSAGE_RECEIVED);
		intent.putExtra(SipMessage.FIELD_FROM, sTo);
		pjService.service.sendBroadcast(intent);
		unlockCpu();
	}

	@Override
	public void on_reg_state(final int accountId) {
		
		Thread t = new Thread() {
			public void run() {
				lockCpu();
				Log.d(THIS_FILE, "New reg state for : " + accountId);
				if(msgHandler != null) {
					msgHandler.sendMessage(msgHandler.obtainMessage(ON_REGISTRATION_STATE, accountId));
				}
				unlockCpu();
			}
		};
		t.start();
	}

	@Override
	public void on_stream_created(int call_id, SWIGTYPE_p_pjmedia_session sess, long stream_idx, SWIGTYPE_p_p_pjmedia_port p_port) {
		lockCpu();
		Log.d(THIS_FILE, "Stream created");
		unlockCpu();
	}
	
	@Override
	public void on_stream_destroyed(int callId, SWIGTYPE_p_pjmedia_session sess, long streamIdx) {
		lockCpu();
		Log.d(THIS_FILE, "Stream destroyed");
		unlockCpu();
	}

	@Override
	public void on_call_media_state(final int callId) {
		
		Thread t = new Thread() {
			public void run() {
				lockCpu();
				if(pjService.mediaManager != null) {
					pjService.mediaManager.stopRing();
				}
				if(incomingCallLock != null && incomingCallLock.isHeld()) {
					incomingCallLock.release();
				}
				
				SipCallSession callInfo = getCallInfo(callId, true);
				
				if (callInfo.getMediaStatus() == SipCallSession.MediaState.ACTIVE) {
					pjsua.conf_connect(callInfo.getConfPort(), 0);
					pjsua.conf_connect(0, callInfo.getConfPort());
					
					// Adjust software volume
					if(pjService.mediaManager != null) {
						pjService.mediaManager.setSoftwareVolume();
					}
					
					pjsua.set_ec( pjService.prefsWrapper.getEchoCancellationTail(), pjService.prefsWrapper.getEchoMode());
					
					// Auto record
					if (recordedCall == INVALID_RECORD && 
							pjService.prefsWrapper.getPreferenceBooleanValue(SipConfigManager.AUTO_RECORD_CALLS)) {
						startRecording(callId);
					}
					
				}
				
				msgHandler.sendMessage(msgHandler.obtainMessage(ON_MEDIA_STATE, callInfo));
				unlockCpu();
			}
		};
		t.start();
	}
	
	@Override
	public void on_mwi_info(int acc_id, pj_str_t mime_type, pj_str_t body) {
		lockCpu();
		//Treat incoming voice mail notification.
		
		String msg = body.getPtr();
		//Log.d(THIS_FILE, "We have a message :: " + acc_id + " | " + mime_type.getPtr() + " | " + body.getPtr());
		
		boolean hasMessage = false;
		int numberOfMessages = 0;
		String voiceMailNumber = "";
		
		String lines[] = msg.split("\\r?\\n");
		// Decapsulate the application/simple-message-summary
		// TODO : should we check mime-type?
		// rfc3842
		Pattern messWaitingPattern = Pattern.compile(".*Messages-Waiting[ \t]?:[ \t]?(yes|no).*", Pattern.CASE_INSENSITIVE); 
		Pattern messAccountPattern = Pattern.compile(".*Message-Account[ \t]?:[ \t]?(.*)", Pattern.CASE_INSENSITIVE); 
		Pattern messVoiceNbrPattern = Pattern.compile(".*Voice-Message[ \t]?:[ \t]?([0-9]*)/[0-9]*.*", Pattern.CASE_INSENSITIVE); 
		
		
		for(String line : lines) {
			Matcher m;
			m = messWaitingPattern.matcher(line);
			if(m.matches()) {
				Log.w(THIS_FILE, "Matches : "+m.group(1));
				if("yes".equalsIgnoreCase(m.group(1))) {
					Log.d(THIS_FILE, "Hey there is messages !!! ");
					hasMessage = true;
					
				}
				continue;
			}
			m = messAccountPattern.matcher(line);
			if(m.matches()) {
				voiceMailNumber = m.group(1);
				Log.d(THIS_FILE, "VM acc : " + voiceMailNumber);
				continue;
			}
			m = messVoiceNbrPattern.matcher(line);
			if(m.matches()) {
				try {
					numberOfMessages = Integer.parseInt(m.group(1));
				}catch(NumberFormatException e) {
					Log.w(THIS_FILE, "Not well formated number "+m.group(1));
				}
				Log.d(THIS_FILE, "Nbr : "+numberOfMessages);
				continue;
			}
		}
		
		if(hasMessage && numberOfMessages > 0) {
			SipProfile acc = pjService.getAccountForPjsipId(acc_id);
			if(acc != null) {
				Log.d(THIS_FILE, acc_id+" -> Has found account "+acc.getDefaultDomain()+" "+ acc.id + " >> "+acc.getProfileName());
				notificationManager.showNotificationForVoiceMail(acc, numberOfMessages, voiceMailNumber);
			}
		}
		unlockCpu();
	}
	
	@Override
	public void on_zrtp_show_sas(pj_str_t sas, int verified) {
		String sasString = sas.getPtr();
		Log.d(THIS_FILE, "Hey hoy hay, we get the show SAS " + sasString);
		Intent zrtpIntent = new Intent("com.cipsimple.tmp.zrtp.showSAS");
		zrtpIntent.putExtra(Intent.EXTRA_SUBJECT, sasString);
		pjService.service.sendBroadcast(zrtpIntent);
		
	}
	
	
	@Override
	public int on_setup_audio(int clockRate) {
		if(pjService != null) {
			return pjService.setAudioInCall(clockRate);
		}
		return -1;
	}
	

	@Override
	public void on_teardown_audio() {
		if(pjService != null) {
			pjService.unsetAudioInCall();
		}
	}
	
	// -------
	// Current call management -- assume for now one unique call is managed
	// -------
	private HashMap<Integer, SipCallSession> callsList = new HashMap<Integer, SipCallSession>();
	//private long currentCallStart = 0;
	
	
	public SipCallSession getCallInfo(Integer callId, boolean update) {
		Log.d(THIS_FILE, "Get call info");
		SipCallSession callInfo;
		synchronized (callsList) {
			callInfo = callsList.get(callId);
			if(callInfo == null) {
				callInfo = PjSipCalls.getCallInfo(callId, pjService);
				callsList.put(callId, callInfo);
			} else {
				if(update) {
					Log.d(THIS_FILE, "UPDATE CALL INFOS !!!");
					PjSipCalls.updateSessionFromPj(callInfo, pjService);
				}
			}
		}
		return callInfo;
	}
	
	public SipCallSession[] getCalls() {
		if(callsList != null ) {
			
			SipCallSession[] callsInfos = new SipCallSession[callsList.size()];
			int i = 0;
			for( Entry<Integer, SipCallSession> entry : callsList.entrySet()) {
				callsInfos[i] = entry.getValue();
				i++;
			}
			return callsInfos;
		}
		return null;
	}

	

	private WorkerHandler msgHandler;
	private HandlerThread handlerThread;
	private WakeLock incomingCallLock;

	private WakeLock eventLock;

	private static final int ON_INCOMING_CALL = 1;
	private static final int ON_CALL_STATE = 2;
	private static final int ON_MEDIA_STATE = 3;
	private static final int ON_REGISTRATION_STATE = 4;
	private static final int ON_PAGER = 5;



    
	private class WorkerHandler extends Handler {

		public WorkerHandler(Looper looper) {
            super(looper);
			Log.d(THIS_FILE, "Create async worker !!!");
        }
			
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ON_INCOMING_CALL:{
				//CallInfo callInfo = (CallInfo) msg.obj;
				
				
				break;
			}
			
			case ON_CALL_STATE:{
				SipCallSession callInfo = (SipCallSession) msg.obj;
				int callState = callInfo.getCallState();
				
				
				
				switch (callState) {
				case SipCallSession.InvState.INCOMING:
				case SipCallSession.InvState.CALLING:
					notificationManager.showNotificationForCall(callInfo);
					launchCallHandler(callInfo);
					broadCastAndroidCallState("RINGING", callInfo.getRemoteContact());
					break;
				case SipCallSession.InvState.EARLY:
					broadCastAndroidCallState("OFFHOOK", callInfo.getRemoteContact());
					break;
				case SipCallSession.InvState.CONFIRMED:
					broadCastAndroidCallState("OFFHOOK", callInfo.getRemoteContact());
					callInfo.callStart = System.currentTimeMillis();
					break;
				case SipCallSession.InvState.DISCONNECTED:
					
					Log.d(THIS_FILE, "Finish call2");
					
					//CallLog
					ContentValues cv = CallLogHelper.logValuesForCall(pjService.service, callInfo, callInfo.callStart);
					
					//Fill our own database
					//TODO : raise in DB
					DBAdapter database = new DBAdapter(pjService.service);
					database.open();
					database.insertCallLog(cv);
					database.close();
					Integer isNew = cv.getAsInteger(CallLog.Calls.NEW);
					if(isNew != null && isNew == 1) {
						notificationManager.showNotificationForMissedCall(cv);
					}
					
					//If needed fill native database
					if(pjService.prefsWrapper.useIntegrateCallLogs()) {
						//Don't add with new flag
						cv.put(CallLog.Calls.NEW, false);
						
						//Reformat number for callogs
						ParsedSipContactInfos callerInfos = SipUri.parseSipContact(cv.getAsString(Calls.NUMBER));
						if (callerInfos != null) {
							String phoneNumber = null;
							if(SipUri.isPhoneNumber(callerInfos.displayName)) {
								phoneNumber = callerInfos.displayName;
							}else if(SipUri.isPhoneNumber(callerInfos.userName)) {
								phoneNumber = callerInfos.userName;
							}
							
							//Only log numbers that can be called by GSM too.
							if(phoneNumber != null) {
								cv.put(Calls.NUMBER, phoneNumber);
								// For log in call logs => don't add as new calls... we manage it ourselves.
								cv.put(Calls.NEW, false);
								CallLogHelper.addCallLog(pjService.service, cv);
							}
						}
					}
					callInfo.setIncoming(false);
					callInfo.callStart = 0;
					
					
					broadCastAndroidCallState("IDLE", callInfo.getRemoteContact());
					
					
					//If no remaining calls, cancel the notification
					if(getActiveCallInProgress() == null) {
						notificationManager.cancelCalls();
						// We should now ask parent to stop if needed
						if(pjService != null && pjService.service != null) {
							if( ! pjService.prefsWrapper.isValidConnectionForIncoming()) {
								pjService.service.stopSelf();
							}
						}
					}
					
					break;
				default:
					break;
				}
				onBroadcastCallState(callInfo);
				break;
			}
			case ON_MEDIA_STATE:{
				SipCallSession mediaCallInfo = (SipCallSession) msg.obj;
				SipCallSession callInfo = callsList.get(mediaCallInfo.getCallId());
				callInfo.setMediaStatus(mediaCallInfo.getMediaStatus());
				onBroadcastCallState(callInfo);
				break;
			}
			case ON_REGISTRATION_STATE:{
				Log.d(THIS_FILE, "In reg state");
				// Update sip pjService (for notifications
				((SipService) pjService.service).updateRegistrationsState();
				// Send a broadcast message that for an account
				// registration state has changed
				Intent regStateChangedIntent = new Intent(SipManager.ACTION_SIP_REGISTRATION_CHANGED);
				pjService.service.sendBroadcast(regStateChangedIntent);
				break;
			}
			case ON_PAGER: {
				//startSMSRing();
				//String message = (String) msg.obj;
				//pjService.showMessage(message);
				Log.e(THIS_FILE, "yana you in CASE ON_PAGER");
				//stopRing();
				break;
			}
			}
		}
	};
	
	
	private void treatIncomingCall(int accountId, SipCallSession callInfo) {
		int callId = callInfo.getCallId();
		

		
		//Get lock while ringing to be sure notification is well done !
		if (incomingCallLock == null) {
			PowerManager pman = (PowerManager) pjService.service.getSystemService(Context.POWER_SERVICE);
			incomingCallLock = pman.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.csipsimple.incomingCallLock");
			incomingCallLock.setReferenceCounted(false);
		}
		//Extra check if set reference counted is false ???
		if(!incomingCallLock.isHeld()) {
			incomingCallLock.acquire();
		}
		pjService.service.getSystemService(Context.POWER_SERVICE);
		
		String remContact = callInfo.getRemoteContact();
		callInfo.setIncoming(true);
		notificationManager.showNotificationForCall(callInfo);

		//Auto answer feature
		SipProfile acc = pjService.getAccountForPjsipId(accountId);
		boolean shouldAutoAnswer = pjService.service.shouldAutoAnswer(remContact, acc);
		Log.d(THIS_FILE, "Should I anto answer????"+shouldAutoAnswer);
		
		//Or by api
		if (shouldAutoAnswer) {
			// Automatically answer incoming calls with 200/OK
			pjService.callAnswer(callId, 200);
		} else {

			// Automatically answer incoming calls with 180/RINGING
			pjService.callAnswer(callId, 180);
			
			if(pjService.service.getGSMCallState() == TelephonyManager.CALL_STATE_IDLE) {
				if(pjService.mediaManager != null) {
					pjService.mediaManager.startRing(remContact);
				}
				broadCastAndroidCallState("RINGING", remContact);
			}
		}
		
		launchCallHandler(callInfo);
	}
	
	// -------
	// Public configuration for receiver
	// -------
	

	public void initService(PjSipService srv) {
		pjService = srv;
		notificationManager = pjService.service.notificationManager;
		
		if(handlerThread == null) {
			handlerThread = new HandlerThread("UAStateAsyncWorker");
			handlerThread.start();
		}
		if(msgHandler == null) {
			msgHandler = new WorkerHandler(handlerThread.getLooper());
		}
		
		if (eventLock == null) {
			PowerManager pman = (PowerManager) pjService.service.getSystemService(Context.POWER_SERVICE);
			eventLock = pman.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.csipsimple.inEventLock");
			eventLock.setReferenceCounted(false);

		}
	}
	

	public void stopService() {

		Threading.stopHandlerThread(handlerThread);
		handlerThread = null;
		msgHandler = null;
		
		//Ensure lock is released since this lock is a ref counted one.
		if( eventLock != null ) {
			while (eventLock.isHeld()) {
				eventLock.release();
			}
		}
	}

	// --------
	// Private methods
	// --------
	

	private void onBroadcastCallState(final SipCallSession callInfo) {
		//Internal event
		Intent callStateChangedIntent = new Intent(SipManager.ACTION_SIP_CALL_CHANGED);
		callStateChangedIntent.putExtra(SipManager.EXTRA_CALL_INFO, callInfo);
		pjService.service.sendBroadcast(callStateChangedIntent);
		
		
	}

	private void broadCastAndroidCallState(String state, String number) {
		//Android normalized event
		Intent intent = new Intent(ACTION_PHONE_STATE_CHANGED);
		intent.putExtra(TelephonyManager.EXTRA_STATE, state);
		if (number != null) {
			intent.putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER, number);
		}
		intent.putExtra(pjService.service.getString(R.string.app_name), true);
		pjService.service.sendBroadcast(intent, android.Manifest.permission.READ_PHONE_STATE);
	}
	
	/**
	 * 
	 * @param currentCallInfo2 
	 * @param callInfo
	 */
	private synchronized void launchCallHandler(SipCallSession currentCallInfo2) {
		
		// Launch activity to choose what to do with this call
		Intent callHandlerIntent = new Intent(SipManager.ACTION_SIP_CALL_UI); //new Intent(pjService, getInCallClass());
		callHandlerIntent.putExtra(SipManager.EXTRA_CALL_INFO, currentCallInfo2);
		callHandlerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP  );
		
		Log.d(THIS_FILE, "Anounce call activity");
		pjService.service.startActivity(callHandlerIntent);

	}

	
	/**
	 * Check if any of call infos indicate there is an active
	 * call in progress.
	 */
	public SipCallSession getActiveCallInProgress() {
		//Log.d(THIS_FILE, "isActiveCallInProgress(), number of calls: " + callsList.keySet().size());
		
		//
		// Go through the whole list of calls and check if
		// any call is in an active state.
		//
		for (Integer i : callsList.keySet()) { 
			SipCallSession callInfo = getCallInfo(i, false);
			if (callInfo.isActive()) {
				return callInfo;
			}
		}
		return null;
	}
	
	
	/**
	 * Broadcast the Headset button press event internally if
	 * there is any call in progress.
	 */
	public boolean handleHeadsetButton() {
		SipCallSession callInfo = getActiveCallInProgress();
		if (callInfo != null) {
			// Headset button has been pressed by user. If there is an
			// incoming call ringing the button will be used to answer the
			// call. If there is an ongoing call in progress the button will
			// be used to hangup the call or mute the microphone.
    		int state = callInfo.getCallState();
    		if (callInfo.isIncoming() && 
    				(state == SipCallSession.InvState.INCOMING || 
    				state == SipCallSession.InvState.EARLY)) {
    			pjService.callAnswer(callInfo.getCallId(), pjsip_status_code.PJSIP_SC_OK.swigValue());
    			return true;
    		}else if(state == SipCallSession.InvState.INCOMING || 
    				state == SipCallSession.InvState.EARLY ||
    				state == SipCallSession.InvState.CALLING ||
    				state == SipCallSession.InvState.CONFIRMED ||
    				state == SipCallSession.InvState.CONNECTING){
    			//
				// In the Android phone app using the media button during
				// a call mutes the microphone instead of terminating the call.
				// We check here if this should be the behavior here or if
				// the call should be cleared.
				//
				switch(pjService.prefsWrapper.getHeadsetAction()) {
				//TODO : add hold -
				case PreferencesWrapper.HEADSET_ACTION_CLEAR_CALL:
					pjService.callHangup(callInfo.getCallId(), 0);
					break;
				case PreferencesWrapper.HEADSET_ACTION_MUTE:
					pjService.mediaManager.toggleMute();
					break;
				}
				return true;
    		}
		}
		return false;
	}
	
	// Recorder
	private static int INVALID_RECORD = -1; 
	private int recordedCall = INVALID_RECORD;
	private int recPort = -1;
	private int recorderId = -1;
	private int recordedConfPort = -1;
	public void startRecording(int callId) {
		// Ensure nothing is recording actually
		if (recordedCall == INVALID_RECORD) {
			SipCallSession callInfo = getCallInfo(callId, false);
			if(callInfo == null || callInfo.getMediaStatus() != SipCallSession.MediaState.ACTIVE) {
				return;
			}
			
		    File mp3File = getRecordFile(callInfo.getRemoteContact());
		    if (mp3File != null){
				int[] recId = new int[1];
				pj_str_t filename = pjsua.pj_str_copy(mp3File.getAbsolutePath());
				int status = pjsua.recorder_create(filename, 0, (byte[]) null, 0, 0, recId);
				if(status == pjsuaConstants.PJ_SUCCESS) {
					recorderId = recId[0];
					Log.d(THIS_FILE, "Record started : " + recorderId);
					recordedConfPort = callInfo.getConfPort();
					recPort = pjsua.recorder_get_conf_port(recorderId);
					pjsua.conf_connect(recordedConfPort, recPort);
					pjsua.conf_connect(0, recPort);
					recordedCall = callId;
				}
		    }else {
		    	//TODO: toaster
		    	Log.w(THIS_FILE, "Impossible to write file");
		    }
		}
	}
	
	public void stopRecording() {
		Log.d(THIS_FILE, "Stop recording " + recordedCall+" et "+ recorderId);
		if (recorderId != -1) {
			pjsua.recorder_destroy(recorderId);
			recorderId = -1;
		}
		recordedCall = INVALID_RECORD;
	}
	
	public boolean canRecord(int callId) {
		if (recordedCall == INVALID_RECORD) {
			SipCallSession callInfo = getCallInfo(callId, false);
			if(callInfo == null || callInfo.getMediaStatus() != SipCallSession.MediaState.ACTIVE) {
				return false;
			}
			return true;
		}
		return false;
	}
	
	public int getRecordedCall() {
		return recordedCall; 
	}
	
	private File getRecordFile(String remoteContact) {
		File dir = PreferencesWrapper.getRecordsFolder();
	    if (dir != null){
			Date d = new Date();
			File file = new File(dir.getAbsoluteFile() + File.separator + sanitizeForFile(remoteContact)+ "_"+DateFormat.format("MM-dd-yy_kkmmss", d)+".wav");
			Log.d(THIS_FILE, "Out dir " + file.getAbsolutePath());
			return file;
	    }
	    return null;
	}


	private String sanitizeForFile(String remoteContact) {
		String fileName = remoteContact;
		fileName = fileName.replaceAll("[\\.\\\\<>:; \"\'\\*]", "_");
		return fileName;
	}
}
