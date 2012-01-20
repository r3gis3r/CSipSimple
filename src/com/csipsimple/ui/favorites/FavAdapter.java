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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;

import com.csipsimple.R;
import com.csipsimple.utils.Log;

public class FavAdapter extends ResourceCursorAdapter {

    private static final String THIS_FILE = "FavAdapter";

    public FavAdapter(Context context, Cursor c) {
        super(context, R.layout.search_contact_list_item, c, 0);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ContentValues cv = new ContentValues();

        DatabaseUtils.cursorRowToContentValues(cursor, cv);

        Log.d(THIS_FILE, "Contents = " + cv);
    }

}
