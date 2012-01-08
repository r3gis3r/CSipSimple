/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.csipsimple.ui.calllog;


import android.content.Context;
import android.provider.CallLog.Calls;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.csipsimple.R;

/**
 * Adapter for a ListView containing history items from the details of a call.
 */
public class CallDetailHistoryAdapter extends BaseAdapter {
    /** The top element is a blank header, which is hidden under the rest of the UI. */
    private static final int VIEW_TYPE_HEADER = 0;
    /** Each history item shows the detail of a call. */
    private static final int VIEW_TYPE_HISTORY_ITEM = 1;

    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final PhoneCallDetails[] mPhoneCallDetails;

    public CallDetailHistoryAdapter(Context context, LayoutInflater layoutInflater,
            PhoneCallDetails[] phoneCallDetails) {
        mContext = context;
        mLayoutInflater = layoutInflater;
        mPhoneCallDetails = phoneCallDetails;
    }

    @Override
    public int getCount() {
        return mPhoneCallDetails.length;
    }

    @Override
    public Object getItem(int position) {
        return mPhoneCallDetails[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        }
        return VIEW_TYPE_HISTORY_ITEM;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Make sure we have a valid convertView to start with
        final View result  = convertView == null
                ? mLayoutInflater.inflate(R.layout.call_detail_history_item, parent, false)
                : convertView;

        PhoneCallDetails details = (PhoneCallDetails) getItem(position);
        CallTypeIconsView callTypeIconView =
                (CallTypeIconsView) result.findViewById(R.id.call_type_icon);
        TextView callTypeTextView = (TextView) result.findViewById(R.id.call_type_text);
        TextView dateView = (TextView) result.findViewById(R.id.date);
        TextView durationView = (TextView) result.findViewById(R.id.duration);

        int callType = details.callTypes[0];
        callTypeIconView.clear();
        callTypeIconView.add(callType);
        
        StringBuilder typeSb = new StringBuilder();
        typeSb.append(mContext.getResources().getString(getCallTypeText(callType)));
        // If not 200, we add text for user feedback about what went wrong
        if(details.statusCode != 200) {
            typeSb.append(" - ");
            typeSb.append(details.statusCode);
            if(!TextUtils.isEmpty(details.statusText)) {
                typeSb.append(" / ");
                typeSb.append(details.statusText);
            }
        }
        callTypeTextView.setText(typeSb.toString());
        
        // Set the date.
        CharSequence dateValue = DateUtils.formatDateRange(mContext, details.date, details.date,
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE |
                DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR);
        dateView.setText(dateValue);
        // Set the duration
        if (callType == Calls.MISSED_TYPE) {
            durationView.setVisibility(View.GONE);
        } else {
            durationView.setVisibility(View.VISIBLE);
            durationView.setText(formatDuration(details.duration));
        }

        return result;
    }
    

    private int getCallTypeText(int callType) {
        switch (callType) {
            case Calls.INCOMING_TYPE:
                return R.string.type_incoming;
            case Calls.OUTGOING_TYPE:
                return R.string.type_outgoing;
            case Calls.MISSED_TYPE:
                return R.string.type_missed;
            default:
                throw new IllegalArgumentException("invalid call type: " + callType);
        }
    }

    private String formatDuration(long elapsedSeconds) {
        long minutes = 0;
        long seconds = 0;

        if (elapsedSeconds >= 60) {
            minutes = elapsedSeconds / 60;
            elapsedSeconds -= minutes * 60;
        }
        seconds = elapsedSeconds;

        return mContext.getString(R.string.callDetailsDurationFormat, minutes, seconds);
    }
}
