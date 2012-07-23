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
 * This file contains relicensed code from Apache copyright of 
 * Copyright (C) 2008 The Android Open Source Project
 */

package com.csipsimple.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.csipsimple.R;
import com.csipsimple.models.CallerInfo;
import com.csipsimple.utils.contacts.ContactsWrapper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ContactsAsyncHelper extends Handler {
    private static final String THIS_FILE = "ContactsAsyncHelper";
    
    // TODO : use LRUCache for bitmaps.
    
    LruCache<Uri, Bitmap> photoCache = new LruCache<Uri, Bitmap>(5 * 1024 * 1024 /* 5MiB */) {
        protected int sizeOf(Uri key, Bitmap value) {
            return value.getRowBytes() * value.getWidth();
        }
    };

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
    private static final int EVENT_LOAD_IMAGE_URI = 2;
    private static final int DEFAULT_TOKEN = -1;
    private static final int TAG_PHOTO_INFOS = R.id.icon;
    private static ContactsWrapper contactsWrapper;

    // static objects
    private static Handler sThreadHandler;

    private static final class WorkerArgs {
        public Context context;
        public ImageView view;
        public int defaultResource;
        public Object result;
        public Uri loadedUri;
        public Object cookie;
        public OnImageLoadCompleteListener listener;
    }

    private static class PhotoViewTag {
        public Uri uri;
    }

    public static final String HIGH_RES_URI_PARAM = "hiRes";
    /**
     * Thread worker class that handles the task of opening the stream and
     * loading the images.
     */
    private class WorkerHandler extends Handler {

        public WorkerHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            WorkerArgs args = (WorkerArgs) msg.obj;
            Uri uri = null;
            if (msg.arg1 == EVENT_LOAD_IMAGE) {
                PhotoViewTag photoTag = (PhotoViewTag) args.view.getTag(TAG_PHOTO_INFOS);
                if (photoTag != null && photoTag.uri != null) {
                    uri = photoTag.uri;
                    boolean hiRes = false;
                    String p = uri.getQueryParameter(HIGH_RES_URI_PARAM);
                    if(!TextUtils.isEmpty(p) && p.equalsIgnoreCase("1")) {
                        hiRes = true;
                    }
                    Log.v(THIS_FILE, "get : " + uri);
                    Bitmap img = null;
                    synchronized (photoCache) {
                        img = photoCache.get(uri);
                    }
                    if(img == null) {
                        img = contactsWrapper.getContactPhoto(args.context, uri, hiRes,
                                args.defaultResource);
                        synchronized (photoCache) {
                            photoCache.put(uri, img);
                        }
                    }
                    if (img != null) {
                        args.result = img;
                    } else {
                        args.result = null;
                    }
                }
            } else if (msg.arg1 == EVENT_LOAD_IMAGE_URI) {
                PhotoViewTag photoTag = (PhotoViewTag) args.view.getTag(TAG_PHOTO_INFOS);
                if (photoTag != null && photoTag.uri != null) {
                    uri = photoTag.uri;
                    Log.v(THIS_FILE, "get : " + uri);

                    Bitmap img = null;

                    synchronized (photoCache) {
                        img = photoCache.get(uri);
                    }
                    if(img == null) {
                        byte[] buffer = new byte[1024 * 16];
                        try {
                            InputStream is = args.context.getContentResolver().openInputStream(uri);
                            if (is != null) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                try {
                                    int size;
                                    while ((size = is.read(buffer)) != -1) {
                                        baos.write(buffer, 0, size);
                                    }
                                } finally {
                                    is.close();
                                }
                                byte[] boasBytes = baos.toByteArray();
                                img = BitmapFactory.decodeByteArray(boasBytes, 0, boasBytes.length,
                                        null);
                            }
                        } catch (Exception ex) {
                            Log.v(THIS_FILE, "Cannot load photo " + uri, ex);
                        }
                    }
                    
                    if (img != null) {
                        args.result = img;
                        synchronized (photoCache) {
                            photoCache.put(uri, img);
                        }
                    } else {
                        args.result = null;
                    }
                    
                }
            }
            args.loadedUri = uri;

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
        HandlerThread thread = new HandlerThread("ContactsAsyncWorker");
        thread.start();
        sThreadHandler = new WorkerHandler(thread.getLooper());
        contactsWrapper = ContactsWrapper.getInstance();
    }

    /**
     * Convenience method for calls that do not want to deal with listeners and
     * tokens.
     */
    public static final void updateImageViewWithContactPhotoAsync(Context context,
            ImageView imageView, CallerInfo person, int placeholderImageResource) {
        // Added additional Cookie field in the callee.
        updateImageViewWithContactPhotoAsync(DEFAULT_TOKEN, null, null, context,
                imageView, person, placeholderImageResource);
    }

    /**
     * Start an image load, attach the result to the specified CallerInfo
     * object. Note, when the query is started, we make the ImageView INVISIBLE
     * if the placeholderImageResource value is -1. When we're given a valid (!=
     * -1) placeholderImageResource value, we make sure the image is visible.
     */
    public static final void updateImageViewWithContactPhotoAsync(int token,
            OnImageLoadCompleteListener listener, Object cookie, Context context,
            ImageView imageView, CallerInfo callerInfo, int placeholderImageResource) {
        if (sThreadHandler == null) {
            new ContactsAsyncHelper();
        }

        // in case the source caller info is null, the URI will be null as well.
        // just update using the placeholder image in this case.
        if (callerInfo == null || callerInfo.contactContentUri == null) {
            defaultImage(imageView, placeholderImageResource);
            return;
        }

        // Check that the view is not already loading for same uri
        if (isAlreadyProcessed(imageView, callerInfo.contactContentUri)) {
            return;
        }

        // Added additional Cookie field in the callee to handle arguments
        // sent to the callback function.

        // setup arguments
        WorkerArgs args = new WorkerArgs();
        args.cookie = cookie;
        args.context = context;
        args.view = imageView;
        PhotoViewTag photoTag = new PhotoViewTag();
        photoTag.uri = callerInfo.contactContentUri;
        args.view.setTag(TAG_PHOTO_INFOS, photoTag);
        args.defaultResource = placeholderImageResource;
        args.listener = listener;

        // setup message arguments
        Message msg = sThreadHandler.obtainMessage(token);
        msg.arg1 = EVENT_LOAD_IMAGE;
        msg.obj = args;

        preloadImage(imageView, placeholderImageResource, msg);
    }

    public static void updateImageViewWithContactPhotoAsync(Context context, ImageView imageView,
            Uri photoUri, int placeholderImageResource) {
        if (sThreadHandler == null) {
            Log.v(THIS_FILE, "Update image view with contact async");
            new ContactsAsyncHelper();
        }

        // in case the source caller info is null, the URI will be null as well.
        // just update using the placeholder image in this case.
        if (photoUri == null) {
            defaultImage(imageView, placeholderImageResource);
            return;
        }
        if (isAlreadyProcessed(imageView, photoUri)) {
            return;
        }

        // Added additional Cookie field in the callee to handle arguments
        // sent to the callback function.

        // setup arguments
        WorkerArgs args = new WorkerArgs();
        args.context = context;
        args.view = imageView;
        PhotoViewTag photoTag = new PhotoViewTag();
        photoTag.uri = photoUri;
        args.view.setTag(TAG_PHOTO_INFOS, photoTag);
        args.defaultResource = placeholderImageResource;

        // setup message arguments
        Message msg = sThreadHandler.obtainMessage();
        msg.arg1 = EVENT_LOAD_IMAGE_URI;
        msg.obj = args;

        preloadImage(imageView, placeholderImageResource, msg);
    }

    private static void defaultImage(ImageView imageView, int placeholderImageResource) {
        Log.v(THIS_FILE, "No uri, just display placeholder.");
        PhotoViewTag photoTag = new PhotoViewTag();
        photoTag.uri = null;
        imageView.setTag(TAG_PHOTO_INFOS, photoTag);
        imageView.setVisibility(View.VISIBLE);
        imageView.setImageResource(placeholderImageResource);
    }

    private static void preloadImage(ImageView imageView, int placeholderImageResource, Message msg) {
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

    private static boolean isAlreadyProcessed(ImageView imageView, Uri uri) {
        PhotoViewTag vt = (PhotoViewTag) imageView.getTag(TAG_PHOTO_INFOS);
        return (vt != null && UriUtils.areEqual(uri, vt.uri));
    }

    /**
     * Called when loading is done.
     */
    @Override
    public void handleMessage(Message msg) {
        WorkerArgs args = (WorkerArgs) msg.obj;
        if (msg.arg1 == EVENT_LOAD_IMAGE || msg.arg1 == EVENT_LOAD_IMAGE_URI) {
            boolean imagePresent = false;
            // Sanity check on image view
            PhotoViewTag photoTag = (PhotoViewTag) args.view.getTag(TAG_PHOTO_INFOS);
            if (photoTag == null) {
                Log.w(THIS_FILE, "Tag has been removed meanwhile");
                return;
            }
            if (!UriUtils.areEqual(args.loadedUri, photoTag.uri)) {
                Log.w(THIS_FILE, "Image view has changed uri meanwhile");
                return;
            }

            // if the image has been loaded then display it, otherwise set
            // default.
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
                Log.v(THIS_FILE, "Notifying listener: " + args.listener.toString() +
                        " image: " + args.loadedUri + " completed");
                args.listener.onImageLoadComplete(msg.what, args.cookie, args.view,
                        imagePresent);
            }
        }
    }

}
