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

package com.csipsimple.pjsip;

import android.os.SystemClock;
import android.text.TextUtils;

import com.csipsimple.service.SipService.SameThreadException;
import com.csipsimple.service.impl.SipCallSessionImpl;
import com.csipsimple.utils.Log;

import org.pjsip.pjsua.pj_time_val;
import org.pjsip.pjsua.pjmedia_dir;
import org.pjsip.pjsua.pjsip_event;
import org.pjsip.pjsua.pjsip_inv_state;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;
import org.pjsip.pjsua.pjsua_call_info;
import org.pjsip.pjsua.zrtp_state_info;

/**
 * Singleton class to manage pjsip calls. It allows to convert retrieve pjsip
 * calls information and convert that into objects that can be easily managed on
 * Android side
 */
public final class PjSipCalls {

    private PjSipCalls() {
    }

    private static final String THIS_FILE = "PjSipCalls";

    /**
     * Update the call session infos
     * 
     * @param session The session to update (input/output). Must have a correct
     *            call id set
     * @param service PjSipService Sip service to retrieve pjsip accounts infos
     * @throws SameThreadException
     */
    public static void updateSessionFromPj(SipCallSessionImpl session, pjsip_event e, PjSipService service)
            throws SameThreadException {
        Log.d(THIS_FILE, "Update call " + session.getCallId());
        pjsua_call_info pjInfo = new pjsua_call_info();
        int status = pjsua.call_get_info(session.getCallId(), pjInfo);

        if (status == pjsua.PJ_SUCCESS) {
            // Transform pjInfo into CallSession object
            updateSession(session, pjInfo, service);
            
            // Update state here because we have pjsip_event here and can get q.850 state
            if(e != null) {
                int status_code = pjsua.get_event_status_code(e);
                if(status_code == 0) {
                    try {
                        status_code = pjInfo.getLast_status().swigValue();
                    } catch (IllegalArgumentException err) {
                        // The status code does not exist in enum ignore it
                    }
                }
                session.setLastStatusCode(status_code);
                Log.d(THIS_FILE, "Last status code is " + status_code);
                // TODO - get comment from q.850 state as well
                String status_text = PjSipService.pjStrToString(pjInfo.getLast_status_text());
                session.setLastStatusComment(status_text);
            }
            
            // And now, about secure information
            String secureInfo = PjSipService.pjStrToString(pjsua.call_secure_info(session
                    .getCallId()));
            session.setMediaSecureInfo(secureInfo);
            session.setMediaSecure(!TextUtils.isEmpty(secureInfo));
            zrtp_state_info zrtpInfo = pjsua.jzrtp_getInfoFromCall(session.getCallId());
            session.setZrtpSASVerified(zrtpInfo.getSas_verified() == pjsuaConstants.PJ_TRUE);
            session.setHasZrtp(zrtpInfo.getSecure() == pjsuaConstants.PJ_TRUE);

            // About video info
            int vidStreamIdx = pjsua.call_get_vid_stream_idx(session.getCallId());
            if(vidStreamIdx >= 0) {
                 int hasVid = pjsua.call_vid_stream_is_running(session.getCallId(), vidStreamIdx, pjmedia_dir.PJMEDIA_DIR_DECODING);
                 session.setMediaHasVideo((hasVid == pjsuaConstants.PJ_TRUE));
            }
            

        } else {
            Log.d(THIS_FILE,
                    "Call info from does not exists in stack anymore - assume it has been disconnected");
            session.setCallState(pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED.swigValue());
        }
    }

    /**
     * Copy infos from pjsua call info object to SipCallSession object
     * 
     * @param session the session to copy info to (output)
     * @param pjCallInfo the call info from pjsip
     * @param service PjSipService Sip service to retrieve pjsip accounts infos
     */
    private static void updateSession(SipCallSessionImpl session, pjsua_call_info pjCallInfo,
            PjSipService service) {
        // Should be unecessary cause we usually copy infos from a valid
        session.setCallId(pjCallInfo.getId());

        // Nothing to think about here cause we have a
        // bijection between int / state
        session.setCallState(pjCallInfo.getState().swigValue());
        session.setMediaStatus(pjCallInfo.getMedia_status().swigValue());
        session.setRemoteContact(PjSipService.pjStrToString(pjCallInfo.getRemote_info()));
        session.setConfPort(pjCallInfo.getConf_slot());

        // Try to retrieve sip account related to this call
        int pjAccId = pjCallInfo.getAcc_id();
        session.setAccId(service.getAccountIdForPjsipId(pjAccId));

        pj_time_val duration = pjCallInfo.getConnect_duration();
        session.setConnectStart(SystemClock.elapsedRealtime() - duration.getSec() * 1000
                - duration.getMsec());
    }

    /**
     * Get infos for this pjsip call
     * 
     * @param callId pjsip call id
     * @return Serialized information about this call
     * @throws SameThreadException
     */
    public static String dumpCallInfo(int callId) throws SameThreadException {
        return PjSipService.pjStrToString(pjsua.call_dump(callId, pjsua.PJ_TRUE, " "));
    }

}
