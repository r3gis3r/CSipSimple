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
import com.csipsimple.wizards.impl.Ippi;
import com.csipsimple.wizards.impl.Keyyo;

public class CustomDistribution {

	// CSipSimple trunk distribution
	
	public static boolean distributionWantsOtherAccounts() {
		return true;
	}
	
	public static boolean distributionWantsOtherProviders() {
		return true;
	}
	
	public static String getSupportEmail() {
		return "developers@csipsimple.com";
	}
	
	public static String getUserAgent() {
		return "CSipSimple";
	}
	
	public static WizardInfo getCustomDistributionWizard() {
		return null; 
	}
	
	public static String getRootPackage() {
		return "com.csipsimple";
	}
	
	
	//Ippi distribution
	/*
	public static boolean distributionWantsOtherAccounts() {
		return true;
	}

	public static boolean distributionWantsOtherProviders() {
		return false;
	}

	public static String getSupportEmail() {
		return "support@ippi.fr";
	}
	
	public static String getUserAgent() {
		return "ippi";
	}
	
	public static WizardInfo getCustomDistributionWizard() {
		 return new WizardInfo("IPPI", "ippi Android", 
				R.drawable.ic_wizard_ippi, 10, 
				new Locale[]{
					Locale.FRANCE,
					new Locale("FR", "be"),
					new Locale("FR", "ch"),
					Locale.CANADA,
					Locale.US,
					new Locale("FR", "ma"),
					new Locale("FR", "dz"),
					new Locale("FR", "tn"),
				}, false, false, Ippi.class);
		 
	}
	*/
	
	
	
	//Keyyo distribution
	/*
	public static boolean distributionWantsOtherAccounts() {
		return true;
	}
	
	public static boolean distributionWantsOtherProviders() {
		return false;
	}
	
	public static String getSupportEmail() {
		return "support@keyyo.net";
	}
	
	public static String getUserAgent() {
		return "CSipSimple";
	}
	
	public static WizardInfo getCustomDistributionWizard() {
		 return new WizardInfo("KEYYO", "Keyyo", 
				R.drawable.ic_wizard_keyyo, 9, 
				new Locale[]{Locale.FRANCE}, false, false, 
				Keyyo.class);
		 
	}
	*/
	
	
}
