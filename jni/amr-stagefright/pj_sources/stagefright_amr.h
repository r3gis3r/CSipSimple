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

#ifndef STAGEFRIGHT_AMR_H_
#define STAGEFRIGHT_AMR_H_



#include <pjmedia-codec/types.h>


PJ_BEGIN_DECL

/**
 * Initialize and register AMR codec factory using default settings to
 * pjmedia endpoint.
 *
 * @param endpt The pjmedia endpoint.
 *
 * @return  PJ_SUCCESS on success.
 */
PJ_DECL(pj_status_t)
pjmedia_codec_opencore_stagefright_init(pjmedia_endpt* endpt);

PJ_END_DECL

#endif /* STAGEFRIGHT_AMR_H_ */
