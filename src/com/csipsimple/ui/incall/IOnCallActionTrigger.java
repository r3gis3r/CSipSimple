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

import com.csipsimple.api.SipCallSession;

/**
 * Interface definition for a callback to be invoked when a tab is triggered by
 * moving it beyond a target zone.
 */
public interface IOnCallActionTrigger {

    /**
     * When user clics on clear call
     */
    int TERMINATE_CALL = 1;
    /**
     * When user clics on take call
     */
    int TAKE_CALL = TERMINATE_CALL + 1;
    /**
     * When user clics on not taking call
     */
    int DONT_TAKE_CALL = TAKE_CALL + 1;
    /**
     * When user clics on reject call
     */
    int REJECT_CALL = DONT_TAKE_CALL + 1;
    /**
     * When mute is set on
     */
    int MUTE_ON = REJECT_CALL + 1;
    /**
     * When mute is set off
     */
    int MUTE_OFF = MUTE_ON + 1;
    /**
     * When bluetooth is set on
     */
    int BLUETOOTH_ON = MUTE_OFF + 1;
    /**
     * When bluetooth is set off
     */
    int BLUETOOTH_OFF = BLUETOOTH_ON + 1;
    /**
     * When speaker is set on
     */
    int SPEAKER_ON = BLUETOOTH_OFF + 1;
    /**
     * When speaker is set off
     */
    int SPEAKER_OFF = SPEAKER_ON + 1;
    /**
     * When detailed display is asked
     */
    int DETAILED_DISPLAY = SPEAKER_OFF + 1;
    /**
     * When hold / reinvite is asked
     */
    int TOGGLE_HOLD = DETAILED_DISPLAY + 1;
    /**
     * When media settings is asked
     */
    int MEDIA_SETTINGS = TOGGLE_HOLD + 1;
    /**
     * When add call is asked
     */
    int ADD_CALL = MEDIA_SETTINGS + 1;
    /**
     * When xfer to a number is asked
     */
    int XFER_CALL = ADD_CALL + 1;
    /**
     * When transfer to a call is asked
     */
    int TRANSFER_CALL = XFER_CALL + 1;
    /**
     * When start recording is asked
     */
    int START_RECORDING = TRANSFER_CALL + 1;
    /**
     * When stop recording is asked
     */
    int STOP_RECORDING = START_RECORDING + 1;
    /**
     * Open the DTMF view
     */
    int DTMF_DISPLAY = STOP_RECORDING +1;
    /**
     * Start the video stream
     */
    int START_VIDEO = DTMF_DISPLAY + 1;
    /**
     * Stop the video stream
     */
    int STOP_VIDEO = START_VIDEO + 1;
    /**
     * Stop the video stream
     */
    int ZRTP_TRUST = STOP_VIDEO + 1;
    /**
     * Stop the video stream
     */
    int ZRTP_REVOKE = ZRTP_TRUST + 1;
    
    /**
     * Called when the user make an action
     * 
     * @param whichAction what action has been done
     */
    void onTrigger(int whichAction, SipCallSession call);

    void onDisplayVideo(boolean show);
}
