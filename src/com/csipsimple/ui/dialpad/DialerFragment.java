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

import android.app.AlertDialog;
import android.app.PendingIntent.CanceledException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.SupportActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.MenuItem.OnMenuItemClickListener;
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
import android.view.MenuInflater;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.csipsimple.R;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.service.OutgoingCall;
import com.csipsimple.ui.SipHome.ViewPagerVisibilityListener;
import com.csipsimple.utils.CallHandler;
import com.csipsimple.utils.CallHandler.onLoadListener;
import com.csipsimple.utils.Compatibility;
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

public class DialerFragment extends Fragment implements OnClickListener, OnLongClickListener,
        OnDialKeyListener, TextWatcher, OnDialActionListener, ViewPagerVisibilityListener, OnKeyListener {

    private static final String THIS_FILE = "DialerFragment";

    protected static final int PICKUP_PHONE = 0;

    private Drawable digitsBackground, digitsEmptyBackground;
    private DigitsEditText digits;
    private ImageButton switchTextView;

    private View digitDialer;

    private LinearLayout digitsWrapper;
    private AccountChooserButton accountChooserButton;
    private boolean isDigit/* , isTablet */;

    private DialingFeedback dialFeedback;

    private final int[] buttonsToAttach = new int[] {
            R.id.button0, R.id.switchTextView
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
    // private TextView domainTextHelper;

    /* private EditSipUri sipTextUri; */
    private AlertDialog missingVoicemailDialog;

    // Auto completion for text mode
    private ListView autoCompleteList;
    private ContactsSearchAdapter autoCompleteAdapter;

    private DialerCallBar callBar;
    private boolean mDualPane;

    private DialerAutocompleteDetailsFragment autoCompleteFragment;

    // private ImageButton backFlipTextDialerButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDualPane = getResources().getBoolean(R.bool.use_dual_panes);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.dialer_digit, container, false);
        // Store the backgrounds objects that will be in use later
        Resources r = getResources();

        digitsBackground = r.getDrawable(R.drawable.btn_dial_textfield_active);
        digitsEmptyBackground = r.getDrawable(R.drawable.btn_dial_textfield_normal);

        // Store some object that could be useful later
        digits = (DigitsEditText) v.findViewById(R.id.digitsText);
        digitsWrapper = (LinearLayout) v.findViewById(R.id.topField);
        dialPad = (Dialpad) v.findViewById(R.id.dialPad);
        callBar = (DialerCallBar) v.findViewById(R.id.dialerCallBar);
        autoCompleteList = (ListView) v.findViewById(R.id.autoCompleteList);
        accountChooserButton = (AccountChooserButton) v.findViewById(R.id.accountChooserButton);
        switchTextView = (ImageButton) v.findViewById(R.id.switchTextView);

        // isTablet = Compatibility.isTabletScreen(getActivity());

        // Digits field setup
        isDigit = prefsWrapper.startIsDigit();
        digits.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView tv, int action, KeyEvent arg2) {
                if (action == EditorInfo.IME_ACTION_GO) {
                    placeCall();
                    return true;
                }
                return false;
            }
        });

        // Account chooser button setup
        accountChooserButton.setShowExternals(true);
        accountChooserButton.setOnAccountChangeListener(new OnAccountChangeListener() {
            @Override
            public void onChooseAccount(SipProfile account) {
                long accId = SipProfile.INVALID_ID;
                if (account != null) {
                    accId = account.id;
                }
                autoCompleteAdapter.setSelectedAccount(accId);
            }
        });

        // Dialpad
        dialPad.setOnDialKeyListener(this);

        // Auto complete list in case of text
        autoCompleteAdapter = new ContactsSearchAdapter(getActivity());

        // We only need to add the autocomplete list if we
        DialerCallBar listCallBar = new DialerCallBar(getActivity(), null);
        listCallBar.setOnDialActionListener(this);
        autoCompleteList.addFooterView(listCallBar);
        autoCompleteList.setAdapter(autoCompleteAdapter);
        autoCompleteList.setOnItemClickListener(new OnAutoCompleteListItemClicked(
                autoCompleteAdapter));

        // Bottom bar setup
        callBar.setOnDialActionListener(this);

        // Ensure that current mode (text/digit) is applied
        setTextDialing(!isDigit);

        // Init other buttons
        initButtons(v);

        // Apply third party theme if any
        applyTheme();
        

        v.setOnKeyListener(this);

        return v;
    }

    private void applyTheme() {
        String theme = prefsWrapper.getPreferenceStringValue(SipConfigManager.THEME);
        if (!TextUtils.isEmpty(theme)) {
            new Theme(getActivity(), theme, new Theme.onLoadListener() {
                @Override
                public void onLoad(Theme t) {

                    dialPad.applyTheme(t);
                    // t.applyBackgroundDrawable(deleteButton,
                    // "btn_dial_delete");
                    // t.applyBackgroundDrawable(dialButton, "btn_dial_action");
                    t.applyBackgroundDrawable(getView().findViewById(R.id.vmButton),
                            "btn_dial_action_left_normal");

                    // Bg ... to be done
                    Drawable bg = t.getDrawableResource("dialpad_bg");
                    if (bg != null) {
                        if (bg instanceof BitmapDrawable) {
                            BitmapDrawable dbg = (BitmapDrawable) bg;
                            dbg.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                        }
                        digitDialer.setBackgroundDrawable(bg);
                    }

                    Drawable dAct = t.getDrawableResource("btn_dial_textfield_activated");
                    Drawable dEmpt = t.getDrawableResource("btn_dial_textfield_normal");
                    if (dAct != null && dEmpt != null) {
                        digitsBackground = dAct;
                        digitsEmptyBackground = dEmpt;
                        afterTextChanged(digits.getText());
                    }
                }
            });
        }
    }

    @Override
    public void onAttach(SupportActivity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(SipManager.INTENT_SIP_SERVICE), connection,
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

    private void attachButtonListener(View v, int id) {
        Log.d(THIS_FILE, "Attaching " + id);
        ImageButton button = (ImageButton) v.findViewById(id);
        button.setOnClickListener(this);

        if (id == R.id.button0 || id == R.id.deleteButton) {
            button.setOnLongClickListener(this);
        }
    }

    private void initButtons(View v) {
        for (int buttonId : buttonsToAttach) {
            attachButtonListener(v, buttonId);
        }

        digits.setOnClickListener(this);
        digits.setKeyListener(DialerKeyListener.getInstance());
        PhoneNumberFormattingTextWatcher digitFormater = new PhoneNumberFormattingTextWatcher();
        digits.addTextChangedListener(digitFormater);
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
        int view_id = view.getId();

        if (view_id == R.id.button0) {
            dialFeedback.giveFeedback(ToneGenerator.TONE_DTMF_0);
            keyPressed(KeyEvent.KEYCODE_0);
        } else if (view_id == R.id.switchTextView) {
            // Set as text dialing if we are currently digit dialing
            setTextDialing(isDigit);
        } else if (view_id == R.id.digitsText) {
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
        }
        return false;
    }

    public void afterTextChanged(Editable input) {
        // Change state of digit dialer
        final boolean notEmpty = digits.length() != 0;
        digitsWrapper.setBackgroundDrawable(notEmpty ? digitsBackground : digitsEmptyBackground);
        digitsWrapper.setPadding(0, 0, 0, 0);
        callBar.setEnabled(notEmpty);

        if (!notEmpty && isDigit) {
            digits.setCursorVisible(false);
        }

        // If single pane for smartphone use autocomplete list
        if (!isDigit && !mDualPane) {
            if (digits.length() >= 2) {
                autoCompleteAdapter.getFilter().filter(digits.getText().toString());
            } else {
                autoCompleteAdapter.swapCursor(null);
            }
        }
        // Dual pane : always use autocomplete list
        if (mDualPane && autoCompleteFragment != null) {
            autoCompleteFragment.filter(digits.getText().toString());
        }
    }

    /**
     * Set the mode of the text/digit input.
     * 
     * @param target Whether we should be in text mode instead of digit mode
     */
    private void setTextDialing(boolean target) {
        isDigit = !target;
        digits.setIsDigit(isDigit, true);
        digits.getText().clear();
        digits.setCursorVisible(!isDigit);
        dialPad.setVisibility(isDigit ? View.VISIBLE : View.GONE);

        callBar.setVisibility(isDigit ? View.VISIBLE : View.GONE);
        autoCompleteList.setVisibility(isDigit ? View.GONE : View.VISIBLE);
        switchTextView.setImageResource(isDigit ? R.drawable.ic_menu_switch_txt
                : R.drawable.ic_menu_switch_digit);

        // Invalidate to ask to require the text button to a digit button
        getSupportActivity().invalidateOptionsMenu();
    }

    /**
     * Set the value of the text field and put caret at the end
     * 
     * @param value the new text to see in the text field
     */
    public void setTextFieldValue(CharSequence value) {
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
        accountChooserButton.setChangeable(TextUtils.isEmpty(digits.getText().toString()));
    }

    // Options
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        boolean showInActionBar = Compatibility.isCompatible(14)
                || Compatibility.isTabletScreen(getActivity());

        int ifRoomIfSplit = showInActionBar ? MenuItem.SHOW_AS_ACTION_IF_ROOM
                : MenuItem.SHOW_AS_ACTION_NEVER;

        MenuItem delMenu = menu.add(isDigit ? R.string.switch_to_text : R.string.switch_to_digit);
        delMenu.setIcon(
                isDigit ? R.drawable.ic_menu_switch_txt
                        : R.drawable.ic_menu_switch_digit).setShowAsAction(ifRoomIfSplit);
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
        if (service == null) {
            return;
        }
        String toCall = "";
        Long accountToUse = SipProfile.INVALID_ID;

        if(isDigit) {
            toCall = PhoneNumberUtils.stripSeparators(digits.getText().toString());
        }else {
            toCall = "sip:" + digits.getText().toString();
        }
        SipProfile acc = accountChooserButton.getSelectedAccount();
        if (acc != null) {
            accountToUse = acc.id;
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
                service.makeCall(toCall, accountToUse.intValue());
            } catch (RemoteException e) {
                Log.e(THIS_FILE, "Service can't be called to make the call");
            }
        } else if (accountToUse != SipProfile.INVALID_ID) {
            // It's an external account, find correct external account
            CallHandler ch = new CallHandler(getActivity());
            ch.loadFrom(accountToUse, toCall, new onLoadListener() {
                @Override
                public void onLoad(CallHandler ch) {
                    placePluginCall(ch);
                }
            });
        }
    }

    @Override
    public void placeVMCall() {
        Long accountToUse = SipProfile.INVALID_ID;
        SipProfile acc = null;
        acc = accountChooserButton.getSelectedAccount();
        if (acc != null) {
            accountToUse = acc.id;
        }

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
        } else if (accountToUse == CallHandler.getAccountIdForCallHandler(getActivity(),
                "com.csipsimple/com.csipsimple.plugins.telephony.CallHandler")) {
            // Case gsm voice mail
            TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(
                    Context.TELEPHONY_SERVICE);
            String vmNumber = tm.getVoiceMailNumber();

            if (!TextUtils.isEmpty(vmNumber)) {
                OutgoingCall.ignoreNext = vmNumber;
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

    private void placePluginCall(CallHandler ch) {
        try {
            String nextExclude = ch.getNextExcludeTelNumber();
            if (nextExclude != null) {
                OutgoingCall.ignoreNext = nextExclude;
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
            ft.commit();
            // }
        }
    }

    @Override
    public boolean onKey(View arg0, int keyCode, KeyEvent arg2) {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        
        return digits.onKeyDown(keyCode, event);
    }

}
