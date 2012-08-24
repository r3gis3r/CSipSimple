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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
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
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;

import com.actionbarsherlock.app.SherlockFragmentActivity;
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
import com.csipsimple.ui.incall.DtmfDialogFragment.OnDtmfListener;
import com.csipsimple.utils.CallsUtils;
import com.csipsimple.utils.DialingFeedback;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.Theme;
import com.csipsimple.utils.keyguard.KeyguardWrapper;
import com.csipsimple.widgets.IOnLeftRightChoice;
import com.csipsimple.widgets.ScreenLocker;

import org.webrtc.videoengine.ViERenderer;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class InCallActivity extends SherlockFragmentActivity implements IOnCallActionTrigger, 
        IOnLeftRightChoice, ProximityDirector, OnDtmfListener {
    private static final int QUIT_DELAY = 3000;
    private final static String THIS_FILE = "InCallActivity";
    //private final static int DRAGGING_DELAY = 150;
    

    private Object callMutex = new Object();
    private SipCallSession[] callsInfo = null;
    private MediaState lastMediaState;
    
    
    private ViewGroup mainFrame;
    private InCallControls inCallControls;

    // Screen wake lock for incoming call
    private WakeLock wakeLock;
    // Screen wake lock for video
    private WakeLock videoWakeLock;

    private InCallInfoGrid activeCallsGrid;
    private Timer quitTimer;

    // private LinearLayout detailedContainer, holdContainer;

    // True if running unit tests
    // private boolean inTest;


    private DialingFeedback dialFeedback;
    private PowerManager powerManager;
    private PreferencesProviderWrapper prefsWrapper;

    // Dnd views
    //private ImageView endCallTarget, holdTarget, answerTarget, xferTarget;
    //private Rect endCallTargetRect, holdTargetRect, answerTargetRect, xferTargetRect;
    

    private SurfaceView cameraPreview;
    private CallProximityManager proximityManager;
    private KeyguardWrapper keyguardManager;
    
    private boolean useAutoDetectSpeaker = false;
    private InCallAnswerControls inCallAnswerControls;
    private CallsAdapter activeCallsAdapter;
    private InCallInfoGrid heldCallsGrid;
    private CallsAdapter heldCallsAdapter;

    private final static int PICKUP_SIP_URI_XFER = 0;
    private final static int PICKUP_SIP_URI_NEW_CALL = 1;
    private static final String CALL_ID = "call_id";
    

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //handler.setActivityInstance(this);
        Log.d(THIS_FILE, "Create in call");
        setContentView(R.layout.in_call_main);

        SipCallSession initialSession = getIntent().getParcelableExtra(SipManager.EXTRA_CALL_INFO);
        synchronized (callMutex) {
            callsInfo = new SipCallSession[1];
            callsInfo[0] = initialSession;
        }

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
        
        // Cache findViews
        mainFrame = (ViewGroup) findViewById(R.id.mainFrame);
        inCallControls = (InCallControls) findViewById(R.id.inCallControls);
        inCallAnswerControls = (InCallAnswerControls) findViewById(R.id.inCallAnswerControls);
        activeCallsGrid = (InCallInfoGrid) findViewById(R.id.activeCallsGrid);
        heldCallsGrid = (InCallInfoGrid) findViewById(R.id.heldCallsGrid);

        // Bind
        attachVideoPreview();

        inCallControls.setOnTriggerListener(this);
        inCallAnswerControls.setOnTriggerListener(this);

        if(activeCallsAdapter == null) {
            activeCallsAdapter = new CallsAdapter(true);
        }
        activeCallsGrid.setAdapter(activeCallsAdapter);
        

        if(heldCallsAdapter == null) {
            heldCallsAdapter = new CallsAdapter(false);
        }
        heldCallsGrid.setAdapter(heldCallsAdapter);

        
        ScreenLocker lockOverlay = (ScreenLocker) findViewById(R.id.lockerOverlay);
        lockOverlay.setActivity(this);
        lockOverlay.setOnLeftRightListener(this);
        
        /*
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
        */

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

        
        useAutoDetectSpeaker = prefsWrapper.getPreferenceBooleanValue(SipConfigManager.AUTO_DETECT_SPEAKER);
        
        applyTheme();
        proximityManager.startTracking();
        
        inCallControls.setCallState(initialSession);
        inCallAnswerControls.setCallState(initialSession);
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
        /*
        endCallTargetRect = null;
        holdTargetRect = null;
        answerTargetRect = null;
        xferTargetRect = null;
        */
        dialFeedback.resume();
        

        runOnUiThread(new UpdateUIFromCallRunnable());
        
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

        if(infoDialog != null) {
            infoDialog.dismiss();
        }
        
        if (quitTimer != null) {
            quitTimer.cancel();
            quitTimer.purge();
            quitTimer = null;
        }
        /*
        if (draggingTimer != null) {
            draggingTimer.cancel();
            draggingTimer.purge();
            draggingTimer = null;
        }
        */

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
        
        if(activeCallsGrid != null) {
            activeCallsGrid.terminate();
        }
        
        detachVideoPreview();

        //handler.setActivityInstance(null);
        
        super.onDestroy();
    }
    
    @SuppressWarnings("deprecation")
    private void attachVideoPreview() {

        // Video stuff
        if(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_VIDEO)) {
            if(cameraPreview == null) {
                Log.d(THIS_FILE, "Create Local Renderer");
                cameraPreview = ViERenderer.CreateLocalRenderer(this);
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(256, 256);
                //lp.leftMargin = 2;
                //lp.topMargin= 4;
                lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                cameraPreview.setVisibility(View.GONE);
                mainFrame.addView(cameraPreview, lp);
            }else {
                Log.d(THIS_FILE, "NO NEED TO Create Local Renderer");
            }
            
            if(videoWakeLock == null) {
                videoWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "com.csipsimple.videoCall");
                videoWakeLock.setReferenceCounted(false);
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

        if(videoWakeLock != null && videoWakeLock.isHeld()) {
            videoWakeLock.release();
        }
        if(cameraPreview != null) {
            cameraPreview = null;
        }
        
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
        runOnUiThread(new UpdateUIFromCallRunnable());
    }

    private void applyTheme() {
        Theme t = Theme.getCurrentTheme(this);
        if (t != null) {
            // TODO ...
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICKUP_SIP_URI_XFER:
                if (resultCode == RESULT_OK && service != null) {
                    String callee = data.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                    int callId = data.getIntExtra(CALL_ID, -1);
                    if(callId != -1) {
                        try {
                            service.xfer((int) callId, callee);
                        } catch (RemoteException e) {
                            // TODO : toaster
                        }
                    }
                }
                return;
            case PICKUP_SIP_URI_NEW_CALL:
                if (resultCode == RESULT_OK && service != null) {
                    String callee = data.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                    long accountId = data.getLongExtra(SipProfile.FIELD_ID,
                            SipProfile.INVALID_ID);
                    if (accountId != SipProfile.INVALID_ID) {
                        try {
                            service.makeCall(callee, (int) accountId);
                        } catch (RemoteException e) {
                            // TODO : toaster
                        }
                    }
                }
                return;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
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
     * Get the call with the higher priority comparing two calls
     * @param call1 First call object to compare
     * @param call2 Second call object to compare
     * @return The call object with highest priority
     */
    private SipCallSession getPrioritaryCall(SipCallSession call1, SipCallSession call2) {
        // We prefer the not null
        if (call1 == null) {
            return call2;
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
    private class UpdateUIFromCallRunnable implements Runnable {
        
        @Override
        public void run() {
            
            
            // Current call is the call emphasis by the UI.
            SipCallSession mainCallInfo = null;
            boolean showCameraPreview = false;
    
            int mainsCalls = 0;
            int heldsCalls = 0;
    
            synchronized (callMutex) {
                
                if (callsInfo != null) {
                    for (SipCallSession callInfo : callsInfo) {
                        Log.d(THIS_FILE,
                                "We have a call " + callInfo.getCallId() + " / " + callInfo.getCallState()
                                        + "/" + callInfo.getMediaStatus());
        
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
            }
            
            // Update call control visibility - must be done before call cards 
            // because badge avail size depends on that
            if ((mainsCalls + heldsCalls) >= 1) {
                // Update in call actions
                inCallControls.setCallState(mainCallInfo);
                inCallAnswerControls.setCallState(mainCallInfo);
            } else {
                inCallControls.setCallState(null);
                inCallAnswerControls.setCallState(null);
            }
            
            heldCallsGrid.setVisibility((heldsCalls > 0)? View.VISIBLE : View.GONE);
            
            activeCallsAdapter.notifyDataSetChanged();
            heldCallsAdapter.notifyDataSetChanged();
            
            
            //findViewById(R.id.inCallContainer).requestLayout();
            
            if (mainCallInfo != null) {
                Log.d(THIS_FILE, "Active call is " + mainCallInfo.getCallId());
                Log.d(THIS_FILE, "Update ui from call " + mainCallInfo.getCallId() + " state "
                        + CallsUtils.getStringCallState(mainCallInfo, InCallActivity.this));
                int state = mainCallInfo.getCallState();
    
                //int backgroundResId = R.drawable.bg_in_call_gradient_unidentified;
    
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
                        break;
                    case SipCallSession.InvState.NULL:
                    case SipCallSession.InvState.DISCONNECTED:
                        Log.d(THIS_FILE, "Active call session is disconnected or null wait for quit...");
                        // This will release locks
                        delayedQuit();
                        return;
    
                }
                
                Log.d(THIS_FILE, "we leave the update ui function");
            }
            
            proximityManager.updateProximitySensorMode();
            
            
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
    }

    /**
     * Update ui from media state.
     */
    private class UpdateUIFromMediaRunnable implements Runnable {
        @Override
        public void run() {
            inCallControls.setMediaState(lastMediaState);
            proximityManager.updateProximitySensorMode();
        }
    }
    

    /*
    private void setSubViewVisibilitySafely(int id, boolean visible) {
        View v = findViewById(id);
        if(v != null) {
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    private class UpdateDraggingRunnable implements Runnable {
        private DraggingInfo di;
        
        UpdateDraggingRunnable(DraggingInfo draggingInfo){
            di = draggingInfo;
        }
        
        public void run() {
            inCallControls.setVisibility(di.isDragging ? View.GONE : View.VISIBLE);
            findViewById(R.id.dropZones).setVisibility(di.isDragging ? View.VISIBLE : View.GONE);
            
            setSubViewVisibilitySafely(R.id.dropHangup, di.isDragging);
            setSubViewVisibilitySafely(R.id.dropHold, (di.isDragging && di.call.isActive() && !di.call.isBeforeConfirmed()));
            setSubViewVisibilitySafely(R.id.dropAnswer, (di.call.isActive() && di.call.isBeforeConfirmed()
                    && di.call.isIncoming() && di.isDragging));
            setSubViewVisibilitySafely(R.id.dropXfer, (!di.call.isBeforeConfirmed()
                    && !di.call.isAfterEnded() && di.isDragging));
            
        }
    }
    */
    
    private synchronized void delayedQuit() {

        if (wakeLock != null && wakeLock.isHeld()) {
            Log.d(THIS_FILE, "Releasing wake up lock");
            wakeLock.release();
        }
        
        proximityManager.release(0);
        
        activeCallsGrid.setVisibility(View.VISIBLE);
        inCallControls.setVisibility(View.GONE);

        Log.d(THIS_FILE, "Start quit timer");
        if (quitTimer != null) {
            quitTimer.schedule(new QuitTimerTask(), QUIT_DELAY);
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

    private void showDialpad(int callId) {
        DtmfDialogFragment newFragment = DtmfDialogFragment.newInstance(callId);
        newFragment.show(getSupportFragmentManager(), "dialog");
    }
    


    @Override
    public void OnDtmf(int callId, int keyCode, int dialTone) {
        proximityManager.restartTimer();

        if (service != null) {
            if (callId != SipCallSession.INVALID_CALL_ID) {
                try {
                    service.sendDtmf(callId, keyCode);
                    dialFeedback.giveFeedback(dialTone);
                } catch (RemoteException e) {
                    Log.e(THIS_FILE, "Was not able to send dtmf tone", e);
                }
            }
        }
        
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
                return inCallAnswerControls.onKeyDown(keyCode, event);
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
                return inCallAnswerControls.onKeyDown(keyCode, event);

        }
        return super.onKeyUp(keyCode, event);
    }


    private BroadcastReceiver callStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(SipManager.ACTION_SIP_CALL_CHANGED)) {
                if (service != null) {
                    try {
                        synchronized (callMutex) {
                            callsInfo = service.getCalls();
                            runOnUiThread(new UpdateUIFromCallRunnable());
                        }
                    } catch (RemoteException e) {
                        Log.e(THIS_FILE, "Not able to retrieve calls");
                    }
                }
            } else if (action.equals(SipManager.ACTION_SIP_MEDIA_CHANGED)) {
                if (service != null) {
                    MediaState mediaState;
                    try {
                        mediaState = service.getCurrentMediaState();
                        Log.d(THIS_FILE, "Media update ...." + mediaState.isSpeakerphoneOn);
                        synchronized (callMutex) {
                            if (!mediaState.equals(lastMediaState)) {
                                lastMediaState = mediaState;
                                runOnUiThread(new UpdateUIFromMediaRunnable());
                            }   
                        }
                    } catch (RemoteException e) {
                        Log.e(THIS_FILE, "Can't get the media state ", e);
                    }
                }
            } else if (action.equals(SipManager.ACTION_ZRTP_SHOW_SAS)) {
                runOnUiThread(new ShowZRTPInfoRunnable(intent.getIntExtra(Intent.EXTRA_UID, 0), intent.getStringExtra(Intent.EXTRA_SUBJECT)));
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

                runOnUiThread(new UpdateUIFromCallRunnable());
                runOnUiThread(new UpdateUIFromMediaRunnable());
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
    private AlertDialog infoDialog;

    // private boolean showDetails = true;


    @Override
    public void onTrigger(int whichAction, final SipCallSession call) {

        // Sanity check for actions requiring valid call id
        if (whichAction == TAKE_CALL || whichAction == DECLINE_CALL ||
            whichAction == CLEAR_CALL || whichAction == DETAILED_DISPLAY || 
            whichAction == TOGGLE_HOLD || whichAction == START_RECORDING ||
            whichAction == STOP_RECORDING || whichAction == DTMF_DISPLAY ||
            whichAction == XFER_CALL || whichAction == TRANSFER_CALL ||
            whichAction == START_VIDEO || whichAction == STOP_VIDEO ) {
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
                        Log.d(THIS_FILE, "Manually switch to speaker");
                        useAutoDetectSpeaker = false;
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
                case DTMF_DISPLAY: {
                    showDialpad(call.getCallId());
                    break;
                }
                case DETAILED_DISPLAY: {
                    if (service != null) {
                        if(infoDialog != null) {
                            infoDialog.dismiss();
                        }
                        String infos = service.showCallInfosDialog(call.getCallId());
                        
                        SpannableStringBuilder buf = new SpannableStringBuilder();
                        Builder builder = new AlertDialog.Builder(this);

                        buf.append(infos);
                        TextAppearanceSpan textSmallSpan = new TextAppearanceSpan(this,
                                android.R.style.TextAppearance_Small);
                        buf.setSpan(textSmallSpan, 0, buf.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        infoDialog = builder.setIcon(android.R.drawable.ic_dialog_info)
                                .setMessage(buf)
                                .setNeutralButton(R.string.ok, null)
                                .create();
                        infoDialog.show();
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
                    pickupIntent.putExtra(CALL_ID, call.getCallId());
                    startActivityForResult(pickupIntent, PICKUP_SIP_URI_XFER);
                    break;
                }
                case TRANSFER_CALL: {
                    final ArrayList<SipCallSession> remoteCalls = new ArrayList<SipCallSession>();
                    if(callsInfo != null) {
                        for(SipCallSession remoteCall : callsInfo) {
                            // Verify not current call
                            if(remoteCall.getCallId() != call.getCallId() && remoteCall.isOngoing()) {
                                remoteCalls.add(remoteCall);
                            }
                        }
                    }

                    if(remoteCalls.size() > 0) {
                        Builder builder = new AlertDialog.Builder(this);
                        CharSequence[] simpleAdapter = new String[remoteCalls.size()];
                        for(int i = 0; i < remoteCalls.size(); i++) {
                            simpleAdapter[i] = remoteCalls.get(i).getRemoteContact();
                        }
                        builder.setSingleChoiceItems(simpleAdapter , -1, new Dialog.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (service != null) {
                                            try {
                                                // 1 = PJSUA_XFER_NO_REQUIRE_REPLACES
                                                service.xferReplace(call.getCallId(), remoteCalls.get(which).getCallId(), 1);
                                            } catch (RemoteException e) {
                                                Log.e(THIS_FILE, "Was not able to call service method", e);
                                            }
                                        }
                                        dialog.dismiss();
                                    }
                                })
                                .setCancelable(true)
                                .setNeutralButton(R.string.cancel, new Dialog.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                    }
                    
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
                case START_VIDEO :
                case STOP_VIDEO : {
                    if(service != null) {
                        Bundle opts = new Bundle();
                        opts.putBoolean(SipCallSession.OPT_CALL_VIDEO, whichAction == START_VIDEO);
                        service.updateCallOptions(call.getCallId(), opts);
                    }
                    break;
                }
            }
        } catch (RemoteException e) {
            Log.e(THIS_FILE, "Was not able to call service method", e);
        }
    }
    


    @Override
    public void onLeftRightChoice(int whichHandle) {
        switch (whichHandle) {
            case LEFT_HANDLE:
                Log.d(THIS_FILE, "We unlock");
                proximityManager.release(0);
                proximityManager.restartTimer();
                break;
            case RIGHT_HANDLE:
                Log.d(THIS_FILE, "We clear the call");
                onTrigger(IOnCallActionTrigger.CLEAR_CALL, getActiveCallInfo());
                proximityManager.release(0);
            default:
                break;
        }

    }

    /*
    // Drag and drop feature
    private Timer draggingTimer;

    public class OnBadgeTouchListener implements OnTouchListener {
        private SipCallSession call;
        private InCallCard badge;
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

        public OnBadgeTouchListener(InCallCard aBadge, SipCallSession aCall) {
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
            DraggingInfo di = new DraggingInfo(isDragging,
                    badge, call);
            runOnUiThread(new UpdateDraggingRunnable(di));
        }
        

        public void setCallState(SipCallSession callInfo) {
            Log.d(THIS_FILE,
                    "Updated call infos : " + call.getCallState() + " and " + call.getMediaStatus()
                            + " et " + call.isLocalHeld());
            call = callInfo;
        }
    }
    
    private Rect getViewRect(int id) {
        View v = findViewById(id);
        if(v != null && v.getVisibility() == View.VISIBLE) {
            return new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        }
        return null;
    }

    private void onDropBadge(int X, int Y, InCallCard badge, SipCallSession call) {
        Log.d(THIS_FILE, "Dropping !!! in " + X + ", " + Y);

        // Rectangle init if not already done
        if (endCallTargetRect == null) {
            endCallTargetRect = getViewRect(R.id.dropHangup);
        }
        if (holdTargetRect == null) {
            holdTargetRect = getViewRect(R.id.dropHold);
        }
        if (answerTargetRect == null) {
            answerTargetRect = getViewRect(R.id.dropAnswer);
        }
        if (xferTargetRect == null) {
            xferTargetRect = getViewRect(R.id.dropXfer);
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
        runOnUiThread(new UpdateUIFromMediaRunnable());
    }

    private class DraggingInfo {
        public boolean isDragging = false;
        // public InCallInfo2 badge;
        public SipCallSession call;

        public DraggingInfo(boolean aIsDragging, InCallCard aBadge, SipCallSession aCall) {
            isDragging = aIsDragging;
            // badge = aBadge;
            call = aCall;
        }
    }
    */
    
    private class ShowZRTPInfoRunnable implements Runnable, DialogInterface.OnClickListener {
        private String sasString;
        private int dataPtr;

        public ShowZRTPInfoRunnable(int aDataPtr, String sas) {
            dataPtr = aDataPtr;
            sasString = sas;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if(which == DialogInterface.BUTTON_POSITIVE) {
                Log.d(THIS_FILE, "ZRTP confirmed");
                if (service != null) {
                    try {
                        service.zrtpSASVerified(dataPtr);
                    } catch (RemoteException e) {
                        Log.e(THIS_FILE, "Error while calling service", e);
                    }
                    dialog.dismiss();
                }
            }else if(which == DialogInterface.BUTTON_NEGATIVE) {
                dialog.dismiss();
            }
        }
        @Override
        public void run() {
            AlertDialog.Builder builder = new AlertDialog.Builder(InCallActivity.this);
            Resources r = getResources();
            builder.setTitle("ZRTP supported by remote party");
            builder.setMessage("Do you confirm the SAS : " + sasString);
            builder.setPositiveButton(r.getString(R.string.yes), this);
            builder.setNegativeButton(r.getString(R.string.no), this);

            AlertDialog backupDialog = builder.create();
            backupDialog.show();
        }
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
        if(useAutoDetectSpeaker && service != null) {
            if(acquired) {
                if(lastMediaState == null || lastMediaState.isSpeakerphoneOn) {
                    try {
                        service.setSpeakerphoneOn(false);
                    } catch (RemoteException e) {
                        Log.e(THIS_FILE, "Can't run speaker change");
                    }
                }
            }else {
                if(lastMediaState == null || !lastMediaState.isSpeakerphoneOn) {
                    try {
                        service.setSpeakerphoneOn(true);
                    } catch (RemoteException e) {
                        Log.e(THIS_FILE, "Can't run speaker change");
                    }
                }
            }
        }
    }

    
    // Active call adapter
    private class CallsAdapter extends BaseAdapter {
        
        private boolean mActiveCalls;
        
        private SparseArray<Long> seenConnected = new SparseArray<Long>();
        
        public CallsAdapter(boolean notOnHold) {
            mActiveCalls = notOnHold;
        }

        private boolean isValidCallForAdapter(SipCallSession call) {
            boolean holdStateOk = false;
            if(mActiveCalls && !call.isLocalHeld()) {
                holdStateOk = true;
            }
            if(!mActiveCalls && call.isLocalHeld()) {
                holdStateOk = true;
            }
            if(holdStateOk) {
                long currentTime = System.currentTimeMillis();
                if(call.isAfterEnded()) {
                    // Only valid if we already seen this call in this adapter to be valid
                    if(hasNoMoreActiveCall() && seenConnected.get(call.getCallId(), currentTime + 2 * QUIT_DELAY) < currentTime + QUIT_DELAY) {
                        return true;
                    }else {
                        seenConnected.delete(call.getCallId());
                        return false;
                    }
                }else {
                    seenConnected.put(call.getCallId(), currentTime);
                    return true;
                }
            }
            return false;
        }
        
        private boolean hasNoMoreActiveCall() {
            synchronized (callMutex) {
                if(callsInfo == null) {
                    return true;
                }
                
                for(SipCallSession call : callsInfo) {
                    // As soon as we have one not after ended, we have at least active call
                    if(!call.isAfterEnded()) {
                        return false;
                    }
                }
                
            }
            return true;
        }
        
        @Override
        public int getCount() {
            int count = 0;
            synchronized (callMutex) {
                if(callsInfo == null) {
                    return 0;
                }
                
                for(SipCallSession call : callsInfo) {
                    if(isValidCallForAdapter(call)) {
                        count ++;
                    }
                }
            }
            return count;
        }

        @Override
        public Object getItem(int position) {
            synchronized (callMutex) {
                if(callsInfo == null) {
                    return null;
                }
                int count = 0;
                for(SipCallSession call : callsInfo) {
                    if(isValidCallForAdapter(call)) {
                        if(count == position) {
                            return call;
                        }
                        count ++;
                     }
                }
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            SipCallSession call = (SipCallSession) getItem(position);
            if(call != null) {
                return call.getCallId();
            }
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = new InCallCard(InCallActivity.this, null);
            }
            
            if(convertView instanceof InCallCard) {
                InCallCard vc = (InCallCard) convertView;
                vc.setOnTriggerListener(InCallActivity.this);
                // TODO ---
                //badge.setOnTouchListener(new OnBadgeTouchListener(badge, call));
                
                SipCallSession session = (SipCallSession) getItem(position);
                Log.d(THIS_FILE, "Set call state : " + session.getCallState());
                vc.setCallState(session);
            }

            return convertView;
        }
        
    }

}
