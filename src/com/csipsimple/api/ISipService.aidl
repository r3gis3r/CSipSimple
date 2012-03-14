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
	void switchToAutoAnswer();
	
	//Call control
	void makeCall(in String callee, int accountId);
	int answer(int callId, int status);
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
	void setEchoCancellation(boolean on);
	void adjustVolume(in SipCallSession callInfo, int direction, int flags);
	MediaState getCurrentMediaState();
	int startLoopbackTest();
	int stopLoopbackTest();
	
	// Record calls
	void startRecording(int callId);
	void stopRecording();
	int getRecordedCall();
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