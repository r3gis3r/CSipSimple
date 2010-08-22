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
 */
package com.csipsimple.service;
import com.csipsimple.models.AccountInfo;
import com.csipsimple.models.CallInfo;

interface ISipService{
	//Stack control
	void sipStart();
	void sipStop();
	void forceStopService();
	
	//Account control
	void addAllAccounts();
	void removeAllAccounts();
	void reAddAllAccounts();
	void setAccountRegistration(int accountId, int renew);
	AccountInfo getAccountInfo(int accountId);
	
	//Call configuration control
	void switchToAutoAnswer();
	
	//Call control
	void makeCall(in String callee, int accountId);
	int answer(int callId, int status);
	int hangup(int callId, int status);
	int sendDtmf(int callId, int keyCode);
	int hold(int callId);
	int reinvite(int callId, boolean unhold);
	CallInfo getCallInfo(int callId);
	
	//Media control
	void setMicrophoneMute(boolean on);
	void setSpeakerphoneOn(boolean on);
	void setBluetoothOn(boolean on);
}