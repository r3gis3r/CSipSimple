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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.CustomDistribution;
import com.csipsimple.wizards.impl.A1;
import com.csipsimple.wizards.impl.AbcVoip;
import com.csipsimple.wizards.impl.Advanced;
import com.csipsimple.wizards.impl.Amivox;
import com.csipsimple.wizards.impl.BGTel;
import com.csipsimple.wizards.impl.BTone;
import com.csipsimple.wizards.impl.Balses;
import com.csipsimple.wizards.impl.Basic;
import com.csipsimple.wizards.impl.Beeztel;
import com.csipsimple.wizards.impl.BelCentrale;
import com.csipsimple.wizards.impl.Betamax;
import com.csipsimple.wizards.impl.Blueface;
import com.csipsimple.wizards.impl.BroadVoice;
import com.csipsimple.wizards.impl.Broadsoft;
import com.csipsimple.wizards.impl.Callcentric;
import com.csipsimple.wizards.impl.CamundaNet;
import com.csipsimple.wizards.impl.Cellip;
import com.csipsimple.wizards.impl.CongstarQSC;
import com.csipsimple.wizards.impl.CongstarTelekom;
import com.csipsimple.wizards.impl.Cotas;
import com.csipsimple.wizards.impl.DeltaThree;
import com.csipsimple.wizards.impl.DvcNg;
import com.csipsimple.wizards.impl.EasyBell;
import com.csipsimple.wizards.impl.Ekiga;
import com.csipsimple.wizards.impl.EuroTelefon;
import com.csipsimple.wizards.impl.Eutelia;
import com.csipsimple.wizards.impl.Expert;
import com.csipsimple.wizards.impl.FastVoip;
import com.csipsimple.wizards.impl.Fayn;
import com.csipsimple.wizards.impl.Freeconet;
import com.csipsimple.wizards.impl.FreephoneLineCa;
import com.csipsimple.wizards.impl.Freephonie;
import com.csipsimple.wizards.impl.Globtelecom;
import com.csipsimple.wizards.impl.Gradwell;
import com.csipsimple.wizards.impl.Haloo;
import com.csipsimple.wizards.impl.HalooCentrala;
import com.csipsimple.wizards.impl.IPComms;
import com.csipsimple.wizards.impl.IPshka;
import com.csipsimple.wizards.impl.ITTelenet;
import com.csipsimple.wizards.impl.IiNet;
import com.csipsimple.wizards.impl.Innotel;
import com.csipsimple.wizards.impl.Interphone365;
import com.csipsimple.wizards.impl.Ip2Mobile;
import com.csipsimple.wizards.impl.IpTel;
import com.csipsimple.wizards.impl.Ippi;
import com.csipsimple.wizards.impl.Keyyo;
import com.csipsimple.wizards.impl.Local;
import com.csipsimple.wizards.impl.Localphone;
import com.csipsimple.wizards.impl.Mondotalk;
import com.csipsimple.wizards.impl.Netelip;
import com.csipsimple.wizards.impl.NeufTalk;
import com.csipsimple.wizards.impl.Nymgo;
import com.csipsimple.wizards.impl.OXO810;
import com.csipsimple.wizards.impl.Odorik;
import com.csipsimple.wizards.impl.OnSip;
import com.csipsimple.wizards.impl.Optimus;
import com.csipsimple.wizards.impl.Orbtalk;
import com.csipsimple.wizards.impl.Ovh;
import com.csipsimple.wizards.impl.Pbxes;
import com.csipsimple.wizards.impl.Pennytel;
import com.csipsimple.wizards.impl.Pfingo;
import com.csipsimple.wizards.impl.Phonzo;
import com.csipsimple.wizards.impl.PlanetPhone;
import com.csipsimple.wizards.impl.Pozitel;
import com.csipsimple.wizards.impl.Rapidvox;
import com.csipsimple.wizards.impl.Sipkom;
import com.csipsimple.wizards.impl.Smarto;
import com.csipsimple.wizards.impl.Sapo;
import com.csipsimple.wizards.impl.Sbohempevnalinko;
import com.csipsimple.wizards.impl.Scarlet;
import com.csipsimple.wizards.impl.Sip2Sip;
import com.csipsimple.wizards.impl.SipCel;
import com.csipsimple.wizards.impl.SipSorcery;
import com.csipsimple.wizards.impl.SipWise;
import com.csipsimple.wizards.impl.Sipgate;
import com.csipsimple.wizards.impl.Sipnet;
import com.csipsimple.wizards.impl.SiptelPt;
import com.csipsimple.wizards.impl.Sonetel;
import com.csipsimple.wizards.impl.Speakezi;
import com.csipsimple.wizards.impl.Tanstagi;
import com.csipsimple.wizards.impl.Telsome;
import com.csipsimple.wizards.impl.Tlenofon;
import com.csipsimple.wizards.impl.UkrTelecom;
import com.csipsimple.wizards.impl.VPhone;
import com.csipsimple.wizards.impl.Vanbergsystems;
import com.csipsimple.wizards.impl.Viva;
import com.csipsimple.wizards.impl.VoipBel;
import com.csipsimple.wizards.impl.VoipMS;
import com.csipsimple.wizards.impl.VoipNor;
import com.csipsimple.wizards.impl.VoipTel;
import com.csipsimple.wizards.impl.Vono;
import com.csipsimple.wizards.impl.WiMobile;
import com.csipsimple.wizards.impl.Zadarma;
import com.csipsimple.wizards.impl.ZonPt;


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
    public static final String ICON = "ICON";
    public static final String ID = "ID";
    public static final String LANG_DISPLAY = "DISPLAY";
    public static final String PRIORITY = "PRIORITY";
    public static final String PRIORITY_INT = "PRIORITY_INT";
    
    public static final String EXPERT_WIZARD_TAG = "EXPERT";
    public static final String LOCAL_WIZARD_TAG = "LOCAL";
    
    
    
    private static HashMap<String, WizardInfo> WIZARDS_DICT;
    
    private static class WizardPrioComparator implements Comparator<Map<String, Object>> {
		

		@Override
		public int compare(Map<String, Object> infos1, Map<String, Object> infos2) {
			if (infos1 != null && infos2 != null) {
			    if((Boolean) infos1.get(PRIORITY_INT)) {
    				Integer w1 = (Integer) infos1.get(PRIORITY);
    				Integer w2 = (Integer) infos2.get(PRIORITY);
    				//Log.d(THIS_FILE, "Compare : "+w1+ " vs "+w2);
    				if (w1 > w2) {
    					return -1;
    				}
    				if (w1 < w2) {
    					return 1;
    				}
			    }else {
			        String name1 = (String) infos1.get(LABEL);
			        String name2 = (String) infos2.get(LABEL);
			        return name1.compareToIgnoreCase(name2);
			    }
			}
			return 0;
		}
    }
    
    private static Locale locale(String isoCode) {
    	String[] codes = isoCode.split("_");
    	if(codes.length == 2) {
    		return new Locale(codes[0].toLowerCase(), codes[1].toUpperCase());
    	}else if(codes.length == 1){
    		return new Locale(codes[0].toLowerCase());
    	}
    	Log.e("WizardUtils", "Invalid locale "+isoCode);
    	return null;
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
		WIZARDS_DICT.put(WizardUtils.EXPERT_WIZARD_TAG, new WizardInfo(WizardUtils.EXPERT_WIZARD_TAG, "Expert", 
				R.drawable.ic_wizard_expert, 1, 
				new Locale[] {}, true, false, 
				Expert.class));
		WIZARDS_DICT.put(WizardUtils.LOCAL_WIZARD_TAG, new WizardInfo(WizardUtils.LOCAL_WIZARD_TAG, "Local", 
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
					OXO810.class));
			WIZARDS_DICT.put("ITTELENET", new WizardInfo("ITTELENET", "ITTelenet", 
					R.drawable.ic_wizard_ittelenet, 10, 
					new Locale[]{}, false, true, 
					ITTelenet.class));
			WIZARDS_DICT.put("DELTATHREE", new WizardInfo("DELTATHREE", "deltathree", 
					R.drawable.ic_wizard_deltathree, 35, 
					new Locale[]{ }, false, true, 
					DeltaThree.class));
			WIZARDS_DICT.put("CAMUNDANET", new WizardInfo("CAMUNDANET", "CamundaNet", 
					R.drawable.ic_wizard_camundanet, 15, 
					new Locale[]{}, false, true, 
					CamundaNet.class));
			WIZARDS_DICT.put("BETAMAX", new WizardInfo("BETAMAX", "Betamax clone", 
					R.drawable.ic_wizard_basic, 30, 
					new Locale[]{}, false, true, 
					Betamax.class));
			WIZARDS_DICT.put("SIPCEL", new WizardInfo("SIPCEL", "SipCel Telecom", 
					R.drawable.ic_wizard_sipcel, 14, 
					new Locale[]{}, false, true, 
					SipCel.class));
			WIZARDS_DICT.put("LOCALPHONE", new WizardInfo("LOCALPHONE", "Localphone", 
					R.drawable.ic_wizard_localphone, 10, 
					new Locale[]{ }, false, true, 
					Localphone.class));
			WIZARDS_DICT.put("BROADSOFT", new WizardInfo("BROADSOFT", "Broadsoft", 
					R.drawable.ic_wizard_broadsoft, 9, 
					new Locale[]{ }, false, true, 
					Broadsoft.class));
			WIZARDS_DICT.put("DVCNG", new WizardInfo("DVCNG", "DVC'NG", 
					R.drawable.ic_wizard_dvcng, 16, 
					new Locale[]{ }, false, true, 
					DvcNg.class));
			WIZARDS_DICT.put("PFINGO", new WizardInfo("PFINGO", "Pfingo", 
					R.drawable.ic_wizard_pfingo, 19, 
					new Locale[]{ }, false, true, 
					Pfingo.class));
			WIZARDS_DICT.put("FASTVOIP", new WizardInfo("FASTVOIP", "FastVoip", 
					R.drawable.ic_wizard_fastvoip, 20, 
					new Locale[]{  }, false, true, 
					FastVoip.class));
			WIZARDS_DICT.put("SIPWISE", new WizardInfo("SIPWISE", "sipwise", 
					R.drawable.ic_wizard_sipwise, 34, 
					new Locale[]{  }, false, true, 
					SipWise.class));
			WIZARDS_DICT.put("VOIPMS", new WizardInfo("VOIPMS", "VoIP.ms", 
					R.drawable.ic_wizard_voipms, 18, 
					new Locale[]{  }, false, true, 
					VoipMS.class));
			WIZARDS_DICT.put("SONETEL", new WizardInfo("SONETEL", "Sonetel", 
					R.drawable.ic_wizard_sonetel, 17, 
					new Locale[]{  }, false, true, 
					Sonetel.class));
			WIZARDS_DICT.put("RAPIDVOX", new WizardInfo("RAPIDVOX", "Rapidvox", 
					R.drawable.ic_wizard_rapidvox, 19, 
					new Locale[]{  }, false, true, 
					Rapidvox.class));
			WIZARDS_DICT.put("TANSTAGI", new WizardInfo("TANSTAGI", "tanstagi", 
					R.drawable.ic_wizard_tanstagi, 35, 
					new Locale[]{  }, false, true, 
					Tanstagi.class));
            WIZARDS_DICT.put("NYMGO", new WizardInfo("NYMGO", "Nymgo", 
                    R.drawable.ic_wizard_nymgo, 18, 
                    new Locale[]{  }, false, true, 
                    Nymgo.class));
            WIZARDS_DICT.put("SIPKOM", new WizardInfo("SIPKOM", "sipkom", 
                    R.drawable.ic_wizard_sipkom, 18, 
                    new Locale[]{  }, false, true, 
                    Sipkom.class));
            WIZARDS_DICT.put("ABCVOIP", new WizardInfo("ABCVOIP", "ABC-VoIP", 
                    R.drawable.ic_wizard_abcvoip, 18, 
                    new Locale[]{  }, false, true, 
                    AbcVoip.class));
            WIZARDS_DICT.put("AMIVOX", new WizardInfo("AMIVOX", "Amivox", 
                    R.drawable.ic_wizard_amivox, 18, 
                    new Locale[]{  }, false, true, 
                    Amivox.class));
            WIZARDS_DICT.put("VOIPNOR", new WizardInfo("VOIPNOR", "VOIPNOR", 
                    R.drawable.ic_wizard_voipnor, 9, 
                    new Locale[]{ }, false, true, 
                    VoipNor.class));
			
            
			
			//Locales
			WIZARDS_DICT.put("CALLCENTRIC", new WizardInfo("CALLCENTRIC", "Callcentric", 
					R.drawable.ic_wizard_callcentric, 10, 
					new Locale[]{Locale.US}, false, false, 
					Callcentric.class));
			WIZARDS_DICT.put("EUTELIA", new WizardInfo("EUTELIA", "Eutelia", 
					R.drawable.ic_wizard_eutelia, 30, 
					new Locale[]{Locale.ITALY}, false, false, 
					Eutelia.class));
			WIZARDS_DICT.put("WIMOBILE", new WizardInfo("WIMOBILE", "WiMobile", 
					R.drawable.ic_wizard_wimobile, 20, 
					new Locale[]{Locale.ITALY}, false, false, 
					WiMobile.class));
			WIZARDS_DICT.put("FREEPHONIE", new WizardInfo("FREEPHONIE", "Freephonie", 
					R.drawable.ic_wizard_freephonie, 30, 
					new Locale[]{Locale.FRANCE}, false, false, 
					Freephonie.class));
			WIZARDS_DICT.put("NEUFTALK", new WizardInfo("NEUFTALK", "NeufTalk", 
					R.drawable.ic_wizard_neuftalk, 25, 
					new Locale[]{Locale.FRANCE}, false, false, 
					NeufTalk.class));
			WIZARDS_DICT.put("IPPI", new WizardInfo("IPPI", "ippi", 
					R.drawable.ic_wizard_ippi, 21, 
					new Locale[]{ Locale.FRENCH, Locale.CANADA, Locale.US, }, false, false, 
					Ippi.class));
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
					new Locale[]{ locale("bg_BG") }, false, false, 
					PlanetPhone.class));
			WIZARDS_DICT.put("SIPGATE", new WizardInfo("SIPGATE", "Sipgate", 
					R.drawable.ic_wizard_sipgate, 10, 
					new Locale[]{Locale.US, Locale.UK, Locale.GERMANY}, false, false, 
					Sipgate.class));
			WIZARDS_DICT.put("PENNYTEL", new WizardInfo("PENNYTEL", "Pennytel", 
					R.drawable.ic_wizard_pennytel, 10, 
					new Locale[]{ locale("en_AU") }, false, false, 
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
			/*
			WIZARDS_DICT.put("GIZMO5", new WizardInfo("GIZMO5", "Gizmo5", 
					R.drawable.ic_wizard_gizmo5, 15, 
					new Locale[]{ Locale.US}, false, false, 
					Gizmo5.class));
					*/
			WIZARDS_DICT.put("BTONE", new WizardInfo("BTONE", "BlueTone", 
					R.drawable.ic_wizard_btone, 20, 
					new Locale[]{ Locale.US}, false, false, 
					BTone.class));
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
			WIZARDS_DICT.put("POZITEL", new WizardInfo("POZITEL", "Pozitel", 
					R.drawable.ic_wizard_pozitel, 30, 
					new Locale[] {new Locale("TR", "tr")}, false, false, 
					Pozitel.class));
			WIZARDS_DICT.put("MONDOTALK", new WizardInfo("MONDOTALK", "Mondotalk", 
					R.drawable.ic_wizard_mondotalk, 20, 
					new Locale[] {new Locale("EN", "au"), new Locale("EN", "us"), new Locale("EN", "nz")}, false, false, 
					Mondotalk.class));
			WIZARDS_DICT.put("A1", new WizardInfo("A1", "A1", 
					R.drawable.ic_wizard_a1, 20, 
					new Locale[] {new Locale("DE", "at")}, false, false, 
					A1.class));
			WIZARDS_DICT.put("SCARLET", new WizardInfo("SCARLET", "scarlet.be", 
					R.drawable.ic_wizard_scarlet, 10, 
					new Locale[]{
						locale("fr_BE"), locale("nl_BE"), locale("nl_NL")
					}, false, false, Scarlet.class));
			WIZARDS_DICT.put("VONO", new WizardInfo("VONO", "vono", 
					R.drawable.ic_wizard_vono, 10, 
					new Locale[] {new Locale("PT", "br")}, false, false, 
					Vono.class));
			WIZARDS_DICT.put("OVH", new WizardInfo("OVH", "Ovh", 
					R.drawable.ic_wizard_ovh, 20, 
					new Locale[]{
						Locale.FRANCE,	locale("fr_BE"),
						Locale.GERMANY,
						Locale.UK
					}, false, false, 
					Ovh.class));
			WIZARDS_DICT.put("FAYN", new WizardInfo("FAYN", "Fayn", 
					R.drawable.ic_wizard_fayn, 30, 
					new Locale[]{
						new Locale("CS", "cz"),
					}, false, false, 
					Fayn.class));
			WIZARDS_DICT.put("VIVA", new WizardInfo("VIVA", "Viva VoIP", 
					R.drawable.ic_wizard_viva, 30, 
					new Locale[]{
						new Locale("EL", "gr"),
					}, false, false, 
					Viva.class));
			WIZARDS_DICT.put("SAPO", new WizardInfo("SAPO", "Sapo", 
					R.drawable.ic_wizard_sapo, 20, 
					new Locale[] {new Locale("PT", "pt")}, false, false, 
					Sapo.class));
			WIZARDS_DICT.put("BROADVOICE", new WizardInfo("BROADVOICE", "BroadVoice", 
					R.drawable.ic_wizard_broadvoice, 19, 
					new Locale[]{Locale.US}, false, false, 
					BroadVoice.class));
			WIZARDS_DICT.put("SIPTEL", new WizardInfo("SIPTEL", "Siptel", 
					R.drawable.ic_wizard_siptel, 10, 
					new Locale[] {new Locale("PT", "pt")}, false, false, 
					SiptelPt.class));
			WIZARDS_DICT.put("OPTIMUS", new WizardInfo("OPTIMUS", "Optimus", 
					R.drawable.ic_wizard_optimus, 9, 
					new Locale[] {new Locale("PT", "pt")}, false, false, 
					Optimus.class));
			WIZARDS_DICT.put("IPSHKA", new WizardInfo("IPSHKA", "IPshka", 
					R.drawable.ic_wizard_ipshka, 10, 
					new Locale[]{new Locale("UK", "ua")}, false, false, 
					IPshka.class));
			WIZARDS_DICT.put("ZADARMA", new WizardInfo("ZADARMA", "Zadarma", 
					R.drawable.ic_wizard_zadarma, 10, 
					new Locale[]{new Locale("UK", "ua"), locale("ru_RU"), locale("cs_CZ"), locale("ro_RO"), locale("hr_HR"), locale("bg_BG"),}, false, false, 
					Zadarma.class));
			WIZARDS_DICT.put("BLUEFACE", new WizardInfo("BLUEFACE", "Blueface", 
					R.drawable.ic_wizard_blueface, 19, 
					new Locale[]{ Locale.UK, new Locale("EN", "ie") }, false, false, 
					Blueface.class));
			WIZARDS_DICT.put("IPCOMMS", new WizardInfo("IPCOMMS", "IPComms", 
					R.drawable.ic_wizard_ipcomms, 19, 
					new Locale[]{ Locale.US, Locale.CANADA }, false, false, 
					IPComms.class));
			WIZARDS_DICT.put("VOIPTELIE", new WizardInfo("VOIPTELIE", "Voiptel Mobile", 
					R.drawable.ic_wizard_voiptelie, 20, 
					new Locale[]{ 
					Locale.UK, Locale.CANADA, Locale.US, locale("en_IE"), locale("en_AU"),
					locale("es_ES"), locale("es_CO") }, false, false, 
					VoipTel.class));
			WIZARDS_DICT.put("EASYBELL", new WizardInfo("EASYBELL", "EasyBell", 
					R.drawable.ic_wizard_easybell, 20, 
					new Locale[]{ Locale.GERMANY }, false, false, 
					EasyBell.class));
			WIZARDS_DICT.put("NETELIP", new WizardInfo("NETELIP", "NETELIP", 
					R.drawable.ic_wizard_netelip, 5, 
					new Locale[]{ 
					new Locale("es"), new Locale("pt"), Locale.FRENCH, Locale.GERMAN, Locale.ENGLISH,
					locale("bg_BG"), locale("nl_NL"), Locale.ITALY, Locale.CHINA,
					new Locale("sv"), locale("da_DA"), locale("nb_NO"), locale("nn_NO"),
					locale("ru_RU"), locale("tr_TR"), locale("el_GR"), locale("hu_HU"),
					locale("cs_CZ"), locale("ro_RO"), locale("hr_HR"), locale("uk_UA"),
					locale("ja_JP") }, false, false, 
					Netelip.class));
			WIZARDS_DICT.put("TELSOME", new WizardInfo("TELSOME", "Telsome", 
					R.drawable.ic_wizard_telsome, 19, 
					new Locale[]{
					locale("es_ES")
					}, false, false, 
					Telsome.class));
			WIZARDS_DICT.put("INNOTEL", new WizardInfo("INNOTEL", "Innotel", 
					R.drawable.ic_wizard_innotel, 19, 
					new Locale[]{
					locale("hu_HU")
					}, false, false, 
					Innotel.class));
			WIZARDS_DICT.put("EUROTELEFON", new WizardInfo("EUROTELEFON", "EuroTELEFON", 
					R.drawable.ic_wizard_eurotelefon, 19, 
					new Locale[]{
					new Locale("pl")
					}, false, false, 
					EuroTelefon.class));
			WIZARDS_DICT.put("ODORIK", new WizardInfo("ODORIK", "Odorik.cz", 
					R.drawable.ic_wizard_odorik, 19, 
					new Locale[]{
					locale("cs_CZ"),new Locale("sk"), new Locale("sl"), locale("uk_UA")  
					}, false, false, 
					Odorik.class));
			WIZARDS_DICT.put("FREEPHONELINECA", new WizardInfo("FREEPHONELINECA", "Freephoneline.ca", 
					R.drawable.ic_wizard_freephonelineca, 19, 
					new Locale[]{
					Locale.CANADA  
					}, false, false, 
					FreephoneLineCa.class));
			WIZARDS_DICT.put("SIPNET", new WizardInfo("SIPNET", "Sipnet", 
					R.drawable.ic_wizard_sipnet, 10, 
					new Locale[]{
					new Locale("RU", "ru"), locale("ru_RU")
					}, false, false, 
					Sipnet.class));
			WIZARDS_DICT.put("CELLIP", new WizardInfo("CELLIP", "Cellip", 
					R.drawable.ic_wizard_cellip, 10, 
					new Locale[]{
					new Locale("sv")
					}, false, false, 
					Cellip.class));
			WIZARDS_DICT.put("SBOHEMPEVNALINKO", new WizardInfo("SBOHEMPEVNALINKO", "sbohempevnalinko.cz", 
					R.drawable.ic_wizard_sbohempevnalinko, 19, 
					new Locale[]{
					locale("cs_CZ")
					}, false, false, 
					Sbohempevnalinko.class));
			WIZARDS_DICT.put("GRADWELL", new WizardInfo("GRADWELL", "Gradwell", 
					R.drawable.ic_wizard_gradwell, 19, 
					new Locale[]{
					Locale.UK
					}, false, false, 
					Gradwell.class));
			WIZARDS_DICT.put("BGTEL", new WizardInfo("BGTEL", "BG-Tel", 
					R.drawable.ic_wizard_bgtel, 10, 
					new Locale[]{ locale("bg_BG") , Locale.CANADA,
					new Locale("EL", "gr"), Locale.US, Locale.GERMANY}, false, false, 
					BGTel.class));
			WIZARDS_DICT.put("BELCENTRALE", new WizardInfo("BELCENTRALE", "Belcentrale", 
					R.drawable.ic_wizard_belcentrale, 20, 
					new Locale[]{ locale("nl_BE"), locale("nl_NL"), locale("fr_BE") }, false, false, 
					BelCentrale.class));

			WIZARDS_DICT.put("FREECONET", new WizardInfo("FREECONET", "Freeconet", 
					R.drawable.ic_wizard_freeconet, 19, 
					new Locale[]{
					new Locale("pl")
					}, false, false, 
					Freeconet.class));
			WIZARDS_DICT.put("TLENOFON", new WizardInfo("TLENOFON", "Tlenofon", 
					R.drawable.ic_wizard_tlenofon, 19, 
					new Locale[]{
					new Locale("pl")
					}, false, false, 
					Tlenofon.class));
			WIZARDS_DICT.put("VANBERGSYSTEMS", new WizardInfo("VANBERGSYSTEMS", "Vanbergsystems", 
					R.drawable.ic_wizard_vanbergsystems, 19, 
					new Locale[]{
					new Locale("pl")
					}, false, false, 
					Vanbergsystems.class));
			WIZARDS_DICT.put("SMARTO", new WizardInfo("SMARTO", "Smarto", 
					R.drawable.ic_wizard_smarto, 19, 
					new Locale[]{
					new Locale("pl")
					}, false, false, 
					Smarto.class));
            WIZARDS_DICT.put("INTERPHONE365", new WizardInfo("INTERPHONE365", "INTERPHONE365", 
                    R.drawable.ic_wizard_interphone365, 19, 
                    new Locale[]{
                    locale("es_AR"), locale("es_ES") 
                    }, false, false, 
                    Interphone365.class));
            WIZARDS_DICT.put("BEEZTEL", new WizardInfo("BEEZTEL", "Beeztel", 
                    R.drawable.ic_wizard_beeztel, 19, 
                    new Locale[]{ new Locale("es"), new Locale("en"), new Locale("pt"), new Locale("fr") }, false, false, 
                    Beeztel.class));
            WIZARDS_DICT.put("COTAS", new WizardInfo("COTAS", "Cotas Line@net", 
                    R.drawable.ic_wizard_cotas, 19, 
                    new Locale[]{ locale("es_CO") }, false, false, 
                    Cotas.class));
            WIZARDS_DICT.put("BALSES", new WizardInfo("BALSES", "Balses", 
                    R.drawable.ic_wizard_balses, 19, 
                    new Locale[]{ locale("tr_TR") }, false, false, 
                    Balses.class));
            WIZARDS_DICT.put("ZONPT", new WizardInfo("ZONPT", "Zon Phone", 
                    R.drawable.ic_wizard_zonpt, 19, 
                    new Locale[]{ locale("pt_PT") }, false, false, 
                    ZonPt.class));
            WIZARDS_DICT.put("ORBTALK", new WizardInfo("ORBTALK", "Orbtalk", 
                    R.drawable.ic_wizard_orbtalk, 19, 
                    new Locale[]{ Locale.UK, Locale.US }, false, false, 
                    Orbtalk.class));
            WIZARDS_DICT.put("HALOOCENTRALA", new WizardInfo("HALOOCENTRALA", "Ha-loo centrala", 
                    R.drawable.ic_wizard_haloo_centrala, 19, 
                    new Locale[]{ new Locale("CS", "cz"), }, false, false, 
                    HalooCentrala.class));
            WIZARDS_DICT.put("HALOO", new WizardInfo("HALOO", "Ha-loo", 
                    R.drawable.ic_wizard_haloo, 19, 
                    new Locale[]{ new Locale("CS", "cz"), }, false, false, 
                    Haloo.class));
            WIZARDS_DICT.put("VOIPBEL", new WizardInfo("VOIPBEL", "VoIPBel", 
                    R.drawable.ic_wizard_voipbel, 19, 
                    new Locale[]{ locale("nl_BE"), locale("nl_NL"), locale("fr_BE") }, false, false, 
                    VoipBel.class));
            WIZARDS_DICT.put("GLOBTELECOM", new WizardInfo("GLOBTELECOM", "Globtelecom", 
                    R.drawable.ic_wizard_globtelecom, 10, 
                    new Locale[]{locale("ru_RU"),}, false, false, 
                    Globtelecom.class));
            WIZARDS_DICT.put("CONGSTARTEL", new WizardInfo("CONGSTARTEL", "Congstar Telekom", 
                    R.drawable.ic_wizard_congstar, 10, 
                    new Locale[]{Locale.GERMANY}, false, false, 
                    CongstarTelekom.class));
            WIZARDS_DICT.put("CONGSTARQSC", new WizardInfo("CONGSTARQSC", "Congstar QSC", 
                    R.drawable.ic_wizard_congstar, 10, 
                    new Locale[]{Locale.GERMANY}, false, false, 
                    CongstarQSC.class));
			
		}else {
			WizardInfo info = CustomDistribution.getCustomDistributionWizard();
			WIZARDS_DICT.put(info.id, info);
		}
		initDone = true;
	}
	
	private static Map<String, Object> wizardInfoToMap(WizardInfo infos, boolean usePriorityInt) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(LABEL, infos.label);
		map.put(ID, infos.id);
		map.put(ICON, infos.icon);
		map.put(PRIORITY, infos.priority);
		map.put(PRIORITY_INT, usePriorityInt);
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
	

	public static Bitmap getWizardBitmap(Context ctxt, SipProfile account) {
		if(account.icon == null) {
			Resources r = ctxt.getResources();
			BitmapDrawable bd = ((BitmapDrawable) r.getDrawable(WizardUtils.getWizardIconRes(account.wizard)));
			account.icon = bd.getBitmap();
		}
		return account.icon;
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
		
		if(CustomDistribution.distributionWantsOtherProviders()) {
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
		}
		
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
				if(country != null) {
					if(country.getCountry().equals(Locale.getDefault().getCountry())) {
						found = true;
						locale_list.add(wizardInfoToMap(wizard.getValue(), true));
						break;
					}else if(country.getCountry().equalsIgnoreCase("")) {
						if(country.getLanguage().equals(Locale.getDefault().getLanguage())) {
							found = true;
							locale_list.add(wizardInfoToMap(wizard.getValue(), true));
							break;
						}
					}
				}
			}
			if(!found) {
				if(wizard.getValue().isGeneric) {
					generic_list.add(wizardInfoToMap(wizard.getValue(), true));
					found = true;
				}else if(wizard.getValue().isWorld) {
					world_list.add(wizardInfoToMap(wizard.getValue(), false));
					found = true;
				}
			}
			if(!found) {
				others_list.add(wizardInfoToMap(wizard.getValue(), false));
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
