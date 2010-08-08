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

public class Log {
	private static int LOG_LEVEL = 1;
	
	public static void setLogLevel(int level) {
		LOG_LEVEL = level;
	}
	
	public static void v(String tag, String msg) {
		if(LOG_LEVEL >= 5) {
			android.util.Log.v(tag, msg);
		}
	}
	
	public static void v(String tag, String msg, Throwable tr) {
		if(LOG_LEVEL >= 5) {
			android.util.Log.v(tag, msg, tr);
		}
	}
	
	public static void d(String tag, String msg) {
		if(LOG_LEVEL >= 4) {
			android.util.Log.d(tag, msg);
		}
	}
	
	public static void d(String tag, String msg, Throwable tr) {
		if(LOG_LEVEL >= 4) {
			android.util.Log.d(tag, msg, tr);
		}
	}
	
	
	public static void i(String tag, String msg) {
		if(LOG_LEVEL >= 3) {
			android.util.Log.i(tag, msg);
		}
	}
	
	static void i(String tag, String msg, Throwable tr) {
		if(LOG_LEVEL >= 3) {
			android.util.Log.i(tag, msg, tr);
		}
	}
	
	public static void w(String tag, String msg) {
		if(LOG_LEVEL >= 2) {
			android.util.Log.w(tag, msg);
		}
	}
	
	public static void w(String tag, String msg, Throwable tr) {
		if(LOG_LEVEL >= 2) {
			android.util.Log.w(tag, msg, tr);
		}
	}
	
	public static void e(String tag, String msg) {
		if(LOG_LEVEL >= 1) {
			android.util.Log.e(tag, msg);
		}
	}
	
	public static void e(String tag, String msg, Throwable tr) {
		if(LOG_LEVEL >= 1) {
			android.util.Log.e(tag, msg, tr);
		}
	}
	
}
