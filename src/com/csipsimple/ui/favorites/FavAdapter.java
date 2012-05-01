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

package com.csipsimple.ui.favorites;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.BaseColumns;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.internal.view.menu.ActionMenuPresenter;
import com.actionbarsherlock.internal.view.menu.ActionMenuView;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.internal.view.menu.MenuBuilder.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.contacts.ContactsWrapper;
import com.csipsimple.wizards.WizardUtils;

public class FavAdapter extends ResourceCursorAdapter {


    //private static final String THIS_FILE = "FavAdapter";
    
    private final static int MENU_SET_GROUP = Menu.FIRST;
    private final static int MENU_SET_SIP_DATA = Menu.FIRST + 1;
    private final static int MENU_SHARE_PRESENCE = Menu.FIRST + 2;

    public FavAdapter(Context context, Cursor c) {
        super(context, R.layout.fav_list_item, c, 0);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        ContentValues cv = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(cursor, cv);
        
        int type = ContactsWrapper.TYPE_CONTACT;
        if(cv.containsKey(ContactsWrapper.FIELD_TYPE)) {
            type = cv.getAsInteger(ContactsWrapper.FIELD_TYPE);
        }
        
        if(type == ContactsWrapper.TYPE_GROUP) {
            showViewForHeader(view, true);
            
            // Get views
            TextView tv = (TextView) view.findViewById(R.id.header_text);
            ImageView icon = (ImageView) view.findViewById(R.id.header_icon);
            PresenceStatusSpinner presSpinner = (PresenceStatusSpinner) view.findViewById(R.id.header_presence_spinner);
            
            // Get datas
            final Long profileId = cv.getAsLong(BaseColumns._ID);
            final String groupName = cv.getAsString(SipProfile.FIELD_ANDROID_GROUP);
            final String displayName = cv.getAsString(SipProfile.FIELD_DISPLAY_NAME);
            final String wizard = cv.getAsString(SipProfile.FIELD_WIZARD);
            final boolean publishedEnabled = (cv.getAsInteger(SipProfile.FIELD_PUBLISH_ENABLED) == 1);
            
            // Bind datas to view
            tv.setText(displayName);
            icon.setImageResource(WizardUtils.getWizardIconRes(wizard));
            presSpinner.setProfileId(profileId);
            
            // Extra menu view if not already set
            ViewGroup menuViewWrapper = (ViewGroup) view.findViewById(R.id.header_cfg_spinner);
            
            MenuCallback newMcb = new MenuCallback(context, profileId, groupName, publishedEnabled);
            
            if(menuViewWrapper.getTag() == null) {

                final LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.MATCH_PARENT);

                ActionMenuPresenter mActionMenuPresenter = new ActionMenuPresenter(mContext);
                mActionMenuPresenter.setReserveOverflow(true);
                MenuBuilder menuBuilder = new MenuBuilder(context);
                menuBuilder.setCallback(newMcb);
                menuBuilder.add(0, MENU_SET_GROUP, 0, R.string.set_android_group).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                menuBuilder.add(0, MENU_SET_SIP_DATA, 0, R.string.set_sip_data).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                menuBuilder.add(0, MENU_SHARE_PRESENCE, 0, publishedEnabled ? R.string.deactivate_presence_sharing : R.string.activate_presence_sharing).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                
                menuBuilder.addMenuPresenter(mActionMenuPresenter);
                ActionMenuView menuView = (ActionMenuView) mActionMenuPresenter.getMenuView(menuViewWrapper);
                menuView.setBackgroundDrawable(null);
                menuViewWrapper.addView(menuView, layoutParams);
                menuViewWrapper.setTag(menuBuilder);
            }else {
                MenuBuilder menuBuilder = (MenuBuilder) menuViewWrapper.getTag();
                menuBuilder.setCallback(newMcb);
                menuBuilder.findItem(MENU_SHARE_PRESENCE).setTitle(publishedEnabled ? R.string.deactivate_presence_sharing : R.string.activate_presence_sharing);
            }
        }else {
            showViewForHeader(view, false);
            ContactsWrapper.getInstance().bindContactView(view, context, cursor);
            
        }
    }
    
    private void showViewForHeader(View view, boolean isHeader) {
        view.findViewById(R.id.header_view).setVisibility(isHeader ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.contact_view).setVisibility(isHeader ? View.GONE : View.VISIBLE);
    }
    
    private class MenuCallback implements Callback {
        private Long profileId = SipProfile.INVALID_ID;
        private Context context;
        private String groupName;
        private boolean publishEnabled;
        
        public MenuCallback(Context ctxt, Long aProfileId, String aGroupName, boolean aPublishedEnabled) {
            profileId = aProfileId;
            context = ctxt;
            groupName = aGroupName;
            publishEnabled = aPublishedEnabled;
        }
        
        @Override
        public void onMenuModeChange(MenuBuilder menu) {
            // Nothing to do
        }
        
        @Override
        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
            int itemId = item.getItemId();
            if(itemId == MENU_SET_GROUP) {
                showDialogForGroupSelection(context, profileId, groupName);
                return true;
            }else if(itemId == MENU_SHARE_PRESENCE) {
                ContentValues cv = new ContentValues();
                cv.put(SipProfile.FIELD_PUBLISH_ENABLED, publishEnabled ? 0 : 1);
                context.getContentResolver().update(ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, profileId), cv, null, null);
                return true;
            }
            return false;
        }
    }

    
    private void showDialogForGroupSelection(final Context context, final Long profileId, final String groupName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.set_android_group);
        final Cursor choiceCursor = ContactsWrapper.getInstance().getGroups(context);
        int selectedIndex = -1;
        if(choiceCursor != null) {
            if (choiceCursor.moveToFirst()) {
                int i = 0;
                int colIdx = choiceCursor.getColumnIndex(ContactsWrapper.FIELD_GROUP_NAME);
                do {
                    String name = choiceCursor.getString(colIdx);
                    if(name.equalsIgnoreCase(groupName)) {
                        selectedIndex = i;
                        break;
                    }
                    i ++;
                } while (choiceCursor.moveToNext());
            }
        }
        builder.setSingleChoiceItems(choiceCursor, selectedIndex, ContactsWrapper.FIELD_GROUP_NAME, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                choiceCursor.moveToPosition(which);
                String name = choiceCursor.getString(choiceCursor.getColumnIndex(ContactsWrapper.FIELD_GROUP_NAME));
                ContentValues cv = new ContentValues();
                cv.put(SipProfile.FIELD_ANDROID_GROUP, name);
                context.getContentResolver().update(ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, profileId), cv, null, null);
                choiceCursor.close();
                dialog.dismiss();
            }
        });
        
        builder.setCancelable(true);
        builder.setNeutralButton(R.string.cancel, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                choiceCursor.close();
                dialog.dismiss();
            }
        });
        builder.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                choiceCursor.close();
            }
        });
        final Dialog dialog = builder.create();
        dialog.show();
    }
}

