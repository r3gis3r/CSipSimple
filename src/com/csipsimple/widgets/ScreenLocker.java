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
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;

import com.csipsimple.R;
import com.csipsimple.widgets.SlidingTab.OnTriggerListener;

public class ScreenLocker extends RelativeLayout implements OnTouchListener{

	//private static final String THIS_FILE = "ScreenLocker";
	private Timer lockTimer;
	private Activity activity;
	private SlidingTab stab;

	public static final int WAIT_BEFORE_LOCK_LONG = 10000;
	public static final int WAIT_BEFORE_LOCK_START = 5000;

	private final static int SHOW_LOCKER = 0;
	private final static int HIDE_LOCKER = 1;
	
	
	
	public ScreenLocker(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnTouchListener(this);
		
		
		stab = new SlidingTab(getContext());
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		//lp.setMargins(0, 286, 0, 0);
		stab.setLayoutParams(lp);
		stab.setLeftHintText(R.string.unlock);
		stab.setLeftTabResources(R.drawable.ic_jog_dial_unlock, R.drawable.jog_tab_target_green, R.drawable.jog_tab_bar_left_answer, R.drawable.jog_tab_left_answer);
		stab.setRightHintText(R.string.clear_call);
		
		addView(stab);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		
		final int parentWidth = r - l;
		final int parentHeight = b - t;
		final int top = parentHeight * 3/4 - stab.getHeight()/2;
		final int bottom = parentHeight * 3/4 + stab.getHeight() / 2;
		stab.layout(0, top, parentWidth, bottom);
		
	}

	public void setActivity(Activity anActivity, OnTriggerListener l) {
		activity = anActivity;
		stab.setOnTriggerListener(l);
	}
	
	public void reset() {
		stab.resetView();
	}
	
	public boolean onTouch(View v, MotionEvent event) {
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

}
