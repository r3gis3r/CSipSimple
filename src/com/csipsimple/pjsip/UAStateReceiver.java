/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2010 Chris McCormick (aka mccormix - chris@mccormick.cx) 
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

package com.csipsimple.pjsip;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SparseArray;

import com.csipsimple.R;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipCallSession.StatusCode;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipManager.PresenceStatus;
import com.csipsimple.api.SipMessage;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.service.MediaManager;
import com.csipsimple.service.SipNotifications;
import com.csipsimple.service.SipService;
import com.csipsimple.service.SipService.SameThreadException;
import com.csipsimple.service.SipService.SipRunnable;
import com.csipsimple.service.impl.SipCallSessionImpl;
import com.csipsimple.utils.CallLogHelper;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.Threading;
import com.csipsimple.utils.TimerWrapper;

import org.pjsip.pjsua.Callback;
import org.pjsip.pjsua.SWIGTYPE_p_pjsip_rx_data;
import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pj_stun_nat_detect_result;
import org.pjsip.pjsua.pjsip_event;
import org.pjsip.pjsua.pjsip_redirect_op;
import org.pjsip.pjsua.pjsip_status_code;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsua_buddy_info;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UAStateReceiver extends Callback {
    private final static String THIS_FILE = "SIP UA Receiver";
    private final static String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";

    private SipNotifications notificationManager;
    private PjSipService pjService;
    // private ComponentName remoteControlResponder;

    // Time in ms during which we should not relaunch call activity again
    final static long LAUNCH_TRIGGER_DELAY = 2000;
    private long lastLaunchCallHandler = 0;

    private int eventLockCount = 0;
    private boolean mIntegrateWithCallLogs;
    private int mPreferedHeadsetAction;
    private boolean mAutoRecordCalls;
    private int mMicroSource;

    private void lockCpu() {
        if (eventLock != null) {
            Log.d(THIS_FILE, "< LOCK CPU");
            eventLock.acquire();
            eventLockCount++;
        }
    }

    private void unlockCpu() {
        if (eventLock != null && eventLock.isHeld()) {
            eventLock.release();
            eventLockCount--;
            Log.d(THIS_FILE, "> UNLOCK CPU " + eventLockCount);
        }
    }

    /*
     * private class IncomingCallInfos { public SipCallSession callInfo; public
     * Integer accId; }
     */
    @Override
    public void on_incoming_call(final int accId, final int callId, SWIGTYPE_p_pjsip_rx_data rdata) {
        lockCpu();

        // Check if we have not already an ongoing call
        boolean hasOngoingSipCall = false;
        if (pjService != null && pjService.service != null) {
            SipCallSessionImpl[] calls = getCalls();
            if (calls != null) {
                for (SipCallSessionImpl existingCall : calls) {
                    if (!existingCall.isAfterEnded() && existingCall.getCallId() != callId) {
                        if (!pjService.service.supportMultipleCalls) {
                            Log.e(THIS_FILE,
                                    "Settings to not support two call at the same time !!!");
                            // If there is an ongoing call and we do not support
                            // multiple calls
                            // Send busy here
                            pjsua.call_hangup(callId, StatusCode.BUSY_HERE, null, null);
                            unlockCpu();
                            return;
                        } else {
                            hasOngoingSipCall = true;
                        }
                    }
                }
            }
        }

        try {
            SipCallSessionImpl callInfo = updateCallInfoFromStack(callId, null);
            Log.d(THIS_FILE, "Incoming call << for account " + accId);

            // Extra check if set reference counted is false ???
            if (!ongoingCallLock.isHeld()) {
                ongoingCallLock.acquire();
            }

            final String remContact = callInfo.getRemoteContact();
            callInfo.setIncoming(true);
            notificationManager.showNotificationForCall(callInfo);

            // Auto answer feature
            SipProfile acc = pjService.getAccountForPjsipId(accId);
            Bundle extraHdr = new Bundle();
            fillRDataHeader("Call-Info", rdata, extraHdr);
            final int shouldAutoAnswer = pjService.service.shouldAutoAnswer(remContact, acc,
                    extraHdr);
            Log.d(THIS_FILE, "Should I anto answer ? " + shouldAutoAnswer);
            if (shouldAutoAnswer >= 200) {
                // Automatically answer incoming calls with 200 or higher final
                // code
                pjService.callAnswer(callId, shouldAutoAnswer);
            } else {
                // Ring and inform remote about ringing with 180/RINGING
                pjService.callAnswer(callId, 180);

                if (pjService.mediaManager != null) {
                    if (pjService.service.getGSMCallState() == TelephonyManager.CALL_STATE_IDLE
                            && !hasOngoingSipCall) {
                        pjService.mediaManager.startRing(remContact);
                    } else {
                        pjService.mediaManager.playInCallTone(MediaManager.TONE_CALL_WAITING);
                    }
                }
                broadCastAndroidCallState("RINGING", remContact);
            }
            if (shouldAutoAnswer < 300) {
                // Or by api
                launchCallHandler(callInfo);
                Log.d(THIS_FILE, "Incoming call >>");
            }
        } catch (SameThreadException e) {
            // That's fine we are in a pjsip thread
        } finally {
            unlockCpu();
        }

    }

    @Override
    public void on_call_state(final int callId, pjsip_event e) {
        pjsua.css_on_call_state(callId, e);
        lockCpu();

        Log.d(THIS_FILE, "Call state <<");
        try {
            // Get current infos now on same thread cause fix has been done on
            // pj
            final SipCallSession callInfo = updateCallInfoFromStack(callId, e);
            int callState = callInfo.getCallState();

            // If disconnected immediate stop required stuffs
            if (callState == SipCallSession.InvState.DISCONNECTED) {
                if (pjService.mediaManager != null) {
                    pjService.mediaManager.stopRingAndUnfocus();
                    pjService.mediaManager.resetSettings();
                }
                if (ongoingCallLock != null && ongoingCallLock.isHeld()) {
                    ongoingCallLock.release();
                }
                // Call is now ended
                pjService.stopDialtoneGenerator(callId);
                pjService.stopRecording(callId);
                pjService.stopPlaying(callId);
            } else {
                if (ongoingCallLock != null && !ongoingCallLock.isHeld()) {
                    ongoingCallLock.acquire();
                }
            }

            msgHandler.sendMessage(msgHandler.obtainMessage(ON_CALL_STATE, callInfo));
            Log.d(THIS_FILE, "Call state >>");
        } catch (SameThreadException ex) {
            // We don't care about that we are at least in a pjsua thread
        } finally {
            // Unlock CPU anyway
            unlockCpu();
        }

    }

    @Override
    public void on_buddy_state(int buddyId) {
        lockCpu();

        pjsua_buddy_info binfo = new pjsua_buddy_info();
        pjsua.buddy_get_info(buddyId, binfo);

        Log.d(THIS_FILE, "On buddy " + buddyId + " state " + binfo.getMonitor_pres() + " state "
                + PjSipService.pjStrToString(binfo.getStatus_text()));
        PresenceStatus presStatus = PresenceStatus.UNKNOWN;
        // First get info from basic status
        String presStatusTxt = PjSipService.pjStrToString(binfo.getStatus_text());
        boolean isDefaultTxt = presStatusTxt.equalsIgnoreCase("Online")
                || presStatusTxt.equalsIgnoreCase("Offline");
        switch (binfo.getStatus()) {
            case PJSUA_BUDDY_STATUS_ONLINE:
                presStatus = PresenceStatus.ONLINE;
                break;
            case PJSUA_BUDDY_STATUS_OFFLINE:
                presStatus = PresenceStatus.OFFLINE;
                break;
            case PJSUA_BUDDY_STATUS_UNKNOWN:
            default:
                presStatus = PresenceStatus.UNKNOWN;
                break;
        }
        // Now get infos from RPID
        switch (binfo.getRpid().getActivity()) {
            case PJRPID_ACTIVITY_AWAY:
                presStatus = PresenceStatus.AWAY;
                if (isDefaultTxt) {
                    presStatusTxt = "";
                }
                break;
            case PJRPID_ACTIVITY_BUSY:
                presStatus = PresenceStatus.BUSY;
                if (isDefaultTxt) {
                    presStatusTxt = "";
                }
                break;
            default:
                break;
        }

        pjService.service.presenceMgr.changeBuddyState(PjSipService.pjStrToString(binfo.getUri()),
                binfo.getMonitor_pres(), presStatus, presStatusTxt);
        unlockCpu();
    }

    @Override
    public void on_pager(int callId, pj_str_t from, pj_str_t to, pj_str_t contact,
            pj_str_t mime_type, pj_str_t body) {
        lockCpu();

        long date = System.currentTimeMillis();
        String fromStr = PjSipService.pjStrToString(from);
        String canonicFromStr = SipUri.getCanonicalSipContact(fromStr);
        String contactStr = PjSipService.pjStrToString(contact);
        String toStr = PjSipService.pjStrToString(to);
        String bodyStr = PjSipService.pjStrToString(body);
        String mimeStr = PjSipService.pjStrToString(mime_type);

        SipMessage msg = new SipMessage(canonicFromStr, toStr, contactStr, bodyStr, mimeStr,
                date, SipMessage.MESSAGE_TYPE_INBOX, fromStr);

        // Insert the message to the DB
        ContentResolver cr = pjService.service.getContentResolver();
        cr.insert(SipMessage.MESSAGE_URI, msg.getContentValues());

        // Broadcast the message
        Intent intent = new Intent(SipManager.ACTION_SIP_MESSAGE_RECEIVED);
        // TODO : could be parcelable !
        intent.putExtra(SipMessage.FIELD_FROM, msg.getFrom());
        intent.putExtra(SipMessage.FIELD_BODY, msg.getBody());
        pjService.service.sendBroadcast(intent, SipManager.PERMISSION_USE_SIP);

        // Notify android os of the new message
        notificationManager.showNotificationForMessage(msg);
        unlockCpu();
    }

    @Override
    public void on_pager_status(int callId, pj_str_t to, pj_str_t body, pjsip_status_code status,
            pj_str_t reason) {
        lockCpu();
        // TODO : treat error / acknowledge of messages
        int messageType = (status.equals(pjsip_status_code.PJSIP_SC_OK)
                || status.equals(pjsip_status_code.PJSIP_SC_ACCEPTED)) ? SipMessage.MESSAGE_TYPE_SENT
                : SipMessage.MESSAGE_TYPE_FAILED;
        String toStr = SipUri.getCanonicalSipContact(PjSipService.pjStrToString(to));
        String reasonStr = PjSipService.pjStrToString(reason);
        String bodyStr = PjSipService.pjStrToString(body);
        int statusInt = status.swigValue();
        Log.d(THIS_FILE, "SipMessage in on pager status " + status.toString() + " / " + reasonStr);

        // Update the db
        ContentResolver cr = pjService.service.getContentResolver();
        ContentValues args = new ContentValues();
        args.put(SipMessage.FIELD_TYPE, messageType);
        args.put(SipMessage.FIELD_STATUS, statusInt);
        if (statusInt != SipCallSession.StatusCode.OK
                && statusInt != SipCallSession.StatusCode.ACCEPTED) {
            args.put(SipMessage.FIELD_BODY, bodyStr + " // " + reasonStr);
        }
        cr.update(SipMessage.MESSAGE_URI, args,
                SipMessage.FIELD_TO + "=? AND " +
                        SipMessage.FIELD_BODY + "=? AND " +
                        SipMessage.FIELD_TYPE + "=" + SipMessage.MESSAGE_TYPE_QUEUED,
                new String[] {
                        toStr, bodyStr
                });

        // Broadcast the information
        Intent intent = new Intent(SipManager.ACTION_SIP_MESSAGE_RECEIVED);
        intent.putExtra(SipMessage.FIELD_FROM, toStr);
        pjService.service.sendBroadcast(intent, SipManager.PERMISSION_USE_SIP);
        unlockCpu();
    }

    @Override
    public void on_reg_state(final int accountId) {
        lockCpu();
        pjService.service.getExecutor().execute(new SipRunnable() {
            @Override
            public void doRun() throws SameThreadException {
                // Update java infos
                pjService.updateProfileStateFromService(accountId);
            }
        });
        unlockCpu();
    }

    @Override
    public void on_call_media_state(final int callId) {
        pjsua.css_on_call_media_state(callId);

        lockCpu();
        if (pjService.mediaManager != null) {
            // Do not unfocus here since we are probably in call.
            // Unfocus will be done anyway on call disconnect
            pjService.mediaManager.stopRing();
        }

        try {
            final SipCallSession callInfo = updateCallInfoFromStack(callId, null);

            /*
             * Connect ports appropriately when media status is ACTIVE or REMOTE
             * HOLD, otherwise we should NOT connect the ports.
             */
            if (callInfo.getMediaStatus() == SipCallSession.MediaState.ACTIVE ||
                    callInfo.getMediaStatus() == SipCallSession.MediaState.REMOTE_HOLD) {
                int callConfSlot = callInfo.getConfPort();
                pjsua.conf_connect(callConfSlot, 0);
                pjsua.conf_connect(0, callConfSlot);

                // Adjust software volume
                if (pjService.mediaManager != null) {
                    pjService.mediaManager.setSoftwareVolume();
                }

                // Auto record
                if (mAutoRecordCalls && pjService.canRecord(callId)
                        && !pjService.isRecording(callId)) {
                    pjService
                            .startRecording(callId, SipManager.BITMASK_IN | SipManager.BITMASK_OUT);
                }

            }

            msgHandler.sendMessage(msgHandler.obtainMessage(ON_MEDIA_STATE, callInfo));
        } catch (SameThreadException e) {
            // Nothing to do we are in a pj thread here
        }

        unlockCpu();
    }

    @Override
    public void on_mwi_info(int acc_id, pj_str_t mime_type, pj_str_t body) {
        lockCpu();
        // Treat incoming voice mail notification.

        String msg = PjSipService.pjStrToString(body);
        // Log.d(THIS_FILE, "We have a message :: " + acc_id + " | " +
        // mime_type.getPtr() + " | " + body.getPtr());

        boolean hasMessage = false;
        int numberOfMessages = 0;
        // String accountNbr = "";

        String lines[] = msg.split("\\r?\\n");
        // Decapsulate the application/simple-message-summary
        // TODO : should we check mime-type?
        // rfc3842
        Pattern messWaitingPattern = Pattern.compile(".*Messages-Waiting[ \t]?:[ \t]?(yes|no).*",
                Pattern.CASE_INSENSITIVE);
        // Pattern messAccountPattern =
        // Pattern.compile(".*Message-Account[ \t]?:[ \t]?(.*)",
        // Pattern.CASE_INSENSITIVE);
        Pattern messVoiceNbrPattern = Pattern.compile(
                ".*Voice-Message[ \t]?:[ \t]?([0-9]*)/[0-9]*.*", Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            Matcher m;
            m = messWaitingPattern.matcher(line);
            if (m.matches()) {
                Log.w(THIS_FILE, "Matches : " + m.group(1));
                if ("yes".equalsIgnoreCase(m.group(1))) {
                    Log.d(THIS_FILE, "Hey there is messages !!! ");
                    hasMessage = true;

                }
                continue;
            }
            /*
             * m = messAccountPattern.matcher(line); if(m.matches()) {
             * accountNbr = m.group(1); Log.d(THIS_FILE, "VM acc : " +
             * accountNbr); continue; }
             */
            m = messVoiceNbrPattern.matcher(line);
            if (m.matches()) {
                try {
                    numberOfMessages = Integer.parseInt(m.group(1));
                } catch (NumberFormatException e) {
                    Log.w(THIS_FILE, "Not well formated number " + m.group(1));
                }
                Log.d(THIS_FILE, "Nbr : " + numberOfMessages);
                continue;
            }
        }

        if (hasMessage && numberOfMessages > 0) {
            SipProfile acc = pjService.getAccountForPjsipId(acc_id);
            if (acc != null) {
                Log.d(THIS_FILE, acc_id + " -> Has found account " + acc.getDefaultDomain() + " "
                        + acc.id + " >> " + acc.getProfileName());
            }
            Log.d(THIS_FILE, "We can show the voice messages notification");
            notificationManager.showNotificationForVoiceMail(acc, numberOfMessages);
        }
        unlockCpu();
    }

    // public String sasString = "";
    // public boolean zrtpOn = false;

    public int on_validate_audio_clock_rate(int clockRate) {
        if (pjService != null) {
            return pjService.validateAudioClockRate(clockRate);
        }
        return -1;
    }

    @Override
    public void on_setup_audio(int beforeInit) {
        if (pjService != null) {
            pjService.setAudioInCall(beforeInit);
        }
    }

    @Override
    public void on_teardown_audio() {
        if (pjService != null) {
            pjService.unsetAudioInCall();
        }
    }

    @Override
    public pjsip_redirect_op on_call_redirected(int call_id, pj_str_t target) {
        Log.w(THIS_FILE, "Ask for redirection, not yet implemented, for now allow all "
                + PjSipService.pjStrToString(target));
        return pjsip_redirect_op.PJSIP_REDIRECT_ACCEPT;
    }

    @Override
    public void on_nat_detect(pj_stun_nat_detect_result res) {
        // TODO : IMPLEMENT THIS FEATURE
        Log.d(THIS_FILE,
                "NAT TYPE DETECTED !!!" + res.getNat_type_name() + " et " + res.getStatus());
    }

    @Override
    public int on_set_micro_source() {
        return mMicroSource;
    }

    @Override
    public int timer_schedule(int entry, int entryId, int time) {
        return TimerWrapper.schedule(entry, entryId, time);
    }

    @Override
    public int timer_cancel(int entry, int entryId) {
        return TimerWrapper.cancel(entry, entryId);
    }

    /**
     * Map callId to known {@link SipCallSession}. This is cache of known
     * session maintained by the UA state receiver. The UA state receiver is in
     * charge to maintain calls list integrity for {@link PjSipService}. All
     * information it gets comes from the stack. Except recording status that
     * comes from service.
     */
    private SparseArray<SipCallSessionImpl> callsList = new SparseArray<SipCallSessionImpl>();

    /**
     * Update the call information from pjsip stack by calling pjsip primitives.
     * 
     * @param callId The id to the call to update
     * @param e the pjsip_even that raised the update request
     * @return The built sip call session. It's also stored in cache.
     * @throws SameThreadException if we are calling that from outside the pjsip
     *             thread. It's a virtual exception to make sure not called from
     *             bad place.
     */
    private SipCallSessionImpl updateCallInfoFromStack(Integer callId, pjsip_event e)
            throws SameThreadException {
        SipCallSessionImpl callInfo;
        Log.d(THIS_FILE, "Updating call infos from the stack");
        synchronized (callsList) {
            callInfo = callsList.get(callId);
            if (callInfo == null) {
                callInfo = new SipCallSessionImpl();
                callInfo.setCallId(callId);
            }
        }
        // We update session infos. callInfo is both in/out and will be updated
        PjSipCalls.updateSessionFromPj(callInfo, e, pjService.service);
        // We update from our current recording state
        callInfo.setIsRecording(pjService.isRecording(callId));
        callInfo.setCanRecord(pjService.canRecord(callId));
        synchronized (callsList) {
            // Re-add to list mainly for case newly added session
            callsList.put(callId, callInfo);
        }
        return callInfo;
    }

    /**
     * Get call info for a given call id.
     * 
     * @param callId the id of the call we want infos for
     * @return the call session infos.
     */
    public SipCallSessionImpl getCallInfo(Integer callId) {
        SipCallSessionImpl callInfo;
        synchronized (callsList) {
            callInfo = callsList.get(callId, null);
        }
        return callInfo;
    }

    /**
     * Get list of calls session available.
     * 
     * @return List of calls.
     */
    public SipCallSessionImpl[] getCalls() {
        if (callsList != null) {
            List<SipCallSessionImpl> calls = new ArrayList<SipCallSessionImpl>();

            for (int i = 0; i < callsList.size(); i++) {
                SipCallSessionImpl callInfo = getCallInfo(i);
                if (callInfo != null) {
                    calls.add(callInfo);
                }
            }
            return calls.toArray(new SipCallSessionImpl[calls.size()]);
        }
        return new SipCallSessionImpl[0];
    }

    private WorkerHandler msgHandler;
    private HandlerThread handlerThread;
    private WakeLock ongoingCallLock;
    private WakeLock eventLock;

    // private static final int ON_INCOMING_CALL = 1;
    private static final int ON_CALL_STATE = 2;
    private static final int ON_MEDIA_STATE = 3;

    // private static final int ON_REGISTRATION_STATE = 4;
    // private static final int ON_PAGER = 5;

    private static class WorkerHandler extends Handler {
        WeakReference<UAStateReceiver> sr;

        public WorkerHandler(Looper looper, UAStateReceiver stateReceiver) {
            super(looper);
            Log.d(THIS_FILE, "Create async worker !!!");
            sr = new WeakReference<UAStateReceiver>(stateReceiver);
        }

        public void handleMessage(Message msg) {
            UAStateReceiver stateReceiver = sr.get();
            if (stateReceiver == null) {
                return;
            }
            stateReceiver.lockCpu();
            switch (msg.what) {
                case ON_CALL_STATE: {
                    SipCallSessionImpl callInfo = (SipCallSessionImpl) msg.obj;
                    final int callState = callInfo.getCallState();

                    switch (callState) {
                        case SipCallSession.InvState.INCOMING:
                        case SipCallSession.InvState.CALLING:
                            stateReceiver.notificationManager.showNotificationForCall(callInfo);
                            stateReceiver.launchCallHandler(callInfo);
                            stateReceiver.broadCastAndroidCallState("RINGING",
                                    callInfo.getRemoteContact());
                            break;
                        case SipCallSession.InvState.EARLY:
                        case SipCallSession.InvState.CONNECTING:
                        case SipCallSession.InvState.CONFIRMED:
                            // As per issue #857 we should re-ensure
                            // notification + callHandler at each state
                            // cause we can miss some states due to the fact
                            // treatment of call state is threaded
                            // Anyway if we miss the call early + confirmed we
                            // do not need to show the UI.
                            stateReceiver.notificationManager.showNotificationForCall(callInfo);
                            stateReceiver.launchCallHandler(callInfo);
                            stateReceiver.broadCastAndroidCallState("OFFHOOK",
                                    callInfo.getRemoteContact());

                            if (stateReceiver.pjService.mediaManager != null) {
                                if (callState == SipCallSession.InvState.CONFIRMED) {
                                    // Don't unfocus here
                                    stateReceiver.pjService.mediaManager.stopRing();
                                }
                            }
                            // Auto send pending dtmf
                            if (callState == SipCallSession.InvState.CONFIRMED) {
                                stateReceiver.sendPendingDtmf(callInfo.getCallId());
                            }
                            // If state is confirmed and not already intialized
                            if (callState == SipCallSession.InvState.CONFIRMED
                                    && callInfo.getCallStart() == 0) {
                                callInfo.setCallStart(System.currentTimeMillis());
                            }
                            break;
                        case SipCallSession.InvState.DISCONNECTED:
                            if (stateReceiver.pjService.mediaManager != null) {
                                stateReceiver.pjService.mediaManager.stopRing();
                            }

                            Log.d(THIS_FILE, "Finish call2");
                            stateReceiver.broadCastAndroidCallState("IDLE",
                                    callInfo.getRemoteContact());

                            // If no remaining calls, cancel the notification
                            if (stateReceiver.getActiveCallInProgress() == null) {
                                stateReceiver.notificationManager.cancelCalls();
                                // We should now ask parent to stop if needed
                                if (stateReceiver.pjService != null
                                        && stateReceiver.pjService.service != null) {
                                    stateReceiver.pjService.service
                                            .treatDeferUnregistersForOutgoing();
                                }
                            }

                            // CallLog
                            ContentValues cv = CallLogHelper.logValuesForCall(
                                    stateReceiver.pjService.service, callInfo,
                                    callInfo.getCallStart());

                            // Fill our own database
                            stateReceiver.pjService.service.getContentResolver().insert(
                                    SipManager.CALLLOG_URI, cv);
                            Integer isNew = cv.getAsInteger(CallLog.Calls.NEW);
                            if (isNew != null && isNew == 1) {
                                stateReceiver.notificationManager.showNotificationForMissedCall(cv);
                            }

                            // If the call goes out in error...
                            if (callInfo.getLastStatusCode() != 200 && callInfo.getLastReasonCode() != 200) {
                                // We notify the user with toaster
                                stateReceiver.pjService.service.notifyUserOfMessage(callInfo
                                        .getLastStatusCode()
                                        + " / "
                                        + callInfo.getLastStatusComment());
                            }

                            // If needed fill native database
                            if (stateReceiver.mIntegrateWithCallLogs) {
                                // Don't add with new flag
                                cv.put(CallLog.Calls.NEW, false);
                                // Remove csipsimple custom entries
                                cv.remove(SipManager.CALLLOG_PROFILE_ID_FIELD);
                                cv.remove(SipManager.CALLLOG_STATUS_CODE_FIELD);
                                cv.remove(SipManager.CALLLOG_STATUS_TEXT_FIELD);

                                // Reformat number for callogs
                                ParsedSipContactInfos callerInfos = SipUri.parseSipContact(cv
                                        .getAsString(Calls.NUMBER));
                                if (callerInfos != null) {
                                    String phoneNumber = SipUri.getPhoneNumber(callerInfos);

                                    // Only log numbers that can be called by
                                    // GSM too.
                                    // TODO : if android 2.3 add sip uri also
                                    if (!TextUtils.isEmpty(phoneNumber)) {
                                        cv.put(Calls.NUMBER, phoneNumber);
                                        // For log in call logs => don't add as
                                        // new calls... we manage it ourselves.
                                        cv.put(Calls.NEW, false);
                                        ContentValues extraCv = new ContentValues();

                                        if (callInfo.getAccId() != SipProfile.INVALID_ID) {
                                            SipProfile acc = stateReceiver.pjService.service
                                                    .getAccount(callInfo.getAccId());
                                            if (acc != null && acc.display_name != null) {
                                                extraCv.put(CallLogHelper.EXTRA_SIP_PROVIDER,
                                                        acc.display_name);
                                            }
                                        }
                                        CallLogHelper.addCallLog(stateReceiver.pjService.service,
                                                cv, extraCv);
                                    }
                                }
                            }
                            callInfo.applyDisconnect();
                            break;
                        default:
                            break;
                    }
                    stateReceiver.onBroadcastCallState(callInfo);
                    break;
                }
                case ON_MEDIA_STATE: {
                    SipCallSession mediaCallInfo = (SipCallSession) msg.obj;
                    SipCallSessionImpl callInfo = stateReceiver.callsList.get(mediaCallInfo
                            .getCallId());
                    callInfo.setMediaStatus(mediaCallInfo.getMediaStatus());
                    stateReceiver.callsList.put(mediaCallInfo.getCallId(), callInfo);
                    stateReceiver.onBroadcastCallState(callInfo);
                    break;
                }
            }
            stateReceiver.unlockCpu();
        }
    };

    // -------
    // Public configuration for receiver
    // -------

    public void initService(PjSipService srv) {
        pjService = srv;
        notificationManager = pjService.service.notificationManager;

        if (handlerThread == null) {
            handlerThread = new HandlerThread("UAStateAsyncWorker");
            handlerThread.start();
        }
        if (msgHandler == null) {
            msgHandler = new WorkerHandler(handlerThread.getLooper(), this);
        }

        if (eventLock == null) {
            PowerManager pman = (PowerManager) pjService.service
                    .getSystemService(Context.POWER_SERVICE);
            eventLock = pman.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "com.csipsimple.inEventLock");
            eventLock.setReferenceCounted(true);

        }
        if (ongoingCallLock == null) {
            PowerManager pman = (PowerManager) pjService.service
                    .getSystemService(Context.POWER_SERVICE);
            ongoingCallLock = pman.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "com.csipsimple.ongoingCallLock");
            ongoingCallLock.setReferenceCounted(false);
        }
    }

    public void stopService() {

        Threading.stopHandlerThread(handlerThread, true);
        handlerThread = null;
        msgHandler = null;

        // Ensure lock is released since this lock is a ref counted one.
        if (eventLock != null) {
            while (eventLock.isHeld()) {
                eventLock.release();
            }
        }
        if (ongoingCallLock != null) {
            if (ongoingCallLock.isHeld()) {
                ongoingCallLock.release();
            }
        }
    }

    public void reconfigure(Context ctxt) {
        mIntegrateWithCallLogs = SipConfigManager.getPreferenceBooleanValue(ctxt,
                SipConfigManager.INTEGRATE_WITH_CALLLOGS);
        mPreferedHeadsetAction = SipConfigManager.getPreferenceIntegerValue(ctxt,
                SipConfigManager.HEADSET_ACTION, SipConfigManager.HEADSET_ACTION_CLEAR_CALL);
        mAutoRecordCalls = SipConfigManager.getPreferenceBooleanValue(ctxt,
                SipConfigManager.AUTO_RECORD_CALLS);
        mMicroSource = SipConfigManager.getPreferenceIntegerValue(ctxt,
                SipConfigManager.MICRO_SOURCE);
    }

    // --------
    // Private methods
    // --------

    /**
     * Broadcast csipsimple intent about the fact we are currently have a sip
     * call state change.<br/>
     * This may be used by third party applications that wants to track
     * csipsimple call state
     * 
     * @param callInfo the new call state infos
     */
    private void onBroadcastCallState(final SipCallSession callInfo) {
        SipCallSession publicCallInfo = new SipCallSession(callInfo);
        Intent callStateChangedIntent = new Intent(SipManager.ACTION_SIP_CALL_CHANGED);
        callStateChangedIntent.putExtra(SipManager.EXTRA_CALL_INFO, publicCallInfo);
        pjService.service.sendBroadcast(callStateChangedIntent, SipManager.PERMISSION_USE_SIP);

    }

    /**
     * Broadcast to android system that we currently have a phone call. This may
     * be managed by other sip apps that want to keep track of incoming calls
     * for example.
     * 
     * @param state The state of the call
     * @param number The corresponding remote number
     */
    private void broadCastAndroidCallState(String state, String number) {
        // Android normalized event
        Intent intent = new Intent(ACTION_PHONE_STATE_CHANGED);
        intent.putExtra(TelephonyManager.EXTRA_STATE, state);
        if (number != null) {
            intent.putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER, number);
        }
        intent.putExtra(pjService.service.getString(R.string.app_name), true);
        pjService.service.sendBroadcast(intent, android.Manifest.permission.READ_PHONE_STATE);
    }

    /**
     * Start the call activity for a given Sip Call Session. <br/>
     * The call activity should take care to get any ongoing calls when started
     * so the currentCallInfo2 parameter is indication only. <br/>
     * This method ensure that the start of the activity is not fired too much
     * in short delay and may just ignore requests if last time someone ask for
     * a launch is too recent
     * 
     * @param currentCallInfo2 the call info that raise this request to open the
     *            call handler activity
     */
    private synchronized void launchCallHandler(SipCallSession currentCallInfo2) {
        long currentElapsedTime = SystemClock.elapsedRealtime();
        // Synchronized ensure we do not get this launched several time
        // We also ensure that a minimum delay has been consumed so that we do
        // not fire this too much times
        // Specially for EARLY - CONNECTING states
        if (lastLaunchCallHandler + LAUNCH_TRIGGER_DELAY < currentElapsedTime) {
            Context ctxt = pjService.service;

            // Launch activity to choose what to do with this call
            Intent callHandlerIntent = SipService.buildCallUiIntent(ctxt, currentCallInfo2);

            Log.d(THIS_FILE, "Anounce call activity");
            ctxt.startActivity(callHandlerIntent);
            lastLaunchCallHandler = currentElapsedTime;
        } else {
            Log.d(THIS_FILE, "Ignore extra launch handler");
        }
    }

    /**
     * Check if any of call infos indicate there is an active call in progress.
     * 
     * @see SipCallSession#isActive()
     */
    public SipCallSession getActiveCallInProgress() {
        // Go through the whole list of calls and find the first active state.
        for (int i = 0; i < callsList.size(); i++) {
            SipCallSession callInfo = getCallInfo(i);
            if (callInfo != null && callInfo.isActive()) {
                return callInfo;
            }
        }
        return null;
    }

    /**
     * Check if any of call infos indicate there is an active call in progress.
     * 
     * @see SipCallSession#isActive()
     */
    public SipCallSession getActiveCallOngoing() {
        // Go through the whole list of calls and find the first active state.
        for (int i = 0; i < callsList.size(); i++) {
            SipCallSession callInfo = getCallInfo(i);
            if (callInfo != null && callInfo.isActive() && callInfo.isOngoing()) {
                return callInfo;
            }
        }
        return null;
    }

    /**
     * Broadcast the Headset button press event internally if there is any call
     * in progress. TODO : register and unregister only while in call
     */
    public boolean handleHeadsetButton() {
        final SipCallSession callInfo = getActiveCallInProgress();
        if (callInfo != null) {
            // Headset button has been pressed by user. If there is an
            // incoming call ringing the button will be used to answer the
            // call. If there is an ongoing call in progress the button will
            // be used to hangup the call or mute the microphone.
            int state = callInfo.getCallState();
            if (callInfo.isIncoming() &&
                    (state == SipCallSession.InvState.INCOMING ||
                    state == SipCallSession.InvState.EARLY)) {
                if (pjService != null && pjService.service != null) {
                    pjService.service.getExecutor().execute(new SipRunnable() {
                        @Override
                        protected void doRun() throws SameThreadException {

                            pjService.callAnswer(callInfo.getCallId(),
                                    pjsip_status_code.PJSIP_SC_OK.swigValue());
                        }
                    });
                }
                return true;
            } else if (state == SipCallSession.InvState.INCOMING ||
                    state == SipCallSession.InvState.EARLY ||
                    state == SipCallSession.InvState.CALLING ||
                    state == SipCallSession.InvState.CONFIRMED ||
                    state == SipCallSession.InvState.CONNECTING) {
                //
                // In the Android phone app using the media button during
                // a call mutes the microphone instead of terminating the call.
                // We check here if this should be the behavior here or if
                // the call should be cleared.
                //
                if (pjService != null && pjService.service != null) {
                    pjService.service.getExecutor().execute(new SipRunnable() {

                        @Override
                        protected void doRun() throws SameThreadException {
                            if (mPreferedHeadsetAction == SipConfigManager.HEADSET_ACTION_CLEAR_CALL) {
                                pjService.callHangup(callInfo.getCallId(), 0);
                            } else if (mPreferedHeadsetAction == SipConfigManager.HEADSET_ACTION_HOLD) {
                                pjService.callHold(callInfo.getCallId());
                            } else if (mPreferedHeadsetAction == SipConfigManager.HEADSET_ACTION_MUTE) {
                                pjService.mediaManager.toggleMute();
                            }
                        }
                    });
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Update status of call recording info in call session info
     * 
     * @param callId The call id to modify
     * @param canRecord if we can now record the call
     * @param isRecording if we are currently recording the call
     */
    public void updateRecordingStatus(int callId, boolean canRecord, boolean isRecording) {
        SipCallSessionImpl callInfo = getCallInfo(callId);
        callInfo.setCanRecord(canRecord);
        callInfo.setIsRecording(isRecording);
        synchronized (callsList) {
            // Re-add it just to be sure
            callsList.put(callId, callInfo);
        }
        onBroadcastCallState(callInfo);
    }

    private void sendPendingDtmf(final int callId) {
        pjService.service.getExecutor().execute(new SipRunnable() {
            @Override
            protected void doRun() throws SameThreadException {
                pjService.sendPendingDtmf(callId);
            }
        });
    }

    private void fillRDataHeader(String hdrName, SWIGTYPE_p_pjsip_rx_data rdata, Bundle out)
            throws SameThreadException {
        String valueHdr = PjSipService.pjStrToString(pjsua.get_rx_data_header(
                pjsua.pj_str_copy(hdrName), rdata));
        if (!TextUtils.isEmpty(valueHdr)) {
            out.putString(hdrName, valueHdr);
        }
    }

    public void updateCallMediaState(int callId) throws SameThreadException {
        SipCallSession callInfo = updateCallInfoFromStack(callId, null);
        msgHandler.sendMessage(msgHandler.obtainMessage(ON_MEDIA_STATE, callInfo));
    }
}
