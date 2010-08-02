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
package com.csipsimple.widgets;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;

import com.csipsimple.utils.Log;

public class ScreenLocker extends RelativeLayout implements OnTouchListener, OnGestureListener, OnDoubleTapListener{

	private static final String THIS_FILE = "ScreenLocker";
	private GestureDetector gestureScanner;
	private Timer lockTimer;
	private Activity activity;

	public static final int WAIT_BEFORE_LOCK_LONG = 20000;
	public static final int WAIT_BEFORE_LOCK_START = 5000;

	private final static int SHOW_LOCKER = 0;
	private final static int HIDE_LOCKER = 1;
	
	
	
	public ScreenLocker(Context context, AttributeSet attrs) {
		super(context, attrs);
		gestureScanner = new GestureDetector(context, this);
		gestureScanner.setIsLongpressEnabled(false);
		gestureScanner.setOnDoubleTapListener(this);
		setOnTouchListener(this);
		
	}
	

	public void setActivity(Activity anActivity) {
		activity = anActivity;
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		gestureScanner.onTouchEvent(event);
		return true;
	}
	
	@Override
	public boolean onDoubleTap(MotionEvent e) {
		Log.d(THIS_FILE, "Double tap");
		hide();
		delayedLock(WAIT_BEFORE_LOCK_LONG);
		return true;
	}
	

	

	private class LockTimerTask extends TimerTask{
		@Override
		public void run() {
			handler.sendMessage(handler.obtainMessage(SHOW_LOCKER));
		}
	};
	

	public void delayedLock(int time) {
		if(lockTimer != null) {
			lockTimer.cancel();
			lockTimer.purge();
			lockTimer = null;
		}
		
		lockTimer = new Timer();
		
		lockTimer.schedule(new LockTimerTask(), time);
	}
	
	
	public void show() {
		setVisibility(VISIBLE);
		if(activity != null) {
			Window win = activity.getWindow();
			win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	        win.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
	}
	
	public void hide() {
		setVisibility(GONE);
		if(activity != null) {
			Window win = activity.getWindow();
			win.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
	        win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}
	
	public void tearDown() {
		if(lockTimer != null) {
			lockTimer.cancel();
			lockTimer.purge();
			lockTimer = null;
		}
	}
	
	
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SHOW_LOCKER:
				show();
				break;
			case HIDE_LOCKER:
				hide();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};
	

	@Override
	public boolean onSingleTapUp(MotionEvent e) { return false; }
	
	@Override
	public void onShowPress(MotionEvent e) {}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {return false;}
	
	@Override
	public void onLongPress(MotionEvent e) {}
	
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}
	
	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {return false;}
	
	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {return false;}
	
	@Override
	public boolean onDown(MotionEvent e) { return false;}

}
