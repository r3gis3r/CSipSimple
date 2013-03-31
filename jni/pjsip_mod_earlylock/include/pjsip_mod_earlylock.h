/*
 * pjsip_mobile_reg_handler.h
 *
 *  Created on: 1 déc. 2012
 *      Author: r3gis3r
 */

#ifndef PJSIP_MOD_EARLYLOCK_H_
#define PJSIP_MOD_EARLYLOCK_H_

#include <pj/config_site.h>
#include <pjsua-lib/pjsua.h>

class EarlyLockCallback {
public:
    virtual ~EarlyLockCallback() {}
    virtual void on_create_early_lock() {}
};


extern "C" {
pj_status_t mod_earlylock_init();
void mod_earlylock_set_callback(EarlyLockCallback* callback);
}

#endif /* PJSIP_MOD_EARLYLOCK_H_ */
