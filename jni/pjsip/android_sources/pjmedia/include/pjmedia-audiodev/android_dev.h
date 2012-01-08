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

#ifndef ANDROID_DEV_H_
#define ANDROID_DEV_H_


#include <pjmedia-audiodev/audiodev_imp.h>
#include <pj/assert.h>
#include <pj/log.h>
#include <pj/os.h>
#include <pj/string.h>
#include <sys/resource.h>
//#include <utils/threads.h>

/*
 * C compatible declaration of Android factory.
 */
PJ_BEGIN_DECL
PJ_DECL(pjmedia_aud_dev_factory*) pjmedia_android_factory(pj_pool_factory *pf);
PJ_END_DECL


#endif /* ANDROID_DEV_H_ */
