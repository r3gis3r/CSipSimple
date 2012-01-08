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
import com.csipsimple.api.SipProfile;

interface ISipConfiguration {
	
	//Prefs
	void setPreferenceString(in String key, in String value);
	void setPreferenceBoolean(in String key, boolean value);
	void setPreferenceFloat(in String key, float value);
	
	String getPreferenceString(in String key);
	boolean getPreferenceBoolean(in String key);
	float getPreferenceFloat(in String key);
	
}