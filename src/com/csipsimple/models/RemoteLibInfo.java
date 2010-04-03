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
	private String mLabel;
	private String mChangelog;
	private String mDescription;
	
	private boolean isValid = false;
	

	public RemoteLibInfo(Parcel in) {
        readFromParcel(in);
	}

	

	public RemoteLibInfo(JSONObject stack) {
		JSONObject latestUpJSON;
		try {
			latestUpJSON = stack.getJSONArray("versions").getJSONObject(0);
			mChangelog = latestUpJSON.getString("changelog");
			mVersion = latestUpJSON.getString("version");
			mURI= URI.create(latestUpJSON.getString("download_url"));
			mDescription = stack.getString("description");
			mLabel = stack.getString("label");
			isValid = true;
		} catch (JSONException e) {
			isValid = false;
		}
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

}
