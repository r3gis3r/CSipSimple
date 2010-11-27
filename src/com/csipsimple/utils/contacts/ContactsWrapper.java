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
package com.csipsimple.utils.contacts;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.CustomDistribution;

public abstract class ContactsWrapper {
	private static ContactsWrapper instance;
	
	public static ContactsWrapper getInstance() {
		if(instance == null) {
			String className = CustomDistribution.getRootPackage() + ".utils.contacts.ContactsUtils";
			if(Compatibility.isCompatible(5)) {
				className += "5";
			}else {
				className += "3";
			}
			try {
                Class<? extends ContactsWrapper> wrappedClass = Class.forName(className).asSubclass(ContactsWrapper.class);
                instance = wrappedClass.newInstance();
	        } catch (Exception e) {
	        	throw new IllegalStateException(e);
	        }
		}
		
		return instance;
	}
	
	protected ContactsWrapper() {}
	
	public abstract Bitmap getContactPhoto(Context ctxt, Uri uri, Integer defaultResource);
	public abstract ArrayList<Phone> getPhoneNumbers(Context ctxt, String id);
	
	public class Phone {
		private String number;
		private String type;

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Phone(String n, String t) {
			this.number = n;
			this.type = t;
		}
	}
}
