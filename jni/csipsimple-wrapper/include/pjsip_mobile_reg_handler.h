/*
 * pjsip_mobile_reg_handler.h
 *
 *  Created on: 1 déc. 2012
 *      Author: r3gis3r
 */

#ifndef PJSIP_MOBILE_REG_HANDLER_H_
#define PJSIP_MOBILE_REG_HANDLER_H_

#include <pj/config_site.h>
#include <pjsua-lib/pjsua.h>

class MobileRegHandlerCallback {
public:
    virtual ~MobileRegHandlerCallback() {}
    virtual void on_save_contact(pjsua_acc_id acc_id, pj_str_t contact, int expires) {}
    virtual pj_str_t on_restore_contact(pjsua_acc_id acc_id) {}
};


extern "C" {
pj_status_t mobile_reg_handler_init();
void mobile_reg_handler_set_callback(MobileRegHandlerCallback* callback);
}

#endif /* PJSIP_MOBILE_REG_HANDLER_H_ */
