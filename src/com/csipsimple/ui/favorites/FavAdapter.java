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
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.BaseColumns;
import android.provider.ContactsContract.Contacts;
import android.support.v4.widget.ResourceCursorAdapter;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.internal.utils.UtilityWrapper;
import com.actionbarsherlock.internal.view.menu.ActionMenuPresenter;
import com.actionbarsherlock.internal.view.menu.ActionMenuView;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.internal.view.menu.MenuBuilder.Callback;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.contacts.ContactsWrapper;
import com.csipsimple.utils.contacts.ContactsWrapper.Phone;
import com.csipsimple.wizards.WizardUtils;

import java.util.List;

public class FavAdapter extends ResourceCursorAdapter implements OnClickListener {


    private static final String THIS_FILE = "FavAdapter";
    
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

        showViewForType(view, type);
        
        
        if(type == ContactsWrapper.TYPE_GROUP) {
            // Get views
            TextView tv = (TextView) view.findViewById(R.id.header_text);
            ImageView icon = (ImageView) view.findViewById(R.id.header_icon);
            PresenceStatusSpinner presSpinner = (PresenceStatusSpinner) view.findViewById(R.id.header_presence_spinner);
            
            // Get datas
            SipProfile acc = new SipProfile(cursor);
            
            final Long profileId = cv.getAsLong(BaseColumns._ID);
            final String groupName = acc.android_group;
            final String displayName = acc.display_name;
            final String wizard = acc.wizard;
            final boolean publishedEnabled = (acc.publish_enabled == 1);
            final String domain = acc.getDefaultDomain();
            
            // Bind datas to view
            tv.setText(displayName);
            icon.setImageResource(WizardUtils.getWizardIconRes(wizard));
            presSpinner.setProfileId(profileId);
            
            // Extra menu view if not already set
            ViewGroup menuViewWrapper = (ViewGroup) view.findViewById(R.id.header_cfg_spinner);
            
            MenuCallback newMcb = new MenuCallback(context, profileId, groupName, domain, publishedEnabled);
            MenuBuilder menuBuilder;
            if(menuViewWrapper.getTag() == null) {

                final LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.MATCH_PARENT);

                ActionMenuPresenter mActionMenuPresenter = new ActionMenuPresenter(mContext);
                mActionMenuPresenter.setReserveOverflow(true);
                menuBuilder = new MenuBuilder(context);
                menuBuilder.setCallback(newMcb);
                MenuInflater inflater = new MenuInflater(context);
                inflater.inflate(R.menu.fav_menu, menuBuilder);
                menuBuilder.addMenuPresenter(mActionMenuPresenter);
                ActionMenuView menuView = (ActionMenuView) mActionMenuPresenter.getMenuView(menuViewWrapper);
                UtilityWrapper.getInstance().setBackgroundDrawable(menuView, null);
                menuViewWrapper.addView(menuView, layoutParams);
                menuViewWrapper.setTag(menuBuilder);
            }else {
                menuBuilder = (MenuBuilder) menuViewWrapper.getTag();
                menuBuilder.setCallback(newMcb);
            }
            menuBuilder.findItem(R.id.share_presence).setTitle(publishedEnabled ? R.string.deactivate_presence_sharing : R.string.activate_presence_sharing);
            menuBuilder.findItem(R.id.set_sip_data).setVisible(!TextUtils.isEmpty(groupName));
            
        }else if(type == ContactsWrapper.TYPE_CONTACT) {
            ContactsWrapper.getInstance().bindContactView(view, context, cursor);
        } else if (type == ContactsWrapper.TYPE_CONFIGURE) {
            // We only bind if it's the correct type
            View v = view.findViewById(R.id.configure_view);
            v.setOnClickListener(this);
            ConfigureObj cfg = new ConfigureObj();
            cfg.profileId = cv.getAsLong(BaseColumns._ID);
            v.setTag(cfg);
        }
    }
    
    private class ConfigureObj extends Object {
        Long profileId = SipProfile.INVALID_ID;
        String groupName = "";
    }
    
    private void showViewForType(View view, int type) {
        
        view.findViewById(R.id.header_view).setVisibility((type == ContactsWrapper.TYPE_GROUP) ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.contact_view).setVisibility((type == ContactsWrapper.TYPE_CONTACT) ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.configure_view).setVisibility((type == ContactsWrapper.TYPE_CONFIGURE) ? View.VISIBLE : View.GONE);
    }
    
    private class MenuCallback implements Callback {
        private Long profileId = SipProfile.INVALID_ID;
        private Context context;
        private String groupName;
        private String domain;
        private boolean publishEnabled;
        
        public MenuCallback(Context ctxt, Long aProfileId, String aGroupName, String aDomain, boolean aPublishedEnabled) {
            profileId = aProfileId;
            context = ctxt;
            groupName = aGroupName;
            domain = aDomain;
            publishEnabled = aPublishedEnabled;
        }
        
        @Override
        public void onMenuModeChange(MenuBuilder menu) {
            // Nothing to do
        }
        
        @Override
        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
            int itemId = item.getItemId();
            if(itemId == R.id.set_group) {
                showDialogForGroupSelection(context, profileId, groupName);
                return true;
            }else if(itemId == R.id.share_presence) {
                ContentValues cv = new ContentValues();
                cv.put(SipProfile.FIELD_PUBLISH_ENABLED, publishEnabled ? 0 : 1);
                context.getContentResolver().update(ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, profileId), cv, null, null);
                return true;
            }else if(itemId == R.id.set_sip_data) {
                showDialogForSipData(context, profileId, groupName, domain);
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
        builder.setSingleChoiceItems(choiceCursor, selectedIndex, ContactsWrapper.FIELD_GROUP_NAME, new DialogInterface.OnClickListener() {
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
        builder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
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
    
    private void showDialogForSipData(final Context context, final Long profileId, final String groupName, final String domain) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.set_android_group);
        builder.setCancelable(true);
        builder.setItems(R.array.sip_data_sources, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                applyNumbersToCSip(groupName, 1 << which, domain);
            }
        });
        
        final Dialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.configure_view) {
            ConfigureObj cfg = (ConfigureObj) v.getTag();
            showDialogForGroupSelection(mContext, cfg.profileId, cfg.groupName);
        }
    }
    
    private void applyNumbersToCSip(String groupName, int flag, String domain) {
        Log.d(THIS_FILE, "Apply numbers to csip " + groupName + " > " + domain);
        ContactsWrapper cw = ContactsWrapper.getInstance();
        Cursor c = cw.getContactsByGroup(mContext, groupName);
        try {
            while (c.moveToNext()) {
                long contactId = c.getLong(c.getColumnIndex(Contacts._ID));
                List<Phone> phones = cw.getPhoneNumbers(mContext, contactId, flag);
                if(phones.size() > 0){
                    String nbr = phones.get(0).getNumber();
                    if(!nbr.contains("@")){
                        nbr += "@" + domain;
                    }
                    Log.d(THIS_FILE, "Apply number to " + contactId + " > " + nbr);
                    cw.insertOrUpdateCSipUri(mContext, contactId, nbr);
                }
            }
        } catch (Exception e) {
            Log.e(THIS_FILE, "Error while looping on contacts", e);
        } finally {
            c.close();
        }
    }
}

