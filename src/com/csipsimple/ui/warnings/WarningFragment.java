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

package com.csipsimple.ui.warnings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.actionbarsherlock.app.SherlockFragment;
import com.csipsimple.R;
import com.csipsimple.ui.warnings.WarningUtils.OnWarningChanged;
import com.csipsimple.ui.warnings.WarningUtils.WarningBlockView;
import com.csipsimple.utils.Log;

import java.util.ArrayList;
import java.util.List;


public class WarningFragment extends SherlockFragment implements OnWarningChanged {

    private static final String THIS_FILE = "WarningFragment";
    private List<String> warnList = new ArrayList<String>();
    private ViewGroup viewContainer = null;
    
    
    public void setWarningList(List<String> list) {
        warnList.clear();
        warnList.addAll(list);
        
        bindView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.warning_container, container,false);
        viewContainer = (ViewGroup) v.findViewById(R.id.container); 
        bindView();
        return v;
    }
    
    private void bindView() {
        if(viewContainer != null) {
            viewContainer.removeAllViews();
            for(String warn : warnList) {
                Log.d(THIS_FILE, "Add " + warn + " warning");
                WarningBlockView v = WarningUtils.getViewForWarning(getActivity(), warn);
                if(v != null) {
                    v.setOnWarnChangedListener(this);
                    v.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    viewContainer.addView(v);
                }
            }
        }
    }
    
    private OnWarningChanged onWChangedListener;

    @Override
    public void onWarningRemoved(String warnKey) {
        if(onWChangedListener != null) {
            onWChangedListener.onWarningRemoved(warnKey);
        }
    }
    /**
     * @param onWChangedListener the onWChangedListener to set
     */
    public void setOnWarningChangedListener(OnWarningChanged onWChangedListener) {
        this.onWChangedListener = onWChangedListener;
    }
}
