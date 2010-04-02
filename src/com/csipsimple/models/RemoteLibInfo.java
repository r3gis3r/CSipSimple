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

import android.os.Parcel;
import android.os.Parcelable;

public class RemoteLibInfo implements Parcelable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5622422102685917690L;
	private String mFileName;
	private File mFilePath;
	private URI mURI;

	public RemoteLibInfo(Parcel in) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub

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
