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
/**
 * This file contains relicensed code from som Apache copyright of 
 * Copyright (C) 2010, The Android Open Source Project
 */

package com.csipsimple.ui.calllog;


import android.database.Cursor;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;

/**
 * Groups together calls in the call log.
 * <p>
 * This class is meant to be used in conjunction with {@link GroupingListAdapter}.
 */
public class CallLogGroupBuilder {
    public interface GroupCreator {
        void addGroup(int cursorPosition, int size, boolean expanded);
    }


    /** The object on which the groups are created. */
    private final GroupCreator mGroupCreator;

    public CallLogGroupBuilder(GroupCreator groupCreator) {
        mGroupCreator = groupCreator;
    }

    /**
     * Finds all groups of adjacent entries in the call log which should be grouped together and
     * calls {@link CallLogListFragment.GroupCreator#addGroup(int, int, boolean)} on
     * {@link #mGroupCreator} for each of them.
     * <p>
     * For entries that are not grouped with others, we do not need to create a group of size one.
     * <p>
     * It assumes that the cursor will not change during its execution.
     *
     * @see GroupingListAdapter#addGroups(Cursor)
     */
    public void addGroups(Cursor cursor) {
        final int count = cursor.getCount();
        if (count == 0) {
            return;
        }
        int numberColIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
        int typeColIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);

        int currentGroupSize = 1;
        cursor.moveToFirst();
        // The number of the first entry in the group.
        String firstNumber = cursor.getString(numberColIndex);
        // This is the type of the first call in the group.
        int firstCallType = cursor.getInt(typeColIndex);
        while (cursor.moveToNext()) {
            // The number of the current row in the cursor.
            final String currentNumber = cursor.getString(numberColIndex);
            final int callType = cursor.getInt(typeColIndex);
            final boolean sameNumber = equalNumbers(firstNumber, currentNumber);
            final boolean shouldGroup;

            if (!sameNumber) {
                // Should only group with calls from the same number.
                shouldGroup = false;
            } else if ( firstCallType == Calls.MISSED_TYPE) {
                // Voicemail and missed calls should only be grouped with subsequent missed calls.
                shouldGroup = callType == Calls.MISSED_TYPE;
            } else {
                // Incoming and outgoing calls group together.
                shouldGroup = callType == Calls.INCOMING_TYPE || callType == Calls.OUTGOING_TYPE;
            }

            if (shouldGroup) {
                // Increment the size of the group to include the current call, but do not create
                // the group until we find a call that does not match.
                currentGroupSize++;
            } else {
                // Create a group for the previous set of calls, excluding the current one, but do
                // not create a group for a single call.
                if (currentGroupSize > 1) {
                    addGroup(cursor.getPosition() - currentGroupSize, currentGroupSize);
                }
                // Start a new group; it will include at least the current call.
                currentGroupSize = 1;
                // The current entry is now the first in the group.
                firstNumber = currentNumber;
                firstCallType = callType;
            }
        }
        // If the last set of calls at the end of the call log was itself a group, create it now.
        if (currentGroupSize > 1) {
            addGroup(count - currentGroupSize, currentGroupSize);
        }
    }

    /**
     * Creates a group of items in the cursor.
     * <p>
     * The group is always unexpanded.
     *
     * @see CallLogAdapter#addGroup(int, int, boolean)
     */
    private void addGroup(int cursorPosition, int size) {
        mGroupCreator.addGroup(cursorPosition, size, false);
    }

    
    private boolean equalNumbers(String number1, String number2) {
//        if (PhoneNumberUtils.isUriNumber(number1) || PhoneNumberUtils.isUriNumber(number2)) {
//            return compareSipAddresses(number1, number2);
//        } else {
        // Optim -- first try to compare very simply
        if(number1.equals(number2)) {
            return true;
        }
        return PhoneNumberUtils.compare(number1, number2);
//        }
    }
    /*
    boolean compareSipAddresses(String number1, String number2) {
        if (number1 == null || number2 == null) return number1 == number2;

        int index1 = number1.indexOf('@');
        final String userinfo1;
        final String rest1;
        if (index1 != -1) {
            userinfo1 = number1.substring(0, index1);
            rest1 = number1.substring(index1);
        } else {
            userinfo1 = number1;
            rest1 = "";
        }

        int index2 = number2.indexOf('@');
        final String userinfo2;
        final String rest2;
        if (index2 != -1) {
            userinfo2 = number2.substring(0, index2);
            rest2 = number2.substring(index2);
        } else {
            userinfo2 = number2;
            rest2 = "";
        }

        return userinfo1.equals(userinfo2) && rest1.equalsIgnoreCase(rest2);
    }
    */
}
