package com.actionbarsherlock.internal.nineoldandroids.view;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.actionbarsherlock.internal.nineoldandroids.view.animation.AnimatorProxy;
import com.actionbarsherlock.internal.utils.UtilityWrapper;

import java.lang.reflect.Method;

public abstract class NineViewGroup extends ViewGroup {
    private final AnimatorProxy mProxy;
    private static Method superGetAlphaMethod;
    private static Method superSetAlphaMethod;
    private static Method superGetTranslationXMethod;
    private static Method superSetTranslationXMethod;
    private static Method superGetTranslationYMethod;
    private static Method superSetTranslationYMethod;

    public NineViewGroup(Context context) {
        super(context);
        mProxy = AnimatorProxy.NEEDS_PROXY ? AnimatorProxy.wrap(this) : null;
        loadStaticMethods();
    }
    public NineViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        mProxy = AnimatorProxy.NEEDS_PROXY ? AnimatorProxy.wrap(this) : null;
        loadStaticMethods();
    }
    public NineViewGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mProxy = AnimatorProxy.NEEDS_PROXY ? AnimatorProxy.wrap(this) : null;
        loadStaticMethods();
    }
    
    
    private void loadStaticMethods() {

        // Building compatibility
        Class<?> cls = NineViewGroup.class;
        if (!AnimatorProxy.NEEDS_PROXY && superGetAlphaMethod == null) {
            superGetAlphaMethod = UtilityWrapper.safelyGetSuperclassMethod(cls, "getAlpha");
            superSetAlphaMethod = UtilityWrapper.safelyGetSuperclassMethod(cls, "setAlpha", float.class);

            superGetTranslationXMethod = UtilityWrapper.safelyGetSuperclassMethod(cls,
                    "getTranslationX");
            superSetTranslationXMethod = UtilityWrapper.safelyGetSuperclassMethod(cls,
                    "setTranslationX", float.class);
            superGetTranslationYMethod = UtilityWrapper.safelyGetSuperclassMethod(cls,
                    "getTranslationY");
            superSetTranslationYMethod = UtilityWrapper.safelyGetSuperclassMethod(cls,
                    "setTranslationY", float.class);
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
            Float res = (Float) UtilityWrapper.safelyInvokeMethod(superGetAlphaMethod, this);
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

    public float getSupportTranslationX() {
        if (AnimatorProxy.NEEDS_PROXY) {
            return mProxy.getTranslationX();
        } else {
            Float res = (Float) UtilityWrapper.safelyInvokeMethod(superGetTranslationXMethod, this);
            if(res != null) {
                return res;
            }
            return 0.0f;
        }
    }
    public void setSupportTranslationX(float translationX) {
        if (AnimatorProxy.NEEDS_PROXY) {
            mProxy.setTranslationX(translationX);
        } else {
            UtilityWrapper.safelyInvokeMethod(superSetTranslationXMethod, this, translationX);
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
    
    public void onConfigurationChanged(Configuration newConfig) {
        // No need to call super cause the one of view + crash on 1.6
        // super.onConfigurationChanged(newConfig);
    }
}
