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
package com.csipsimple.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.models.DownloadProgress;
import com.csipsimple.models.RemoteLibInfo;
import com.csipsimple.service.DownloadLibService;
import com.csipsimple.service.IDownloadLibService;
import com.csipsimple.service.IDownloadLibServiceCallback;
import com.csipsimple.service.SipService;

public class WelcomeScreen extends Activity {
	
	private static final String THIS_FILE = "WelcomeScreen";
	
	private RemoteLibInfo mCurrentDownload;

	private ProgressBar mProgressBar;
	private TextView mProgressBarText;

	private Button mNextButton;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.welcome);
		//Update welcome screen with the good html value
        TextView textContent = (TextView) findViewById(R.id.FirstLaunchText);
        textContent.setText(Html.fromHtml(getString(R.string.first_launch_text)));
        
        mProgressBar = (ProgressBar) findViewById(R.id.dl_progressbar);
        mProgressBarText = (TextView) findViewById(R.id.dl_text);
        mNextButton = (Button) findViewById(R.id.next_button);
        
        mProgressBar.setMax(100);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		setStepInit();
		try {
			if (mService != null && mService.isDownloadRunning()) {
				mCurrentDownload = mService.getCurrentRemoteLib();
				mService.registerCallback(mCallback);
				mBound = true;
			} else {
				Intent serviceIntent = new Intent(this, DownloadLibService.class);
				startService(serviceIntent);
				mBound = bindService(serviceIntent, mConnection, 0);
			}
		} catch (RemoteException ex) {
			Log.e(THIS_FILE, "Error on DownloadService call", ex);
			mService = null;
			finish();
		}

		// TODO: update fields
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			if (mService != null && !mService.isDownloadRunning() && mBound) {
				unbindService(mConnection);
				mBound = false;
			}
		} catch (RemoteException e) {
			Log.e(THIS_FILE, "Exception on calling DownloadService", e);
		}
	}
	
	// --- 
	// UI update
	// ---
	private void setStepInit() {
		Log.d(THIS_FILE, "Step init ...");
		mProgressBar.setIndeterminate(true);
		mNextButton.setVisibility(View.GONE);
		mProgressBar.setVisibility(View.VISIBLE);
		mProgressBarText.setVisibility(View.VISIBLE);
		
		mProgressBarText.setText("Initialize");
	}
	
	private void setStepGetLibForDevice() {
		mProgressBarText.setText("Detecting correct library for your device");
	}
	
	private void setStepStartDownloading() {
		mProgressBar.setIndeterminate(false);
		mProgressBar.setProgress(0);
		mProgressBarText.setText("Downloading 0%");
	}
	
	private void updateDownloadProgress(long downloaded, int total) {
		int progress = (int) (100.0*downloaded/total);
		mProgressBar.setProgress(progress);
		mProgressBarText.setText("Downloading "+progress+"%");
		
	}
	
	private void setStepDownloadFinished() {
		mProgressBar.setIndeterminate(true);
		mProgressBarText.setText("Installing library...");
	}
	
	private void setStepInstalled() {
		Log.d(THIS_FILE, "Set step installed");
		mNextButton.setVisibility(View.VISIBLE);
		mProgressBar.setVisibility(View.GONE);
		mProgressBarText.setVisibility(View.GONE);
		
	}
	
	private void setStepError(String error) {
		// TODO Auto-generated method stub
	}
	
	
	// ---
	// Link with service
	// ---
	private IDownloadLibService mService;
	private boolean mBound;
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = IDownloadLibService.Stub.asInterface(service);
			try {
				mService.registerCallback(mCallback);
			} catch (RemoteException e) {
			}
			// Start looking for latest update
			Thread t = new Thread() {
				public void run() {
					try {
						setStepGetLibForDevice();
						mCurrentDownload = mService.getLibForDevice("http://10.0.2.2/android/update.json", "sip_core");
						
						if(mCurrentDownload != null) {
							Log.d(THIS_FILE, "We have a library for you : "+mCurrentDownload.getDownloadURI().toString());
							setStepStartDownloading();
							mCurrentDownload.setFileName( SipService.STACK_FILE_NAME );
							mCurrentDownload.setFilePath( SipService.getGuessedStackLibFile(WelcomeScreen.this).getParentFile() );
							
							if (mService.isDownloadRunning()) {
								//TODO : check whether it's a sip path.
								mCurrentDownload = mService.getCurrentRemoteLib();
							} else {
								mService.startDownload(mCurrentDownload);
							}
							
						}else {
							Log.d(THIS_FILE, "No lib have been found...");
						}
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Exception on calling DownloadService", e);
					}
				}
			};
			t.start();
		}

		public void onServiceDisconnected(ComponentName name) {
			try {
				mService.unregisterCallback(mCallback);
			} catch (RemoteException e) {
			}
			mService = null;
		}
	};
	
	// ----
	// Link with the service callbacks
	// ----
    private static final int UPDATE_DOWNLOAD_PROGRESS = 1;
    private static final int DOWNLOAD_FINISHED = 2;
    private static final int DOWNLOAD_ERROR = 3;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_DOWNLOAD_PROGRESS:
				DownloadProgress dp = (DownloadProgress) msg.obj;
				updateDownloadProgress(dp.getDownloaded(), dp.getTotal());
				break;
			case DOWNLOAD_FINISHED:
				RemoteLibInfo u = (RemoteLibInfo) msg.obj;
				setStepDownloadFinished();
				try {
					boolean installed = mService.installLib(u);
					if(installed) {
						Log.d(THIS_FILE, "Is installed ok");
						setStepInstalled();
					}else {
						Log.d(THIS_FILE, "Failed to install");
						setStepError("Failed to install");
					}
				} catch (RemoteException e) {
					Log.d(THIS_FILE, "Remote exception ", e);
				}
				
				if (mService != null && mBound) {
					unbindService(mConnection);
					mBound = false;
				}
				break;
			case DOWNLOAD_ERROR:
				setStepError("Download error. Check your connection and retry");
				if (mService != null && mBound) {
					unbindService(mConnection);
					mBound = false;
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}

	};

	private IDownloadLibServiceCallback mCallback = new IDownloadLibServiceCallback.Stub() {

		@Override
		public void onDownloadError() throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(DOWNLOAD_ERROR));

		}

		@Override
		public void onDownloadFinished(RemoteLibInfo u) throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(DOWNLOAD_FINISHED, u));
		}

		@Override
		public void updateDownloadProgress(long downloaded, int total) throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(UPDATE_DOWNLOAD_PROGRESS, new DownloadProgress(downloaded, total)));

		}
	};

	
}
