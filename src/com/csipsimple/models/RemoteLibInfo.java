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
package com.csipsimple.models;

import java.io.File;
import java.io.Serializable;
import java.net.URI;

import org.json.JSONException;
import org.json.JSONObject;

import com.csipsimple.utils.Log;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Parcelable/Serializable class to store information of a dynamic library (.so) stored on the network 
 * @author r3gis3r
 *
 */
public class RemoteLibInfo implements Parcelable, Serializable {
	private static final String THIS_FILE = "RemoteLib";
	
	private static final long serialVersionUID = -5622422102685917690L;
	public int PrimaryKey = -1;
	private String fileName;
	private File filePath;
	private URI uri;
	private String version;
	
	//Blabla
	private String id;
	private String label;
	private String changelog;
	private String description;

	

	public RemoteLibInfo(Parcel in) {
        readFromParcel(in);
	}

	/**
	 * Constructor from json object (retrieved on the web)
	 * @param library
	 * @throws JSONException if json is not well formated or doesn't contains everything we want
	 */
	public RemoteLibInfo(JSONObject library) throws JSONException {
		JSONObject latestVersion;
		latestVersion = library.getJSONArray("versions").getJSONObject(0);
		changelog = latestVersion.getString("changelog");
		version = latestVersion.getString("version");
		uri= URI.create(latestVersion.getString("download_url"));
		description = library.getString("description");
		label = library.getString("label");
		id = library.getString("id");
	}

	@Override
	public int describeContents() {
		return 0;
	}

    public void readFromParcel(Parcel in){
            PrimaryKey = in.readInt();
            fileName = in.readString();
            filePath = new File(in.readString());
            uri = URI.create(in.readString());
            version = in.readString();
            label = in.readString();
            changelog = in.readString();
            description = in.readString();
            id = in.readString();
    }
    
	@Override
    public void writeToParcel(Parcel arg0, int arg1){
            arg0.writeInt(PrimaryKey);
            arg0.writeString(fileName);
            arg0.writeString(filePath.getAbsolutePath());
            arg0.writeString(uri.toString());
            arg0.writeString(version);
            arg0.writeString(label);
            arg0.writeString(changelog);
            arg0.writeString(description);
            arg0.writeString(id);
    }

	
	

	public static final Parcelable.Creator<RemoteLibInfo> CREATOR = new Parcelable.Creator<RemoteLibInfo>() {
		public RemoteLibInfo createFromParcel(Parcel in) {
			return new RemoteLibInfo(in);
		}

		public RemoteLibInfo[] newArray(int size) {
			return new RemoteLibInfo[size];
		}
	};

	// Setters/Getters
	
	
	/**
	 * Set the library file name
	 * @param fileName the library file name : the name the library has to be saved to
	 */
	public void setFileName(String aFileName) {
		fileName = aFileName;
	}
	
	/**
	 * Get the library file name
	 * @return the library file name : the name the library has to be saved to
	 */
	public String getFileName() {
		return fileName;
	}
	
	
	/**
	 * Set the file path to save the library to 
	 * @param filePath  the filePath to save the library to
	 */
	public void setFilePath(File aFilePath) {
		this.filePath = aFilePath;
	}

	/**
	 * Get the file path to save the library to 
	 * @param filePath  the filePath to save the library to
	 */
	public File getFilePath() {
		return filePath;
	}

	/**
	 * Get the uri from where it the library has to be downloaded
	 * @return the library uri
	 */
	public URI getDownloadUri() {
		return uri;
	}

	/**
	 * Get the library id
	 * @return the library id (internal use)
	 */
	public String getId() {
		return id;
	}

	/**
	 * Get the library version
	 * @return the library version string, should be formated like that : \d.\d{2}-\d{2}
	 */
	public String getVersion() {
		return version;
	}


	/**
	 * Test whether this lib is more up to date than an other version of the lib
	 * @param oldLibVersion the other version to compare to
	 * @return true if our lib is more up to date
	 */
	public boolean isMoreUpToDateThan(String oldLibVersion) {
		if(oldLibVersion.equalsIgnoreCase("0.01-00")) {
			//FORCE SINCE WE MADE A PRIMARY BAD COMMIT
			return true;
		}
		//Speed up things if version is the same
		if(version.equalsIgnoreCase(oldLibVersion)) {
			return false;
		}
		
		//Else do a real comparaison parsing versions
		String[] split_master_old = oldLibVersion.split("\\.");
		String[] split_master = version.split("\\.");
		try {
			Log.d(THIS_FILE, "We have "+split_master_old.length);
			int int_master_old = Integer.decode(split_master_old[0]);
			int int_master = Integer.decode(split_master[0]);
			if(int_master>int_master_old) {
				return true;
			}
			if(int_master < int_master_old) {
				return false;
			}
			
			String[] split_build_old = split_master_old[1].split("-");
			String[] split_build = split_master[1].split("-");
			
			int int_vers_old = Integer.decode(split_build_old[0]);
			int int_vers = Integer.decode(split_build[0]);
			if(int_vers>int_vers_old) {
				return true;
			}
			if(int_vers<int_vers_old) {
				return false;
			}
			
			int int_build_old = Integer.decode(split_build_old[1]);
			int int_build = Integer.decode(split_build[1]);
			if(int_build>int_build_old) {
				return true;
			}
			if(int_build<int_build_old) {
				return false;
			}
			
		}catch(NumberFormatException e) {
			return true;
		}catch(Exception e) {
			Log.w(THIS_FILE, "Error while trying to decode versions", e);
			return true;
		}
		
		return false;
	}

}
