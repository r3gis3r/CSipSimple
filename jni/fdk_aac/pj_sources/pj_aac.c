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

#include <pjmedia/codec.h>
#include <pjmedia/endpoint.h>
#include <pjmedia/errno.h>
#include <pjmedia/port.h>
#include <pjmedia-codec/types.h>
#include <pj/pool.h>
#include <pj/string.h>
#include <pj/assert.h>
#include <pj/log.h>

#if defined(PJMEDIA_HAS_AAC_CODEC) && (PJMEDIA_HAS_AAC_CODEC!=0)
#include "pj_aac.h"
#include <FDK_audio.h>
#include <aacenc_lib.h>
#include <aacdecoder_lib.h>

#define THIS_FILE       "pj_aac.c"

#define DEFAULT_CLOCK_RATE  48000
#define DEFAULT_PTIME       20
#define INDEX_BIT_LENGTH    3

/* Prototypes for AAC factory */
static pj_status_t aac_test_alloc(pjmedia_codec_factory *factory,
		const pjmedia_codec_info *id);
static pj_status_t aac_default_attr(pjmedia_codec_factory *factory,
		const pjmedia_codec_info *id, pjmedia_codec_param *attr);
static pj_status_t aac_enum_codecs(pjmedia_codec_factory *factory,
		unsigned *count, pjmedia_codec_info codecs[]);
static pj_status_t aac_alloc_codec(pjmedia_codec_factory *factory,
		const pjmedia_codec_info *id, pjmedia_codec **p_codec);
static pj_status_t aac_dealloc_codec(pjmedia_codec_factory *factory,
		pjmedia_codec *codec);

/* Prototypes for AAC implementation. */
static pj_status_t aac_codec_init(pjmedia_codec *codec, pj_pool_t *pool);
static pj_status_t aac_codec_open(pjmedia_codec *codec,
		pjmedia_codec_param *attr);
static pj_status_t aac_codec_close(pjmedia_codec *codec);
static pj_status_t aac_codec_modify(pjmedia_codec *codec,
		const pjmedia_codec_param *attr);
static pj_status_t aac_codec_parse(pjmedia_codec *codec, void *pkt,
		pj_size_t pkt_size, const pj_timestamp *timestamp, unsigned *frame_cnt,
		pjmedia_frame frames[]);
static pj_status_t aac_codec_encode(pjmedia_codec *codec,
		const struct pjmedia_frame *input, unsigned output_buf_len,
		struct pjmedia_frame *output);
static pj_status_t aac_codec_decode(pjmedia_codec *codec,
		const struct pjmedia_frame *input, unsigned output_buf_len,
		struct pjmedia_frame *output);
static pj_status_t aac_codec_recover(pjmedia_codec *codec,
		unsigned output_buf_len, struct pjmedia_frame *output);

/* Definition for AAC codec operations. */
static pjmedia_codec_op aac_op = { &aac_codec_init, &aac_codec_open,
		&aac_codec_close, &aac_codec_modify, &aac_codec_parse,
		&aac_codec_encode, &aac_codec_decode, &aac_codec_recover };

/* Definition for AAC codec factory operations. */
static pjmedia_codec_factory_op aac_factory_op = { &aac_test_alloc,
		&aac_default_attr, &aac_enum_codecs, &aac_alloc_codec,
		&aac_dealloc_codec, &pjmedia_codec_aac_deinit };

/* AAC factory private data */
static struct aac_factory {
	pjmedia_codec_factory base;
	pjmedia_endpt *endpt;
	pj_pool_t *pool;
	pj_mutex_t *mutex;
	pjmedia_codec codec_list;
} aac_factory;

/* AAC codec private data. */
struct aac_encoder_params {
    AUDIO_OBJECT_TYPE           aot;
    CHANNEL_MODE                channel_mode;
    int                         granule_length;
    pj_bool_t                   sbr;
};

struct aac_private {
	pj_pool_t *pool; /**< Pool for each instance.    */

	pj_bool_t                   enc_ready;
    unsigned                    clock_rate;      /**< Sampling rate in Hz        */
    unsigned                    channel_cnt;
	HANDLE_AACENCODER           hAacEnc;
	struct aac_encoder_params   enc_params;


	pj_bool_t                   dec_ready;
	HANDLE_AACDECODER           hAacDec;
	unsigned                    pcm_frame_size;  /**< PCM frame size in bytes */
};


/**
 * Utilities from pjsip
 */
#define hex_digits   "0123456789abcdef"

static void val_to_hex_digit(unsigned value, char *p) {
    *p++ = hex_digits[ (value & 0xF0) >> 4 ];
    *p   = hex_digits[ (value & 0x0F) ];
}

static unsigned hex_digit_to_val(unsigned char c) {
    if (c <= '9')
    return (c-'0') & 0x0F;
    else if (c <= 'F')
    return  (c-'A'+10) & 0x0F;
    else
    return (c-'a'+10) & 0x0F;
}

static pj_status_t aac_encoder_open(HANDLE_AACENCODER* hAacEnc, const pjmedia_codec_param *attr, const struct aac_encoder_params *aac_params) {
    AACENC_ERROR error;
    UINT         encModules = 0x01; /* AAC core */

    if (aac_params->sbr) {
        /* AAC SBR */
        encModules |= 0x02;
    }

    error = aacEncOpen(hAacEnc, encModules, attr->info.channel_cnt);

    if(error != AACENC_OK) {
        PJ_LOG(1, (THIS_FILE, "Error while creating encoder %d", error));
        return PJ_EINVAL;
    }

    aacEncoder_SetParam(*hAacEnc, AACENC_AOT, aac_params->aot);
    aacEncoder_SetParam(*hAacEnc, AACENC_SAMPLERATE, attr->info.clock_rate);
    aacEncoder_SetParam(*hAacEnc, AACENC_CHANNELMODE, aac_params->channel_mode);
    aacEncoder_SetParam(*hAacEnc, AACENC_CHANNELORDER, 1);
    aacEncoder_SetParam(*hAacEnc, AACENC_TRANSMUX, TT_MP4_RAW);
    aacEncoder_SetParam(*hAacEnc, AACENC_SIGNALING_MODE, 0);
    aacEncoder_SetParam(*hAacEnc, AACENC_GRANULE_LENGTH, aac_params->granule_length);
    aacEncoder_SetParam(*hAacEnc, AACENC_BITRATE, attr->info.avg_bps);
    aacEncoder_SetParam(*hAacEnc, AACENC_BITRATEMODE, 8);
    aacEncoder_SetParam(*hAacEnc, AACENC_SBR_MODE, aac_params->sbr ? 1 : 0);


    error = aacEncEncode(*hAacEnc, NULL, NULL, NULL, NULL);
    if(error != AACENC_OK) {
        PJ_LOG(1, (THIS_FILE, "Error while initializing encoder %d", error));
        return PJ_EINVAL;
    }
    return PJ_SUCCESS;
}

static void aac_add_int_codec_param(pj_pool_t* pool, pjmedia_codec_fmtp* fmtp, pj_str_t name, int value){
    fmtp->param[fmtp->cnt].name = name;
    fmtp->param[fmtp->cnt].val.ptr = (char*) pj_pool_alloc(pool, 32);
    fmtp->param[fmtp->cnt].val.slen = pj_utoa(value, fmtp->param[fmtp->cnt].val.ptr);
    fmtp->cnt++;
}

static void aac_add_str_codec_param(pj_pool_t* pool, pjmedia_codec_fmtp* fmtp, pj_str_t name, pj_str_t value){
    fmtp->param[fmtp->cnt].name = name;
    fmtp->param[fmtp->cnt].val = value;
    fmtp->cnt++;
}

static pj_status_t aac_parse_fmtp(
                    const pjmedia_codec_fmtp *fmtp,
                    struct aac_encoder_params *aac_fmtp)
{
    const pj_str_t OBJECT = {"object", 6};
    const pj_str_t SBR = {"sbr", 3};
    unsigned i;

    pj_bzero(aac_fmtp, sizeof(*aac_fmtp));
    aac_fmtp->aot = AOT_ER_AAC_ELD;
    /* Assume only mono/stereo for now */
    aac_fmtp->channel_mode = MODE_1;
    aac_fmtp->granule_length = 480;
    aac_fmtp->sbr = 0;

    for (i=0; i<fmtp->cnt; ++i) {
    if (pj_stricmp(&fmtp->param[i].name, &OBJECT)==0) {
        aac_fmtp->aot = pj_strtoul(&fmtp->param[i].val);
    } else if (pj_stricmp(&fmtp->param[i].name, &SBR)==0) {
        if(fmtp->param[i].val.slen == 0 || fmtp->param[i].val.ptr[0] == '1'){
            aac_fmtp->sbr = 1;
        }
    }
    }

    return PJ_SUCCESS;
}

/**
 * Apply aac settings to dec_fmtp parameters
 */
static void aac_apply_codec_params(pj_pool_t* pool, pjmedia_codec_param *attr, const struct aac_encoder_params *aac_params) {
    unsigned i;
    HANDLE_AACENCODER hAacEnc;
    AACENC_InfoStruct pInfo;

	attr->setting.dec_fmtp.cnt = 0;

	aac_add_int_codec_param(pool, &attr->setting.dec_fmtp, pj_str("streamtype"), 5);
    aac_add_str_codec_param(pool, &attr->setting.dec_fmtp, pj_str("mode"), pj_str("AAC-hbr"));
	aac_add_str_codec_param(pool, &attr->setting.dec_fmtp, pj_str("profile-level-id"), pj_str("58"));
    aac_add_int_codec_param(pool, &attr->setting.dec_fmtp, pj_str("sizelength"), 16 - INDEX_BIT_LENGTH);
    aac_add_int_codec_param(pool, &attr->setting.dec_fmtp, pj_str("indexlength"), INDEX_BIT_LENGTH);
    aac_add_int_codec_param(pool, &attr->setting.dec_fmtp, pj_str("indexdeltalength"), INDEX_BIT_LENGTH);
    aac_add_int_codec_param(pool, &attr->setting.dec_fmtp, pj_str("object"), aac_params->aot);
    if(aac_params->sbr) {
        aac_add_int_codec_param(pool, &attr->setting.dec_fmtp, pj_str("sbr"), 1);
    }
    aac_add_int_codec_param(pool, &attr->setting.dec_fmtp, pj_str("bitrate"), attr->info.avg_bps);


    /* Compute config */
    aac_encoder_open(&hAacEnc, attr, aac_params);
    aacEncInfo(hAacEnc, &pInfo);
    aacEncClose(&hAacEnc);
    attr->setting.dec_fmtp.param[attr->setting.dec_fmtp.cnt].name = pj_str("config");
    attr->setting.dec_fmtp.param[attr->setting.dec_fmtp.cnt].val.slen = pInfo.confSize*2;
    attr->setting.dec_fmtp.param[attr->setting.dec_fmtp.cnt].val.ptr = (char*) pj_pool_alloc(pool, pInfo.confSize * 2);
    for( i = 0; i < pInfo.confSize; i++ ){
        val_to_hex_digit( pInfo.confBuf[i], &attr->setting.dec_fmtp.param[attr->setting.dec_fmtp.cnt].val.ptr[2*i]);
    }
    attr->setting.dec_fmtp.cnt++;
}

PJ_DEF(pj_status_t) pjmedia_codec_aac_init(pjmedia_endpt *endpt) {
	pjmedia_codec_mgr *codec_mgr;
	pj_status_t status;

	if (aac_factory.endpt != NULL) {
		/* Already initialized. */
		return PJ_SUCCESS;
	}

	/* Init factory */
	aac_factory.base.op = &aac_factory_op;
	aac_factory.base.factory_data = NULL;
	aac_factory.endpt = endpt;

	/* Create pool */
	aac_factory.pool = pjmedia_endpt_create_pool(endpt, "aac codecs", 4000,
			4000);
	if (!aac_factory.pool)
		return PJ_ENOMEM;

	/* Init list */
	pj_list_init(&aac_factory.codec_list);

	/* Create mutex. */
	status = pj_mutex_create_simple(aac_factory.pool, "aac codecs",
			&aac_factory.mutex);
	if (status != PJ_SUCCESS)
		goto on_error;
	PJ_LOG(5, (THIS_FILE, "Init AAC"));

	/* Get the codec manager. */
	codec_mgr = pjmedia_endpt_get_codec_mgr(endpt);
	if (!codec_mgr) {
		return PJ_EINVALIDOP;
	}

	/* Register codec factory to endpoint. */
	status = pjmedia_codec_mgr_register_factory(codec_mgr, &aac_factory.base);
	if (status != PJ_SUCCESS)
		return status;

	return PJ_SUCCESS;

	on_error:
	if (aac_factory.mutex) {
		pj_mutex_destroy(aac_factory.mutex);
		aac_factory.mutex = NULL;
	}
	if (aac_factory.pool) {
		pj_pool_release(aac_factory.pool);
		aac_factory.pool = NULL;
	}

	return status;
}

/*
 * Unregister AAC codec factory from pjmedia endpoint and deinitialize
 * the AAC codec library.
 */PJ_DEF(pj_status_t) pjmedia_codec_aac_deinit(void) {
	pjmedia_codec_mgr *codec_mgr;
	pj_status_t status;

	if (aac_factory.endpt == NULL) {
		/* Not registered. */
		return PJ_SUCCESS;
	}

	/* Lock mutex. */
	pj_mutex_lock(aac_factory.mutex);

	/* Get the codec manager. */
	codec_mgr = pjmedia_endpt_get_codec_mgr(aac_factory.endpt);
	if (!codec_mgr) {
		aac_factory.endpt = NULL;
		pj_mutex_unlock(aac_factory.mutex);
		return PJ_EINVALIDOP;
	}

	/* Unregister AAC codec factory. */
	status = pjmedia_codec_mgr_unregister_factory(codec_mgr,
			&aac_factory.base);
	aac_factory.endpt = NULL;

	/* Destroy mutex. */
	pj_mutex_destroy(aac_factory.mutex);
	aac_factory.mutex = NULL;

	/* Release pool. */
	pj_pool_release(aac_factory.pool);
	aac_factory.pool = NULL;

	return status;
}

/*
 * Check if factory can allocate the specified codec.
 */
static pj_status_t aac_test_alloc(pjmedia_codec_factory *factory,
		const pjmedia_codec_info *info) {
	const pj_str_t aac_tag = { "mpeg4-generic", 13 };
	unsigned i;

	PJ_UNUSED_ARG(factory);
	PJ_ASSERT_RETURN(factory==&aac_factory.base, PJ_EINVAL);

	/* Type MUST be audio. */
	if (info->type != PJMEDIA_TYPE_AUDIO) {
		return PJMEDIA_CODEC_EUNSUP;
	}

	/* Check encoding name. */
	if (pj_stricmp(&info->encoding_name, &aac_tag) != 0) {
		return PJMEDIA_CODEC_EUNSUP;
	}

	/* Check clock-rate */
	if (info->clock_rate == 8000 || info->clock_rate == 12000
			|| info->clock_rate == 16000 || info->clock_rate == 24000
			|| info->clock_rate == 48000) {
		return PJ_SUCCESS;
	}
	/* Clock rate not supported */
	return PJMEDIA_CODEC_EUNSUP;
}

/*
 * Generate default attribute.
 */
static pj_status_t aac_default_attr(pjmedia_codec_factory *factory,
		const pjmedia_codec_info *id, pjmedia_codec_param *attr) {
    struct aac_encoder_params default_aac_params;
	pj_bzero(attr, sizeof(pjmedia_codec_param));
	PJ_ASSERT_RETURN(factory == &aac_factory.base, PJ_EINVAL);
    /*
    Audio Object Type  |  Bit Rate Range  |            Supported  | Preferred  | No. of
                       |         [bit/s]  |       Sampling Rates  |    Sampl.  |  Chan.
                       |                  |                [kHz]  |      Rate  |
                       |                  |                       |     [kHz]  |
    ELD + SBR          |  16000 -  24999  |        32.00 - 44.10  |     32.00  |      1
    ELD + SBR          |  25000 -  31999  |        32.00 - 48.00  |     32.00  |      1
    ELD + SBR          |  32000 -  64000  |        32.00 - 48.00  |     48.00  |      1
     *
     */
	attr->info.clock_rate = DEFAULT_CLOCK_RATE;
    attr->info.channel_cnt = 1;
	attr->info.avg_bps = 48000;
	attr->info.max_bps = 64000;
	attr->info.frm_ptime = DEFAULT_PTIME;
	attr->info.pcm_bits_per_sample = 16;
	attr->info.pt = (pj_uint8_t) id->pt;

	attr->setting.frm_per_pkt = 1;
	attr->setting.vad = 0;
	attr->setting.plc = 1;

	default_aac_params.aot = AOT_ER_AAC_ELD;
	/* Assume only mono/stereo for now */
	default_aac_params.channel_mode = (attr->info.channel_cnt == 1) ? MODE_1 : MODE_2;
	default_aac_params.granule_length = 480;
	default_aac_params.sbr = 1;

	// Apply these settings to relevant fmtp parameters
	aac_apply_codec_params(aac_factory.pool, attr, &default_aac_params);

	return PJ_SUCCESS;
}

/*
 * Enum codecs supported by this factory.
 */
static pj_status_t aac_enum_codecs(pjmedia_codec_factory *factory,
		unsigned *count, pjmedia_codec_info codecs[]) {
	unsigned max;
	int i; /* Must be signed */
	PJ_LOG(5, (THIS_FILE, "aac enum codecs"));

	PJ_UNUSED_ARG(factory);
	PJ_ASSERT_RETURN(codecs && *count > 0, PJ_EINVAL);

	max = *count;
	*count = 0;

	pj_bzero(&codecs[*count], sizeof(pjmedia_codec_info));
	codecs[*count].encoding_name = pj_str("mpeg4-generic");
	codecs[*count].pt = PJMEDIA_RTP_PT_MPEG4;
	codecs[*count].type = PJMEDIA_TYPE_AUDIO;
	codecs[*count].clock_rate = DEFAULT_CLOCK_RATE;
	codecs[*count].channel_cnt = 1;

	++*count;

	return PJ_SUCCESS;

}

/*
 * Allocate a new AAC codec instance.
 */
static pj_status_t aac_alloc_codec(pjmedia_codec_factory *factory,
		const pjmedia_codec_info *id, pjmedia_codec **p_codec) {
	pjmedia_codec *codec;
	struct aac_private *aac;

	PJ_ASSERT_RETURN(factory && id && p_codec, PJ_EINVAL);
	PJ_ASSERT_RETURN(factory == &aac_factory.base, PJ_EINVAL);

	pj_mutex_lock(aac_factory.mutex);

	/* Get free nodes, if any. */
	if (!pj_list_empty(&aac_factory.codec_list)) {
		codec = aac_factory.codec_list.next;
		pj_list_erase(codec);
	} else {
		codec = PJ_POOL_ZALLOC_T(aac_factory.pool, pjmedia_codec);
		PJ_ASSERT_RETURN(codec != NULL, PJ_ENOMEM);
		codec->op = &aac_op;
		codec->factory = factory;
		codec->codec_data = pj_pool_alloc(aac_factory.pool,
				sizeof(struct aac_private));
	}

	pj_mutex_unlock(aac_factory.mutex);

	aac = (struct aac_private*) codec->codec_data;
	aac->enc_ready = PJ_FALSE;
	aac->dec_ready = PJ_FALSE;

	/* Create pool for codec instance */
	aac->pool = pjmedia_endpt_create_pool(aac_factory.endpt, "mpeg4codec", 512,
			512);

	*p_codec = codec;
	return PJ_SUCCESS;
}

/*
 * Free codec.
 */
static pj_status_t aac_dealloc_codec(pjmedia_codec_factory *factory,
		pjmedia_codec *codec) {
	struct aac_private *aac;

	PJ_ASSERT_RETURN(factory && codec, PJ_EINVAL);
	PJ_UNUSED_ARG(factory);
	PJ_ASSERT_RETURN(factory == &aac_factory.base, PJ_EINVAL);

	aac = (struct aac_private*) codec->codec_data;

	/* Close codec, if it's not closed. */
	if (aac->enc_ready == PJ_TRUE || aac->dec_ready == PJ_TRUE) {
		aac_codec_close(codec);
	}

	/* Put in the free list. */
	pj_mutex_lock(aac_factory.mutex);
	pj_list_push_front(&aac_factory.codec_list, codec);
	pj_mutex_unlock(aac_factory.mutex);

	pj_pool_release(aac->pool);
	aac->pool = NULL;

	return PJ_SUCCESS;
}

/*
 * Init codec.
 */
static pj_status_t aac_codec_init(pjmedia_codec *codec, pj_pool_t *pool) {
	PJ_UNUSED_ARG(codec);
	PJ_UNUSED_ARG(pool);
	return PJ_SUCCESS;
}

/*
 * Open codec.
 */
static pj_status_t aac_codec_open(pjmedia_codec *codec,
		pjmedia_codec_param *attr) {
    AACENC_ERROR error;
	pj_status_t status;
	struct aac_private *aac;
	int id, ret = 0, i;
	struct aac_encoder_params default_aac_params;
	const pj_str_t STR_FMTP_CONFIG = { "config", 6 };

	aac = (struct aac_private*) codec->codec_data;

	pj_assert(aac != NULL);
	pj_assert(aac->enc_ready == PJ_FALSE && aac->dec_ready == PJ_FALSE);

	/* Create Encoder */
	/* TODO : get from fmtp */
	default_aac_params.aot = AOT_ER_AAC_ELD;
    /* Assume only mono/stereo for now */
    default_aac_params.channel_mode = (attr->info.channel_cnt == 1) ? MODE_1 : MODE_2;
    default_aac_params.granule_length = 480;
    default_aac_params.sbr = 1;

	status = aac_encoder_open(&aac->hAacEnc, attr, &default_aac_params);

	aac->enc_ready = PJ_TRUE;

	/* Create Decoder */
	aac->hAacDec = aacDecoder_Open(TT_MP4_RAW, 1);
    UCHAR* configBuffers[2];
    UINT configBufferSizes[2] = {0};
    for (i = 0; i < attr->setting.enc_fmtp.cnt; ++i) {
        if (pj_stricmp(&attr->setting.enc_fmtp.param[i].name,
                &STR_FMTP_CONFIG) == 0) {
            pj_str_t value = attr->setting.enc_fmtp.param[i].val;
            configBufferSizes[0] = value.slen / 2;
            configBuffers[0] = pj_pool_alloc(aac->pool, configBufferSizes[0] * sizeof(UCHAR));
            for(i = 0; i < configBufferSizes[0]; i++){
                unsigned v = 0;
                unsigned ptr_idx = 2 * i;
                if(ptr_idx < value.slen){
                    v |= (hex_digit_to_val(value.ptr[ptr_idx]) << 4);
                }
                ptr_idx++;
                if(ptr_idx < value.slen){
                    v |= (hex_digit_to_val(value.ptr[ptr_idx]));
                }
                configBuffers[0][i] = v;
            }
            break;
        }
    }
	aacDecoder_ConfigRaw(aac->hAacDec, configBuffers, configBufferSizes);
    aacDecoder_SetParam(aac->hAacDec, AAC_PCM_OUTPUT_CHANNELS, attr->info.channel_cnt);

    // TODO

	aac->dec_ready = PJ_TRUE;
	PJ_LOG(4, (THIS_FILE, "Decoder ready"));

	aac->channel_cnt = attr->info.channel_cnt;
	aac->clock_rate  = attr->info.clock_rate;
    aac->pcm_frame_size = (attr->info.channel_cnt * attr->info.clock_rate
            * attr->info.pcm_bits_per_sample * attr->info.frm_ptime) / 8000;

	return PJ_SUCCESS;
}

/*
 * Close codec.
 */
static pj_status_t aac_codec_close(pjmedia_codec *codec) {
	struct aac_private *aac;
	aac = (struct aac_private*) codec->codec_data;

	aacEncClose(&aac->hAacEnc);
	aac->enc_ready = PJ_FALSE;

	aacDecoder_Close(aac->hAacDec);
	aac->dec_ready = PJ_FALSE;

	return PJ_SUCCESS;
}

/*
 * Modify codec settings.
 */
static pj_status_t aac_codec_modify(pjmedia_codec *codec,
		const pjmedia_codec_param *attr) {
    PJ_TODO(implement_silk_codec_modify);

    PJ_UNUSED_ARG(codec);
    PJ_UNUSED_ARG(attr);

	return PJ_SUCCESS;
}

/*
 * Encode frame.
 */
static pj_status_t aac_codec_encode(pjmedia_codec *codec,
		const struct pjmedia_frame *input, unsigned output_buf_len,
		struct pjmedia_frame *output) {
	struct aac_private *aac;
    AACENC_BufDesc in_buf   = { 0 }, out_buf = { 0 };
    AACENC_InArgs  in_args  = { 0 };
    AACENC_OutArgs out_args = { 0 };
    int in_buffer_identifier = IN_AUDIO_DATA;
    int in_buffer_size, in_buffer_element_size;
    int out_buffer_identifier = OUT_BITSTREAM_DATA;
    int out_buffer_size, out_buffer_element_size;
    void *in_ptr, *out_ptr;
    pj_uint8_t* p;
    pj_uint8_t* sp;
    AACENC_ERROR       error;

    PJ_ASSERT_RETURN(codec && input && output, PJ_EINVAL);

    aac = (struct aac_private*) codec->codec_data;


    in_ptr = input->buf;
    in_buffer_size           = input->size;
    in_buffer_element_size   = 2;
    in_args.numInSamples     = input->size / 2;
    in_buf.numBufs           = 1;
    in_buf.bufs              = &in_ptr;
    in_buf.bufferIdentifiers = &in_buffer_identifier;
    in_buf.bufSizes          = &in_buffer_size;
    in_buf.bufElSizes        = &in_buffer_element_size;

    p = (pj_uint8_t*)output->buf;
    *p++ = 0;
    *p++ = 16;
    sp = p;
    p++; p++;
    out_ptr = p;
    out_buffer_size           = output_buf_len - 4;
    out_buffer_element_size   = 1;
    out_buf.numBufs           = 1;
    out_buf.bufs              = &out_ptr;
    out_buf.bufferIdentifiers = &out_buffer_identifier;
    out_buf.bufSizes          = &out_buffer_size;
    out_buf.bufElSizes        = &out_buffer_element_size;


    output->type = PJMEDIA_FRAME_TYPE_NONE;
    output->size = 0;
    output->timestamp = input->timestamp;

	error = aacEncEncode(aac->hAacEnc, &in_buf, &out_buf, &in_args, &out_args);
    if (error == AACENC_OK) {
        if(out_args.numOutBytes > 0){
            pj_uint16_t size = (out_args.numOutBytes << 3);
            output->size = out_args.numOutBytes + 4;
            output->type = PJMEDIA_FRAME_TYPE_AUDIO;
            *sp++ = (size & 0xFF00) >> 8;
            *sp = (size & 0x00FF);
        } else {
            PJ_LOG(2, (THIS_FILE, "AAC Encoder had %d (%d) > %d", out_args.numInSamples, input->size, out_args.numOutBytes));
        }
    } else {
        PJ_LOG(1,
                (THIS_FILE, "AAC Encoder error %d - inSize %d", error, input->size));
    }

	return PJ_SUCCESS;
}

/*
 * Get frames in the packet.
 */

static pj_status_t aac_codec_parse(pjmedia_codec *codec, void *pkt,
		pj_size_t pkt_size, const pj_timestamp *ts, unsigned *frame_cnt,
		pjmedia_frame frames[]) {
	struct aac_private *aac;
    unsigned i;
    pj_uint8_t* p;
    pj_uint16_t au_headers_length = 0, au_header = 0, au_size = 0;

	PJ_ASSERT_RETURN(frame_cnt, PJ_EINVAL);

	aac = (struct aac_private*) codec->codec_data;

    p = (pj_uint8_t*) pkt;
    // For now just do not use AU-headers-length should be done later
    au_headers_length |= (*p++ << 8);
    au_headers_length |= (*p++);
    if(au_headers_length != 16){
        *frame_cnt = 0;
        PJ_LOG(1, (THIS_FILE, "Unsupported packet for now %d", au_headers_length));
        return PJMEDIA_CODEC_EFAILED;
    }
    au_header |= (*p++ << 8);
    au_header |= (*p++);
    au_size = (au_header >> 3); /* 16bits of headers with 13 of size and 3 of index (assume 0 for now) */

    if(au_size > pkt_size - 4){
        PJ_LOG(1, (THIS_FILE, "Truncated packet or invalid size %d - %d", au_size, pkt_size - 4));
        return PJMEDIA_CODEC_EFAILED;
    }
    //PJ_LOG(4, (THIS_FILE, "Parsed packet : size %d", au_size));

	// Assume just one frame per packet for now
	*frame_cnt = 1;
    frames[0].type = PJMEDIA_FRAME_TYPE_AUDIO;
    frames[0].bit_info = 0;
    frames[0].buf = p;
    frames[0].size = au_size;
    frames[0].timestamp.u64 = ts->u64;

	return PJ_SUCCESS;
}

static pj_status_t aac_codec_decode(pjmedia_codec *codec,
		const struct pjmedia_frame *input, unsigned output_buf_len,
		struct pjmedia_frame *output) {
	struct aac_private *aac;
	int bytesValid;
    AACENC_ERROR       error;
    UCHAR* p;

    PJ_ASSERT_RETURN(codec && input && output_buf_len && output, PJ_EINVAL);

	aac = (struct aac_private*) codec->codec_data;
	p = (UCHAR*) input->buf;
	bytesValid = input->size;
	error = aacDecoder_Fill(aac->hAacDec, &p, &input->size, &bytesValid);
	if(error != AAC_DEC_OK){
        PJ_LOG(1, (THIS_FILE, "Error while filling decoder buffer %d", error));
        output->type = PJMEDIA_TYPE_NONE;
        output->buf = NULL;
        output->size = 0;
        return PJMEDIA_CODEC_EFAILED;
	}

	error = aacDecoder_DecodeFrame(aac->hAacDec, output->buf, output_buf_len, 0);

    if(error != AAC_DEC_OK){
        PJ_LOG(1, (THIS_FILE, "Error while decoding frame %d", error));
        output->type = PJMEDIA_TYPE_NONE;
        output->buf = NULL;
        output->size = 0;
        return PJMEDIA_CODEC_EFAILED;
    }
    output->size = aac->pcm_frame_size;
	output->type = PJMEDIA_TYPE_AUDIO;
	output->timestamp = input->timestamp;

	return PJ_SUCCESS;
}

/*
 * Recover lost frame.
 */
static pj_status_t aac_codec_recover(pjmedia_codec *codec,
		unsigned output_buf_len, struct pjmedia_frame *output) {
	struct aac_private *aac;
	int ret = 0;
	int frame_size;

	PJ_ASSERT_RETURN(output, PJ_EINVAL);
	aac = (struct aac_private*) codec->codec_data;

    output->type = PJMEDIA_FRAME_TYPE_NONE;
    output->buf = NULL;
    output->size = 0;

	return PJ_SUCCESS;
}

#endif
