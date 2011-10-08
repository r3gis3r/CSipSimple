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
import java.util.concurrent.Semaphore;

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
	
	//Map<String, PendingIntent> pendings = new HashMap<String, PendingIntent>();
	Map<String, Semaphore> pendingsSemaphores = new HashMap<String, Semaphore>();
	boolean serviceRegistered = false;
	
	private int hashOffset = 0;
	
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
		hashOffset ++;
		hashOffset = hashOffset % 10;
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

	private PendingIntent getPendingIntentForTimer(int heapId, int timerId) {
		Intent intent = new Intent(TIMER_ACTION);
		String toSend = EXTRA_TIMER_SCHEME + "://" + Integer.toString(heapId) + "/" + Integer.toString(timerId);
		intent.setData(Uri.parse(toSend));
		//intent.setClass(service, TimerWrapper.class);
		return PendingIntent.getBroadcast(service, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}
	
	private String getHashPending(int heapId, int timerId) {
		return Integer.toString(hashOffset) + ":" + Integer.toString(heapId) + ":" + Integer.toString(timerId);
	}
	
	synchronized private int doSchedule(int heapId, int timerId, int intervalMs) {
		PendingIntent pendingIntent = getPendingIntentForTimer(heapId, timerId);
		
		//List<Integer> keys = getPendingsKeys();
		//Log.v(THIS_FILE, "1 In buffer : "+keys);
		String hash = getHashPending(heapId, timerId);
		//pendings.put(hash, pendingIntent);
		if(pendingsSemaphores.containsKey(hash)) {
			Log.e(THIS_FILE, "Huston we've got a problem here - trying to add to something not terminated");
		}
		pendingsSemaphores.put(hash, new Semaphore(0));
		
		long firstTime = SystemClock.elapsedRealtime() + intervalMs;
		Log.v(THIS_FILE, "SCHED add " + timerId + " in " + intervalMs);
		
		int type = AlarmManager.ELAPSED_REALTIME_WAKEUP;
		if(intervalMs < 1000) {
			// If less than 1 sec, do not wake up -- that's probably stun check so useless to wake up about that
			type = AlarmManager.ELAPSED_REALTIME;
		}
		// Cancel previous reg anyway
		alarmManager.cancel(pendingIntent);
		// Push next
		alarmManager.set(type, firstTime, pendingIntent);
		Log.v(THIS_FILE, "SCHED list " + pendingsSemaphores.keySet());
		pendingsSemaphores.get(hash).release();
		
		return 0;
	}
	
	private int doCancel(int heapId, int timerId) {
		Semaphore semForHash;
		String hash;
		synchronized (this) {
			hash = getHashPending(heapId, timerId);
			semForHash = pendingsSemaphores.get(hash);
			//pendingIntent = pendings.get(hash);
		}
		if(semForHash == null) {
			Log.e(THIS_FILE, "CANCEL something that does not exists !!!");
			// For safety return 1 so that the underlying stack can remove it
			return 1;
		}
		
		if(semForHash.tryAcquire()) {
		
			synchronized (this) {
				if(semForHash != pendingsSemaphores.get(hash)) {
					Log.w(THIS_FILE, "CANCEL Pending has been removed meanwhile");
					// For safety return 1 so that the underlying stack can remove it
					return 1;
				}
			
			
				Log.v(THIS_FILE, "CANCEL process "+ hash);
				
				alarmManager.cancel(getPendingIntentForTimer(heapId, timerId));
				//pendings.remove(hash);
				pendingsSemaphores.remove(hash);
			}
			// Never release to not allow post acquire of a fire
			//semForHash.release();
			Log.v(THIS_FILE, "CANCEL rel SEM "+ hash);
			return 1;
		}else {
			Log.v(THIS_FILE, "CANCEL failed : ongoing fire let fire do his job "+ hash);
			return 0;
		}
	}
	
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if(TIMER_ACTION.equalsIgnoreCase(intent.getAction())) {
			
			final int heapId = Integer.parseInt(intent.getData().getAuthority());
			final int timerId = Integer.parseInt(intent.getData().getLastPathSegment());
			if(singleton != null) {
				singleton.treatAlarm(heapId, timerId);
			}else {
				Log.w(THIS_FILE, "Not found singleton");
			}
		}
	}
	
	public synchronized void treatAlarm(int heapId, int timerId) {
		Semaphore semForHash;
		String hash = getHashPending(heapId, timerId);
		Log.v(THIS_FILE, "FIRE will proceed " + hash);
		
		synchronized (this) {
			semForHash = pendingsSemaphores.get(hash);
		}
		
		// From now, the timer can't be cancelled anymore
		if(semForHash != null) {
			TimerJob t = new TimerJob(heapId, timerId, semForHash);
			t.start();
		}else {
			Log.w(THIS_FILE, "FIRE ko : cancelling meanwhile "+hash);
		}
		
		
	}
	
	
	
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
	
	
	public static int schedule(int heapId, int timerId, int time) {
		if(singleton == null) {
			Log.e(THIS_FILE, "Timer NOT initialized");
			return -1;
		}
		singleton.doSchedule(heapId, timerId, time);
			
		
		return 0;
	}
	
	public static int cancel(int heapId, int timerId) {
		return singleton.doCancel(heapId, timerId);
	}
	
	

	private class TimerJob extends Thread {
		private int heapId;
		private int timerId;
		private String hash;
		private Semaphore semForHash;
		
		public TimerJob(int aHeapId, int aTimerId, Semaphore aSemForHash) {
			heapId = aHeapId;
			timerId = aTimerId;
			hash = getHashPending(heapId, timerId);
			semForHash = aSemForHash;
			wakeLock.acquire(this);
		}

		@Override
		public void run() {
			// From now, the timer can't be cancelled anymore

			Log.v(THIS_FILE, "FIRE start " + hash);
			
			try {
				if(semForHash.tryAcquire()) {
					pjsua.pj_timer_fire(heapId, timerId);
				}else {
					Log.v(THIS_FILE, "FIRE aborted " + hash);
				}
			}catch(Exception e) {
				Log.e(THIS_FILE, "Native error ", e);
			}finally {
				release();
			}
			
			wakeLock.release(this);
		}

		private void release() {
			synchronized(TimerWrapper.this){
				if(pendingsSemaphores.containsKey(hash) && pendingsSemaphores.get(hash) == semForHash) {
					pendingsSemaphores.remove(hash);
				}
			}
			semForHash.release();
			//Log.v(THIS_FILE, "FIRE end " + hash + " : " + pendingsSemaphores.keySet());
		}
	}
	


}
