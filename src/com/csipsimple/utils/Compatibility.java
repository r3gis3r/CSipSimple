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

import java.lang.reflect.Field;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.Contacts;
import android.text.TextUtils;

@SuppressWarnings("deprecation")
public class Compatibility {
	
	private static final String THIS_FILE = "Compat";
	private static int currentApi = 0;

	public static int getApiLevel() {

		if (currentApi > 0) {
			return currentApi;
		}

		if (android.os.Build.VERSION.SDK.equalsIgnoreCase("3")) {
			currentApi = 3;
		} else {
			try {
				Field f = android.os.Build.VERSION.class.getDeclaredField("SDK_INT");
				currentApi = (Integer) f.get(null);
			} catch (Exception e) {
				return 0;
			}
		}

		return currentApi;
	}
	
	
	public static boolean isCompatible(int apiLevel) {
		return getApiLevel() >= apiLevel;
	}


	/**
	 * Get the stream id for in call track. Can differ on some devices.
	 * Current device for which it's different :
	 * Archos 5IT
	 * @return
	 */
	public static int getInCallStream() {
		if (android.os.Build.BRAND.equalsIgnoreCase("archos")) {
			//Since archos has no voice call capabilities, voice call stream is not implemented
			//So we have to choose the good stream tag, which is by default falled back to music
			return AudioManager.STREAM_MUSIC;
		}
		//return AudioManager.STREAM_MUSIC;
		return AudioManager.STREAM_VOICE_CALL;
	}
	
	public static boolean shouldUseRoutingApi() {
		Log.d(THIS_FILE, "Current device " + android.os.Build.BRAND + " - " + android.os.Build.DEVICE);

		//HTC evo 4G
		if(android.os.Build.PRODUCT.equalsIgnoreCase("htc_supersonic")) {
			return true;
		}
		
		if (!isCompatible(4)) {
			//If android 1.5, force routing api use 
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean shouldUseModeApi() {
		Log.d(THIS_FILE, "Current device " + android.os.Build.BRAND + " - " + android.os.Build.DEVICE);
		//ZTE blade
		if(android.os.Build.DEVICE.equalsIgnoreCase("blade")) {
			return true;
		}
		//Samsung GT-I5500
		if(android.os.Build.DEVICE.equalsIgnoreCase("GT-I5500")) {
			return true;
		}
		//HTC evo 4G
		if(android.os.Build.PRODUCT.equalsIgnoreCase("htc_supersonic")) {
			return true;
		}
		if(android.os.Build.DEVICE.equalsIgnoreCase("LG-P500")) {
			return true;
		}
		
		return false;
	}


	public static String guessInCallMode() {
		if (android.os.Build.BRAND.equalsIgnoreCase("sdg")) {
			return "3";
		}
		if(android.os.Build.DEVICE.equalsIgnoreCase("blade")) {
			return Integer.toString(AudioManager.MODE_IN_CALL);
		}

		if (!isCompatible(5)) {
			return Integer.toString(AudioManager.MODE_IN_CALL);
		}

		return Integer.toString(AudioManager.MODE_NORMAL);
	}
	
	public static String getCpuAbi() {
		if (isCompatible(4)) {
			Field field;
			try {
				field = android.os.Build.class.getField("CPU_ABI");
				return field.get(null).toString();
			} catch (Exception e) {
				Log.w(THIS_FILE, "Announce to be android 1.6 but no CPU ABI field", e);
			}

		}
		return "armeabi";
	}
	
	private static boolean needPspWorkaround(PreferencesWrapper preferencesWrapper) {
		//Nexus one is impacted
		if(android.os.Build.DEVICE.equalsIgnoreCase("passion")){
			return true;
		}
		//All htc except....
		if(android.os.Build.PRODUCT.toLowerCase().startsWith("htc") 
				|| android.os.Build.BRAND.toLowerCase().startsWith("htc") 
				|| android.os.Build.PRODUCT.toLowerCase().equalsIgnoreCase("inc") /* For Incredible */ ) {
			if(android.os.Build.DEVICE.equalsIgnoreCase("hero") /* HTC HERO */ 
					|| android.os.Build.DEVICE.equalsIgnoreCase("magic") /* Magic Aka Dev G2 */
					|| android.os.Build.DEVICE.equalsIgnoreCase("tatoo") /* Tatoo */
					|| android.os.Build.DEVICE.equalsIgnoreCase("dream") /* Dream Aka Dev G1 */
					|| android.os.Build.DEVICE.equalsIgnoreCase("legend") /* Legend */
					
					) {
				return false;
			}
			return true;
		}
		//Dell streak
		if(android.os.Build.BRAND.toLowerCase().startsWith("dell") &&
				android.os.Build.DEVICE.equalsIgnoreCase("streak")) {
			return true;
		}
		//Motorola milestone 2
		if(android.os.Build.DEVICE.toLowerCase().contains("milestone2")) {
			return true;
		}
		
		return false;
	}
	

	private static boolean needToneWorkaround(PreferencesWrapper prefWrapper) {
		if(android.os.Build.PRODUCT.toLowerCase().startsWith("gt-i5800") ) {
			return true;
		}
		return false;
	}
	
	public static void setFirstRunParameters(PreferencesWrapper preferencesWrapper) {
		//Disable iLBC if not armv7
		preferencesWrapper.setCodecPriority("iLBC/8000/1", 
				getCpuAbi().equalsIgnoreCase("armeabi-v7a") ? "189" : "0");
		preferencesWrapper.setPreferenceStringValue(PreferencesWrapper.SND_MEDIA_QUALITY, getCpuAbi().equalsIgnoreCase("armeabi-v7a") ? "4" : "3");
		
		//Values get from wince pjsip app
		preferencesWrapper.setCodecPriority("PCMU/8000/1", "240");
		preferencesWrapper.setCodecPriority("PCMA/8000/1", "230");
		preferencesWrapper.setCodecPriority("speex/8000/1", "190");
		preferencesWrapper.setCodecPriority("speex/16000/1", "180");
		preferencesWrapper.setCodecPriority("speex/32000/1", "0");
		preferencesWrapper.setCodecPriority("GSM/8000/1", "100");
		preferencesWrapper.setCodecPriority("G722/16000/1", "0");
		preferencesWrapper.setCodecPriority("G729/8000/1", "0");

		preferencesWrapper.setPreferenceStringValue(PreferencesWrapper.SND_AUTO_CLOSE_TIME, isCompatible(4) ? "1" : "5");
		preferencesWrapper.setPreferenceStringValue(PreferencesWrapper.SND_CLOCK_RATE, isCompatible(4) ? "16000" : "8000");
		preferencesWrapper.setPreferenceBooleanValue(PreferencesWrapper.ECHO_CANCELLATION, isCompatible(4) ? true : false);
		//HTC PSP mode hack
		preferencesWrapper.setPreferenceBooleanValue(PreferencesWrapper.KEEP_AWAKE_IN_CALL, needPspWorkaround(preferencesWrapper));
		
		// Galaxy S default settings
		if (android.os.Build.DEVICE.toUpperCase().startsWith("GT-I9000")) {
			preferencesWrapper.setPreferenceFloatValue(PreferencesWrapper.SND_MIC_LEVEL, (float) 0.4);
			preferencesWrapper.setPreferenceFloatValue(PreferencesWrapper.SND_SPEAKER_LEVEL, (float) 0.2);
			preferencesWrapper.setPreferenceBooleanValue(PreferencesWrapper.USE_SOFT_VOLUME, true);
		}
		//HTC evo 4G
		if(android.os.Build.PRODUCT.equalsIgnoreCase("htc_supersonic")) {
			preferencesWrapper.setPreferenceFloatValue(PreferencesWrapper.SND_MIC_LEVEL, (float) 0.5);
			preferencesWrapper.setPreferenceFloatValue(PreferencesWrapper.SND_SPEAKER_LEVEL, (float) 1.5);
			
		}
		
		//Use routing API?
		preferencesWrapper.setPreferenceBooleanValue(PreferencesWrapper.USE_ROUTING_API, shouldUseRoutingApi());
		preferencesWrapper.setPreferenceBooleanValue(PreferencesWrapper.USE_MODE_API, shouldUseModeApi());
		preferencesWrapper.setPreferenceBooleanValue(PreferencesWrapper.SET_AUDIO_GENERATE_TONE, needToneWorkaround(preferencesWrapper));
	}

	public static boolean useFlipAnimation() {
		if (android.os.Build.BRAND.equalsIgnoreCase("archos")) {
			return false;
		}
		return true;
	}
	
	public static List<ResolveInfo> getPossibleActivities(Context ctxt, Intent i){
		PackageManager pm = ctxt.getPackageManager();
		return pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER);
		
	}
	

	public static Intent getPriviledgedIntent(String number) {
		Intent i = new Intent("android.intent.action.CALL_PRIVILEGED");
		Builder b = new Uri.Builder(); 
		b.scheme("tel").appendPath(number);
		i.setData( b.build() );
		return i;
	}
	
	private static List<ResolveInfo> callIntents = null;
	public final static List<ResolveInfo> getIntentsForCall(Context ctxt){
		if(callIntents == null) {
			return getPossibleActivities(ctxt, getPriviledgedIntent("123"));
		}
		return callIntents;
	}
	
	public static boolean canResolveIntent(Context context, final Intent intent) {
		 final PackageManager packageManager = context.getPackageManager();
		 //final Intent intent = new Intent(action);
		 List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		 return list.size() > 0;
	}
	
	private static Boolean canMakeGSMCall = null;
	private static Boolean canMakeSkypeCall = null;
	
	public static boolean canMakeGSMCall(Context context) {
		PreferencesWrapper prefs = new PreferencesWrapper(context);
		if(prefs.getGsmIntegrationType() == PreferencesWrapper.GSM_TYPE_AUTO) {
			if (canMakeGSMCall == null) {
				Intent intentMakePstnCall = new Intent(Intent.ACTION_CALL);
				intentMakePstnCall.setData(Uri.fromParts("tel", "12345", null));
				canMakeGSMCall = canResolveIntent(context, intentMakePstnCall);
			}
			return canMakeGSMCall;
		}
		if(prefs.getGsmIntegrationType() == PreferencesWrapper.GSM_TYPE_PREVENT) {
			return false;
		}
		return true;
	}
	
	public static boolean canMakeSkypeCall(Context context) {
		if (canMakeSkypeCall == null) {
			try {
			    PackageInfo skype = context.getPackageManager().getPackageInfo("com.skype.raider", 0);
			    if(skype != null) {
			    	canMakeSkypeCall = true;
			    }
			} catch (NameNotFoundException e) {
				canMakeSkypeCall = false;
			}
		}
		return canMakeSkypeCall;
	}
	

	public static Intent getContactPhoneIntent() {
    	Intent intent = new Intent(Intent.ACTION_PICK);
    	/*
		intent.setAction(Intent.ACTION_GET_CONTENT);
		intent.setType(Contacts.Phones.CONTENT_ITEM_TYPE);
		*/
    	if (!isCompatible(5)) {
    		intent.setData(Contacts.People.CONTENT_URI);
    	}else {
    		intent.setData(Uri.parse("content://com.android.contacts/contacts"));
    	}
    	

		return intent;
		
    }


	public static void updateVersion(PreferencesWrapper prefWrapper, int lastSeenVersion, int runningVersion) {
		if (lastSeenVersion < 14) {
			
			// Galaxy S default settings
			if (android.os.Build.DEVICE.toUpperCase().startsWith("GT-I9000")) {
				prefWrapper.setPreferenceFloatValue(PreferencesWrapper.SND_MIC_LEVEL, (float) 0.4);
				prefWrapper.setPreferenceFloatValue(PreferencesWrapper.SND_SPEAKER_LEVEL, (float) 0.2);
			}
			
			if (TextUtils.isEmpty(prefWrapper.getStunServer())) {
				prefWrapper.setPreferenceStringValue(PreferencesWrapper.STUN_SERVER, "stun.counterpath.com");
			}
		}
		if (lastSeenVersion < 15) {
			prefWrapper.setPreferenceBooleanValue(PreferencesWrapper.ENABLE_STUN, false);
		}
		//Now we use svn revisions
		if (lastSeenVersion < 369) {
			// Galaxy S default settings
			if (android.os.Build.DEVICE.toUpperCase().startsWith("GT-I9000")) {
				prefWrapper.setPreferenceBooleanValue(PreferencesWrapper.USE_SOFT_VOLUME, true);
			}
			
		}
		
		if(lastSeenVersion < 385) {
			if(needPspWorkaround(prefWrapper)) {
				prefWrapper.setPreferenceBooleanValue(PreferencesWrapper.KEEP_AWAKE_IN_CALL, true);
			}
			prefWrapper.setPreferenceBooleanValue(PreferencesWrapper.USE_ROUTING_API, shouldUseRoutingApi());
			prefWrapper.setPreferenceBooleanValue(PreferencesWrapper.USE_MODE_API, shouldUseModeApi());
			prefWrapper.setPreferenceStringValue(PreferencesWrapper.SIP_AUDIO_MODE, guessInCallMode());
		}
		
		if(lastSeenVersion < 394) {
			//HTC PSP mode hack
			prefWrapper.setPreferenceBooleanValue(PreferencesWrapper.KEEP_AWAKE_IN_CALL, needPspWorkaround(prefWrapper));
		}
		if(lastSeenVersion < 400) {
			prefWrapper.setCodecPriority("G729/8000/1", "0");
		}
		if(lastSeenVersion < 552) {
			prefWrapper.setPreferenceStringValue(PreferencesWrapper.THREAD_COUNT, "3");
			prefWrapper.setPreferenceBooleanValue(PreferencesWrapper.SET_AUDIO_GENERATE_TONE, needToneWorkaround(prefWrapper));

			if(lastSeenVersion > 0) {
				prefWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP_SERVICE, true);
			}
			prefWrapper.setPreferenceBooleanValue(PreferencesWrapper.ENABLE_QOS, false);
			//HTC evo 4G
			if(android.os.Build.PRODUCT.equalsIgnoreCase("htc_supersonic")) {
				prefWrapper.setPreferenceFloatValue(PreferencesWrapper.SND_MIC_LEVEL, (float) 0.5);
				prefWrapper.setPreferenceFloatValue(PreferencesWrapper.SND_SPEAKER_LEVEL, (float) 1.5);
				prefWrapper.setPreferenceBooleanValue(PreferencesWrapper.USE_ROUTING_API, true);
			}
			prefWrapper.setPreferenceBooleanValue(PreferencesWrapper.USE_MODE_API, shouldUseModeApi());
			prefWrapper.setPreferenceBooleanValue(PreferencesWrapper.KEEP_AWAKE_IN_CALL, needPspWorkaround(prefWrapper));
		}
		
	}


}

