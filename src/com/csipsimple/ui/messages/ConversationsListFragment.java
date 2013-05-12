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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.csipsimple.R;
import com.csipsimple.api.SipMessage;
import com.csipsimple.service.SipNotifications;
import com.csipsimple.ui.SipHome.ViewPagerVisibilityListener;
import com.csipsimple.ui.messages.ConversationsAdapter.ConversationListItemViews;
import com.csipsimple.widgets.CSSListFragment;

/**
 * This activity provides a list view of existing conversations.
 */
public class ConversationsListFragment extends CSSListFragment implements ViewPagerVisibilityListener {
	//private static final String THIS_FILE = "Conv List";
	
    // IDs of the main menu items.
    public static final int MENU_COMPOSE_NEW          = 0;
    public static final int MENU_DELETE_ALL           = 1;

    // IDs of the context menu items for the list of conversations.
    public static final int MENU_DELETE               = 0;
    public static final int MENU_VIEW                 = 1;
	
    private boolean mDualPane;

    private ConversationsAdapter mAdapter;
    private View mHeaderView;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        ListView lv = getListView();

        if(getListAdapter() == null && mHeaderView != null) {
            lv.addHeaderView(mHeaderView, null, true);
        }
        
        lv.setOnCreateContextMenuListener(this);
    }
    
    private void attachAdapter() {
        // Header view add
        if(mAdapter == null) {
            // Adapter
            mAdapter = new ConversationsAdapter(getActivity(), null);
            setListAdapter(mAdapter);
        }
        
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View v = inflater.inflate(R.layout.message_list_fragment, container, false);
        
        ListView lv = (ListView) v.findViewById(android.R.id.list);

        View.OnClickListener addClickButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickAddMessage();
            }
        };
        

        // Header view
        mHeaderView = (ViewGroup)
                inflater.inflate(R.layout.conversation_list_item, lv, false);
        ((TextView) mHeaderView.findViewById(R.id.from) ).setText(R.string.new_message);
        ((TextView) mHeaderView.findViewById(R.id.subject) ).setText(R.string.create_new_message);
        mHeaderView.findViewById(R.id.quick_contact_photo).setVisibility(View.GONE);
        mHeaderView.setOnClickListener(addClickButtonListener);
        // Empty view
        Button bt = (Button) v.findViewById(android.R.id.empty);
        bt.setOnClickListener(addClickButtonListener);
        return v;
    }
    
    private void onClickAddMessage() {
        viewDetails(-getListView().getHeaderViewsCount(), null);
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // View management
        mDualPane = getResources().getBoolean(R.bool.use_dual_panes);

        // Modify list view
        ListView lv = getListView();
        lv.setVerticalFadingEdgeEnabled(true);
        // lv.setCacheColorHint(android.R.color.transparent);
        if (mDualPane) {
            lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            lv.setItemsCanFocus(false);
        } else {
            lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
            lv.setItemsCanFocus(true);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();

        SipNotifications nManager = new SipNotifications(getActivity());
        nManager.cancelMessages();
    }

    public void viewDetails(int position, ConversationListItemViews cri) {
        String number = null;
        String fromFull = null;
        if(cri != null) {
            number = cri.getRemoteNumber();
            fromFull = cri.fromFull;
        }
        viewDetails(position, number, fromFull);
    }
    
    
    public void viewDetails(int position, String number, String fromFull) {
        
        Bundle b = MessageFragment.getArguments(number, fromFull);

        if (mDualPane) {
            // If we are not currently showing a fragment for the new
            // position, we need to create and install a new one.
            MessageFragment df = new MessageFragment();
            df.setArguments(b);
            // Execute a transaction, replacing any existing fragment
            // with this one inside the frame.
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.details, df, null);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();

            getListView().setItemChecked(position, true);
        } else {
            Intent it = new Intent(getActivity(), MessageActivity.class);
            it.putExtras(b);
            startActivity(it);
        }
    }


    // Options
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        
        
        MenuItem writeMenu = menu.add(R.string.menu_compose_new);
        writeMenu.setIcon(R.drawable.ic_menu_msg_compose_holo_dark).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        writeMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onClickAddMessage();
                return true;
            }
        });

        if (getListAdapter() != null && getListAdapter().getCount() > 0) {

            MenuItem deleteAllMenu = menu.add(R.string.menu_delete_all);
            deleteAllMenu.setIcon(android.R.drawable.ic_menu_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            deleteAllMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    confirmDeleteThread(null);
                    return true;
                }
            });
        }
    }
    
    
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), SipMessage.THREAD_URI, null, null, null, null);
    }

    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (info.position > 0) {
            menu.add(0, MENU_VIEW, 0, R.string.menu_view);
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
        }
    }
    
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        
        if (info.position > 0) {
            ConversationListItemViews cri = (ConversationListItemViews) info.targetView.getTag();
            
            if(cri != null) {
                switch (item.getItemId()) {
                case MENU_DELETE: {
                    confirmDeleteThread(cri.getRemoteNumber());
                    break;
                }
                case MENU_VIEW: {
                    viewDetails(info.position, cri);
                    break;
                }
                default:
                    break;
                }
            }
        }
        return super.onContextItemSelected(item);
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        ConversationListItemViews cri = (ConversationListItemViews) v.getTag();
        viewDetails(position, cri);
    }


	/*
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();


        return super.onPrepareOptionsMenu(menu);
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case MENU_COMPOSE_NEW:
                createNewMessage();
                break;
            case MENU_DELETE_ALL:
            	confirmDeleteThread(null);
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
            if (tag.from.equals("SELF")) {
                openThread(tag.to, tag.fromFull);
            } else {
                openThread(tag.from, tag.fromFull);
            }
        }
    }
    */
    
    private void confirmDeleteThread(final String from) {
    	
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.confirm_dialog_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
        .setCancelable(true)
        .setPositiveButton(R.string.delete, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(TextUtils.isEmpty(from)) {
				    getActivity().getContentResolver().delete(SipMessage.MESSAGE_URI, null, null);
				}else {
				    Builder threadUriBuilder = SipMessage.THREAD_ID_URI_BASE.buildUpon();
				    threadUriBuilder.appendEncodedPath(from);
				    getActivity().getContentResolver().delete(threadUriBuilder.build(), null, null);
				}
			}
		})
        .setNegativeButton(R.string.no, null)
        .setMessage(TextUtils.isEmpty(from)
                ? R.string.confirm_delete_all_conversations
                        : R.string.confirm_delete_conversation)
        .show();
    }

    boolean alreadyLoaded = false;

    @Override
    public void onVisibilityChanged(boolean visible) {

        if(visible) {
            attachAdapter();
            // Start loading
            if(!alreadyLoaded) {
                getLoaderManager().initLoader(0, null, this);
                alreadyLoaded = true;
            }
        }
        
        if (visible && isResumed()) {
            ListView lv = getListView();
            if (lv != null && mAdapter != null) {
                final int checkedPos = lv.getCheckedItemPosition();
                if (checkedPos >= 0) {
                    // TODO post instead
                    Thread t = new Thread() {
                        public void run() {
                            Cursor c = (Cursor) mAdapter.getItem(checkedPos - getListView().getHeaderViewsCount());
                            if(c != null) {
                                String from = c.getString(c.getColumnIndex(SipMessage.FIELD_FROM));
                                String to = c.getString(c.getColumnIndex(SipMessage.FIELD_TO));
                                final String fromFull = c.getString(c.getColumnIndex(SipMessage.FIELD_FROM_FULL));
                                String number = from;
                                if (SipMessage.SELF.equals(number)) {
                                    number = to;
                                }
                                final String nbr = number;
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        viewDetails(checkedPos, nbr, fromFull);
                                    }
                                });
                            }
                        };
                    };
                    t.start();
                }
            }
        }
    }

    @Override
    public void changeCursor(Cursor c) {
        if(mAdapter != null) {
            mAdapter.changeCursor(c);
        }
    }
	
    
}
