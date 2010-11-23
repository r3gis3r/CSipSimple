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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

public class CollectLogs {

	private static final Object LINE_SEPARATOR = "\n";
	private static final String THIS_FILE = "Collect Logs";
	

	/*Usage: logcat [options] [filterspecs]
    options include:
      -s              Set default filter to silent.
                      Like specifying filterspec '*:s'
      -f <filename>   Log to file. Default to stdout
      -r [<kbytes>]   Rotate log every kbytes. (16 if unspecified). Requires -f
      -n <count>      Sets max number of rotated logs to <count>, default 4
      -v <format>     Sets the log print format, where <format> is one of:

                      brief process tag thread raw time threadtime long

      -c              clear (flush) the entire log and exit
      -d              dump the log and then exit (don't block)
      -g              get the size of the log's ring buffer and exit
      -b <buffer>     request alternate ring buffer
                      ('main' (default), 'radio', 'events')
      -B              output the log in binary
    filterspecs are a series of
      <tag>[:priority]

    where <tag> is a log component tag (or * for all) and priority is:
      V    Verbose
      D    Debug
      I    Info
      W    Warn
      E    Error
      F    Fatal
      S    Silent (supress all output)

    '*' means '*:d' and <tag> by itself means <tag>:v

    If not specified on the commandline, filterspec is set from ANDROID_LOG_TAGS.
    If no filterspec is found, filter defaults to '*:I'

    If not specified with -v, format is set from ANDROID_PRINTF_LOG
    or defaults to "brief"*/
	public final static StringBuilder getLogs() {
        final StringBuilder log = new StringBuilder();
        try{
            ArrayList<String> commandLine = new ArrayList<String>();
            commandLine.add("logcat");//$NON-NLS-1$
            commandLine.add("-d");//$NON-NLS-1$
            commandLine.add("D");
            
            Process process = Runtime.getRuntime().exec(commandLine.toArray(new String[0]));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line ;
            while ((line = bufferedReader.readLine()) != null){ 
                log.append(line);
                log.append(LINE_SEPARATOR); 
            }
            
        } 
        catch (IOException e){
            Log.e(THIS_FILE, "Collect logs failed : ", e);//$NON-NLS-1$
            log.append("Unable to get logs : " + e.toString());
        }
        
        return log;
	}
	
	public final static StringBuilder getDeviceInfo() {
		final StringBuilder log = new StringBuilder();
		
		log.append( "Here are important informations about Device : ");
        log.append(LINE_SEPARATOR); 
        log.append("android.os.Build.BOARD : " + android.os.Build.BOARD );
        log.append(LINE_SEPARATOR); 
		log.append("android.os.Build.BRAND : " + android.os.Build.BRAND );
        log.append(LINE_SEPARATOR); 
		log.append("android.os.Build.DEVICE : " + android.os.Build.DEVICE );
        log.append(LINE_SEPARATOR); 
		log.append("android.os.Build.ID : " + android.os.Build.ID );
        log.append(LINE_SEPARATOR); 
		log.append("android.os.Build.MODEL : " + android.os.Build.MODEL );
        log.append(LINE_SEPARATOR); 
		log.append("android.os.Build.PRODUCT : " + android.os.Build.PRODUCT );
        log.append(LINE_SEPARATOR); 
		log.append("android.os.Build.TAGS : " + android.os.Build.TAGS );
        log.append(LINE_SEPARATOR); 
		log.append("android.os.Build.VERSION.INCREMENTAL : " + android.os.Build.VERSION.INCREMENTAL );
        log.append(LINE_SEPARATOR); 
		log.append("android.os.Build.VERSION.RELEASE : " + android.os.Build.VERSION.RELEASE );
        log.append(LINE_SEPARATOR); 
		log.append("android.os.Build.VERSION.SDK : " + android.os.Build.VERSION.SDK );
        log.append(LINE_SEPARATOR); 
		try {
			log.append("android.os.Build.VERSION.SDK_INT : " + android.os.Build.VERSION.SDK_INT);
	        log.append(LINE_SEPARATOR); 
		}catch(Exception e) {
			//android 1.5
		}
		
		return log;
	}
	
	public final static String getApplicationInfo(Context ctx) {
		String result = "";
		result += "Based on the GPL CSipSimple version : ";
		
		PackageInfo pinfo;
		try {
			pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
			result += pinfo.versionName + " r" + pinfo.versionCode;
		} catch (NameNotFoundException e) {
			Log.e(THIS_FILE, "Impossible to find version of current package !!");
		}
		return result;
	}
	
	public static Intent getLogReportIntent(String userComment, Context ctx) {
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "CSipSimple Error-Log report");
        sendIntent.setType("text/plain");//$NON-NLS-1$
        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { CustomDistribution.getSupportEmail() });
        
        StringBuilder log = new StringBuilder();
        log.append(userComment);
        log.append(LINE_SEPARATOR);
        log.append(LINE_SEPARATOR);
        log.append(getApplicationInfo(ctx));
        log.append(LINE_SEPARATOR);
        log.append(getDeviceInfo());
        log.append(LINE_SEPARATOR);
        log.append(getLogs());
        log.append(LINE_SEPARATOR);
        log.append(LINE_SEPARATOR);
        log.append(userComment);
        
        sendIntent.putExtra(Intent.EXTRA_TEXT, log.toString());
        
        return sendIntent;
	}
}
