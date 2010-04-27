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
package com.csipsimple.service;

import org.pjsip.pjsua.Callback;
import org.pjsip.pjsua.SWIGTYPE_p_p_pjmedia_port;
import org.pjsip.pjsua.SWIGTYPE_p_pjmedia_session;
import org.pjsip.pjsua.SWIGTYPE_p_pjsip_rx_data;
import org.pjsip.pjsua.pjsip_event;
import org.pjsip.pjsua.pjsip_inv_state;
import org.pjsip.pjsua.pjsip_status_code;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsua_acc_info;
import org.pjsip.pjsua.pjsua_call_info;
import org.pjsip.pjsua.pjsua_call_media_status;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.provider.Settings;

import com.csipsimple.R;
import com.csipsimple.models.CallInfo;
import com.csipsimple.ui.CallHandler;
import com.csipsimple.ui.SipHome;
import com.csipsimple.utils.Log;

public class UAStateReceiver extends Callback {

	static String THIS_FILE = "SIP UA Receiver";
	
	
	
	@Override
	public void on_incoming_call(int acc_id, int call_id, SWIGTYPE_p_pjsip_rx_data rdata) {
		Log.d(THIS_FILE, "Has incoming call "+call_id);
		/*pjsua_call_info info = new pjsua_call_info();
		pjsua.call_get_info(call_id, info);
		Log.i(THIS_FILE, "Has incoming call !!! "+info.getRemote_info().getPtr());
		*/
		final  int c_id = call_id;
		
		// Automatically answer incoming calls with 100/RINGING 
		mRingtone = RingtoneManager.getRingtone(service, Settings.System.DEFAULT_RINGTONE_URI);
        mRingtone.play();
        
        CallInfo incomingCall = new CallInfo(c_id);
        
		pjsua.call_answer(c_id, 180, null, null);
		
		if(auto_accept_current){
			// Automatically answer incoming calls with 200/OK 
			pjsua.call_answer(c_id, 200, null, null);
			auto_accept_current = false;
			showNotificationForCall(incomingCall);
		}else{
			showNotificationForCall(incomingCall);
			anounceCall(incomingCall);
		}
	}
	
	
	@Override
	public void on_call_state(int call_id, pjsip_event e) {
		
		CallInfo call_info = new CallInfo(call_id);
		Log.i(THIS_FILE, "State of call "+call_id+" :: "+call_info.getStringCallState());
		AudioManager am = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
		
		
		pjsip_inv_state call_state = call_info.getCallState();
		
		if( call_state .equals(pjsip_inv_state.PJSIP_INV_STATE_INCOMING) ||
				call_state.equals(pjsip_inv_state.PJSIP_INV_STATE_CALLING) ) {
			showNotificationForCall(call_info);
			anounceCall(call_info);
			
		}else if( call_state.equals(pjsip_inv_state.PJSIP_INV_STATE_EARLY) ){
			Log.d(THIS_FILE, "Early state");
		}else{
			Log.d(THIS_FILE, "Will stop ringing");
			if(mRingtone != null){
				mRingtone.stop();
			}
			//Call is now ended
			if(call_state.equals(pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)){
				mNotificationManager.cancel(CALL_NOTIF_ID);
				am.setMode(AudioManager.MODE_NORMAL);
			}
		}
		
		
		Intent callStateChangedIntent = new Intent(UA_CALL_STATE_CHANGED);
		callStateChangedIntent.putExtra("call_info", call_info);
		service.sendBroadcast(callStateChangedIntent);
		
	}
	
	

	@Override
	public void on_reg_state(int acc_id) {
		Log.d(THIS_FILE, "New reg state for : "+acc_id);
		pjsua_acc_info info = new pjsua_acc_info();
		pjsua.acc_get_info(acc_id, info);
		onRegisterState(info);
	}
	
	@Override
	public void on_stream_created(int call_id, SWIGTYPE_p_pjmedia_session sess, long stream_idx, SWIGTYPE_p_p_pjmedia_port p_port) {
		Log.d(THIS_FILE, "Stream created");
		
	}
	
	
	@Override
	public void on_call_media_state(int call_id) {
		pjsua_call_info info = new pjsua_call_info();
		pjsua.call_get_info(call_id, info);
		if (info.getMedia_status() == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE) {
			
			AudioManager am = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
		//	am.setSpeakerphoneOn(true);
			am.setMicrophoneMute(false);
			am.setMode(AudioManager.MODE_IN_CALL);
	
			
			//May be done under media thread instead of this one
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
			
			// When media is active, connect call to sound device.
			pjsua.conf_connect(info.getConf_slot(), 0);
			pjsua.conf_connect(0, info.getConf_slot());
			
			
		}else if(info.getMedia_status() == pjsua_call_media_status.PJSUA_CALL_MEDIA_NONE || 
				info.getMedia_status() == pjsua_call_media_status.PJSUA_CALL_MEDIA_ERROR){
		//	pjsua.call_hangup(call_id, 0, null, null);
		}
	}
	
	
	
	// -------
	// Static constants
	// -------

	public static String UA_CALL_STATE_CHANGED = "com.csipsimple.ua.CALL_STATE_CHANGED";
	public static String UA_REG_STATE_CHANGED = "com.csipsimple.ua.REG_STATE_CHANGED";
	
	
	// -------
	// Public configuration for receiver
	// -------
	public void setAutoAnswerNext(boolean auto_response){
		auto_accept_current = auto_response;
	}
	
	public void initService(Service srv){
		service = srv;
		mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	// --------
	// Private methods
	// --------
	
	private static final int REGISTER_NOTIF_ID = 1;
	private static final int CALL_NOTIF_ID = REGISTER_NOTIF_ID + 1;
	
	private boolean auto_accept_current = false;
	private Ringtone mRingtone;
	private NotificationManager mNotificationManager;
	private Service service;
	
	
	/**
	 * Register state for an account
	 * @param info
	 */
	private void onRegisterState(pjsua_acc_info info){
		//First of all send a broadcast message that for an account registration state has changed
		Intent regStateChangedIntent = new Intent(UA_REG_STATE_CHANGED);
		
		regStateChangedIntent.putExtra("acc_id",  info.getAcc_uri().getPtr());
		regStateChangedIntent.putExtra("acc_expires", info.getExpires());
		regStateChangedIntent.putExtra("acc_status", info.getStatus());
		
		
		//Send notification to broadcaster
		service.sendBroadcast(regStateChangedIntent);
		
		//Handle status bar notification
		if(info.getExpires() > 0 && info.getStatus() == pjsip_status_code.PJSIP_SC_OK){
			int icon = R.drawable.sipok;
			CharSequence tickerText = "Sip Registred";
			long when = System.currentTimeMillis();
			
			Notification notification = new Notification(icon, tickerText, when);
			Context context = service.getApplicationContext();
			CharSequence contentTitle = "SIP";
			CharSequence contentText = "Registred";
			
			Intent notificationIntent = new Intent(service, SipHome.class);
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent contentIntent = PendingIntent.getActivity(service, 0, notificationIntent, 0);

			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
			notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
			//notification.flags = Notification.FLAG_FOREGROUND_SERVICE;
			
			mNotificationManager.notify(REGISTER_NOTIF_ID, notification);
			((SipService) service).lockResources();
		}else{
			mNotificationManager.cancel(REGISTER_NOTIF_ID);
		}
	}
	
	public void forceDeleteNotifications(){
		if(mNotificationManager != null) {
			mNotificationManager.cancel(REGISTER_NOTIF_ID);
		}
	}
	
	
	private void showNotificationForCall(CallInfo call_info){
		 //This is the pending call notification
        int icon = R.drawable.ic_incall_ongoing;
		CharSequence tickerText = "Ongoing call";
		long when = System.currentTimeMillis();
		
		Notification notification = new Notification(icon, tickerText, when);
		Context context = service.getApplicationContext();
        
        Intent notificationIntent = new Intent(service, CallHandler.class);
        notificationIntent.putExtra("call_info", call_info);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(service, 0, notificationIntent, 0);
		

		notification.setLatestEventInfo(context, "Ongoing Call", "There is a current call", contentIntent);
		notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		//notification.flags = Notification.FLAG_FOREGROUND_SERVICE;
		
		mNotificationManager.notify(CALL_NOTIF_ID, notification);
	}
	
	private void anounceCall(CallInfo call_info){
		//TODO : manage Vibrate / sound on/off
		Log.i(THIS_FILE, "Anounce call");
        
		
        // Launch activity to choose what to do with this call
        Intent callHandlerIntent = new Intent(service, CallHandler.class);
        callHandlerIntent.putExtra("call_info", call_info);
        callHandlerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        Log.i(THIS_FILE, "Anounce call activity please");
        service.startActivity(callHandlerIntent);
        
	}
	



}
