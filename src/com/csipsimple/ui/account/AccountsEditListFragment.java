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

package com.csipsimple.ui.account;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.ui.account.AccountsEditListAdapter.AccountRowTag;
import com.csipsimple.ui.account.AccountsEditListAdapter.OnCheckedRowListener;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.utils.SipProfileJson;
import com.csipsimple.wizards.BasePrefsWizard;
import com.csipsimple.wizards.WizardChooser;
import com.csipsimple.wizards.WizardUtils;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AccountsEditListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, /*OnQuitListener,*/ OnCheckedRowListener{

    private boolean dualPane;
	private Long curCheckPosition = SipProfile.INVALID_ID;
	private String curCheckWizard = WizardUtils.EXPERT_WIZARD_TAG;
	private final Handler mHandler = new Handler();
	private AccountStatusContentObserver statusObserver = null;
	
	class AccountStatusContentObserver extends ContentObserver {
		
		public AccountStatusContentObserver(Handler h) {
			super(h);
		}

		public void onChange(boolean selfChange) {
			Log.d(THIS_FILE, "Accounts status.onChange( " + selfChange + ")");
			((BaseAdapter) getListAdapter()).notifyDataSetChanged();
		}
	}
	
	
	private final static String CURRENT_CHOICE = "curChoice";
	private final static String CURRENT_WIZARD = "curWizard";

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override 
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //getListView().setSelector(R.drawable.transparent);
        getListView().setCacheColorHint(Color.TRANSPARENT);
        
        // View management
        View detailsFrame = getActivity().findViewById(R.id.details);
        dualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
        

        if (savedInstanceState != null) {
            // Restore last state for checked position.
            curCheckPosition = savedInstanceState.getLong(CURRENT_CHOICE, SipProfile.INVALID_ID);
            curCheckWizard = savedInstanceState.getString(CURRENT_WIZARD);
        }

        
        AccountsEditListAdapter adapter = new AccountsEditListAdapter(getActivity(), null);
        adapter.setOnCheckedRowListener(this);
        //getListView().setEmptyView(getActivity().findViewById(R.id.progress_container));
        //getActivity().findViewById(android.R.id.empty).setVisibility(View.GONE);
        setListAdapter(adapter);
        registerForContextMenu(getListView());

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
        
        getListView().setVerticalFadingEdgeEnabled(true);

        if (dualPane) {
            // In dual-pane mode, the list view highlights the selected item.
        	Log.d("lp", "dual pane mode");
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        	//getListView().setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
            getListView().setVerticalScrollBarEnabled(false);
            getListView().setFadingEdgeLength(50);
            
            updateCheckedItem();
            // Make sure our UI is in the correct state.
            //showDetails(curCheckPosition, curCheckWizard);
        }else {
        	//getListView().setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_RIGHT);
        	getListView().setVerticalScrollBarEnabled(true);
        	getListView().setFadingEdgeLength(100);
        }
    }

	private static final int CHOOSE_WIZARD = 0;
	private static final int CHANGE_WIZARD = 1;
	
	// Menu stuff
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(R.string.add_account)
                .setIcon(android.R.drawable.ic_menu_add)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        startActivityForResult(new Intent(getActivity(), WizardChooser.class),
                                CHOOSE_WIZARD);
                        return true;
                    }
                })
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(R.string.reorder).setIcon(android.R.drawable.ic_menu_sort_by_size)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        /*
                         * ListProfileFrag f = (ListProfileFrag)
                         * getSupportFragmentManager
                         * ().findFragmentById(R.id.list); ListProfileAdapter ad
                         * = (ListProfileAdapter) f.getListView().getAdapter();
                         * ad.toggleDraggable(); return true;
                         */
                        return true;
                    }
                }).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
        menu.add(R.string.backup_restore).setIcon(android.R.drawable.ic_menu_save)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        // Populate choice list
                        List<String> items = new ArrayList<String>();
                        items.add(getResources().getString(R.string.backup));
                        final File backupDir = PreferencesWrapper.getConfigFolder(getActivity());
                        if (backupDir != null) {
                            String[] filesNames = backupDir.list();
                            for (String fileName : filesNames) {
                                items.add(fileName);
                            }
                        }

                        final String[] fItems = (String[]) items.toArray(new String[0]);
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(R.string.backup_restore);
                        builder.setItems(fItems, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                if (item == 0) {
                                    SipProfileJson.saveSipConfiguration(getActivity());
                                } else {
                                    File fileToRestore = new File(backupDir + File.separator
                                            + fItems[item]);
                                    SipProfileJson.restoreSipConfiguration(getActivity(),
                                            fileToRestore);
                                }
                            }
                        });
                        builder.setCancelable(true);
                        AlertDialog backupDialog = builder.create();
                        backupDialog.show();
                        return true;
                    }
                });
		
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	private static final String THIS_FILE = null;
    
	/*
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
    	View v = inflater.inflate(R.layout.profiles_list, container, false);
    	ListView lv = (ListView) v.findViewById(android.R.id.list);
    	
    	lv.setDropListener(new DropListener() {
			@Override
			public void drop(int from, int to) {
				Log.d(THIS_FILE, "Drop from " + from + " to " + to);
				int i;
				ArrayList<Long> orderedList = new ArrayList<Long>();
				CursorAdapter ad = (CursorAdapter) getListAdapter();
				for(i=0; i < ad.getCount(); i++) {
					orderedList.add(ad.getItemId(i));
				}
				
				Long moved = orderedList.remove(from);
				orderedList.add(to, moved);
				Log.d(THIS_FILE, "new list is "+orderedList);
				ContentResolver cr = getActivity().getContentResolver();
				for(i=0; i<orderedList.size(); i++) {
					Uri uri = ContentUris.withAppendedId(DBAdapter.PROFILE_ID_URI_BASE, orderedList.get(i));
					ContentValues cv = new ContentValues();
					cv.put(Profile.FIELD_PRIORITY, i);
					cr.update(uri, cv, null, null);
				}
			}
		});
    	
    	return v;
    }
    */
    
    private void updateCheckedItem() {
    	if(curCheckPosition != SipProfile.INVALID_ID) {
	    	for(int i=0; i<getListAdapter().getCount(); i++) {
	        	long profId = getListAdapter().getItemId(i);
	        	if(profId == curCheckPosition) {
	        		getListView().setItemChecked(i, true);
	        	}
	        }
    	}else {
    		for(int i=0; i<getListAdapter().getCount(); i++) {
    			getListView().setItemChecked(i, false);
    		}
    	}
    }
    

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CURRENT_CHOICE, curCheckPosition);
    }
    

    @Override
    public void onResume() {
    	super.onResume();
    	statusObserver = new AccountStatusContentObserver(mHandler);
    	getActivity().getContentResolver().registerContentObserver(SipProfile.ACCOUNT_STATUS_URI, true, statusObserver);
    	((BaseAdapter) getListAdapter()).notifyDataSetChanged();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	if(statusObserver != null) {
    		getActivity().getContentResolver().unregisterContentObserver(statusObserver);
    	}
    }

    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
    	long fId = l.getItemIdAtPosition(position);
        Log.d(THIS_FILE, "Checked " + position + " et " + id + " et " + fId);
        
        getListView().setItemChecked(position, true);
        
        curCheckPosition = id;
        Cursor c = (Cursor) getListAdapter().getItem(position);
        showDetails(id, c.getString(c.getColumnIndex(SipProfile.FIELD_WIZARD)));
    }

    /**
     * Helper function to show the details of a selected item, either by
     * displaying a fragment in-place in the current UI, or starting a
     * whole new activity in which it is displayed.
     */
    private void showDetails(long profileId, String wizard) {
        //curCheckPosition = index;

        /*
    	if (dualPane) {
            // If we are not currently showing a fragment for the new
            // position, we need to create and install a new one.
        	AccountEditFragment df = AccountEditFragment.newInstance(profileId);
            //df.setOnQuitListener(this);
            // Execute a transaction, replacing any existing fragment
            // with this one inside the frame.
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.details, df, null);
          //  ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.setTransition(FragmentTransaction.TRANSIT_NONE);
            //if(profileId != Profile.INVALID_ID) {
            //	ft.addToBackStack(null);
            //}
            ft.commit();
        } else {
        */
        	
            // Otherwise we need to launch a new activity to display
            // the dialog fragment with selected text.
            Intent intent = new Intent();
            //intent.setClass(getActivity(), AccountEdit.class);
            intent.setClass(getActivity(), BasePrefsWizard.class);
            if(profileId != SipProfile.INVALID_ID) {
                intent.putExtra(SipProfile.FIELD_ID, profileId);
            }
            intent.putExtra(SipProfile.FIELD_WIZARD, wizard);
            startActivity(intent);
        	
        	/*
        }
        */
    }

    /*
	@Override
	public void onQuit() {
		curCheckPosition = SipProfile.INVALID_ID;
		if(dualPane) {
			showDetails(curCheckPosition, null);
		}
	}

	@Override
	public void onShowProfile(long profileId) {
		curCheckPosition = profileId;
		updateCheckedItem();
	}
	*/

	@Override
	public void onToggleRow(AccountRowTag tag) {
		ContentValues cv = new ContentValues();
		cv.put(SipProfile.FIELD_ACTIVE, !tag.activated);
		getActivity().getContentResolver().update(ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, tag.accountId), cv, null, null);
	}
    
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		 return new CursorLoader(getActivity(), SipProfile.ACCOUNT_URI, new String[] {
			SipProfile.FIELD_ID + " AS " + BaseColumns._ID,
			SipProfile.FIELD_ID,
			SipProfile.FIELD_DISPLAY_NAME,
			SipProfile.FIELD_WIZARD,
			SipProfile.FIELD_ACTIVE
		 }, null, null, null);
		 
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.d(THIS_FILE, "Load of ad finished");
		
		// Remove loading indicator
		//getActivity().findViewById(R.id.progress_container).setVisibility(View.GONE);
		// Reset empty view
        //getListView().setEmptyView(getActivity().findViewById(android.R.id.empty));
		
        // Swap cursor
        ((CursorAdapter) getListAdapter()).swapCursor(data);
        // Select correct item if any
		updateCheckedItem();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		((CursorAdapter) getListAdapter()).swapCursor(null);
	}

	

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
		    
		    if(requestCode == CHOOSE_WIZARD) {
		        // Wizard has been choosen, now create an account
    			String wizardId = data.getStringExtra(WizardUtils.ID);
    			if (wizardId != null) {
    			    showDetails(SipProfile.INVALID_ID, wizardId);
    			}
            } else if (requestCode == CHANGE_WIZARD) {
                // Change wizard done for this account.
                String wizardId = data.getStringExtra(WizardUtils.ID);
                long accountId = data.getLongExtra(Intent.EXTRA_UID, SipProfile.INVALID_ID);
                
                if (wizardId != null && accountId != SipProfile.INVALID_ID) {
                    ContentValues cv = new ContentValues();
                    cv.put(SipProfile.FIELD_WIZARD, wizardId);
                    getActivity().getContentResolver().update(
                            ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, accountId),
                            cv, null, null);

                }

            }
		}

	}


	// Context menu stuff
	// Activate / deactive menu
    public static final int MENU_ITEM_ACTIVATE = Menu.FIRST;
    // Modify the account
    public static final int MENU_ITEM_MODIFY = Menu.FIRST + 1;
    // Delete the account
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 2;
    // Change the wizard of the account
    public static final int MENU_ITEM_WIZARD = Menu.FIRST + 3;

    /**
     * Retrieve sip account from a given context menu info pressed
     * @param cmi The context menu info to retrieve infos from
     * @return corresponding sip profile if everything goes well, null if not able to retrieve profile
     */
    private SipProfile profileFromContextMenuInfo(ContextMenuInfo cmi) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) cmi;
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
            return null;
        }
        Cursor c = (Cursor) getListAdapter().getItem(info.position);
        if (c == null) {
            // For some reason the requested item isn't available, do nothing
            return null;
        }
        return new SipProfile(c);
    }
    
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final SipProfile account = profileFromContextMenuInfo(menuInfo);
        if(account == null) {
            return;
        }
        WizardInfo wizardInfos = WizardUtils.getWizardClass(account.wizard);

        // Setup the menu header
        menu.setHeaderTitle(account.display_name);
        menu.setHeaderIcon(wizardInfos.icon);

        // Add a menu item to delete the note
        menu.add(0, MENU_ITEM_ACTIVATE, 0, account.active ? R.string.deactivate_account
                : R.string.activate_account);
        menu.add(0, MENU_ITEM_MODIFY, 0, R.string.modify_account);
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.delete_account);
        menu.add(0, MENU_ITEM_WIZARD, 0, R.string.choose_wizard);

    }

	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    final SipProfile account = profileFromContextMenuInfo(item.getMenuInfo());
        if (account == null) {
            // For some reason the requested item isn't available, do nothing
            return super.onContextItemSelected(item);
        }
        
        switch (item.getItemId()) {
            case MENU_ITEM_DELETE: {
                getActivity().getContentResolver().delete(ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, account.id), null, null);
                return true;
            }
            case MENU_ITEM_MODIFY : {
                showDetails(account.id, account.wizard);
                return true;
            }
            case MENU_ITEM_ACTIVATE: {
                ContentValues cv = new ContentValues();
                cv.put(SipProfile.FIELD_ACTIVE, ! account.active);
                getActivity().getContentResolver().update(ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, account.id), cv, null, null);
                return true;
            }
            case MENU_ITEM_WIZARD:{
                Intent it = new Intent(getActivity(), WizardChooser.class);
                it.putExtra(Intent.EXTRA_UID, account.id);
                startActivityForResult(it, CHANGE_WIZARD);
                return true;
            }
        }
        return super.onContextItemSelected(item);

	}
}
