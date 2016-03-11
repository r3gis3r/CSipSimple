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
 * Copyright (C) 2012 The Android Open Source Project
 */
package com.csipsimple.ui.incall.locker.multiwaveview;

import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class Ease {
    private static final float DOMAIN = 1.0f;
    private static final float DURATION = 1.0f;
    private static final float START = 0.0f;

    static class Linear {
        public static final TimeInterpolator easeNone = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return input;
            }
        };
    }

    static class Cubic {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return DOMAIN*(input/=DURATION)*input*input + START;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return DOMAIN*((input=input/DURATION-1)*input*input + 1) + START;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return ((input/=DURATION/2) < 1.0f) ?
                        (DOMAIN/2*input*input*input + START)
                            : (DOMAIN/2*((input-=2)*input*input + 2) + START);
            }
        };
    }

    static class Quad {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            public float getInterpolation (float input) {
                return DOMAIN*(input/=DURATION)*input + START;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return -DOMAIN *(input/=DURATION)*(input-2) + START;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return ((input/=DURATION/2) < 1) ?
                        (DOMAIN/2*input*input + START)
                            : (-DOMAIN/2 * ((--input)*(input-2) - 1) + START);
            }
        };
    }

    static class Quart {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return DOMAIN*(input/=DURATION)*input*input*input + START;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return -DOMAIN * ((input=input/DURATION-1)*input*input*input - 1) + START;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return ((input/=DURATION/2) < 1) ?
                        (DOMAIN/2*input*input*input*input + START)
                            : (-DOMAIN/2 * ((input-=2)*input*input*input - 2) + START);
            }
        };
    }

    static class Quint {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return DOMAIN*(input/=DURATION)*input*input*input*input + START;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return DOMAIN*((input=input/DURATION-1)*input*input*input*input + 1) + START;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return ((input/=DURATION/2) < 1) ?
                        (DOMAIN/2*input*input*input*input*input + START)
                            : (DOMAIN/2*((input-=2)*input*input*input*input + 2) + START);
            }
        };
    }

    static class Sine {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return -DOMAIN * (float) Math.cos(input/DURATION * (Math.PI/2)) + DOMAIN + START;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return DOMAIN * (float) Math.sin(input/DURATION * (Math.PI/2)) + START;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return -DOMAIN/2 * ((float)Math.cos(Math.PI*input/DURATION) - 1.0f) + START;
            }
        };
    }

}

