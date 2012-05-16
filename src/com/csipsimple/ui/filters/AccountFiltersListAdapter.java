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

package com.csipsimple.ui.filters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.models.Filter;

public class AccountFiltersListAdapter extends ResourceCursorAdapter {

//    private static final String THIS_FILE = "AccEditListAd";

    public AccountFiltersListAdapter(Context context, Cursor c) {
        super(context, R.layout.filters_list_item, c, 0);
    }


    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Filter filter = new Filter();
        filter.createFromDb(cursor);
        String filterDesc = filter.getRepresentation(context);
        
        TextView tv = (TextView) view.findViewById(R.id.line1);
        ImageView icon = (ImageView) view.findViewById(R.id.action_icon);
        
        tv.setText(filterDesc);
        icon.setContentDescription(filterDesc);
        switch (filter.action) {
            case Filter.ACTION_CAN_CALL:
                icon.setImageResource(R.drawable.ic_menu_goto);
                break;
            case Filter.ACTION_CANT_CALL:
                icon.setImageResource(R.drawable.ic_menu_blocked_user);
                break;
            case Filter.ACTION_REPLACE:
                icon.setImageResource(android.R.drawable.ic_menu_edit);
                break;
            case Filter.ACTION_DIRECTLY_CALL:
                icon.setImageResource(R.drawable.ic_menu_answer_call);
                break;
            case Filter.ACTION_AUTO_ANSWER:
                icon.setImageResource(R.drawable.ic_menu_auto_answer);
                break;
            default:
                break;
        }
    }


}
