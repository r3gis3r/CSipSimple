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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;

import com.csipsimple.service.SipService;
import com.csipsimple.service.SipWakeLock;

import org.pjsip.pjsua.pjsua;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TimerWrapper extends BroadcastReceiver {
	
	private static final String THIS_FILE = "Timer wrap";
	
	private static final String TIMER_ACTION = "com.csipsimple.PJ_TIMER";
	private static final String EXTRA_TIMER_SCHEME = "timer";
	private SipService service;
	private AlarmManager alarmManager;
	private SipWakeLock wakeLock;

	//private WakeLock wakeLock;
	private static TimerWrapper singleton;

    private static HandlerThread executorThread;
	
	private boolean serviceRegistered = false;
	
	private final List<Integer> scheduleEntries = new ArrayList<Integer>();
    private final List<Long> scheduleTimes = new ArrayList<Long>();

    private SipTimersExecutor mExecutor;
	
	
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
			for(Integer entryId : scheduleEntries) {
				alarmManager.cancel(getPendingIntentForTimer(entryId));
			}
		}
		scheduleEntries.clear();
		scheduleTimes.clear();
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

    private PendingIntent getPendingIntentForTimer(int entryId) {
        return getPendingIntentForTimer(entryId, null);
    }
    
	private PendingIntent getPendingIntentForTimer(int entryId, Long expires) {
		Intent intent = new Intent(TIMER_ACTION);
		String toSend = EXTRA_TIMER_SCHEME + "://" + Integer.toString(entryId);
		intent.setData(Uri.parse(toSend));
		intent.putExtra(EXTRA_TIMER_ENTRY, entryId);
		if(expires != null) {
	        intent.putExtra(EXTRA_TIMER_EXPIRATION, expires);
		}
		return PendingIntent.getBroadcast(service, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}
	
	
	private synchronized int doSchedule(int entryId, int intervalMs) {
		//Log.d(THIS_FILE, "SCHED add " + entryId + " in " + intervalMs);
        long firstTime = SystemClock.elapsedRealtime();
        // Clamp min
        if(intervalMs < 10) {
            firstTime +=  10;
        }else {
            firstTime += intervalMs;
        }
        
		PendingIntent pendingIntent = getPendingIntentForTimer(entryId, firstTime);
		
		
		// If less than 1 sec, do not wake up -- that's probably stun check so useless to wake up about that
		//int alarmType = (intervalMs < 1000) ? AlarmManager.ELAPSED_REALTIME : AlarmManager.ELAPSED_REALTIME_WAKEUP;
		int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
		
		// Cancel previous reg anyway
		alarmManager.cancel(pendingIntent);
		int existingReg = scheduleEntries.indexOf((Integer) entryId);
		if(existingReg != -1) {
		    scheduleEntries.remove(existingReg);
		    scheduleTimes.remove(existingReg);
		}
		
		// Push next
        Log.v(THIS_FILE, "Schedule timer " + entryId + " in " + intervalMs + "ms @ " + firstTime);
        Compatibility.setExactAlarm(alarmManager, alarmType, firstTime, pendingIntent);
		scheduleEntries.add((Integer) entryId);
        scheduleTimes.add((Long) firstTime);
		return 1;
	}
	
	private synchronized int doCancel(int entryId) {
        Log.v(THIS_FILE, "Cancel timer " + entryId);
		alarmManager.cancel(getPendingIntentForTimer(entryId));
        int existingReg = scheduleEntries.indexOf((Integer) entryId);
        if(existingReg != -1) {
            scheduleEntries.remove(existingReg);
            scheduleTimes.remove(existingReg);
            return 1;
        }
		return 0;
	}
	
	
	private final static String EXTRA_TIMER_ENTRY = "entry";
    private final static String EXTRA_TIMER_EXPIRATION = "expires";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if(TIMER_ACTION.equalsIgnoreCase(intent.getAction())) {
			if(singleton == null) {
				Log.w(THIS_FILE, "Not found singleton");
				return;
			}
			int timerEntry = intent.getIntExtra(EXTRA_TIMER_ENTRY, -1);
            Log.v(THIS_FILE, "FIRE Received TIMER " + timerEntry + " " + intent.getLongExtra(EXTRA_TIMER_EXPIRATION, 0) + " vs " + SystemClock.elapsedRealtime());
			singleton.treatAlarm(timerEntry, intent.getLongExtra(EXTRA_TIMER_EXPIRATION, 0));
		}
	}
	
	public void treatAlarm(int entry, long fireTime) {
		getExecutor().execute(new TimerJob(entry, fireTime));
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
		return singleton.doSchedule(entryId, time);
	}
	
	public static int cancel(int entry, int entryId) {
		return singleton.doCancel(entryId);
	}

    private static Looper createLooper() {
        if (executorThread == null) {
            Log.d(THIS_FILE, "Creating new handler thread");
            executorThread = new HandlerThread("SipTimers.Executor");
            executorThread.start();
        }
        return executorThread.getLooper();
    }
    
    private SipTimersExecutor getExecutor() {
        // create mExecutor lazily
        if (mExecutor == null) {
            mExecutor = new SipTimersExecutor(this);
        }
        return mExecutor;
    }

    // Executes immediate tasks in a single executorThread.
    public static class SipTimersExecutor extends Handler {
        WeakReference<TimerWrapper> handlerService;
        
        SipTimersExecutor(TimerWrapper s) {
            super(createLooper());
            handlerService = new WeakReference<TimerWrapper>(s);
        }

        public void execute(Runnable task) {
            Message.obtain(this, 0/* don't care */, task).sendToTarget();
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.obj instanceof Runnable) {
                executeInternal((Runnable) msg.obj);
            } else {
                Log.w(THIS_FILE, "can't handle msg: " + msg);
            }
        }

        private void executeInternal(Runnable task) {
            try {
                task.run();
            } catch (Throwable t) {
                Log.e(THIS_FILE, "run task: " + task, t);
            }
        }
    }

	private class TimerJob implements Runnable {
		private final int entryId;
		private final long fireTime;
		
		public TimerJob(int anEntry, long aFireTime) {
			entryId = anEntry;
			fireTime = aFireTime;
			wakeLock.acquire(this);
		}
		
		@Override
		public void run() {
			// From now, the timer can't be cancelled anymore
			Log.v(THIS_FILE, "FIRE START " + entryId);
			try {
			    boolean doFire = false;
			    synchronized (TimerWrapper.this) {
			        int existingReg = scheduleEntries.indexOf((Integer) entryId);
			        if(existingReg != -1) {
			            if(scheduleTimes.get(existingReg) == fireTime) {
			                doFire = true;
	                        scheduleEntries.remove(existingReg);
	                        scheduleTimes.remove(existingReg);
			            }
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
