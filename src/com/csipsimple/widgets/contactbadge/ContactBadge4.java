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

package com.csipsimple.widgets.contactbadge;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ContactBadge4 extends ContactBadgeContract {

    private OverlayedImageView imageView;

    public ContactBadge4(Context context, AttributeSet attrs, int defStyle,
            QuickContactBadge topBadge) {
        super(context, attrs, defStyle, topBadge);
        imageView = new OverlayedImageView(context, attrs, defStyle, topBadge);

    }

    @Override
    public ImageView getImageView() {
        return imageView;
    }

    @Override
    public void assignContactUri(Uri uri) {
        // Nothing to do
    }

}
