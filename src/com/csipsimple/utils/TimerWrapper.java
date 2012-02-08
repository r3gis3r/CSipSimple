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

import java.util.ArrayList;
import java.util.List;

import org.pjsip.pjsua.pjsua;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
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
	
	private boolean serviceRegistered = false;
	
	private final List<Integer> scheduleEntries = new ArrayList<Integer>();
	
	
	private TimerWrapper(SipService ctxt) {
		super();
		setContext(ctxt);
	}
	
	private synchronized void setContext(SipService ctxt) {
	    // If we have a new context, restart bindings
		if(service != ctxt) {
    	    // Reset
    		quit();
    		// Set new service
    		service = ctxt;
    		alarmManager = (AlarmManager) service.getSystemService(Context.ALARM_SERVICE);
            wakeLock = new SipWakeLock((PowerManager) ctxt.getSystemService(Context.POWER_SERVICE));
		}
		if(!serviceRegistered) {
    		IntentFilter filter = new IntentFilter(TIMER_ACTION);
    		filter.addDataScheme(EXTRA_TIMER_SCHEME);
    		service.registerReceiver(this, filter);
    		serviceRegistered = true;
		}
	}
	
	
	private synchronized void quit() {
		Log.v(THIS_FILE, "Quit this wrapper");
		if(serviceRegistered) {
            serviceRegistered = false;
			try {
				service.unregisterReceiver(this);
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
		
		if(alarmManager != null) {
			for(Integer entry : scheduleEntries) {
				alarmManager.cancel(getPendingIntentForTimer(entry, entry));
			}
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
	
	
	private synchronized int doSchedule(int entry, int entryId, int intervalMs) {
		//Log.d(THIS_FILE, "SCHED add " + entryId + " in " + intervalMs);
		PendingIntent pendingIntent = getPendingIntentForTimer(entry, entryId);
		
		// If less than 1 sec, do not wake up -- that's probably stun check so useless to wake up about that
		//int alarmType = (intervalMs < 1000) ? AlarmManager.ELAPSED_REALTIME : AlarmManager.ELAPSED_REALTIME_WAKEUP;
		int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
		
		// Cancel previous reg anyway
		alarmManager.cancel(pendingIntent);
		scheduleEntries.remove((Integer) entry);
		
		
		long firstTime = SystemClock.elapsedRealtime();
		// Clamp min
		if(intervalMs < 10) {
			firstTime +=  10;
		}else {
			firstTime += intervalMs;
		}
		
		// Push next
        Log.v(THIS_FILE, "Schedule " + entry + " in " + intervalMs + "ms");
		alarmManager.set(alarmType, firstTime, pendingIntent);
		scheduleEntries.add((Integer) entry);
		return 1;
	}
	
	private synchronized int doCancel(int entry, int entryId) {
        Log.v(THIS_FILE, "Cancel " + entry );
		alarmManager.cancel(getPendingIntentForTimer(entry, entryId));
		scheduleEntries.remove((Integer) entry);
		return 1;
	}
	
	
	private final static String EXTRA_TIMER_ENTRY = "entry";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if(TIMER_ACTION.equalsIgnoreCase(intent.getAction())) {
			Log.v(THIS_FILE, "FIRE Received...");
			if(singleton == null) {
				Log.w(THIS_FILE, "Not found singleton");
				return;
			}
			int timerEntry = intent.getIntExtra(EXTRA_TIMER_ENTRY, -1);

            Log.d(THIS_FILE, "Treat " + timerEntry);
			singleton.treatAlarm(timerEntry);
		}
	}
	
	public void treatAlarm(int entry) {
		TimerJob t = new TimerJob(entry);
		t.start();
	}
	
	//private final Handler handler = new Handler();
	
	private final static Object singletonLock = new Object();
	
	// Public API
	public static void create(SipService ctxt) {
	    synchronized (singletonLock) {
	        if(singleton == null) {
	            singleton = new TimerWrapper(ctxt);
	        }else {
	            singleton.setContext(ctxt);
	        }
        }
	}


	public static void destroy() {
	    synchronized (singletonLock) {
    		if(singleton != null) {
    			singleton.quit();
    		}
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
		private final int entryId;
		
		public TimerJob(int anEntry) {
			super("TimerJob");
			entryId = anEntry;
			wakeLock.acquire(this);
		}
		
		@Override
		public void run() {
			// From now, the timer can't be cancelled anymore

			Log.v(THIS_FILE, "FIRE START " + entryId);
			
			
			try {
			    boolean doFire = false;
			    synchronized (TimerWrapper.this) {
	                if(scheduleEntries.contains(entryId)) {
	                    scheduleEntries.remove((Integer) entryId);
	                    doFire = true;
	                }
                }
			    if(doFire) {
			        pjsua.pj_timer_fire(entryId);
			    }else {
			        Log.w(THIS_FILE, "Fire from old run " + entryId);
			    }
			}catch(Exception e) {
				Log.e(THIS_FILE, "Native error ", e);
			}finally {
				wakeLock.release(this);
			}
			Log.v(THIS_FILE, "FIRE DONE " + entryId);
			
		}
	}

}
