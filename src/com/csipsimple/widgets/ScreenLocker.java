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

package com.csipsimple.widgets;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.csipsimple.R;

import java.util.Timer;
import java.util.TimerTask;

public class ScreenLocker extends RelativeLayout implements OnTouchListener{

	//private static final String THIS_FILE = "ScreenLocker";
	private Timer lockTimer;
	private Activity activity;
	private SlidingTab stab;
    private IOnLeftRightChoice onLRChoiceListener;

	public static final int WAIT_BEFORE_LOCK_LONG = 10000;
	public static final int WAIT_BEFORE_LOCK_START = 5000;
    public static final int WAIT_BEFORE_LOCK_SHORT = 500;

	private final static int SHOW_LOCKER = 0;
	private final static int HIDE_LOCKER = 1;
	
	
	
	public ScreenLocker(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnTouchListener(this);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		updateTabLayout(l, t, r, b);
	}

    /**
     * Re-layout the slider to put it on bottom of the screen
     * @param l parent view left
     * @param t parent view top
     * @param r parent view right
     * @param b parent view bottom
     */
	private void updateTabLayout(int l, int t, int r, int b) {

        if(stab != null) {
            final int parentWidth = r - l;
            final int parentHeight = b - t;
            final int top = parentHeight * 3/4 - stab.getHeight()/2;
            final int bottom = parentHeight * 3/4 + stab.getHeight() / 2;
            stab.layout(0, top, parentWidth, bottom);
        }
	}

	public void setActivity(Activity anActivity) {
		activity = anActivity;
	}
	
	public void setOnLeftRightListener(IOnLeftRightChoice l) {
	    onLRChoiceListener = l;
	}
	
	private void reset() {
	    if(stab != null) {
	        stab.resetView();
	    }
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}
	
	
	@Override
	public void setVisibility(int visibility) {
	    super.setVisibility(visibility);
	    // We inflate the sliding tab only if we become visible.
	    if(visibility == VISIBLE && stab == null) {
	        stab = new SlidingTab(getContext());
	        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	        //lp.setMargins(0, 286, 0, 0);
	        stab.setLayoutParams(lp);
	        stab.setLeftHintText(R.string.unlock);
	        stab.setLeftTabResources(R.drawable.ic_jog_dial_unlock, R.drawable.jog_tab_target_green, R.drawable.jog_tab_bar_left_answer, R.drawable.jog_tab_left_answer);
	        stab.setRightHintText(R.string.clear_call);
	        stab.setOnLeftRightListener(onLRChoiceListener);
	        
	        addView(stab);
	        updateTabLayout(getLeft(), getTop(), getRight(), getBottom());
	    }
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
		
		lockTimer = new Timer("ScreenLock-timer");
		
		lockTimer.schedule(new LockTimerTask(), time);
	}
	
	
	public void show() {
		setVisibility(VISIBLE);
		if(activity != null) {
			Window win = activity.getWindow();
			win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	        win.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
		clearLockTasks();
	}
	
	public void hide() {
		setVisibility(GONE);
		if(activity != null) {
			Window win = activity.getWindow();
			win.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
	        win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		clearLockTasks();
		reset();
	}
	
	private void clearLockTasks() {
	    if(lockTimer != null) {
            lockTimer.cancel();
            lockTimer.purge();
            lockTimer = null;
        }
	}
	
	public void tearDown() {
		clearLockTasks();
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
