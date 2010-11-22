/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.csipsimple.ui.messages;

import android.app.Activity;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.SipMessage;
import com.csipsimple.service.SipService;
import com.csipsimple.ui.SipHome;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.SipUri;

/**
 * This activity provides a list view of existing conversations.
 */
public class ConversationList extends ListActivity {
	private static final String THIS_FILE = "Conv List";
	
    // IDs of the main menu items.
    public static final int MENU_COMPOSE_NEW          = 0;
    public static final int MENU_DELETE_ALL           = 1;

    // IDs of the context menu items for the list of conversations.
    public static final int MENU_DELETE               = 0;
    public static final int MENU_VIEW                 = 1;


	private DBAdapter database;

	private Activity contextToBindTo = this;

	private BroadcastReceiver registrationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
     // Bind to the service
		if (getParent() != null) {
			contextToBindTo = getParent();
		}
		
        setContentView(R.layout.conversation_list_screen);

        ListView listView = getListView();
        LayoutInflater inflater = LayoutInflater.from(this);
        RelativeLayout headerView = (RelativeLayout)
                inflater.inflate(R.layout.conversation_list_item, listView, false);
        ((TextView) headerView.findViewById(R.id.from) ).setText(R.string.new_message);
        ((TextView) headerView.findViewById(R.id.subject) ).setText(R.string.create_new_message);
        listView.addHeaderView(headerView, null, true);

        
        initListAdapter();
        
        registrationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(SipService.ACTION_SIP_MESSAGE_RECEIVED.equalsIgnoreCase(intent.getAction()) ) {
					updateAdapter();
				}
			}
		};
		registerReceiver(registrationReceiver, new IntentFilter(SipService.ACTION_SIP_MESSAGE_RECEIVED));
        
    }

    private void initListAdapter() {
		// Db
		if (database == null) {
			database = new DBAdapter(this);
		}
		database.open();
		

		ConversationsCursorAdapter cad = new ConversationsCursorAdapter(this, null);

		setListAdapter(cad);
		updateAdapter();
    }
    
    private void updateAdapter() {

		Cursor cursor = database.getAllConversations();
		startManagingCursor(cursor);
		
		((CursorAdapter) getListAdapter()).changeCursor(cursor);
    }


	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
	}

	@Override
	protected void onStart() {
		super.onStart();
		
	}
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK : {
			onBackPressed();
			//return true;
			break;
		}
		}

		return super.onKeyUp(keyCode, event);
	}
	
	public void onBackPressed() {
		if(contextToBindTo != null && contextToBindTo instanceof SipHome) {
			((SipHome) contextToBindTo).onBackPressed();
		}else {
			finish();
		}
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();

        menu.add(0, MENU_COMPOSE_NEW, 0, R.string.menu_compose_new).setIcon(
                R.drawable.ic_menu_compose);

        if (getListAdapter().getCount() > 0) {
            menu.add(0, MENU_DELETE_ALL, 0, R.string.menu_delete_all).setIcon(
                    android.R.drawable.ic_menu_delete);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case MENU_COMPOSE_NEW:
                createNewMessage();
                break;
            default:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        if (position == 0) {
            createNewMessage();
        } else {
            ConversationListItemViews tag = (ConversationListItemViews) v.getTag();
            openThread(tag.from);
        }
    }

    private void createNewMessage() {
        startActivity(ComposeMessageActivity.createIntent(this, null));
    }

    private void openThread(String from) {
        startActivity(ComposeMessageActivity.createIntent(this, from));
    }


    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        Cursor cursor = ((CursorAdapter) getListAdapter()).getCursor();
        if (cursor == null || cursor.getPosition() < 0) {
            return;
        }
        
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (info.position > 0) {
            menu.add(0, MENU_VIEW, 0, R.string.menu_view);
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
        }
    }
    
    

	public static final class ConversationListItemViews {
		TextView fromView;
		TextView dateView;
		TextView subjectView;
		String from;
	}
    

    private static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);
    
    class ConversationsCursorAdapter extends ResourceCursorAdapter {

        
		

		public ConversationsCursorAdapter(Context context, Cursor c) {
			super(context, R.layout.conversation_list_item, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final ConversationListItemViews tagView = (ConversationListItemViews) view.getTag();
			String number = cursor.getString(cursor.getColumnIndex(SipMessage.FIELD_FROM));
			int read = cursor.getInt(cursor.getColumnIndex(SipMessage.FIELD_READ));
			long date = cursor.getLong(cursor.getColumnIndex(SipMessage.FIELD_DATE));
			String subject = cursor.getString(cursor.getColumnIndex(SipMessage.FIELD_BODY));
			
			Log.d(THIS_FILE, "Here read is "+read);
			Drawable background = (read == 0)?
	                context.getResources().getDrawable(R.drawable.conversation_item_background_unread) :
	                context.getResources().getDrawable(R.drawable.conversation_item_background_read);

	        view.setBackgroundDrawable(background);

	        // Subject
	        tagView.subjectView.setText(subject);
//	        LayoutParams subjectLayout = (LayoutParams)tagView.subjectView.getLayoutParams();
	        // We have to make the subject left of whatever optional items are shown on the right.
//	        subjectLayout.addRule(RelativeLayout.LEFT_OF, R.id.date);
			
			//From
			tagView.from = number;
			tagView.fromView.setText(formatMessage(cursor));

			//Date
			// Set the date/time field by mixing relative and absolute times.
			int flags = DateUtils.FORMAT_ABBREV_RELATIVE;
			tagView.dateView.setText(DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, flags));

		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			View view = super.newView(context, cursor, parent);
			
			ConversationListItemViews tagView = new ConversationListItemViews();
			tagView.fromView = (TextView) view.findViewById(R.id.from);
			tagView.dateView = (TextView) view.findViewById(R.id.date);
			tagView.subjectView = (TextView) view.findViewById(R.id.subject);

			view.setTag(tagView);

			return view;
		}
		
		private CharSequence formatMessage(Cursor cursor) {
			String remoteContact = cursor.getString(cursor.getColumnIndex(SipMessage.FIELD_FROM));
	        SpannableStringBuilder buf = new SpannableStringBuilder(SipUri.getDisplayedSimpleUri(remoteContact));

	        
	        int counter = cursor.getInt(cursor.getColumnIndex("counter"));
	        if (counter > 1) {
	            buf.append(" (" + counter + ") ");
	        }
	       

			int read = cursor.getInt(cursor.getColumnIndex(SipMessage.FIELD_READ));
	        // Unread messages are shown in bold
	        if (read == 0) {
	            buf.setSpan(STYLE_BOLD, 0, buf.length(),
	                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
	        }
	        return buf;
		}
		
		


	}
    
}
