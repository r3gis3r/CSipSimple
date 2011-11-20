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

import org.pjsip.pjsua.pjsua;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;

import com.csipsimple.service.SipService;
import com.csipsimple.service.SipWakeLock;

public class TimerWrapper extends BroadcastReceiver {
	
	private static final String THIS_FILE = "Timer wrap";
	
	private static final String TIMER_ACTION = "com.csipsimple.PJ_TIMER";
	private static final String EXTRA_TIMER_SCHEME = "timer";
	private SipService service;
	private AlarmManager alarmManager;
	private SipWakeLock wakeLock;

	//private WakeLock wakeLock;
	private static TimerWrapper singleton;
	
	boolean serviceRegistered = false;
	
	
	private TimerWrapper(SipService ctxt) {
		setContext(ctxt);
	}
	
	private synchronized void setContext(SipService ctxt) {
		// Reset --
		quit();
		// Set new service
		service = ctxt;
		alarmManager = (AlarmManager) service.getSystemService(Context.ALARM_SERVICE);
		IntentFilter filter = new IntentFilter(TIMER_ACTION);
		filter.addDataScheme(EXTRA_TIMER_SCHEME);
		service.registerReceiver(this, filter);
		serviceRegistered = true;
		wakeLock = new SipWakeLock((PowerManager) ctxt.getSystemService(Context.POWER_SERVICE));
	}
	
	
	synchronized private void quit() {
		Log.v(THIS_FILE, "Quit this wrapper");
		if(serviceRegistered) {
			try {
				service.unregisterReceiver(this);
				serviceRegistered = false;
			} catch (IllegalArgumentException e) {
				Log.e(THIS_FILE, "Impossible to destroy timer wrapper", e);
			}
		}
		
		/*
		List<Integer> keys = getPendingsKeys();
		//Log.v(THIS_FILE, "In buffer : "+keys);
		for(Integer key : keys) {
			int heapId = (key & 0xFFF000) >> 8;
			int timerId = key & 0x000FFF;
			doCancel(heapId, timerId);
		}
		*/
		
		if(wakeLock != null) {
			wakeLock.reset();
		}
//		hashOffset ++;
//		hashOffset = hashOffset % 10;
	}
	
	/*
	private synchronized List<Integer> getPendingsKeys(){
		ArrayList<Integer> keys = new ArrayList<Integer>();
		for(Integer key : pendings.keySet()) {
			keys.add(key);
		}
		return keys;
	}
	*/

	private PendingIntent getPendingIntentForTimer(int entry, int entryId) {
		Intent intent = new Intent(TIMER_ACTION);
		String toSend = EXTRA_TIMER_SCHEME + "://" + Integer.toString(entryId);
		intent.setData(Uri.parse(toSend));
		intent.putExtra(EXTRA_TIMER_ENTRY, entry);
		return PendingIntent.getBroadcast(service, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}
	
	
	private int doSchedule(int entry, int entryId, int intervalMs) {
		PendingIntent pendingIntent = getPendingIntentForTimer(entry, entryId);
		
		Log.d(THIS_FILE, "SCHED add " + entryId + " in " + intervalMs);
		
		int type = AlarmManager.ELAPSED_REALTIME_WAKEUP;
		if(intervalMs < 1000) {
			// If less than 1 sec, do not wake up -- that's probably stun check so useless to wake up about that
			type = AlarmManager.ELAPSED_REALTIME;
		}
		// Cancel previous reg anyway
		alarmManager.cancel(pendingIntent);
		// Clamp min
		if(intervalMs < 10) {
			intervalMs = 10;
		}
		
		long firstTime = SystemClock.elapsedRealtime() + intervalMs;
		// Push next
		alarmManager.set(type, firstTime, pendingIntent);
		return 1;
	}
	
	private int doCancel(int entry, int entryId) {
		alarmManager.cancel(getPendingIntentForTimer(entry, entryId));
		return 1;
	}
	
	
	public final static String EXTRA_TIMER_ENTRY = "entry";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if(TIMER_ACTION.equalsIgnoreCase(intent.getAction())) {
			int entry = intent.getIntExtra(EXTRA_TIMER_ENTRY, -1);
			Log.d(THIS_FILE, "FIRE Received : " + entry);
			if(singleton != null) {
				singleton.treatAlarm(entry);
			}else {
				Log.w(THIS_FILE, "Not found singleton");
			}
		}
	}
	
	public void treatAlarm(int entry) {
		TimerJob t = new TimerJob(entry);
		t.start();
	}
	
	Handler handler = new Handler();
	
	
	// Public API
	public static void create(SipService ctxt) {
		if(singleton == null) {
			singleton = new TimerWrapper(ctxt);
		}else {
			singleton.setContext(ctxt);
		}
	}


	public static void destroy() {
		if(singleton != null) {
			singleton.quit();
		}
	}
	
	
	public static int schedule(int entry, int entryId, int time) {
		if(singleton == null) {
			Log.e(THIS_FILE, "Timer NOT initialized");
			return -1;
		}
		return singleton.doSchedule(entry, entryId, time);
	}
	
	public static int cancel(int entry, int entryId) {
		return singleton.doCancel(entry, entryId);
	}
	
	

	private class TimerJob extends Thread {
		private int entry;
		
		public TimerJob(int anEntry) {
			super("TimerJob");
			entry = anEntry;
			wakeLock.acquire(this);
		}
		
		@Override
		public void run() {
			// From now, the timer can't be cancelled anymore

			Log.d(THIS_FILE, "FIRE START " + entry);
			
			try {
				pjsua.pj_timer_fire(entry);
			}catch(Exception e) {
				Log.e(THIS_FILE, "Native error ", e);
			}finally {
				wakeLock.release(this);
			}
			Log.d(THIS_FILE, "FIRE DONE " + entry);
			
		}
	}

}
