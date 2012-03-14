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

#ifndef WEBRTC_VID_CAPTURE_DEV_H_
#define WEBRTC_VID_CAPTURE_DEV_H_


#include <pjmedia-videodev/videodev_imp.h>



/*
 * C compatible declaration of Android screen capture factory.
 */
PJ_BEGIN_DECL
PJ_DECL(pjmedia_vid_dev_factory*) pjmedia_ascreen_vid_capture_factory(pj_pool_factory *pf);

PJ_END_DECL


#endif /* WEBRTC_VID_CAPTURE_DEV_H_ */
