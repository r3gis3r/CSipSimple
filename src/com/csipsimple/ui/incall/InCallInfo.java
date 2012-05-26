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

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.models.CallerInfo;
import com.csipsimple.service.SipService;
import com.csipsimple.ui.incall.InCallActivity.OnBadgeTouchListener;
import com.csipsimple.ui.incall.InCallControls.OnTriggerListener;
import com.csipsimple.utils.CallsUtils;
import com.csipsimple.utils.ContactsAsyncHelper;
import com.csipsimple.utils.CustomDistribution;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.widgets.ExtensibleBadge;

import org.webrtc.videoengine.ViERenderer;

public class InCallInfo extends ExtensibleBadge {

    private static final String THIS_FILE = "InCallInfo";
    private SipCallSession callInfo;
    private String cachedRemoteUri = "";
    private int cachedInvState = SipCallSession.InvState.INVALID;
    private int cachedMediaState = SipCallSession.MediaState.ERROR;
    private boolean cachedCanRecord = false;
    private boolean cachedIsRecording = false;
    private ImageView photo, callIcon;
    private TextView remoteName, status;// , title;
    private Chronometer elapsedTime;
    private int colorConnected, colorEnd;
    //private View[] bottomViews = new View[] {};
    
    // private TextView remotePhoneNumber;
    private TextView secureInfo;
    private SurfaceView renderView;
    private int callCardBorderWidth;
    private int callCardBorderHeight;
    private PreferencesProviderWrapper prefs;
    

    public InCallInfo(Context context, AttributeSet attrs) {
        super(context, attrs);
        initControllerView();

        prefs = new PreferencesProviderWrapper(context);
        
        // Width : only border
        callCardBorderWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        
        // Height : border + 2 text + bottom arrow
        callCardBorderHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        callCardBorderHeight += (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35, getResources().getDisplayMetrics()) * 2;
        callCardBorderHeight += (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 19, getResources().getDisplayMetrics());
    }

    private void initControllerView() {
        photo = (ImageView) findViewById(R.id.card_img);
        remoteName = (TextView) findViewById(R.id.card_label);
        // title = (TextView) findViewById(R.id.title);
        elapsedTime = (Chronometer) findViewById(R.id.elapsedTime);
        status = (TextView) findViewById(R.id.card_status);
        callIcon = (ImageView) findViewById(R.id.callStatusIcon);
        secureInfo = (TextView) findViewById(R.id.secureIndicator);
        /*
        bottomViews = new View[] {
                findViewById(R.id.handle),
                findViewById(R.id.card_status),
                findViewById(R.id.card_label)
        };
        */

        // currentInfo = (LinearLayout) findViewById(R.id.currentCallInfo);
        // currentDetailedInfo = (LinearLayout)
        // findViewById(R.id.currentCallDetailedInfo);

        // Colors
        colorConnected = Color.parseColor("#99CE3F");
        colorEnd = Color.parseColor("#FF6072");

        // secure.bringToFront();

    }

    public void setCallState(SipCallSession aCallInfo) {
        callInfo = aCallInfo;
        if (callInfo == null) {
            updateElapsedTimer();
            return;
        }

        updateRemoteName();
        updateTitle();
        updateQuickActions();
        updateElapsedTimer();

        cachedInvState = callInfo.getCallState();
        cachedMediaState = callInfo.getMediaStatus();
        cachedCanRecord = callInfo.canRecord();
        cachedIsRecording = callInfo.isRecording();

        
        // VIDEO STUFF -- EXPERIMENTAL
        if(prefs.getPreferenceBooleanValue(SipConfigManager.USE_VIDEO)) {
            if (callInfo.getCallId() >= 0 && callInfo.mediaHasVideo()) {
                if (renderView == null) {
                    renderView = ViERenderer.CreateRenderer(getContext(), true);
                    photo.setVisibility(View.GONE);
                    FrameLayout container = (FrameLayout) findViewById(R.id.card_img_container);
                    ViewGroup.LayoutParams photoLp = photo.getLayoutParams();
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                            photoLp.width,
                            photoLp.height);
                    renderView.setLayoutParams(lp);
                    container.addView(renderView, 0);
    
                }
                
                SipService.setVideoWindow(callInfo.getCallId(), renderView);
            }else {
                if(renderView != null) {
                    renderView.setVisibility(View.GONE);
                    photo.setVisibility(View.VISIBLE);
                }
            }
        }
        
        // End of video stuff
        dragListener.setCallState(callInfo);
    }

    private synchronized void updateQuickActions() {
        // Useless to process that
        if (cachedInvState == callInfo.getCallState() &&
                cachedMediaState == callInfo.getMediaStatus() &&
                cachedIsRecording == callInfo.isRecording() &&
                cachedCanRecord == callInfo.canRecord()) {
            return;
        }

        removeAllItems();

        // Add items if possible

        // Take/decline items
        if (callInfo.isBeforeConfirmed()) {
            // Answer
            if (callInfo.isIncoming()) {
                addItem(R.drawable.ic_in_call_touch_answer,
                        getContext().getString(R.string.take_call), new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dispatchTriggerEvent(OnTriggerListener.TAKE_CALL);
                                collapse();
                            }
                        });
            }
            // Decline
            addItem(R.drawable.ic_in_call_touch_end, getContext().getString(R.string.decline_call),
                    new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dispatchTriggerEvent(OnTriggerListener.DECLINE_CALL);
                            collapse();
                        }
                    });
        }

        // In comm items
        if (!callInfo.isAfterEnded() && !callInfo.isBeforeConfirmed()) {
            // End
            addItem(R.drawable.ic_in_call_touch_end, getContext().getString(R.string.clear_call),
                    new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dispatchTriggerEvent(OnTriggerListener.CLEAR_CALL);
                            collapse();
                        }
                    });

            // Hold
            addItem(callInfo.isLocalHeld() ? R.drawable.ic_in_call_touch_unhold
                    : R.drawable.ic_in_call_touch_hold, callInfo.isLocalHeld() ? "Unhold" : "Hold",
                    new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dispatchTriggerEvent(OnTriggerListener.TOGGLE_HOLD);
                            collapse();
                        }
                    });
        }

        // Info item
        if (!callInfo.isAfterEnded()) {
            addItem(R.drawable.ic_in_call_touch_round_details, R.string.info, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTriggerEvent(OnTriggerListener.DETAILED_DISPLAY);
                    collapse();
                }
            });
        }

        if(CustomDistribution.supportCallRecord()) {
            if(callInfo.canRecord() && ! callInfo.isRecording()) {
                addItem(R.drawable.record, R.string.record, new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dispatchTriggerEvent(OnTriggerListener.START_RECORDING);
                        collapse();
                    }
                });
            }else if(callInfo.isRecording()) {
                addItem(R.drawable.stop, R.string.stop_recording, new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dispatchTriggerEvent(OnTriggerListener.STOP_RECORDING);
                        collapse();
                    }
                });
            }
        }

    }

    /**
     * Bind the main visible view with data from call info
     */
    private synchronized void updateTitle() {
        // Useless to process that
        if (cachedInvState == callInfo.getCallState() &&
                cachedMediaState == callInfo.getMediaStatus()) {
            return;
        }

        int stateIcon = R.drawable.ic_incall_ongoing;
        if (callInfo.isAfterEnded()) {
            stateIcon = R.drawable.ic_incall_end;
        } else if (callInfo.isLocalHeld() || callInfo.isRemoteHeld()) {
            stateIcon = R.drawable.ic_incall_onhold;
        } else if (callInfo.isBeforeConfirmed()) {
            if (callInfo.isIncoming()) {
                stateIcon = R.drawable.ic_call_log_header_incoming_call;
            } else {
                stateIcon = R.drawable.ic_call_log_header_outgoing_call;
            }
        }
        callIcon.setImageResource(stateIcon);
        callIcon.setContentDescription(CallsUtils.getStringCallState(callInfo, getContext()));

    }

    private synchronized void updateRemoteName() {

        final String aRemoteUri = callInfo.getRemoteContact();

        // If not already set with the same value, just ignore it
        if (aRemoteUri != null && !aRemoteUri.equalsIgnoreCase(cachedRemoteUri)) {
            cachedRemoteUri = aRemoteUri;
            ParsedSipContactInfos uriInfos = SipUri.parseSipContact(cachedRemoteUri);
            String text = SipUri.getDisplayedSimpleContact(aRemoteUri);

            StringBuffer statusTextBuffer = new StringBuffer();

            remoteName.setText(text);
            if (callInfo.getAccId() != SipProfile.INVALID_ID) {
                SipProfile acc = SipProfile.getProfileFromDbId(getContext(), callInfo.getAccId(),
                        new String[] {
                                SipProfile.FIELD_ID, SipProfile.FIELD_DISPLAY_NAME
                        });
                if (acc != null && acc.display_name != null) {
                    statusTextBuffer.append("SIP/" + acc.display_name + " : ");
                }
            } else {
                statusTextBuffer.append("SIP : ");
            }

            statusTextBuffer.append(uriInfos.userName);
            status.setText(statusTextBuffer.toString());

            Thread t = new Thread() {
                public void run() {
                    // Looks like a phone number so search the contact throw
                    // contacts
                    CallerInfo callerInfo = CallerInfo.getCallerInfoFromSipUri(getContext(),
                            cachedRemoteUri);
                    if (callerInfo != null && callerInfo.contactExists) {
                        userHandler.sendMessage(userHandler.obtainMessage(LOAD_CALLER_INFO,
                                callerInfo));
                    }
                };
            };
            t.start();

        }
    }

    private void updateElapsedTimer() {

        if (callInfo == null) {
            elapsedTime.stop();
            elapsedTime.setVisibility(VISIBLE);
            elapsedTime.setTextColor(colorEnd);
            return;
        }

        elapsedTime.setBase(callInfo.getConnectStart());
        secureInfo.setVisibility(callInfo.isSecure() ? View.VISIBLE : View.GONE);
        secureInfo.setText(callInfo.getMediaSecureInfo());

        int state = callInfo.getCallState();
        switch (state) {
            case SipCallSession.InvState.INCOMING:
            case SipCallSession.InvState.CALLING:
            case SipCallSession.InvState.EARLY:
            case SipCallSession.InvState.CONNECTING:
                elapsedTime.setVisibility(GONE);
                elapsedTime.start();
                break;
            case SipCallSession.InvState.CONFIRMED:
                Log.v(THIS_FILE, "we start the timer now ");
                elapsedTime.start();
                elapsedTime.setVisibility(VISIBLE);
                elapsedTime.setTextColor(colorConnected);

                break;
            case SipCallSession.InvState.NULL:
            case SipCallSession.InvState.DISCONNECTED:
                elapsedTime.stop();
                elapsedTime.setVisibility(VISIBLE);
                elapsedTime.setTextColor(colorEnd);
                break;
            default:
                break;
        }

    }

    public void switchDetailedInfo(boolean showDetails) {
        /*
         * currentInfo.setVisibility(showDetails?GONE:VISIBLE);
         * currentDetailedInfo.setVisibility(showDetails?VISIBLE:GONE);
         * if(showDetails && callInfo != null) { String infos =
         * PjSipCalls.dumpCallInfo(callInfo.getCallId()); TextView detailText =
         * (TextView) findViewById(R.id.detailsText); detailText.setText(infos);
         * }
         */
    }

    private static final int LOAD_CALLER_INFO = 0;

    private final Handler userHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.arg1 == LOAD_CALLER_INFO) {
                CallerInfo callerInfo = (CallerInfo) msg.obj;
                ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(getContext(),
                        photo,
                        callerInfo,
                        R.drawable.picture_unknown);
                remoteName.setText(callerInfo.name);
                photo.setContentDescription(callerInfo.name);
            }

        };
    };

    private OnTriggerListener onTriggerListener;
    private OnBadgeTouchListener dragListener;

    /*
     * Registers a callback to be invoked when the user triggers an event.
     * @param listener the OnTriggerListener to attach to this view
     */
    public void setOnTriggerListener(OnTriggerListener listener) {
        onTriggerListener = listener;
    }

    private void dispatchTriggerEvent(int whichHandle) {
        Log.d(THIS_FILE, "dispatch " + onTriggerListener);
        if (onTriggerListener != null) {
            onTriggerListener.onTrigger(whichHandle, callInfo);
        }
    }

    public Rect setSize(int i, int j) {
        
        int s = Math.min(i - callCardBorderWidth, j - callCardBorderHeight);

        // Photo LP
        ViewGroup.LayoutParams lp = photo.getLayoutParams();
        lp.width = s;
        lp.height = s;
        photo.setLayoutParams(lp);

        if (renderView != null) {
            lp = renderView.getLayoutParams();
            lp.width = s;
            lp.height = s;
        }

        return new Rect(0, 0, s + callCardBorderWidth, s + callCardBorderHeight);
    }

    public SipCallSession getCallInfo() {
        return callInfo;
    }

    public void setOnTouchListener(OnBadgeTouchListener l) {
        dragListener = l;
        super.setOnTouchListener(l);
    }
    
    @Override
    protected void onDetachedFromWindow() {
        if(callInfo != null && renderView != null) {
            //SipService.setVideoWindow(callInfo.getCallId(), null);
            renderView = null;
        }
        super.onDetachedFromWindow();
    }

}
