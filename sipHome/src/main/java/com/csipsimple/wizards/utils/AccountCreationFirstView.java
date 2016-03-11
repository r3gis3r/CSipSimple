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

package com.csipsimple.wizards.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;

import com.csipsimple.R;

public class AccountCreationFirstView extends RelativeLayout implements OnClickListener {

    private OnAccountCreationFirstViewListener mListener= null;

    public AccountCreationFirstView(Context context) {
        this(context, null);
    }

    public AccountCreationFirstView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.wizard_create_or_edit, this, true);

        bindElements();
    }
    
    private void bindElements() {
        findViewById(R.id.button0).setOnClickListener(this);
        findViewById(R.id.button1).setOnClickListener(this);
    }

    public AccountCreationFirstView(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs);
    }
    
    @Override
    public void onClick(View v) {
        if(mListener == null) {
            return;
        }
        int id = v.getId();
        if(id == R.id.button0) {
            mListener.onCreateAccountRequested();
        }else if(id == R.id.button1) {
            mListener.onEditAccountRequested();
        }
    }
    
    public void setOnAccountCreationFirstViewListener(OnAccountCreationFirstViewListener listener) {
        mListener = listener;
    }
    
    /**
     * Interface for listeners of {@link AccountCreationFirstView} 
     * see {@link AccountCreationFirstView#setOnAccountCreationFirstViewListener}
     */
    public interface OnAccountCreationFirstViewListener {
        /**
         * User asked to create the account
         */
        void onCreateAccountRequested();
        /**
         * User asked to edit : he has an existing account
         */
        void onEditAccountRequested();
    }

}
