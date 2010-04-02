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

/**
 * Info : this code is deeply inspired from CMUpdater source code
 */
package com.csipsimple.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import com.csipsimple.models.RemoteLibInfo;
import com.csipsimple.utils.MD5;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import com.csipsimple.R;
import com.csipsimple.service.IDownloadLibService;
import com.csipsimple.service.IDownloadLibServiceCallback;

public class DownloadLibService extends Service {

	private static final String THIS_FILE = "DownloadLibService";
	private WifiLock mWifiLock;
	private ConnectivityManager mConnectivityManager;
	private ConnectionChangeReceiver myConnectionChangeReceiver;
	private boolean connected;
	private final RemoteCallbackList<IDownloadLibServiceCallback> mCallbacks = new RemoteCallbackList<IDownloadLibServiceCallback>();
	private RemoteLibInfo mCurrentUpdate;
	private boolean prepareForDownloadCancel;

	// Implement public interface for the service
	private final IDownloadLibService.Stub mBinder = new IDownloadLibService.Stub() {

		@Override
		public void startDownload(RemoteLibInfo lib) throws RemoteException {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean isDownloadRunning() throws RemoteException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean pauseDownload() throws RemoteException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean cancelDownload() throws RemoteException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public RemoteLibInfo getCurrentUpdate() throws RemoteException {
			// TODO Auto-generated method stub
			return null;
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

	};
	private long localFileSize;

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		// Lock wifi if possible to ensure download will be done
		mWifiLock = ((WifiManager) getSystemService(WIFI_SERVICE)).createWifiLock("com.csipsimple.service.DownloadLibService");
		mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		myConnectionChangeReceiver = new ConnectionChangeReceiver();
		registerReceiver(myConnectionChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		android.net.NetworkInfo.State state = mConnectivityManager.getActiveNetworkInfo().getState();
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
		if (prepareForDownloadCancel) {
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
			localFileSize = partialDestinationFile.length();
		}
		if (!prepareForDownloadCancel) {
			updateURI = updateInfo.getDownloadURI();

			boolean md5Available = true;

			try {
				req = new HttpGet(updateURI);
				md5req = new HttpGet(updateURI + ".md5sum");

				// Add no-cache Header, so the File gets downloaded each time
				req.addHeader("Cache-Control", "no-cache");
				md5req.addHeader("Cache-Control", "no-cache");
				md5response = MD5httpClient.execute(md5req);

				if (localFileSize > 0) {
					req.addHeader("Range", "bytes=" + localFileSize + "-");
				}
				response = httpClient.execute(req);

				int serverResponse = response.getStatusLine().getStatusCode();
				int md5serverResponse = md5response.getStatusLine().getStatusCode();

				if (serverResponse == HttpStatus.SC_NOT_FOUND) {

				} else if (serverResponse != HttpStatus.SC_OK && serverResponse != HttpStatus.SC_PARTIAL_CONTENT) {

				} else {
					// server must support partial content for resume
					if (localFileSize > 0 && serverResponse != HttpStatus.SC_PARTIAL_CONTENT) {
						ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.download_resume_not_supported, 0));
						// To get the UdpateProgressBar working correctly, when
						// server does not support resume
						localFileSize = 0;
					} else if (localFileSize > 0 && serverResponse == HttpStatus.SC_PARTIAL_CONTENT) {
						ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.download_resume_download, 0));
					}

					if (md5serverResponse != HttpStatus.SC_OK) {
						md5Available = false;

					}

					if (md5Available) {

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
							// TODO: Do not throw, continue with zipfile
							// download
							throw new IOException("MD5 Response cannot be read");
						}
					}

					// Download Update ZIP if md5sum went ok
					HttpEntity entity = response.getEntity();
					// dumpFile(entity, partialDestinationFile,
					// destinationFile);
					// Was the download canceled?
					if (prepareForDownloadCancel) {
						ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.unable_to_download_file, 0));
						return false;
					}
					if (entity != null && !prepareForDownloadCancel) {
						entity.consumeContent();
					} else {
						entity = null;
					}

					if (md5Available) {
						if (!MD5.checkMD5(downloadedMD5, destinationFile)) {
							throw new IOException("md5_verification_failed");
						}
					}

					// If we reach here, download & MD5 check went fine :)
					return true;
				}
			} catch (IOException ex) {
				ToastHandler.sendMessage(ToastHandler.obtainMessage(0, ex.getMessage()));
			}
			if (Thread.currentThread().isInterrupted() || !Thread.currentThread().isAlive()) {
				ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.unable_to_download_file, 0));
				return false;
			}
		}

		ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.unable_to_download_file, 0));
		return false;
	}

	private Handler ToastHandler = new Handler() {
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
