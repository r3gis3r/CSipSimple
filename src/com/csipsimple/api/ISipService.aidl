/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  This file and this file only is released under dual Apache license
 */
package com.csipsimple.api;
import com.csipsimple.api.SipProfileState;
import com.csipsimple.api.SipCallSession;

interface ISipService{
	//Stack control
	void sipStart();
	void sipStop();
	void forceStopService();
	void askThreadedRestart();
	
	//Account control
	void addAllAccounts();
	void removeAllAccounts();
	void reAddAllAccounts();
	void setAccountRegistration(int accountId, int renew);
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
	
	//Media control
	void setMicrophoneMute(boolean on);
	void setSpeakerphoneOn(boolean on);
	void setBluetoothOn(boolean on);
	void confAdjustTxLevel(int port, float value);
	void confAdjustRxLevel(int port, float value);
	void setEchoCancellation(boolean on);
	void adjustVolume(in SipCallSession callInfo, int direction, int flags);
	
	//Record calls
	void startRecording(int callId);
	void stopRecording();
	int getRecordedCall();
	boolean canRecord(int callId);
	
	//SMS
	void sendMessage(String msg, String toNumber, int accountId);
	
	//Secure
	void zrtpSASVerified();
}