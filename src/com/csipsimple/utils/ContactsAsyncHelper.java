/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2008 The Android Open Source Project
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.csipsimple.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;

import com.csipsimple.models.CallerInfo;
import com.csipsimple.utils.contacts.ContactsWrapper;

public class ContactsAsyncHelper extends Handler {
    private static final String THIS_FILE = "ContactsAsyncHelper";
	/**
     * Interface for a WorkerHandler result return.
     */
    public interface OnImageLoadCompleteListener {
        /**
         * Called when the image load is complete.
         * 
         * @param imagePresent true if an image was found
         */  
        public void onImageLoadComplete(int token, Object cookie, ImageView iView,
                boolean imagePresent);
    }
    // constants
    private static final int EVENT_LOAD_IMAGE = 1;
    private static final int DEFAULT_TOKEN = -1;
    private static ContactsWrapper contactsWrapper;
    
    // static objects
    private static Handler sThreadHandler;
    
    private static final class WorkerArgs {
        public Context context;
        public ImageView view;
        public Uri uri;
        public int defaultResource;
        public Object result;
        public Object cookie;
        public OnImageLoadCompleteListener listener;
    }
    
    /**
     * Thread worker class that handles the task of opening the stream and loading 
     * the images.
     */
    private class WorkerHandler extends Handler {

		public WorkerHandler(Looper looper) {
            super(looper);
        }
		
		
        
		public void handleMessage(Message msg) {
            WorkerArgs args = (WorkerArgs) msg.obj;
            
            switch (msg.arg1) {
                case EVENT_LOAD_IMAGE:
                	Log.d(THIS_FILE, "get : "+args.uri.toString());
                	Bitmap img = contactsWrapper.getContactPhoto(args.context, args.uri, args.defaultResource);
                    if (img != null) {
                        args.result = img;

                        Log.d(THIS_FILE, "Loading image: " + msg.arg1 +
                                " token: " + msg.what + " image URI: " + args.uri);
                    } else {
                        args.result = null;
                        Log.d(THIS_FILE, "Problem with image: " + msg.arg1 + 
                                " token: " + msg.what + " image URI: " + args.uri + 
                                ", using default image.");
                    }
                    break;
                default:
            }
            
            // send the reply to the enclosing class. 
            Message reply = ContactsAsyncHelper.this.obtainMessage(msg.what);
            reply.arg1 = msg.arg1;
            reply.obj = msg.obj;
            reply.sendToTarget();
        }
    }
    
    /**
     * Private constructor for static class
     */
    private ContactsAsyncHelper() {
    	Log.d(THIS_FILE, "Self creation");
        HandlerThread thread = new HandlerThread("ContactsAsyncWorker");
        thread.start();
        sThreadHandler = new WorkerHandler(thread.getLooper());
        contactsWrapper = ContactsWrapper.getInstance();
    }
    
    /**
     * Convenience method for calls that do not want to deal with listeners and tokens.
     */
    public static final void updateImageViewWithContactPhotoAsync(Context context, 
            ImageView imageView, CallerInfo person, int placeholderImageResource) {
        // Added additional Cookie field in the callee.
        updateImageViewWithContactPhotoAsync (DEFAULT_TOKEN, null, null, context, 
                imageView, person, placeholderImageResource);
    }

    
    /**
     * Start an image load, attach the result to the specified CallerInfo object.
     * Note, when the query is started, we make the ImageView INVISIBLE if the
     * placeholderImageResource value is -1.  When we're given a valid (!= -1)
     * placeholderImageResource value, we make sure the image is visible.
     */
    public static final void updateImageViewWithContactPhotoAsync(int token, 
            OnImageLoadCompleteListener listener, Object cookie, Context context, 
            ImageView imageView, CallerInfo callerInfo, int placeholderImageResource) {
        if(sThreadHandler == null) {
        	Log.d(THIS_FILE, "Update image view with contact async");
        	new ContactsAsyncHelper();
        }
    	
        // in case the source caller info is null, the URI will be null as well.
        // just update using the placeholder image in this case.
        if (callerInfo == null) {
            Log.d(THIS_FILE, "No CallerInfo, just display placeholder.");
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(placeholderImageResource);
            return;
        }
        
        // Added additional Cookie field in the callee to handle arguments
        // sent to the callback function.
        
        // setup arguments
        WorkerArgs args = new WorkerArgs();
        args.cookie = cookie;
        args.context = context;
        args.view = imageView;
        args.uri = callerInfo.contactContentUri;
        args.defaultResource = placeholderImageResource;
        args.listener = listener;
        
        // setup message arguments
        Message msg = sThreadHandler.obtainMessage(token);
        msg.arg1 = EVENT_LOAD_IMAGE;
        msg.obj = args;
        
        Log.d(THIS_FILE, "Begin loading image: " + args.uri + 
                ", displaying default image for now.");
        
        // set the default image first, when the query is complete, we will
        // replace the image with the correct one.
        if (placeholderImageResource != -1) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(placeholderImageResource);
        } else {
            imageView.setVisibility(View.INVISIBLE);
        }
        
        // notify the thread to begin working
        sThreadHandler.sendMessage(msg);
    }
    
    /**
     * Called when loading is done.
     */
    @Override
    public void handleMessage(Message msg) {
        WorkerArgs args = (WorkerArgs) msg.obj;
        switch (msg.arg1) {
            case EVENT_LOAD_IMAGE:
                boolean imagePresent = false;

                // if the image has been loaded then display it, otherwise set default.
                // in either case, make sure the image is visible.
                if (args.result != null) {
                    args.view.setVisibility(View.VISIBLE);
                    args.view.setImageBitmap((Bitmap) args.result);
                    imagePresent = true;
                } else if (args.defaultResource != -1) {
                    args.view.setVisibility(View.VISIBLE);
                    args.view.setImageResource(args.defaultResource);
                }
                
                
                // notify the listener if it is there.
                if (args.listener != null) {
                    Log.d(THIS_FILE, "Notifying listener: " + args.listener.toString() + 
                            " image: " + args.uri + " completed");
                    args.listener.onImageLoadComplete(msg.what, args.cookie, args.view,
                            imagePresent);
                }
                break;
            default:    
        }
    }
    
}
