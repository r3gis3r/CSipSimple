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
package com.csipsimple.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import com.csipsimple.R;
import com.csipsimple.ui.SipHome;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.MD5;

public class Downloader extends IntentService {
	private final static String THIS_FILE = "Downloader";

	public final static String EXTRA_ICON = "icon";
	public final static String EXTRA_TITLE = "title";
	public final static String EXTRA_OUTPATH = "outpath";
	public final static String EXTRA_CHECK_MD5 = "checkMd5";
	public final static String EXTRA_PENDING_FINISH_INTENT = "pendingIntent";

	private static final int NOTIF_DOWNLOAD = 0;

	private NotificationManager notificationManager;
	private DefaultHttpClient client;

	public Downloader() {
		super("Downloader");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		client = new DefaultHttpClient();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		client.getConnectionManager().shutdown();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		HttpGet getMethod = new HttpGet(intent.getData().toString());
		int result = Activity.RESULT_CANCELED;
		String outPath = intent.getStringExtra(EXTRA_OUTPATH);
		boolean checkMd5 = intent.getBooleanExtra(EXTRA_CHECK_MD5, false);
		int icon = intent.getIntExtra(EXTRA_ICON, 0);
		String title = intent.getStringExtra(EXTRA_TITLE);
		boolean showNotif = (icon > 0 && !TextUtils.isEmpty(title));
		
		final Notification notification = showNotif ? new Notification(android.R.drawable.stat_sys_download, title, System.currentTimeMillis()) : null;
		if(notification != null) {
			notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;
			Intent i = new Intent(this, SipHome.class);
			notification.contentIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
	        notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_notif);
	        notification.contentView.setImageViewResource(R.id.status_icon, icon);
	        notification.contentView.setTextViewText(R.id.status_text, getResources().getString(R.string.downloading_text));
	        notification.contentView.setProgressBar(R.id.status_progress, 50, 0, false);
	        notification.contentView.setViewVisibility(R.id.status_progress_wrapper, View.VISIBLE);
		}
		
		if(!TextUtils.isEmpty(outPath)) {
			try {
				File output = new File(outPath);
				if (output.exists()) {
					output.delete();
				}
				
				if(notification != null) {
					notificationManager.notify(NOTIF_DOWNLOAD, notification);
				}
				ResponseHandler<Boolean> responseHandler = new FileStreamResponseHandler(output, new Progress() {
					private int oldState = 0;
					@Override
					public void run(long progress, long total) {
						//Log.d(THIS_FILE, "Progress is "+progress+" on "+total);
						int newState = (int) Math.round(progress * 50.0f/total);
						if(oldState != newState) {
							
					        notification.contentView.setProgressBar(R.id.status_progress, 50, newState, false);
					        notificationManager.notify(NOTIF_DOWNLOAD, notification);
					        oldState = newState;
						}

					}
				});
				boolean hasReply = client.execute(getMethod, responseHandler);
				
				if(hasReply) {
				
					if(checkMd5) {
						URL url = new URL(intent.getData().toString().concat(".md5sum"));
						InputStream content = (InputStream) url.getContent();
						if(content != null) {
							BufferedReader br = new BufferedReader(new InputStreamReader(content));
							String downloadedMD5 = "";
							try {
								downloadedMD5 = br.readLine().split("  ")[0];
							} catch (NullPointerException e) {
								throw new IOException("md5_verification : no sum on server");
							}
							if (!MD5.checkMD5(downloadedMD5, output)) {
								throw new IOException("md5_verification : incorrect");
							}
						}
					}
					PendingIntent pendingIntent = (PendingIntent) intent.getParcelableExtra(EXTRA_PENDING_FINISH_INTENT);
					

					try {
						Runtime.getRuntime().exec("chmod 644 " + outPath);
					} catch (IOException e) {
						Log.e(THIS_FILE, "Unable to make the apk file readable", e);
					}
					
					Log.d(THIS_FILE, "Download finished of : " + outPath);
					if(pendingIntent != null) {

						
						notification.contentIntent = pendingIntent;
						notification.flags = Notification.FLAG_AUTO_CANCEL;
						notification.icon = android.R.drawable.stat_sys_download_done;
						notification.contentView.setViewVisibility(R.id.status_progress_wrapper, View.GONE);
				        notification.contentView.setTextViewText(R.id.status_text, 
				        		getResources().getString(R.string.done)
				        		// TODO should be a parameter of this class
				        		+" - Click to install");
				        notificationManager.notify(NOTIF_DOWNLOAD, notification);
				        
						/*
						try {
							pendingIntent.send();
					        notificationManager.cancel(NOTIF_DOWNLOAD);
						} catch (CanceledException e) {
							Log.e(THIS_FILE, "Impossible to start pending intent for download finish");
						}
						*/
					}else {
						Log.w(THIS_FILE, "Invalid pending intent for finish !!!");
					}
					
					
					result = Activity.RESULT_OK;
				}
			} catch (IOException e) {
				Log.e(THIS_FILE, "Exception in download", e);
			}
		}
		
		if(result == Activity.RESULT_CANCELED) {
			notificationManager.cancel(NOTIF_DOWNLOAD);
		}
	}

	
	public interface Progress {
        void run(long progress, long total);
    }
	

	
    private class FileStreamResponseHandler implements ResponseHandler<Boolean> {
        private Progress mProgress;
		private File mFile;
        
		FileStreamResponseHandler(File outputFile, Progress progress) {
            mFile = outputFile;
            mProgress = progress;
        }
        
        public Boolean handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            FileOutputStream fos = new FileOutputStream(mFile.getPath());
            
            HttpEntity entity = response.getEntity();
            boolean done = false;
            
            try {
	            if (entity != null) {
	                Long length = entity.getContentLength();
	                InputStream input = entity.getContent();
	                byte[] buffer = new byte[4096];
	                int size  = 0;
	                int total = 0;
	                while (true) {
	                    size = input.read(buffer);
	                    if (size == -1) break;
	                    fos.write(buffer, 0, size);
	                    total += size;
	                    mProgress.run(total, length);
	                }
	                done = true;
	            }
            }catch(IOException e) {
            	Log.e(THIS_FILE, "Problem on downloading");
            }finally {
            	fos.close();
            }
            
            return done;
        }

    }
}
