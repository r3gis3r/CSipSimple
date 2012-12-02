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
#include <pjmedia-codec/gsm.h>
#include <pjmedia/codec.h>
#include <pjmedia/errno.h>
#include <pjmedia/endpoint.h>
#include <pjmedia/plc.h>
#include <pjmedia/port.h>
#include <pjmedia/silencedet.h>
#include <pj/assert.h>
#include <pj/pool.h>
#include <pj/string.h>
#include <pj/os.h>
#include <pj/log.h>

/*
 * Only build this file if PJMEDIA_HAS_GSM_CODEC != 0
 */
#if defined(PJMEDIA_HAS_CODEC2_CODEC) && PJMEDIA_HAS_CODEC2_CODEC != 0

#define PLC_DISABLED	0
#define THIS_FILE       "codec2.c"

#define BITS_SIZE	((CODEC2_BITS_PER_FRAME + 7) / 8)

#include <codec2.h>
#include <pj_codec2.h>

/* Prototypes for factory */
static pj_status_t codec2_test_alloc( pjmedia_codec_factory *factory,
				   const pjmedia_codec_info *id );
static pj_status_t codec2_default_attr( pjmedia_codec_factory *factory,
				     const pjmedia_codec_info *id, 
				     pjmedia_codec_param *attr );
static pj_status_t codec2_enum_codecs( pjmedia_codec_factory *factory,
				    unsigned *count, 
				    pjmedia_codec_info codecs[]);
static pj_status_t codec2_alloc_codec( pjmedia_codec_factory *factory,
				    const pjmedia_codec_info *id, 
				    pjmedia_codec **p_codec);
static pj_status_t codec2_dealloc_codec( pjmedia_codec_factory *factory,
				      pjmedia_codec *codec );

/* Prototypes for implementation. */
static pj_status_t  codec2_codec_init( pjmedia_codec *codec,
				    pj_pool_t *pool );
static pj_status_t  codec2_codec_open( pjmedia_codec *codec,
				    pjmedia_codec_param *attr );
static pj_status_t  codec2_codec_close( pjmedia_codec *codec );
static pj_status_t  codec2_codec_modify(pjmedia_codec *codec,
				     const pjmedia_codec_param *attr );
static pj_status_t  codec2_codec_parse( pjmedia_codec *codec,
				     void *pkt,
				     pj_size_t pkt_size,
				     const pj_timestamp *ts,
				     unsigned *frame_cnt,
				     pjmedia_frame frames[]);
static pj_status_t  codec2_codec_encode( pjmedia_codec *codec,
				      const struct pjmedia_frame *input,
				      unsigned output_buf_len, 
				      struct pjmedia_frame *output);
static pj_status_t  codec2_codec_decode( pjmedia_codec *codec,
				      const struct pjmedia_frame *input,
				      unsigned output_buf_len, 
				      struct pjmedia_frame *output);
#if !PLC_DISABLED
static pj_status_t  codec2_codec_recover(pjmedia_codec *codec,
				      unsigned output_buf_len,
				      struct pjmedia_frame *output);
#endif


/* Definition for codec2 codec operations. */
static pjmedia_codec_op codec2_op =
{
    &codec2_codec_init,
    &codec2_codec_open,
    &codec2_codec_close,
    &codec2_codec_modify,
    &codec2_codec_parse,
    &codec2_codec_encode,
    &codec2_codec_decode,
#if !PLC_DISABLED
    &codec2_codec_recover
#else
    NULL
#endif
};

/* Definition for codec factory operations. */
static pjmedia_codec_factory_op codec2_factory_op =
{
    &codec2_test_alloc,
    &codec2_default_attr,
    &codec2_enum_codecs,
    &codec2_alloc_codec,
    &codec2_dealloc_codec,
    &pjmedia_codec_codec2_deinit
};

/* GSM factory */
static struct codec2_codec_factory
{
    pjmedia_codec_factory    base;
    pjmedia_endpt	    *endpt;
    pj_pool_t		    *pool;
    pj_mutex_t		    *mutex;
    pjmedia_codec	     codec_list;
} codec2_codec_factory;


/* GSM codec private data. */
struct codec2_data
{
    void	*encoder;
    void	*decoder;
    pj_bool_t		 plc_enabled;
    pj_bool_t		 vad_enabled;
#if !PLC_DISABLED
    pjmedia_plc		*plc;
#endif
    pjmedia_silence_det	*vad;
    pj_timestamp	 last_tx;
};



/*
 * Initialize and register GSM codec factory to pjmedia endpoint.
 */
PJ_DEF(pj_status_t) pjmedia_codec_codec2_init( pjmedia_endpt *endpt )
{
    pjmedia_codec_mgr *codec_mgr;
    pj_status_t status;

    if (codec2_codec_factory.pool != NULL)
	return PJ_SUCCESS;

    /* Create CODEC2 codec factory. */
    codec2_codec_factory.base.op = &codec2_factory_op;
    codec2_codec_factory.base.factory_data = NULL;
    codec2_codec_factory.endpt = endpt;

    codec2_codec_factory.pool = pjmedia_endpt_create_pool(endpt, "codec2", 4000,
						       4000);
    if (!codec2_codec_factory.pool)
	return PJ_ENOMEM;

    pj_list_init(&codec2_codec_factory.codec_list);

    /* Create mutex. */
    status = pj_mutex_create_simple(codec2_codec_factory.pool, "codec2",
				    &codec2_codec_factory.mutex);
    if (status != PJ_SUCCESS)
	goto on_error;

    /* Get the codec manager. */
    codec_mgr = pjmedia_endpt_get_codec_mgr(endpt);
    if (!codec_mgr) {
	status = PJ_EINVALIDOP;
	goto on_error;
    }

    /* Register codec factory to endpoint. */
    status = pjmedia_codec_mgr_register_factory(codec_mgr, 
						&codec2_codec_factory.base);
    if (status != PJ_SUCCESS)
	goto on_error;

    /* Done. */
    return PJ_SUCCESS;

on_error:
    pj_pool_release(codec2_codec_factory.pool);
    codec2_codec_factory.pool = NULL;
    return status;
}



/*
 * Unregister CODEC2 codec factory from pjmedia endpoint and deinitialize
 * the CODEC2 codec library.
 */
PJ_DEF(pj_status_t) pjmedia_codec_codec2_deinit(void)
{
    pjmedia_codec_mgr *codec_mgr;
    pj_status_t status;

    if (codec2_codec_factory.pool == NULL)
	return PJ_SUCCESS;

    /* Get the codec manager. */
    codec_mgr = pjmedia_endpt_get_codec_mgr(codec2_codec_factory.endpt);
    if (!codec_mgr) {
	pj_pool_release(codec2_codec_factory.pool);
	codec2_codec_factory.pool = NULL;
	return PJ_EINVALIDOP;
    }

    /* Unregister GSM codec factory. */
    status = pjmedia_codec_mgr_unregister_factory(codec_mgr,
						  &codec2_codec_factory.base);
    
    /* Destroy mutex. */
    pj_mutex_destroy(codec2_codec_factory.mutex);

    /* Destroy pool. */
    pj_pool_release(codec2_codec_factory.pool);
    codec2_codec_factory.pool = NULL;

    return status;
}

/* 
 * Check if factory can allocate the specified codec. 
 */
static pj_status_t codec2_test_alloc( pjmedia_codec_factory *factory,
				   const pjmedia_codec_info *info )
{
    const pj_str_t codec2_tag = { "CODEC2", 6 };
    PJ_UNUSED_ARG(factory);

    /* The type must be audio */
    if (info->type != PJMEDIA_TYPE_AUDIO) {
        return PJMEDIA_CODEC_EUNSUP;
    }

    /* Check encoding name. */
    if (pj_stricmp(&info->encoding_name, &codec2_tag) != 0) {
        return PJMEDIA_CODEC_EUNSUP;
    }

    /* Check clock-rate */
    if (info->clock_rate == 8000) {
        return PJ_SUCCESS;
    }

    return PJMEDIA_CODEC_EUNSUP;
}

/*
 * Generate default attribute.
 */
static pj_status_t codec2_default_attr (pjmedia_codec_factory *factory,
				      const pjmedia_codec_info *id, 
				      pjmedia_codec_param *attr )
{
    PJ_UNUSED_ARG(factory);
    PJ_UNUSED_ARG(id);

    pj_bzero(attr, sizeof(pjmedia_codec_param));
    attr->info.clock_rate = 8000;
    attr->info.channel_cnt = 1;
    attr->info.avg_bps = 2550;
    attr->info.max_bps = 2550;
    attr->info.pcm_bits_per_sample = 16;
    attr->info.frm_ptime = 20;
    attr->info.pt = PJMEDIA_RTP_PT_CODEC2;

    attr->setting.frm_per_pkt = 1;
    attr->setting.vad = 1;
#if !PLC_DISABLED
    attr->setting.plc = 1;
#endif

    /* Default all other flag bits disabled. */

    return PJ_SUCCESS;
}

/*
 * Enum codecs supported by this factory (i.e. only GSM!).
 */
static pj_status_t codec2_enum_codecs(pjmedia_codec_factory *factory,
				    unsigned *count, 
				    pjmedia_codec_info codecs[])
{
    PJ_UNUSED_ARG(factory);
    PJ_ASSERT_RETURN(codecs && *count > 0, PJ_EINVAL);

    pj_bzero(&codecs[0], sizeof(pjmedia_codec_info));
    codecs[0].encoding_name = pj_str("CODEC2");
    codecs[0].pt = PJMEDIA_RTP_PT_CODEC2;
    codecs[0].type = PJMEDIA_TYPE_AUDIO;
    codecs[0].clock_rate = 8000;
    codecs[0].channel_cnt = 1;

    *count = 1;

    return PJ_SUCCESS;
}

/*
 * Allocate a new CODEC2 codec instance.
 */
static pj_status_t codec2_alloc_codec( pjmedia_codec_factory *factory,
				    const pjmedia_codec_info *id,
				    pjmedia_codec **p_codec)
{
    pjmedia_codec *codec;
    struct codec2_data *codec2_data;
    pj_status_t status;

    PJ_ASSERT_RETURN(factory && id && p_codec, PJ_EINVAL);
    PJ_ASSERT_RETURN(factory == &codec2_codec_factory.base, PJ_EINVAL);
    PJ_LOG(4, (THIS_FILE, "codec2 alloc codecs"));

    pj_mutex_lock(codec2_codec_factory.mutex);

    /* Get free nodes, if any. */
    if (!pj_list_empty(&codec2_codec_factory.codec_list)) {
	codec = codec2_codec_factory.codec_list.next;
	pj_list_erase(codec);
    } else {
	codec = PJ_POOL_ZALLOC_T(codec2_codec_factory.pool, pjmedia_codec);
	PJ_ASSERT_RETURN(codec != NULL, PJ_ENOMEM);
	codec->op = &codec2_op;
	codec->factory = factory;

	codec2_data = PJ_POOL_ZALLOC_T(codec2_codec_factory.pool, struct codec2_data);
	codec->codec_data = codec2_data;

#if !PLC_DISABLED
	/* Create PLC */
	status = pjmedia_plc_create(codec2_codec_factory.pool, 8000,
				    160, 0, &codec2_data->plc);
	if (status != PJ_SUCCESS) {
	    pj_mutex_unlock(codec2_codec_factory.mutex);
	    return status;
	}
#endif

	/* Create silence detector */
	status = pjmedia_silence_det_create(codec2_codec_factory.pool,
					    8000, 160,
					    &codec2_data->vad);
	if (status != PJ_SUCCESS) {
	    pj_mutex_unlock(codec2_codec_factory.mutex);
	    return status;
	}
    }

    pj_mutex_unlock(codec2_codec_factory.mutex);

    *p_codec = codec;
    return PJ_SUCCESS;
}

/*
 * Free codec.
 */
static pj_status_t codec2_dealloc_codec( pjmedia_codec_factory *factory,
				      pjmedia_codec *codec )
{
    struct codec2_data *codec2_data;
    int i;

    PJ_ASSERT_RETURN(factory && codec, PJ_EINVAL);
    PJ_ASSERT_RETURN(factory == &codec2_codec_factory.base, PJ_EINVAL);

    codec2_data = (struct codec2_data*) codec->codec_data;

    /* Close codec, if it's not closed. */
    codec2_codec_close(codec);

    PJ_UNUSED_ARG(i);

    /* Re-init silence_period */
    pj_set_timestamp32(&codec2_data->last_tx, 0, 0);

    /* Put in the free list. */
    pj_mutex_lock(codec2_codec_factory.mutex);
    pj_list_push_front(&codec2_codec_factory.codec_list, codec);
    pj_mutex_unlock(codec2_codec_factory.mutex);

    return PJ_SUCCESS;
}

/*
 * Init codec.
 */
static pj_status_t codec2_codec_init( pjmedia_codec *codec,
				   pj_pool_t *pool )
{
    PJ_UNUSED_ARG(codec);
    PJ_UNUSED_ARG(pool);
    return PJ_SUCCESS;
}

/*
 * Open codec.
 */
static pj_status_t codec2_codec_open( pjmedia_codec *codec,
				   pjmedia_codec_param *attr )
{
    struct codec2_data *codec2_data = (struct codec2_data*) codec->codec_data;

    pj_assert(codec2_data != NULL);
    pj_assert(codec2_data->encoder == NULL && codec2_data->decoder == NULL);

    PJ_LOG(4, (THIS_FILE, "codec2 open !! "));

    codec2_data->encoder = codec2_create();
    if (!codec2_data->encoder)
	return PJMEDIA_CODEC_EFAILED;

    codec2_data->decoder = codec2_create();
    if (!codec2_data->decoder)
	return PJMEDIA_CODEC_EFAILED;

    codec2_data->vad_enabled = (attr->setting.vad != 0);
    codec2_data->plc_enabled = (attr->setting.plc != 0);

    return PJ_SUCCESS;
}

/*
 * Close codec.
 */
static pj_status_t codec2_codec_close( pjmedia_codec *codec )
{
    struct codec2_data *codec2_data = (struct codec2_data*) codec->codec_data;

    pj_assert(codec2_data != NULL);

    if (codec2_data->encoder) {
    codec2_destroy(codec2_data->encoder);
	codec2_data->encoder = NULL;
    }
    if (codec2_data->decoder) {
    codec2_destroy(codec2_data->decoder);
	codec2_data->decoder = NULL;
    }

    return PJ_SUCCESS;
}


/*
 * Modify codec settings.
 */
static pj_status_t  codec2_codec_modify(pjmedia_codec *codec,
				     const pjmedia_codec_param *attr )
{
    struct codec2_data *codec2_data = (struct codec2_data*) codec->codec_data;

    pj_assert(codec2_data != NULL);
    pj_assert(codec2_data->encoder != NULL && codec2_data->decoder != NULL);

    codec2_data->vad_enabled = (attr->setting.vad != 0);
    codec2_data->plc_enabled = (attr->setting.plc != 0);

    return PJ_SUCCESS;
}


/*
 * Get frames in the packet.
 */
static pj_status_t  codec2_codec_parse( pjmedia_codec *codec,
				     void *pkt,
				     pj_size_t pkt_size,
				     const pj_timestamp *ts,
				     unsigned *frame_cnt,
				     pjmedia_frame frames[])
{
    unsigned count = 0;

    PJ_UNUSED_ARG(codec);

    PJ_ASSERT_RETURN(frame_cnt, PJ_EINVAL);

  //  PJ_LOG(4, (THIS_FILE, "codec2 parse %d", pkt_size));

    while (pkt_size >= BITS_SIZE && count < *frame_cnt) {
	frames[count].type = PJMEDIA_FRAME_TYPE_AUDIO;
	frames[count].buf = pkt;
	frames[count].size = BITS_SIZE;
	frames[count].timestamp.u64 = ts->u64 + count * CODEC2_SAMPLES_PER_FRAME;

	pkt = ((char*)pkt) + BITS_SIZE;
	pkt_size -= BITS_SIZE;

	++count;
    }
  //  PJ_LOG(4, (THIS_FILE, "Found : %d", count));
    *frame_cnt = count;
    return PJ_SUCCESS;
}

/*
 * Encode frame.
 */
static pj_status_t codec2_codec_encode( pjmedia_codec *codec,
				     const struct pjmedia_frame *input,
				     unsigned output_buf_len, 
				     struct pjmedia_frame *output)
{
    struct codec2_data *codec2_data = (struct codec2_data*) codec->codec_data;
    pj_int16_t *pcm_in;
    unsigned in_size;

  //  PJ_LOG(4, (THIS_FILE, "codec2 encode ...."));
    pj_assert(codec2_data && input && output);
    
    pcm_in = (pj_int16_t*)input->buf;
    in_size = input->size;
  //  PJ_LOG(4, (THIS_FILE, "codec2 encode %d ", input->size));

    PJ_ASSERT_RETURN(in_size % (CODEC2_SAMPLES_PER_FRAME*2) == 0, PJMEDIA_CODEC_EPCMFRMINLEN);
    //PJ_ASSERT_RETURN(output_buf_len >= BYTES_PER_FRAME * in_size/(CODEC2_SAMPLES_PER_FRAME*2),
	//	     PJMEDIA_CODEC_EFRMTOOSHORT);

    /* Detect silence */
    if (codec2_data->vad_enabled) {
	pj_bool_t is_silence;
	pj_int32_t silence_duration;

	silence_duration = pj_timestamp_diff32(&codec2_data->last_tx,
					       &input->timestamp);

	is_silence = pjmedia_silence_det_detect(codec2_data->vad,
					        (const pj_int16_t*) input->buf,
						(input->size >> 1),
						NULL);
	if (is_silence &&
	    (PJMEDIA_CODEC_MAX_SILENCE_PERIOD == -1 ||
	     silence_duration < PJMEDIA_CODEC_MAX_SILENCE_PERIOD*8000/1000))
	{
	    output->type = PJMEDIA_FRAME_TYPE_NONE;
	    output->buf = NULL;
	    output->size = 0;
	    output->timestamp = input->timestamp;
	    return PJ_SUCCESS;
	} else {
		codec2_data->last_tx = input->timestamp;
	}
    }
    /* Encode */
    output->size = 0;
  //  PJ_LOG(4, (THIS_FILE, "codec2 encoding.... ", input->size));
    while (in_size >= (CODEC2_SAMPLES_PER_FRAME*2)) {
    codec2_encode(codec2_data->encoder,
    		(void *) output->buf + output->size, /* Encoded */
    		(void *) pcm_in /* Decoded */);
	output->size += BITS_SIZE;
	pcm_in += CODEC2_SAMPLES_PER_FRAME;
	in_size -= (CODEC2_SAMPLES_PER_FRAME*2);
    }
  //  PJ_LOG(4, (THIS_FILE, "codec2 encoded.... %d", output->size));

    output->type = PJMEDIA_FRAME_TYPE_AUDIO;
    output->timestamp = input->timestamp;

    return PJ_SUCCESS;
}

/*
 * Decode frame.
 */
static pj_status_t codec2_codec_decode( pjmedia_codec *codec,
				     const struct pjmedia_frame *input,
				     unsigned output_buf_len, 
				     struct pjmedia_frame *output)
{
    struct codec2_data *codec2_data = (struct codec2_data*) codec->codec_data;

    pj_assert(codec2_data != NULL);
    PJ_ASSERT_RETURN(input && output, PJ_EINVAL);
  //  PJ_LOG(4, (THIS_FILE, "codec2 decode %d ", input->size));

    if (output_buf_len < (CODEC2_SAMPLES_PER_FRAME*2))
	return PJMEDIA_CODEC_EPCMTOOSHORT;

    if (input->size < BITS_SIZE)
	return PJMEDIA_CODEC_EFRMTOOSHORT;

    codec2_decode(codec2_data->decoder,
    	  (short int *)output->buf,
 	      (const unsigned char *)input->buf);

    output->size = (CODEC2_SAMPLES_PER_FRAME*2);
    output->type = PJMEDIA_FRAME_TYPE_AUDIO;
    output->timestamp = input->timestamp;

#if !PLC_DISABLED
    if (codec2_data->plc_enabled)
	pjmedia_plc_save( codec2_data->plc, (pj_int16_t*)output->buf);
#endif

    return PJ_SUCCESS;
}


#if !PLC_DISABLED
/*
 * Recover lost frame.
 */
static pj_status_t  codec2_codec_recover(pjmedia_codec *codec,
				      unsigned output_buf_len,
				      struct pjmedia_frame *output)
{
    struct codec2_data *codec2_data = (struct codec2_data*) codec->codec_data;

    PJ_ASSERT_RETURN(codec2_data->plc_enabled, PJ_EINVALIDOP);

    PJ_ASSERT_RETURN(output_buf_len >= (CODEC2_SAMPLES_PER_FRAME*2), PJMEDIA_CODEC_EPCMTOOSHORT);

  //  PJ_LOG(4, (THIS_FILE, "codec2 recover"));

    pjmedia_plc_generate(codec2_data->plc, (pj_int16_t*)output->buf);
    output->size = (CODEC2_SAMPLES_PER_FRAME*2);

    return PJ_SUCCESS;
}
#endif


#endif	/* PJMEDIA_HAS_GSM_CODEC */

