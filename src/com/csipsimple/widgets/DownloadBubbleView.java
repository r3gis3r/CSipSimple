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
package com.csipsimple.widgets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import com.csipsimple.utils.MD5;

import android.content.Context;
import android.os.Handler;
import com.csipsimple.utils.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.csipsimple.R;

public class DownloadBubbleView extends LinearLayout implements OnClickListener {

	private static final String THIS_FILE = "Download Bubble";
	protected static final int BUFFER = 2048;
	private URI target_url = null;
	private String what = "Unknown download";
	private String desc = "Something that could be downloaded";
	private File dest;
	private boolean check_md5 = true;

	private String tmp_name = "download_bubble";

	private Context mContext;

	public DownloadBubbleView(Context context) {
		super(context);
		mContext = context;
		initLayout();
	}

	public DownloadBubbleView(Context context, String text, String description,
			URI url, File destination) {
		super(context);
		mContext = context;
		target_url = url;
		what = text;
		desc = description;
		dest = destination;
		initLayout();
	}

	private void initLayout() {
		LayoutInflater li = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.download_bubble, this, true);
		Button bt = (Button) findViewById(R.id.main_btn);
		bt.setOnClickListener(this);

		ProgressBar pb = (ProgressBar) findViewById(R.id.progress_bar);
		pb.setMax(100);
		pb.setProgress(0);

		setBtnText(what);
		setDescText(desc);
	}

	public void setBtnText(String text) {
		what = text;
		Button bt = (Button) findViewById(R.id.main_btn);
		bt.setText(text);
	}

	public void setDescText(String description) {
		desc = description;
		TextView tv = (TextView) findViewById(R.id.under_btn_label);
		tv.setText(description);
	}

	public void setCheckMd5(boolean check) {
		check_md5 = check;
	}

	// Need handler for callbacks to the UI thread
	final Handler mHandler = new Handler();

	// Create runnable for posting
	final Runnable mFinishUpload = new Runnable() {
		public void run() {
			finishUpload();
		}
	};
	private int mcontentLength;
	private long localFileSize;
	private long mtotalDownloaded;

	@Override
	public void onClick(View v) {
		Thread t = new Thread() {
			@Override
			public void run() {
				super.run();

				HttpClient httpClient = new DefaultHttpClient();
				HttpClient MD5httpClient = new DefaultHttpClient();

				HttpUriRequest req, md5req;
				HttpResponse response, md5response;
				String downloadedMD5 = "";

				try {

					if (check_md5) {
						md5req = new HttpGet(target_url + ".md5sum");
						md5req.addHeader("Cache-Control", "no-cache");
						md5response = MD5httpClient.execute(md5req);

						HttpEntity temp = md5response.getEntity();
						InputStreamReader isr = new InputStreamReader(temp
								.getContent());
						BufferedReader br = new BufferedReader(isr);
						downloadedMD5 = br.readLine().split("  ")[0];
						br.close();
						isr.close();
						if (temp != null) {
							temp.consumeContent();
						}
					}

					localFileSize = 0;
					req = new HttpGet(target_url);
					req.addHeader("Cache-Control", "no-cache");
					response = httpClient.execute(req);
					// Download library .gz
					HttpEntity entity = response.getEntity();
					File tmp_gz = File.createTempFile(tmp_name, ".gz");
					File tmp_part = File.createTempFile(tmp_name,".part");
					dumpFile(entity, tmp_part, tmp_gz);
					if (check_md5) {
						if (!MD5.checkMD5(downloadedMD5, tmp_gz)) {
							Log.e(THIS_FILE, "MD5 doesn't match");
							tmp_gz.delete();
							mHandler.post(mFinishUpload);
							return; // TODO : throw something?
						}
					}
					
					Log.d(THIS_FILE, "Gzip is in : "+tmp_gz.getAbsolutePath());

					// Ungzip
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
					mHandler.post(mFinishUpload);
				} catch (ClientProtocolException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
		};
		Button bt = (Button) findViewById(R.id.main_btn);
		View pl = findViewById(R.id.progress_layout);

		bt.setVisibility(GONE);
		pl.setVisibility(VISIBLE);
		TextView tv = (TextView) findViewById(R.id.under_btn_label);
		tv.setText(what);
		t.start();
	}

	private void dumpFile(HttpEntity entity, File partialDestinationFile,
			File destinationFile) throws IOException {

		mcontentLength = (int) entity.getContentLength();
		if (mcontentLength <= 0) {
			Log
					.w(THIS_FILE,
							"Don't understand why but content length can't be detected");
			mcontentLength = 1024;
		}


		byte[] buff = new byte[64 * 1024];
		int read = 0;
		RandomAccessFile out = new RandomAccessFile(partialDestinationFile,
				"rw");
		out.seek(0);
		InputStream is = entity.getContent();
		TimerTask progressUpdateTimerTask = new TimerTask() {
			@Override
			public void run() {
				onProgressUpdate();
			}

		};
		Timer progressUpdateTimer = new Timer();
		try {
			mtotalDownloaded = 0;
			progressUpdateTimer.scheduleAtFixedRate(progressUpdateTimerTask,
					100, 100);
			while ((read = is.read(buff)) > 0) {
				out.write(buff, 0, read);
				mtotalDownloaded += read;
			}
			out.close();
			is.close();
			partialDestinationFile.renameTo(destinationFile);
			Log.d(THIS_FILE, "Download finished");

		} catch (IOException e) {
			out.close();
			try {
				destinationFile.delete();
			} catch (SecurityException ex) {
				Log.e(THIS_FILE,
						"Unable to delete downloaded File. Continue anyway.",
						ex);
			}
		} finally {
			progressUpdateTimer.cancel();
			buff = null;
		}
	}

	private void onProgressUpdate() {
		ProgressBar pb = (ProgressBar) findViewById(R.id.progress_bar);
		pb.setProgress((int) (100.0 * localFileSize / mcontentLength));
	}

	private void finishUpload() {
		View pl = findViewById(R.id.progress_layout);
		pl.setVisibility(GONE);
	}
}
