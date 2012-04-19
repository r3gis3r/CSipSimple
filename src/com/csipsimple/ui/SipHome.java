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

package com.csipsimple.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.Toast;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.service.SipService;
import com.csipsimple.ui.account.AccountsEditList;
import com.csipsimple.ui.calllog.CallLogListFragment;
import com.csipsimple.ui.dialpad.DialerFragment;
import com.csipsimple.ui.favorites.FavListFragment;
import com.csipsimple.ui.help.Help;
import com.csipsimple.ui.messages.ConversationsListFragment;
import com.csipsimple.ui.prefs.MainPrefs;
import com.csipsimple.ui.prefs.PrefsFast;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.CustomDistribution;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.NightlyUpdater;
import com.csipsimple.utils.NightlyUpdater.UpdaterPopupLauncher;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.wizards.BasePrefsWizard;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

import java.util.ArrayList;
import java.util.List;

public class SipHome extends SherlockFragmentActivity {
    public static final int ACCOUNTS_MENU = Menu.FIRST + 1;
    public static final int PARAMS_MENU = Menu.FIRST + 2;
    public static final int CLOSE_MENU = Menu.FIRST + 3;
    public static final int HELP_MENU = Menu.FIRST + 4;
    public static final int DISTRIB_ACCOUNT_MENU = Menu.FIRST + 5;


    private static final String THIS_FILE = "SIP_HOME";

    private static final int TAB_INDEX_DIALER = 0;
    private static final int TAB_INDEX_CALL_LOG = 1;
    private static final int TAB_INDEX_FAVORITES = 2;
    private static final int TAB_INDEX_MESSAGES = 3;

    // protected static final int PICKUP_PHONE = 0;
    private static final int REQUEST_EDIT_DISTRIBUTION_ACCOUNT = 0; // PICKUP_PHONE
                                                                    // + 1;

    private Intent serviceIntent;

    //private PreferencesWrapper prefWrapper;
    private PreferencesProviderWrapper prefProviderWrapper;

    private boolean hasTriedOnceActivateAcc = false;
    // private ImageButton pickupContact;
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private boolean mDualPane;
    private Thread asyncSanityCheker;

    public final static boolean USE_LIGHT_THEME = false;

    /**
     * Listener interface for Fragments accommodated in {@link ViewPager}
     * enabling them to know when it becomes visible or invisible inside the
     * ViewPager.
     */
    public interface ViewPagerVisibilityListener {
        void onVisibilityChanged(boolean visible);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //prefWrapper = new PreferencesWrapper(this);
        prefProviderWrapper = new PreferencesProviderWrapper(this);

        /*
         * Resources r; try { r =
         * getPackageManager().getResourcesForApplication("com.etatgere"); int
         * rThemeId = r.getIdentifier("com.etatgere:style/LightTheme", null,
         * null); Log.e(THIS_FILE, "Remote theme " + rThemeId); Theme t =
         * r.newTheme(); t.applyStyle(rThemeId, false); //getTheme().setTo(t); }
         * catch (NameNotFoundException e) { Log.e(THIS_FILE,
         * "Not found app etatgere"); }
         */
        if (USE_LIGHT_THEME) {
            setTheme(R.style.LightTheme_noTopActionBar);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.sip_home);

        final ActionBar ab = getSupportActionBar();
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        // showAbTitle = Compatibility.hasPermanentMenuKey

        ab.setDisplayShowHomeEnabled(false);
        ab.setDisplayShowTitleEnabled(false);

        Tab dialerTab = ab.newTab()
                // .setText(R.string.dial_tab_name_text)
                .setIcon(R.drawable.ic_ab_dialer_holo_dark);
        Tab callLogTab = ab.newTab()
                // .setText(R.string.calllog_tab_name_text)
                .setIcon(R.drawable.ic_ab_history_holo_dark);

        Tab favoritesTab = ab.newTab()
                // .setText(R.string.messages_tab_name_text)
                .setIcon(R.drawable.ic_ab_favourites_holo_dark);

        Tab messagingTab = null;
        if (CustomDistribution.supportMessaging()) {
            messagingTab = ab.newTab()
                    // .setText(R.string.messages_tab_name_text)
                    .setIcon(R.drawable.ic_ab_text_holo_dark);
        }

        mDualPane = getResources().getBoolean(R.bool.use_dual_panes);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, getSupportActionBar(), mViewPager);
        mTabsAdapter.addTab(dialerTab, DialerFragment.class);
        mTabsAdapter.addTab(callLogTab, CallLogListFragment.class);
        mTabsAdapter.addTab(favoritesTab, FavListFragment.class);
        if (messagingTab != null) {
            mTabsAdapter.addTab(messagingTab, ConversationsListFragment.class);
        }

        hasTriedOnceActivateAcc = false;

        if (!prefProviderWrapper.getPreferenceBooleanValue(SipConfigManager.PREVENT_SCREEN_ROTATION)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        selectTabWithAction(getIntent());
        Log.setLogLevel(prefProviderWrapper.getLogLevel());
        

        // Async check
        asyncSanityCheker = new Thread() {
            public void run() {
                asyncSanityCheck();
            };
        };
        asyncSanityCheker.start();
    }

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost. It relies on a
     * trick. Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show. This is not sufficient for switching
     * between pages. So instead we make the content part of the tab host 0dp
     * high (it is not shown) and the TabsAdapter supplies its own dummy view to
     * show as the tab content. It listens to changes in tabs, and takes care of
     * switch to the correct paged in the ViewPager whenever the selected tab
     * changes.
     */
    private class TabsAdapter extends FragmentPagerAdapter implements
            ViewPager.OnPageChangeListener, ActionBar.TabListener {
        private final Context mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final List<String> mTabs = new ArrayList<String>();
        private boolean hasClearedDetails = false;

        private int mCurrentPosition = -1;
        /**
         * Used during page migration, to remember the next position
         * {@link #onPageSelected(int)} specified.
         */
        private int mNextPosition = -1;

        public TabsAdapter(FragmentActivity activity, ActionBar actionBar, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mActionBar = actionBar;
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss) {
            mTabs.add(clss.getName());
            mActionBar.addTab(tab.setTabListener(this));
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            return Fragment.instantiate(mContext, mTabs.get(position), null);
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            clearDetails();
            if (mViewPager.getCurrentItem() != tab.getPosition()) {
                mViewPager.setCurrentItem(tab.getPosition(), true);
            }
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);

            if (mCurrentPosition == position) {
                Log.w(THIS_FILE, "Previous position and next position became same (" + position
                        + ")");
            }

            mNextPosition = position;
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // Nothing to do
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            // Nothing to do
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Nothing to do
        }

        /*
         * public void setCurrentPosition(int position) { mCurrentPosition =
         * position; }
         */

        @Override
        public void onPageScrollStateChanged(int state) {
            switch (state) {
                case ViewPager.SCROLL_STATE_IDLE: {
                    if (mCurrentPosition >= 0) {
                        sendFragmentVisibilityChange(mCurrentPosition, false);
                    }
                    if (mNextPosition >= 0) {
                        sendFragmentVisibilityChange(mNextPosition, true);
                    }
                    invalidateOptionsMenu();

                    mCurrentPosition = mNextPosition;
                    break;
                }
                case ViewPager.SCROLL_STATE_DRAGGING:
                    clearDetails();
                    hasClearedDetails = true;
                    break;
                case ViewPager.SCROLL_STATE_SETTLING:
                    hasClearedDetails = false;
                    break;
                default:
                    break;
            }
        }

        private void clearDetails() {
            if (mDualPane && !hasClearedDetails) {
                FragmentTransaction ft = SipHome.this.getSupportFragmentManager()
                        .beginTransaction();
                ft.replace(R.id.details, new Fragment(), null);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        }
    }

    private DialerFragment mDialpadFragment;
    private CallLogListFragment mCallLogFragment;
    private ConversationsListFragment mMessagesFragment;
    private FavListFragment mPhoneFavoriteFragment;

    private Fragment getFragmentAt(int position) {
        switch (position) {
            case TAB_INDEX_DIALER:
                return mDialpadFragment;
            case TAB_INDEX_CALL_LOG:
                return mCallLogFragment;
            case TAB_INDEX_MESSAGES:
                return mMessagesFragment;
            case TAB_INDEX_FAVORITES:
                return mPhoneFavoriteFragment;
            default:
                throw new IllegalStateException("Unknown fragment index: " + position);
        }
    }

    public Fragment getCurrentFragment() {
        if (mViewPager != null) {
            return getFragmentAt(mViewPager.getCurrentItem());
        }
        return null;
    }

    private void sendFragmentVisibilityChange(int position, boolean visibility) {
        final Fragment fragment = getFragmentAt(position);
        if (fragment instanceof ViewPagerVisibilityListener) {
            ((ViewPagerVisibilityListener) fragment).onVisibilityChanged(visibility);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        // This method can be called before onCreate(), at which point we cannot
        // rely on ViewPager.
        // In that case, we will setup the "current position" soon after the
        // ViewPager is ready.
        final int currentPosition = mViewPager != null ? mViewPager.getCurrentItem() : -1;

        if (fragment instanceof DialerFragment) {
            mDialpadFragment = (DialerFragment) fragment;
            if (currentPosition == TAB_INDEX_DIALER) {
                mDialpadFragment.onVisibilityChanged(true);
            }
        } else if (fragment instanceof CallLogListFragment) {
            mCallLogFragment = (CallLogListFragment) fragment;
            if (currentPosition == TAB_INDEX_CALL_LOG) {
                mCallLogFragment.onVisibilityChanged(true);
            }
        } else if (fragment instanceof ConversationsListFragment) {
            mMessagesFragment = (ConversationsListFragment) fragment;
        } else if (fragment instanceof FavListFragment) {
            mPhoneFavoriteFragment = (FavListFragment) fragment;
        }

    }


    private void asyncSanityCheck() {
        // if(Compatibility.isCompatible(9)) {
        // // We check now if something is wrong with the gingerbread dialer
        // integration
        // Compatibility.getDialerIntegrationState(SipHome.this);
        // }

        PackageInfo pinfo = PreferencesProviderWrapper.getCurrentPackageInfos(this);
        if (pinfo != null) {
            if (pinfo.applicationInfo.icon == R.drawable.ic_launcher_nightly) {
                Log.d(THIS_FILE, "Sanity check : we have a nightly build here");
                ConnectivityManager connectivityService = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo ni = connectivityService.getActiveNetworkInfo();
                // Only do the process if we are on wifi
                if (ni != null && ni.isConnected() && ni.getType() == ConnectivityManager.TYPE_WIFI) {
                    // Only do the process if we didn't dismissed previously
                    NightlyUpdater nu = new NightlyUpdater(this);

                    if (!nu.ignoreCheckByUser()) {
                        long lastCheck = nu.lastCheck();
                        long current = System.currentTimeMillis();
                        long oneDay = 43200000; // 12 hours
                        if (current - oneDay > lastCheck) {
                            if (onForeground) {
                                // We have to check for an update
                                UpdaterPopupLauncher ru = nu.getUpdaterPopup(false);
                                if (ru != null && asyncSanityCheker != null) {
                                    runOnUiThread(ru);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Service monitoring stuff
    private void startSipService() {
        if (serviceIntent == null) {
            serviceIntent = new Intent(this, SipService.class);
        }
        Thread t = new Thread("StartSip") {
            public void run() {
                startService(serviceIntent);
                postStartSipService();
            };
        };
        t.start();

    }

    private void postStartSipService() {
        // If we have never set fast settings
        if (CustomDistribution.showFirstSettingScreen()) {
            if (!prefProviderWrapper.getPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP, false)) {
                Intent prefsIntent = new Intent(this, PrefsFast.class);
                prefsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(prefsIntent);
                return;
            }
        } else {
            boolean doFirstParams = !prefProviderWrapper.getPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP, false);
            prefProviderWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP, true);
            if (doFirstParams) {
                prefProviderWrapper.resetAllDefaultValues();
            }
        }

        // If we have no account yet, open account panel,
        if (!hasTriedOnceActivateAcc) {

            Cursor c = getContentResolver().query(SipProfile.ACCOUNT_URI, new String[] {
                    SipProfile.FIELD_ID
            }, null, null, null);
            int accountCount = 0;
            if (c != null) {
                accountCount = c.getCount();
            }
            c.close();

            if (accountCount == 0) {
                Intent accountIntent = null;
                WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();
                if (distribWizard != null) {
                    accountIntent = new Intent(this, BasePrefsWizard.class);
                    accountIntent.putExtra(SipProfile.FIELD_WIZARD, distribWizard.id);
                } else {
                    accountIntent = new Intent(this, AccountsEditList.class);
                }

                if (accountIntent != null) {
                    accountIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(accountIntent);
                    hasTriedOnceActivateAcc = true;
                    return;
                }
            }
            hasTriedOnceActivateAcc = true;
        }
    }

    private boolean onForeground = false;

    @Override
    protected void onPause() {
        Log.d(THIS_FILE, "On Pause SIPHOME");
        onForeground = false;
        if(asyncSanityCheker != null) {
            if(asyncSanityCheker.isAlive()) {
                asyncSanityCheker.interrupt();
                asyncSanityCheker = null;
            }
        }
        super.onPause();

    }

    @Override
    protected void onResume() {
        Log.d(THIS_FILE, "On Resume SIPHOME");
        super.onResume();
        onForeground = true;

        prefProviderWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_BEEN_QUIT, false);

        Log.d(THIS_FILE, "WE CAN NOW start SIP service");
        startSipService();

        // Set visible the currently selected account
        sendFragmentVisibilityChange(mViewPager.getCurrentItem(), true);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        selectTabWithAction(intent);
    }

    private void selectTabWithAction(Intent intent) {
        if (intent != null) {
            String callAction = intent.getAction();
            if (!TextUtils.isEmpty(callAction)) {
                ActionBar ab = getSupportActionBar();
                Tab toSelectTab = null;
                if (callAction.equalsIgnoreCase(SipManager.ACTION_SIP_DIALER)) {
                    toSelectTab = ab.getTabAt(TAB_INDEX_DIALER);
                } else if (callAction.equalsIgnoreCase(SipManager.ACTION_SIP_CALLLOG)) {
                    toSelectTab = ab.getTabAt(TAB_INDEX_CALL_LOG);
                } else if (callAction.equalsIgnoreCase(SipManager.ACTION_SIP_MESSAGES)) {
                    toSelectTab = ab.getTabAt(TAB_INDEX_MESSAGES);
                }
                if (toSelectTab != null) {
                    ab.selectTab(toSelectTab);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(THIS_FILE, "---DESTROY SIP HOME END---");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0
                && !Compatibility.isCompatible(5)) {
            onBackPressed();

        }
        return super.onKeyDown(keyCode, event);
    }

    public void onBackPressed() {
        if (prefProviderWrapper != null) {
            Log.d(THIS_FILE, "On back pressed ! ");
            // ArrayList<String> networks =
            // prefWrapper.getAllIncomingNetworks();
            // if (networks.size() == 0) {
            if (!prefProviderWrapper.isValidConnectionForIncoming()) {
                disconnectAndQuit();
                return;
            }
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO -- make sure we are not in split action bar a different way
        boolean showInActionBar = Compatibility.isCompatible(14)
                || Compatibility.isTabletScreen(this);
        int ifRoomIfSplit = showInActionBar ? MenuItem.SHOW_AS_ACTION_IF_ROOM
                : MenuItem.SHOW_AS_ACTION_NEVER;

        WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();
        if (distribWizard != null) {
            menu.add(Menu.NONE, DISTRIB_ACCOUNT_MENU, Menu.NONE, "My " + distribWizard.label)
                    .setIcon(distribWizard.icon)
                    .setShowAsAction(ifRoomIfSplit);
        }
        if (CustomDistribution.distributionWantsOtherAccounts()) {
            menu.add(Menu.NONE, ACCOUNTS_MENU, Menu.NONE,
                    (distribWizard == null) ? R.string.accounts : R.string.other_accounts)
                    .setIcon(R.drawable.ic_menu_account_list)
                    .setAlphabeticShortcut('a')
                    .setShowAsAction(ifRoomIfSplit | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
        menu.add(Menu.NONE, PARAMS_MENU, Menu.NONE, R.string.prefs)
                .setIcon(android.R.drawable.ic_menu_preferences)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        menu.add(Menu.NONE, HELP_MENU, Menu.NONE, R.string.help)
                .setIcon(android.R.drawable.ic_menu_help)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, CLOSE_MENU, Menu.NONE, R.string.menu_disconnect)
                .setIcon(R.drawable.ic_lock_power_off)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ACCOUNTS_MENU:
                startActivity(new Intent(this, AccountsEditList.class));
                return true;
            case PARAMS_MENU:
                startActivity(new Intent(this, MainPrefs.class));
                return true;
            case CLOSE_MENU:
                Log.d(THIS_FILE, "CLOSE");
                if (prefProviderWrapper.isValidConnectionForIncoming()) {
                    // Alert user that we will disable for all incoming calls as
                    // he want to quit
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.warning)
                            .setMessage(getString(R.string.disconnect_and_incoming_explaination))
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // prefWrapper.disableAllForIncoming();
                                    prefProviderWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_BEEN_QUIT, true);
                                    disconnectAndQuit();
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                } else {
                    ArrayList<String> networks = prefProviderWrapper.getAllIncomingNetworks();
                    if (networks.size() > 0) {
                        String msg = getString(R.string.disconnect_and_will_restart,
                                TextUtils.join(", ", networks));
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                    disconnectAndQuit();
                }
                return true;
            case HELP_MENU:
                // Create the fragment and show it as a dialog.
                DialogFragment newFragment = Help.newInstance();
                newFragment.show(getSupportFragmentManager(), "dialog");
                return true;
            case DISTRIB_ACCOUNT_MENU:
                WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();

                Cursor c = getContentResolver().query(SipProfile.ACCOUNT_URI, new String[] {
                        SipProfile.FIELD_ID
                }, SipProfile.FIELD_WIZARD + "=?", new String[] {
                        distribWizard.id
                }, null);

                Intent it = new Intent(this, BasePrefsWizard.class);
                it.putExtra(SipProfile.FIELD_WIZARD, distribWizard.id);
                Long accountId = null;
                if (c != null && c.getCount() > 0) {
                    try {
                        c.moveToFirst();
                        accountId = c.getLong(c.getColumnIndex(SipProfile.FIELD_ID));
                    } catch (Exception e) {
                        Log.e(THIS_FILE, "Error while getting wizard", e);
                    } finally {
                        c.close();
                    }
                }
                if (accountId != null) {
                    it.putExtra(SipProfile.FIELD_ID, accountId);
                }
                startActivityForResult(it, REQUEST_EDIT_DISTRIBUTION_ACCOUNT);

                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void disconnectAndQuit() {
        Log.d(THIS_FILE, "True disconnection...");
        if (serviceIntent != null) {
            // stopService(serviceIntent);
            // we don't not need anymore the currently started sip
            Intent it = new Intent(SipManager.ACTION_SIP_CAN_BE_STOPPED);
            sendBroadcast(it);
        }
        serviceIntent = null;
        finish();
    }

}
