/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
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
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.csipsimple.R;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipMessage;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.models.CallerInfo;
import com.csipsimple.service.SipNotifications;
import com.csipsimple.service.SipService;
import com.csipsimple.ui.PickupSipUri;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.SmileyParser;
import com.csipsimple.utils.clipboard.ClipboardWrapper;
import com.csipsimple.utils.contacts.ContactsWrapper;
import com.csipsimple.widgets.AccountChooserButton;

public class MessageFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor>, OnClickListener {
    private static final String THIS_FILE = "ComposeMessage";
    private String remoteFrom;
    private TextView fromText;
    private TextView fullFromText;
    private EditText bodyInput;
    private AccountChooserButton accountChooserButton;
    private Button sendButton;
    private SipNotifications notifications;
    private MessageAdapter mAdapter;
    

    public interface OnQuitListener {
        public void onQuit();
    }

    private OnQuitListener quitListener;
    private ClipboardWrapper clipboardManager;

    public void setOnQuitListener(OnQuitListener l) {
        quitListener = l;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        

        ListView lv = getListView();
        lv.setOnCreateContextMenuListener(this);
        
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SmileyParser.init(getActivity());
        notifications = new SipNotifications(getActivity());
        clipboardManager = ClipboardWrapper.getInstance(getActivity());
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.compose_message_activity, container, false);

        fullFromText = (TextView) v.findViewById(R.id.subject);
        fromText = (TextView) v.findViewById(R.id.subjectLabel);
        bodyInput = (EditText) v.findViewById(R.id.embedded_text_editor);
        accountChooserButton = (AccountChooserButton) v.findViewById(R.id.accountChooserButton);
        sendButton = (Button) v.findViewById(R.id.send_button);
        accountChooserButton.setShowExternals(false);
        
        return v;
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setDivider(null);
        fromText.setOnClickListener(this);
        sendButton.setOnClickListener(this);
        
        mAdapter = new MessageAdapter(getActivity(), null);
        getListView().setAdapter(mAdapter);
        
        // Setup from args
        String from = getArguments().getString(SipMessage.FIELD_FROM);
        String fullFrom = getArguments().getString(SipMessage.FIELD_FROM_FULL);
        if (fullFrom == null) {
            fullFrom = from;
        }
        setupFrom(from, fullFrom);
        if (remoteFrom == null) {
            chooseSipUri();
        }
        
        
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SipService.class), connection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(connection);
        } catch (Exception e) {
            // Just ignore that
        }
        service = null;
        super.onDetach();
    }
    
    @Override
    public void onResume() {
        Log.d(THIS_FILE, "Resume compose message act");
        super.onResume();
        notifications.setViewingMessageFrom(remoteFrom);
    }

    @Override
    public void onPause() {
        super.onPause();
        notifications.setViewingMessageFrom(null);
    }

    private final static int PICKUP_SIP_URI = 0;
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(THIS_FILE, "On activity result");
        if (requestCode == PICKUP_SIP_URI) {
            if (resultCode == Activity.RESULT_OK) {
                String from = data.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                setupFrom(from, from);
            }
            if (TextUtils.isEmpty(remoteFrom)) {
                if(quitListener != null) {
                    quitListener.onQuit();
                }
            }else {
                loadMessageContent();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Service connection
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
    
    private void loadMessageContent() {
        getLoaderManager().restartLoader(0, getArguments(), this);
        
        String from = getArguments().getString(SipMessage.FIELD_FROM);

        if (!TextUtils.isEmpty(from)) {
            ContentValues args = new ContentValues();
            args.put(SipMessage.FIELD_READ, true);
            getActivity().getContentResolver().update(SipMessage.MESSAGE_URI, args,
                    SipMessage.FIELD_FROM + "=?", new String[] {
                        from
                    });
        }
    }
    

    public static Bundle getArguments(String from, String fromFull) {
        Bundle bundle = new Bundle();
        if (from != null) {
            bundle.putString(SipMessage.FIELD_FROM, from);
            bundle.putString(SipMessage.FIELD_FROM_FULL, fromFull);
        }

        return bundle;
    }
    

    private void setupFrom(String from, String fullFrom) {
        if (from != null) {
            if (remoteFrom != from) {
                remoteFrom = from;
                fromText.setText(remoteFrom);
                CallerInfo callerInfo = CallerInfo.getCallerInfoFromSipUri(getActivity(), fullFrom);
                if (callerInfo != null && callerInfo.contactExists) {
                	fullFromText.setText(callerInfo.name);
                } else {
                	fullFromText.setText(SipUri.getDisplayedSimpleContact(fullFrom));
                }
                loadMessageContent();
                notifications.setViewingMessageFrom(remoteFrom);
            }
        }
    }

    private void chooseSipUri() {
        Intent pickupIntent = new Intent(getActivity(), PickupSipUri.class);
        startActivityForResult(pickupIntent, PICKUP_SIP_URI);
    }

    private void sendMessage() {
        if (service != null) {
            SipProfile acc = accountChooserButton.getSelectedAccount();
            if (acc != null && acc.id != SipProfile.INVALID_ID) {
                try {
                    String textToSend = bodyInput.getText().toString();
                    if(!TextUtils.isEmpty(textToSend)) {
                        service.sendMessage(textToSend, remoteFrom, (int) acc.id);
                        bodyInput.getText().clear();
                    }
                } catch (RemoteException e) {
                    Log.e(THIS_FILE, "Not able to send message");
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        int clickedId = v.getId();
        if (clickedId == R.id.subject) {
            chooseSipUri();
        } else if (clickedId == R.id.send_button) {
            sendMessage();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Builder toLoadUriBuilder = SipMessage.THREAD_ID_URI_BASE.buildUpon().appendEncodedPath(remoteFrom.replaceAll("/", "%2F"));
        return new CursorLoader(getActivity(), toLoadUriBuilder.build(), null, null, null,
                SipMessage.FIELD_DATE + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
    

    // Options
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        int actionRoom = getResources().getBoolean(R.bool.menu_in_bar) ? MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_NEVER;
        MenuItem addContactMenu = menu.add(R.string.menu_add_to_contacts);
        addContactMenu.setIcon(R.drawable.ic_add_contact_holo_dark).setShowAsAction(actionRoom);
        addContactMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent it = ContactsWrapper.getInstance().getAddContactIntent(null, remoteFrom);
                startActivity(it);
                return true;
            }
        });
    }
    
    // Context menu
    public static final int MENU_COPY = ContextMenu.FIRST;

    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        menu.add(0, MENU_COPY, 0, R.string.copy_message_text);
    }
    
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Cursor c = (Cursor) mAdapter.getItem(info.position);
        if (c != null) {
            SipMessage msg = new SipMessage(c);
            switch (item.getItemId()) {
                case MENU_COPY: {
                    clipboardManager.setText(msg.getDisplayName(), msg.getBody());
                    break;
                }
                default:
                    break;
            }

        }
        return super.onContextItemSelected(item);
    }
    
}
