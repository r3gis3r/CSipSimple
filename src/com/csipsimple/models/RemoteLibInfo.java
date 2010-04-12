/**
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

public class RemoteLibInfo implements Parcelable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5622422102685917690L;
	public int PrimaryKey = -1;
	private String mFileName;
	private File mFilePath;
	private URI mURI;
	private String mVersion;
	
	//Blabla
	private String mId;
	private String mLabel;
	private String mChangelog;
	private String mDescription;

	private static final String THIS_FILE = "RemoteLib";
	

	public RemoteLibInfo(Parcel in) {
        readFromParcel(in);
	}

	

	public RemoteLibInfo(JSONObject stack) throws JSONException {
		JSONObject latestUpJSON;
		latestUpJSON = stack.getJSONArray("versions").getJSONObject(0);
		mChangelog = latestUpJSON.getString("changelog");
		mVersion = latestUpJSON.getString("version");
		mURI= URI.create(latestUpJSON.getString("download_url"));
		mDescription = stack.getString("description");
		mLabel = stack.getString("label");
		mId = stack.getString("id");
	}

	@Override
	public int describeContents() {
		return 0;
	}

    public void readFromParcel(Parcel in){
            PrimaryKey = in.readInt();
            mFileName = in.readString();
            mFilePath = new File(in.readString());
            mURI = URI.create(in.readString());
            mVersion = in.readString();
            mLabel = in.readString();
            mChangelog = in.readString();
            mDescription = in.readString();
            mId = in.readString();
    }
    
	@Override
    public void writeToParcel(Parcel arg0, int arg1){
            arg0.writeInt(PrimaryKey);
            arg0.writeString(mFileName);
            arg0.writeString(mFilePath.getAbsolutePath());
            arg0.writeString(mURI.toString());
            arg0.writeString(mVersion);
            arg0.writeString(mLabel);
            arg0.writeString(mChangelog);
            arg0.writeString(mDescription);
            arg0.writeString(mId);
    }

	
	

	public static final Parcelable.Creator<RemoteLibInfo> CREATOR = new Parcelable.Creator<RemoteLibInfo>() {
		public RemoteLibInfo createFromParcel(Parcel in) {
			return new RemoteLibInfo(in);
		}

		public RemoteLibInfo[] newArray(int size) {
			return new RemoteLibInfo[size];
		}
	};

	public String getFileName() {
		return mFileName;
	}

	public void setFileName(String fileName) {
		mFileName = fileName;
	}

	public URI getDownloadURI() {
		return mURI;
	}

	/**
	 * @param mFilePath
	 *            the mFilePath to set
	 */
	public void setFilePath(File mFilePath) {
		this.mFilePath = mFilePath;
	}

	/**
	 * @return the mFilePath
	 */
	public File getFilePath() {
		return mFilePath;
	}



	public String getId() {
		return mId;
	}



	public void setId(String id) {
		mId = id;
	}



	public String getVersion() {
		return mVersion;
	}



	public void setVersion(String version) {
		mVersion = version;
	}



	public boolean isMoreUpToDateThan(String oldStackVersion) {
		if(oldStackVersion.equalsIgnoreCase("0.01-00")) {
			//FORCE SINCE WE MADE A PRIMARY BAD COMMIT
			return true;
		}
		String[] split_master_old = oldStackVersion.split("\\.");
		String[] split_master = mVersion.split("\\.");
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
