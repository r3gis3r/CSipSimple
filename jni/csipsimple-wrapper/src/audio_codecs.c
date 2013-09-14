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

#include "csipsimple_internal.h"
#include "pjsip_opus_sdp_rewriter.h"

#include <pjmedia-codec.h>
#include <pjmedia/g711.h>

#if PJMEDIA_HAS_WEBRTC_CODEC
#include <webrtc_codec.h>
#endif

#if PJMEDIA_HAS_OPENCORE_AMRNB_CODEC || DPJMEDIA_HAS_OPENCORE_AMRWB_CODEC
#include <stagefright_amr.h>
#endif

#define THIS_FILE "audio_codecs.c"

PJ_DEF(void) pjmedia_audio_codec_config_default(pjmedia_audio_codec_config*cfg) {
	pj_bzero(cfg, sizeof(*cfg));
	cfg->speex.option = 0;
	cfg->speex.quality = PJMEDIA_CODEC_SPEEX_DEFAULT_QUALITY;
	cfg->speex.complexity = PJMEDIA_CODEC_SPEEX_DEFAULT_COMPLEXITY;
	cfg->ilbc.mode = 30;
	cfg->passthrough.setting.ilbc_mode = cfg->ilbc.mode;
}

PJ_DEF(pj_status_t) pjmedia_codec_register_audio_codecs(pjmedia_endpt *endpt,
		const pjmedia_audio_codec_config *c) {
	pjmedia_audio_codec_config default_cfg;
	pj_status_t status;

	PJ_ASSERT_RETURN(endpt, PJ_EINVAL);
	if (!c) {
		pjmedia_audio_codec_config_default(&default_cfg);
		c = &default_cfg;
	}

	PJ_ASSERT_RETURN(c->ilbc.mode==20 || c->ilbc.mode==30, PJ_EINVAL);

#if PJMEDIA_HAS_PASSTHROUGH_CODECS
	status = pjmedia_codec_passthrough_init2(endpt, &c->passthough.ilbc);
	if (status != PJ_SUCCESS)
	return status;
#endif

#if PJMEDIA_HAS_SPEEX_CODEC
	/* Register speex. */
	status = pjmedia_codec_speex_init(endpt, c->speex.option, c->speex.quality,
			c->speex.complexity);
	if (status != PJ_SUCCESS)
		return status;
#endif

#if PJMEDIA_HAS_ILBC_CODEC
	/* Register iLBC. */
	status = pjmedia_codec_ilbc_init(endpt, c->ilbc.mode);
	if (status != PJ_SUCCESS)
		return status;
#endif /* PJMEDIA_HAS_ILBC_CODEC */

#if PJMEDIA_HAS_GSM_CODEC
	/* Register GSM */
	status = pjmedia_codec_gsm_init(endpt);
	if (status != PJ_SUCCESS)
		return status;
#endif /* PJMEDIA_HAS_GSM_CODEC */

#if PJMEDIA_HAS_G711_CODEC
	/* Register PCMA and PCMU */
	status = pjmedia_codec_g711_init(endpt);
	if (status != PJ_SUCCESS)
		return status;
#endif	/* PJMEDIA_HAS_G711_CODEC */

#if PJMEDIA_HAS_G722_CODEC
	status = pjmedia_codec_g722_init(endpt);
	if (status != PJ_SUCCESS)
		return status;
#endif  /* PJMEDIA_HAS_G722_CODEC */

#if PJMEDIA_HAS_L16_CODEC
	/* Register L16 family codecs */
	status = pjmedia_codec_l16_init(endpt, 0);
	if (status != PJ_SUCCESS)
	return status;
#endif	/* PJMEDIA_HAS_L16_CODEC */

#if PJMEDIA_HAS_OPENCORE_AMRNB_CODEC || PJMEDIA_HAS_OPENCORE_AMRWB_CODEC
	/* Register OpenCORE AMR */
    status = pjmedia_codec_opencore_stagefright_init(endpt);
    if (status != PJ_SUCCESS)
        return status;
#endif

#if PJMEDIA_HAS_WEBRTC_CODEC
	/* Register WEBRTC */
	status = pjmedia_codec_webrtc_init(endpt);
	if (status != PJ_SUCCESS)
		return status;
#endif /* PJMEDIA_HAS_WEBRTC_CODEC */

#if PJMEDIA_HAS_SILK_CODEC
	status = pjmedia_codec_silk_init(endpt);
	if (status != PJ_SUCCESS)
		return status;

	// Our default config
	pjmedia_codec_silk_setting silk_settings;
	silk_settings.complexity = -1;
	silk_settings.enabled = PJ_TRUE;
	silk_settings.quality = 3;
	pjmedia_codec_silk_set_config(8000, &silk_settings);
	pjmedia_codec_silk_set_config(12000, &silk_settings);
	pjmedia_codec_silk_set_config(16000, &silk_settings);
	pjmedia_codec_silk_set_config(24000, &silk_settings);

#endif /* PJMEDIA_HAS_SILK_CODEC */

	// Dynamic loading of plugins codecs
	unsigned i;

	for (i = 0; i < css_var.extra_aud_codecs_cnt; i++) {
		dynamic_factory *codec = &css_var.extra_aud_codecs[i];
		pj_status_t (*init_factory)(
							pjmedia_endpt *endpt) = get_library_factory(codec);
		if(init_factory != NULL){
			status = init_factory(endpt);
			if(status != PJ_SUCCESS) {
				PJ_LOG(2, (THIS_FILE,"Error loading dynamic codec plugin"));
			}
    	}
	}

	// Register opus sdp rewriter
	// TODO -- get info from registrations made previously + only when opus detected
	pjsip_opus_sdp_rewriter_init(16000);

	return PJ_SUCCESS;
}
