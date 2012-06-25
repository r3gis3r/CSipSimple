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


package com.csipsimple.ui.incall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.Vibrator;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.csipsimple.R;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.MediaState;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.service.SipService;
import com.csipsimple.ui.PickupSipUri;
import com.csipsimple.ui.incall.CallProximityManager.ProximityDirector;
import com.csipsimple.ui.incall.InCallControls.OnTriggerListener;
import com.csipsimple.utils.CallsUtils;
import com.csipsimple.utils.CustomDistribution;
import com.csipsimple.utils.DialingFeedback;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.Theme;
import com.csipsimple.utils.keyguard.KeyguardWrapper;
import com.csipsimple.widgets.Dialpad;
import com.csipsimple.widgets.Dialpad.OnDialKeyListener;
import com.csipsimple.widgets.IOnLeftRightChoice;
import com.csipsimple.widgets.ScreenLocker;

import org.webrtc.videoengine.ViERenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

public class InCallActivity extends Activity implements OnTriggerListener, OnDialKeyListener,
        IOnLeftRightChoice, ProximityDirector, OnLayoutChangeListener {
    private final static String THIS_FILE = "SIP CALL HANDLER";
    private final static int DRAGGING_DELAY = 150;
    

    private int MAIN_PADDING/* = 15*/;
    private int HOLD_PADDING/* = 7*/;

    private SipCallSession[] callsInfo = null;
    private FrameLayout mainFrame;
    private InCallControls inCallControls;

    // Screen wake lock for incoming call
    private WakeLock wakeLock;
    // Screen wake lock for video
    private WakeLock videoWakeLock;

    private Dialpad dialPad;
    private LinearLayout dialPadContainer;
    private EditText dialPadTextView;

    private final HashMap<Integer, InCallInfo> badges = new HashMap<Integer, InCallInfo>();

    private ViewGroup callInfoPanel;
    private Timer quitTimer;

    // private LinearLayout detailedContainer, holdContainer;

    // True if running unit tests
    // private boolean inTest;

    private MediaState lastMediaState;

    private DialingFeedback dialFeedback;
    private PowerManager powerManager;
    private PreferencesProviderWrapper prefsWrapper;

    // Dnd views
    private ImageView endCallTarget;
    private Rect endCallTargetRect;
    private ImageView holdTarget, answerTarget, xferTarget;
    private Rect holdTargetRect, answerTargetRect, xferTargetRect;
    private Button middleAddCall;

    private DisplayMetrics metrics;
    private SurfaceView cameraPreview;
    private CallProximityManager proximityManager;
    private KeyguardWrapper keyguardManager;
    
    private boolean useAutoDetectSpeaker = false;

    private final static int PICKUP_SIP_URI_XFER = 0;
    private final static int PICKUP_SIP_URI_NEW_CALL = 1;
    

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(THIS_FILE, "Create in call");
        setContentView(R.layout.in_call_main);

        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        HOLD_PADDING = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        MAIN_PADDING = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());;
        

        bindService(new Intent(this, SipService.class), connection, Context.BIND_AUTO_CREATE);
        prefsWrapper = new PreferencesProviderWrapper(this);

        // Log.d(THIS_FILE, "Creating call handler for " +
        // callInfo.getCallId()+" state "+callInfo.getRemoteContact());
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                "com.csipsimple.onIncomingCall");
        wakeLock.setReferenceCounted(false);
        
        
        takeKeyEvents(true);

        // remoteContact = (TextView) findViewById(R.id.remoteContact);
        mainFrame = (FrameLayout) findViewById(R.id.mainFrame);
        inCallControls = (InCallControls) findViewById(R.id.inCallControls);
        inCallControls.setOnTriggerListener(this);

        dialPad = (Dialpad) findViewById(R.id.dialPad);
        dialPad.setOnDialKeyListener(this);
        dialPadContainer = (LinearLayout) findViewById(R.id.dialPadContainer);
        dialPadTextView = (EditText) findViewById(R.id.digitsText);
        callInfoPanel = (ViewGroup) findViewById(R.id.callInfoPanel);

        ScreenLocker lockOverlay = (ScreenLocker) findViewById(R.id.lockerOverlay);
        lockOverlay.setActivity(this, this);

        endCallTarget = (ImageView) findViewById(R.id.dropHangup);
        endCallTarget.getBackground().setDither(true);
        holdTarget = (ImageView) findViewById(R.id.dropHold);
        holdTarget.getBackground().setDither(true);
        answerTarget = (ImageView) findViewById(R.id.dropAnswer);
        answerTarget.getBackground().setDither(true);
        xferTarget = (ImageView) findViewById(R.id.dropXfer);
        xferTarget.getBackground().setDither(true);

        middleAddCall = (Button) findViewById(R.id.add_call_button);
        middleAddCall.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                onTrigger(ADD_CALL, null);
            }
        });
        if (!prefsWrapper.getPreferenceBooleanValue(SipConfigManager.SUPPORT_MULTIPLE_CALLS)) {
            middleAddCall.setEnabled(false);
            middleAddCall.setText(R.string.not_configured_multiple_calls);
        }

        // Listen to media & sip events to update the UI
        registerReceiver(callStateReceiver, new IntentFilter(SipManager.ACTION_SIP_CALL_CHANGED));
        registerReceiver(callStateReceiver, new IntentFilter(SipManager.ACTION_SIP_MEDIA_CHANGED));
        registerReceiver(callStateReceiver, new IntentFilter(SipManager.ACTION_ZRTP_SHOW_SAS));
        
        proximityManager = new CallProximityManager(this, this, lockOverlay);
        keyguardManager = KeyguardWrapper.getKeyguardManager(this);

        dialFeedback = new DialingFeedback(this, true);

        if (prefsWrapper.getPreferenceBooleanValue(SipConfigManager.PREVENT_SCREEN_ROTATION)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        if (quitTimer == null) {
            quitTimer = new Timer("Quit-timer");
        }

        attachVideoPreview();
        mainFrame.addOnLayoutChangeListener(this);
        
        useAutoDetectSpeaker = prefsWrapper.getPreferenceBooleanValue(SipConfigManager.AUTO_DETECT_SPEAKER);
        
        applyTheme();
        proximityManager.startTracking();
        
    }

    @Override
    protected void onStart() {
        Log.d(THIS_FILE, "Start in call");
        super.onStart();
        
        keyguardManager.unlock();
    }

    @Override
    protected void onResume() {
        super.onResume();

        endCallTargetRect = null;
        holdTargetRect = null;
        answerTargetRect = null;
        xferTargetRect = null;
        dialFeedback.resume();
        
        
        handler.sendMessage(handler.obtainMessage(UPDATE_FROM_CALL));
        
    }

    @Override
    protected void onPause() {
        super.onPause();
        dialFeedback.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        keyguardManager.lock();
    }

    @Override
    protected void onDestroy() {

        if (quitTimer != null) {
            quitTimer.cancel();
            quitTimer.purge();
            quitTimer = null;
        }

        if (draggingTimer != null) {
            draggingTimer.cancel();
            draggingTimer.purge();
            draggingTimer = null;
        }

        try {
            unbindService(connection);
        } catch (Exception e) {
            // Just ignore that
        }
        service = null;
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        proximityManager.stopTracking();
        proximityManager.release(0);
        try {
            unregisterReceiver(callStateReceiver);
        } catch (IllegalArgumentException e) {
            // That's the case if not registered (early quit)
        }
         
        // Remove badges
        for(InCallInfo badge : badges.values()) {
            callInfoPanel.removeView(badge);
            badge.terminate();
        }
        badges.clear();
        detachVideoPreview();
        
        super.onDestroy();
    }
    
    @SuppressWarnings("deprecation")
    private void attachVideoPreview() {

        // Video stuff
        if(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_VIDEO)) {
            if(cameraPreview == null) {
                Log.d(THIS_FILE, "Create Local Renderer");
                cameraPreview = ViERenderer.CreateLocalRenderer(this);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(100, 100);
                lp.gravity = Gravity.TOP | Gravity.LEFT;
                cameraPreview.setVisibility(View.GONE);
                mainFrame.addView(cameraPreview, lp);

                videoWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "com.csipsimple.videoCall");
                videoWakeLock.setReferenceCounted(false);
                
            }else {
                Log.d(THIS_FILE, "NO NEED TO Create Local Renderer");
            }
        }

        if(videoWakeLock != null && videoWakeLock.isHeld()) {
            videoWakeLock.release();
        }
    }
    
    private void detachVideoPreview() {
        if(mainFrame != null && cameraPreview != null) {
            mainFrame.removeView(cameraPreview);
        }
        cameraPreview = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        // TODO : update UI
        Log.d(THIS_FILE, "New intent is launched");

        super.onNewIntent(intent);
    }

    
    
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(THIS_FILE, "Configuration changed");
        if(cameraPreview != null && cameraPreview.getVisibility() == View.VISIBLE) {
            
            cameraPreview.setVisibility(View.GONE);
        }
        updateUIFromCall();
    }

    private void applyTheme() {
        String theme = prefsWrapper.getPreferenceStringValue(SipConfigManager.THEME);
        if (!TextUtils.isEmpty(theme)) {
            new Theme(this, theme, new Theme.onLoadListener() {
                @Override
                public void onLoad(Theme t) {
                    dialPad.applyTheme(t);
                    inCallControls.applyTheme(t);
                }
            });
        }
    }

    private static final int UPDATE_FROM_CALL = 1;
    private static final int UPDATE_FROM_MEDIA = 2;
    private static final int UPDATE_DRAGGING = 3;
    private static final int SHOW_SAS = 4;
    // Ui handler
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_FROM_CALL:
                    updateUIFromCall();
                    break;
                case UPDATE_FROM_MEDIA:
                    updateUIFromMedia();
                    break;
                case UPDATE_DRAGGING:
                    DraggingInfo di = (DraggingInfo) msg.obj;
                    inCallControls.setVisibility(di.isDragging ? View.GONE : View.VISIBLE);
                    endCallTarget.setVisibility(di.isDragging ? View.VISIBLE : View.GONE);
                    holdTarget.setVisibility(
                            (di.isDragging && di.call.isActive() && !di.call.isBeforeConfirmed()) ?
                                    View.VISIBLE : View.GONE);
                    answerTarget.setVisibility((di.call.isActive() && di.call.isBeforeConfirmed()
                            && di.call.isIncoming() && di.isDragging) ?
                            View.VISIBLE : View.GONE);
                    xferTarget.setVisibility((!di.call.isBeforeConfirmed()
                            && !di.call.isAfterEnded() && di.isDragging) ?
                            View.VISIBLE : View.GONE);
                    break;
                case SHOW_SAS:
                    ZrtpSasInfo sasInfo = (ZrtpSasInfo) msg.obj;
                    showZRTPInfo(sasInfo.dataPtr, sasInfo.sas);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICKUP_SIP_URI_XFER:
                if (resultCode == RESULT_OK && service != null) {
                    String callee = data.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                    try {
                        // TODO : should get the call that was xfered in buffer
                        // first.
                        SipCallSession currentCall = getActiveCallInfo();
                        if (currentCall != null
                                && currentCall.getCallId() != SipCallSession.INVALID_CALL_ID) {
                            service.xfer(currentCall.getCallId(), callee);
                        }
                    } catch (RemoteException e) {
                        // TODO : toaster
                    }
                }
                return;
            case PICKUP_SIP_URI_NEW_CALL:
                if (resultCode == RESULT_OK && service != null) {
                    String callee = data.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                    long accountId = data.getLongExtra(SipProfile.FIELD_ACC_ID,
                            SipProfile.INVALID_ID);
                    if (accountId != SipProfile.INVALID_ID) {
                        try {
                            service.makeCall(callee, (int) accountId);
                        } catch (RemoteException e) {
                            // TODO : toaster
                        }
                    }
                }
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static final int AUDIO_SETTINGS_MENU = Menu.FIRST + 1;


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, AUDIO_SETTINGS_MENU, Menu.NONE, R.string.prefs_media).setIcon(
                R.drawable.ic_menu_media);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int optSel = item.getItemId();
        if (optSel == AUDIO_SETTINGS_MENU) {
            startActivity(new Intent(this, InCallMediaControl.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Get the call that is active on the view
     * 
     * @param excludeHold if true we do not return cals hold locally
     * @return
     */
    private SipCallSession getActiveCallInfo() {
        SipCallSession currentCallInfo = null;
        if (callsInfo == null) {
            return null;
        }
        for (SipCallSession callInfo : callsInfo) {
            currentCallInfo = getPrioritaryCall(callInfo, currentCallInfo);
        }
        return currentCallInfo;
    }

    /**
     * Get call info for a given call id.
     * @param callId the id of the call
     * @return the sip call session.
     */
    private SipCallSession getCallInfo(int callId) {
        if (callsInfo == null) {
            return null;
        }
        for (SipCallSession callInfo : callsInfo) {
            if (callInfo.getCallId() == callId) {
                return callInfo;
            }
        }
        return null;
    }

    /**
     * Get the call with the higher priority comparing two calls
     * @param call1 First call object to compare
     * @param call2 Second call object to compare
     * @return The call object with highest priority
     */
    private SipCallSession getPrioritaryCall(SipCallSession call1, SipCallSession call2) {
        // We prefer the not null
        if (call1 == null) {
            return call1;
        } else if (call2 == null) {
            return call1;
        }
        // We prefer the one not terminated
        if (call1.isAfterEnded()) {
            return call2;
        } else if (call2.isAfterEnded()) {
            return call1;
        }
        // We prefer the one not held
        if (call1.isLocalHeld()) {
            return call2;
        } else if (call2.isLocalHeld()) {
            return call1;
        }
        // We prefer the most recent
        return (call1.callStart > call2.callStart) ? call2 : call1;
    }

    
    /**
     * Update the user interface from calls state.
     */
    private synchronized void updateUIFromCall() {
        if (!serviceConnected) {
            return;
        }

        // Current call is the call emphasis by the UI.
        SipCallSession mainCallInfo = null;
        boolean showCameraPreview = false;

        int mainsCalls = 0;
        int heldsCalls = 0;
        int heldIndex = 0;
        int mainIndex = 0;

        // Add badges if necessary
        if (callsInfo != null) {
            for (SipCallSession callInfo : callsInfo) {
                Log.d(THIS_FILE,
                        "We have a call " + callInfo.getCallId() + " / " + callInfo.getCallState()
                                + "/" + callInfo.getMediaStatus());

                if (!callInfo.isAfterEnded() && !hasBadgeForCall(callInfo)) {
                    Log.d(THIS_FILE, "Has to add badge for " + callInfo.getCallId());
                    addBadgeForCall(callInfo);
                }

                if (!callInfo.isAfterEnded()) {
                    if (callInfo.isLocalHeld()) {
                        heldsCalls++;
                    } else {
                        mainsCalls++;
                    }
                    if(callInfo.mediaHasVideo()) {
                        showCameraPreview = true;
                    }
                }

                mainCallInfo = getPrioritaryCall(callInfo, mainCallInfo);
                
            }
        }
        
        

        boolean hasBottomButtons = false;
        // Update call control visibility - must be done before call cards 
        // because badge avail size depends on that
        if ((mainsCalls == 1 || (mainsCalls == 0 && heldsCalls == 1)) && mainCallInfo != null) {
            Log.d(THIS_FILE, "Current call is " + mainCallInfo.getCallId());

            // Update in call actions
            inCallControls.setCallState(mainCallInfo);
            inCallControls.setVisibility(View.VISIBLE);
            hasBottomButtons = true;
        } else {
            inCallControls.setVisibility(View.GONE);
        }
        
        // Update call cards layouting
        
        int fullWidth = mainFrame.getWidth();
        if(fullWidth == 0) {
            // Not yet rendered, fall back to screen dimension
            fullWidth = metrics.widthPixels;
        }
        int mainWidth = fullWidth; 
        if (heldsCalls > 0) {
            // In this case available width for MAIN part is 2/3 of the view
            mainWidth -= mainWidth / 3;
        }

        // Compute main height (remove all possible bottom views)
        int mainHeight = mainFrame.getHeight();
        if(mainHeight == 0) {
            //Not yet rendered, fall back to screen dimension
            mainHeight = metrics.heightPixels;
        }
        if(hasBottomButtons) {
            // Bottom height is 263px in hdpi
            mainHeight -= (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 175, getResources().getDisplayMetrics());
        }else {
            if(inCallControls.getLockerVisibility() == View.VISIBLE) {
                // If it's the locker, the best way is just to 
                // remove some bottom part of the screen
                mainHeight = mainHeight * 7 / 15;
            }
        }
        Log.d(THIS_FILE, "Avail size is : " + mainWidth + "x" + mainHeight);
        
        // Update each badges
        ArrayList<InCallInfo> badgesToRemove = new ArrayList<InCallInfo>();
        for (Entry<Integer, InCallInfo> badgeSet : badges.entrySet()) {
            SipCallSession callInfo = getCallInfo(badgeSet.getKey());
            InCallInfo badge = badgeSet.getValue();
            if (callInfo != null) {
                // Main call position / size should be done at the end
                if (callInfo.isAfterEnded() && (mainsCalls + heldsCalls > 0)) {
                    // The badge should be removed
                    badgesToRemove.add(badge);

                } else if (callInfo.isLocalHeld()) {
                    // The call is held
                    int y = MAIN_PADDING + heldIndex * (mainHeight / 3 + MAIN_PADDING);
                    Rect wrap = new Rect(
                            mainWidth + HOLD_PADDING,
                            y,
                            metrics.widthPixels - HOLD_PADDING,
                            y + mainHeight / 3);
                    layoutBadge(wrap, badge);
                    heldIndex++;

                } else {

                    // The call is normal
                    int x = MAIN_PADDING;
                    int y = MAIN_PADDING;
                    int end_x = mainWidth - MAIN_PADDING;
                    int end_y = mainHeight - MAIN_PADDING;
                    if (mainsCalls > 1) {
                        // we split view in 4
                        if ((mainIndex % 2) == 0) {
                            // First column
                            end_x = mainWidth / 2 - MAIN_PADDING;
                        } else {
                            // Second column
                            x = mainWidth / 2 + MAIN_PADDING;
                        }
                        if (mainIndex < 2) {
                            end_y = mainHeight / 2 - MAIN_PADDING;
                        } else {
                            y = mainHeight / 2 + MAIN_PADDING;
                        }
                    }
                    Rect wrap = new Rect(
                            x,
                            y,
                            end_x,
                            end_y);
                    Log.d(THIS_FILE, "Layout badge for size "+x+","+y+" to "+end_x+","+end_y);
                    layoutBadge(wrap, badge);
                    mainIndex++;
                }
            }
            // Update badge state
            badge.setCallState(callInfo);
        }

        // Remove useless badges
        for (InCallInfo badge : badgesToRemove) {
            badge.terminate();
            callInfoPanel.removeView(badge);
            SipCallSession ci = badge.getCallInfo();
            if (ci != null) {
                badges.remove(ci.getCallId());
            }
        }


        if (mainCallInfo != null) {
            Log.d(THIS_FILE, "Active call is " + mainCallInfo.getCallId());
            Log.d(THIS_FILE, "Update ui from call " + mainCallInfo.getCallId() + " state "
                    + CallsUtils.getStringCallState(mainCallInfo, this));
            int state = mainCallInfo.getCallState();

            int backgroundResId = R.drawable.bg_in_call_gradient_unidentified;

            // We manage wake lock
            switch (state) {
                case SipCallSession.InvState.INCOMING:
                case SipCallSession.InvState.EARLY:
                case SipCallSession.InvState.CALLING:
                case SipCallSession.InvState.CONNECTING:

                    Log.d(THIS_FILE, "Acquire wake up lock");
                    if (wakeLock != null && !wakeLock.isHeld()) {
                        wakeLock.acquire();
                    }
                    break;
                case SipCallSession.InvState.CONFIRMED:
                    backgroundResId = R.drawable.bg_in_call_gradient_connected;
                    if (lastMediaState != null && lastMediaState.isBluetoothScoOn) {
                        backgroundResId = R.drawable.bg_in_call_gradient_bluetooth;
                    }
                    if (wakeLock != null && wakeLock.isHeld()) {
                        Log.d(THIS_FILE, "Releasing wake up lock - confirmed");
                        wakeLock.release();
                    }
                    break;
                case SipCallSession.InvState.NULL:
                case SipCallSession.InvState.DISCONNECTED:
                    Log.d(THIS_FILE, "Active call session is disconnected or null wait for quit...");
                    // This will release locks
                    delayedQuit();
                    return;

            }

            int mediaStatus = mainCallInfo.getMediaStatus();
            switch (mediaStatus) {
                case SipCallSession.MediaState.ACTIVE:
                    break;
                case SipCallSession.MediaState.REMOTE_HOLD:
                case SipCallSession.MediaState.LOCAL_HOLD:
                case SipCallSession.MediaState.NONE:
                    if (backgroundResId == R.drawable.bg_in_call_gradient_connected ||
                            backgroundResId == R.drawable.bg_in_call_gradient_bluetooth) {
                        backgroundResId = R.drawable.bg_in_call_gradient_on_hold;
                    }
                    break;
                case SipCallSession.MediaState.ERROR:
                default:
                    break;
            }

            mainFrame.setBackgroundResource(backgroundResId);
            Log.d(THIS_FILE, "we leave the update ui function");
        }
        
        proximityManager.updateProximitySensorMode();

        if (mainsCalls == 0) {
            if (!CustomDistribution.forceNoMultipleCalls()) {
                middleAddCall.setVisibility(View.VISIBLE);
            }
        } else {
            middleAddCall.setVisibility(View.GONE);
        }
        
        // Update the camera preview visibility 
        if(cameraPreview != null) {
            cameraPreview.setVisibility(showCameraPreview ? View.VISIBLE : View.GONE);
            if(showCameraPreview) {
                if(videoWakeLock != null) {
                    videoWakeLock.acquire();
                }
            }else {

                if(videoWakeLock != null && videoWakeLock.isHeld()) {
                    videoWakeLock.release();
                }
            }
        }
        

        if (heldsCalls + mainsCalls == 0) {
            delayedQuit();
        }
    }

    /**
     * Update ui from media state.
     */
    private synchronized void updateUIFromMedia() {
        if (service != null) {
            MediaState mediaState;
            try {
                mediaState = service.getCurrentMediaState();
                Log.d(THIS_FILE, "Media update ....");
                if (!mediaState.equals(lastMediaState)) {
                    SipCallSession callInfo = getActiveCallInfo();
                    lastMediaState = mediaState;

                    if (callInfo != null) {
                        int state = callInfo.getCallState();

                        // Background
                        if (state == SipCallSession.InvState.CONFIRMED) {
                            mainFrame
                                    .setBackgroundResource(lastMediaState.isBluetoothScoOn ? R.drawable.bg_in_call_gradient_bluetooth
                                            : R.drawable.bg_in_call_gradient_connected);
                        }
                    }

                    // Actions
                    inCallControls.setMediaState(lastMediaState);
                }
            } catch (RemoteException e) {
                Log.e(THIS_FILE, "Can't get the media state ", e);
            }
        }

        proximityManager.updateProximitySensorMode();
    }

    private synchronized void delayedQuit() {

        if (wakeLock != null && wakeLock.isHeld()) {
            Log.d(THIS_FILE, "Releasing wake up lock");
            wakeLock.release();
        }
        
        proximityManager.release(0);
        
        setDialpadVisibility(View.GONE);
        middleAddCall.setVisibility(View.GONE);
        setCallBadgesVisibility(View.VISIBLE);
        inCallControls.setVisibility(View.GONE);
        mainFrame.setBackgroundResource(R.drawable.bg_in_call_gradient_ended);

        Log.d(THIS_FILE, "Start quit timer");
        if (quitTimer != null) {
            quitTimer.schedule(new QuitTimerTask(), 3000);
        } else {
            finish();
        }
    }

    private class QuitTimerTask extends TimerTask {
        @Override
        public void run() {
            Log.d(THIS_FILE, "Run quit timer");
            finish();
        }
    };

    private void setDialpadVisibility(int visibility) {
        dialPadContainer.setVisibility(visibility);
        int antiVisibility = visibility == View.GONE ? View.VISIBLE : View.GONE;
        setCallBadgesVisibility(antiVisibility);
    }

    private void setCallBadgesVisibility(int visibility) {
        callInfoPanel.setVisibility(visibility);
    }

    private boolean hasBadgeForCall(SipCallSession call) {
        return badges.containsKey(call.getCallId());
    }

    private void addBadgeForCall(SipCallSession call) {
        InCallInfo badge = new InCallInfo(this, null);
        callInfoPanel.addView(badge);

        badge.forceLayout();
        badge.setOnTriggerListener(this);
        badge.setOnTouchListener(new OnBadgeTouchListener(badge, call));
        badges.put(call.getCallId(), badge);
    }

    private void layoutBadge(Rect wrap, InCallInfo mainBadge) {
        //Log.d(THIS_FILE,
        //        "Layout badge for " + wrap.width() + " x " + wrap.height() +
        //                " +" + wrap.top + " + " + wrap.left);
        // Set badge size
        Rect r = mainBadge.setSize(wrap.width(), wrap.height());
        // Reposition badge at the correct place
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        Log.d(THIS_FILE, "Sized @ : " + r.width() + "x" + r.height());
        lp.topMargin = wrap.centerY() - (r.height() >> 1);
        lp.leftMargin = wrap.centerX() - (r.width() >> 1);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        //Log.d(THIS_FILE, "Set margins : " + lp.topMargin + ", " + lp.leftMargin);
        mainBadge.setLayoutParams(lp);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(THIS_FILE, "Key down : " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                //
                // Volume has been adjusted by the user.
                //
                Log.d(THIS_FILE, "onKeyDown: Volume button pressed");
                int action = AudioManager.ADJUST_RAISE;
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    action = AudioManager.ADJUST_LOWER;
                }

                // Detect if ringing
                SipCallSession currentCallInfo = getActiveCallInfo();
                // If not any active call active
                if (currentCallInfo == null && serviceConnected) {
                    break;
                }

                if (service != null) {
                    try {
                        service.adjustVolume(currentCallInfo, action, AudioManager.FLAG_SHOW_UI);
                    } catch (RemoteException e) {
                        Log.e(THIS_FILE, "Can't adjust volume", e);
                    }
                }

                return true;
            case KeyEvent.KEYCODE_CALL:
            case KeyEvent.KEYCODE_ENDCALL:
                return inCallControls.onKeyDown(keyCode, event);
            case KeyEvent.KEYCODE_SEARCH:
                // Prevent search
                return true;
            default:
                // Nothing to do
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(THIS_FILE, "Key up : " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_CALL:
            case KeyEvent.KEYCODE_SEARCH:
                return true;
            case KeyEvent.KEYCODE_ENDCALL:
                return inCallControls.onKeyDown(keyCode, event);

        }
        return super.onKeyUp(keyCode, event);
    }

    private class ZrtpSasInfo {
        public String sas;
        public int dataPtr;
    }

    private BroadcastReceiver callStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(SipManager.ACTION_SIP_CALL_CHANGED)) {
                if (service != null) {
                    try {
                        callsInfo = service.getCalls();
                    } catch (RemoteException e) {
                        Log.e(THIS_FILE, "Not able to retrieve calls");
                    }
                }

                handler.sendMessage(handler.obtainMessage(UPDATE_FROM_CALL));
            } else if (action.equals(SipManager.ACTION_SIP_MEDIA_CHANGED)) {
                handler.sendMessage(handler.obtainMessage(UPDATE_FROM_MEDIA));
            } else if (action.equals(SipManager.ACTION_ZRTP_SHOW_SAS)) {
                ZrtpSasInfo sasInfo = new ZrtpSasInfo();
                sasInfo.sas = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                sasInfo.dataPtr = intent.getIntExtra(Intent.EXTRA_UID, 0);
                handler.sendMessage(handler.obtainMessage(SHOW_SAS, sasInfo));
            }
        }
    };

    /**
     * Service binding
     */
    private boolean serviceConnected = false;
    private ISipService service;
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            service = ISipService.Stub.asInterface(arg1);
            try {
                // Log.d(THIS_FILE,
                // "Service started get real call info "+callInfo.getCallId());
                callsInfo = service.getCalls();
                serviceConnected = true;
                handler.sendMessage(handler.obtainMessage(UPDATE_FROM_CALL));
                handler.sendMessage(handler.obtainMessage(UPDATE_FROM_MEDIA));
            } catch (RemoteException e) {
                Log.e(THIS_FILE, "Can't get back the call", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceConnected = false;
            callsInfo = null;
        }
    };

    // private boolean showDetails = true;


    @Override
    public void onTrigger(int whichAction, SipCallSession call) {

        // Sanity check for actions requiring valid call id
        if (whichAction == TAKE_CALL || whichAction == DECLINE_CALL ||
            whichAction == CLEAR_CALL || whichAction == DETAILED_DISPLAY || 
            whichAction == TOGGLE_HOLD || whichAction == START_RECORDING ||
            whichAction == STOP_RECORDING ) {
            // We check that current call is valid for any actions
            if (call == null) {
                Log.e(THIS_FILE, "Try to do an action on a null call !!!");
                return;
            }
            if (call.getCallId() == SipCallSession.INVALID_CALL_ID) {
                Log.e(THIS_FILE, "Try to do an action on an invalid call !!!");
                return;
            }
        }

        // Reset proximity sensor timer
        proximityManager.restartTimer();
        
        try {
            switch (whichAction) {
                case TAKE_CALL: {
                    if (service != null) {
                        Log.d(THIS_FILE, "Answer call " + call.getCallId());

                        boolean shouldHoldOthers = false;

                        // Well actually we should be always before confirmed
                        if (call.isBeforeConfirmed()) {
                            shouldHoldOthers = true;
                        }

                        service.answer(call.getCallId(), SipCallSession.StatusCode.OK);

                        // if it's a ringing call, we assume that user wants to
                        // hold other calls
                        if (shouldHoldOthers && callsInfo != null) {
                            for (SipCallSession callInfo : callsInfo) {
                                // For each active and running call
                                if (SipCallSession.InvState.CONFIRMED == callInfo.getCallState()
                                        && !callInfo.isLocalHeld()
                                        && callInfo.getCallId() != call.getCallId()) {

                                    Log.d(THIS_FILE, "Hold call " + callInfo.getCallId());
                                    service.hold(callInfo.getCallId());

                                }
                            }
                        }
                    }
                    break;
                }
                case DECLINE_CALL:
                case CLEAR_CALL: {
                    if (service != null) {
                        service.hangup(call.getCallId(), 0);
                    }
                    break;
                }
                case MUTE_ON:
                case MUTE_OFF: {
                    if (service != null) {
                        service.setMicrophoneMute((whichAction == MUTE_ON) ? true : false);
                    }
                    break;
                }
                case SPEAKER_ON:
                case SPEAKER_OFF: {
                    if (service != null) {
                        service.setSpeakerphoneOn((whichAction == SPEAKER_ON) ? true : false);
                    }
                    break;
                }
                case BLUETOOTH_ON:
                case BLUETOOTH_OFF: {
                    if (service != null) {
                        service.setBluetoothOn((whichAction == BLUETOOTH_ON) ? true : false);
                    }
                    break;
                }
                case DIALPAD_ON:
                case DIALPAD_OFF: {
                    setDialpadVisibility((whichAction == DIALPAD_ON) ? View.VISIBLE : View.GONE);
                    break;
                }
                case DETAILED_DISPLAY: {
                    if (service != null) {
                        String infos = service.showCallInfosDialog(call.getCallId());

                        SpannableStringBuilder buf = new SpannableStringBuilder();
                        Builder builder = new AlertDialog.Builder(this);

                        buf.append(infos);
                        TextAppearanceSpan textSmallSpan = new TextAppearanceSpan(this,
                                android.R.style.TextAppearance_Small);
                        buf.setSpan(textSmallSpan, 0, buf.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        AlertDialog dialog = builder.setIcon(android.R.drawable.ic_dialog_info)
                                .setMessage(buf)
                                .setNeutralButton(R.string.ok, null)
                                .create();
                        dialog.show();
                    }
                    break;
                }
                case TOGGLE_HOLD: {
                    if (service != null) {
                        // Log.d(THIS_FILE,
                        // "Current state is : "+callInfo.getCallState().name()+" / "+callInfo.getMediaStatus().name());
                        if (call.getMediaStatus() == SipCallSession.MediaState.LOCAL_HOLD ||
                                call.getMediaStatus() == SipCallSession.MediaState.NONE) {
                            service.reinvite(call.getCallId(), true);
                        } else {
                            service.hold(call.getCallId());
                        }
                    }
                    break;
                }
                case MEDIA_SETTINGS: {
                    startActivity(new Intent(this, InCallMediaControl.class));
                    break;
                }
                case XFER_CALL: {
                    Intent pickupIntent = new Intent(this, PickupSipUri.class);
                    startActivityForResult(pickupIntent, PICKUP_SIP_URI_XFER);
                    break;
                }

                case ADD_CALL: {
                    Intent pickupIntent = new Intent(this, PickupSipUri.class);
                    startActivityForResult(pickupIntent, PICKUP_SIP_URI_NEW_CALL);
                    break;
                }
                case START_RECORDING :{
                    if(service != null) {
                        service.startRecording(call.getCallId());
                    }
                    break;
                }
                case STOP_RECORDING : {
                    if(service != null) {
                        service.stopRecording(call.getCallId());
                    }
                    break;
                }
                    
            }
        } catch (RemoteException e) {
            Log.e(THIS_FILE, "Was not able to call service method", e);
        }
    }

    @Override
    public void onTrigger(int keyCode, int dialTone) {
        proximityManager.restartTimer();

        if (service != null) {
            SipCallSession currentCall = getActiveCallInfo();
            if (currentCall != null && currentCall.getCallId() != SipCallSession.INVALID_CALL_ID) {
                try {
                    service.sendDtmf(currentCall.getCallId(), keyCode);
                    dialFeedback.giveFeedback(dialTone);
                    KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
                    char nbr = event.getNumber();
                    dialPadTextView.getText().append(nbr);
                } catch (RemoteException e) {
                    Log.e(THIS_FILE, "Was not able to send dtmf tone", e);
                }
            }
        }

    }


    @Override
    public void onLeftRightChoice(int whichHandle) {
        switch (whichHandle) {
            case LEFT_HANDLE:
                Log.d(THIS_FILE, "We unlock");
                proximityManager.release(0);
                proximityManager.updateProximitySensorMode();
                break;
            case RIGHT_HANDLE:
                Log.d(THIS_FILE, "We clear the call");
                onTrigger(OnTriggerListener.CLEAR_CALL, getActiveCallInfo());
                proximityManager.release(0);
            default:
                break;
        }

    }

    // Drag and drop feature
    private Timer draggingTimer;

    public class OnBadgeTouchListener implements OnTouchListener {
        private SipCallSession call;
        private InCallInfo badge;
        private boolean isDragging = false;
        private SetDraggingTimerTask draggingDelayTask;
        Vibrator vibrator;
        int beginX = 0;
        int beginY = 0;

        private class SetDraggingTimerTask extends TimerTask {
            @Override
            public void run() {
                vibrator.vibrate(50);
                setDragging(true);
                Log.d(THIS_FILE, "Begin dragging");
            }
        };

        public OnBadgeTouchListener(InCallInfo aBadge, SipCallSession aCall) {
            call = aCall;
            badge = aBadge;
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            // TODO : move somewhere else
            if (draggingTimer == null) {
                draggingTimer = new Timer("Dragging-timer");
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            int X = (int) event.getRawX();
            int Y = (int) event.getRawY();

            // Reset the not proximity sensor lock overlay
            proximityManager.restartTimer();
            

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (draggingDelayTask != null) {
                        draggingDelayTask.cancel();
                    }
                    draggingDelayTask = new SetDraggingTimerTask();
                    beginX = X;
                    beginY = Y;
                    draggingTimer.schedule(draggingDelayTask, DRAGGING_DELAY);
                case MotionEvent.ACTION_MOVE:
                    if (isDragging) {
                        float size = Math.max(75.0f, event.getSize() + 50.0f);

                        Rect wrap = new Rect(
                                (int) (X - (size)),
                                (int) (Y - (size)),
                                (int) (X + (size / 2.0f)),
                                (int) (Y + (size / 2.0f)));
                        layoutBadge(wrap, badge);
                        badge.bringToFront();
                        // Log.d(THIS_FILE, "Is moving to "+X+", "+Y);
                        return true;
                    } else {
                        if (Math.abs(X - beginX) > 50 || Math.abs(Y - beginY) > 50) {
                            Log.d(THIS_FILE, "Stop dragging");
                            stopDragging();
                            return true;
                        }
                        return false;
                    }

                case MotionEvent.ACTION_UP:
                    onDropBadge(X, Y, badge, call);
                    stopDragging();
                    return true;
                    // Yes we continue cause this is a stop action
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_OUTSIDE:
                    Log.d(THIS_FILE, "Stop dragging");
                    stopDragging();
                    return false;
            }
            return false;
        }

        private void stopDragging() {
            // TODO : thread save it
            if (draggingDelayTask != null) {
                draggingDelayTask.cancel();
            }
            setDragging(false);
        }

        private void setDragging(boolean dragging) {
            isDragging = dragging;
            handler.sendMessage(handler.obtainMessage(UPDATE_DRAGGING, new DraggingInfo(isDragging,
                    badge, call)));
        }

        public void setCallState(SipCallSession callInfo) {
            Log.d(THIS_FILE,
                    "Updated call infos : " + call.getCallState() + " and " + call.getMediaStatus()
                            + " et " + call.isLocalHeld());
            call = callInfo;
        }
    }

    private void onDropBadge(int X, int Y, InCallInfo badge, SipCallSession call) {
        Log.d(THIS_FILE, "Dropping !!! in " + X + ", " + Y);

        // Rectangle init if not already done
        if (endCallTargetRect == null && endCallTarget.getVisibility() == View.VISIBLE) {
            endCallTargetRect = new Rect(endCallTarget.getLeft(), endCallTarget.getTop(),
                    endCallTarget.getRight(), endCallTarget.getBottom());
        }
        if (holdTargetRect == null && holdTarget.getVisibility() == View.VISIBLE) {
            holdTargetRect = new Rect(holdTarget.getLeft(), holdTarget.getTop(),
                    holdTarget.getRight(), holdTarget.getBottom());
        }
        if (answerTargetRect == null && answerTarget.getVisibility() == View.VISIBLE) {
            answerTargetRect = new Rect(answerTarget.getLeft(), answerTarget.getTop(),
                    answerTarget.getRight(), answerTarget.getBottom());
        }
        if (xferTargetRect == null && xferTarget.getVisibility() == View.VISIBLE) {
            xferTargetRect = new Rect(xferTarget.getLeft(), xferTarget.getTop(),
                    xferTarget.getRight(), xferTarget.getBottom());
        }

        // Rectangle matching

        if (endCallTargetRect != null && endCallTargetRect.contains(X, Y)) {
            // Drop in end call zone
            onTrigger(call.isIncoming() && call.isBeforeConfirmed() ? DECLINE_CALL : CLEAR_CALL,
                    call);
        } else if (holdTargetRect != null && holdTargetRect.contains(X, Y)) {
            // check if not drop on held call
            boolean dropOnOtherCall = false;
            for (Entry<Integer, InCallInfo> badgeSet : badges.entrySet()) {
                Log.d(THIS_FILE, "On drop target searching for another badge");
                int callId = badgeSet.getKey();
                if (callId != call.getCallId()) {
                    Log.d(THIS_FILE, "found a different badge than self");
                    SipCallSession callInfo = getCallInfo(callId);
                    if (callInfo.isLocalHeld()) {
                        Log.d(THIS_FILE, "Other badge is hold");
                        InCallInfo otherBadge = badgeSet.getValue();
                        Rect r = new Rect(otherBadge.getLeft(), otherBadge.getTop(),
                                otherBadge.getRight(), otherBadge.getBottom());
                        Log.d(THIS_FILE, "Current X, Y " + X + ", " + Y + " -- " + r.top + ", "
                                + r.left + ", " + r.right + ", " + r.bottom);
                        if (r.contains(X, Y)) {
                            Log.d(THIS_FILE, "Yep we've got one");
                            dropOnOtherCall = true;
                            if (service != null) {
                                try {
                                    // 1 = PJSUA_XFER_NO_REQUIRE_REPLACES
                                    service.xferReplace(call.getCallId(), callId, 1);
                                } catch (RemoteException e) {
                                    // TODO : toaster
                                }
                            }
                        }
                    }
                }
            }

            // Drop in hold zone

            if (!dropOnOtherCall && !call.isLocalHeld()) {
                onTrigger(TOGGLE_HOLD, call);
            }
        } else if (answerTargetRect != null && answerTargetRect.contains(X, Y)) {
            if (call.isIncoming() && call.isBeforeConfirmed()) {
                onTrigger(TAKE_CALL, call);
            }
        } else if (xferTargetRect != null && xferTargetRect.contains(X, Y)) {
            if (!call.isBeforeConfirmed() && !call.isAfterEnded()) {
                onTrigger(XFER_CALL, call);
            }

        } else {
            Log.d(THIS_FILE, "Drop is done somewhere else " + call.getMediaStatus());
            // Drop somewhere else
            if (call.isLocalHeld()) {
                Log.d(THIS_FILE, "Try to unhold");
                onTrigger(TOGGLE_HOLD, call);
            }
        }
        updateUIFromCall();
    }

    private class DraggingInfo {
        public boolean isDragging = false;
        // public InCallInfo2 badge;
        public SipCallSession call;

        public DraggingInfo(boolean aIsDragging, InCallInfo aBadge, SipCallSession aCall) {
            isDragging = aIsDragging;
            // badge = aBadge;
            call = aCall;
        }
    }

    private void showZRTPInfo(final int dataPtr, String sasString) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Resources r = getResources();
        builder.setTitle("ZRTP supported by remote party");
        builder.setMessage("Do you confirm the SAS : " + sasString);
        builder.setPositiveButton(r.getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(THIS_FILE, "ZRTP confirmed");

                if (service != null) {
                    try {
                        service.zrtpSASVerified(dataPtr);
                    } catch (RemoteException e) {
                        Log.e(THIS_FILE, "Error while calling service", e);
                    }
                    dialog.dismiss();
                }
            }
        });
        builder.setNegativeButton(r.getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog backupDialog = builder.create();
        backupDialog.show();
    }

    
    @Override
    public boolean shouldActivateProximity() {

        // TODO : missing headset & keyboard open
        if(lastMediaState != null) {
            if(lastMediaState.isBluetoothScoOn) {
                return false;
            }
            if(lastMediaState.isSpeakerphoneOn && ! useAutoDetectSpeaker) {
                // Imediate reason to not enable proximity sensor
                return false;
            }
        }
        
        if (callsInfo == null) {
            return false;
        }

        boolean isValidCallState = true;
        int count = 0;
        for (SipCallSession callInfo : callsInfo) {
            if(!callInfo.isAfterEnded()) {
                int state = callInfo.getCallState();
                
                isValidCallState &= (
                        (state == SipCallSession.InvState.CONFIRMED) ||
                        (state == SipCallSession.InvState.CONNECTING) ||
                        (state == SipCallSession.InvState.CALLING) ||
                        (state == SipCallSession.InvState.EARLY && !callInfo.isIncoming())
                        );
                count ++;
            }
        }
        if(count == 0) {
            return false;
        }

        return isValidCallState;
    }

    @Override
    public void onProximityTrackingChanged(boolean acquired) {
        if(useAutoDetectSpeaker) {
            if(acquired) {
                if(lastMediaState == null || lastMediaState.isSpeakerphoneOn) {
                    onTrigger(SPEAKER_OFF, null);
                }
            }
            if(!acquired) {
                if(lastMediaState == null || !lastMediaState.isSpeakerphoneOn) {
                    onTrigger(SPEAKER_ON, null);
                }
            }
        }
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
            int oldTop, int oldRight, int oldBottom) {
        Log.d(THIS_FILE, "Layouting main view");
        if(cameraPreview != null) {
            cameraPreview.setVisibility(View.GONE);
        }
        updateUIFromCall();
    }
}
