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
package com.csipsimple.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
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
import com.csipsimple.utils.Log;

public class WelcomeScreen extends Activity {
	
	private static final String THIS_FILE = "WelcomeScreen";
	
	private static final String DEFAULT_UPDATE_URI = "http://csipsimple.googlecode.com/svn/trunk/pjsip_android/update.json";
	//WARNING this is used in 0.00-13
	//private static final String DEFAULT_UPDATE_URI = "http://csipsimple.googlecode.com/svn/trunk/pjsip_android/dev_update.json";
	//"http://10.0.2.2/android/update.json"
	
	private RemoteLibInfo currentDownload;

	private ProgressBar progressBar;
	private TextView progressBarText;
	private Button nextButton;

	private SharedPreferences prefs;
	
	public static String KEY_MODE = "mode";
	public static int MODE_WELCOME = 0;
	public static int MODE_CHANGELOG = 1;
	private int mode = MODE_WELCOME;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mode =  extras.getInt(KEY_MODE, MODE_WELCOME);
		}

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		setContentView(R.layout.welcome);
		//Update welcome screen with the good html value
        TextView textContent = (TextView) findViewById(R.id.FirstLaunchText);
        if(mode == MODE_WELCOME){
        	textContent.setText(Html.fromHtml(getString(R.string.first_launch_text)));
        }else{
        	textContent.setText(Html.fromHtml(getString(R.string.changelog_text)));
        }
        progressBar = (ProgressBar) findViewById(R.id.dl_progressbar);
        progressBarText = (TextView) findViewById(R.id.dl_text);
        nextButton = (Button) findViewById(R.id.next_button);
        nextButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//and now we can go to home
				Intent homeIntent = new Intent(WelcomeScreen.this, SipHome.class);
				homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(homeIntent);
				
				finish();
				
			}
		});
        
        progressBar.setMax(100);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		setStepInit();
		try {
			if (service != null && service.isDownloadRunning()) {
				currentDownload = service.getCurrentRemoteLib();
				service.registerCallback(callback);
				bound = true;
			} else {
				Intent serviceIntent = new Intent(this, DownloadLibService.class);
				startService(serviceIntent);
				bound = bindService(serviceIntent, connection, 0);
			}
		} catch (RemoteException ex) {
			Log.e(THIS_FILE, "Error on DownloadService call", ex);
			service = null;
			finish();
		}

		// TODO: update fields
	}


	@Override
	protected void onPause() {
		super.onPause();
		try {
			if (service != null /*&& !service.isDownloadRunning()*/ && bound) {
				unbindService(connection);
				bound = false;
			}
		} catch (Exception e) {
			Log.e(THIS_FILE, "Exception on unbinding service", e);
		}
	}
	
	// --- 
	// UI update
	// ---
	private void setStepInit() {
		Log.d(THIS_FILE, "Step init ...");
		progressBar.setIndeterminate(true);
		nextButton.setVisibility(View.GONE);
		progressBar.setVisibility(View.VISIBLE);
		progressBarText.setVisibility(View.VISIBLE);
		
		progressBarText.setText(getString(R.string.intializing_text));
	}
	
	private void setStepGetLibForDevice() {
		progressBarText.setText(getString(R.string.detecting_text));
	}
	
	private void setStepStartDownloading() {
		progressBar.setIndeterminate(false);
		progressBar.setProgress(0);
		progressBarText.setText(getString(R.string.downloading_text)+" 0%");
	}
	
	private void updateDownloadProgress(long downloaded, long total) {
		int progress = (int) (100.0*downloaded/total);
		progressBar.setProgress(progress);
		progressBarText.setText(getString(R.string.downloading_text)+" "+progress+"%");
		
	}
	
	private void setStepDownloadFinished() {
		progressBar.setIndeterminate(true);
		progressBarText.setText(getString(R.string.installing_library_text));
	}
	
	private void setStepInstalled() {
		Log.d(THIS_FILE, "Set step installed");
		nextButton.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.GONE);
		progressBarText.setVisibility(View.GONE);
		
	}
	
	private void setStepError(String error) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
	    alertDialog.setTitle("Oups there is a problem...");
	    alertDialog.setMessage(error);
	    alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialog, int which) {
	    	  WelcomeScreen.this.finish();
	        return;
	    } }); 
	    try {
	    	alertDialog.show();
	    }catch(Exception e) {
	    	Log.e(THIS_FILE, "error while trying to show dialog : TODO defer it later : (in case activity not yet running)");
	    }
	}
	
	
	// ---
	// Link to the service
	// ---
	private IDownloadLibService service;
	private boolean bound;
	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder aService) {
			service = IDownloadLibService.Stub.asInterface(aService);
			try {
				service.registerCallback(callback);
			} catch (RemoteException e) {
			}
			// Start looking for latest update
			Thread t = new Thread() {
				public void run() {
					try {
						handler.sendMessage(handler.obtainMessage(GET_LIB));
						currentDownload = service.getLibForDevice(DEFAULT_UPDATE_URI, "sip_core");
						
						if(currentDownload != null) {
							Log.d(THIS_FILE, "We have a library for you : "+currentDownload.getDownloadUri().toString());
							String old_stack_version = prefs.getString("current_stack_version", "0.00-00");
							
							Log.d(THIS_FILE, "Compare old : "+old_stack_version+ " et "+currentDownload.getVersion());
							if(currentDownload.isMoreUpToDateThan(old_stack_version)) {
								handler.sendMessage(handler.obtainMessage(DOWNLOAD_STARTED));
								currentDownload.setFileName( SipService.STACK_FILE_NAME );
								currentDownload.setFilePath( SipService.getGuessedStackLibFile(WelcomeScreen.this).getParentFile() );
								
								if (service.isDownloadRunning()) {
									//TODO : check whether it's a sip path.
									currentDownload = service.getCurrentRemoteLib();
								} else {
									service.startDownload(currentDownload);
								}
							}else {
								Log.d(THIS_FILE, "Nothing to update...");
								handler.sendMessage(handler.obtainMessage(INSTALLED));
							}
							
						}else {
							
							Log.e(THIS_FILE, "No lib have been found...");
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
				service.unregisterCallback(callback);
			} catch (RemoteException e) {
			}
			service = null;
		}
	};
	
	// ----
	// Link from the service (callback)
	// ----
    private static final int UPDATE_DOWNLOAD_PROGRESS = 1;
    private static final int DOWNLOAD_FINISHED = 2;
    private static final int DOWNLOAD_ERROR = 3;
    private static final int INCOMPATIBLE_HARDWARE = 4;
    private static final int DOWNLOAD_STARTED = 5;
    private static final int GET_LIB = 6;
    private static final int INSTALLED = 7;

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case GET_LIB:
				setStepGetLibForDevice();
				break;
			case DOWNLOAD_STARTED:
				setStepStartDownloading();
				break;
			case UPDATE_DOWNLOAD_PROGRESS:
				DownloadProgress downloadProgress = (DownloadProgress) msg.obj;
				updateDownloadProgress(downloadProgress.getDownloaded(), downloadProgress.getTotal());
				break;
			case DOWNLOAD_FINISHED:
				RemoteLibInfo library = (RemoteLibInfo) msg.obj;
				setStepDownloadFinished();
				try {
					if(service.installLib(library)) {
						Log.d(THIS_FILE, "Is installed ok");
		    			PackageInfo pinfo = getPackageManager().getPackageInfo(WelcomeScreen.this.getPackageName(), 0);
		    			int running_version = pinfo.versionCode;
	    				Editor editor = prefs.edit();
	    				editor.putInt(SipHome.LAST_KNOWN_VERSION_PREF, running_version);
	    				editor.commit();
						setStepInstalled();
					}else {
						Log.d(THIS_FILE, "Failed to install");
						setStepError("Failed to install");
					}
				} catch (RemoteException e) {
					Log.d(THIS_FILE, "Remote exception ", e);
				} catch (NameNotFoundException e) {
					//Will never happen
				}
				
				if (service != null && bound) {
					try {
						service.forceStopService();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					unbindService(connection);
					bound = false;
				}
				
				
				break;
			case INSTALLED:
				Log.d(THIS_FILE, "Is installed ok (here nothing was done in fact)");
    			PackageInfo pinfo;
				try {
					pinfo = getPackageManager().getPackageInfo(WelcomeScreen.this.getPackageName(), 0);
					int running_version = pinfo.versionCode;
					Editor editor = prefs.edit();
					editor.putInt(SipHome.LAST_KNOWN_VERSION_PREF, running_version);
					editor.commit();
					setStepInstalled();
				} catch (NameNotFoundException e) {
					//Will never happen
				}
				break;
			case INCOMPATIBLE_HARDWARE:
				setStepError("Can't find library for your device please report the problem on www.csipsimple.com");
				break;
			case DOWNLOAD_ERROR:
				setStepError("Download error. Check your connection and retry");
				if (service != null && bound) {
					try {
						unbindService(connection);
					}catch(IllegalArgumentException e) {
						//Nothing to do, just ignore that we were not able to unbind... we were already unbinded
						//python style ignore
					}
					bound = false;
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}

	};

	private IDownloadLibServiceCallback callback = new IDownloadLibServiceCallback.Stub() {

		@Override
		public void onDownloadError() throws RemoteException {
			handler.sendMessage(handler.obtainMessage(DOWNLOAD_ERROR));

		}

		@Override
		public void onDownloadFinished(RemoteLibInfo u) throws RemoteException {
			handler.sendMessage(handler.obtainMessage(DOWNLOAD_FINISHED, u));
		}

		@Override
		public void updateDownloadProgress(long downloaded, long total) throws RemoteException {
			handler.sendMessage(handler.obtainMessage(UPDATE_DOWNLOAD_PROGRESS, new DownloadProgress(downloaded, total)));

		}
	};

	
}
