package com.csipsimple.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.csipsimple.R;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.OutgoingCall;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Log;

public class SMSComposer extends Activity implements OnClickListener
{
	private static final String THIS_FILE = "SMSComposer";
	
//	private View smsView;
	private EditText message;
	private Button sendButton, cancelButton;
	private Integer accid;
	private String number;
	
	private static final int USE_GSM = -2;
	
	private Activity contextToBindTo = this;
	private ISipService service;
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			service = null;
		}

	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		accid = getIntent().getIntExtra("com.ui.SMSComposer.accid", -1);
		number = getIntent().getStringExtra("com.ui.SMSComposer.number");
		
		if (accid == -1 || number.equals("")) {
			finish();
		}
		
		// Bind to the service
		if (getParent() != null) {
			contextToBindTo = getParent();
		}

		setContentView(R.layout.sms_activity);
	//	smsView = (View) findViewById(R.id.sms_composer);
		
		message = (EditText) findViewById(R.id.message);
		
		sendButton = (Button) findViewById(R.id.send_sms);
		sendButton.setOnClickListener(this);
		
		cancelButton = (Button) findViewById(R.id.cancel_sms);
		cancelButton.setOnClickListener(this);
	}
	
	public void onClick(View view) {
		int view_id = view.getId();
		Intent result = new Intent();
		
		if (view_id == R.id.send_sms) {
			//Bundle b = new Bundle();
			//b.putString("message", message.getText().toString());
			//result.putExtras(b);
			//setResult(Activity.RESULT_OK, result);
			Log.e(THIS_FILE, "sms: " + message.getText().toString());
			//sendSMS(data.getStringExtra("message").toString());
			sendSMS(message.getText().toString());
		} else if (view_id == R.id.cancel_sms) {
			setResult(Activity.RESULT_CANCELED, result);
			Log.e(THIS_FILE, "sms cancelled");
		}
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Bind service
		contextToBindTo.bindService(new Intent(contextToBindTo, SipService.class), connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Unbind service
		// TODO : should be done by a cleaner way (check if bind function has
		// been launched is better than check if bind has been done)
		if (service != null) {
			contextToBindTo.unbindService(connection);
		}
	}

	private void sendSMS(String message) {
		if (service == null) {
			return;
		}

		if (TextUtils.isEmpty(number)) {
			return;
		}

		// dialDomain.getText().clear();
		if (accid != USE_GSM) {
			try {
				service.sendSMS(message, number, accid);
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Service can't be called to make the call");
			}
		} else {
			OutgoingCall.ignoreNext = number;
			Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+number));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}

	}
}
