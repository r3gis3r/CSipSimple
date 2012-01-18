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


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;

import com.csipsimple.R;

public class ExtensibleBadge extends LinearLayout implements OnClickListener, OnGestureListener {

	private final static int ANIM_DURATION = 250;
	private final static int ANIM_INTERPOLATOR = android.R.anim.accelerate_interpolator;
	
	private LinearLayout badge;
	private View handle;
	private ViewGroup content;
	private ScaleAnimation contentAnimation, contentRAnimation;
	
	private boolean isCollapsing = false;
	private int contentHeight;
	private State state = State.IDLE;
	private GestureDetector gestureScanner;
	private LinearLayout quickActions;
	//private Animation mTrackAnim;
	
	private enum State {
		ABOUT_TO_ANIMATE,
		ANIMATING,
		IDLE
	};
	
	public ExtensibleBadge(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.extensible_card, this, true);
		
		contentAnimation = new ScaleAnimation(1.0f, 1.0f, 0.0f, 1.0f);
		contentAnimation.setInterpolator(getContext(), ANIM_INTERPOLATOR);
		contentAnimation.setDuration(ANIM_DURATION);
		contentAnimation.setAnimationListener(animListener);
		
		contentRAnimation = new ScaleAnimation(1.0f, 1.0f, 1.0f, 0.0f);
		contentRAnimation.setInterpolator(getContext(), ANIM_INTERPOLATOR);
		contentRAnimation.setDuration(ANIM_DURATION);
		contentRAnimation.setAnimationListener(animListener);
		
		gestureScanner = new GestureDetector(this);
		
		// Prepare track entrance animation
		/*
		mTrackAnim = AnimationUtils.loadAnimation(context, R.anim.quickaction);
		mTrackAnim.setInterpolator(new Interpolator() {
			public float getInterpolation(float t) {
				// Pushes past the target area, then snaps back into place.
				// Equation for graphing: 1.2-((x*1.6)-1.1)^2
				final float inner = (t * 1.55f) - 1.1f;
				return 1.2f - inner * inner;
			}
		});
		*/
		

		badge = (LinearLayout) findViewById(R.id.badge);
		handle = findViewById(R.id.handle);
		content = (ViewGroup) findViewById(R.id.content);
		quickActions = (LinearLayout) findViewById(R.id.quickactions);
		
		handle.setFocusable(true);
		handle.setClickable(true);
		handle.setOnClickListener(this);
		
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent me) {
		return gestureScanner.onTouchEvent(me);
	}

	
	@Override
	protected void onFinishInflate() {
	
		super.onFinishInflate();
		
		

		Log.d("Badge", "inflating to "+content.getHeight());
		content.setVisibility(GONE);
		state = State.IDLE;
	}


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            if(content != null) {
	    	//	Log.d("Badge", "Layouting to "+content.getHeight());
	            contentHeight = content.getHeight();
            }
    }


	@Override
	public void onClick(View v) {
		toogleExpand();
	}

	public boolean isExpanded() {
		return content.getVisibility() == VISIBLE;
	}

	synchronized public void toogleExpand() {
		if(isExpanded()) {
			collapse();
		}else {
			expand();
		}
	}




	synchronized public void expand() {
		Log.d("Badge", "expand");
		if(!isExpanded()) {
			isCollapsing = false;
	
			initAnimation();
			post(startAnimation);
			bringToFront();
		}
		
	}
	

	synchronized public void collapse() {
		Log.d("Badge", "collpase");
		if(isExpanded()) {
			isCollapsing = true;
			
			initAnimation();
			post(startAnimation);
		}
		
	}
	
	private boolean initAnimation() {
		state = State.ABOUT_TO_ANIMATE;
		if(!isCollapsing) {
			content.setVisibility(VISIBLE);
		}
		return true;
	}
	
	 @Override
     protected void dispatchDraw(Canvas canvas) {
//           String name = getResources().getResourceEntryName(getId());
//           Log.d(TAG, name + " ispatchDraw " + mState);
             // this is why 'mState' was added:
             // avoid flicker before animation start
             if (state == State.ABOUT_TO_ANIMATE && !isCollapsing ) {
            	 canvas.save();
            	 canvas.clipRect(0, badge.getHeight(), badge.getWidth(), badge.getHeight()+content.getHeight()+handle.getHeight());
            	 canvas.translate(0, -contentHeight);
            	 super.dispatchDraw(canvas);
            	 
            	 canvas.restore();
            	 
            	 //draw normal the rest of the view
            	 canvas.clipRect(0, 0, badge.getWidth(), badge.getHeight());
            	 super.dispatchDraw(canvas);
            	 Log.d("badge", "about to animate translate canvas");
             }else {
            	 super.dispatchDraw(canvas);
             }
     }

	
	Runnable startAnimation = new Runnable() {
        public void run() {
        	Log.d("Badge", "height is "+contentHeight);
    		//Handler animation
    		TranslateAnimation tAnim = new TranslateAnimation(0, 0, 
    				isCollapsing ? 0 : -contentHeight, 
    				isCollapsing ? -contentHeight : 0);
    		tAnim.setDuration(ANIM_DURATION);
    		tAnim.setInterpolator(getContext(), ANIM_INTERPOLATOR);

    		// animations
    		handle.startAnimation(tAnim);
    		content.startAnimation(isCollapsing ? contentRAnimation : contentAnimation);
        }
	};
	
	private AnimationListener animListener = new AnimationListener() {

		@Override
		public void onAnimationEnd(Animation animation) {
			state = State.IDLE;
			Log.d("Anim", "End animation");
			if(isCollapsing) {
				content.setVisibility(GONE);
			}else {
			//	quickActions.startAnimation(mTrackAnim);
			}
			handle.setBackgroundResource(isCollapsing ? R.drawable.badge_handle: R.drawable.badge_handle_close );
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			Log.d("Anim", "repeat animation");
		}

		@Override
		public void onAnimationStart(Animation animation) {
			state = State.ANIMATING;
		}
		
	};
	
	@Override
	public boolean onDown(MotionEvent e) {
		return true;
	}

	

	private static final int SWIPE_MIN_DISTANCE = 30;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 10;
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		Log.d("Events", "FLING");
		if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
			Log.d("Events", "too long");
			return false;
		}
		if(e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
			Log.d("Events", "Ok buddy up");
			collapse();
		} else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
			Log.d("Events", "Ok buddy down");
			expand();
		}
		return true;
	}


	@Override
	public void onLongPress(MotionEvent e) {
		
	}


	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}


	@Override
	public void onShowPress(MotionEvent e) {
		
	}


	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	
	//Actions
	public void addItem(Drawable drawable, String text, OnClickListener l) {

		LayoutInflater inflater = LayoutInflater.from(getContext());
		
		QuickActionItem view = (QuickActionItem) inflater.inflate(R.layout.quickaction_vitem, quickActions, false);
		view.setImageDrawable(drawable);
		view.setText(text);
		view.setOnClickListener(l);

		final int index = quickActions.getChildCount() - 1;
		quickActions.addView(view, index);
	}
	
	public void addItem(int drawable, String text, OnClickListener l) {
		addItem(getContext().getResources().getDrawable(drawable), text, l);
	}

	public void addItem(Drawable drawable, int resid, OnClickListener l) {
		addItem(drawable, getContext().getResources().getString(resid), l);
	}

	public void addItem(int drawable, int resid, OnClickListener l) {
		addItem(getContext().getResources().getDrawable(drawable), getContext().getResources().getText(resid).toString(), l);
	}
	

	public void removeAllItems() {
		quickActions.removeViews(1, quickActions.getChildCount()-2);
	}
}
