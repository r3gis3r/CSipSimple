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

package com.csipsimple.ui.dialpad;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent.CanceledException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.FragmentTransaction;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.csipsimple.R;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.models.Filter;
import com.csipsimple.ui.SipHome.ViewPagerVisibilityListener;
import com.csipsimple.ui.dialpad.DialerLayout.OnAutoCompleteListVisibilityChangedListener;
import com.csipsimple.utils.CallHandlerPlugin;
import com.csipsimple.utils.CallHandlerPlugin.OnLoadListener;
import com.csipsimple.utils.DialingFeedback;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.utils.Theme;
import com.csipsimple.utils.contacts.ContactsSearchAdapter;
import com.csipsimple.widgets.AccountChooserButton;
import com.csipsimple.widgets.AccountChooserButton.OnAccountChangeListener;
import com.csipsimple.widgets.DialerCallBar;
import com.csipsimple.widgets.DialerCallBar.OnDialActionListener;
import com.csipsimple.widgets.Dialpad;
import com.csipsimple.widgets.Dialpad.OnDialKeyListener;

public class DialerFragment extends SherlockFragment implements OnClickListener, OnLongClickListener,
        OnDialKeyListener, TextWatcher, OnDialActionListener, ViewPagerVisibilityListener, OnKeyListener,
        OnAutoCompleteListVisibilityChangedListener {

    private static final String THIS_FILE = "DialerFragment";

    protected static final int PICKUP_PHONE = 0;

    //private Drawable digitsBackground, digitsEmptyBackground;
    private DigitsEditText digits;
    private String initText = null;
    //private ImageButton switchTextView;

    //private View digitDialer;

    private AccountChooserButton accountChooserButton;
    private Boolean isDigit = null;
    /* , isTablet */
    
    private DialingFeedback dialFeedback;

    /*
    private final int[] buttonsToAttach = new int[] {
            R.id.switchTextView
    };
    */
    private final int[] buttonsToLongAttach = new int[] {
            R.id.button0, R.id.button1
    };

    // TimingLogger timings = new TimingLogger("SIP_HOME", "test");

    private ISipService service;
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            service = ISipService.Stub.asInterface(arg1);
            /*
             * timings.addSplit("Service connected"); if(configurationService !=
             * null) { timings.dumpToLog(); }
             */
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            service = null;
        }
    };

    // private GestureDetector gestureDetector;
    private Dialpad dialPad;

    private PreferencesWrapper prefsWrapper;
    private AlertDialog missingVoicemailDialog;

    // Auto completion for text mode
    private ListView autoCompleteList;
    private ContactsSearchAdapter autoCompleteAdapter;

    private DialerCallBar callBar;
    private boolean mDualPane;

    private DialerAutocompleteDetailsFragment autoCompleteFragment;
    private PhoneNumberFormattingTextWatcher digitFormater;
    private OnAutoCompleteListItemClicked autoCompleteListItemListener;

    private DialerLayout dialerLayout;

    private MenuItem accountChooserFilterItem;

    private TextView rewriteTextInfo;

	@Override
	public void onAutoCompleteListVisibiltyChanged() {
        applyTextToAutoComplete();
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDualPane = getResources().getBoolean(R.bool.use_dual_panes);
        digitFormater = new PhoneNumberFormattingTextWatcher();
        // Auto complete list in case of text
        autoCompleteAdapter = new ContactsSearchAdapter(getActivity());
        autoCompleteListItemListener = new OnAutoCompleteListItemClicked(autoCompleteAdapter);

        if(isDigit == null) {
            isDigit = !prefsWrapper.getPreferenceBooleanValue(SipConfigManager.START_WITH_TEXT_DIALER);
        }
        
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.dialer_digit, container, false);
        // Store the backgrounds objects that will be in use later
        /*
        Resources r = getResources();
        
        digitsBackground = r.getDrawable(R.drawable.btn_dial_textfield_active);
        digitsEmptyBackground = r.getDrawable(R.drawable.btn_dial_textfield_normal);
        */

        // Store some object that could be useful later
        digits = (DigitsEditText) v.findViewById(R.id.digitsText);
        dialPad = (Dialpad) v.findViewById(R.id.dialPad);
        callBar = (DialerCallBar) v.findViewById(R.id.dialerCallBar);
        autoCompleteList = (ListView) v.findViewById(R.id.autoCompleteList);
        rewriteTextInfo = (TextView) v.findViewById(R.id.rewriteTextInfo);
        
        accountChooserButton = (AccountChooserButton) v.findViewById(R.id.accountChooserButton);
        
        accountChooserFilterItem = accountChooserButton.addExtraMenuItem(R.string.apply_rewrite);
        accountChooserFilterItem.setCheckable(true);
        accountChooserFilterItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                setRewritingFeature(!accountChooserFilterItem.isChecked());
                return true;
            }
        });
        setRewritingFeature(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.REWRITE_RULES_DIALER));
        
        dialerLayout = (DialerLayout) v.findViewById(R.id.top_digit_dialer);
        //switchTextView = (ImageButton) v.findViewById(R.id.switchTextView);

        // isTablet = Compatibility.isTabletScreen(getActivity());

        // Digits field setup
        if(savedInstanceState != null) {
            isDigit = savedInstanceState.getBoolean(TEXT_MODE_KEY, isDigit);
        }
        
        digits.setOnEditorActionListener(keyboardActionListener);
        
        // Layout 
        dialerLayout.setForceNoList(mDualPane);
        dialerLayout.setAutoCompleteListVisibiltyChangedListener(this);

        // Account chooser button setup
        accountChooserButton.setShowExternals(true);
        accountChooserButton.setOnAccountChangeListener(accountButtonChangeListener);

        // Dialpad
        dialPad.setOnDialKeyListener(this);

        // We only need to add the autocomplete list if we
        autoCompleteList.setAdapter(autoCompleteAdapter);
        autoCompleteList.setOnItemClickListener(autoCompleteListItemListener);
        autoCompleteList.setFastScrollEnabled(true);

        // Bottom bar setup
        callBar.setOnDialActionListener(this);
        callBar.setVideoEnabled(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_VIDEO));

        //switchTextView.setVisibility(Compatibility.isCompatible(11) ? View.GONE : View.VISIBLE);

        // Init other buttons
        initButtons(v);
        // Ensure that current mode (text/digit) is applied
        setTextDialing(!isDigit, true);
        if(initText != null) {
            digits.setText(initText);
            initText = null;
        }

        // Apply third party theme if any
        applyTheme(v);
        v.setOnKeyListener(this);
        applyTextToAutoComplete();
        return v;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if(callBar != null) {
            callBar.setVideoEnabled(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_VIDEO));
        }
    }

    private void applyTheme(View v) {
        Theme t = Theme.getCurrentTheme(getActivity());
        if (t != null) {
            dialPad.applyTheme(t);
            
            View subV;
            // Delete button
            subV = v.findViewById(R.id.deleteButton);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "btn_dial_delete");
                t.applyLayoutMargin(subV, "btn_dial_delete_margin");
                t.applyImageDrawable((ImageView) subV, "ic_dial_action_delete");
            }
            
            // Dial button
            subV = v.findViewById(R.id.dialButton);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "btn_dial_action");
                t.applyLayoutMargin(subV, "btn_dial_action_margin");
                t.applyImageDrawable((ImageView) subV, "ic_dial_action_call");
            }
            
            // Additional button
            subV = v.findViewById(R.id.dialVideoButton);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "btn_add_action");
                t.applyLayoutMargin(subV, "btn_dial_add_margin");
            }
            
            // Action dividers
            subV = v.findViewById(R.id.divider1);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "btn_bar_divider");
                t.applyLayoutSize(subV, "btn_dial_divider");
            }
            subV = v.findViewById(R.id.divider2);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "btn_bar_divider");
                t.applyLayoutSize(subV, "btn_dial_divider");
            }
            
            // Dialpad background
            subV = v.findViewById(R.id.dialPad);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "dialpad_background");
            }
            
            // Callbar background
            subV = v.findViewById(R.id.dialerCallBar);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "dialer_callbar_background");
            }
            
            // Top field background
            subV = v.findViewById(R.id.topField);
            if(subV != null) {
                t.applyBackgroundDrawable(subV, "dialer_textfield_background");
            }
            
            subV = v.findViewById(R.id.digitsText);
            if(subV != null) {
                t.applyTextColor((TextView) subV, "textColorPrimary");
            }
            
        }
        
        // Fix dialer background
        if(callBar != null) {
            Theme.fixRepeatableBackground(callBar);
        }
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Intent serviceIntent = new Intent(SipManager.INTENT_SIP_SERVICE);
        // Optional, but here we bundle so just ensure we are using csipsimple package
        serviceIntent.setPackage(activity.getPackageName());
        getActivity().bindService(serviceIntent, connection,
                Context.BIND_AUTO_CREATE);
        // timings.addSplit("Bind asked for two");
        if (prefsWrapper == null) {
            prefsWrapper = new PreferencesWrapper(getActivity());
        }
        if (dialFeedback == null) {
            dialFeedback = new DialingFeedback(getActivity(), false);
        }

        dialFeedback.resume();
        
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(connection);
        } catch (Exception e) {
            // Just ignore that
            Log.w(THIS_FILE, "Unable to un bind", e);
        }
        dialFeedback.pause();
        super.onDetach();
    }
    
    
    private final static String TEXT_MODE_KEY = "text_mode";

	private static final String TAG = null;
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(TEXT_MODE_KEY, isDigit);
        super.onSaveInstanceState(outState);
    }
    
    private OnEditorActionListener keyboardActionListener = new OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView tv, int action, KeyEvent arg2) {
            if (action == EditorInfo.IME_ACTION_GO) {
                placeCall();
                return true;
            }
            return false;
        }
    };
    
    OnAccountChangeListener accountButtonChangeListener = new OnAccountChangeListener() {
        @Override
        public void onChooseAccount(SipProfile account) {
            long accId = SipProfile.INVALID_ID;
            if (account != null) {
                accId = account.id;
            }
            autoCompleteAdapter.setSelectedAccount(accId);
            applyRewritingInfo();
        }
    };
    
    private void attachButtonListener(View v, int id, boolean longAttach) {
        ImageButton button = (ImageButton) v.findViewById(id);
        if(button == null) {
            Log.w(THIS_FILE, "Not found button " + id);
            return;
        }
        if(longAttach) {
            button.setOnLongClickListener(this);
        }else {
            button.setOnClickListener(this);
        }
    }

    private void initButtons(View v) {
        /*
        for (int buttonId : buttonsToAttach) {
            attachButtonListener(v, buttonId, false);
        }
        */
        for (int buttonId : buttonsToLongAttach) {
            attachButtonListener(v, buttonId, true);
        }

        digits.setOnClickListener(this);
        digits.setKeyListener(DialerKeyListener.getInstance());
        digits.addTextChangedListener(this);
        digits.setCursorVisible(false);
        afterTextChanged(digits.getText());
    }

    
    
    private void keyPressed(int keyCode) {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        digits.onKeyDown(keyCode, event);
    }

    private class OnAutoCompleteListItemClicked implements OnItemClickListener {
        private ContactsSearchAdapter searchAdapter;

        /**
         * Instanciate with a ContactsSearchAdapter adapter to search in when a
         * contact entry is clicked
         * 
         * @param adapter the adapter to use
         */
        public OnAutoCompleteListItemClicked(ContactsSearchAdapter adapter) {
            searchAdapter = adapter;
        }

        @Override
        public void onItemClick(AdapterView<?> list, View v, int position, long id) {
            Object selectedItem = searchAdapter.getItem(position);
            if (selectedItem != null) {
                CharSequence newValue = searchAdapter.getFilter().convertResultToString(
                        selectedItem);
                setTextFieldValue(newValue);
            }
        }

    }

    public void onClick(View view) {
        // ImageButton b = null;
        int viewId = view.getId();
        /*
        if (view_id == R.id.switchTextView) {
            // Set as text dialing if we are currently digit dialing
            setTextDialing(isDigit);
        } else */
        if (viewId == digits.getId()) {
            if (digits.length() != 0) {
                digits.setCursorVisible(true);
            }
        }
    }

    public boolean onLongClick(View view) {
        // ImageButton b = (ImageButton)view;
        int vId = view.getId();
        if (vId == R.id.button0) {
            dialFeedback.hapticFeedback();
            keyPressed(KeyEvent.KEYCODE_PLUS);
            return true;
        }else if(vId == R.id.button1) {
            if(digits.length() == 0) {
                placeVMCall();
                return true;
            }
        }
        return false;
    }

    public void afterTextChanged(Editable input) {
        // Change state of digit dialer
        final boolean notEmpty = digits.length() != 0;
        //digitsWrapper.setBackgroundDrawable(notEmpty ? digitsBackground : digitsEmptyBackground);
        callBar.setEnabled(notEmpty);

        if (!notEmpty && isDigit) {
            digits.setCursorVisible(false);
        }
        applyTextToAutoComplete();
    }
    
    private void applyTextToAutoComplete() {

        // If single pane for smartphone use autocomplete list
        if (hasAutocompleteList()) {
            String filter = digits.getText().toString();
            autoCompleteAdapter.setSelectedText(filter);
            //else {
            //    autoCompleteAdapter.swapCursor(null);
            //}
        }
        // Dual pane : always use autocomplete list
        if (mDualPane && autoCompleteFragment != null) {
            autoCompleteFragment.filter(digits.getText().toString());
        }
    }

    /**
     * Set the mode of the text/digit input.
     * 
     * @param textMode True if text mode. False if digit mode
     */
    public void setTextDialing(boolean textMode) {
        Log.d(THIS_FILE, "Switch to mode " + textMode);
        setTextDialing(textMode, false);
    }
    

    /**
     * Set the mode of the text/digit input.
     * 
     * @param textMode True if text mode. False if digit mode
     */
    public void setTextDialing(boolean textMode, boolean forceRefresh) {
        if(!forceRefresh && (isDigit != null && isDigit == !textMode)) {
            // Nothing to do
            return;
        }
        isDigit = !textMode;
        if(digits == null) {
            return;
        }
        if(isDigit) {
            // We need to clear the field because the formatter will now 
            // apply and unapply to this field which could lead to wrong values when unapplied
            digits.getText().clear();
            digits.addTextChangedListener(digitFormater);
        }else {
            digits.removeTextChangedListener(digitFormater);
        }
        digits.setCursorVisible(!isDigit);
        digits.setIsDigit(isDigit, true);
        
        // Update views visibility
        dialPad.setVisibility(isDigit ? View.VISIBLE : View.GONE);
        autoCompleteList.setVisibility(hasAutocompleteList() ? View.VISIBLE : View.GONE);
        //switchTextView.setImageResource(isDigit ? R.drawable.ic_menu_switch_txt
        //        : R.drawable.ic_menu_switch_digit);

        // Invalidate to ask to require the text button to a digit button
        getSherlockActivity().supportInvalidateOptionsMenu();
    }
    
    private boolean hasAutocompleteList() {
        if(!isDigit) {
            return true;
        }
        return dialerLayout.canShowList();
    }

    /**
     * Set the value of the text field and put caret at the end
     * 
     * @param value the new text to see in the text field
     */
    public void setTextFieldValue(CharSequence value) {
        if(digits == null) {
            initText = value.toString();
            return;
        }
        digits.setText(value);
        // make sure we keep the caret at the end of the text view
        Editable spannable = digits.getText();
        Selection.setSelection(spannable, spannable.length());
    }

    // @Override
    public void onTrigger(int keyCode, int dialTone) {
        dialFeedback.giveFeedback(dialTone);
        keyPressed(keyCode);
    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        // Nothing to do here
    }

    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        afterTextChanged(digits.getText());
        String newText = digits.getText().toString();
        // Allow account chooser button to automatically change again as we have clear field
        accountChooserButton.setChangeable(TextUtils.isEmpty(newText));
        applyRewritingInfo();
    }

    // Options
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        int action = getResources().getBoolean(R.bool.menu_in_bar) ? MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_NEVER;
        MenuItem delMenu = menu.add(isDigit ? R.string.switch_to_text : R.string.switch_to_digit);
        delMenu.setIcon(
                isDigit ? R.drawable.ic_menu_switch_txt
                        : R.drawable.ic_menu_switch_digit).setShowAsAction( action );
        delMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                setTextDialing(isDigit);
                return true;
            }
        });
    }

    @Override
    public void placeCall() {
        placeCallWithOption(null);
    }

    @Override
    public void placeVideoCall() {
        Bundle b = new Bundle();
        b.putBoolean(SipCallSession.OPT_CALL_VIDEO, true);
        placeCallWithOption(b );
    }
    
    private void placeCallWithOption(Bundle b) {
    	Log.i(TAG, "placeCallWithOption");
        if (service == null) {
            return;
        }
        String toCall = "";
        Long accountToUse = SipProfile.INVALID_ID;
        // Find account to use
        SipProfile acc = accountChooserButton.getSelectedAccount();
        
        if(acc == null) {
        	Log.e(TAG, "account null!");
            return;
        }

        System.out.println("toString: " + acc.toString());
        
        accountToUse = acc.id;
        // Find number to dial
        toCall = digits.getText().toString();
        if(isDigit) {
            toCall = PhoneNumberUtils.stripSeparators(toCall);
        }

        if(accountChooserFilterItem != null && accountChooserFilterItem.isChecked()) {
            toCall = rewriteNumber(toCall);
        }
        
        if (TextUtils.isEmpty(toCall)) {
            return;
        }

        // Well we have now the fields, clear theses fields
        digits.getText().clear();

        // -- MAKE THE CALL --//
        if (accountToUse >= 0) {
            // It is a SIP account, try to call service for that
            try {
                service.makeCallWithOptions(toCall, accountToUse.intValue(), b);
            } catch (RemoteException e) {
                Log.e(THIS_FILE, "Service can't be called to make the call");
            }
        } else if (accountToUse != SipProfile.INVALID_ID) {
            // It's an external account, find correct external account
            CallHandlerPlugin ch = new CallHandlerPlugin(getActivity());
            ch.loadFrom(accountToUse, toCall, new OnLoadListener() {
                @Override
                public void onLoad(CallHandlerPlugin ch) {
                    placePluginCall(ch);
                }
            });
        }
    }
    
    public void placeVMCall() {
        Long accountToUse = SipProfile.INVALID_ID;
        SipProfile acc = null;
        acc = accountChooserButton.getSelectedAccount();
        if (acc == null) {
            // Maybe we could inform user nothing will happen here?
            return;
        }
        
        accountToUse = acc.id;

        if (accountToUse >= 0) {
            SipProfile vmAcc = SipProfile.getProfileFromDbId(getActivity(), acc.id, new String[] {
                    SipProfile.FIELD_VOICE_MAIL_NBR
            });
            if (!TextUtils.isEmpty(vmAcc.vm_nbr)) {
                // Account already have a VM number
                try {
                    service.makeCall(vmAcc.vm_nbr, (int) acc.id);
                } catch (RemoteException e) {
                    Log.e(THIS_FILE, "Service can't be called to make the call");
                }
            } else {
                // Account has no VM number, propose to create one
                final long editedAccId = acc.id;
                LayoutInflater factory = LayoutInflater.from(getActivity());
                final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);

                missingVoicemailDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(acc.display_name)
                        .setView(textEntryView)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                if (missingVoicemailDialog != null) {
                                    TextView tf = (TextView) missingVoicemailDialog
                                            .findViewById(R.id.vmfield);
                                    if (tf != null) {
                                        String vmNumber = tf.getText().toString();
                                        if (!TextUtils.isEmpty(vmNumber)) {
                                            ContentValues cv = new ContentValues();
                                            cv.put(SipProfile.FIELD_VOICE_MAIL_NBR, vmNumber);

                                            int updated = getActivity().getContentResolver()
                                                    .update(ContentUris.withAppendedId(
                                                            SipProfile.ACCOUNT_ID_URI_BASE,
                                                            editedAccId),
                                                            cv, null, null);
                                            Log.d(THIS_FILE, "Updated accounts " + updated);
                                        }
                                    }
                                    missingVoicemailDialog.hide();
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (missingVoicemailDialog != null) {
                                    missingVoicemailDialog.hide();
                                }
                            }
                        })
                        .create();

                // When the dialog is up, completely hide the in-call UI
                // underneath (which is in a partially-constructed state).
                missingVoicemailDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                missingVoicemailDialog.show();
            }
        } else if (accountToUse == CallHandlerPlugin.getAccountIdForCallHandler(getActivity(),
                (new ComponentName(getActivity(), com.csipsimple.plugins.telephony.CallHandler.class).flattenToString()))) {
            // Case gsm voice mail
            TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(
                    Context.TELEPHONY_SERVICE);
            String vmNumber = tm.getVoiceMailNumber();

            if (!TextUtils.isEmpty(vmNumber)) {
                if(service != null) {
                    try {
                        service.ignoreNextOutgoingCallFor(vmNumber);
                    } catch (RemoteException e) {
                        Log.e(THIS_FILE, "Not possible to ignore next");
                    }
                }
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", vmNumber, null));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {

                missingVoicemailDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.gsm)
                        .setMessage(R.string.no_voice_mail_configured)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (missingVoicemailDialog != null) {
                                    missingVoicemailDialog.hide();
                                }
                            }
                        })
                        .create();

                // When the dialog is up, completely hide the in-call UI
                // underneath (which is in a partially-constructed state).
                missingVoicemailDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                missingVoicemailDialog.show();
            }
        }
        // TODO : manage others ?... for now, no way to do so cause no vm stored
    }

    private void placePluginCall(CallHandlerPlugin ch) {
        try {
            String nextExclude = ch.getNextExcludeTelNumber();
            if (service != null && nextExclude != null) {
                try {
                    service.ignoreNextOutgoingCallFor(nextExclude);
                } catch (RemoteException e) {
                    Log.e(THIS_FILE, "Impossible to ignore next outgoing call", e);
                }
            }
            ch.getIntent().send();
        } catch (CanceledException e) {
            Log.e(THIS_FILE, "Pending intent cancelled", e);
        }
    }

    @Override
    public void deleteChar() {
        keyPressed(KeyEvent.KEYCODE_DEL);
    }

    @Override
    public void deleteAll() {
        digits.getText().clear();
    }

    private final static String TAG_AUTOCOMPLETE_SIDE_FRAG = "autocomplete_dial_side_frag";

    @Override
    public void onVisibilityChanged(boolean visible) {
        if (visible && getResources().getBoolean(R.bool.use_dual_panes)) {
            // That's far to be optimal we should consider uncomment tests for reusing fragment
            // if (autoCompleteFragment == null) {
            autoCompleteFragment = new DialerAutocompleteDetailsFragment();

            if (digits != null) {
                Bundle bundle = new Bundle();
                bundle.putCharSequence(DialerAutocompleteDetailsFragment.EXTRA_FILTER_CONSTRAINT,
                        digits.getText().toString());

                autoCompleteFragment.setArguments(bundle);

            }
            // }
            // if
            // (getFragmentManager().findFragmentByTag(TAG_AUTOCOMPLETE_SIDE_FRAG)
            // != autoCompleteFragment) {
            // Execute a transaction, replacing any existing fragment
            // with this one inside the frame.
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.details, autoCompleteFragment, TAG_AUTOCOMPLETE_SIDE_FRAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commitAllowingStateLoss();
        
            // }
        }
    }

    @Override
    public boolean onKey(View arg0, int keyCode, KeyEvent arg2) {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        
        return digits.onKeyDown(keyCode, event);
    }

    // In dialer rewriting feature
    
    private void setRewritingFeature(boolean active) {
        accountChooserFilterItem.setChecked(active);
        rewriteTextInfo.setVisibility(active?View.VISIBLE:View.GONE);
        if(active) {
             applyRewritingInfo();
        }
        prefsWrapper.setPreferenceBooleanValue(SipConfigManager.REWRITE_RULES_DIALER, active);
    }
    
    private String rewriteNumber(String number) {
        SipProfile acc = accountChooserButton.getSelectedAccount();
        if (acc == null) {
            return number;
        }
        String numberRewrite = Filter.rewritePhoneNumber(getActivity(), acc.id, number);
        if(TextUtils.isEmpty(numberRewrite)) {
            return "";
        }
        ParsedSipContactInfos finalCallee = acc.formatCalleeNumber(numberRewrite);
        if(!TextUtils.isEmpty(finalCallee.displayName)) {
            return finalCallee.toString();
        }
        return finalCallee.getReadableSipUri();
    }
    
    private void applyRewritingInfo() {
        // Rewrite information textView update
        String newText = digits.getText().toString();
        if(accountChooserFilterItem != null && accountChooserFilterItem.isChecked()) {
            if(isDigit) {
                newText = PhoneNumberUtils.stripSeparators(newText);
            }
            rewriteTextInfo.setText(rewriteNumber(newText));
        }else {
            rewriteTextInfo.setText("");
        }
    }
}
