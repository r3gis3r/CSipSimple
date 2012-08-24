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

package com.csipsimple.ui.calllog;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipUri;
import com.csipsimple.models.CallerInfo;
import com.csipsimple.ui.SipHome;
import com.csipsimple.utils.ContactsAsyncHelper;

/**
 * Adapter class to fill in data for the Call Log.
 */
public class CallLogAdapter extends GroupingListAdapter
        implements CallLogGroupBuilder.GroupCreator {
    /** Interface used to initiate a refresh of the content. */
    public interface CallFetcher {
        void fetchCalls();
    }

    private final Context mContext;
    private final CallFetcher mCallFetcher;


    protected static final String THIS_FILE = "CallLogAdapter";

    /** Instance of helper class for managing views. */
    private final CallLogListItemHelper mCallLogViewsHelper;

    /** Helper to set up contact photos. */
    // private final ContactPhotoManager mContactPhotoManager;
    /** Helper to group call log entries. */
    private final CallLogGroupBuilder mCallLogGroupBuilder;

    private OnCallLogAction callLogActionListener = null;

    public void setOnCallLogActionListener(OnCallLogAction l) {
        callLogActionListener = l;
    }

    public interface OnCallLogAction {
        void viewDetails(int position, long[] callIds);

        void placeCall(String number, Long accId);
    }

    private class CallRowInfos {
        long[] callIds;
        int position;
        String number;
        Long accId;
    }

    /** Listener for the primary action in the list, opens the call details. */
    private final View.OnClickListener mPrimaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            CallRowInfos cri = (CallRowInfos) view.getTag();
            if (callLogActionListener != null) {
                callLogActionListener.viewDetails(cri.position, cri.callIds);
            }
        }
    };
    /** Listener for the secondary action in the list, either call or play. */
    private final View.OnClickListener mSecondaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            CallRowInfos cri = (CallRowInfos) view.getTag();
            if (callLogActionListener != null) {
                callLogActionListener.placeCall(cri.number, cri.accId);
            }
        }
    };

    public CallLogAdapter(Context context, CallFetcher callFetcher) {
        super(context);

        mContext = context;
        mCallFetcher = callFetcher;

        Resources resources = mContext.getResources();

        // mContactPhotoManager = ContactPhotoManager.getInstance(mContext);
        PhoneCallDetailsHelper phoneCallDetailsHelper = new PhoneCallDetailsHelper(resources);
        mCallLogViewsHelper = new CallLogListItemHelper(phoneCallDetailsHelper, context);
        mCallLogGroupBuilder = new CallLogGroupBuilder(this);
    }

    /**
     * Requery on background thread when {@link Cursor} changes.
     */
    @Override
    protected void onContentChanged() {
        mCallFetcher.fetchCalls();
    }

    @Override
    protected void addGroups(Cursor cursor) {
        mCallLogGroupBuilder.addGroups(cursor);
    }

    @Override
    public View newStandAloneView(Context context, ViewGroup parent) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.call_log_list_item, parent, false);
        findAndCacheViews(view);
        return view;
    }

    @Override
    public void bindStandAloneView(int position, View view, Context context, Cursor cursor) {
        bindView(position, view, cursor, 1);
    }

    @Override
    public View newChildView(Context context, ViewGroup parent) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.call_log_list_item, parent, false);
        findAndCacheViews(view);
        return view;
    }

    @Override
    public void bindChildView(int position, View view, Context context, Cursor cursor) {
        bindView(position, view, cursor, 1);
    }

    @Override
    public View newGroupView(Context context, ViewGroup parent) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.call_log_list_item, parent, false);
        findAndCacheViews(view);
        return view;
    }

    @Override
    public void bindGroupView(int position, View view, Context context, Cursor cursor,
            int groupSize,
            boolean expanded) {
        bindView(position, view, cursor, groupSize);
    }

    private void findAndCacheViews(View view) {
        // Get the views to bind to.
        CallLogListItemViews views = CallLogListItemViews.fromView(view);
        views.primaryActionView.setLongClickable(true);
        views.primaryActionView.setOnClickListener(mPrimaryActionListener);
        views.secondaryActionView.setOnClickListener(mSecondaryActionListener);
        view.setTag(views);
    }

    /**
     * Binds the views in the entry to the data in the call log.
     * 
     * @param view the view corresponding to this entry
     * @param c the cursor pointing to the entry in the call log
     * @param count the number of entries in the current item, greater than 1 if
     *            it is a group
     */
    private void bindView(int position, View view, Cursor c, int count) {
        final CallLogListItemViews views = (CallLogListItemViews) view.getTag();

        // Default case: an item in the call log.
        views.primaryActionView.setVisibility(View.VISIBLE);
        views.bottomDivider.setVisibility(isLastOfSection(c) ? View.GONE : View.VISIBLE);

        int numberColIndex = c.getColumnIndex(CallLog.Calls.NUMBER);
        int dateColIndex = c.getColumnIndex(CallLog.Calls.DATE);
        int durationColIndex = c.getColumnIndex(CallLog.Calls.DURATION);
        int accIdIndex = c.getColumnIndex(SipManager.CALLLOG_PROFILE_ID_FIELD);
        // int typeColIndex = c.getColumnIndex(CallLog.Calls.TYPE);

        String number = c.getString(numberColIndex);
        final long date = c.getLong(dateColIndex);
        final long duration = c.getLong(durationColIndex);
        final Long accId = c.getLong(accIdIndex);
        // final int callType = c.getInt(typeColIndex);

        // final CallerInfo cachedContactInfo = getContactInfoFromCallLog(c);

        CallRowInfos cri = new CallRowInfos();
        cri.callIds = getCallIds(c, count);
        cri.position = position;
        cri.number = number;
        cri.accId = accId;
        views.primaryActionView.setTag(cri);
        views.secondaryActionView.setTag(cri);
        views.secondaryActionView.setVisibility(TextUtils.isEmpty(number) ? View.GONE
                : View.VISIBLE);

        String cachedNumber = (String) view.getTag(R.id.number);
        if (cachedNumber != null && cachedNumber.equals(number)) {
            // No need to go set details about the contact again this has
            // already been done
            return;
        }
        CallerInfo info = CallerInfo.getCallerInfoFromSipUri(mContext, number);

        final Uri lookupUri = info.contactContentUri;
        final String name = info.name;
        final int ntype = info.numberType;
        final String label = info.phoneLabel;
        CharSequence formattedNumber = SipUri.getCanonicalSipContact(number, false);
        final int[] callTypes = getCallTypes(c, count);
        PhoneCallDetails details;
        if (TextUtils.isEmpty(name)) {
            details = new PhoneCallDetails(number, formattedNumber,
                    callTypes, date, duration);
        } else {
            // We do not pass a photo id since we do not need the high-res
            // picture.
            details = new PhoneCallDetails(number, formattedNumber,
                    callTypes, date, duration, name, ntype, label, lookupUri, null);
        }

        mCallLogViewsHelper.setPhoneCallDetails(views, details);
        setPhoto(views, info);
    }

    /** Returns true if this is the last item of a section. */
    private boolean isLastOfSection(Cursor c) {
        if (c.isLast()) {
            return true;
        }
        return false;
    }

    /**
     * Returns the call types for the given number of items in the cursor.
     * <p>
     * It uses the next {@code count} rows in the cursor to extract the types.
     * <p>
     * It position in the cursor is unchanged by this function.
     */
    private int[] getCallTypes(Cursor cursor, int count) {
        int position = cursor.getPosition();
        int[] callTypes = new int[count];
        for (int index = 0; index < count; ++index) {
            callTypes[index] = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
            cursor.moveToNext();
        }
        cursor.moveToPosition(position);
        return callTypes;
    }

    /**
     * Retrieve call ids list of the item at a given position
     * 
     * @param position the position to look at
     * @return the list of call ids
     */
    public long[] getCallIdsAtPosition(int position) {
        Cursor item = (Cursor) getItem(position);
        int count = getGroupSize(position);
        return getCallIds(item, count);
    }

    /**
     * Returns the call ids for the given number of items in the cursor.
     * <p>
     * It uses the next {@code count} rows in the cursor to extract the types.
     * <p>
     * It position in the cursor is unchanged by this function.
     */
    private long[] getCallIds(Cursor cursor, int count) {
        int position = cursor.getPosition();
        long[] callIds = new long[count];
        for (int index = 0; index < count; ++index) {
            callIds[index] = cursor.getLong(cursor.getColumnIndex(CallLog.Calls._ID));
            cursor.moveToNext();
        }
        cursor.moveToPosition(position);
        return callIds;
    }
    
    /**
     * Retrieve the remote sip uri for a call log at the given position
     * @param position  the position to look at
     * @return the sip uri
     */
    public String getCallRemoteAtPostion(int position) {
        Cursor item = (Cursor) getItem(position);
        if(item != null) {
            String number = item.getString(item.getColumnIndex(CallLog.Calls.NUMBER));
            return SipUri.getCanonicalSipContact(number, false);
        }
        return "";
    }

    private void setPhoto(CallLogListItemViews views, CallerInfo ci) {

        views.quickContactView.assignContactUri(ci.contactContentUri);
        ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(mContext, views.quickContactView
                .getImageView(),
                ci,
                SipHome.USE_LIGHT_THEME ? R.drawable.ic_contact_picture_holo_light
                        : R.drawable.ic_contact_picture_holo_dark);
    }

    @Override
    public void addGroup(int cursorPosition, int size, boolean expanded) {
        super.addGroup(cursorPosition, size, expanded);
    }

}
