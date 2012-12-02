/* $Id */
/*
 * Copyright (C) 2011 Teluu Inc. (http://www.teluu.com)
 * Copyright (C) 2011 Dan Arrhenius <dan@keystream.se>
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
 * AMR-NB codec implementation with OpenCORE AMRNB library
 */
#include <pjmedia-codec/g722.h>
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
#include <pjmedia-codec/amr_sdp_match.h>

#if defined(PJMEDIA_HAS_AMR_STAGEFRIGHT_CODEC) && (PJMEDIA_HAS_AMR_STAGEFRIGHT_CODEC!=0)

#include <pjmedia-codec/amr_helper.h>
#include <pjmedia-codec/opencore_amrnb.h>
#include <dlfcn.h>

#define THIS_FILE       "amr_stagefright_dyn.c"

/* Tracing */
#define PJ_TRACE    0

#if PJ_TRACE
#   define TRACE_(expr)	PJ_LOG(4,expr)
#else
#   define TRACE_(expr)
#endif

/* Use PJMEDIA PLC */
#define USE_PJMEDIA_PLC	    1


/************* AMR defs ***************/

#define AMR_TX_WMF  0
#define AMR_TX_IF2  1
#define AMR_TX_ETS  2

enum Frame_Type_3GPP
{
    AMR_475 = 0,
    AMR_515,
    AMR_59,
    AMR_67,
    AMR_74,
    AMR_795,
    AMR_102,
    AMR_122,
    AMR_SID,
    GSM_EFR_SID,
    TDMA_EFR_SID,
    PDC_EFR_SID,
    FOR_FUTURE_USE1,
    FOR_FUTURE_USE2,
    FOR_FUTURE_USE3,
    AMR_NO_DATA
};

enum Mode { MR475 = 0,
	MR515,
	MR59,
	MR67,
	MR74,
	MR795,
	MR102,
	MR122,

	MRDTX,

	N_MODES /* number of (SPC) modes */

};


typedef enum {
    /*
* One word (2-byte) to indicate type of frame type.
* One word (2-byte) to indicate frame type.
* One word (2-byte) to indicate mode.
* N words (2-byte) containing N bits (bit 0 = 0xff81, bit 1 = 0x007f).
*/
    ETS = 0, /* Both AMR-Narrowband and AMR-Wideband */
    /*
* One word (2-byte) for sync word (good frames: 0x6b21, bad frames: 0x6b20)
* One word (2-byte) for frame length N.
* N words (2-byte) containing N bits (bit 0 = 0x007f, bit 1 = 0x0081).
*/
    ITU, /* AMR-Wideband */
    /*
* AMR-WB MIME/storage format, see RFC 3267 (sections 5.1 and 5.3) for details
*/
    MIME_IETF,

    WMF, /* AMR-Narrowband */

    IF2 /* AMR-Narrowband */

} bitstream_format;


/************************************************************/



/* Prototypes for AMR-NB factory */
static pj_status_t amr_test_alloc(pjmedia_codec_factory *factory,
				   const pjmedia_codec_info *id );
static pj_status_t amr_default_attr(pjmedia_codec_factory *factory,
				     const pjmedia_codec_info *id,
				     pjmedia_codec_param *attr );
static pj_status_t amr_enum_codecs(pjmedia_codec_factory *factory,
				    unsigned *count,
				    pjmedia_codec_info codecs[]);
static pj_status_t amr_alloc_codec(pjmedia_codec_factory *factory,
				    const pjmedia_codec_info *id,
				    pjmedia_codec **p_codec);
static pj_status_t amr_dealloc_codec(pjmedia_codec_factory *factory,
				      pjmedia_codec *codec );

/* Prototypes for AMR-NB implementation. */
static pj_status_t  amr_codec_init(pjmedia_codec *codec,
				    pj_pool_t *pool );
static pj_status_t  amr_codec_open(pjmedia_codec *codec,
				    pjmedia_codec_param *attr );
static pj_status_t  amr_codec_close(pjmedia_codec *codec );
static pj_status_t  amr_codec_modify(pjmedia_codec *codec,
				      const pjmedia_codec_param *attr );
static pj_status_t  amr_codec_parse(pjmedia_codec *codec,
				     void *pkt,
				     pj_size_t pkt_size,
				     const pj_timestamp *ts,
				     unsigned *frame_cnt,
				     pjmedia_frame frames[]);
static pj_status_t  amr_codec_encode(pjmedia_codec *codec,
				      const struct pjmedia_frame *input,
				      unsigned output_buf_len,
				      struct pjmedia_frame *output);
static pj_status_t  amr_codec_decode(pjmedia_codec *codec,
				      const struct pjmedia_frame *input,
				      unsigned output_buf_len,
				      struct pjmedia_frame *output);
static pj_status_t  amr_codec_recover(pjmedia_codec *codec,
				      unsigned output_buf_len,
				      struct pjmedia_frame *output);



/* Definition for AMR-NB codec operations. */
static pjmedia_codec_op amr_op =
{
    &amr_codec_init,
    &amr_codec_open,
    &amr_codec_close,
    &amr_codec_modify,
    &amr_codec_parse,
    &amr_codec_encode,
    &amr_codec_decode,
    &amr_codec_recover
};

/* Definition for AMR-NB codec factory operations. */
static pjmedia_codec_factory_op amr_factory_op =
{
    &amr_test_alloc,
    &amr_default_attr,
    &amr_enum_codecs,
    &amr_alloc_codec,
    &amr_dealloc_codec,
    &pjmedia_codec_opencore_amrnb_deinit
};


/* AMR-NB factory */
static struct amr_codec_factory
{
    pjmedia_codec_factory    base;
    pjmedia_endpt	    *endpt;
    pj_pool_t		    *pool;
} amr_codec_factory;


/* AMR-NB codec private data. */
struct amr_data
{
    pj_pool_t		*pool;
    void		*encoder;
    void		*decoder;
    pj_bool_t		 plc_enabled;
    pj_bool_t		 vad_enabled;
    int			 enc_mode;
    pjmedia_codec_amr_pack_setting enc_setting;
    pjmedia_codec_amr_pack_setting dec_setting;
#if USE_PJMEDIA_PLC
    pjmedia_plc		*plc;
#endif
    pj_timestamp	 last_tx;

    /* Dyn lib stagefright */

    // Handle lib
    void* libEncode;
    void* libDecode;

    // Methods
    // -- Encoder --
    pj_int16_t (* AMREncodeInit) (
    		void **pEncStructure,
            void **pSidSyncStructure,
            int dtx_enable);

    pj_int16_t (* AMREncodeReset) (
    		void *pEncStructure,
    		void *pSidSyncStructure);

    void (* AMREncodeExit) (
    		void **pEncStructure,
            void **pSidSyncStructure);

    pj_int16_t (* AMREncode) (
        void *pEncState,
        void *pSidSyncState,
        enum Mode mode,
        pj_int16_t *pEncInput,
        pj_uint8_t *pEncOutput,
        enum Frame_Type_3GPP *p3gpp_frame_type,
        pj_int16_t output_format
    );
    // -- Decoder --
    /*
    * This function allocates memory for filter structure and initializes state
    * memory used by the GSM AMR decoder. This function returns zero. It will
    * return negative one if there is an error.
    */
    pj_int16_t (*GSMInitDecode) (
    	void **state_data,
        char *id);
    /*
    * AMRDecode steps into the part of the library that decodes the raw data
    * speech bits for the decoding process. It returns the address offset of
    * the next frame to be decoded.
    */
    pj_int16_t (*AMRDecode) (
		void *state_data,
		enum Frame_Type_3GPP frame_type,
		pj_uint8_t *speech_bits_ptr,
		pj_int16_t *raw_pcm_buffer,
		pj_int16_t input_format
	);

    /*
	* This function resets the state memory used by the GSM AMR decoder. This
	* function returns zero. It will return negative one if there is an error.
	*/
    pj_int16_t (* Speech_Decode_Frame_reset)(void *state_data);
    /*
	* This function frees up the memory used for the state memory of the
	* GSM AMR decoder.
	*/
    void (* GSMDecodeFrameExit)(void **state_data);
};

static pjmedia_codec_amrnb_config def_config =
{
    PJ_FALSE,	    /* octet align	*/
    5900	    /* bitrate		*/
};


// Private AMR WRAPPER

pj_status_t dlsym_stagefright(struct amr_data* amr_data){

	pj_status_t status;
	amr_data->libEncode = NULL;
	amr_data->libDecode = NULL;

	status = dlsym_stagefright_40(amr_data);
	if(status == PJ_SUCCESS){
		return status;
	}

	status = dlsym_stagefright_23(amr_data);
	if(status == PJ_SUCCESS){
		return status;
	}

	status = dlsym_stagefright_21(amr_data);
	if(status == PJ_SUCCESS){
		return status;
	}

    return status;

on_error :
	dlclose_stagefright(amr_data);
	return PJ_EINVAL;
}


pj_status_t dlsym_stagefright_40(struct amr_data* amr_data){
	amr_data->libEncode = dlopen("libstagefright.so", RTLD_LAZY);
	if(amr_data->libEncode != NULL){
		amr_data->AMREncodeInit = dlsym(amr_data->libEncode, "AMREncodeInit");
		amr_data->AMREncodeReset = dlsym(amr_data->libEncode, "AMREncodeReset");
		amr_data->AMREncodeExit = dlsym(amr_data->libEncode, "AMREncodeExit");
		amr_data->AMREncode = dlsym(amr_data->libEncode, "AMREncode");

		amr_data->libDecode = dlopen("libstagefright_soft_amrdec.so", RTLD_LAZY);
		if(amr_data->libDecode != NULL){
			amr_data->GSMInitDecode = dlsym(amr_data->libDecode, "GSMInitDecode");
			amr_data->AMRDecode = dlsym(amr_data->libDecode, "AMRDecode");
			amr_data->Speech_Decode_Frame_reset = dlsym(amr_data->libDecode, "Speech_Decode_Frame_reset");
			amr_data->GSMDecodeFrameExit = dlsym(amr_data->libDecode, "GSMDecodeFrameExit");
		}

	    return dlcheck_sym(amr_data);
	}
	return PJ_EINVAL;
}


pj_status_t dlsym_stagefright_23(struct amr_data* amr_data){
	amr_data->libEncode = dlopen("libstagefright.so", RTLD_LAZY);
	if(amr_data->libEncode != NULL){
		amr_data->AMREncodeInit = dlsym(amr_data->libEncode, "AMREncodeInit");
		amr_data->AMREncodeReset = dlsym(amr_data->libEncode, "AMREncodeReset");
		amr_data->AMREncodeExit = dlsym(amr_data->libEncode, "AMREncodeExit");
		amr_data->AMREncode = dlsym(amr_data->libEncode, "AMREncode");
		amr_data->GSMInitDecode = dlsym(amr_data->libEncode, "GSMInitDecode");
		amr_data->AMRDecode = dlsym(amr_data->libEncode, "AMRDecode");
		amr_data->Speech_Decode_Frame_reset = dlsym(amr_data->libEncode, "Speech_Decode_Frame_reset");
		amr_data->GSMDecodeFrameExit = dlsym(amr_data->libEncode, "GSMDecodeFrameExit");

	    return dlcheck_sym(amr_data);
	}
	return PJ_EINVAL;
}

pj_status_t dlsym_stagefright_21(struct amr_data* amr_data){
	amr_data->libEncode = dlopen("libomx_amrenc_sharedlibrary.so", RTLD_LAZY);
	if(amr_data->libEncode != NULL){
		amr_data->AMREncodeInit = dlsym(amr_data->libEncode, "AMREncodeInit");
		amr_data->AMREncodeReset = dlsym(amr_data->libEncode, "AMREncodeReset");
		amr_data->AMREncodeExit = dlsym(amr_data->libEncode, "AMREncodeExit");
		amr_data->AMREncode = dlsym(amr_data->libEncode, "AMREncode");

		amr_data->libDecode = dlopen("libomx_amrdec_sharedlibrary.so", RTLD_LAZY);
		if(amr_data->libDecode != NULL){
			amr_data->GSMInitDecode = dlsym(amr_data->libDecode, "GSMInitDecode");
			amr_data->AMRDecode = dlsym(amr_data->libDecode, "AMRDecode");
			amr_data->Speech_Decode_Frame_reset = dlsym(amr_data->libDecode, "Speech_Decode_Frame_reset");
			amr_data->GSMDecodeFrameExit = dlsym(amr_data->libDecode, "GSMDecodeFrameExit");
		}

	    return dlcheck_sym(amr_data);
	}
	return PJ_EINVAL;
}

pj_status_t dlcheck_sym(struct amr_data* amr_data){
	 if(amr_data->AMREncodeReset == NULL
	    		|| amr_data->AMREncodeExit == NULL
	    		|| amr_data->AMREncode == NULL
	    		|| amr_data->GSMInitDecode == NULL
	    		|| amr_data->AMRDecode == NULL
	    		|| amr_data->GSMDecodeFrameExit == NULL ){
			return PJ_EINVAL;
	}
	 return PJ_SUCCESS;
}

pj_status_t dlclose_stagefright(struct amr_data* amr_data){
	if(amr_data != NULL && amr_data->libEncode != NULL){
		PJ_LOG(4, (THIS_FILE, "Close encode lib"));
		dlclose(amr_data->libEncode);
		amr_data->libEncode = NULL;
	}
	if(amr_data != NULL && amr_data->libDecode != NULL){
		PJ_LOG(4, (THIS_FILE, "Close decode lib"));
		dlclose(amr_data->libDecode);
		amr_data->libDecode = NULL;
	}
	amr_data->AMREncodeReset = NULL;
	amr_data->AMREncodeExit = NULL;
	amr_data->AMREncode = NULL;
	amr_data->GSMInitDecode = NULL;
	amr_data->AMRDecode = NULL;
	amr_data->GSMDecodeFrameExit = NULL;

	return PJ_SUCCESS;
}

void* Decoder_Interface_init(struct amr_data* amr_data) {
	void* ptr = NULL;
	amr_data->GSMInitDecode(&ptr, (pj_uint8_t*)"Decoder");
	return ptr;
}

void Decoder_Interface_exit(struct amr_data* amr_data, void* state) {
	amr_data->GSMDecodeFrameExit(&state);
}

void Decoder_Interface_Decode(struct amr_data* amr_data, void* state, const unsigned char* in, short* out, int bfi) {
	unsigned char type = (in[0] >> 3) & 0x0f;
	in++;
	amr_data->AMRDecode(state, (enum Frame_Type_3GPP) type, (pj_uint8_t*) in, out, MIME_IETF);
}

struct encoder_state {
	void* encCtx;
	void* pidSyncCtx;
};

void* Encoder_Interface_init(struct amr_data* amr_data, int dtx) {
	struct encoder_state* state = (struct encoder_state*) malloc(sizeof(struct encoder_state));
	amr_data->AMREncodeInit(&state->encCtx, &state->pidSyncCtx, dtx);
	return state;
}

void Encoder_Interface_exit(struct amr_data* amr_data, void* s) {
	struct encoder_state* state = (struct encoder_state*) s;
	amr_data->AMREncodeExit(&state->encCtx, &state->pidSyncCtx);
	free(state);
}

int Encoder_Interface_Encode(struct amr_data* amr_data, void* s, enum Mode mode, const short* speech, unsigned char* out, int forceSpeech) {
	struct encoder_state* state = (struct encoder_state*) s;
	enum Frame_Type_3GPP frame_type = (enum Frame_Type_3GPP) mode;
	int ret = amr_data->AMREncode(state->encCtx, state->pidSyncCtx, mode, (pj_uint16_t*) speech, out, &frame_type, AMR_TX_WMF);
	out[0] = ((frame_type & 0x0f) << 3) | 0x04;
	return ret;
}

/*
 * Initialize and register AMR-NB codec factory to pjmedia endpoint.
 */
PJ_DEF(pj_status_t) pjmedia_codec_opencore_amrnb_init( pjmedia_endpt *endpt )
{
    pjmedia_codec_mgr *codec_mgr;
    pj_status_t status;
    pj_str_t codec_name;

    if (amr_codec_factory.pool != NULL)
	return PJ_SUCCESS;

    /* Create AMR-NB codec factory. */
    amr_codec_factory.base.op = &amr_factory_op;
    amr_codec_factory.base.factory_data = NULL;
    amr_codec_factory.endpt = endpt;

    amr_codec_factory.pool = pjmedia_endpt_create_pool(endpt, "amrnb", 1000,
						       1000);
    if (!amr_codec_factory.pool)
	return PJ_ENOMEM;

    /* Get the codec manager. */
    codec_mgr = pjmedia_endpt_get_codec_mgr(endpt);
    if (!codec_mgr) {
	status = PJ_EINVALIDOP;
	goto on_error;
    }
    /* Register format match callback. */
    pj_cstr(&codec_name, "AMR");
    status = pjmedia_sdp_neg_register_fmt_match_cb( &codec_name,
    		&pjmedia_codec_amr_match_sdp);
    if (status != PJ_SUCCESS){
    	goto on_error;
    }
    /* Register codec factory to endpoint. */
    status = pjmedia_codec_mgr_register_factory(codec_mgr,
						&amr_codec_factory.base);
    if (status != PJ_SUCCESS)
	goto on_error;

    /* Done. */
    return PJ_SUCCESS;

on_error:
    pj_pool_release(amr_codec_factory.pool);
    amr_codec_factory.pool = NULL;
    return status;
}


/*
 * Unregister AMR-NB codec factory from pjmedia endpoint and deinitialize
 * the AMR-NB codec library.
 */
PJ_DEF(pj_status_t) pjmedia_codec_opencore_amrnb_deinit(void)
{
    pjmedia_codec_mgr *codec_mgr;
    pj_status_t status;

    if (amr_codec_factory.pool == NULL)
	return PJ_SUCCESS;

    /* Get the codec manager. */
    codec_mgr = pjmedia_endpt_get_codec_mgr(amr_codec_factory.endpt);
    if (!codec_mgr) {
	pj_pool_release(amr_codec_factory.pool);
	amr_codec_factory.pool = NULL;
	return PJ_EINVALIDOP;
    }

    /* Unregister AMR-NB codec factory. */
    status = pjmedia_codec_mgr_unregister_factory(codec_mgr,
						  &amr_codec_factory.base);

    /* Destroy pool. */
    pj_pool_release(amr_codec_factory.pool);
    amr_codec_factory.pool = NULL;

    return status;
}


PJ_DEF(pj_status_t) pjmedia_codec_opencore_amrnb_set_config(
			    const pjmedia_codec_amrnb_config *config)
{
    unsigned nbitrates;


    def_config = *config;

    /* Normalize bitrate. */
    nbitrates = PJ_ARRAY_SIZE(pjmedia_codec_amrnb_bitrates);
    if (def_config.bitrate < pjmedia_codec_amrnb_bitrates[0])
	def_config.bitrate = pjmedia_codec_amrnb_bitrates[0];
    else if (def_config.bitrate > pjmedia_codec_amrnb_bitrates[nbitrates-1])
	def_config.bitrate = pjmedia_codec_amrnb_bitrates[nbitrates-1];
    else
    {
	unsigned i;

	for (i = 0; i < nbitrates; ++i) {
	    if (def_config.bitrate <= pjmedia_codec_amrnb_bitrates[i])
		break;
	}
	def_config.bitrate = pjmedia_codec_amrnb_bitrates[i];
    }

    return PJ_SUCCESS;
}

/*
 * Check if factory can allocate the specified codec.
 */
static pj_status_t amr_test_alloc( pjmedia_codec_factory *factory,
				   const pjmedia_codec_info *info )
{
    PJ_UNUSED_ARG(factory);
    const pj_str_t amr_tag = {"AMR", 3};


    /* Type MUST be audio. */
    if (info->type != PJMEDIA_TYPE_AUDIO)
    return PJMEDIA_CODEC_EUNSUP;

    /* Check encoding name. */
    if (pj_stricmp(&info->encoding_name, &amr_tag) != 0)
    return PJMEDIA_CODEC_EUNSUP;

    /* Channel count must be one */
    if (info->channel_cnt != 1)
    return PJMEDIA_CODEC_EUNSUP;

    if(info->clock_rate != 8000)
    return PJMEDIA_CODEC_EUNSUP;

    return PJ_SUCCESS;
}

/*
 * Generate default attribute.
 */
static pj_status_t amr_default_attr( pjmedia_codec_factory *factory,
				     const pjmedia_codec_info *id,
				     pjmedia_codec_param *attr )
{
    PJ_UNUSED_ARG(factory);
    PJ_UNUSED_ARG(id);

    pj_bzero(attr, sizeof(pjmedia_codec_param));
    attr->info.clock_rate = 8000;
    attr->info.channel_cnt = 1;
    attr->info.avg_bps = def_config.bitrate;
    attr->info.max_bps = pjmedia_codec_amrnb_bitrates[7];
    attr->info.pcm_bits_per_sample = 16;
    attr->info.frm_ptime = 20;
    attr->info.pt = PJMEDIA_RTP_PT_AMR;

    attr->setting.frm_per_pkt = 2;
    attr->setting.vad = 1;
    attr->setting.plc = 1;

    if (def_config.octet_align) {
	attr->setting.dec_fmtp.cnt = 1;
	attr->setting.dec_fmtp.param[0].name = pj_str("octet-align");
	attr->setting.dec_fmtp.param[0].val = pj_str("1");
    }

    /* Default all other flag bits disabled. */

    return PJ_SUCCESS;
}


/*
 * Enum codecs supported by this factory (i.e. only AMR-NB!).
 */
static pj_status_t amr_enum_codecs( pjmedia_codec_factory *factory,
				    unsigned *count,
				    pjmedia_codec_info codecs[])
{
    PJ_UNUSED_ARG(factory);
    PJ_ASSERT_RETURN(codecs && *count > 0, PJ_EINVAL);
    pj_status_t res = PJ_SUCCESS;

    struct amr_data amr_data;
    res = dlsym_stagefright(&amr_data);
    if(res != PJ_SUCCESS){
    	// No lib available
    	*count = 0;
    	return PJ_SUCCESS;
    }
    PJ_LOG(4, (THIS_FILE, "Found AMR dyn lib(s)"));
    dlclose_stagefright(&amr_data);


    pj_bzero(&codecs[0], sizeof(pjmedia_codec_info));
    codecs[0].encoding_name = pj_str("AMR");
    codecs[0].pt = PJMEDIA_RTP_PT_AMR;
    codecs[0].type = PJMEDIA_TYPE_AUDIO;
    codecs[0].clock_rate = 8000;
    codecs[0].channel_cnt = 1;

    *count = 1;

    return PJ_SUCCESS;
}


/*
 * Allocate a new AMR-NB codec instance.
 */
static pj_status_t amr_alloc_codec( pjmedia_codec_factory *factory,
				    const pjmedia_codec_info *id,
				    pjmedia_codec **p_codec)
{
    pj_pool_t *pool;
    pjmedia_codec *codec;
    struct amr_data *amr_data;
    pj_status_t status = PJ_SUCCESS;

    PJ_ASSERT_RETURN(factory && id && p_codec, PJ_EINVAL);
    PJ_ASSERT_RETURN(factory == &amr_codec_factory.base, PJ_EINVAL);

    pool = pjmedia_endpt_create_pool(amr_codec_factory.endpt, "amrnb-inst",
				     512, 512);

    codec = PJ_POOL_ZALLOC_T(pool, pjmedia_codec);
    PJ_ASSERT_RETURN(codec != NULL, PJ_ENOMEM);
    codec->op = &amr_op;
    codec->factory = factory;

    amr_data = PJ_POOL_ZALLOC_T(pool, struct amr_data);
    codec->codec_data = amr_data;
    amr_data->pool = pool;

#if USE_PJMEDIA_PLC
    /* Create PLC */
    status = pjmedia_plc_create(pool, 8000, 160, 0, &amr_data->plc);
    if (status != PJ_SUCCESS) {
	return status;
    }
#else
    PJ_UNUSED_ARG(status);
#endif

    status = dlsym_stagefright(amr_data);
    if(status != PJ_SUCCESS){
    	return status;
    }

    *p_codec = codec;
    return PJ_SUCCESS;
}


/*
 * Free codec.
 */
static pj_status_t amr_dealloc_codec( pjmedia_codec_factory *factory,
				      pjmedia_codec *codec )
{
    struct amr_data *amr_data;

    PJ_ASSERT_RETURN(factory && codec, PJ_EINVAL);
    PJ_ASSERT_RETURN(factory == &amr_codec_factory.base, PJ_EINVAL);

    amr_data = (struct amr_data*) codec->codec_data;

    /* Close codec, if it's not closed. */
    amr_codec_close(codec);

    pj_pool_release(amr_data->pool);

    dlclose_stagefright(amr_data);

    amr_data = NULL;

    return PJ_SUCCESS;
}

/*
 * Init codec.
 */
static pj_status_t amr_codec_init( pjmedia_codec *codec,
				   pj_pool_t *pool )
{
    PJ_UNUSED_ARG(codec);
    PJ_UNUSED_ARG(pool);
    return PJ_SUCCESS;
}


/*
 * Open codec.
 */
static pj_status_t amr_codec_open( pjmedia_codec *codec,
				   pjmedia_codec_param *attr )
{
    struct amr_data *amr_data = (struct amr_data*) codec->codec_data;
    pjmedia_codec_amr_pack_setting *setting;
    unsigned i;
    pj_uint8_t octet_align = 0;
    pj_int8_t enc_mode;
    const pj_str_t STR_FMTP_OCTET_ALIGN = {"octet-align", 11};

    PJ_ASSERT_RETURN(codec && attr, PJ_EINVAL);
    PJ_ASSERT_RETURN(amr_data != NULL, PJ_EINVALIDOP);

    enc_mode = pjmedia_codec_amr_get_mode(attr->info.avg_bps);
    pj_assert(enc_mode >= 0 && enc_mode <= 7);

    /* Check octet-align */
    for (i = 0; i < attr->setting.dec_fmtp.cnt; ++i) {
	if (pj_stricmp(&attr->setting.dec_fmtp.param[i].name,
		       &STR_FMTP_OCTET_ALIGN) == 0)
	{
	    octet_align = (pj_uint8_t)
			  (pj_strtoul(&attr->setting.dec_fmtp.param[i].val));
	    break;
	}
    }

    /* Check mode-set */
    for (i = 0; i < attr->setting.enc_fmtp.cnt; ++i) {
	const pj_str_t STR_FMTP_MODE_SET = {"mode-set", 8};

	if (pj_stricmp(&attr->setting.enc_fmtp.param[i].name,
		       &STR_FMTP_MODE_SET) == 0)
	{
	    const char *p;
	    pj_size_t l;
	    pj_int8_t diff = 99;

	    /* Encoding mode is chosen based on local default mode setting:
	     * - if local default mode is included in the mode-set, use it
	     * - otherwise, find the closest mode to local default mode;
	     *   if there are two closest modes, prefer to use the higher
	     *   one, e.g: local default mode is 4, the mode-set param
	     *   contains '2,3,5,6', then 5 will be chosen.
	     */
	    p = pj_strbuf(&attr->setting.enc_fmtp.param[i].val);
	    l = pj_strlen(&attr->setting.enc_fmtp.param[i].val);
	    while (l--) {
		if (*p>='0' && *p<='7') {
		    pj_int8_t tmp = *p - '0' - enc_mode;

		    if (PJ_ABS(diff) > PJ_ABS(tmp) ||
			(PJ_ABS(diff) == PJ_ABS(tmp) && tmp > diff))
		    {
			diff = tmp;
			if (diff == 0) break;
		    }
		}
		++p;
	    }
	    PJ_ASSERT_RETURN(diff != 99, PJMEDIA_CODEC_EFAILED);

	    enc_mode = enc_mode + diff;

	    break;
	}
    }

    amr_data->vad_enabled = (attr->setting.vad != 0);
    amr_data->plc_enabled = (attr->setting.plc != 0);
    amr_data->enc_mode = enc_mode;

    amr_data->encoder = Encoder_Interface_init(amr_data, amr_data->vad_enabled);
    if (amr_data->encoder == NULL) {
	TRACE_((THIS_FILE, "Encoder_Interface_init() failed"));
	amr_codec_close(codec);
	return PJMEDIA_CODEC_EFAILED;
    }
    setting = &amr_data->enc_setting;
    pj_bzero(setting, sizeof(pjmedia_codec_amr_pack_setting));
    setting->amr_nb = 1;
    setting->reorder = 0;
    setting->octet_aligned = octet_align;
    setting->cmr = 15;

    amr_data->decoder = Decoder_Interface_init(amr_data);
    if (amr_data->decoder == NULL) {
	TRACE_((THIS_FILE, "Decoder_Interface_init() failed"));
	amr_codec_close(codec);
	return PJMEDIA_CODEC_EFAILED;
    }
    setting = &amr_data->dec_setting;
    pj_bzero(setting, sizeof(pjmedia_codec_amr_pack_setting));
    setting->amr_nb = 1;
    setting->reorder = 0;
    setting->octet_aligned = octet_align;

    TRACE_((THIS_FILE, "AMR-NB codec allocated: vad=%d, plc=%d, bitrate=%d",
			amr_data->vad_enabled, amr_data->plc_enabled,
			pjmedia_codec_amrnb_bitrates[amr_data->enc_mode]));
    return PJ_SUCCESS;
}


/*
 * Close codec.
 */
static pj_status_t amr_codec_close( pjmedia_codec *codec )
{
    struct amr_data *amr_data;

    PJ_ASSERT_RETURN(codec, PJ_EINVAL);

    amr_data = (struct amr_data*) codec->codec_data;
    PJ_ASSERT_RETURN(amr_data != NULL, PJ_EINVALIDOP);

    if (amr_data->encoder) {
        Encoder_Interface_exit(amr_data, amr_data->encoder);
        amr_data->encoder = NULL;
    }

    if (amr_data->decoder) {
        Decoder_Interface_exit(amr_data, amr_data->decoder);
        amr_data->decoder = NULL;
    }

    TRACE_((THIS_FILE, "AMR-NB codec closed"));
    return PJ_SUCCESS;
}


/*
 * Modify codec settings.
 */
static pj_status_t amr_codec_modify( pjmedia_codec *codec,
				     const pjmedia_codec_param *attr )
{
    struct amr_data *amr_data = (struct amr_data*) codec->codec_data;
    pj_bool_t prev_vad_state;

    pj_assert(amr_data != NULL);
    pj_assert(amr_data->encoder != NULL && amr_data->decoder != NULL);

    prev_vad_state = amr_data->vad_enabled;
    amr_data->vad_enabled = (attr->setting.vad != 0);
    amr_data->plc_enabled = (attr->setting.plc != 0);

    if (prev_vad_state != amr_data->vad_enabled) {
	/* Reinit AMR encoder to update VAD setting */
	TRACE_((THIS_FILE, "Reiniting AMR encoder to update VAD setting."));
        Encoder_Interface_exit(amr_data, amr_data->encoder);
        amr_data->encoder = Encoder_Interface_init(amr_data, amr_data->vad_enabled);
        if (amr_data->encoder == NULL) {
	    TRACE_((THIS_FILE, "Encoder_Interface_init() failed"));
	    amr_codec_close(codec);
	    return PJMEDIA_CODEC_EFAILED;
	}
    }

    TRACE_((THIS_FILE, "AMR-NB codec modified: vad=%d, plc=%d",
			amr_data->vad_enabled, amr_data->plc_enabled));
    return PJ_SUCCESS;
}


/*
 * Get frames in the packet.
 */
static pj_status_t amr_codec_parse( pjmedia_codec *codec,
				    void *pkt,
				    pj_size_t pkt_size,
				    const pj_timestamp *ts,
				    unsigned *frame_cnt,
				    pjmedia_frame frames[])
{
    struct amr_data *amr_data = (struct amr_data*) codec->codec_data;
    pj_uint8_t cmr;
    pj_status_t status;

    status = pjmedia_codec_amr_parse(pkt, pkt_size, ts, &amr_data->dec_setting,
				     frames, frame_cnt, &cmr);
    if (status != PJ_SUCCESS)
	return status;

    /* Check for Change Mode Request. */
    if (cmr <= 7 && amr_data->enc_mode != cmr) {
	amr_data->enc_mode = cmr;
	TRACE_((THIS_FILE, "AMR-NB encoder switched mode to %d (%dbps)",
			    amr_data->enc_mode,
			    pjmedia_codec_amrnb_bitrates[amr_data->enc_mode]));
    }

    return PJ_SUCCESS;
}


/*
 * Encode frame.
 */
static pj_status_t amr_codec_encode( pjmedia_codec *codec,
				     const struct pjmedia_frame *input,
				     unsigned output_buf_len,
				     struct pjmedia_frame *output)
{
    struct amr_data *amr_data = (struct amr_data*) codec->codec_data;
    unsigned char *bitstream;
    pj_int16_t *speech;
    unsigned nsamples, samples_per_frame;
    enum {MAX_FRAMES_PER_PACKET = 16};
    pjmedia_frame frames[MAX_FRAMES_PER_PACKET];
    pj_uint8_t *p;
    unsigned i, out_size = 0, nframes = 0;
    pj_size_t payload_len;
    unsigned dtx_cnt, sid_cnt;
    pj_status_t status;
    int size;

    pj_assert(amr_data != NULL);
    PJ_ASSERT_RETURN(input && output, PJ_EINVAL);

    nsamples = input->size >> 1;
    samples_per_frame = 160;
    PJ_ASSERT_RETURN(nsamples % samples_per_frame == 0,
		     PJMEDIA_CODEC_EPCMFRMINLEN);

    nframes = nsamples / samples_per_frame;
    PJ_ASSERT_RETURN(nframes <= MAX_FRAMES_PER_PACKET,
		     PJMEDIA_CODEC_EFRMTOOSHORT);

    /* Encode the frames */
    speech = (pj_int16_t*)input->buf;
    bitstream = (unsigned char*)output->buf;
    while (nsamples >= samples_per_frame) {
        size = Encoder_Interface_Encode (amr_data, amr_data->encoder, amr_data->enc_mode,
                                         speech, bitstream, 0);
	if (size == 0) {
	    output->size = 0;
	    output->buf = NULL;
	    output->type = PJMEDIA_FRAME_TYPE_NONE;
	    TRACE_((THIS_FILE, "AMR-NB encode() failed"));
	    return PJMEDIA_CODEC_EFAILED;
	}
	nsamples -= 160;
	speech += samples_per_frame;
	bitstream += size;
	out_size += size;
	TRACE_((THIS_FILE, "AMR-NB encode(): mode=%d, size=%d",
		amr_data->enc_mode, out_size));
    }

    /* Pack payload */
    p = (pj_uint8_t*)output->buf + output_buf_len - out_size;
    pj_memmove(p, output->buf, out_size);
    dtx_cnt = sid_cnt = 0;
    for (i = 0; i < nframes; ++i) {
	pjmedia_codec_amr_bit_info *info = (pjmedia_codec_amr_bit_info*)
					   &frames[i].bit_info;
	info->frame_type = (pj_uint8_t)((*p >> 3) & 0x0F);
	info->good_quality = (pj_uint8_t)((*p >> 2) & 0x01);
	info->mode = (pj_int8_t)amr_data->enc_mode;
	info->start_bit = 0;
	frames[i].buf = p + 1;
	frames[i].size = (info->frame_type <= 8)?
			 pjmedia_codec_amrnb_framelen[info->frame_type] : 0;
	p += frames[i].size + 1;

	/* Count the number of SID and DTX frames */
	if (info->frame_type == 15) /* DTX*/
	    ++dtx_cnt;
	else if (info->frame_type == 8) /* SID */
	    ++sid_cnt;
    }

    /* VA generates DTX frames as DTX+SID frames switching quickly and it
     * seems that the SID frames occur too often (assuming the purpose is
     * only for keeping NAT alive?). So let's modify the behavior a bit.
     * Only an SID frame will be sent every PJMEDIA_CODEC_MAX_SILENCE_PERIOD
     * milliseconds.
     */
    if (sid_cnt + dtx_cnt == nframes) {
	pj_int32_t dtx_duration;

	dtx_duration = pj_timestamp_diff32(&amr_data->last_tx,
					   &input->timestamp);
	if (PJMEDIA_CODEC_MAX_SILENCE_PERIOD == -1 ||
	    dtx_duration < PJMEDIA_CODEC_MAX_SILENCE_PERIOD*8000/1000)
	{
	    output->size = 0;
	    output->type = PJMEDIA_FRAME_TYPE_NONE;
	    output->timestamp = input->timestamp;
	    return PJ_SUCCESS;
	}
    }

    payload_len = output_buf_len;

    status = pjmedia_codec_amr_pack(frames, nframes, &amr_data->enc_setting,
				    output->buf, &payload_len);
    if (status != PJ_SUCCESS) {
	output->size = 0;
	output->buf = NULL;
	output->type = PJMEDIA_FRAME_TYPE_NONE;
	TRACE_((THIS_FILE, "Failed to pack AMR payload, status=%d", status));
	return status;
    }

    output->size = payload_len;
    output->type = PJMEDIA_FRAME_TYPE_AUDIO;
    output->timestamp = input->timestamp;

    amr_data->last_tx = input->timestamp;

    return PJ_SUCCESS;
}


/*
 * Decode frame.
 */
static pj_status_t amr_codec_decode( pjmedia_codec *codec,
				     const struct pjmedia_frame *input,
				     unsigned output_buf_len,
				     struct pjmedia_frame *output)
{
    struct amr_data *amr_data = (struct amr_data*) codec->codec_data;
    pjmedia_frame input_;
    pjmedia_codec_amr_bit_info *info;
    /* VA AMR-NB decoding buffer: AMR-NB max frame size + 1 byte header. */
    unsigned char bitstream[32];

    pj_assert(amr_data != NULL);
    PJ_ASSERT_RETURN(input && output, PJ_EINVAL);

    if (output_buf_len < 320)
	return PJMEDIA_CODEC_EPCMTOOSHORT;

    input_.buf = &bitstream[1];
    input_.size = 31; /* AMR-NB max frame size */
    pjmedia_codec_amr_predecode(input, &amr_data->dec_setting, &input_);
    info = (pjmedia_codec_amr_bit_info*)&input_.bit_info;

    /* VA AMRNB decoder requires frame info in the first byte. */
    bitstream[0] = (info->frame_type << 3) | (info->good_quality << 2);

    TRACE_((THIS_FILE, "AMR-NB decode(): mode=%d, ft=%d, size=%d",
	    info->mode, info->frame_type, input_.size));

    /* Decode */
    Decoder_Interface_Decode(amr_data, amr_data->decoder, bitstream,
                             (pj_int16_t*)output->buf, 0);

    output->size = 320;
    output->type = PJMEDIA_FRAME_TYPE_AUDIO;
    output->timestamp = input->timestamp;

#if USE_PJMEDIA_PLC
    if (amr_data->plc_enabled)
	pjmedia_plc_save(amr_data->plc, (pj_int16_t*)output->buf);
#endif

    return PJ_SUCCESS;
}


/*
 * Recover lost frame.
 */
#if USE_PJMEDIA_PLC
/*
 * Recover lost frame.
 */
static pj_status_t  amr_codec_recover( pjmedia_codec *codec,
				       unsigned output_buf_len,
				       struct pjmedia_frame *output)
{
    struct amr_data *amr_data = codec->codec_data;

    TRACE_((THIS_FILE, "amr_codec_recover"));

    PJ_ASSERT_RETURN(amr_data->plc_enabled, PJ_EINVALIDOP);

    PJ_ASSERT_RETURN(output_buf_len >= 320,  PJMEDIA_CODEC_EPCMTOOSHORT);

    pjmedia_plc_generate(amr_data->plc, (pj_int16_t*)output->buf);

    output->size = 320;
    output->type = PJMEDIA_FRAME_TYPE_AUDIO;

    return PJ_SUCCESS;
}
#endif


#endif
