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
#include <pjmedia/alaw_ulaw.h>
#include <pjmedia/endpoint.h>
#include <pjmedia/errno.h>
#include <pjmedia/port.h>
#include <pjmedia-codec/types.h>
#include <pjmedia/plc.h>
#include <pjmedia/silencedet.h>
#include <pj/pool.h>
#include <pj/string.h>
#include <pj/assert.h>
#include <pj/log.h>


#if defined(PJMEDIA_HAS_WEBRTC_CODEC) && (PJMEDIA_HAS_WEBRTC_CODEC!=0)

#include <modules/audio_coding/main/interface/audio_coding_module.h>
#include <modules/utility/source/coder.h>

#define THIS_FILE       "webrtc_codec.c"

using namespace webrtc;

PJ_BEGIN_DECL
PJ_DEF(pj_status_t) pjmedia_codec_webrtc_init(pjmedia_endpt *endpt);
PJ_DEF(pj_status_t) pjmedia_codec_webrtc_deinit(void);
PJ_END_DECL
/* Prototypes for webrtc factory */
static pj_status_t webrtc_test_alloc(pjmedia_codec_factory *factory,
		const pjmedia_codec_info *id);
static pj_status_t webrtc_default_attr(pjmedia_codec_factory *factory,
		const pjmedia_codec_info *id, pjmedia_codec_param *attr);
static pj_status_t webrtc_enum_codecs(pjmedia_codec_factory *factory,
		unsigned *count, pjmedia_codec_info codecs[]);
static pj_status_t webrtc_alloc_codec(pjmedia_codec_factory *factory,
		const pjmedia_codec_info *id, pjmedia_codec **p_codec);
static pj_status_t webrtc_dealloc_codec(pjmedia_codec_factory *factory,
		pjmedia_codec *codec);

/* Prototypes for webRTC implementation. */
static pj_status_t webrtc_init(pjmedia_codec *codec, pj_pool_t *pool);
static pj_status_t webrtc_open(pjmedia_codec *codec, pjmedia_codec_param *attr);
static pj_status_t webrtc_close(pjmedia_codec *codec);
static pj_status_t webrtc_modify(pjmedia_codec *codec,
		const pjmedia_codec_param *attr);
static pj_status_t webrtc_parse(pjmedia_codec *codec, void *pkt,
		pj_size_t pkt_size, const pj_timestamp *timestamp, unsigned *frame_cnt,
		pjmedia_frame frames[]);
static pj_status_t webrtc_encode(pjmedia_codec *codec,
		const struct pjmedia_frame *input, unsigned output_buf_len,
		struct pjmedia_frame *output);
static pj_status_t webrtc_decode(pjmedia_codec *codec,
		const struct pjmedia_frame *input, unsigned output_buf_len,
		struct pjmedia_frame *output);
static pj_status_t webrtc_recover(pjmedia_codec *codec, unsigned output_buf_len,
		struct pjmedia_frame *output);

/* Definition for webrtc codec operations. */
static pjmedia_codec_op webrtc_op = { &webrtc_init, &webrtc_open, &webrtc_close,
		&webrtc_modify, &webrtc_parse, &webrtc_encode, &webrtc_decode,
		&webrtc_recover };

/* Definition for webRTC codec factory operations. */
static pjmedia_codec_factory_op webrtc_factory_op = {
		&webrtc_test_alloc,
		&webrtc_default_attr,
		&webrtc_enum_codecs,
		&webrtc_alloc_codec,
		&webrtc_dealloc_codec,
		&pjmedia_codec_webrtc_deinit
};
static char CHAR_MODE[] = "mode";
static pj_str_t STR_MODE = {CHAR_MODE, 4};

/* webRTC factory private data */
static struct webrtc_factory {
	pjmedia_codec_factory base;
	pjmedia_endpt *endpt;
	pj_pool_t *pool;
	pj_mutex_t *mutex;
} webrtc_factory;

/* webRTC codec private data. */
struct webrtc_private {
	pj_pool_t *pool; /**< Pool for each instance.    */

	AudioCoder* coder;

	unsigned pt;
	unsigned clock_rate;
	unsigned ptime;
	int channels;
	unsigned pacbsize;
};

PJ_DEF(pj_status_t) pjmedia_codec_webrtc_init(pjmedia_endpt *endpt) {
	pjmedia_codec_mgr *codec_mgr;
	pj_status_t status;

	if (webrtc_factory.endpt != NULL) {
		/* Already initialized. */
		return PJ_SUCCESS;
	}

	/* Init factory */
	webrtc_factory.base.op = &webrtc_factory_op;
	webrtc_factory.base.factory_data = NULL;
	webrtc_factory.endpt = endpt;

	/* Create pool */
	webrtc_factory.pool = pjmedia_endpt_create_pool(endpt, "webrtc codecs",
			4000, 4000);
	if (!webrtc_factory.pool)
		return PJ_ENOMEM;

	/* Create mutex. */
	status = pj_mutex_create_simple(webrtc_factory.pool, "webrtc codecs",
			&webrtc_factory.mutex);
	if (status != PJ_SUCCESS
		)
		goto on_error;

	/* Get the codec manager. */
	codec_mgr = pjmedia_endpt_get_codec_mgr(endpt);
	if (!codec_mgr) {
		return PJ_EINVALIDOP;
	}

	/* Register codec factory to endpoint. */
	status = pjmedia_codec_mgr_register_factory(codec_mgr,
			&webrtc_factory.base);
	if (status != PJ_SUCCESS
		)
		return status;

	return PJ_SUCCESS;

	on_error: if (webrtc_factory.mutex) {
		pj_mutex_destroy(webrtc_factory.mutex);
		webrtc_factory.mutex = NULL;
	}
	if (webrtc_factory.pool) {
		pj_pool_release(webrtc_factory.pool);
		webrtc_factory.pool = NULL;
	}

	return status;
}

PJ_DEF(pj_status_t) pjmedia_codec_webrtc_deinit(void) {
	pjmedia_codec_mgr *codec_mgr;
	pj_status_t status;

	if (webrtc_factory.endpt == NULL) {
		/* Not registered. */
		return PJ_SUCCESS;
	}

	/* Lock mutex. */
	pj_mutex_lock(webrtc_factory.mutex);

	/* Get the codec manager. */
	codec_mgr = pjmedia_endpt_get_codec_mgr(webrtc_factory.endpt);
	if (!codec_mgr) {
		webrtc_factory.endpt = NULL;
		pj_mutex_unlock(webrtc_factory.mutex);
		return PJ_EINVALIDOP;
	}

	/* Unregister webRTC codec factory. */
	status = pjmedia_codec_mgr_unregister_factory(codec_mgr,
			&webrtc_factory.base);
	webrtc_factory.endpt = NULL;

	/* Destroy mutex. */
	pj_mutex_destroy(webrtc_factory.mutex);
	webrtc_factory.mutex = NULL;

	/* Release pool. */
	pj_pool_release(webrtc_factory.pool);
	webrtc_factory.pool = NULL;

	return status;
}

// Find the correct webrtc codec regarding pjsip codec infos
pj_status_t find_codec(unsigned pt, unsigned clock_rate, unsigned channel_cnt,
		CodecInst *codec) {
	unsigned i;
	CodecInst codecParam;

	for (i = 0; i < AudioCodingModule::NumberOfCodecs(); i++) {
		AudioCodingModule::Codec(i, codecParam);
		if (pt == codecParam.pltype && channel_cnt == codecParam.channels
				&& clock_rate == codecParam.plfreq) {
		    // TODO : we should use encoding name instead
		    if(pt == PJMEDIA_RTP_PT_ILBC || pt == PJMEDIA_RTP_PT_ISAC_WB || pt == PJMEDIA_RTP_PT_ISAC_UWB ){
                pj_memcpy(codec, &codecParam, sizeof(CodecInst));
                return PJ_SUCCESS;
		    }

		}
	}
	return PJMEDIA_ERROR;
}

static pj_status_t webrtc_test_alloc(pjmedia_codec_factory *factory,
		const pjmedia_codec_info *id) {
	PJ_UNUSED_ARG(factory);
	CodecInst codec;

	return find_codec(id->pt, id->clock_rate, id->channel_cnt, &codec);

}

static pj_status_t webrtc_default_attr(pjmedia_codec_factory *factory,
		const pjmedia_codec_info *id, pjmedia_codec_param *attr) {
	PJ_UNUSED_ARG(factory);
	CodecInst codecParam;
	pj_status_t status;

	pj_bzero(attr, sizeof(pjmedia_codec_param));

	status = find_codec(id->pt, id->clock_rate, id->channel_cnt, &codecParam);
	attr->info.clock_rate = codecParam.plfreq;
	attr->info.channel_cnt = codecParam.channels;
	attr->info.avg_bps = codecParam.rate * codecParam.plfreq / 8000;
	attr->info.max_bps = codecParam.rate * codecParam.plfreq / 8000;
	if(id->pt == PJMEDIA_RTP_PT_ILBC){
		attr->info.max_bps = 15200;
	}
	attr->info.pcm_bits_per_sample = 16;
	attr->info.frm_ptime = codecParam.pacsize * 1000 / (codecParam.channels * codecParam.plfreq);
	attr->info.pt = codecParam.pltype;

	attr->setting.frm_per_pkt = 1;
	attr->setting.plc = 0;
	attr->setting.vad = 1;

	if (id->pt == PJMEDIA_RTP_PT_ILBC) {
		attr->setting.dec_fmtp.cnt = 1;
		attr->setting.dec_fmtp.param[0].name = STR_MODE;
		if((160 == codecParam.pacsize) ||
		        (320 == codecParam.pacsize)) {
			// processing block of 20ms
			attr->setting.dec_fmtp.param[0].val = pj_str((char *) "20");
		} else /*if((240 == codecParam.pacsize) ||
		        (480 == codecParam.pacsize))*/ {
			// processing block of 30ms
			attr->setting.dec_fmtp.param[0].val = pj_str((char *) "30");
		}

	}

	return PJ_SUCCESS;
}

static pj_status_t webrtc_enum_codecs(pjmedia_codec_factory *factory,
		unsigned *count, pjmedia_codec_info codecs[]) {
	unsigned max;
	unsigned i;
	int numCodecs = 1;

	PJ_UNUSED_ARG(factory);
	PJ_ASSERT_RETURN(codecs && *count > 0, PJ_EINVAL);

	max = *count;

	// TODO : we could use AudioCodingModule::RegisterReceiveCodec / UnRegister... in order to change PT
	AudioCodingModule *acmTmp = AudioCodingModule::Create(0);
	struct CodecInst sendCodecTmp;
	numCodecs = acmTmp->NumberOfCodecs();

	PJ_LOG(4, (THIS_FILE, "List of supported codec."));

	for (i = 0, *count = 0; i < numCodecs && *count < max; ++i) {

		acmTmp->Codec(i, sendCodecTmp);
		pj_str_t codec_name = pj_str((char*) sendCodecTmp.plname);

		// Exclude useless codecs
		if ((pj_stricmp2(&codec_name, "telephone-event") != 0)
				&& (pj_stricmp2(&codec_name, "cn") != 0)
				/* Exclude PCMU/PCMA cause already in pjsip and wertc force that to be built in*/
				&& (pj_stricmp2(&codec_name, "pcmu") != 0)
				&& (pj_stricmp2(&codec_name, "pcma") != 0)
		) {
			PJ_LOG(
					4,
					(THIS_FILE, "%d %s %d %d %d %d", i, sendCodecTmp.plname, sendCodecTmp.pltype, sendCodecTmp.plfreq, sendCodecTmp.pacsize, sendCodecTmp.rate));

			pj_bzero(&codecs[*count], sizeof(pjmedia_codec_info));
			pj_strdup2(webrtc_factory.pool, &codecs[*count].encoding_name,
					sendCodecTmp.plname);
			codecs[*count].pt = sendCodecTmp.pltype;
			codecs[*count].type = PJMEDIA_TYPE_AUDIO;
			codecs[*count].clock_rate = sendCodecTmp.plfreq;
			codecs[*count].channel_cnt = sendCodecTmp.channels;

			++*count;
		}
	}

	AudioCodingModule::Destroy(acmTmp);

	return PJ_SUCCESS;
}

static pj_status_t webrtc_alloc_codec(pjmedia_codec_factory *factory,
		const pjmedia_codec_info *id, pjmedia_codec **p_codec) {
	pjmedia_codec *codec = NULL;
	pj_status_t status;
	pj_pool_t *pool;

	PJ_ASSERT_RETURN(factory && id && p_codec, PJ_EINVAL);
	PJ_ASSERT_RETURN(factory==&webrtc_factory.base, PJ_EINVAL);

	/* Lock mutex. */
	pj_mutex_lock(webrtc_factory.mutex);

	/* Allocate new codec if no more is available */
	struct webrtc_private *codec_priv;

	/* Create pool for codec instance */
	pool = pjmedia_endpt_create_pool(webrtc_factory.endpt, "webrtc_codec", 512,
			512);

	codec = PJ_POOL_ALLOC_T(pool, pjmedia_codec);
	codec_priv = PJ_POOL_ZALLOC_T(pool, struct webrtc_private);
	if (!codec || !codec_priv) {
		pj_pool_release(pool);
		pj_mutex_unlock(webrtc_factory.mutex);
		return PJ_ENOMEM;
	}

	codec_priv->pool = pool;
	/* Set the payload type */
	codec_priv->pt = id->pt;
	codec_priv->coder = new AudioCoder(1);

	codec->factory = factory;
	codec->op = &webrtc_op;
	codec->codec_data = codec_priv;

	*p_codec = codec;

	/* Unlock mutex. */
	pj_mutex_unlock(webrtc_factory.mutex);

	return PJ_SUCCESS;
}

static pj_status_t webrtc_dealloc_codec(pjmedia_codec_factory *factory,
		pjmedia_codec *codec) {
	struct webrtc_private *priv = (struct webrtc_private*) codec->codec_data;
	int i = 0;

	PJ_ASSERT_RETURN(factory && codec, PJ_EINVAL);
	PJ_ASSERT_RETURN(factory==&webrtc_factory.base, PJ_EINVAL);

	/* Close codec, if it's not closed. */
	webrtc_close(codec);
	delete priv->coder;

	pj_pool_release(priv->pool);

	return PJ_SUCCESS;
}

static pj_status_t webrtc_init(pjmedia_codec *codec, pj_pool_t *pool) {
	PJ_UNUSED_ARG(pool);
	PJ_UNUSED_ARG(codec);
	//struct webrtc_private *priv = (struct webrtc_private*) codec->codec_data;

	return PJ_SUCCESS;
}

static pj_status_t webrtc_open(pjmedia_codec *codec,
		pjmedia_codec_param *attr) {
	struct webrtc_private *priv = (struct webrtc_private*) codec->codec_data;
	pj_pool_t *pool;
	CodecInst coderParam, decoderParam;
	unsigned i;
	pj_status_t status;
    pj_uint16_t dec_fmtp_mode = 30, enc_fmtp_mode = 30;

	priv->pt = attr->info.pt;
	priv->clock_rate = attr->info.clock_rate;
	priv->ptime = attr->info.frm_ptime;
	priv->channels = attr->info.channel_cnt;

	priv->pacbsize = (priv->ptime * priv->channels * priv->clock_rate / 1000) << 1;

	pool = priv->pool;

	status = find_codec(attr->info.pt, attr->info.clock_rate,
			attr->info.channel_cnt, &coderParam);
	status = find_codec(attr->info.pt, attr->info.clock_rate,
			attr->info.channel_cnt, &decoderParam);

	if (status != PJ_SUCCESS) {
		return status;
	}

	if (attr->info.pt == PJMEDIA_RTP_PT_ILBC) {
		/* Get decoder mode */
		for (i = 0; i < attr->setting.dec_fmtp.cnt; ++i) {
			if (pj_stricmp(&attr->setting.dec_fmtp.param[i].name, &STR_MODE)
					== 0) {
				dec_fmtp_mode = (pj_uint16_t) pj_strtoul(
						&attr->setting.dec_fmtp.param[i].val);
				break;
			}
		}
		decoderParam.pacsize = dec_fmtp_mode * coderParam.plfreq / 1000;
		decoderParam.rate = (dec_fmtp_mode == 20)? 15200 : 13300;

		/* Get encoder mode */
		for (i = 0; i < attr->setting.enc_fmtp.cnt; ++i) {
			if (pj_stricmp(&attr->setting.enc_fmtp.param[i].name, &STR_MODE)
					== 0) {
				enc_fmtp_mode = (pj_uint16_t) pj_strtoul(
						&attr->setting.enc_fmtp.param[i].val);
				break;
			}
		}
		coderParam.pacsize = enc_fmtp_mode * coderParam.plfreq / 1000;
	}


	priv->coder->SetEncodeCodec(coderParam);
	priv->coder->SetDecodeCodec(decoderParam);

	return PJ_SUCCESS;
}

static pj_status_t webrtc_close(pjmedia_codec *codec) {
	//PJ_UNUSED_ARG(codec);
	struct webrtc_private *priv = (struct webrtc_private*) codec->codec_data;

	return PJ_SUCCESS;
}

static pj_status_t webrtc_modify(pjmedia_codec *codec,
		const pjmedia_codec_param *attr) {
	struct webrtc_private *priv = (struct webrtc_private*) codec->codec_data;

	if (attr->info.pt != priv->pt)
		return PJMEDIA_EINVALIDPT;

	return PJ_SUCCESS;
}

static pj_status_t webrtc_parse(pjmedia_codec *codec, void *pkt,
		pj_size_t pkt_size, const pj_timestamp *ts, unsigned *frame_cnt,
		pjmedia_frame frames[]) {
	unsigned count = 0;
	struct webrtc_private *priv = (struct webrtc_private*) codec->codec_data;

	PJ_ASSERT_RETURN(frame_cnt, PJ_EINVAL);

	count = 0;

	int dec_frame_size = pkt_size;
	int samples_per_frame = priv->clock_rate / 100; //10ms @ Frequency

	while (pkt_size >= dec_frame_size && count < *frame_cnt) {
		frames[count].type = PJMEDIA_FRAME_TYPE_AUDIO;
		frames[count].buf = pkt;
		frames[count].size = dec_frame_size;
		frames[count].timestamp.u64 = ts->u64 + count * samples_per_frame; // fHz * ptime / 1000

		pkt = ((char*) pkt) + dec_frame_size;
		pkt_size -= dec_frame_size;

		++count;
	}

	//PJ_LOG(4, (THIS_FILE, "Found to decode %d %d %d", count, pkt_size, dec_frame_size));
	*frame_cnt = count;
	return PJ_SUCCESS;
}

static pj_status_t webrtc_encode(pjmedia_codec *codec,
		const struct pjmedia_frame *input, unsigned output_buf_len,
		struct pjmedia_frame *output) {
	struct webrtc_private *priv = (struct webrtc_private*) codec->codec_data;
	pj_int16_t *pcm_in;
	unsigned nsamples, enc_samples_per_frame, encoded_size, offset;
	int nb;
	AudioFrame audioFrame;

	pj_assert(priv && input && output);

	nsamples = input->size >> 1;
	enc_samples_per_frame = priv->clock_rate / 100; //10ms @ Frequency

	output->size = 0;
	offset = 0;
	while (nsamples >= enc_samples_per_frame) {
		pjmedia_copy_samples((pj_int16_t*) audioFrame.data_, ((pj_int16_t*) input->buf) + offset, enc_samples_per_frame);
		audioFrame.num_channels_ = 1;
		audioFrame.sample_rate_hz_ = priv->clock_rate;
		audioFrame.samples_per_channel_ = enc_samples_per_frame;

		unsigned state = priv->coder->Encode(audioFrame, (WebRtc_Word8*) output->buf + output->size,
				encoded_size);

		output->size += encoded_size;
		nsamples -= enc_samples_per_frame;
		offset += enc_samples_per_frame;
		//PJ_LOG(4, (THIS_FILE, "10ms chunk %d as %d size", enc_samples_per_frame, encoded_size));
	}

	output->type = PJMEDIA_FRAME_TYPE_AUDIO;
	output->timestamp = input->timestamp;
	//PJ_LOG(4, (THIS_FILE, "Encoded From %d to %d", input->size, output->size));

	return PJ_SUCCESS;
}

static pj_status_t webrtc_decode(pjmedia_codec *codec,
		const struct pjmedia_frame *input, unsigned output_buf_len,
		struct pjmedia_frame *output) {
	struct webrtc_private *priv = (struct webrtc_private*) codec->codec_data;
	unsigned state, offset;
	pj_assert(priv != NULL);
	PJ_ASSERT_RETURN(input && output, PJ_EINVAL);
	PJ_ASSERT_RETURN(output_buf_len >= priv->pacbsize, PJMEDIA_CODEC_EPCMTOOSHORT);

	output->size = 0;
	offset = 0;
	//PJ_LOG(4, (THIS_FILE, "Will decode %d", priv->pacbsize));
	while(output->size < priv->pacbsize){
		AudioFrame decodedFrame;
		if(output->size == 0){
			//Put datas first and get 10ms
			state = priv->coder->Decode(decodedFrame, priv->clock_rate, (WebRtc_Word8*)input->buf, input->size);
		}else{
			// Get next 10ms
			state = priv->coder->Decode(decodedFrame, priv->clock_rate, NULL, 0);
		}
		if(state == -1){
			PJ_LOG(1, (THIS_FILE, "Error with frame @%d for input size %d", offset, input->size));
			return PJ_EINVAL;
		}

		pjmedia_copy_samples(((pj_int16_t*) output->buf) + offset, (pj_int16_t*) decodedFrame.data_, decodedFrame.samples_per_channel_);
		output->size += decodedFrame.samples_per_channel_ << 1;
		offset += decodedFrame.samples_per_channel_;

		//PJ_LOG(4, (THIS_FILE, "Decoded 10ms %d to %d", offset, decodedFrame.samples_per_channel_));
	}
	output->type = PJMEDIA_FRAME_TYPE_AUDIO;
	output->timestamp = input->timestamp;
	//PJ_LOG(4, (THIS_FILE, "Decoded From %d to %d", input->size, output->size));


	return PJ_SUCCESS;
}

static pj_status_t webrtc_recover(pjmedia_codec *codec, unsigned output_buf_len,
		struct pjmedia_frame *output) {
	struct webrtc_private *priv = (struct webrtc_private*) codec->codec_data;
	unsigned state, offset;
	PJ_ASSERT_RETURN(output_buf_len >= priv->pacbsize, PJMEDIA_CODEC_EPCMTOOSHORT);



	output->size = 0;
	//PJ_LOG(4, (THIS_FILE, "Will try to recover %d", priv->pacbsize));
	while(output->size < priv->pacbsize){
		AudioFrame decodedFrame;
		state = priv->coder->Decode(decodedFrame, priv->clock_rate, NULL, 0);

		if(state == -1){
			PJ_LOG(1, (THIS_FILE, "Error with frame"));
			return PJ_EINVAL;
		}

		pjmedia_copy_samples((pj_int16_t*) output->buf + offset, (pj_int16_t*) decodedFrame.data_, decodedFrame.samples_per_channel_);
		output->size += decodedFrame.samples_per_channel_ << 1;
		offset += decodedFrame.samples_per_channel_;
	}
	output->type = PJMEDIA_FRAME_TYPE_AUDIO;

	return PJ_SUCCESS;
}

#endif
