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
package com.csipsimple.utils;

import java.util.Locale;

import com.csipsimple.R;
import com.csipsimple.wizards.WizardUtils.WizardInfo;
import com.csipsimple.wizards.impl.BTone;


public class CustomDistribution {

	
	public static boolean distributionWantsOtherAccounts() {
		return true;
	}
	
	public static boolean distributionWantsOtherProviders() {
		return false;
	}
	
	public static String getSupportEmail() {
		return null;
	}
	
	public static String getUserAgent() {
		return "CSipSimple";
	}
	
	public static WizardInfo getCustomDistributionWizard() {
		 return new WizardInfo("BTONE", "BlueTone", 
					R.drawable.ic_wizard_btone, 20, 
					new Locale[]{ Locale.US}, false, false, 
					BTone.class);
		 
	}

	public static String getRootPackage() {
		return "us.btone.voip.app";
	}
	
	public static boolean showIssueList() {
		return false;
	}
	
	public static String getFaqLink() {
		return "http://code.google.com/p/csipsimple/wiki/FAQ#Summary";
	}
	
	public static boolean showFirstSettingScreen() {
		return true;
	}
	
	public static boolean supportMessaging() {
		return false;
	}
	
}
