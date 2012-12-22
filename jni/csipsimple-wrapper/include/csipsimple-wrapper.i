%header %{

#include "pjsua_jni_addons.h"
#include "csipsimple_codecs_utils.h"
#include "zrtp_android.h"
#include "zrtp_android_callback.h"
#include "pjsip_mobile_reg_handler.h"

%}

%feature("director") ZrtpCallback;
%feature("director") MobileRegHandlerCallback;

%include zrtp_android_callback.h
%include pjsip_mobile_reg_handler.h