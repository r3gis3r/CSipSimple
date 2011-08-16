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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.pjsip.pjsua.pjsua;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;

import com.csipsimple.service.SipService;

public class TimerWrapper extends BroadcastReceiver {
	
	private static final String THIS_FILE = "Timer wrap";
	
	private static final String TIMER_ACTION = "com.csipsimple.PJ_TIMER";
	private static final String EXTRA_TIMER_SCHEME = "timer";
	private SipService service;
	private AlarmManager alarmManager;

	private WakeLock wakeLock;
	private static TimerWrapper singleton;
	
	Map<Integer, PendingIntent> pendings = new HashMap<Integer, PendingIntent>();
	
	private TimerWrapper(SipService ctxt) {
		service = ctxt;
		alarmManager = (AlarmManager) service.getSystemService(Context.ALARM_SERVICE);
		IntentFilter filter = new IntentFilter(TIMER_ACTION);
		filter.addDataScheme(EXTRA_TIMER_SCHEME);
		service.registerReceiver(this, filter);
		
		PowerManager pm = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.csipsimple.wl.PJ_TIMER");
		wakeLock.setReferenceCounted(false);
	}
	
	
	private void quit() {
		try {
			service.unregisterReceiver(this);
		} catch (IllegalArgumentException e) {
			Log.e(THIS_FILE, "Impossible to destroy KA timer", e);
		}
		Set<Integer> mySet = pendings.keySet();
		for(Integer key : mySet) {
			int heapId = (key & 0xFFF000) >> 8;
			int timerId = key & 0x000FFF;
			doCancel(heapId, timerId);
		}
	}

	private PendingIntent getPendingIntentForTimer(int heapId, int timerId) {
		Intent intent = new Intent(TIMER_ACTION);
		String toSend = EXTRA_TIMER_SCHEME+"://"+Integer.toString(heapId)+"/"+Integer.toString(timerId);
		Log.d(THIS_FILE, "Send timer "+toSend);
		intent.setData(Uri.parse(toSend));
		return PendingIntent.getBroadcast(service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
	}
	
	private int getHashPending(int heapId, int timerId) {
		return heapId << 16 | timerId;
	}
	
	
	private int doSchedule(int heapId, int timerId, int intervalMs) {
		PendingIntent pendingIntent = getPendingIntentForTimer(heapId, timerId);
		pendings.put(getHashPending(heapId, timerId), pendingIntent);
		
		long firstTime = SystemClock.elapsedRealtime() + intervalMs;
		Log.d(THIS_FILE, "Ask to schedule @" + SystemClock.elapsedRealtime() + " :: next @"+ firstTime +" ("+intervalMs+"ms) timer : "+timerId );
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, pendingIntent);
		
		return 0;
	}
	
	private int doCancel(int heapId, int timerId) {
		int hash = getHashPending(heapId, timerId);
		PendingIntent pendingIntent = pendings.get(hash);
		if(pendingIntent != null) {
			pendings.remove(hash);
		}else {
			pendingIntent = getPendingIntentForTimer(heapId, timerId);
		}
		alarmManager.cancel(pendingIntent);
		return 0;
	}
	
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if(TIMER_ACTION.equalsIgnoreCase(intent.getAction())) {
			
			final int heapId = Integer.parseInt(intent.getData().getAuthority());
			final int timerId = Integer.parseInt(intent.getData().getLastPathSegment());
			
			Log.d(THIS_FILE, "Fire timer : "+heapId+"/" + timerId);

			
			Thread t = new Thread() {
				@Override
				public void run() {
					if(wakeLock != null) {
						wakeLock.acquire();
					}
					
					pjsua.pj_timer_fire(heapId, timerId);
					
					if(wakeLock != null) {
						wakeLock.release();
					}
				}
			};
			t.start();
			
			/*
			service.getExecutor().execute(new Runnable() {
				
				@Override
				public void run() {
					if(wakeLock != null) {
						wakeLock.acquire();
					}
					
					pjsua.pj_timer_fire(heapId, timerId);
					
					if(wakeLock != null) {
						wakeLock.release();
					}
				}
			});
			*/
		}
	}
	
	
	
	
	// Public API
	public static void create(SipService ctxt) {
		singleton = new TimerWrapper(ctxt);
	}
	
	public static void destroy() {
		if(singleton != null) {
			singleton.quit();
		}
	}
	
	
	public static int schedule(int heapId, int timerId, int time) {
		if(singleton == null) {
			Log.e(THIS_FILE, "Timer NOT initialized");
			return -1;
		}
		singleton.doSchedule(heapId, timerId, time);
			
		
		return 0;
	}
	
	public static int cancel(int heapId, int timerId) {
		singleton.doCancel(heapId, timerId);
		
		return 0;
	}


}
