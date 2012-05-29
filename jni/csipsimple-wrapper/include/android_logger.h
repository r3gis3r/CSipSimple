/*
 * android_logger.h
 *
 *  Created on: 29 mai 2012
 *      Author: r3gis3r
 */

#ifndef ANDROID_LOGGER_H_
#define ANDROID_LOGGER_H_

#include <pj/config_site.h>
#include <pjsua-lib/pjsua.h>

PJ_BEGIN_DECL

PJ_DECL(void) pj_android_log_msg(int level, const char *data, int len);

PJ_END_DECL

#endif /* ANDROID_LOGGER_H_ */
