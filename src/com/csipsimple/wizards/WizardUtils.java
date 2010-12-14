/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2010 Jan Tschirschwitz <jan.tschirschwitz@googlemail.com>
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
package com.csipsimple.wizards;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.CustomDistribution;
import com.csipsimple.wizards.impl.Advanced;
import com.csipsimple.wizards.impl.Basic;
import com.csipsimple.wizards.impl.Callcentric;
import com.csipsimple.wizards.impl.Ecs;
import com.csipsimple.wizards.impl.Ekiga;
import com.csipsimple.wizards.impl.Eutelia;
import com.csipsimple.wizards.impl.Expert;
import com.csipsimple.wizards.impl.Freephonie;
import com.csipsimple.wizards.impl.Gizmo5;
import com.csipsimple.wizards.impl.IiNet;
import com.csipsimple.wizards.impl.Ip2Mobile;
import com.csipsimple.wizards.impl.IpTel;
import com.csipsimple.wizards.impl.Ippi;
import com.csipsimple.wizards.impl.Keyyo;
import com.csipsimple.wizards.impl.Local;
import com.csipsimple.wizards.impl.NexGenTel;
import com.csipsimple.wizards.impl.OnSip;
import com.csipsimple.wizards.impl.Pbxes;
import com.csipsimple.wizards.impl.Pennytel;
import com.csipsimple.wizards.impl.Phonzo;
import com.csipsimple.wizards.impl.PlanetPhone;
import com.csipsimple.wizards.impl.Sip2Sip;
import com.csipsimple.wizards.impl.SipSorcery;
import com.csipsimple.wizards.impl.Sipgate;
import com.csipsimple.wizards.impl.Speakezi;
import com.csipsimple.wizards.impl.UkrTelecom;
import com.csipsimple.wizards.impl.VPhone;


public class WizardUtils {
	
	
	public static class WizardInfo {
		public String label;
		public String id;
		public int icon;
		public int priority=99;
		public Locale[] countries;
		public boolean isGeneric = false;
		public boolean isWorld = false;
		public Class<?> classObject;
		
		public WizardInfo(String aId, String aLabel, int aIcon, int aPriority, Locale[] aCountries, boolean aIsGeneric, boolean aIsWorld, Class<?> aClassObject) {
			id = aId;
			label = aLabel;
			icon = aIcon;
			priority = aPriority;
			countries = aCountries;
			isGeneric = aIsGeneric;
			isWorld = aIsWorld;
			classObject = aClassObject;
		}
	};
	
	private static boolean initDone = false;
	
	
    public static final String LABEL = "LABEL";
    public static final String ICON  = "ICON";
    public static final String ID  = "ID";
    public static final String LANG_DISPLAY  = "DISPLAY";
    public static final String PRIORITY  = "PRIORITY";
    
    
    
    private static HashMap<String, WizardInfo> WIZARDS_DICT;
    
    private static class WizardPrioComparator implements Comparator<Map<String, Object>> {
		

		@Override
		public int compare(Map<String, Object> infos1, Map<String, Object> infos2) {
			if (infos1 != null && infos2 != null) {
				Integer w1 = (Integer) infos1.get(PRIORITY);
				Integer w2 = (Integer) infos2.get(PRIORITY);
				//Log.d(THIS_FILE, "Compare : "+w1+ " vs "+w2);
				if (w1 > w2) {
					return -1;
				}
				if (w1 < w2) {
					return 1;
				}
			}
			return 0;
		}
    }
    
    
	
    /**
     * Initialize wizards list
     */
	private static void initWizards() {
		WIZARDS_DICT = new HashMap<String, WizardInfo>();
		
		//Generic
		WIZARDS_DICT.put("BASIC", new WizardInfo("BASIC", "Basic", 
				R.drawable.ic_wizard_basic, 50, 
				new Locale[] {}, true, false, 
				Basic.class));
		WIZARDS_DICT.put("ADVANCED", new WizardInfo("ADVANCED", "Advanced", 
				R.drawable.ic_wizard_advanced, 10, 
				new Locale[] {}, true, false, 
				Advanced.class));
		WIZARDS_DICT.put("EXPERT", new WizardInfo("EXPERT", "Expert", 
				R.drawable.ic_wizard_expert, 1, 
				new Locale[] {}, true, false, 
				Expert.class));
		WIZARDS_DICT.put("LOCAL", new WizardInfo("LOCAL", "Local", 
				R.drawable.ic_wizard_expert, 1, 
				new Locale[] {}, true, false, 
				Local.class));
		
		if(CustomDistribution.distributionWantsOtherProviders()) {
			//World wide
			WIZARDS_DICT.put("EKIGA", new WizardInfo("EKIGA", "Ekiga", 
					R.drawable.ic_wizard_ekiga, 50, 
					new Locale[]{}, false, true, 
					Ekiga.class));
			WIZARDS_DICT.put("SIP2SIP", new WizardInfo("SIP2SIP", "Sip2Sip", 
					R.drawable.ic_wizard_sip2sip, 10, 
					new Locale[]{}, false, true, 
					Sip2Sip.class));
			WIZARDS_DICT.put("IPTEL", new WizardInfo("IPTEL", "IpTel", 
					R.drawable.ic_wizard_iptel, 30, 
					new Locale[]{}, false, true, 
					IpTel.class));
			WIZARDS_DICT.put("SIPSORCERY", new WizardInfo("SIPSORCERY", "SIPSorcery", 
					R.drawable.ic_wizard_sipsorcery, 35, 
					new Locale[]{}, false, true, 
					SipSorcery.class));
			WIZARDS_DICT.put("PBXES", new WizardInfo("PBXES", "Pbxes.org", 
					R.drawable.ic_wizard_pbxes, 20, 
					new Locale[]{}, false, true, 
					Pbxes.class));
			WIZARDS_DICT.put("ECS", new WizardInfo("ECS", "Alcatel-Lucent OmniPCX Office", 
					R.drawable.ic_wizard_ale, 5, 
					new Locale[]{}, false, true, 
					Ecs.class));
			
			
			//Locales
			WIZARDS_DICT.put("CALLCENTRIC", new WizardInfo("CALLCENTRIC", "Callcentric", 
					R.drawable.ic_wizard_callcentric, 10, 
					new Locale[]{Locale.US}, false, false, 
					Callcentric.class));
			WIZARDS_DICT.put("EUTELIA", new WizardInfo("EUTELIA", "Eutelia", 
					R.drawable.ic_wizard_eutelia, 30, 
					new Locale[]{Locale.ITALY}, false, false, 
					Eutelia.class));
			WIZARDS_DICT.put("FREEPHONIE", new WizardInfo("FREEPHONIE", "Freephonie", 
					R.drawable.ic_wizard_freephonie, 30, 
					new Locale[]{Locale.FRANCE}, false, false, 
					Freephonie.class));
			WIZARDS_DICT.put("IPPI", new WizardInfo("IPPI", "ippi", 
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
					}, false, false, Ippi.class));
			WIZARDS_DICT.put("KEYYO", new WizardInfo("KEYYO", "Keyyo", 
					R.drawable.ic_wizard_keyyo, 9, 
					new Locale[]{Locale.FRANCE}, false, false, 
					Keyyo.class));
			WIZARDS_DICT.put("PHONZO", new WizardInfo("PHONZO", "Phonzo", 
					R.drawable.ic_wizard_phonzo, 10, 
					new Locale[]{new Locale("SE")}, false, false, 
					Phonzo.class));
			WIZARDS_DICT.put("PLANETPHONE", new WizardInfo("PLANETPHONE", "PlanetPhone", 
					R.drawable.ic_wizard_planetphone, 10, 
					new Locale[]{new Locale("BG", "bg"),}, false, false, 
					PlanetPhone.class));
			WIZARDS_DICT.put("SIPGATE", new WizardInfo("SIPGATE", "Sipgate", 
					R.drawable.ic_wizard_sipgate, 10, 
					new Locale[]{Locale.US, Locale.UK, Locale.GERMANY}, false, false, 
					Sipgate.class));
			WIZARDS_DICT.put("PENNYTEL", new WizardInfo("PENNYTEL", "Pennytel", 
					R.drawable.ic_wizard_pennytel, 10, 
					new Locale[]{new Locale("EN", "au")}, false, false, 
					Pennytel.class));
			/*
			WIZARDS_DICT.put("MAGICJACK", new WizardInfo("MAGICJACK", "MagicJack", 
					R.drawable.ic_wizard_magicjack, 20, 
					new Locale[]{ Locale.US, Locale.CANADA}, false, false, 
					MagicJack.class));
					*/
			WIZARDS_DICT.put("ONSIP", new WizardInfo("ONSIP", "OnSIP", 
					R.drawable.ic_wizard_onsip, 30, 
					new Locale[]{ Locale.US}, false, false, 
					OnSip.class));
			WIZARDS_DICT.put("GIZMO5", new WizardInfo("GIZMO5", "Gizmo5", 
					R.drawable.ic_wizard_gizmo5, 15, 
					new Locale[]{ Locale.US}, false, false, 
					Gizmo5.class));
			WIZARDS_DICT.put("NEXGENTEL", new WizardInfo("NEXGENTEL", "NGeen", 
					R.drawable.ic_wizard_ngeen, 20, 
					new Locale[]{ Locale.US}, false, false, 
					NexGenTel.class));
			WIZARDS_DICT.put("IINET", new WizardInfo("IINET", "iinet", 
					R.drawable.ic_wizard_iinet, 5, 
					new Locale[]{new Locale("EN", "au")}, false, false, 
					IiNet.class));
			WIZARDS_DICT.put("VPHONE", new WizardInfo("VPHONE", "VTel", 
					R.drawable.ic_wizard_vphone, 5, 
					new Locale[]{new Locale("EN", "au")}, false, false, 
					VPhone.class));
			WIZARDS_DICT.put("UKRTEL", new WizardInfo("UKRTEL", "UkrTelecom", 
					R.drawable.ic_wizard_ukrtelecom, 10, 
					new Locale[]{new Locale("UK", "ua")}, false, false, 
					UkrTelecom.class));
			WIZARDS_DICT.put("IP2MOBILE", new WizardInfo("IP2MOBILE", "ip2Mobile", 
					R.drawable.ic_wizard_ip2mobile, 10, 
					new Locale[]{new Locale("DK", "dk")}, false, false, 
					Ip2Mobile.class));
			WIZARDS_DICT.put("SPEAKEZI", new WizardInfo("SPEAKEZI", "Speakezi Telecoms", 
					R.drawable.ic_wizard_speakezi, 30, 
					new Locale[] {new Locale("EN", "za"), new Locale("AF", "za")}, false, false, 
					Speakezi.class));
			
		}else {
			WizardInfo info = CustomDistribution.getCustomDistributionWizard();
			WIZARDS_DICT.put(info.id, info);
		}
		initDone = true;
	}
	
	private static Map<String, Object> wizardInfoToMap(WizardInfo infos) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(LABEL, infos.label);
		map.put(ID, infos.id);
		map.put(ICON, infos.icon);
		map.put(PRIORITY, infos.priority);
		return map;
	}
	
    
	//Ok, what could have be done is declaring an interface but not able with static fields
	// I'll later check whether this is more interesting to declare an interface or an info class
	// used to declare wizards
	public static WizardInfo getWizardClassInfos(Class<?> wizard) {
		Method method;
		try {
			method = wizard.getMethod("getWizardInfo", (Class[]) null);
			return (WizardInfo) method.invoke(null, (Object[]) null);
		} catch (Exception e) {
			//Generic catch : we are not interested in more details
			e.printStackTrace();
		} 
		return null;
	}
   
	public static HashMap<String, WizardInfo> getWizardsList(){
		if(!initDone){
			initWizards();
		}
		return WIZARDS_DICT;
	}
	
	
	public static WizardInfo getWizardClass(String wizardId) {
		if(!initDone){
			initWizards();
		}
		return WIZARDS_DICT.get(wizardId);
	}
	
	public static int getWizardIconRes(String wizardId) {
		// Update account image
		WizardInfo wizard_infos = WizardUtils.getWizardClass(wizardId);
		if (wizard_infos != null) {
			if(!wizard_infos.isGeneric) {
				return wizard_infos.icon;
			}
		}
		return R.drawable.ic_launcher_phone;
	}
	

	public static int getWizardIconRes(SipProfile account) {
		return WizardUtils.getWizardIconRes(account.wizard);
	}


	public static ArrayList<HashMap<String, String>> getWizardsGroups(Context context) {
		ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> m;
		
		//Local
		m = new HashMap<String, String>();
		
	//	m.put("lang", Locale.getDefault().getCountry());
		m.put(LANG_DISPLAY, Locale.getDefault().getDisplayCountry());
		result.add(m);
		
		//Generic
		m = new HashMap<String, String>();
	//	m.put("lang", "generic");
		m.put(LANG_DISPLAY, context.getString(R.string.generic_wizards_text));
		result.add(m);
		
		//World
		m = new HashMap<String, String>();
	//	m.put("lang", "world");
		m.put(LANG_DISPLAY, context.getString(R.string.world_wide_providers_text));
		result.add(m);
		
		//Others
		m = new HashMap<String, String>();
	//	m.put("lang", "others");
		m.put(LANG_DISPLAY, context.getString(R.string.other_country_providers_text));
		result.add(m);
		
		return result;
	}


	public static ArrayList<ArrayList<Map<String, Object>>> getWizardsGroupedList() {
		ArrayList<Map<String, Object>> locale_list = new ArrayList<Map<String, Object>>();
		ArrayList<Map<String, Object>> generic_list = new ArrayList<Map<String, Object>>();
		ArrayList<Map<String, Object>> world_list = new ArrayList<Map<String, Object>>();
		ArrayList<Map<String, Object>> others_list = new ArrayList<Map<String, Object>>();
		
		Set<Entry<String, WizardInfo>> wizards = getWizardsList().entrySet();
		for( Entry<String, WizardInfo> wizard : wizards) {
			boolean found = false;
			
			for (Locale country : wizard.getValue().countries) {
				if(country.getCountry().equals(Locale.getDefault().getCountry())) {
					found = true;
					locale_list.add(wizardInfoToMap(wizard.getValue()));
				}
			}
			if(!found) {
				if(wizard.getValue().isGeneric) {
					generic_list.add(wizardInfoToMap(wizard.getValue()));
					found = true;
				}else if(wizard.getValue().isWorld) {
					world_list.add(wizardInfoToMap(wizard.getValue()));
					found = true;
				}
			}
			if(!found) {
				others_list.add(wizardInfoToMap(wizard.getValue()));
			}
		}
		
		WizardPrioComparator comparator = new WizardPrioComparator();
		Collections.sort(locale_list, comparator);
		Collections.sort(generic_list, comparator);
		Collections.sort(world_list, comparator);
		Collections.sort(others_list, comparator);
		
		ArrayList<ArrayList<Map<String, Object>>> result = new ArrayList<ArrayList<Map<String,Object>>>();
		result.add(locale_list);
		result.add(generic_list);
		result.add(world_list);
		result.add(others_list);
		return result;
	}

	
	
	
}
