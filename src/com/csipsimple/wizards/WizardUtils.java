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
package com.csipsimple.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;



public class WizardUtils {
	
	
	public static class WizardInfo {
		public String label;
		public String id;
		public int icon;
	};

	//I didn't manage to introspect since in dalvik package aren't directly visible
	//from ClassLoader resources
	private static String[] wizards_classes_names = new String[]{
		"Basic",
		"Ecs",
		"Expert",
		"Freephonie"
	};
	
	private static boolean init_done = false;
	
    public static final String LABEL = "LABEL";
    public static final String ICON  = "ICON";
    public static final String ID  = "ID";
    
    
    private static ArrayList<Map<String, Object>> wizards_list;
    private static Class<?>[] wizards_classes;
    
    private static class WizardPrioComparator implements Comparator<Class<?>> {

		@Override
		public int compare(Class<?> object1, Class<?> object2) {
			try {
				int p1 = object1.getField("priority").getInt(null);
				int p2 = object2.getField("priority").getInt(null);
				if(p1 > p2){
					return -1;
				}
				if(p1 < p2 ){
					return 1;
				}
				
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return 0;
		}

    }
	
    /**
     * Initialize wizards list
     */
    private static void init_wizards() {
    	try {
    		wizards_classes = getClasses();
			Arrays.sort(wizards_classes, new WizardPrioComparator());
			
			wizards_list = new ArrayList< Map<String,Object> >();
			
			
			for( Class<?> cls : wizards_classes ){
				Map<String,Object> map = new HashMap<String,Object>();
				
				map.put( LABEL, (String) cls.getField("label").get(null) );
		        map.put( ID, (String) cls.getField("id").get(null));
		        map.put( ICON,  cls.getField("icon").getInt(null));
		        
		        wizards_list.add(map);
			}
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		init_done = true;
	}
	
	
	/** 
	 * List wizard classes
	 */
	private static Class<?>[] getClasses() throws ClassNotFoundException {
		
		
		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
		for(String cls : wizards_classes_names){
			classes.add(Class.forName("com.csipsimple.wizards.impl." + cls));
		}
		Class<?>[] classesA = new Class[classes.size()];
		classes.toArray(classesA);
		return classesA;
	}
    
   
	public static ArrayList<Map<String, Object>> getWizardsList(){
		if(!init_done){
			init_wizards();
		}
		return wizards_list;
	}
	
	public static Class<?>[] getWizardsClasses(){
		if(!init_done){
			init_wizards();
		}
		return wizards_classes;
	}
	
	//Ok, what could have be done is declaring an interface but not able with static fields
	// I'll later check whether this is more interesting to declare an interface or an info class
	// used to declare wizards
	public static WizardInfo getWizardClassInfos(String wizard_id) {
		Class<?> candidate_class = getWizardClass(wizard_id);
		if(candidate_class != null){
			WizardInfo result = new WizardInfo();
			try {
				result.label = (String) candidate_class.getField("label").get(null);
				result.id = (String) candidate_class.getField("id").get(null);
				result.icon =  (Integer) candidate_class.getField("icon").get(null);

				return result;
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
		
	}
	
	
	public static Class<?> getWizardClass(String wizard_id) {
		if(!init_done){
			init_wizards();
		}
		for(Class<?> candidate_class : wizards_classes){
			try {
				if( candidate_class.getField("id").get(null).equals(wizard_id) ){
					return candidate_class;
				}
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return null;
	}
}
