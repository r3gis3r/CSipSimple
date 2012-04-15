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

package com.csipsimple.ui.prefs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItem;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.widgets.DragnDropListView;
import com.csipsimple.widgets.DragnDropListView.DropListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodecsFragment extends ListFragment implements OnCheckedChangeListener {

    protected static final String THIS_FILE = "CodecsFragment";

    private static final String CODEC_NAME = "codec_name";
    private static final String CODEC_ID = "codec_id";
    private static final String CODEC_PRIORITY = "codec_priority";
    
    public static final String BAND_TYPE = "band_type";
    public static final String MEDIA_TYPE = "media_type";
    public static final int MEDIA_AUDIO = 0;
    public static final int MEDIA_VIDEO = 1;
    public static final int MENU_ITEM_ACTIVATE = Menu.FIRST + 1;

    private SimpleAdapter mAdapter;
    private List<Map<String, Object>> codecsList;

    private PreferencesWrapper prefsWrapper;
    
    // Type for codec bandwidth
    private String bandtype;
    // Type for media (audio/video)
    private Integer mediatype;

    private Boolean useCodecsPerSpeed = true;
    

    private static final Map<String, String> NON_FREE_CODECS = new HashMap<String, String>();

    static {
        NON_FREE_CODECS.put("G729/8000/1", "http://www.synapseglobal.com/g729_codec_license.html");
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        prefsWrapper = new PreferencesWrapper(getActivity());
        useCodecsPerSpeed  = SipConfigManager.getPreferenceBooleanValue(getActivity(), SipConfigManager.CODECS_PER_BANDWIDTH);
        initDatas();
        setHasOptionsMenu(true);

        
        
        // Adapter
        mAdapter = new SimpleAdapter(getActivity(), codecsList, R.layout.codecs_list_item, new String[] {
                CODEC_NAME,
                CODEC_NAME,
                CODEC_PRIORITY
        }, new int[] {
                R.id.line1,
                R.id.AccCheckBoxActive,
                R.id.entiere_line
        });

        mAdapter.setViewBinder(new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view.getId() == R.id.entiere_line) {
                    Log.d(THIS_FILE, "Entiere line is binded ");
                    TextView tv = (TextView) view.findViewById(R.id.line1);
                    ImageView grabber = (ImageView) view.findViewById(R.id.icon);
                    CompoundButton checker = (CompoundButton) view.findViewById(R.id.AccCheckBoxActive);
                    checker.setOnCheckedChangeListener(CodecsFragment.this);
                    if ((Short) data == 0) {
                        tv.setTextColor(Color.GRAY);
                        grabber.setVisibility(View.GONE);
                        checker.setChecked(false);
                    } else {
                        tv.setTextColor(Color.WHITE);
                        grabber.setVisibility(View.VISIBLE);
                        checker.setChecked(true);
                    }
                    return true;
                }else if(view.getId() == R.id.AccCheckBoxActive) {
                    view.setTag(data);
                    return true;
                }
                return false;
            }

        });

        setListAdapter(mAdapter);
        registerForContextMenu(getListView());
    }
    
    /**
     * Initialize datas list
     */
    private void initDatas() {
        if(codecsList == null) {
            codecsList = new ArrayList<Map<String, Object>>();
        }else {
            codecsList.clear();
        }
        

        bandtype = (String) getArguments().get(BAND_TYPE);
        mediatype = (Integer) getArguments().get(MEDIA_TYPE);
        
        String[] codecNames;
        if(mediatype == MEDIA_AUDIO) {
            codecNames = prefsWrapper.getCodecList();
        }else {
            codecNames = prefsWrapper.getVideoCodecList();
        }
        
        int current_prio = 130;
        for(String codecName : codecNames) {
            Log.d(THIS_FILE, "Fill codec "+codecName+" for "+bandtype);
            String[] codecParts = codecName.split("/");
            if(codecParts.length >=2 ) {
                HashMap<String, Object> codecInfo = new HashMap<String, Object>();
                codecInfo.put(CODEC_ID, codecName);
                if(mediatype == MEDIA_AUDIO) {
                    codecInfo.put(CODEC_NAME, codecParts[0]+" "+codecParts[1].substring(0, codecParts[1].length()-3)+" kHz");
                }else if(mediatype == MEDIA_VIDEO) {
                    codecInfo.put(CODEC_NAME, codecParts[0]);
                }
                codecInfo.put(CODEC_PRIORITY, prefsWrapper.getCodecPriority(codecName, bandtype, Integer.toString(current_prio)));
                codecsList.add(codecInfo);
                current_prio --;
                Log.d(THIS_FILE, "Found priority is "+codecInfo.get(CODEC_PRIORITY));
            }
            
        }
        
        Collections.sort(codecsList, codecsComparator);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.codecs_list, container, false);

        DragnDropListView listView = (DragnDropListView) v.findViewById(android.R.id.list);
        listView.setOnDropListener(new DropListener() {
            @Override
            public void drop(int from, int to) {
                
                @SuppressWarnings("unchecked")
                HashMap<String, Object> item = (HashMap<String, Object>) getListAdapter().getItem(from);
                
                Log.d(THIS_FILE, "Dropped "+item.get(CODEC_NAME)+" -> "+to);
                
                //Prevent disabled codecsList to be reordered
                if((Short) item.get(CODEC_PRIORITY) <= 0 ) {
                    return ;
                }
                
                codecsList.remove(from);
                codecsList.add(to, item);
                
                //Update priorities
                short currentPriority = 130;
                for(Map<String, Object> codec : codecsList) {
                    if((Short) codec.get(CODEC_PRIORITY) > 0) {
                        if(currentPriority != (Short) codec.get(CODEC_PRIORITY)) {
                            setCodecPriority((String)codec.get(CODEC_ID), currentPriority);
                            codec.put(CODEC_PRIORITY, currentPriority);
                        }
                        //Log.d(THIS_FILE, "Reorder : "+codec.toString());
                        currentPriority --;
                    }
                }
                
                
                //Log.d(THIS_FILE, "Data set "+codecsList.toString());
                mAdapter.notifyDataSetChanged();
            }
        });
        
        listView.setOnCreateContextMenuListener(this);
        return v;
    }
    
    private void setCodecPriority(String codecName, short priority) {
        if(useCodecsPerSpeed) {
            prefsWrapper.setCodecPriority(codecName, bandtype, Short.toString(priority));
        }else {
            prefsWrapper.setCodecPriority(codecName, SipConfigManager.CODEC_NB, Short.toString(priority));
            prefsWrapper.setCodecPriority(codecName, SipConfigManager.CODEC_WB, Short.toString(priority));
        }
    }
    

    @Override
    @SuppressWarnings("unchecked")
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
            return;
        }

        HashMap<String, Object> codec = (HashMap<String, Object>) mAdapter.getItem(info.position);
        if (codec == null) {
            // If for some reason the requested item isn't available, do nothing
            return;
        }
        
        boolean isDisabled = ((Short)codec.get(CODEC_PRIORITY) == 0);
        menu.add(0, MENU_ITEM_ACTIVATE, 0, isDisabled ? R.string.activate : R.string.deactivate);
        
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
            return false;
        }
        
        HashMap<String, Object> codec = null;
        codec = (HashMap<String, Object>) mAdapter.getItem(info.position);
        
        if (codec == null) {
            // If for some reason the requested item isn't available, do nothing
            return false;
        }
        int selId = item.getItemId();
        if (selId == MENU_ITEM_ACTIVATE) {
            boolean isDisabled = ((Short) codec.get(CODEC_PRIORITY) == 0);
            userActivateCodec(codec, isDisabled);
            return true;
        }
        return false;
    }
    
    private void userActivateCodec(final Map<String, Object> codec, boolean activate) {
        
        String codecName = (String) codec.get(CODEC_ID);
        final short newPrio = activate ? (short) 1 : (short) 0;
        
        if(NON_FREE_CODECS.containsKey(codecName) && activate) {

            final TextView message = new TextView(getActivity());
            final SpannableString s = new SpannableString(getString(R.string.this_codec_is_not_free) + NON_FREE_CODECS.get(codecName));
            Linkify.addLinks(s, Linkify.WEB_URLS);
            message.setText(s);
            message.setMovementMethod(LinkMovementMethod.getInstance());
            message.setPadding(10, 10, 10, 10);
              
            
            //Alert user that we will disable for all incoming calls as he want to quit
            new AlertDialog.Builder(getActivity())
                .setTitle(R.string.warning)
                .setView(message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        setCodecActivated(codec, newPrio);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        }else {
            setCodecActivated(codec, newPrio);
        }
    }
    
    /**
     * Internal method to activate codec priority
     * @param codec the codec to activate
     * @param newPrio the new priority of the codec (0 to disable)
     */
    private void setCodecActivated(Map<String, Object> codec, short newPrio) {
        setCodecPriority((String) codec.get(CODEC_ID), newPrio);
        codec.put(CODEC_PRIORITY, newPrio);
        Collections.sort(codecsList, codecsComparator);
        mAdapter.notifyDataSetChanged();
    }


    /**
     * Class to compare the codecs based on their priority
     */
    private final Comparator<Map<String, Object>> codecsComparator = new Comparator<Map<String, Object>>() {
        @Override
        public int compare(Map<String, Object> infos1, Map<String, Object> infos2) {
            if (infos1 != null && infos2 != null) {
                short c1 = (Short)infos1.get(CODEC_PRIORITY);
                short c2 = (Short)infos2.get(CODEC_PRIORITY);
                if (c1 > c2) {
                    return -1;
                }
                if (c1 < c2) {
                    return 1;
                }
            }

            return 0;
        }
    };


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        String codecName = (String) buttonView.getTag();
        if(codecName != null) {
            HashMap<String, Object> codec = null;
            for( int i = 0; i < mAdapter.getCount(); i++) {
                @SuppressWarnings("unchecked")
                HashMap<String, Object> tCodec = (HashMap<String, Object>) mAdapter.getItem(i);
                if(codecName.equalsIgnoreCase( (String) tCodec.get(CODEC_NAME))) {
                    codec = tCodec;
                    break;
                }
            }
            if(codec != null) {
                userActivateCodec(codec, isChecked);
            }
        }
    }
}
