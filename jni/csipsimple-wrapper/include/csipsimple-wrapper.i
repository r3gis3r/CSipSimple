%header %{

#include "pjsua_jni_addons.h"
#include "csipsimple_codecs_utils.h"
#include "zrtp_android.h"
#include "zrtp_android_callback.h"

%}

%feature("director") ZrtpCallback;

%include zrtp_android_callback.h
