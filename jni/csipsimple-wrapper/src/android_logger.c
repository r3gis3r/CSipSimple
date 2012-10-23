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

#include "android_logger.h"
#include <android/log.h>


#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "libpjsip", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "libpjsip", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "libpjsip", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "libpjsip", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "libpjsip", __VA_ARGS__)

PJ_DEF(void) pj_android_log_msg(int level, const char *data, int len) {
	const char delims[] = "\n";
	char *line = strtok(data, delims);
	while(line != NULL){
		if (level <= 1) {
			LOGE("%s", line);
		} else if (level == 2) {
			LOGW("%s", line);
		} else if (level == 3) {
			LOGI("%s", line);
		} else if (level == 4) {
			LOGD("%s", line);
		} else if (level >= 5) {
			LOGV("%s", line);
		}
		line = strtok(NULL, delims);
	}
}
