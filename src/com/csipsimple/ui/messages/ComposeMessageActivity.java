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
package com.csipsimple.ui.messages;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.SipMessage;
import com.csipsimple.api.ISipService;
import com.csipsimple.service.SipNotifications;
import com.csipsimple.service.SipService;
import com.csipsimple.ui.PickupSipUri;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.SmileyParser;
import com.csipsimple.widgets.AccountChooserButton;

public class ComposeMessageActivity extends Activity implements OnClickListener {
	private static final String THIS_FILE = "ComposeMessage";
	private DBAdapter database;
	private String remoteFrom;
	private ListView messageList;
	private TextView fromText;
	private TextView fullFromText;
	private EditText bodyInput;
	private AccountChooserButton accountChooserButton;
	private Button sendButton;
	private BroadcastReceiver registrationReceiver;
	private SipNotifications notifications;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.compose_message_activity);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        
        SmileyParser.init(this);
        Log.d(THIS_FILE, "Creating compose message");
        
        messageList = (ListView) findViewById(R.id.history);
        fullFromText = (TextView) findViewById(R.id.subject);
        fromText = (TextView) findViewById(R.id.subjectLabel);
        bodyInput = (EditText) findViewById(R.id.embedded_text_editor);
        accountChooserButton = (AccountChooserButton) findViewById(R.id.accountChooserButton);
        sendButton = (Button) findViewById(R.id.send_button);
        
        notifications = new SipNotifications(this);
        
        messageList.setDivider(null);
        
		// Db
		if (database == null) {
			database = new DBAdapter(this);
		}
		database.open();

		MessagesCursorAdapter cad = new MessagesCursorAdapter(this, null);
		messageList.setAdapter(cad);
		
		fromText.setOnClickListener(this);
		sendButton.setOnClickListener(this);
		
		registrationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(SipManager.ACTION_SIP_REGISTRATION_CHANGED.equalsIgnoreCase(intent.getAction())) {
					updateRegistrations();
				}else if(SipManager.ACTION_SIP_MESSAGE_RECEIVED.equalsIgnoreCase(intent.getAction()) ||
						SipManager.ACTION_SIP_MESSAGE_STATUS.equalsIgnoreCase(intent.getAction())) {
					//Check if intent correspond to current message
					String from = intent.getStringExtra(SipMessage.FIELD_FROM);
					if(from != null && from.equalsIgnoreCase(remoteFrom)) {
						loadMessageContent();
					}
				}
			}
		};
		registerReceiver(registrationReceiver, new IntentFilter(SipManager.ACTION_SIP_REGISTRATION_CHANGED));
		registerReceiver(registrationReceiver, new IntentFilter(SipManager.ACTION_SIP_MESSAGE_RECEIVED));
		registerReceiver(registrationReceiver, new IntentFilter(SipManager.ACTION_SIP_MESSAGE_STATUS));

		

        bindService(new Intent(this, SipService.class), connection, Context.BIND_AUTO_CREATE);
        
        Intent intent = getIntent();
        String from = intent.getStringExtra(SipMessage.FIELD_FROM);
		String fullForm = intent.getStringExtra(SipMessage.FIELD_FROM_FULL);
		if(fullForm == null) {
			fullForm = from;
		}
		setFromField(from, fullForm);
		
        if(remoteFrom == null) {
			chooseSipUri();
		}
        loadMessageContent();
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		

    	try {
			unbindService(connection);
		}catch(Exception e) {
			//Just ignore that
		}
		service = null;
		
		
		database.close();
		try {
			unregisterReceiver(registrationReceiver);
		} catch (Exception e) {
			// Nothing to do here
		}
	}
    
    @Override
    protected void onResume() {
    	Log.d(THIS_FILE, "Resume compose message act");
    	super.onResume();
    	notifications.setViewingMessageFrom(remoteFrom);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	notifications.setViewingMessageFrom(null);
    }
    
	private final static int PICKUP_SIP_URI = 0;
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		String from = intent.getStringExtra(SipMessage.FIELD_FROM);
		String fullForm = intent.getStringExtra(SipMessage.FIELD_FROM_FULL);
		if(fullForm == null) {
			fullForm = from;
		}
		setFromField(from, fullForm);
		if(remoteFrom == null) {
			chooseSipUri();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(THIS_FILE, "On activity result");
		switch (requestCode) {
		case PICKUP_SIP_URI:
			if(resultCode == RESULT_OK) {
				String from = data.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
				setFromField(from, from);
			}
			if(remoteFrom == null) {
				finish();
			}
			return;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	// Service connection
	private ISipService service;
	private ServiceConnection connection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
			accountChooserButton.updateService(service);
			updateRegistrations();
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			service = null;
		}
    };
	
	private void loadMessageContent() {
		CursorAdapter ad = (CursorAdapter) messageList.getAdapter();
		
		if(remoteFrom != null) {
			Cursor cursor = database.getConversation(remoteFrom);
			startManagingCursor(cursor);
			ad.changeCursor(cursor);
			
			//And now update the read state of thread
			database.markConversationAsRead(remoteFrom);
		}else {
			ad.changeCursor(null);
		}
	}
	
	protected void updateRegistrations() {
		boolean canChangeIfValid = TextUtils.isEmpty(bodyInput.getText().toString());
		accountChooserButton.updateRegistration(canChangeIfValid);
	}
	
    public static Intent createIntent(Context context, String from, String fromFull) {
        Intent intent = new Intent(context, ComposeMessageActivity.class);

        if (from != null) {
            intent.putExtra(SipMessage.FIELD_FROM, from);
            intent.putExtra(SipMessage.FIELD_FROM_FULL, fromFull);
        }

        return intent;
   }
    
    private void setFromField(String from, String fullFrom) {
    	if(from != null) {
    		if(remoteFrom != from) {
    			remoteFrom = from;
    			fromText.setText(remoteFrom);
    			fullFromText.setText(SipUri.getDisplayedSimpleContact(fullFrom));
    			loadMessageContent();
    			notifications.setViewingMessageFrom(remoteFrom);
    		}
    	}
    }
    

	public static final class MessageListItemViews {
		TextView contentView;
		TextView errorView;
		ImageView deliveredIndicator;
	}
    
	class MessagesCursorAdapter extends ResourceCursorAdapter {

        
		public MessagesCursorAdapter(Context context, Cursor c) {
			super(context, R.layout.message_list_item, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final MessageListItemViews tagView = (MessageListItemViews) view.getTag();
			String number = cursor.getString(cursor.getColumnIndex(SipMessage.FIELD_FROM));
			if(! number.equalsIgnoreCase(SipMessage.SELF)) {
				number = cursor.getString(cursor.getColumnIndex(SipMessage.FIELD_FROM_FULL));
			}
			long date = cursor.getLong(cursor.getColumnIndex(SipMessage.FIELD_DATE));
			String subject = cursor.getString(cursor.getColumnIndex(SipMessage.FIELD_BODY));
			String mimeType = cursor.getString(cursor.getColumnIndex(SipMessage.FIELD_MIME_TYPE));
			int type = cursor.getInt(cursor.getColumnIndex(SipMessage.FIELD_TYPE));
			int status = cursor.getInt(cursor.getColumnIndex(SipMessage.FIELD_STATUS));
			
			int flags = DateUtils.FORMAT_ABBREV_RELATIVE;
			String timestamp = (String) DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, flags);
			
			
	        
			
	        //Delivery state
	        if(type == SipMessage.MESSAGE_TYPE_QUEUED) {
	        	tagView.deliveredIndicator.setVisibility(View.VISIBLE);
	        	tagView.deliveredIndicator.setImageResource(R.drawable.ic_email_pending);
	        }else if(type == SipMessage.MESSAGE_TYPE_FAILED) {
	        	tagView.deliveredIndicator.setVisibility(View.VISIBLE);
	        	tagView.deliveredIndicator.setImageResource(R.drawable.ic_sms_mms_not_delivered);
	        }else {
	        	tagView.deliveredIndicator.setVisibility(View.GONE);
	        }
	        
	        if(status == SipMessage.STATUS_NONE 
	        		|| status == SipCallSession.StatusCode.OK
	        		|| status == SipCallSession.StatusCode.ACCEPTED) {
	        	tagView.errorView.setVisibility(View.GONE);
	        }else {
	        	
	        	int splitIndex = subject.indexOf(" // ");
	        	String errorTxt = null;
	        	if(splitIndex != -1) {
	        		errorTxt = subject.substring(splitIndex+4, subject.length());
	        		subject = subject.substring(0, splitIndex);
	        	}
	        	if(errorTxt != null) {
		        	tagView.errorView.setVisibility(View.VISIBLE);
		        	tagView.errorView.setText(errorTxt);
	        	}
	        }
	        
	     // Subject
	        tagView.contentView.setText(formatMessage(number, subject, timestamp, mimeType));
	       
	        view.setBackgroundResource(type == SipMessage.MESSAGE_TYPE_INBOX ? R.drawable.listitem_background_lightblue: R.drawable.listitem_background);

		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			View view = super.newView(context, cursor, parent);
			
			MessageListItemViews tagView = new MessageListItemViews();
			tagView.contentView = (TextView) view.findViewById(R.id.text_view);
			tagView.errorView = (TextView) view.findViewById(R.id.error_view);
			tagView.deliveredIndicator = (ImageView) view.findViewById(R.id.delivered_indicator);
			
			view.setTag(tagView);

			return view;
		}
		
		TextAppearanceSpan mTextSmallSpan =
	        new TextAppearanceSpan(ComposeMessageActivity.this, android.R.style.TextAppearance_Small);
		
		private CharSequence formatMessage(String contact, String body, String timestamp, String contentType) {
			CharSequence template = ComposeMessageActivity.this.getResources().getText(R.string.name_colon);
			String formatedContact;
			if(contact.equalsIgnoreCase(SipMessage.SELF)) {
				formatedContact = getString(R.string.messagelist_sender_self);
			}else {
				formatedContact = SipUri.getDisplayedSimpleContact(contact);
			}
			SpannableStringBuilder buf = new SpannableStringBuilder(TextUtils.replace(template, 
					new String[] { "%s" }, 
					new CharSequence[] { formatedContact }));

			if (!TextUtils.isEmpty(body)) {
				// Converts html to spannable if ContentType is "text/html".
				if (contentType != null && "text/html".equals(contentType)) {
					buf.append("\n");
					buf.append(Html.fromHtml(body));
				} else {
					SmileyParser parser = SmileyParser.getInstance();
					buf.append(parser.addSmileySpans(body));
				}
			}
			
			// We always show two lines because the optional icon bottoms are
			// aligned with the
			// bottom of the text field, assuming there are two lines for the
			// message and the sent time.
			buf.append("\n");
			int startOffset = buf.length();

			startOffset = buf.length();
			buf.append(TextUtils.isEmpty(timestamp) ? " " : timestamp);

			buf.setSpan(mTextSmallSpan, startOffset, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			
			return buf;
		}

	}
	
	private void chooseSipUri() {
		Intent pickupIntent = new Intent(this, PickupSipUri.class);
		startActivityForResult(pickupIntent, PICKUP_SIP_URI);
	}

	private void sendMessage() {
		if(service != null) {
			SipProfile acc = accountChooserButton.getSelectedAccount();
			if(acc != null && acc.id != SipProfile.INVALID_ID) {
				try {
					service.sendMessage(bodyInput.getText().toString(), remoteFrom, acc.id);
					bodyInput.getText().clear();
					loadMessageContent();
				} catch (RemoteException e) {
					Log.e(THIS_FILE, "Not able to send message");
				}
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.subject:
			chooseSipUri();
			break;
		case R.id.send_button:
			sendMessage();
			break;
		default:
			break;
		}
	}
}
