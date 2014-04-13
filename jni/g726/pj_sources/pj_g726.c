/* $Id */
/* 
 * Copyright (C) 2011 Keystream AB
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA 
 */

/* 
 * G726 codec integration.
 */
#include <pjmedia/codec.h>
#include <pjmedia/errno.h>
#include <pjmedia/endpoint.h>
#include <pjmedia/plc.h>
#include <pjmedia/port.h>
#include <pjmedia/silencedet.h>
#include <pj/assert.h>
#include <pj/log.h>
#include <pj/pool.h>
#include <pj/string.h>
#include <pj/os.h>
#include <pj/math.h>
#include <pj_g726.h>


#if PJMEDIA_HAS_G726_CODEC

#include <g72x.h>


#define THIS_FILE "g726.c"

/* Debug trace */
#define PJ_TRACE 0

#if PJ_TRACE
#  define TRACE_(format,...) PJ_LOG(4,(THIS_FILE, format, ## __VA_ARGS__))
#else
#  define TRACE_(format,...)
#endif

/* Use PJMEDIA PLC */
#define USE_PJMEDIA_PLC	    1

#define SAMPLE_RATE 8000
#define PTIME 10
#define SAMPLES_PER_FRAME ((SAMPLE_RATE * PTIME) / 1000)
#define PCM_FRAME_SIZE (2 * SAMPLES_PER_FRAME)


typedef int (*decode_func_t) (int, int, g726_state*);
typedef int (*encode_func_t) (int, int, g726_state*);


/* Prototypes for g726 codec factory */
static pj_status_t g726_test_alloc(pjmedia_codec_factory *factory, 
				   const pjmedia_codec_info *id );
static pj_status_t g726_default_attr(pjmedia_codec_factory *factory, 
				     const pjmedia_codec_info *id, 
				     pjmedia_codec_param *attr );
static pj_status_t g726_enum_codecs(pjmedia_codec_factory *factory, 
				    unsigned *count, 
				    pjmedia_codec_info codecs[]);
static pj_status_t g726_alloc_codec(pjmedia_codec_factory *factory, 
				    const pjmedia_codec_info *id, 
				    pjmedia_codec **p_codec);
static pj_status_t g726_dealloc_codec(pjmedia_codec_factory *factory, 
				      pjmedia_codec *codec );

/* Prototypes for g726 implementation. */
static pj_status_t  g726_codec_init(pjmedia_codec *codec, 
				    pj_pool_t *pool );
static pj_status_t  g726_codec_open(pjmedia_codec *codec, 
				    pjmedia_codec_param *attr );
static pj_status_t  g726_codec_close(pjmedia_codec *codec );
static pj_status_t  g726_codec_modify(pjmedia_codec *codec, 
				      const pjmedia_codec_param *attr );
static pj_status_t  g726_codec_parse(pjmedia_codec *codec,
				     void *pkt,
				     pj_size_t pkt_size,
				     const pj_timestamp *ts,
				     unsigned *frame_cnt,
				     pjmedia_frame frames[]);
static pj_status_t  g726_codec_encode(pjmedia_codec *codec, 
				      const struct pjmedia_frame *input,
				      unsigned output_buf_len, 
				      struct pjmedia_frame *output);
static pj_status_t  g726_codec_decode(pjmedia_codec *codec, 
				      const struct pjmedia_frame *input,
				      unsigned output_buf_len, 
				      struct pjmedia_frame *output);
static pj_status_t  g726_codec_recover(pjmedia_codec *codec,
				      unsigned output_buf_len,
				      struct pjmedia_frame *output);



/* Definition for G726 codec operations. */
static pjmedia_codec_op g726_op = 
{
    &g726_codec_init,
    &g726_codec_open,
    &g726_codec_close,
    &g726_codec_modify,
    &g726_codec_parse,
    &g726_codec_encode,
    &g726_codec_decode,
    &g726_codec_recover
};

/* Definition for G726 codec factory operations. */
static pjmedia_codec_factory_op g726_factory_op =
{
    &g726_test_alloc,
    &g726_default_attr,
    &g726_enum_codecs,
    &g726_alloc_codec,
    &g726_dealloc_codec,
    &pjmedia_codec_g726_deinit
};


/* G726 factory */
static struct g726_codec_factory
{
    pjmedia_codec_factory    base;
    pjmedia_endpt	    *endpt;
    pj_pool_t		    *pool;
} g726_codec_factory;


/* G726 codec private data. */
struct g726_private
{
    pj_pool_t	 *pool;
    g726_state    encoder;
    g726_state    decoder;
    encode_func_t encode_func;
    decode_func_t decode_func;
    unsigned      bitrate;
    unsigned      code_bits; /* Number of bits for each coded audio sample. */
    unsigned      code_bit_mask; /* Bit mask for the encoded bits */
    unsigned      encoded_frame_size; /* Size in bytes of an encoded 10ms frame. */
    pj_bool_t	  plc_enabled;
    pj_bool_t	  vad_enabled;
    pjmedia_silence_det *vad;
    pj_timestamp	 last_tx;
#if USE_PJMEDIA_PLC
    pjmedia_plc	*plc;
#endif
};



/*
 * Initialize and register G726 codec factory to pjmedia endpoint.
 */
PJ_DEF(pj_status_t) pjmedia_codec_g726_init( pjmedia_endpt *endpt )
{
    pjmedia_codec_mgr *codec_mgr;
    pj_status_t status;

    if (g726_codec_factory.pool != NULL)
	return PJ_SUCCESS;

    /* Create G726 codec factory. */
    g726_codec_factory.base.op = &g726_factory_op;
    g726_codec_factory.base.factory_data = NULL;
    g726_codec_factory.endpt = endpt;

    g726_codec_factory.pool = pjmedia_endpt_create_pool(endpt, "g726", 512, 512);
    if (!g726_codec_factory.pool)
	return PJ_ENOMEM;

    /* Get the codec manager. */
    codec_mgr = pjmedia_endpt_get_codec_mgr(endpt);
    if (!codec_mgr) {
	status = PJ_EINVALIDOP;
	goto on_error;
    }

    /* Register codec factory to endpoint. */
    status = pjmedia_codec_mgr_register_factory(codec_mgr, 
						&g726_codec_factory.base);
    if (status != PJ_SUCCESS)
	goto on_error;

    /* Done. */
    return PJ_SUCCESS;

on_error:
    pj_pool_release(g726_codec_factory.pool);
    g726_codec_factory.pool = NULL;
    return status;
}


/*
 * Unregister G726 codec factory from pjmedia endpoint and deinitialize
 * the G726 codec library.
 */
PJ_DEF(pj_status_t) pjmedia_codec_g726_deinit(void)
{
    pjmedia_codec_mgr *codec_mgr;
    pj_status_t status;

    if (g726_codec_factory.pool == NULL)
	return PJ_SUCCESS;

    /* Get the codec manager. */
    codec_mgr = pjmedia_endpt_get_codec_mgr(g726_codec_factory.endpt);
    if (!codec_mgr) {
	pj_pool_release(g726_codec_factory.pool);
	g726_codec_factory.pool = NULL;
	return PJ_EINVALIDOP;
    }

    /* Unregister G726 codec factory. */
    status = pjmedia_codec_mgr_unregister_factory(codec_mgr,
						  &g726_codec_factory.base);
    
    /* Destroy pool. */
    pj_pool_release(g726_codec_factory.pool);
    g726_codec_factory.pool = NULL;
    
    return status;
}

/* 
 * Check if factory can allocate the specified codec. 
 */
static pj_status_t g726_test_alloc( pjmedia_codec_factory *factory, 
				    const pjmedia_codec_info *info )
{
    PJ_UNUSED_ARG(factory);

    /* Type MUST be audio. */
    if (info->type != PJMEDIA_TYPE_AUDIO)
	return PJMEDIA_CODEC_EUNSUP;

    /* Check payload type. */
    if (info->pt != PJMEDIA_RTP_PT_G721 &&
	info->pt != PJMEDIA_RTP_PT_G726_16 &&
	info->pt != PJMEDIA_RTP_PT_G726_24 &&
	info->pt != PJMEDIA_RTP_PT_G726_32 &&
	info->pt != PJMEDIA_RTP_PT_G726_40)
    {
	return PJMEDIA_CODEC_EUNSUP;
    }

    /* Check clock-rate */
    if (info->clock_rate != SAMPLE_RATE)
	return PJMEDIA_CODEC_EUNSUP;

    return PJ_SUCCESS;
}

/*
 * Generate default attribute.
 */
static pj_status_t g726_default_attr( pjmedia_codec_factory *factory, 
				      const pjmedia_codec_info *id, 
				      pjmedia_codec_param *attr )
{
    PJ_UNUSED_ARG(factory);
    PJ_ASSERT_RETURN(id && attr, PJ_EINVAL);

    pj_bzero(attr, sizeof(pjmedia_codec_param));
    attr->info.clock_rate          = SAMPLE_RATE;
    attr->info.channel_cnt         = 1;
    attr->info.pcm_bits_per_sample = 16;
    attr->info.frm_ptime           = PTIME;
    attr->info.pt                  = id->pt;
    attr->setting.frm_per_pkt      = 2; /* 20 ms */
    attr->setting.vad              = 1;
#if USE_PJMEDIA_PLC
    attr->setting.plc              = 1;
#endif

    switch (id->pt) {
    case PJMEDIA_RTP_PT_G726_16:
	attr->info.max_bps = 16000;
	attr->info.avg_bps = 16000;
	break;
    case PJMEDIA_RTP_PT_G726_24:
	attr->info.max_bps = 24000;
	attr->info.avg_bps = 24000;
	break;
    case PJMEDIA_RTP_PT_G726_32:
    case PJMEDIA_RTP_PT_G721:
	attr->info.max_bps = 32000;
	attr->info.avg_bps = 32000;
	break;
    case PJMEDIA_RTP_PT_G726_40:
	attr->info.max_bps = 40000;
	attr->info.avg_bps = 40000;
	break;
    default:
	return PJMEDIA_CODEC_EUNSUP;
    }

    return PJ_SUCCESS;
}


/*
 * Enum codecs supported by this factory (i.e. only G726!).
 */
static pj_status_t g726_enum_codecs( pjmedia_codec_factory *factory, 
				     unsigned *count, 
				     pjmedia_codec_info codecs[])
{
    PJ_UNUSED_ARG(factory);
    PJ_ASSERT_RETURN(codecs && *count > 0, PJ_EINVAL);
    unsigned index = 0;

    if (index < *count) {
	pj_bzero(&codecs[index], sizeof(pjmedia_codec_info));
	codecs[index].encoding_name = pj_str("G726-40");
	codecs[index].pt            = PJMEDIA_RTP_PT_G726_40;
	codecs[index].type          = PJMEDIA_TYPE_AUDIO;
	codecs[index].clock_rate    = SAMPLE_RATE;
	codecs[index].channel_cnt   = 1;
	index++;
    }
    if (index < *count) {
	pj_bzero(&codecs[index], sizeof(pjmedia_codec_info));
	codecs[index].encoding_name = pj_str("G726-32");
	codecs[index].pt            = PJMEDIA_RTP_PT_G726_32;
	codecs[index].type          = PJMEDIA_TYPE_AUDIO;
	codecs[index].clock_rate    = SAMPLE_RATE;
	codecs[index].channel_cnt   = 1;
	index++;
    }
    if (index < *count) {
	pj_bzero(&codecs[index], sizeof(pjmedia_codec_info));
	codecs[index].encoding_name = pj_str("G726-24");
	codecs[index].pt            = PJMEDIA_RTP_PT_G726_24;
	codecs[index].type          = PJMEDIA_TYPE_AUDIO;
	codecs[index].clock_rate    = SAMPLE_RATE;
	codecs[index].channel_cnt   = 1;
	index++;
    }
    if (index < *count) {
	pj_bzero(&codecs[index], sizeof(pjmedia_codec_info));
	codecs[index].encoding_name = pj_str("G726-16");
	codecs[index].pt            = PJMEDIA_RTP_PT_G726_16;
	codecs[index].type          = PJMEDIA_TYPE_AUDIO;
	codecs[index].clock_rate    = SAMPLE_RATE;
	codecs[index].channel_cnt   = 1;
	index++;
    }

    *count = index;
    return PJ_SUCCESS;
}


/*
 * Allocate a new G726 codec instance.
 */
static pj_status_t g726_alloc_codec( pjmedia_codec_factory *factory, 
				     const pjmedia_codec_info *id,
				     pjmedia_codec **p_codec)
{
    pj_pool_t *pool;
    pjmedia_codec *codec;
    struct g726_private *priv;
    pj_status_t status;

    PJ_ASSERT_RETURN(factory && id && p_codec, PJ_EINVAL);
    PJ_ASSERT_RETURN(factory == &g726_codec_factory.base, PJ_EINVAL);

    pool = pjmedia_endpt_create_pool(g726_codec_factory.endpt, "g726-inst", 512, 512);

    codec = PJ_POOL_ZALLOC_T(pool, pjmedia_codec);
    PJ_ASSERT_RETURN(codec != NULL, PJ_ENOMEM);
    codec->op = &g726_op;
    codec->factory = factory;

    priv = PJ_POOL_ZALLOC_T(pool, struct g726_private);
    priv->pool = pool;
    codec->codec_data = priv;

#if USE_PJMEDIA_PLC
    /* Create PLC */
    status = pjmedia_plc_create(pool, SAMPLE_RATE, SAMPLES_PER_FRAME, 0, &priv->plc);
    if (status != PJ_SUCCESS) {
	return status;
    }
#else
    PJ_UNUSED_ARG(status);
#endif

    /* Create VAD */
    status = pjmedia_silence_det_create(pool, SAMPLE_RATE,
					SAMPLES_PER_FRAME, &priv->vad);
    if (status != PJ_SUCCESS) {
	return status;
    }

    *p_codec = codec;
    return PJ_SUCCESS;
}


/*
 * Free codec.
 */
static pj_status_t g726_dealloc_codec( pjmedia_codec_factory *factory, 
				       pjmedia_codec *codec )
{
    struct g726_private *priv;

    PJ_ASSERT_RETURN(factory && codec, PJ_EINVAL);
    PJ_ASSERT_RETURN(factory == &g726_codec_factory.base, PJ_EINVAL);

    priv = (struct g726_private*) codec->codec_data;
    PJ_ASSERT_RETURN(priv != NULL, PJ_EINVAL);

    /* Close codec, if it's not closed. */
    g726_codec_close(codec);

    codec->codec_data = NULL;
    pj_pool_release(priv->pool);

    return PJ_SUCCESS;
}

/*
 * Init codec.
 */
static pj_status_t g726_codec_init( pjmedia_codec *codec, 
				    pj_pool_t *pool )
{
    PJ_UNUSED_ARG(codec);
    PJ_UNUSED_ARG(pool);
    return PJ_SUCCESS;
}


/*
 * Open codec.
 */
static pj_status_t g726_codec_open( pjmedia_codec *codec, 
				    pjmedia_codec_param *attr )
{
    struct g726_private *priv = (struct g726_private*) codec->codec_data;

    PJ_ASSERT_RETURN(codec && attr, PJ_EINVAL);
    PJ_ASSERT_RETURN(priv != NULL, PJ_EINVALIDOP);

    /* Initialize common state */
    priv->vad_enabled = (attr->setting.vad != 0);
    priv->plc_enabled = (attr->setting.plc != 0);

    switch (attr->info.pt) {
    case PJMEDIA_RTP_PT_G726_16:
	priv->encode_func = g726_16_encoder;
	priv->decode_func = g726_16_decoder;
	priv->bitrate = 16000;
	priv->code_bits = 2;
	priv->code_bit_mask = 0x03;
	priv->encoded_frame_size = (SAMPLES_PER_FRAME * 2) / 8;
	break;
    case PJMEDIA_RTP_PT_G726_24:
	priv->encode_func = g726_24_encoder;
	priv->decode_func = g726_24_decoder;
	priv->bitrate = 24000;
	priv->code_bits = 3;
	priv->code_bit_mask = 0x07;
	priv->encoded_frame_size = (SAMPLES_PER_FRAME * 3) / 8;
	break;
    case PJMEDIA_RTP_PT_G726_32:
    case PJMEDIA_RTP_PT_G721:
	priv->encode_func = g726_32_encoder;
	priv->decode_func = g726_32_decoder;
	priv->bitrate = 32000;
	priv->code_bits = 4;
	priv->code_bit_mask = 0x0f;
	priv->encoded_frame_size = (SAMPLES_PER_FRAME * 4) / 8;
	break;
    case PJMEDIA_RTP_PT_G726_40:
	priv->encode_func = g726_40_encoder;
	priv->decode_func = g726_40_decoder;
	priv->bitrate = 40000;
	priv->code_bits = 5;
	priv->code_bit_mask = 0x1f;
	priv->encoded_frame_size = (SAMPLES_PER_FRAME * 5) / 8;
	break;
    default:
	return PJMEDIA_CODEC_EUNSUP;
    }

    g726_init_state(&priv->encoder);
    g726_init_state(&priv->decoder);

    TRACE_("G726 codec allocated: vad=%d, plc=%d, bitrate=%d",
	   priv->vad_enabled, priv->plc_enabled, 
	   priv->bitrate);
    return PJ_SUCCESS;
}


/*
 * Close codec.
 */
static pj_status_t g726_codec_close( pjmedia_codec *codec )
{
    TRACE_("G726 codec closed");
    return PJ_SUCCESS;
}


/*
 * Modify codec settings.
 */
static pj_status_t g726_codec_modify( pjmedia_codec *codec, 
				      const pjmedia_codec_param *attr )
{
    struct g726_private *priv = (struct g726_private*) codec->codec_data;

    PJ_ASSERT_RETURN(codec && attr, PJ_EINVAL);
    PJ_ASSERT_RETURN(priv != NULL, PJ_EINVALIDOP);

    priv->vad_enabled = (attr->setting.vad != 0);
    priv->plc_enabled = (attr->setting.plc != 0);

    TRACE_("G726 codec modified: vad=%d, plc=%d",
	   priv->vad_enabled, priv->plc_enabled);

    return PJ_SUCCESS;
}


/*
 * Get frames in the packet.
 */
static pj_status_t g726_codec_parse( pjmedia_codec *codec,
				     void *pkt,
				     pj_size_t pkt_size,
				     const pj_timestamp *ts,
				     unsigned *frame_cnt,
				     pjmedia_frame frames[])
{
    struct g726_private *priv = (struct g726_private*) codec->codec_data;

    unsigned index = 0;
    while (index < *frame_cnt && pkt_size >= priv->encoded_frame_size) {
	frames[index].type = PJMEDIA_FRAME_TYPE_AUDIO;
	frames[index].buf  = pkt;
	frames[index].size = priv->encoded_frame_size;
	frames[index].timestamp.u64 = ts->u64 + index * SAMPLES_PER_FRAME;

	pkt = (pj_uint8_t*)pkt + priv->encoded_frame_size;
	pkt_size -= priv->encoded_frame_size;
	++index;
    }
    *frame_cnt = index;

    return PJ_SUCCESS;
}


/*
 * Encode frame.
 */
static pj_status_t g726_codec_encode( pjmedia_codec *codec, 
				      const struct pjmedia_frame *input,
				      unsigned output_buf_len, 
				      struct pjmedia_frame *output)
{
    struct g726_private *priv = (struct g726_private*) codec->codec_data;
    unsigned samples, i;
    pj_uint8_t *dst;
    pj_int16_t *src;
    unsigned code;
    unsigned int out_buffer = 0;
    int out_bits = 0;

    /* Check output buffer length */
    if (output_buf_len < priv->encoded_frame_size)
	return PJMEDIA_CODEC_EFRMTOOSHORT;

    /* Detect silence if VAD is enabled */
    if (priv->vad_enabled) {
	pj_bool_t is_silence;
	pj_int32_t silence_period;

	silence_period = pj_timestamp_diff32(&priv->last_tx,
					     &input->timestamp);

	is_silence = pjmedia_silence_det_detect(priv->vad, 
						(const pj_int16_t*) input->buf, 
						(input->size >> 1), NULL);
	if (is_silence && 
	    (PJMEDIA_CODEC_MAX_SILENCE_PERIOD == -1 ||
	     silence_period < PJMEDIA_CODEC_MAX_SILENCE_PERIOD*SAMPLE_RATE/1000))
	{
	    output->type = PJMEDIA_FRAME_TYPE_NONE;
	    output->buf = NULL;
	    output->size = 0;
	    output->timestamp = input->timestamp;
	    return PJ_SUCCESS;
	} else {
	    priv->last_tx = input->timestamp;
	}
    }

    /* Encode */
    samples = input->size >> 1; /* each sample is 16 bits */
    src = (pj_int16_t*) input->buf;
    dst = (pj_uint8_t*) output->buf;
    for (i=0; i<samples; i++) {
	code = (unsigned) priv->encode_func(src[i],
					    AUDIO_ENCODING_LINEAR,
					    &priv->encoder);
	out_buffer |= (code << out_bits);
	out_bits += priv->code_bits;
	if (out_bits >= 8) {
	    *dst++ = out_buffer & 0xff;
	    out_bits -= 8;
	    out_buffer >>= 8;
	}
    }

    output->type = PJMEDIA_FRAME_TYPE_AUDIO;
    output->size = (pj_size_t)dst - (pj_size_t)output->buf;
    output->timestamp = input->timestamp;

    return PJ_SUCCESS;
}


/*
 * Decode frame.
 */
static pj_status_t g726_codec_decode( pjmedia_codec *codec, 
				      const struct pjmedia_frame *input,
				      unsigned output_buf_len, 
				      struct pjmedia_frame *output)
{
    struct g726_private *priv = (struct g726_private*) codec->codec_data;
    unsigned samples, i;
    pj_uint8_t  *src;
    pj_int16_t *dst;
    unsigned int in_buffer = 0;
    int in_bits = 0;
    pj_uint8_t code;

    /* Check i/o buffer lengths */
    PJ_ASSERT_RETURN(output_buf_len >= PCM_FRAME_SIZE,
		     PJMEDIA_CODEC_EPCMTOOSHORT);
    PJ_ASSERT_RETURN(input->size >= priv->encoded_frame_size,
		     PJMEDIA_CODEC_EFRMTOOSHORT);

    /* Decode */
    samples = SAMPLES_PER_FRAME;
    src = (pj_uint8_t*) input->buf;
    dst = (pj_int16_t*) output->buf;
    for (i=0; i<samples; i++) {
	if (in_bits < priv->code_bits) {
	    in_buffer |= (*src++ << in_bits);
	    in_bits += 8;
	}
	code = in_buffer & priv->code_bit_mask;
	in_buffer >>= priv->code_bits;
	in_bits -= priv->code_bits;
	dst[i] = (pj_int16_t) priv->decode_func(code,
						AUDIO_ENCODING_LINEAR,
						&priv->decoder);
    }

    output->type = PJMEDIA_FRAME_TYPE_AUDIO;
    output->size = samples << 1;
    output->timestamp = input->timestamp;

#if !PLC_DISABLED
    if (priv->plc_enabled)
	pjmedia_plc_save( priv->plc, (pj_int16_t*)output->buf);
#endif

    return PJ_SUCCESS;
}


#if USE_PJMEDIA_PLC
/*
 * Recover lost frame.
 */
static pj_status_t  g726_codec_recover( pjmedia_codec *codec,
					unsigned output_buf_len,
					struct pjmedia_frame *output)
{
    struct g726_private *priv = (struct g726_private*)codec->codec_data;

    PJ_ASSERT_RETURN(output_buf_len >= PCM_FRAME_SIZE,  PJMEDIA_CODEC_EPCMTOOSHORT);

    output->size = PCM_FRAME_SIZE;
    if (priv->plc_enabled)
	pjmedia_plc_generate(priv->plc, (pj_int16_t*)output->buf);
    else
	pjmedia_zero_samples((pj_int16_t*)output->buf, SAMPLES_PER_FRAME);
    output->type = PJMEDIA_FRAME_TYPE_AUDIO;
    return PJ_SUCCESS;
}
#endif /* USE_PJMEDIA_PLC */


#endif /* PJMEDIA_HAS_G726_CODEC */

