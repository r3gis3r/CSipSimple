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

import com.csipsimple.R;
import android.content.Context;


public class WizardUtils {
	
	
	public static class WizardInfo {
		public String label;
		public String id;
		public int icon;
		public int priority=99;
		public Locale[] countries;
		public boolean isGeneric = false;
		public boolean isWorld = false;
		public Class<?> implementation;
	};
	
	//I didn't manage to introspect since in dalvik package aren't directly visible
	//from ClassLoader resources
	private static String[] wizards_classes_names = new String[]{
		"Advanced",
		"Basic",
		"Callcentric",
		"Ecs",
		"Ekiga",
		"Eutelia",
		"Expert",
		"Freephonie",
		"Sip2Sip",
		"Ippi",
		"Pbxes",
		"MagicJack",
		"PlanetPhone"
	};
	
	private static boolean init_done = false;
	
	
    public static final String LABEL = "LABEL";
    public static final String ICON  = "ICON";
    public static final String ID  = "ID";
    public static final String LANG_DISPLAY  = "DISPLAY";
    
    
    private static ArrayList<Map<String, Object>> wizards_list;
    private static ArrayList<WizardInfo> wizards_classes;
    
    private static class WizardPrioComparator implements Comparator<WizardInfo> {
		@Override
		public int compare(WizardInfo infos1, WizardInfo infos2) {
			if (infos1 != null && infos2 != null) {
				if (infos1.priority > infos2.priority) {
					return -1;
				}
				if (infos1.priority < infos2.priority) {
					return 1;
				}
			}

			return 0;
		}
    }
    
    
	
    /**
     * Initialize wizards list
     */
    private static void init_wizards() {
		wizards_classes = getWizards();
		
		Collections.sort(wizards_classes, new WizardPrioComparator());
		
		wizards_list = new ArrayList< Map<String,Object> >();
		for( WizardInfo infos : wizards_classes ){
	        wizards_list.add(wizardInfoToMap(infos));
		}
		
		init_done = true;
	}
	
    private static Map<String, Object> wizardInfoToMap(WizardInfo infos){
    	Map<String,Object> map = new HashMap<String,Object>();
		map.put( LABEL, infos.label);
        map.put( ID, infos.id);
        map.put( ICON, infos.icon);
    	return map;
    }
	
	/** 
	 * List wizard classes
	 */
	private static ArrayList<WizardInfo> getWizards() {
		ArrayList<WizardInfo> classes = new ArrayList<WizardInfo>();
		for(String cls : wizards_classes_names){
			try {
				Class<?> classe = Class.forName("com.csipsimple.wizards.impl." + cls);
				WizardInfo infos = getWizardClassInfos(classe);
				infos.implementation = classe;
				if(infos != null) {
					classes.add(infos);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return classes;
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
   
	public static ArrayList<Map<String, Object>> getWizardsList(){
		if(!init_done){
			init_wizards();
		}
		return wizards_list;
	}
	
	public static ArrayList<WizardInfo> getWizardsClasses(){
		if(!init_done){
			init_wizards();
		}
		return wizards_classes;
	}
	
	
	
	public static WizardInfo getWizardClass(String wizard_id) {
		if(!init_done){
			init_wizards();
		}
		for(WizardInfo candidate_class : wizards_classes){
			if(candidate_class.id.equalsIgnoreCase(wizard_id)) {
				return candidate_class;
			}
		}
		return null;
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
		
		ArrayList<WizardInfo> wizards = getWizardsClasses();
		for( WizardInfo wizard : wizards) {
			boolean found = false;
			
			for (Locale country : wizard.countries) {
				if(country.getCountry().equals(Locale.getDefault().getCountry())) {
					found = true;
					locale_list.add(wizardInfoToMap(wizard));
				}
			}
			if(!found) {
				if(wizard.isGeneric) {
					generic_list.add(wizardInfoToMap(wizard));
					found = true;
				}else if(wizard.isWorld) {
					world_list.add(wizardInfoToMap(wizard));
					found = true;
				}
			}
			if(!found) {
				others_list.add(wizardInfoToMap(wizard));
			}
		}
		
		ArrayList<ArrayList<Map<String, Object>>> result = new ArrayList<ArrayList<Map<String,Object>>>();
		result.add(locale_list);
		result.add(generic_list);
		result.add(world_list);
		result.add(others_list);
		return result;
	}
}
