package com.csipsimple.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.csipsimple.R;

public class SMSReader extends Activity implements OnClickListener
{
	private static final String THIS_FILE = "SMSReader";
	
	//private View smsView;
	private TextView message;
	private TextView number;
	private Button replyButton, okButton;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.sms_reader);
//		smsView = (View) findViewById(R.id.sms_reader);
		
		message = (TextView) findViewById(R.id.sms_read_message);
		number = (TextView) findViewById(R.id.sms_read_number);
		
		number.setText(getIntent().getStringExtra("com.ui.SMSReader.number"));
		message.setText(getIntent().getStringExtra("com.ui.SMSReader.message"));
		
		replyButton = (Button) findViewById(R.id.reply_sms);
		replyButton.setOnClickListener(this);
		
		okButton = (Button) findViewById(R.id.ok_sms);
		okButton.setOnClickListener(this);
	}
	
	public void onClick(View view) {
		int view_id = view.getId();
		Intent result = new Intent();
		
		if (view_id == R.id.reply_sms) {
			// show the SMSComposer window
			Intent composer = new Intent(this, SMSComposer.class);
			composer.putExtra("com.ui.SMSComposer.number", number.getText().toString());
			// TODO: get the account which it was sent to
			composer.putExtra("com.ui.SMSComposer.accid", 0);
			startActivity(composer);
		} else if (view_id == R.id.ok_sms) {
			setResult(Activity.RESULT_CANCELED, result);
		}
		finish();
	}
}
