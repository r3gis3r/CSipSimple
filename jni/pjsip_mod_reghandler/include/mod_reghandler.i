%header %{
#include "pjsip_mobile_reg_handler.h"
%}

%feature("director") MobileRegHandlerCallback;
%include pjsip_mobile_reg_handler.h