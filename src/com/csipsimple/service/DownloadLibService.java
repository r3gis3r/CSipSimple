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

/**
 * Info : this code is deeply inspired from CMUpdater source code
 */
package com.csipsimple.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import com.csipsimple.utils.Log;
import android.widget.Toast;

import com.csipsimple.R;
import com.csipsimple.models.RemoteLibInfo;
import com.csipsimple.utils.MD5;

public class DownloadLibService extends Service {

	private static final String THIS_FILE = "DownloadLibService";
	private static final int BUFFER = 2048;
	private WifiLock mWifiLock;
	private ConnectivityManager mConnectivityManager;
	private ConnectionChangeReceiver myConnectionChangeReceiver;
	private boolean connected;
	private final RemoteCallbackList<IDownloadLibServiceCallback> mCallbacks = new RemoteCallbackList<IDownloadLibServiceCallback>();
	private RemoteLibInfo mCurrentUpdate;
	private boolean hasToCancelDownload;
	private boolean mDownloading;
	private long downloadedFileSize;
	private long mTotalDownloaded;
	private long mContentLength;
	private SharedPreferences prefs;

	// Implement public interface for the service
	private final IDownloadLibService.Stub mBinder = new IDownloadLibService.Stub() {

		@Override
		public void startDownload(RemoteLibInfo lib) throws RemoteException {
			mDownloading = true;
			boolean success = checkForConnectionAndDownload(lib);
			mDownloading = false;
			if(success) {
				downloadFinished();
			}else {
				downloadError();
			}
		}

		@Override
		public boolean isDownloadRunning() throws RemoteException {
			return mDownloading;
		}

		@Override
		public boolean cancelDownload() throws RemoteException {
			return cancelCurrentDownload();
		}

		@Override
		public RemoteLibInfo getCurrentRemoteLib() throws RemoteException {
			return mCurrentUpdate;
		}

		@Override
		public void registerCallback(IDownloadLibServiceCallback cb) throws RemoteException {
			if (cb != null) {
				mCallbacks.register(cb);
			}
		}

		@Override
		public void unregisterCallback(IDownloadLibServiceCallback cb) throws RemoteException {
			if (cb != null) {
				mCallbacks.unregister(cb);
			}
		}

		@Override
		public RemoteLibInfo getLibForDevice(String uri, String for_what) throws RemoteException {
			return getLibUpdate(URI.create(uri), for_what);
		}

		@Override
		public boolean installLib(RemoteLibInfo lib) throws RemoteException {
			return installRemoteLib(lib);
		}

		@Override
		public void forceStopService() throws RemoteException {
			stopSelf();
		}

		@Override
		public void stopDownload() throws RemoteException {
			cancelCurrentDownload();
		}

	};

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		Log.d(THIS_FILE, "Download Lib Service started");
		
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// Lock wifi if possible to ensure download will be done
		mWifiLock = ((WifiManager) getSystemService(WIFI_SERVICE)).createWifiLock("com.csipsimple.service.DownloadLibService");
		mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		myConnectionChangeReceiver = new ConnectionChangeReceiver();
		registerReceiver(myConnectionChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		NetworkInfo.State state = mConnectivityManager.getActiveNetworkInfo().getState();
		connected = (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.SUSPENDED);
	}

	
	@Override
	public void onDestroy() {
		mCallbacks.kill();
		unregisterReceiver(myConnectionChangeReceiver);
		
		super.onDestroy();
	}

	private boolean checkForConnectionAndDownload(RemoteLibInfo updateToDownload) {
		mCurrentUpdate = updateToDownload;

		boolean success;
		mWifiLock.acquire();

		// wait for a data connection
		while (!connected) {
			synchronized (mConnectivityManager) {
				try {
					mConnectivityManager.wait();
					break;
				} catch (InterruptedException e) {
					Log.e(THIS_FILE, "Error in connectivityManager.wait", e);
				}
			}
		}
		try {
			Log.d(THIS_FILE, "we will downlad : "+updateToDownload.getFileName()+" from "+updateToDownload.getDownloadURI().toString());
			success = downloadFile(updateToDownload);
		} catch (RuntimeException ex) {
			Log.e(THIS_FILE, "RuntimeEx while downloading file", ex);
			notificateDownloadError(ex.getMessage());
			return false;
		} catch (IOException ex) {
			Log.e(THIS_FILE, "Exception while downloading file", ex);
			notificateDownloadError(ex.getMessage());
			return false;
		} finally {
			mWifiLock.release();
		}
		// Be sure to return false if the User canceled the Download
		if (hasToCancelDownload) {
			return false;
		} else {
			return success;
		}
	}

	private void notificateDownloadError(String message) {
		// TODO Auto-generated method stub

	}

	private boolean downloadFile(RemoteLibInfo updateInfo) throws IOException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpClient MD5httpClient = new DefaultHttpClient();

		HttpUriRequest req, md5req;
		HttpResponse response, md5response;

		URI updateURI;
		File destinationFile = null;
		File partialDestinationFile = null;

		String downloadedMD5 = null;

		String fileName = updateInfo.getFileName();
		File filePath = updateInfo.getFilePath();

		// Set the Filename to update.zip.partial
		partialDestinationFile = new File(filePath, fileName + ".part");
		destinationFile = new File(filePath, fileName + ".gz");
		
		if (partialDestinationFile.exists()) {
			partialDestinationFile.delete();
		}
		downloadedFileSize = 0;
		if (!hasToCancelDownload) {
			updateURI = updateInfo.getDownloadURI();

			boolean md5Available = true;

			try {
				req = new HttpGet(updateURI);
				md5req = new HttpGet(updateURI + ".md5sum");

				// Add no-cache Header, so the File gets downloaded each time
				req.addHeader("Cache-Control", "no-cache");
				md5req.addHeader("Cache-Control", "no-cache");
				//Proceed request
				md5response = MD5httpClient.execute(md5req);
				response = httpClient.execute(req);
				
				//Get responses codes
				int serverResponse = response.getStatusLine().getStatusCode();
				int md5serverResponse = md5response.getStatusLine().getStatusCode();
				
				if (md5serverResponse != HttpStatus.SC_OK) {
					md5Available = false;
				}
				
				if (serverResponse == HttpStatus.SC_OK) {
					if (md5Available) {
						//Get the md5 sum and save it into a string
						try {
							HttpEntity temp = md5response.getEntity();
							InputStreamReader isr = new InputStreamReader(temp.getContent());
							BufferedReader br = new BufferedReader(isr);
							downloadedMD5 = br.readLine().split("  ")[0];
							br.close();
							isr.close();

							if (temp != null) {
								temp.consumeContent();
							}
						} catch (IOException e) {
							//Nothing to do, just imagine there is a problem with download
							md5Available = false;
						}
					}

					// Download Update ZIP if md5sum went ok
					HttpEntity entity = response.getEntity();
					dumpFile(entity, partialDestinationFile, destinationFile);
					//Will cancel download
					if (hasToCancelDownload) {
						mToastHandler.sendMessage(mToastHandler.obtainMessage(0, R.string.unable_to_download_file, 0));
						return false;
					}
					if (entity != null && !hasToCancelDownload) {
						entity.consumeContent();
					} else {
						entity = null;
					}

					if (md5Available) {
						if (!MD5.checkMD5(downloadedMD5, destinationFile)) {
							throw new IOException("md5_verification_failed");
						}
					}
					
					return true;
				}
			} catch (IOException ex) {
				mToastHandler.sendMessage(mToastHandler.obtainMessage(0, ex.getMessage()));
			}
			if (Thread.currentThread().isInterrupted() || !Thread.currentThread().isAlive()) {
				mToastHandler.sendMessage(mToastHandler.obtainMessage(0, R.string.unable_to_download_file, 0));
				return false;
			}
		}

		mToastHandler.sendMessage(mToastHandler.obtainMessage(0, R.string.unable_to_download_file, 0));
		return false;
	}

	private void dumpFile(HttpEntity entity, File partialDestinationFile, File destinationFile) throws IOException {
		if (!hasToCancelDownload) {
			mContentLength = (int) entity.getContentLength();
			if (mContentLength <= 0) {
				mContentLength = 1024;
			}
			
			byte[] buff = new byte[64 * 1024];
			int read = 0;
			RandomAccessFile out = new RandomAccessFile(partialDestinationFile, "rw");
			out.seek(downloadedFileSize);
			InputStream is = entity.getContent();
			TimerTask progressUpdateTimerTask = new TimerTask() {
				@Override
				public void run() {
					onProgressUpdate();
				}
			};
			Timer progressUpdateTimer = new Timer();
			try {
				// If File exists, set the Progress to it. Otherwise it will be
				// initial 0
				mTotalDownloaded = downloadedFileSize;
				progressUpdateTimer.scheduleAtFixedRate(progressUpdateTimerTask, 100, 100);
				while ((read = is.read(buff)) > 0 && !hasToCancelDownload) {
					out.write(buff, 0, read);
					mTotalDownloaded += read;
				}
				out.close();
				is.close();
				if (!hasToCancelDownload) {
					partialDestinationFile.renameTo(destinationFile);
				}
			} catch (IOException e) {
				out.close();
				try {
					destinationFile.delete();
				} catch (SecurityException ex) {
					Log.e(THIS_FILE, "Unable to delete downloaded File. Continue anyway.", ex);
				}
			} finally {
				progressUpdateTimer.cancel();
				buff = null;
			}
		}
	}
	
	private boolean cancelCurrentDownload() {
		hasToCancelDownload = true;
		String fileName = mCurrentUpdate.getFileName();
		File filePath = mCurrentUpdate.getFilePath();

		File partialDestinationFile = new File(filePath, fileName + ".part");
		File destinationFile = new File(filePath, fileName + ".gz");

		if (partialDestinationFile.exists()) {
			partialDestinationFile.delete();
		}
		if (destinationFile.exists()) {
			destinationFile.delete();
		}
		mDownloading = false;
		stopSelf();
		return true;
	}

	private void onProgressUpdate() {
		if (!hasToCancelDownload) {
			long contentLengthOfFullDownload = mContentLength + downloadedFileSize;
			// Update the DownloadProgress
			updateDownloadProgress(mTotalDownloaded, (int) contentLengthOfFullDownload);
		}
	}

	private void updateDownloadProgress(final long downloaded, final int total) {
		final int n = mCallbacks.beginBroadcast();
		for (int i = 0; i < n; i++) {
			try {
				mCallbacks.getBroadcastItem(i).updateDownloadProgress(downloaded, total);
			} catch (RemoteException e) {
				//Should not happen
			}
		}
		mCallbacks.finishBroadcast();
	}
	
	
	private void downloadFinished() {
		final int n = mCallbacks.beginBroadcast();
		for (int i = 0; i < n; i++) {
			try {
				mCallbacks.getBroadcastItem(i).onDownloadFinished(mCurrentUpdate);
			} catch (RemoteException e) {
				//Should not happen
			}
		}
		mCallbacks.finishBroadcast();
	}

	private void downloadError() {
		final int n = mCallbacks.beginBroadcast();
		for (int i = 0; i < n; i++) {
			try {
				mCallbacks.getBroadcastItem(i).onDownloadError();
			} catch (RemoteException e) {
				//Should not happen
			}
		}
		mCallbacks.finishBroadcast();
	}

	
	
	private RemoteLibInfo getLibUpdate(URI updateServerUri, String for_what) {
		HttpClient updateHttpClient = new DefaultHttpClient();

		HttpUriRequest updateReq = new HttpGet(updateServerUri);
		updateReq.addHeader("Cache-Control", "no-cache");
		
		Log.d(THIS_FILE, "Get updates from "+updateServerUri.toString());

		try {
			HttpResponse updateResponse;
			HttpEntity updateResponseEntity = null;

			updateResponse = updateHttpClient.execute(updateReq);
			int updateServerResponse = updateResponse.getStatusLine().getStatusCode();
			if (updateServerResponse != HttpStatus.SC_OK) {
				Log.e(THIS_FILE, "can't get updates from site : "+updateResponse.getStatusLine().getReasonPhrase()+" - ");
				downloadError();
				return null;
			}

			updateResponseEntity = updateResponse.getEntity();
			BufferedReader upLineReader = new BufferedReader(new InputStreamReader(updateResponseEntity.getContent()), 2 * 1024);
			StringBuffer upBuf = new StringBuffer();
			String upLine;
			while ((upLine = upLineReader.readLine()) != null) {
				upBuf.append(upLine);
			}
			upLineReader.close();
			String content = upBuf.toString();
			try {
				JSONObject mainJSONObject = new JSONObject(content);

				JSONArray coreJSONArray = mainJSONObject.getJSONArray(for_what);

				JSONObject stack = getCompatibleStack(coreJSONArray);
				Log.d(THIS_FILE, "Here we are");
				if (stack != null) {
					Log.d(THIS_FILE, "we are about to return a stack...");
					return new RemoteLibInfo(stack);
				}
			} catch (JSONException e) {
				Log.e(THIS_FILE, "Unable to parse "+content, e);
				downloadError();
			}

		} catch (ClientProtocolException e) {
			downloadError();
		} catch (IOException e) {
			downloadError();
		}

		return null;
	}
	
	private JSONObject getCompatibleStack(JSONArray availableStacks ){
		int core_count = availableStacks.length();
		for ( int i=0; i< core_count; i++) {
			JSONObject plateform_stack;
			try{
				plateform_stack = availableStacks.getJSONObject(i);
				Log.d(THIS_FILE, "Check if stack "+plateform_stack.getString("id")+" is compatible");
				if(isCompatibleStack(plateform_stack.getJSONObject("filters"))){
					Log.d(THIS_FILE, "Found : "+plateform_stack.getString("id"));
					return plateform_stack;
				}else{
					Log.d(THIS_FILE, "NOT VALID : "+plateform_stack.getString("id"));
				}
			}catch(Exception e){
				Log.w(THIS_FILE, "INVALID FILTER FOR");
				e.printStackTrace();
			}
		}
		return null;
	}
	
	
	
	
	private boolean isCompatibleStack(JSONObject filter) throws SecurityException, NoSuchFieldException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, JSONException {
		
		//For each filter keys, we check if the filter is not invalid
		Iterator<?> iter = filter.keys();
		while(iter.hasNext()){
			//Each filter key correspond to a android class which values has to be checked
			String class_filter = (String) iter.next();
			//Get this class
			Class<?> cls = Class.forName(class_filter);
			
			//Then for this class, we have to check if each static field matches defined regexp rule
			Iterator<?> cls_iter = filter.getJSONObject(class_filter).keys();
			
			while(cls_iter.hasNext()){
				String field_name = (String) cls_iter.next();
				Field field = cls.getField(field_name);
				//Get the current value on the system
				String current_value = field.get(null).toString();
				//Get the filter for this value
				String regexp_filter = filter.getJSONObject(class_filter).getString(field_name);
				
				//Check if matches
				if(! Pattern.matches(regexp_filter, current_value)){
					Log.d(THIS_FILE, "Regexp not match : "+current_value+" matches /"+regexp_filter+"/");
					return false;
				}
			}
		}
		return true;
	}
	
	private boolean installRemoteLib(RemoteLibInfo lib) {
		String fileName = lib.getFileName();
		File filePath = lib.getFilePath();
		
		File tmp_gz = new File(filePath, fileName+".gz");
		File dest = new File(filePath, fileName);
		
		
		try {
			if(dest.exists()){
				dest.delete();
			}
			RandomAccessFile out = new RandomAccessFile(dest, "rw");
			out.seek(0);
			GZIPInputStream zis = new GZIPInputStream(new FileInputStream(tmp_gz));
			int len;
			byte[] buf = new byte[BUFFER];
	        while ((len = zis.read(buf)) > 0) {
	          out.write(buf, 0, len);
	        }
			zis.close();
			out.close();
			Log.d(THIS_FILE, "Ungzip is in : "+dest.getAbsolutePath());
			tmp_gz.delete();
			//Update preferences fields with current stack values
			Editor editor = prefs.edit();
			editor.putString("current_stack_id", lib.getId());
			editor.putString("current_stack_version", lib.getVersion());
			editor.putString("current_stack_uri", lib.getDownloadURI().toString());
			editor.commit();
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	

	private Handler mToastHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.arg1 != 0) {
				Toast.makeText(DownloadLibService.this, msg.arg1, Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(DownloadLibService.this, (String) msg.obj, Toast.LENGTH_LONG).show();
			}
		}
	};

	// Is called when Network Connection Changes
	private class ConnectionChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			android.net.NetworkInfo.State state = mConnectivityManager.getActiveNetworkInfo().getState();
			connected = (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.SUSPENDED);
		}
	}
	

}
