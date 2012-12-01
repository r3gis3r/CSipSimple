/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of pjsip_android.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ZRTP_ANDROID_CALLBACK_H_
#define ZRTP_ANDROID_CALLBACK_H_


#include <pjsua-lib/pjsua.h>

class ZrtpCallback {
public:
    virtual ~ZrtpCallback() {}
    virtual void on_zrtp_show_sas (pjsua_call_id call_id, const pj_str_t *sas, int verified) {}
    virtual void on_zrtp_update_transport (pjsua_call_id call_id) {}
};


extern "C" {
    void setZrtpCallbackObject(ZrtpCallback* callback);
}


#endif /* ZRTP_ANDROID_CALLBACK_H_ */
