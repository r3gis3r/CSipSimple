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


#if defined(PJMEDIA_HAS_OPUS_CODEC) && (PJMEDIA_HAS_OPUS_CODEC!=0)
#include "pj_opus.h"
#include <opus.h>

#define FRAME_LENGTH_MS		20
#define USE_PLC 				1

#define THIS_FILE       "pj_opus.c"


/* Prototypes for OPUS factory */
static pj_status_t opus_test_alloc( pjmedia_codec_factory *factory,
				    const pjmedia_codec_info *id );
static pj_status_t opus_default_attr( pjmedia_codec_factory *factory,
				      const pjmedia_codec_info *id,
				      pjmedia_codec_param *attr );
static pj_status_t opus_enum_codecs (pjmedia_codec_factory *factory,
				     unsigned *count,
				     pjmedia_codec_info codecs[]);
static pj_status_t opus_alloc_codec( pjmedia_codec_factory *factory,
				     const pjmedia_codec_info *id,
				     pjmedia_codec **p_codec);
static pj_status_t opus_dealloc_codec( pjmedia_codec_factory *factory,
				       pjmedia_codec *codec );

/* Prototypes for OPUS implementation. */
static pj_status_t  opus_codec_init( pjmedia_codec *codec,
			       pj_pool_t *pool );
static pj_status_t  opus_codec_open( pjmedia_codec *codec,
			       pjmedia_codec_param *attr );
static pj_status_t  opus_codec_close( pjmedia_codec *codec );
static pj_status_t  opus_codec_modify(pjmedia_codec *codec,
			        const pjmedia_codec_param *attr );
static pj_status_t  opus_codec_parse(pjmedia_codec *codec,
			       void *pkt,
			       pj_size_t pkt_size,
			       const pj_timestamp *timestamp,
			       unsigned *frame_cnt,
			       pjmedia_frame frames[]);
static pj_status_t  opus_codec_encode( pjmedia_codec *codec,
				 const struct pjmedia_frame *input,
				 unsigned output_buf_len,
				 struct pjmedia_frame *output);
static pj_status_t  opus_codec_decode( pjmedia_codec *codec,
				 const struct pjmedia_frame *input,
				 unsigned output_buf_len,
				 struct pjmedia_frame *output);
#if !PLC_DISABLED
static pj_status_t  opus_codec_recover( pjmedia_codec *codec,
				  unsigned output_buf_len,
				  struct pjmedia_frame *output);
#endif


enum
{
    PARAM_NB,   /* Index for narrowband parameter.	*/
    PARAM_MB,	/* Index for medium parameter.	*/
    PARAM_WB,	/* Index for wideband parameter.	*/
    PARAM_SWB,	/* Index for ultra-wideband parameter	*/
    PARAM_FB,	/* Index for super-wideband parameter	*/
};

/* Opus default parameter */
struct opus_param
{
    int		     enabled;		/* Is this mode enabled?	    */
    int		     pt;		/* Payload type.		    */
    unsigned	 clock_rate;	/* Default sampling rate to be used.*/
    int		     packet_size_ms;	/* packet size ms.		    */
    pj_uint32_t  bitrate;		/* Bit rate for current mode.	    */
    pj_uint32_t  max_bitrate;	/* Max bit rate for current mode.   */
    int 		complexity;
    char	     bitrate_str[8];
};

/* Definition for OPUS codec operations. */
static pjmedia_codec_op opus_op =
{
    &opus_codec_init,
    &opus_codec_open,
    &opus_codec_close,
    &opus_codec_modify,
    &opus_codec_parse,
    &opus_codec_encode,
    &opus_codec_decode,
    &opus_codec_recover
};

/* Definition for OPUS codec factory operations. */
static pjmedia_codec_factory_op opus_factory_op =
{
    &opus_test_alloc,
    &opus_default_attr,
    &opus_enum_codecs,
    &opus_alloc_codec,
    &opus_dealloc_codec,
    &pjmedia_codec_opus_deinit
};

/* OPUS factory private data */
static struct opus_factory
{
    pjmedia_codec_factory	base;
    pjmedia_endpt	       *endpt;
    pj_pool_t		       *pool;
    pj_mutex_t		       *mutex;
    pjmedia_codec	     codec_list;
    struct opus_param	    opus_param[5];
} opus_factory;


/* OPUS codec private data. */
struct opus_private
{
    int			 param_id;	    /**< Index to speex param.	*/
    pj_pool_t   *pool;        /**< Pool for each instance.    */

   // char		 obj_name[PJ_MAX_OBJ_NAME];

    pj_bool_t		 enc_ready;
    OpusEncoder* psEnc;

    pj_bool_t		 dec_ready;
    OpusDecoder* psDec;
};

int opus_get_type_for_clock_rate(int clock_rate) {
	if (clock_rate <= opus_factory.opus_param[PARAM_NB].clock_rate) {
		return PARAM_NB;
	} else if (clock_rate <= opus_factory.opus_param[PARAM_MB].clock_rate) {
		return PARAM_MB;
	} else if (clock_rate <= opus_factory.opus_param[PARAM_WB].clock_rate) {
		return PARAM_WB;
	} else if (clock_rate <= opus_factory.opus_param[PARAM_SWB].clock_rate) {
		return PARAM_SWB;
	}
	return PARAM_FB;
}

int opus_to_pjsip_error_code(int opus_error){
	switch(opus_error){
	case OPUS_BAD_ARG:
		/** One or more invalid/out of range arguments @hideinitializer*/
		return PJ_EINVAL;
	case OPUS_BUFFER_TOO_SMALL:
		/** The mode struct passed is invalid @hideinitializer*/
		return PJMEDIA_CODEC_EPCMTOOSHORT;
	case OPUS_INTERNAL_ERROR:
		/** An internal error was detected @hideinitializer*/
		return PJMEDIA_CODEC_EFAILED;
	case OPUS_INVALID_PACKET:
		/** The compressed data passed is corrupted @hideinitializer*/
		return PJMEDIA_CODEC_EBADBITSTREAM;
	case OPUS_UNIMPLEMENTED:
		/** Invalid/unsupported request number @hideinitializer*/
		return PJ_ENOTSUP;
	case OPUS_INVALID_STATE:
		/** An encoder or decoder structure is invalid or already freed @hideinitializer*/
		return PJ_EINVALIDOP;
	case OPUS_ALLOC_FAIL:
		/** Memory allocation has failed @hideinitializer*/
		return PJMEDIA_CODEC_EFAILED;
	}
	return PJMEDIA_ERROR;
}

PJ_DEF(pj_status_t) pjmedia_codec_opus_init(pjmedia_endpt *endpt)
{
    pjmedia_codec_mgr *codec_mgr;
    pj_status_t status;

    if (opus_factory.endpt != NULL) {
	/* Already initialized. */
	return PJ_SUCCESS;
    }

    /* Init factory */
    opus_factory.base.op = &opus_factory_op;
    opus_factory.base.factory_data = NULL;
    opus_factory.endpt = endpt;

    /* Create pool */
    opus_factory.pool = pjmedia_endpt_create_pool(endpt, "opus codecs", 4000, 4000);
    if (!opus_factory.pool)
	return PJ_ENOMEM;

    /* Init list */
    pj_list_init(&opus_factory.codec_list);

    /* Create mutex. */
    status = pj_mutex_create_simple(opus_factory.pool, "opus codecs",
				    &opus_factory.mutex);
    if (status != PJ_SUCCESS)
	goto on_error;
    PJ_LOG(5, (THIS_FILE, "Init opus"));

    /* Table from opus rfc
 	  +-------+---------+-----------+
	  |  Mode | fs (Hz) | BR (kbps) |
	  +-------+---------+-----------+
	  | voice |   8000  |   6 - 20  |
	  | voice |  12000  |   7 - 25  |
	  | voice |  16000  |   8 - 30  |
	  | voice |  24000  |  18 - 28  |
	  | voice |  48000  |  24 - 32  |
	  +-------+---------+-----------+
    */
    struct opus_param *opus_param;
    opus_param = &opus_factory.opus_param[PARAM_NB];
    opus_param->pt = PJMEDIA_RTP_PT_OPUS_NB;
    opus_param->clock_rate = 8000;
    opus_param->bitrate = 13000;
	opus_param->max_bitrate = 20000;
	pj_utoa(opus_param->bitrate, opus_param->bitrate_str);
	opus_param->packet_size_ms = FRAME_LENGTH_MS;
	opus_param->complexity = 2;
	opus_param->enabled = 1;

    opus_param = &opus_factory.opus_param[PARAM_MB];
    opus_param->pt = PJMEDIA_RTP_PT_OPUS_MB;
    opus_param->clock_rate = 12000;
    opus_param->bitrate = 16000;
    opus_param->max_bitrate = 25000;
    pj_utoa(opus_param->bitrate, opus_param->bitrate_str);
    opus_param->packet_size_ms = FRAME_LENGTH_MS;
    opus_param->complexity = 2;
    opus_param->enabled = 1;

    opus_param = &opus_factory.opus_param[PARAM_WB];
    opus_param->pt = PJMEDIA_RTP_PT_OPUS_WB;
	opus_param->clock_rate = 16000;
	opus_param->bitrate = 19000;
	opus_param->max_bitrate = 30000;
	pj_utoa(opus_param->bitrate, opus_param->bitrate_str);
	opus_param->packet_size_ms = FRAME_LENGTH_MS;
	opus_param->complexity = 2;
	opus_param->enabled = 1;

    opus_param = &opus_factory.opus_param[PARAM_SWB];
    opus_param->pt = PJMEDIA_RTP_PT_OPUS_SWB;
    opus_param->clock_rate = 24000;
    opus_param->bitrate = 23000;
    opus_param->max_bitrate = 28000;
	pj_utoa(opus_param->bitrate, opus_param->bitrate_str);
	opus_param->packet_size_ms = FRAME_LENGTH_MS;
	opus_param->complexity = 2;
	opus_param->enabled = 1;

    opus_param = &opus_factory.opus_param[PARAM_FB];
    opus_param->pt = PJMEDIA_RTP_PT_OPUS_FB;
    opus_param->clock_rate = 48000;
    opus_param->bitrate = 28000;
    opus_param->max_bitrate = 32000;
	pj_utoa(opus_param->bitrate, opus_param->bitrate_str);
	opus_param->packet_size_ms = FRAME_LENGTH_MS;
	opus_param->complexity = 2;
	opus_param->enabled = 1;


    /* Get the codec manager. */
    codec_mgr = pjmedia_endpt_get_codec_mgr(endpt);
    if (!codec_mgr) {
	return PJ_EINVALIDOP;
    }

    PJ_LOG(5, (THIS_FILE, "Init opus > DONE"));

    /* Register codec factory to endpoint. */
    status = pjmedia_codec_mgr_register_factory(codec_mgr,
						&opus_factory.base);
    if (status != PJ_SUCCESS)
	return status;

    return PJ_SUCCESS;

on_error:
    if (opus_factory.mutex) {
	pj_mutex_destroy(opus_factory.mutex);
	opus_factory.mutex = NULL;
    }
    if (opus_factory.pool) {
	pj_pool_release(opus_factory.pool);
	opus_factory.pool = NULL;
    }

    return status;
}

/*
 * Unregister OPUS codec factory from pjmedia endpoint and deinitialize
 * the OPUS codec library.
 */
PJ_DEF(pj_status_t) pjmedia_codec_opus_deinit(void)
{
    pjmedia_codec_mgr *codec_mgr;
    pj_status_t status;

    if (opus_factory.endpt == NULL) {
	/* Not registered. */
	return PJ_SUCCESS;
    }

    /* Lock mutex. */
    pj_mutex_lock(opus_factory.mutex);

    /* Get the codec manager. */
    codec_mgr = pjmedia_endpt_get_codec_mgr(opus_factory.endpt);
    if (!codec_mgr) {
	opus_factory.endpt = NULL;
	pj_mutex_unlock(opus_factory.mutex);
	return PJ_EINVALIDOP;
    }

    /* Unregister opus codec factory. */
    status = pjmedia_codec_mgr_unregister_factory(codec_mgr,
						  &opus_factory.base);
    opus_factory.endpt = NULL;

    /* Destroy mutex. */
    pj_mutex_destroy(opus_factory.mutex);
    opus_factory.mutex = NULL;


    /* Release pool. */
    pj_pool_release(opus_factory.pool);
    opus_factory.pool = NULL;

    return status;
}

/*
 * Check if factory can allocate the specified codec.
 */
static pj_status_t opus_test_alloc(pjmedia_codec_factory *factory,
				   const pjmedia_codec_info *info )
{
    const pj_str_t opus_tag = {"opus", 4};
    unsigned i;

    PJ_UNUSED_ARG(factory);
    PJ_ASSERT_RETURN(factory==&opus_factory.base, PJ_EINVAL);


    /* Type MUST be audio. */
    if (info->type != PJMEDIA_TYPE_AUDIO)
	return PJMEDIA_CODEC_EUNSUP;

    /* Check encoding name. */
    if (pj_stricmp(&info->encoding_name, &opus_tag) != 0)
	return PJMEDIA_CODEC_EUNSUP;

    /* Channel count must be one */
    if (info->channel_cnt != 1)
	return PJMEDIA_CODEC_EUNSUP;

    /* Check clock-rate */
    for (i=0; i<PJ_ARRAY_SIZE(opus_factory.opus_param); ++i) {
	if (info->clock_rate == opus_factory.opus_param[i].clock_rate) {
	    /* Okay, let's Speex! */
	    return PJ_SUCCESS;
	}
    }
    /* Clock rate not supported */
    return PJMEDIA_CODEC_EUNSUP;
}

/*
 * Generate default attribute.
 */
static pj_status_t opus_default_attr( pjmedia_codec_factory *factory,
				      const pjmedia_codec_info *id,
				      pjmedia_codec_param *attr )
{
    struct opus_param *opus_param;
    PJ_UNUSED_ARG(factory);
    PJ_LOG(5, (THIS_FILE, "opus default attr"));
    pj_bzero(attr, sizeof(pjmedia_codec_param));

    opus_param = &opus_factory.opus_param[opus_get_type_for_clock_rate(id->clock_rate)];

    attr->info.channel_cnt = 1;
	attr->info.clock_rate = opus_param->clock_rate;
	attr->info.avg_bps = opus_param->bitrate;
	attr->info.max_bps = opus_param->max_bitrate;
	attr->info.frm_ptime = opus_param->packet_size_ms;
	attr->info.pcm_bits_per_sample = 16;

	attr->info.pt = (pj_uint8_t) id->pt;

    attr->setting.frm_per_pkt = 1;
    attr->setting.vad = 0;
    attr->setting.plc = USE_PLC;

    if(attr->setting.plc == 1){
		attr->setting.dec_fmtp.cnt = 1;
		// Inform PLC
		attr->setting.dec_fmtp.param[0].name = pj_str("useinbandfec");
		attr->setting.dec_fmtp.param[0].val = pj_str("1");
    }
	// Inform Bitrate
	/*
	attr->setting.dec_fmtp.param[1].name = pj_str("maxaveragebitrate");
	attr->setting.dec_fmtp.param[1].val = pj_str(mode->bitrate_str);
	*/

    return PJ_SUCCESS;
}

/*
 * Enum codecs supported by this factory.
 */
static pj_status_t opus_enum_codecs(pjmedia_codec_factory *factory,
				    unsigned *count,
				    pjmedia_codec_info codecs[])
{
    unsigned max;
    int i;  /* Must be signed */
    PJ_LOG(5, (THIS_FILE, "opus enum codecs"));

    PJ_UNUSED_ARG(factory);
    PJ_ASSERT_RETURN(codecs && *count > 0, PJ_EINVAL);

    max = *count;
    *count = 0;

    for (i=PJ_ARRAY_SIZE(opus_factory.opus_param)-1; i>=0 && *count<max; --i) {

    	if (!opus_factory.opus_param[i].enabled){
    	    continue;
    	}
    	PJ_LOG(5, (THIS_FILE, "Add codec %d", opus_factory.opus_param[i].clock_rate));

    	pj_bzero(&codecs[*count], sizeof(pjmedia_codec_info));
    	codecs[*count].encoding_name = pj_str("opus");
    	codecs[*count].pt = opus_factory.opus_param[i].pt;
    	codecs[*count].type = PJMEDIA_TYPE_AUDIO;
    	codecs[*count].clock_rate = opus_factory.opus_param[i].clock_rate;
    	codecs[*count].channel_cnt = 1;

    	++*count;
	}

	return PJ_SUCCESS;

}


/*
 * Allocate a new OPUS codec instance.
 */
static pj_status_t opus_alloc_codec(pjmedia_codec_factory *factory,
				    const pjmedia_codec_info *id,
				    pjmedia_codec **p_codec)
{
    pjmedia_codec *codec;
    struct opus_private *opus;

    PJ_ASSERT_RETURN(factory && id && p_codec, PJ_EINVAL);
    PJ_ASSERT_RETURN(factory == &opus_factory.base, PJ_EINVAL);


    pj_mutex_lock(opus_factory.mutex);

    /* Get free nodes, if any. */
    if (!pj_list_empty(&opus_factory.codec_list)) {
		codec = opus_factory.codec_list.next;
		pj_list_erase(codec);
    } else {
		codec = PJ_POOL_ZALLOC_T(opus_factory.pool, pjmedia_codec);
		PJ_ASSERT_RETURN(codec != NULL, PJ_ENOMEM);
		codec->op = &opus_op;
		codec->factory = factory;
		codec->codec_data = pj_pool_alloc(opus_factory.pool,
						  sizeof(struct opus_private));
    }

    pj_mutex_unlock(opus_factory.mutex);

    opus = (struct opus_private*) codec->codec_data;
    opus->enc_ready = PJ_FALSE;
    opus->dec_ready = PJ_FALSE;
    opus->param_id = opus_get_type_for_clock_rate(id->clock_rate);

    /* Create pool for codec instance */
    opus->pool = pjmedia_endpt_create_pool(opus_factory.endpt, "opuscodec", 512, 512);

    *p_codec = codec;
    return PJ_SUCCESS;
}



/*
 * Free codec.
 */
static pj_status_t opus_dealloc_codec( pjmedia_codec_factory *factory,
				      pjmedia_codec *codec )
{
    struct opus_private *opus;

    PJ_ASSERT_RETURN(factory && codec, PJ_EINVAL);
    PJ_UNUSED_ARG(factory);
    PJ_ASSERT_RETURN(factory == &opus_factory.base, PJ_EINVAL);

    opus = (struct opus_private*) codec->codec_data;

    /* Close codec, if it's not closed. */
    if (opus->enc_ready == PJ_TRUE || opus->dec_ready == PJ_TRUE) {
    	opus_codec_close(codec);
    }


    /* Put in the free list. */
    pj_mutex_lock(opus_factory.mutex);
    pj_list_push_front(&opus_factory.codec_list, codec);
    pj_mutex_unlock(opus_factory.mutex);

    pj_pool_release(opus->pool);
    opus->pool = NULL;


    return PJ_SUCCESS;
}

/*
 * Init codec.
 */
static pj_status_t opus_codec_init(pjmedia_codec *codec,
				   pj_pool_t *pool )
{
    PJ_UNUSED_ARG(codec);
    PJ_UNUSED_ARG(pool);
    return PJ_SUCCESS;
}


/*
 * Open codec.
 */
static pj_status_t opus_codec_open(pjmedia_codec *codec,
				   pjmedia_codec_param *attr )
{

    pj_status_t status;
    struct opus_private *opus;
    int id, ret = 0;
    unsigned i;
    struct opus_param params;
    int encSizeBytes, decSizeBytes, API_fs_Hz, maxBitRate;
	int useInBandFEC = 0;
	unsigned channels = 1;
    const pj_str_t STR_FMTP_USE_INBAND_FEC = {"useinbandfec", 12};
    const pj_str_t STR_FMTP_MAX_AVERAGE_BITRATE = {"maxaveragebitrate", 17};

    opus = (struct opus_private*) codec->codec_data;
    id = opus->param_id;

    pj_assert(opus != NULL);
    pj_assert(opus->enc_ready == PJ_FALSE &&
   	      opus->dec_ready == PJ_FALSE);

    params = opus_factory.opus_param[id];

    //PJ_LOG(4, (THIS_FILE, "Open opus codec @ %d", params.clock_rate));
    /* default settings */
	API_fs_Hz = params.clock_rate;
	maxBitRate = 0;
    /* Check fmtp params */
    for (i = 0; i < attr->setting.enc_fmtp.cnt; ++i) {
		if (pj_stricmp(&attr->setting.enc_fmtp.param[i].name,
				   &STR_FMTP_USE_INBAND_FEC) == 0)	{
			useInBandFEC = (pj_uint8_t)
				  (pj_strtoul(&attr->setting.enc_fmtp.param[i].val));
			break;
		}else if(pj_stricmp(&attr->setting.enc_fmtp.param[i].name,
				   &STR_FMTP_MAX_AVERAGE_BITRATE) == 0)	{
			int remoteBitRate = (int)(pj_strtoul(&attr->setting.enc_fmtp.param[i].val));
			if(remoteBitRate < maxBitRate || maxBitRate == 0){
				maxBitRate = remoteBitRate;
			}
		}
    }

    /* Create Encoder */
    encSizeBytes = opus_encoder_get_size(channels);
    opus->psEnc = pj_pool_zalloc(opus->pool, encSizeBytes);
    ret = opus_encoder_init(opus->psEnc, API_fs_Hz, channels, OPUS_APPLICATION_VOIP);
    if(ret){
		PJ_LOG(1, (THIS_FILE, "Unable to init encoder : %d", ret));
		return PJ_EINVAL;
	}

    /* Set Encoder parameters */
    if(maxBitRate >= 6000 && maxBitRate <= 510000){
        opus_encoder_ctl(opus->psEnc, OPUS_SET_BITRATE(maxBitRate));
    }
    opus_encoder_ctl(opus->psEnc, OPUS_SET_INBAND_FEC(useInBandFEC));
    opus_encoder_ctl(opus->psEnc, OPUS_SET_COMPLEXITY(params.complexity));
    opus_encoder_ctl(opus->psEnc, OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE));

    opus->enc_ready = PJ_TRUE;

    //Decoder
    /* Create decoder */
    decSizeBytes = opus_decoder_get_size(channels);
	opus->psDec = pj_pool_zalloc(opus->pool, decSizeBytes);
	ret = opus_decoder_init(opus->psDec, API_fs_Hz, channels);
	if(ret){
		PJ_LOG(1, (THIS_FILE, "Unable to init dencoder : %d", ret));
		return PJ_EINVAL;
	}

    opus->dec_ready = PJ_TRUE;

    return PJ_SUCCESS;
}

/*
 * Close codec.
 */
static pj_status_t opus_codec_close( pjmedia_codec *codec )
{
    struct opus_private *opus;
    opus = (struct opus_private*) codec->codec_data;

    opus->enc_ready = PJ_FALSE;
    opus->dec_ready = PJ_FALSE;

    PJ_LOG(5, (THIS_FILE, "OPUS codec closed"));
    return PJ_SUCCESS;
}

/*
 * Modify codec settings.
 */
static pj_status_t  opus_codec_modify(pjmedia_codec *codec,
				      const pjmedia_codec_param *attr )
{

    return PJ_SUCCESS;
}


/*
 * Encode frame.
 */
static pj_status_t opus_codec_encode(pjmedia_codec *codec,
				     const struct pjmedia_frame *input,
				     unsigned output_buf_len,
				     struct pjmedia_frame *output)
{
	struct opus_private *opus;
	int ret;
    pj_assert(opus && input && output);

    opus = (struct opus_private*) codec->codec_data;

    /* Encode */
    output->size = 0;
    //PJ_LOG(4, (THIS_FILE, "Input size : %d - Encoder packet size  %d", input->size, opus->enc.packetSize));

    //That's fine with pjmedia cause input size is always already the good size
	ret = opus_encode(opus->psEnc,
			(opus_int16*)input->buf, ( input->size >> 1 ),
			(unsigned char *)output->buf, output_buf_len);
	if( ret < 0 ) {
		PJ_LOG(1, (THIS_FILE, "Impossible to encode packet %d", ret));
		return opus_to_pjsip_error_code(ret);
	}else{
		output->size = ret;
	}
    output->type = PJMEDIA_FRAME_TYPE_AUDIO;
    output->timestamp = input->timestamp;
    //PJ_LOG(4, (THIS_FILE, "Encoder packet size %d for input %d ouput max len %d", output->size, input->size, output_buf_len));

    return PJ_SUCCESS;
}


/*
 * Get frames in the packet.
 */

static pj_status_t  opus_codec_parse( pjmedia_codec *codec,
				     void *pkt,
				     pj_size_t pkt_size,
				     const pj_timestamp *ts,
				     unsigned *frame_cnt,
				     pjmedia_frame frames[])
{
	    unsigned count;
	    PJ_UNUSED_ARG(codec);
	    PJ_ASSERT_RETURN(frame_cnt, PJ_EINVAL);

	    // The decoder of opus is capable to parse itself, so consider all as a single input
		frames[0].type = PJMEDIA_FRAME_TYPE_AUDIO;
		frames[0].buf = pkt;
		frames[0].size = pkt_size;
		frames[0].timestamp.u64 = ts->u64;
	    *frame_cnt = 1;
	    return PJ_SUCCESS;
}



static pj_status_t opus_codec_decode(pjmedia_codec *codec,
				     const struct pjmedia_frame *input,
				     unsigned output_buf_len,
				     struct pjmedia_frame *output)
{
    int ret = 0;
    struct opus_private *opus;

    PJ_ASSERT_RETURN(input && output, PJ_EINVAL);

    opus = (struct opus_private*) codec->codec_data;
    //PJ_LOG(4, (THIS_FILE, "Decode opus frame %d to %d", input->size, output->size));
    //For opus parsing need to decode...
    ret = opus_decode( opus->psDec,
    		(const unsigned char *) input->buf, //Packet
    		(opus_int32) input->size, //Packet size
    		output->buf,
    		(int)(output->size >> 1),
    		USE_PLC /* decode FEC */);
	if(ret <= 0){
		PJ_LOG(1, (THIS_FILE, "Failed to decode opus frame : %d", ret));
		output->type = PJMEDIA_FRAME_TYPE_NONE;
		output->buf = NULL;
		output->size = 0;
	}else{
		output->size = ret;
		output->type = PJMEDIA_FRAME_TYPE_AUDIO;
		output->timestamp = input->timestamp;
	}

    return PJ_SUCCESS;
}

/*
 * Recover lost frame.
 */
static pj_status_t  opus_codec_recover(pjmedia_codec *codec,
				      unsigned output_buf_len,
				      struct pjmedia_frame *output)
{
	struct opus_private *opus;
    int ret = 0;

    PJ_ASSERT_RETURN(output, PJ_EINVAL);

	opus = (struct opus_private*) codec->codec_data;

    PJ_LOG(4, (THIS_FILE, "Recover opus frame"));

    /* Decode */
#if USE_PLC == 1
	ret = opus_decode( opus->psDec,
			(const unsigned char *) NULL,
			0,
			output->buf,
			(output_buf_len >> 1),
			0);
#endif
	if(ret < 0){
		PJ_LOG(1, (THIS_FILE, "Failed to recover opus frame %d", ret));
		return PJ_EINVAL;
	}else if(ret == 0){
		PJ_LOG(4, (THIS_FILE, "Empty frame recovered %d", ret));
		output->type = PJMEDIA_FRAME_TYPE_NONE;
		output->buf = NULL;
		output->size = 0;
	}else{
		output->size = ret;
	    output->type = PJMEDIA_FRAME_TYPE_AUDIO;
	}


    return PJ_SUCCESS;
}



#endif
