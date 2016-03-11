package com.actionbarsherlock.internal.nineoldandroids.widget;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.actionbarsherlock.internal.nineoldandroids.view.animation.AnimatorProxy;
import com.actionbarsherlock.internal.utils.UtilityWrapper;

import java.lang.reflect.Method;

public class NineFrameLayout extends FrameLayout {
    private final AnimatorProxy mProxy;
    private static Method superSetAlphaMethod = null;
    private static Method superGetAlphaMethod = null;
    private static Method superGetTranslationYMethod = null;
    private static Method superSetTranslationYMethod = null;
    private static Method superOnHoverEventMethod = null;

    public NineFrameLayout(Context context) {
        super(context);
        mProxy = AnimatorProxy.NEEDS_PROXY ? AnimatorProxy.wrap(this) : null;
        loadStaticMethods();
    }
    public NineFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mProxy = AnimatorProxy.NEEDS_PROXY ? AnimatorProxy.wrap(this) : null;
        loadStaticMethods();
    }
    public NineFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mProxy = AnimatorProxy.NEEDS_PROXY ? AnimatorProxy.wrap(this) : null;
        loadStaticMethods();
    }
    
    private void loadStaticMethods() {

        // Building compatibility
        Class<?> cls = NineFrameLayout.class;
        if (!AnimatorProxy.NEEDS_PROXY && superGetAlphaMethod == null) {
            superGetAlphaMethod = UtilityWrapper.safelyGetSuperclassMethod(cls, "getAlpha");
            superSetAlphaMethod = UtilityWrapper.safelyGetSuperclassMethod(cls, "setAlpha", float.class);
            superGetTranslationYMethod = UtilityWrapper.safelyGetSuperclassMethod(cls,
                    "getTranslationY");
            superSetTranslationYMethod = UtilityWrapper.safelyGetSuperclassMethod(cls,
                    "setTranslationY", float.class);

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
                && superOnHoverEventMethod == null) {
            superOnHoverEventMethod = UtilityWrapper.safelyGetSuperclassMethod(cls, "onHoverEvent", MotionEvent.class);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        if (mProxy != null) {
            if (visibility == GONE) {
                clearAnimation();
            } else if (visibility == VISIBLE) {
                setAnimation(mProxy);
            }
        }
        super.setVisibility(visibility);
    }

    public float getSupportAlpha() {
        if (AnimatorProxy.NEEDS_PROXY) {
            return mProxy.getAlpha();
        } else {
            Float res = (Float) UtilityWrapper.safelyInvokeMethod(superGetAlphaMethod, this, (Object[]) null);
            if(res != null) {
                return res;
            }
        }
        return 0.0f;
    }
    
    public void setSupportAlpha(float alpha) {
        if (AnimatorProxy.NEEDS_PROXY) {
            mProxy.setAlpha(alpha);
        } else {
            UtilityWrapper.safelyInvokeMethod(superSetAlphaMethod, this, alpha);
        }
    }
    
    public float getSupportTranslationY() {
        if (AnimatorProxy.NEEDS_PROXY) {
            return mProxy.getTranslationY();
        } else {
            Float res = (Float) UtilityWrapper.safelyInvokeMethod(superGetTranslationYMethod, this);
            if(res != null) {
                return res;
            }
            return 0.0f;
        }
    }
    public void setSupportTranslationY(float translationY) {
        if (AnimatorProxy.NEEDS_PROXY) {
            mProxy.setTranslationY(translationY);
        } else {
            UtilityWrapper.safelyInvokeMethod(superSetTranslationYMethod, this, translationY);
        }
    }
    
    /*
    public boolean supportOnHoverEvent(MotionEvent event) {
        if(superOnHoverEventMethod != null) {
            UtilityWrapper.safelyInvokeMethod(superOnHoverEventMethod, this, event);
        }
        return true;
    }
    */
}
