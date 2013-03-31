%header %{
#include "pjsip_mod_earlylock.h"
%}

%feature("director") EarlyLockCallback;
%include pjsip_mod_earlylock.h
