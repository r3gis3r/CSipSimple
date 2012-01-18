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

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.AccountListUtils;
import com.csipsimple.utils.AccountListUtils.AccountStatusDisplay;
import com.csipsimple.utils.CallHandler;
import com.csipsimple.utils.CallHandler.onLoadListener;
import com.csipsimple.utils.Log;
import com.csipsimple.wizards.WizardUtils;

import java.util.Map;

public class AccountChooserButton extends LinearLayout implements OnClickListener {

    protected static final String THIS_FILE = "AccountChooserButton";

    private final TextView textView;
    private final ImageView imageView;
    private HorizontalQuickActionWindow quickAction;
    private SipProfile account = null;
    private Long targetAccountId = null;

    private boolean showExternals = true;

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

    public AccountChooserButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.account_chooser_button, this, true);
        LinearLayout root = (LinearLayout) findViewById(R.id.quickaction_button);
        root.setOnClickListener(this);

        textView = (TextView) findViewById(R.id.quickaction_text);
        imageView = (ImageView) findViewById(R.id.quickaction_icon);
        setAccount(null);
    }

    private final Handler mHandler = new Handler();
    private AccountStatusContentObserver statusObserver = null;
    private boolean canChangeIfValid = true;

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
        statusObserver = new AccountStatusContentObserver(mHandler);
        getContext().getContentResolver().registerContentObserver(SipProfile.ACCOUNT_STATUS_URI,
                true, statusObserver);
        updateRegistration();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (statusObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(statusObserver);
        }
    }

    @Override
    public void onClick(View v) {
        Log.d(THIS_FILE, "Click the account chooser button");
        int[] xy = new int[2];
        v.getLocationInWindow(xy);
        Rect r = new Rect(xy[0], xy[1], xy[0] + v.getWidth(), xy[1] + v.getHeight());

        if (quickAction == null) {
            LinearLayout root = (LinearLayout) findViewById(R.id.quickaction_button);
            quickAction = new HorizontalQuickActionWindow(getContext(), root);
        }

        quickAction.setAnchor(r);
        quickAction.removeAllItems();

        Cursor c = getContext().getContentResolver().query(SipProfile.ACCOUNT_URI, new String[] {
                SipProfile.FIELD_ID,
                SipProfile.FIELD_DISPLAY_NAME,
                SipProfile.FIELD_WIZARD
        }, SipProfile.FIELD_ACTIVE + "=?", new String[] {
                "1"
        }, null);

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    do {
                        final SipProfile account = new SipProfile(c);
                        AccountStatusDisplay accountStatusDisplay = AccountListUtils
                                .getAccountDisplay(getContext(), account.id);
                        if (accountStatusDisplay.availableForCalls) {
                            BitmapDrawable drawable = new BitmapDrawable(
                                    WizardUtils.getWizardBitmap(getContext(), account));
                            quickAction.addItem(drawable, account.display_name,
                                    new OnClickListener() {
                                        public void onClick(View v) {
                                            setAccount(account);
                                            quickAction.dismiss();
                                        }
                                    });
                        }
                    } while (c.moveToNext());
                }
            } catch (Exception e) {
                Log.e(THIS_FILE, "Error on looping over sip profiles", e);
            } finally {
                c.close();
            }
        }

        if (showExternals) {
            // Add external rows
            Map<String, String> callHandlers = CallHandler.getAvailableCallHandlers(getContext());
            for (String packageName : callHandlers.keySet()) {
                CallHandler ch = new CallHandler(getContext());
                ch.loadFrom(packageName, null, new onLoadListener() {
                    @Override
                    public void onLoad(final CallHandler ch) {
                        quickAction.addItem(ch.getIconDrawable(), ch.getLabel().toString(),
                                new OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        setAccount(ch.getFakeProfile());
                                        quickAction.dismiss();
                                    }
                                });
                    }
                });
            }
        }

        quickAction.show();
    }

    public void setAccount(SipProfile aAccount) {
        account = aAccount;

        if (account == null) {
            textView.setText(getResources().getString(R.string.gsm));
            imageView.setImageResource(R.drawable.ic_wizard_gsm);
        } else {
            textView.setText(account.display_name);
            imageView.setImageDrawable(new BitmapDrawable(WizardUtils.getWizardBitmap(getContext(),
                    account)));
        }
        if (onAccountChange != null) {
            onAccountChange.onChooseAccount(account);
        }

    }

    private void updateRegistration() {
        Cursor c = getContext().getContentResolver().query(SipProfile.ACCOUNT_URI, new String[] {
                SipProfile.FIELD_ID,
                SipProfile.FIELD_DISPLAY_NAME,
                SipProfile.FIELD_WIZARD
        }, SipProfile.FIELD_ACTIVE + "=?", new String[] {
                "1"
        }, null);

        SipProfile toSelectAcc = null;
        SipProfile firstAvail = null;
        
        if (c != null && c.getCount() > 0) {
            try {
                if (c.moveToFirst()) {
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

    public SipProfile getSelectedAccount() {
        if (account == null) {
            SipProfile retAcc = new SipProfile();
            Map<String, String> handlers = CallHandler.getAvailableCallHandlers(getContext());
            for (String callHandler : handlers.keySet()) {
                // Try to prefer the GSM handler
                if (callHandler
                        .equalsIgnoreCase("com.csipsimple/com.csipsimple.plugins.telephony.CallHandler")) {
                    Log.d(THIS_FILE, "Prefer GSM");
                    retAcc.id = CallHandler.getAccountIdForCallHandler(getContext(), callHandler);
                    return retAcc;
                }
            }
            // Fast way to get first if exists
            for (String callHandler : handlers.values()) {
                retAcc.id = CallHandler.getAccountIdForCallHandler(getContext(), callHandler);
                return retAcc;
            }

            retAcc.id = SipProfile.INVALID_ID;
            return retAcc;
        }
        return account;
    }

    public void setOnAccountChangeListener(OnAccountChangeListener anAccountChangeListener) {
        onAccountChange = anAccountChangeListener;
    }

    public void setShowExternals(boolean b) {
        showExternals = b;
    }

}
