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

package com.csipsimple.widgets;

import android.content.ComponentName;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.internal.view.View_HasStateListenerSupport;
import com.actionbarsherlock.internal.view.View_OnAttachStateChangeListener;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.internal.view.menu.MenuPopupHelper;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.AccountListUtils;
import com.csipsimple.utils.AccountListUtils.AccountStatusDisplay;
import com.csipsimple.utils.CallHandlerPlugin;
import com.csipsimple.utils.CallHandlerPlugin.OnLoadListener;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;
import com.csipsimple.wizards.WizardUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AccountChooserButton extends LinearLayout implements OnClickListener, View_HasStateListenerSupport {

    protected static final String THIS_FILE = "AccountChooserButton";

    private static final String[] ACC_PROJECTION = new String[] {
                SipProfile.FIELD_ID,
                SipProfile.FIELD_ACC_ID, // Needed for default domain
                SipProfile.FIELD_REG_URI, // Needed for default domain
                SipProfile.FIELD_PROXY, // Needed for default domain
                SipProfile.FIELD_DEFAULT_URI_SCHEME, // Needed for default scheme
                SipProfile.FIELD_DISPLAY_NAME,
                SipProfile.FIELD_WIZARD
        };

    private final TextView textView;
    private final ImageView imageView;
    private SipProfile account = null;
    private Long targetAccountId = null;

    private boolean showExternals = true;
    
    private final ComponentName telCmp;

    private OnAccountChangeListener onAccountChange = null;

    /**
     * Interface definition for a callback to be invoked when PjSipAccount is
     * choosen
     */
    public interface OnAccountChangeListener {

        /**
         * Called when the user make an action
         * 
         * @param keyCode keyCode pressed
         * @param dialTone corresponding dialtone
         */
        void onChooseAccount(SipProfile account);
    }

    public AccountChooserButton(Context context) {
        this(context, null);
    }
    
    public AccountChooserButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        telCmp = new ComponentName(getContext(), com.csipsimple.plugins.telephony.CallHandler.class);
        
        // UI management
        setClickable(true);
        setFocusable(true);
        setBackgroundResource(R.drawable.abs__spinner_ab_holo_dark);
        setOrientation(VERTICAL);
        setPadding(6, 0, 6, 0);
        setGravity(Gravity.CENTER);
        // Inflate sub views
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.account_chooser_button, this, true);
        setOnClickListener(this);
        textView = (TextView) findViewById(R.id.quickaction_text);
        imageView = (ImageView) findViewById(R.id.quickaction_icon);

        mMenuBuilder = new MenuBuilder(getContext());
        
        // Init accounts
        setAccount(null);
    }
    
    public AccountChooserButton(Context context, AttributeSet attrs, int style) {
        this(context, attrs);
    }
    
    private final Handler mHandler = new Handler();
    private AccountStatusContentObserver statusObserver = null;
    private boolean canChangeIfValid = true;

    private MenuPopupHelper mPopupMenu;
    private MenuBuilder mMenuBuilder;
    private final Set<View_OnAttachStateChangeListener> mListeners = new HashSet<View_OnAttachStateChangeListener>();

    /**
     * Observer for changes of account registration status
     */
    class AccountStatusContentObserver extends ContentObserver {
        public AccountStatusContentObserver(Handler h) {
            super(h);
        }

        public void onChange(boolean selfChange) {
            Log.d(THIS_FILE, "Accounts status.onChange( " + selfChange + ")");
            updateRegistration();
        }
    }

    /**
     * Allow this widget to automatically change the current account without
     * user interaction if a new account is registered with higher priority
     * 
     * @param changeable Whether the widget is allowed to change selected
     *            account by itself
     */
    public void setChangeable(boolean changeable) {
        canChangeIfValid = changeable;
    }

    /**
     * Set the account id that should be tried to be adopted by this button if
     * available If null it will try to select account with higher priority
     * 
     * @param aTargetAccountId id of the account to try to select
     */
    public void setTargetAccount(Long aTargetAccountId) {
        targetAccountId = aTargetAccountId;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(statusObserver == null) {
            statusObserver = new AccountStatusContentObserver(mHandler);
            getContext().getContentResolver().registerContentObserver(SipProfile.ACCOUNT_STATUS_URI,
                    true, statusObserver);
        }
        if(!isInEditMode()) {
            updateRegistration();
        }

        for (View_OnAttachStateChangeListener listener : mListeners) {
            listener.onViewAttachedToWindow(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (statusObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(statusObserver);
            statusObserver = null;
        }

        for (View_OnAttachStateChangeListener listener : mListeners) {
            listener.onViewDetachedFromWindow(this);
        }
    }

    @Override
    public void onClick(View v) {
        Log.d(THIS_FILE, "Click the account chooser button");

        if(mPopupMenu == null) {
            mPopupMenu = new MenuPopupHelper(getContext(), mMenuBuilder, this, false);
            mPopupMenu.setForceShowIcon(true);
        }
        mMenuBuilder.removeGroup(R.id.menu_accbtn_accounts);

        Cursor c = getContext().getContentResolver().query(SipProfile.ACCOUNT_URI, ACC_PROJECTION, SipProfile.FIELD_ACTIVE + "=?", new String[] {
                "1"
        }, null);
        
        boolean hasSomeSip = false;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    do {
                        final SipProfile account = new SipProfile(c);
                        AccountStatusDisplay accountStatusDisplay = AccountListUtils
                                .getAccountDisplay(getContext(), account.id);
                        if (accountStatusDisplay.availableForCalls) {
                            BitmapDrawable drawable = new BitmapDrawable(getResources(), 
                                    WizardUtils.getWizardBitmap(getContext(), account));
                            
                            MenuItem item = mMenuBuilder.add(R.id.menu_accbtn_accounts, MenuBuilder.NONE, MenuBuilder.NONE, account.display_name);
                            item.setIcon(drawable);
                            item.setOnMenuItemClickListener(new OnAccountMenuItemListener(account));
                            
                            hasSomeSip = true;
                        }
                    } while (c.moveToNext());
                }
            } catch (Exception e) {
                Log.e(THIS_FILE, "Error on looping over sip profiles", e);
            } finally {
                c.close();
            }
        }
        if(!hasSomeSip) {
            MenuItem item = mMenuBuilder.add(R.id.menu_accbtn_accounts, MenuBuilder.NONE, MenuBuilder.NONE, R.string.acct_inactive);
            item.setIcon(android.R.drawable.ic_dialog_alert);
        }

        if (showExternals) {
            // Add external rows
            Map<String, String> callHandlers = CallHandlerPlugin.getAvailableCallHandlers(getContext());
            boolean includeGsm = Compatibility.canMakeGSMCall(getContext()); 
            for (String packageName : callHandlers.keySet()) {
                Log.d(THIS_FILE, "Compare "+packageName+" to "+telCmp.flattenToString());
                // We ensure that GSM integration is not prevented
                if(!includeGsm && packageName.equals(telCmp.flattenToString())) {
                    continue;
                }
                // Else we can add
                CallHandlerPlugin ch = new CallHandlerPlugin(getContext());
                ch.loadFrom(packageName, null, new OnPluginLoadListener());
            }
        }

        mPopupMenu.show();
    }
    
    private class OnPluginLoadListener implements OnLoadListener {
        @Override
        public void onLoad(CallHandlerPlugin ch) {
            mHandler.post(new PluginButtonManager(ch));
        }
    }
    /**
     * This runnable is intended to be run in UI thread (so in handler).
     */
    private class PluginButtonManager implements Runnable {
        CallHandlerPlugin ch;

        PluginButtonManager(CallHandlerPlugin callHandler) {
            ch = callHandler;
        }

        @Override
        public void run() {
            MenuItem item = mMenuBuilder.add(R.id.menu_accbtn_accounts, Menu.NONE, Menu.NONE,  ch.getLabel().toString());
            item.setIcon(ch.getIconDrawable());
            item.setOnMenuItemClickListener(new OnAccountMenuItemListener(ch.getFakeProfile()));
        }
    }

    /**
     * Set the currently selected account for this widget
     * It will change internal state,
     * Change icon and label of the account
     * @param aAccount
     */
    public void setAccount(SipProfile aAccount) {
        account = aAccount;

        if (account == null) {
            if(isInEditMode() || Compatibility.canMakeGSMCall(getContext())) {
                textView.setText(getResources().getString(R.string.gsm));
                imageView.setImageResource(R.drawable.ic_wizard_gsm);
            }else {
                textView.setText(getResources().getString(R.string.acct_inactive));
                imageView.setImageResource(android.R.drawable.ic_dialog_alert);
            }
        } else {
            textView.setText(account.display_name);
            imageView.setImageDrawable(new BitmapDrawable(getResources(), WizardUtils.getWizardBitmap(getContext(),
                    account)));
        }
        if (onAccountChange != null) {
            onAccountChange.onChooseAccount(account);
        }

    }

    /**
     * Update user interface when registration of account has changed
     * This include change selected account if we are in canChangeIfValid mode
     */
    private void updateRegistration() {
        Cursor c = getContext().getContentResolver().query(SipProfile.ACCOUNT_URI, ACC_PROJECTION, SipProfile.FIELD_ACTIVE + "=?", new String[] {
                "1"
        }, null);

        SipProfile toSelectAcc = null;
        SipProfile firstAvail = null;
        
        if (c != null) {
            try {
                if (c.getCount() > 0 && c.moveToFirst()) {
                    do {
                        final SipProfile acc = new SipProfile(c);

                        AccountStatusDisplay accountStatusDisplay = AccountListUtils
                                .getAccountDisplay(getContext(), acc.id);
                        if (accountStatusDisplay.availableForCalls) {
                            if (firstAvail == null) {
                                firstAvail = acc;
                            }

                            if (canChangeIfValid) {
                                // We can change even if valid, so select this
                                // account if valid for outgoings
                                if(targetAccountId != null) {
                                    // Check if this is the target one
                                    if(targetAccountId == acc.id) {
                                        toSelectAcc = acc;
                                        break;
                                    }
                                }else {
                                    // Select first
                                    toSelectAcc = acc;
                                    break;
                                }
                            } else if (account != null && account.id == acc.id) {
                                // Current is valid
                                toSelectAcc = acc;
                                break;
                            }
                        }
                    } while (c.moveToNext());
                }
            } catch (Exception e) {
                Log.e(THIS_FILE, "Error on looping over sip profiles", e);
            } finally {
                c.close();
            }
        }

        if (toSelectAcc == null) {
            // Nothing to force select, fallback to first avail
            toSelectAcc = firstAvail;
        }

        // Finally, set the account to be valid
        setAccount(toSelectAcc);
    }

    /**
     * Retrieve account that is currently selected by this widget
     * @return The SipProfile selected
     */
    public SipProfile getSelectedAccount() {
        if (account == null) {
            SipProfile retAcc = new SipProfile();
            if(showExternals) {
                Map<String, String> handlers = CallHandlerPlugin.getAvailableCallHandlers(getContext());
                boolean includeGsm = Compatibility.canMakeGSMCall(getContext()); 
                
                if(includeGsm) {
                    for (String callHandler : handlers.keySet()) {
                        // Try to prefer the GSM handler
                        if (callHandler.equalsIgnoreCase(telCmp.flattenToString())) {
                            retAcc.id = CallHandlerPlugin.getAccountIdForCallHandler(getContext(), callHandler);
                            return retAcc;
                        }
                    }
                }
                
                // Fast way to get first if exists
                for (String callHandler : handlers.values()) {
                    // Ignore tel handler if we do not include gsm in settings
                    if(callHandler.equals(telCmp.flattenToString()) && !includeGsm) {
                        continue;
                    }
                    retAcc.id = CallHandlerPlugin.getAccountIdForCallHandler(getContext(), callHandler);
                    return retAcc;
                }
            }

            retAcc.id = SipProfile.INVALID_ID;
            return retAcc;
        }
        return account;
    }

    /**
     * Attach listened to the widget that will fire when account selection change
     * @param anAccountChangeListener the listener
     */
    public void setOnAccountChangeListener(OnAccountChangeListener anAccountChangeListener) {
        onAccountChange = anAccountChangeListener;
    }

    /**
     * Set whether this button should consider plugins account as selectable accounts
     * @param b true if you want the widget to show external accounts
     */
    public void setShowExternals(boolean b) {
        showExternals = b;
    }


    /* (non-Javadoc)
     * @see com.actionbarsherlock.internal.view.View_HasStateListenerSupport#addOnAttachStateChangeListener(com.actionbarsherlock.internal.view.View_OnAttachStateChangeListener)
     */
    @Override
    public void addOnAttachStateChangeListener(View_OnAttachStateChangeListener listener) {
        mListeners.add(listener);
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.internal.view.View_HasStateListenerSupport#removeOnAttachStateChangeListener(com.actionbarsherlock.internal.view.View_OnAttachStateChangeListener)
     */
    @Override
    public void removeOnAttachStateChangeListener(View_OnAttachStateChangeListener listener) {
        mListeners.remove(listener);
    }
    
    
    private class OnAccountMenuItemListener implements OnMenuItemClickListener {
        private SipProfile mAccount;
        OnAccountMenuItemListener(SipProfile account){
            mAccount = account;
        }
        /* (non-Javadoc)
         * @see com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener#onMenuItemClick(com.actionbarsherlock.view.MenuItem)
         */
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            setAccount(mAccount);
            return true;
        }
    }
    
    public MenuItem addExtraMenuItem(int titleRes) {
        return mMenuBuilder.add(R.id.menu_accbtn_extras, MenuBuilder.NONE, 100, titleRes);
    }

}
