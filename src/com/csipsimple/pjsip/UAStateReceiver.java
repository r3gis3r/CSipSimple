/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2010 Chris McCormick (aka mccormix - chris@mccormick.cx) 
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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;

import com.csipsimple.R;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipMessage;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipProfileState;
import com.csipsimple.api.SipUri;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.service.SipNotifications;
import com.csipsimple.service.SipService.SameThreadException;
import com.csipsimple.service.SipService.SipRunnable;
import com.csipsimple.utils.CallLogHelper;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.Threading;
import com.csipsimple.utils.TimerWrapper;

import org.pjsip.pjsua.Callback;
import org.pjsip.pjsua.SWIGTYPE_p_pjsip_rx_data;
import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pj_stun_nat_detect_result;
import org.pjsip.pjsua.pjsip_event;
import org.pjsip.pjsua.pjsip_redirect_op;
import org.pjsip.pjsua.pjsip_status_code;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UAStateReceiver extends Callback {
	private final static String THIS_FILE = "SIP UA Receiver";
	private final static String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
	

	private SipNotifications notificationManager;
	private PjSipService pjService;
//	private ComponentName remoteControlResponder;

	// Time in ms during which we should not relaunch call activity again
	final static long LAUNCH_TRIGGER_DELAY = 2000;
	private long lastLaunchCallHandler = 0;
	
	
	int eventLockCount = 0;
	private void lockCpu(){
		if(eventLock != null) {
			Log.d(THIS_FILE, "< LOCK CPU");
			eventLock.acquire();
			eventLockCount ++;
		}
	}
	private void unlockCpu() {
		if(eventLock != null && eventLock.isHeld()) {
			eventLock.release();
			eventLockCount --;
			Log.d(THIS_FILE, "> UNLOCK CPU " + eventLockCount);
		}
	}
	/*
	private class IncomingCallInfos {
		public SipCallSession callInfo;
		public Integer accId;
	}
*/	
	@Override
	public void on_incoming_call(final int accId, final int callId, SWIGTYPE_p_pjsip_rx_data rdata) {
		lockCpu();
		
		//Check if we have not already an ongoing call
		if(pjService != null && pjService.service != null && !pjService.service.supportMultipleCalls) {
			SipCallSession[] calls = getCalls();
			if(calls != null) {
				for( SipCallSession existingCall : calls ) {
					if(!existingCall.isAfterEnded() && existingCall.getCallId() != callId) {
						Log.e(THIS_FILE, "Settings to not support two call at the same time !!!");
						//If there is an ongoing call and we do not support multiple calls
						//Send busy here
						pjsua.call_hangup(callId, 486, null, null);
						unlockCpu();
						return;
					}
				}
			}
		}
		
		try {
            SipCallSession callInfo = updateCallInfoFromStack(callId);
			Log.d(THIS_FILE, "Incoming call << for account " + accId);
            
            //Extra check if set reference counted is false ???
            if(!incomingCallLock.isHeld()) {
                incomingCallLock.acquire();
            }
            
            final String remContact = callInfo.getRemoteContact();
            callInfo.setIncoming(true);
            notificationManager.showNotificationForCall(callInfo);

            //Auto answer feature
            SipProfile acc = pjService.getAccountForPjsipId(accId);
            final boolean shouldAutoAnswer = pjService.service.shouldAutoAnswer(remContact, acc);
            Log.d(THIS_FILE, "Should I anto answer ? " + shouldAutoAnswer);
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
            //Or by api
            launchCallHandler(callInfo);
			
			Log.d(THIS_FILE, "Incoming call >>");
        } catch (SameThreadException e) {
            // That's fine we are in a pjsip thread
        }finally {
            unlockCpu();
        }
		
	}
	
	
	@Override
	public void on_call_state(final int callId, pjsip_event e) {
		lockCpu();
		
		Log.d(THIS_FILE, "Call state <<");
		try {
    		//Get current infos now on same thread cause fix has been done on pj
            final SipCallSession callInfo = updateCallInfoFromStack(callId);
			int callState = callInfo.getCallState();
			
			// If disconnected immediate stop required stuffs
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
				pjService.stopRecording();
			}
			
			msgHandler.sendMessage(msgHandler.obtainMessage(ON_CALL_STATE, callInfo));
			Log.d(THIS_FILE, "Call state >>");
		} catch(SameThreadException ex) {
            // We don't care about that we are at least in a pjsua thread
        } finally {
            // Unlock CPU anyway
            unlockCpu();
        }
		
	}

	@Override
	public void on_buddy_state(int buddyId) {
		lockCpu();
		Log.d(THIS_FILE, "On buddy state");
		// buddy_info = pjsua.buddy_get_info(buddy_id, new pjsua_buddy_info());
		
		unlockCpu();
	}

	@Override
	public void on_pager(int callId, pj_str_t from, pj_str_t to, pj_str_t contact, pj_str_t mime_type, pj_str_t body) {
		lockCpu();
		
		long date = System.currentTimeMillis();
		String fromStr = PjSipService.pjStrToString(from);
		String canonicFromStr = SipUri.getCanonicalSipContact(fromStr);
		String contactStr = PjSipService.pjStrToString(contact);
		String toStr = PjSipService.pjStrToString(to);
		String bodyStr = PjSipService.pjStrToString(body);
		String mimeStr = PjSipService.pjStrToString(mime_type);
		
		SipMessage msg = new SipMessage(canonicFromStr, toStr, contactStr, bodyStr, mimeStr, 
				date, SipMessage.MESSAGE_TYPE_INBOX, fromStr);
		
		//Insert the message to the DB
		ContentResolver cr = pjService.service.getContentResolver();
		cr.insert(SipMessage.MESSAGE_URI, msg.getContentValues());
		
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
	public void on_pager_status(int callId, pj_str_t to, pj_str_t body, pjsip_status_code status, pj_str_t reason) {
		lockCpu();
		//TODO : treat error / acknowledge of messages
		int messageType = (status.equals(pjsip_status_code.PJSIP_SC_OK) 
				|| status.equals(pjsip_status_code.PJSIP_SC_ACCEPTED))? SipMessage.MESSAGE_TYPE_SENT : SipMessage.MESSAGE_TYPE_FAILED;
		String sTo = SipUri.getCanonicalSipContact(PjSipService.pjStrToString(to));

		String reasonStr = PjSipService.pjStrToString(reason);
		Log.d(THIS_FILE, "SipMessage in on pager status "+status.toString()+" / "+reasonStr);
		
		//Update the db
        ContentResolver cr = pjService.service.getContentResolver();
        int sStatus = status.swigValue();
		ContentValues args = new ContentValues();
        args.put(SipMessage.FIELD_TYPE, messageType);
        args.put(SipMessage.FIELD_STATUS, sStatus);
        if(sStatus != SipCallSession.StatusCode.OK 
            && sStatus != SipCallSession.StatusCode.ACCEPTED ) {
            args.put(SipMessage.FIELD_BODY, body + " // " + reasonStr);
        }
        cr.update(SipMessage.MESSAGE_URI, args,
                SipMessage.FIELD_TO + "=? AND "+
                SipMessage.FIELD_BODY+ "=? AND "+
                SipMessage.FIELD_TYPE+ "="+SipMessage.MESSAGE_TYPE_QUEUED, 
                new String[] {sTo, PjSipService.pjStrToString(body)});
        
		//Broadcast the information
		Intent intent = new Intent(SipManager.ACTION_SIP_MESSAGE_RECEIVED);
		intent.putExtra(SipMessage.FIELD_FROM, sTo);
		pjService.service.sendBroadcast(intent);
		unlockCpu();
	}

	
	private List<Integer> pendingCleanup = new ArrayList<Integer>();
	
	@Override
	public void on_reg_state(final int accountId) {
		lockCpu();
		pjService.service.getExecutor().execute(new SipRunnable() {
			@Override
			public void doRun() throws SameThreadException {
				// Update java infos
				Log.d(THIS_FILE, "New reg state for : " + accountId);
				pjService.updateProfileStateFromService(accountId);
				
				// Dispatch to UA handler thread
				if(msgHandler != null) {
					msgHandler.sendMessage(msgHandler.obtainMessage(ON_REGISTRATION_STATE, accountId));
				}
				
				
				//Try to recover registration many other clients (or self) also registered
				SipProfile account = pjService.getAccountForPjsipId(accountId);
				if(account != null && account.try_clean_registers != 0 && account.active) {
					SipProfileState pState = pjService.getProfileState(account);
					if(pState != null) {
						Log.d(THIS_FILE, "We have a new status "+
								pState.getStatusCode()+ " "+
								pendingCleanup.contains(accountId)+" "+
								pState.getExpires());
						
						// Failure on registration
						// TODO : refine cases ? 403 only? 
						if(pState.getStatusCode() > 200
								&& !pendingCleanup.contains(accountId) ) {
							Log.w(THIS_FILE, "Error while registering for "+accountId+" "+
										pState.getStatusCode()+" "+pState.getStatusText());
							
							
							int state = pjsua.acc_clean_all_registrations(accountId);
							if(state == pjsuaConstants.PJ_SUCCESS) {
								pendingCleanup.add(accountId);
							}
						// Success on clean up
						}else if(pState.getStatusCode() == 200 && 
								pState.getExpires() == -1 &&
								pendingCleanup.contains(accountId)) {
							
							int state = pjsua.acc_set_registration(accountId, 1);
							if(state == pjsuaConstants.PJ_SUCCESS) {
								pendingCleanup.remove((Object) accountId);
							}else {
								Log.e(THIS_FILE, "Impossible to set again registration now " + state);
							}
							
						// Success
						}else if(pState.getStatusCode() == 200) {
							pendingCleanup.remove((Object) accountId);
						}
						Log.d(THIS_FILE, "pending clean ups are  " + pendingCleanup);
					}
				
				}
			}
		});
		unlockCpu();
	}


	@Override
	public void on_call_media_state(final int callId) {
		lockCpu();
		
		
		if(pjService.mediaManager != null) {
			pjService.mediaManager.stopRing();
		}
		if(incomingCallLock != null && incomingCallLock.isHeld()) {
			incomingCallLock.release();
        }

        try {
            final SipCallSession callInfo = updateCallInfoFromStack(callId);

            if (callInfo.getMediaStatus() == SipCallSession.MediaState.ACTIVE) {
                pjsua.conf_connect(callInfo.getConfPort(), 0);
                pjsua.conf_connect(0, callInfo.getConfPort());

                // Adjust software volume
                if (pjService.mediaManager != null) {
                    pjService.mediaManager.setSoftwareVolume();
                }
                // Useless with Peter's patch
                // pjsua.set_ec(
                // pjService.prefsWrapper.getEchoCancellationTail(),
                // pjService.prefsWrapper.getEchoMode());

                // Auto record
                if (pjService.recordedCall == PjSipService.INVALID_RECORD
                        &&
                        pjService.prefsWrapper
                                .getPreferenceBooleanValue(SipConfigManager.AUTO_RECORD_CALLS)) {
                    pjService.startRecording(callId);
                }

            }

            msgHandler.sendMessage(msgHandler.obtainMessage(ON_MEDIA_STATE, callInfo));
        } catch (SameThreadException e) {
            // Nothing to do we are in a pj thread here
        }
		
		unlockCpu();
	}
	
	@Override
	public void on_mwi_info(int acc_id, pj_str_t mime_type, pj_str_t body) {
		lockCpu();
		//Treat incoming voice mail notification.
		
		String msg = PjSipService.pjStrToString(body);
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
			}
			Log.d(THIS_FILE, "We can show the voice messages notification");
			notificationManager.showNotificationForVoiceMail(acc, numberOfMessages, voiceMailNumber);
		}
		unlockCpu();
	}
	
	//public String sasString = "";
	//public boolean zrtpOn = false;
	
	@Override
	public void on_zrtp_show_sas(int dataPtr, pj_str_t sas, int verified) {
		String sasString = PjSipService.pjStrToString(sas);
		Log.d(THIS_FILE, "ZRTP show SAS " + sasString + " verified : " + verified);
		if(verified != 1) {
			Intent zrtpIntent = new Intent(SipManager.ACTION_ZRTP_SHOW_SAS);
			zrtpIntent.putExtra(Intent.EXTRA_SUBJECT, sasString);
			zrtpIntent.putExtra(Intent.EXTRA_UID, dataPtr);
			pjService.service.sendBroadcast(zrtpIntent);
		}else{
			updateZrtpInfos(dataPtr);
		}
	}
	

	@Override
	public void on_zrtp_update_transport(int dataPtr) {
		updateZrtpInfos(dataPtr);
	}

	public void updateZrtpInfos(int dataPtr) {
		final int callId = pjsua.jzrtp_getCallId(dataPtr);
		pjService.service.getExecutor().execute(new SipRunnable() {
			@Override
			public void doRun() throws SameThreadException {
				SipCallSession callInfo = updateCallInfoFromStack(callId);
				msgHandler.sendMessage(msgHandler.obtainMessage(ON_MEDIA_STATE, callInfo));
			}
		});
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
	
	@Override
	public pjsip_redirect_op on_call_redirected(int call_id, pj_str_t target) {
		Log.w(THIS_FILE, "Ask for redirection, not yet implemented, for now allow all "+ PjSipService.pjStrToString(target));
		return pjsip_redirect_op.PJSIP_REDIRECT_ACCEPT;
	}
	
	@Override
	public void on_nat_detect(pj_stun_nat_detect_result res) {
		//TODO : IMPLEMENT THIS FEATURE 
		Log.d(THIS_FILE, "NAT TYPE DETECTED !!!" + res.getNat_type_name()+ " et "+res.getStatus());
	}
	
	@Override
	public int on_set_micro_source() {
		return pjService.prefsWrapper.getPreferenceIntegerValue(SipConfigManager.MICRO_SOURCE);
	}
	
	@Override
	public int timer_schedule(int entry, int entryId, int time) {
		return TimerWrapper.schedule(entry, entryId, time);
	}
	
	@Override
	public int timer_cancel(int entry, int entryId) {
		return TimerWrapper.cancel(entry, entryId);
	}
	
	// -------
	// Current call management -- assume for now one unique call is managed
	// -------
	private HashMap<Integer, SipCallSession> callsList = new HashMap<Integer, SipCallSession>();
	//private long currentCallStart = 0;
	
	private SipCallSession updateCallInfoFromStack(Integer callId) throws SameThreadException {
		SipCallSession callInfo;
		Log.d(THIS_FILE, "Get call info for update");
		synchronized (callsList) {
			callInfo = callsList.get(callId);
			if(callInfo == null) {
				callInfo = new SipCallSession();
				callInfo.setCallId(callId);
			}
		}
		Log.d(THIS_FILE, "Launch update");
		// We update session infos. callInfo is both in/out and will be updated
		PjSipCalls.updateSessionFromPj(callInfo, pjService);
		synchronized (callsList) {
    		// Re-add to list mainly for case newly added session
    		callsList.put(callId, callInfo);
		}
		return callInfo;
	}
	
	public SipCallSession getCallInfo(Integer callId) {
		Log.d(THIS_FILE, "Get call info");
		SipCallSession callInfo;
		synchronized (callsList) {
			callInfo = callsList.get(callId);
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
		return new SipCallSession[0];
	}

	

	private WorkerHandler msgHandler;
	private HandlerThread handlerThread;
	private WakeLock incomingCallLock;

	private WakeLock eventLock;


	//private static final int ON_INCOMING_CALL = 1;
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
			lockCpu();
			switch (msg.what) {
			    /*
			case ON_INCOMING_CALL:{
				
				
				break;
			}
*/			
			case ON_CALL_STATE:{
				SipCallSession callInfo = (SipCallSession) msg.obj;
				final int callState = callInfo.getCallState();
				
				switch (callState) {
				case SipCallSession.InvState.INCOMING:
				case SipCallSession.InvState.CALLING:
					notificationManager.showNotificationForCall(callInfo);
					launchCallHandler(callInfo);
					broadCastAndroidCallState("RINGING", callInfo.getRemoteContact());
					break;
				case SipCallSession.InvState.EARLY:
				case SipCallSession.InvState.CONNECTING :
				case SipCallSession.InvState.CONFIRMED:
					// As per issue #857 we should re-ensure notification + callHandler at each state
					// cause we can miss some states due to the fact treatment of call state is threaded
					// Anyway if we miss the call early + confirmed we do not need to show the UI.
					notificationManager.showNotificationForCall(callInfo);
					launchCallHandler(callInfo);
					broadCastAndroidCallState("OFFHOOK", callInfo.getRemoteContact());
					
					
					if(pjService.mediaManager != null) {
						if(callState == SipCallSession.InvState.CONFIRMED) {
							pjService.mediaManager.stopRing();
						}
					}
					if(incomingCallLock != null && incomingCallLock.isHeld()) {
						incomingCallLock.release();
					}
					
					// If state is confirmed and not already intialized
					if(callState == SipCallSession.InvState.CONFIRMED && callInfo.callStart == 0) {
						callInfo.callStart = System.currentTimeMillis();
					}
					break;
				case SipCallSession.InvState.DISCONNECTED:
					if(pjService.mediaManager != null) {
						pjService.mediaManager.stopRing();
					}
					if(incomingCallLock != null && incomingCallLock.isHeld()) {
						incomingCallLock.release();
					}
					
					Log.d(THIS_FILE, "Finish call2");
					
					//CallLog
					ContentValues cv = CallLogHelper.logValuesForCall(pjService.service, callInfo, callInfo.callStart);
					
					//Fill our own database
					pjService.service.getContentResolver().insert(SipManager.CALLLOG_URI, cv);
					Integer isNew = cv.getAsInteger(CallLog.Calls.NEW);
					if(isNew != null && isNew == 1) {
						notificationManager.showNotificationForMissedCall(cv);
					}
					
					//If needed fill native database
					if(pjService.prefsWrapper.getPreferenceBooleanValue(SipConfigManager.INTEGRATE_WITH_CALLLOGS)) {
						//Don't add with new flag
						cv.put(CallLog.Calls.NEW, false);
						// Remove csipsimple custom entries
						cv.remove(SipManager.CALLLOG_PROFILE_ID_FIELD);
						cv.remove(SipManager.CALLLOG_STATUS_CODE_FIELD);
                        cv.remove(SipManager.CALLLOG_STATUS_TEXT_FIELD);
						
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
							// TODO : if android 2.3 add sip uri also
							if(phoneNumber != null) {
								cv.put(Calls.NUMBER, phoneNumber);
								// For log in call logs => don't add as new calls... we manage it ourselves.
								cv.put(Calls.NEW, false);
								ContentValues extraCv = new ContentValues();
								
								if(callInfo.getAccId() != SipProfile.INVALID_ID) {
									SipProfile acc = pjService.service.getAccount(callInfo.getAccId());
									if(acc != null && acc.display_name != null) {
										extraCv.put(CallLogHelper.EXTRA_SIP_PROVIDER, acc.display_name);
									}
								}
								CallLogHelper.addCallLog(pjService.service, cv, extraCv);
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
								pjService.service.cleanStop();
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
			unlockCpu();
		}
	};
	
	
	
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
			eventLock.setReferenceCounted(true);

		}
		if(incomingCallLock == null) {
            PowerManager pman = (PowerManager) pjService.service.getSystemService(Context.POWER_SERVICE);
            incomingCallLock = pman.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.csipsimple.incomingCallLock");
            incomingCallLock.setReferenceCounted(false);
		}
	}
	

	public void stopService() {

		Threading.stopHandlerThread(handlerThread, true);
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
		long currentElapsedTime = SystemClock.elapsedRealtime();
		
		// Synchronized ensure we do not get this launched several time
		// We also ensure that a minimum delay has been consumed so that we do not fire this too much times
		// Specially for EARLY - CONNECTING states 
		if(lastLaunchCallHandler + LAUNCH_TRIGGER_DELAY < currentElapsedTime) {
			
			// Launch activity to choose what to do with this call
			Intent callHandlerIntent = new Intent(SipManager.ACTION_SIP_CALL_UI);
			callHandlerIntent.putExtra(SipManager.EXTRA_CALL_INFO, currentCallInfo2);
			callHandlerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP  );
			
			Log.d(THIS_FILE, "Anounce call activity");
			pjService.service.startActivity(callHandlerIntent);
			lastLaunchCallHandler = currentElapsedTime;
		}else {
			Log.d(THIS_FILE, "Ignore extra launch handler");
		}
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
			SipCallSession callInfo = getCallInfo(i);
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
		final SipCallSession callInfo = getActiveCallInProgress();
		if (callInfo != null) {
			// Headset button has been pressed by user. If there is an
			// incoming call ringing the button will be used to answer the
			// call. If there is an ongoing call in progress the button will
			// be used to hangup the call or mute the microphone.
    		int state = callInfo.getCallState();
    		if (callInfo.isIncoming() && 
    				(state == SipCallSession.InvState.INCOMING || 
    				state == SipCallSession.InvState.EARLY)) {
    			if(pjService != null && pjService.service != null ) {
    				pjService.service.getExecutor().execute(new SipRunnable() {
						@Override
						protected void doRun() throws SameThreadException {
							
							pjService.callAnswer(callInfo.getCallId(), pjsip_status_code.PJSIP_SC_OK.swigValue());
						}
					});
    			}
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
    			if(pjService != null && pjService.service != null ) {
    				pjService.service.getExecutor().execute(new SipRunnable() {
						@Override
						protected void doRun() throws SameThreadException {
							int preferedAction = pjService.prefsWrapper.getHeadsetAction();
							if (preferedAction == PreferencesProviderWrapper.HEADSET_ACTION_CLEAR_CALL) {
								pjService.callHangup(callInfo.getCallId(), 0);
							} else if (preferedAction == PreferencesProviderWrapper.HEADSET_ACTION_MUTE) {
								pjService.mediaManager.toggleMute();
							}
						}
	    			});
    			}
				return true;
    		}
		}
		return false;
	}
	
}
