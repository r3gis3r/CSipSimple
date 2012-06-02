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
 *  
 *  This file and this file only is also released under Apache license as an API file
 */
package com.csipsimple.api;
import com.csipsimple.api.SipProfileState;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.MediaState;

interface ISipService{
	/**
	* Get the current API version
	* @return version number. 1000 x major version + minor version
	* Each major version must be compatible with all versions of the same major version
	*/
	int getVersion();

	//Stack control
	/**
	* Start the sip stack
	*/
	void sipStart();
	/**
	* Stop the sip stack
	*/
	void sipStop();
	/**
	* Force to stop the sip service (stack + everything that goes arround stack)
	*/
	void forceStopService();
	/**
	* Restart the sip stack
	*/
	void askThreadedRestart();
	
	//Account control
	/**
	* Add all accounts available in database and marked active to running sip stack (loaded previously using sipStart)
	*/
	void addAllAccounts();
	/**
	* Remove all accounts from running sip stack (this does nothing in database)
	*/
	void removeAllAccounts();
	/**
	* remove and add all accounts available in database and marked active
	*/
	void reAddAllAccounts();
	/**
	* Change registration for a given account/profile id (id in database)
	* @param accountId the account for which we'd like to change the registration state
	* @param renew 0 if we don't want to unregister, 1 to renew registration
	*/
	void setAccountRegistration(int accountId, int renew);
	/**
	* Get registration state for a given account id
	* @param accountId the account/profile id for which we'd like to get the info (in database)
	* @return the Profile state
	*/ 
	SipProfileState getSipProfileState(int accountId);
	
	//Call configuration control
	/**
	 * Switch next incoming request to auto answer
	 */
	void switchToAutoAnswer();
	/**
	 * Ignore next outgoing call request from tel handler processing
	 */
	void ignoreNextOutgoingCallFor(String number);
	
	//Call control
	/**
	 * Place a call.
	 * 
	 * @param callee The sip uri to call. 
	 * It can also be a simple number, in which case the app will autocomplete.
	 * If you add the scheme, take care to fill completely else it could be considered as a call
	 * to a sip IP/domain
	 * @param accountId The id of the account to use for this call. 
	 */
	void makeCall(in String callee, int accountId);
	/**
	 * Answer a call.
	 * 
	 * @param callId The id of the call to answer.
	 * @param status The sip status code you'd like to answer with. 200 to take the call.  400 <= status < 500 if refusing.
	 */
	int answer(int callId, int status);
	/**
	 * Hangup a call.
	 *
	 * @param callId The id of the call to hangup.
	 * @param status The sip status code you'd like to hangup with.
	 */
	int hangup(int callId, int status);
	int sendDtmf(int callId, int keyCode);
	int hold(int callId);
	int reinvite(int callId, boolean unhold);
	int xfer(int callId, in String callee);
	int xferReplace(int callId, int otherCallId, int options);
	SipCallSession getCallInfo(int callId);
	SipCallSession[] getCalls();
	String showCallInfosDialog(int callId);
	
	//Media control
	void setMicrophoneMute(boolean on);
	void setSpeakerphoneOn(boolean on);
	void setBluetoothOn(boolean on);
	void confAdjustTxLevel(int port, float value);
	void confAdjustRxLevel(int port, float value);
	/**
	 * Get Rx and Tx sound level for a given port.
	 *
	 * @param port Port id we'd like to have the level
	 * @return The RX and TX (0-255) level encoded as RX << 8 | TX
	 */
	long confGetRxTxLevel(int port);
	void setEchoCancellation(boolean on);
	void adjustVolume(in SipCallSession callInfo, int direction, int flags);
	MediaState getCurrentMediaState();
	int startLoopbackTest();
	int stopLoopbackTest();
	
	// Record calls
	/**
	 * Start recording of a call to a file (in/out).
	 * 
	 * @param callId the call id to start recording of.
	 */
	void startRecording(int callId);
	/**
	 * Stop recording of a call.
	 * 
	 * @param callId the call id to stop recording.
	 */
	void stopRecording(int callId);
	/**
	 * Is the call being recorded ?
	 * 
	 * @param callId the call id to get recording status of.
	 * @return true if the call is currently being recorded
	 */
	boolean isRecording(int callId);
	/**
	 * Can the call be recorded ?
	 * 
	 * @param callId the call id to get record capability of.
	 * @return true if it's possible to record the call. 
	 */
	boolean canRecord(int callId);
	
	// Play files to stream
	/**
	* @param filePath filePath the file to play in stream
	* @param callId the call to play to
	* @param way the way the file should be played 
	*  (way & (1<<0)) => send to remote party (micro), 
	*  (way & (1<<1) ) => send to user (speaker/earpiece)
	* example : way = 3 : will play sound both ways
	*/
	void playWaveFile(String filePath, int callId, int way);
	
	// SMS
	void sendMessage(String msg, String toNumber, long accountId);
	
	// Presence
	void setPresence(int presence, String statusText, long accountId);
	int getPresence(long accountId);
	String getPresenceStatus(long accountId);
	
	//Secure
	void zrtpSASVerified(int dataPtr);
}