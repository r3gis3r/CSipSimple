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

//This file is a port for android devices
// It's deeply inspired from port audio

#include "opensl_dev.h"

//TODO : should be done in config there
#define PJMEDIA_AUDIO_DEV_HAS_OPENSL 1

#if PJMEDIA_AUDIO_DEV_HAS_OPENSL

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <SLES/OpenSLES_AndroidConfiguration.h>
#include <sys/system_properties.h>

#include "pjmedia/errno.h"
#include "audio_dev_wrap.h"

#define THIS_FILE	"opensl_dev.cpp"
#define DRIVER_NAME	"OPENSL"

#define PREFILL_BUFFERS 2


struct opensl_aud_factory
{
	pjmedia_aud_dev_factory base;
	pj_pool_factory *pf;
	pj_pool_t *pool;

	SLObjectItf engineObject;
	SLEngineItf engineEngine;
	SLObjectItf outputMixObject;
};

/*
 * Sound stream descriptor.
 * This struct may be used for both unidirectional or bidirectional sound
 * streams.
 */
struct opensl_aud_stream
{
	pjmedia_aud_stream base;

	pj_pool_t *pool;
	pj_str_t name;
	pjmedia_dir dir;
	pjmedia_aud_param param;

	void *user_data;

	pj_bool_t quit_flag;

	pjmedia_aud_rec_cb rec_cb;
	pjmedia_aud_play_cb play_cb;


	//Player
	SLObjectItf playerObject;
	SLPlayItf playerPlay;
	SLAndroidSimpleBufferQueueItf playerBufferQueue;
    unsigned playerBufferSize;
    char * playerBuffer;

    //Recorder
    SLObjectItf recorderObject;
    SLRecordItf recorderRecord;
    SLAndroidSimpleBufferQueueItf recorderBufferQueue;
    unsigned recorderBufferSize;
    char * recorderBuffer;

};

/* Factory prototypes */
static pj_status_t opensl_init(pjmedia_aud_dev_factory *f);
static pj_status_t opensl_destroy(pjmedia_aud_dev_factory *f);
static pj_status_t opensl_refresh(pjmedia_aud_dev_factory *f);
static unsigned opensl_get_dev_count(pjmedia_aud_dev_factory *f);
static pj_status_t opensl_get_dev_info(pjmedia_aud_dev_factory *f,
		unsigned index,
		pjmedia_aud_dev_info *info);
static pj_status_t opensl_default_param(pjmedia_aud_dev_factory *f,
		unsigned index,
		pjmedia_aud_param *param);
static pj_status_t opensl_create_stream(pjmedia_aud_dev_factory *f,
		const pjmedia_aud_param *param,
		pjmedia_aud_rec_cb rec_cb,
		pjmedia_aud_play_cb play_cb,
		void *user_data,
		pjmedia_aud_stream **p_aud_strm);

/* Stream prototypes */
static pj_status_t strm_get_param(pjmedia_aud_stream *strm,
		pjmedia_aud_param *param);
static pj_status_t strm_get_cap(pjmedia_aud_stream *strm,
		pjmedia_aud_dev_cap cap,
		void *value);
static pj_status_t strm_set_cap(pjmedia_aud_stream *strm,
		pjmedia_aud_dev_cap cap,
		const void *value);
static pj_status_t strm_start(pjmedia_aud_stream *strm);
static pj_status_t strm_stop(pjmedia_aud_stream *strm);
static pj_status_t strm_destroy(pjmedia_aud_stream *strm);

static pjmedia_aud_dev_factory_op opensl_op =
{
	&opensl_init,
	&opensl_destroy,
	&opensl_get_dev_count,
	&opensl_get_dev_info,
	&opensl_default_param,
	&opensl_create_stream,
    &opensl_refresh
};

static pjmedia_aud_stream_op opensl_strm_op =
{
	&strm_get_param,
	&strm_get_cap,
	&strm_set_cap,
	&strm_start,
	&strm_stop,
	&strm_destroy
};



// this callback handler is called every time a buffer finishes playing
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {
    assert(NULL != context);
	SLresult result;
    int status;
    pj_timestamp tstamp;
	struct opensl_aud_stream *stream = (struct opensl_aud_stream*) context;

	if(stream->quit_flag == 0){
		pj_bzero (stream->playerBuffer, stream->playerBufferSize);
		tstamp.u64 = 0;

		// enqueue another buffer ( we choose only one frame )

		// clean buffer
		pj_bzero (stream->playerBuffer, stream->playerBufferSize);
		pjmedia_frame frame;

		frame.type = PJMEDIA_FRAME_TYPE_AUDIO;
		frame.buf = stream->playerBuffer;
		frame.size = stream->playerBufferSize;
		frame.timestamp.u64 = tstamp.u64;
		frame.bit_info = 0;

		status = (*stream->play_cb)(stream->user_data, &frame);
		result = (*bq)->Enqueue(bq, frame.buf, frame.size);

		// the most likely other result is SL_RESULT_BUFFER_INSUFFICIENT,
		// which for this code example would indicate a programming error
		if(SL_RESULT_SUCCESS != result){
			PJ_LOG(1, (THIS_FILE, "We could not enqueue next player buffer !!! %d", result));
		}
	}
}


void bqRecorderCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {
	assert(NULL != context);
	SLresult result;
	int status;
	struct opensl_aud_stream *stream = (struct opensl_aud_stream*) context;

	if(stream->quit_flag == 0){
		// previous buffer has been filled by the recorder

		pjmedia_frame frame;

		frame.type = PJMEDIA_FRAME_TYPE_AUDIO;
		frame.buf = stream->recorderBuffer;
		frame.size = stream->recorderBufferSize;
		frame.timestamp.u64 = 0;
		frame.bit_info = 0;

		status = (*stream->rec_cb)(stream->user_data, &frame);

		//We have treated this buffer, clear it

		pj_bzero (stream->recorderBuffer, stream->recorderBufferSize);
		//And now enqueue next buffer

		result = (*bq)->Enqueue(bq, stream->recorderBuffer, stream->recorderBufferSize);

		// the most likely other result is SL_RESULT_BUFFER_INSUFFICIENT,
		// which for this code example would indicate a programming error
		if(SL_RESULT_SUCCESS != result){
			PJ_LOG(1, (THIS_FILE, "We could not enqueue next record buffer !!! %d", result));
		}
	}
}

pj_status_t opensl_to_pj_error(SLresult code) {
	switch(code){
	case SL_RESULT_SUCCESS:
		return PJ_SUCCESS;
	case SL_RESULT_PRECONDITIONS_VIOLATED:
	case SL_RESULT_PARAMETER_INVALID:
	case SL_RESULT_CONTENT_CORRUPTED:
	case SL_RESULT_FEATURE_UNSUPPORTED:
		return PJ_EINVALIDOP;
	case SL_RESULT_MEMORY_FAILURE:
	case SL_RESULT_BUFFER_INSUFFICIENT:
		return PJ_ENOMEM;
	case SL_RESULT_RESOURCE_ERROR:
	case SL_RESULT_RESOURCE_LOST:
	case SL_RESULT_CONTROL_LOST:
		return PJ_EBUSY;
	case SL_RESULT_CONTENT_UNSUPPORTED:
		return PJ_ENOTSUP;
	default:
		return PJMEDIA_ERROR;
	}
}

/*
 * Init Android audio driver.
 */
pjmedia_aud_dev_factory* pjmedia_opensl_factory(pj_pool_factory *pf) {
	struct opensl_aud_factory *f;
	pj_pool_t *pool;

	pool = pj_pool_create(pf, "opensles", 64, 64, NULL);
	f = PJ_POOL_ZALLOC_T(pool, struct opensl_aud_factory);
	f->pf = pf;
	f->pool = pool;
	f->base.op = &opensl_op;

	return &f->base;
}

/* API: Init factory */
static pj_status_t opensl_init(pjmedia_aud_dev_factory *f) {
	struct opensl_aud_factory *pa = (struct opensl_aud_factory*)f;

	SLresult result;

	/* Create OpenSL ES engine in thread-safe mode */
	SLEngineOption EngineOption[] = {(SLuint32)	SL_ENGINEOPTION_THREADSAFE, (SLuint32) SL_BOOLEAN_TRUE};

	//For future use
	const SLInterfaceID ids[2] = {SL_IID_AUDIODECODERCAPABILITIES, SL_IID_AUDIOENCODERCAPABILITIES};
	const SLboolean req[2] = { SL_BOOLEAN_FALSE, SL_BOOLEAN_FALSE};


    // create engine
    result = slCreateEngine(&pa->engineObject, 1, EngineOption, 2, ids, req);
    if(result != SL_RESULT_SUCCESS){
		PJ_LOG(1, (THIS_FILE, "Can't create engine %d ", result));
		return opensl_to_pj_error(result);
	}

    // realize the engine
    result = (*pa->engineObject)->Realize(pa->engineObject, SL_BOOLEAN_FALSE);
    if(result != SL_RESULT_SUCCESS){
		PJ_LOG(1, (THIS_FILE, "Can't realize engine"));
		return opensl_to_pj_error(result);
	}

    // get the engine interface, which is needed in order to create other objects
    result = (*pa->engineObject)->GetInterface(pa->engineObject, SL_IID_ENGINE, &pa->engineEngine);
    if(result != SL_RESULT_SUCCESS){
		PJ_LOG(1, (THIS_FILE, "Can't get engine iface"));
		// Destroy engine since created
		(*pa->engineObject)->Destroy(pa->engineObject);
		return opensl_to_pj_error(result);
	}

//FOR future use
//    //Get the audio cap interface
//	result = (*pa->engineObject)->GetInterface(pa->engineObject, SL_IID_AUDIODECODERCAPABILITIES, &pa->decCaps);
//	if(result != SL_RESULT_SUCCESS){
//		PJ_LOG(1, (THIS_FILE, "Can't get decoder caps iface - %d", result));
//	}


    result = (*pa->engineEngine)->CreateOutputMix(pa->engineEngine, &pa->outputMixObject, 0, NULL, NULL);
    if(result != SL_RESULT_SUCCESS){
		PJ_LOG(1, (THIS_FILE, "Can't create output mix"));
		// Destroy engine since created
		(*pa->engineObject)->Destroy(pa->engineObject);
		return opensl_to_pj_error(result);
	}

    result = (*pa->outputMixObject)->Realize(pa->outputMixObject, SL_BOOLEAN_FALSE);
    if(result != SL_RESULT_SUCCESS){
		PJ_LOG(1, (THIS_FILE, "Can't realize output mix"));
		// Destroy engine since created
		(*pa->engineObject)->Destroy(pa->engineObject);
		return opensl_to_pj_error(result);
	}


	PJ_LOG(4,(THIS_FILE, "Opensl sound library initialized"));
	return PJ_SUCCESS;
}

/* API: Destroy factory */
static pj_status_t opensl_destroy(pjmedia_aud_dev_factory *f) {
	struct opensl_aud_factory *pa = (struct opensl_aud_factory*)f;
	pj_pool_t *pool;
	int err;

	PJ_LOG(4,(THIS_FILE, "Opensl sound library shutting down.."));


	/* Destroy Output Mix object */
	if(pa->outputMixObject){
		(*pa->outputMixObject)->Destroy(pa->outputMixObject);
	}

	if(pa->engineObject){
		(*pa->engineObject)->Destroy(pa->engineObject);
	}

	pool = pa->pool;
	pa->pool = NULL;
	pj_pool_release(pool);

	return PJ_SUCCESS;
}


/* API: refresh the list of devices */
static pj_status_t opensl_refresh(pjmedia_aud_dev_factory *f)
{
    PJ_UNUSED_ARG(f);
    return PJ_SUCCESS;
}

/* API: Get device count. */
static unsigned opensl_get_dev_count(pjmedia_aud_dev_factory *f) {
	PJ_LOG(4,(THIS_FILE, "Get dev count"));
	int count = 1;
	PJ_UNUSED_ARG(f);
	return count < 0 ? 0 : count;
}

/* API: Get device info. */
static pj_status_t opensl_get_dev_info(pjmedia_aud_dev_factory *f,
		unsigned index,
		pjmedia_aud_dev_info *info) {

	struct opensl_aud_factory *pa = (struct opensl_aud_factory*)f;
	SLresult result;


	PJ_LOG(4,(THIS_FILE, "Get dev info"));

	pj_bzero(info, sizeof(*info));

	pj_ansi_strcpy(info->name, "OpenSL Audio");
	info->default_samples_per_sec = 8000;
	info->caps = PJMEDIA_AUD_DEV_CAP_INPUT_VOLUME_SETTING |
				 PJMEDIA_AUD_DEV_CAP_OUTPUT_VOLUME_SETTING;
	info->input_count = 1;
	info->output_count = 1;

	return PJ_SUCCESS;
}

/* API: fill in with default parameter. */
static pj_status_t opensl_default_param(pjmedia_aud_dev_factory *f,
		unsigned index,
		pjmedia_aud_param *param)
{

	PJ_LOG(4,(THIS_FILE, "Default params"));
	pjmedia_aud_dev_info adi;
	pj_status_t status;

	PJ_UNUSED_ARG(f);

	status = opensl_get_dev_info(f, index, &adi);
	if (status != PJ_SUCCESS)
	return status;

	pj_bzero(param, sizeof(*param));
	if (adi.input_count && adi.output_count) {
		param->dir = PJMEDIA_DIR_CAPTURE_PLAYBACK;
		param->rec_id = index;
		param->play_id = index;
	} else if (adi.input_count) {
		param->dir = PJMEDIA_DIR_CAPTURE;
		param->rec_id = index;
		param->play_id = PJMEDIA_AUD_INVALID_DEV;
	} else if (adi.output_count) {
		param->dir = PJMEDIA_DIR_PLAYBACK;
		param->play_id = index;
		param->rec_id = PJMEDIA_AUD_INVALID_DEV;
	} else {
		return PJMEDIA_EAUD_INVDEV;
	}

	param->clock_rate = adi.default_samples_per_sec;
	param->channel_count = 1;
	param->samples_per_frame = adi.default_samples_per_sec * 20 / 1000;
	param->bits_per_sample = 16;
	param->flags = adi.caps;
	param->input_latency_ms = PJMEDIA_SND_DEFAULT_REC_LATENCY;
	param->output_latency_ms = PJMEDIA_SND_DEFAULT_PLAY_LATENCY;

	return PJ_SUCCESS;
}

/* API: create stream */
static pj_status_t opensl_create_stream(pjmedia_aud_dev_factory *f,
		const pjmedia_aud_param *param,
		pjmedia_aud_rec_cb rec_cb,
		pjmedia_aud_play_cb play_cb,
		void *user_data,
		pjmedia_aud_stream **p_aud_strm)
{

	PJ_LOG(4,(THIS_FILE, "Creating stream"));
	struct opensl_aud_factory *pa = (struct opensl_aud_factory*)f;
	pj_pool_t *pool;
	struct opensl_aud_stream *stream;
	pj_status_t status = PJ_SUCCESS;
	int has_set_in_call = 0;
	SLresult result;
	SLDataFormat_PCM format_pcm;

	// Only supports for mono channel for now
	PJ_ASSERT_RETURN(param->channel_count == 1, PJ_EINVAL);
	PJ_ASSERT_RETURN(play_cb && rec_cb && p_aud_strm, PJ_EINVAL);


	pool = pj_pool_create(pa->pf, "sndstream", 1024, 1024, NULL);
	if (!pool) {
		return PJ_ENOMEM;
	}

	stream = PJ_POOL_ZALLOC_T(pool, struct opensl_aud_stream);
	stream->pool = pool;
	pj_strdup2_with_null(pool, &stream->name, "Android stream");

	stream->dir = PJMEDIA_DIR_CAPTURE_PLAYBACK;
	stream->param = *param;
	stream->user_data = user_data;
	stream->rec_cb = rec_cb;
	stream->play_cb = play_cb;

	int bufferSize =  param->samples_per_frame * param->channel_count * param->bits_per_sample / 8;

    /* Set our buffers */
    stream->playerBufferSize =  bufferSize;
    stream->playerBuffer = (char*) pj_pool_alloc(stream->pool, stream->playerBufferSize);
    stream->recorderBufferSize = bufferSize;
    stream->recorderBuffer = (char*) pj_pool_alloc(stream->pool, stream->recorderBufferSize);


	PJ_LOG(3, (THIS_FILE, "Create stream : %d samples/sec, %d samples/frame, %d channels",
			param->clock_rate,
			param->samples_per_frame,
			param->channel_count));


	status = on_setup_audio_wrapper(param->clock_rate);
	if(status != PJ_SUCCESS){
		return PJMEDIA_EAUD_INVOP;
	}
	has_set_in_call = 1;


	// Init OPENSL layer
	{

		// configure Audio PCM format
		SLDataLocator_AndroidSimpleBufferQueue loc_bq = { SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, PREFILL_BUFFERS };

		format_pcm.formatType = SL_DATAFORMAT_PCM;
		format_pcm.numChannels = param->channel_count;
		// Here samples per sec should be supported else we will get an error
		format_pcm.samplesPerSec  = (SLuint32) param->clock_rate * 1000;
		format_pcm.bitsPerSample = (SLuint16) param->bits_per_sample;
		format_pcm.containerSize = (SLuint16) param->bits_per_sample;
		format_pcm.channelMask = SL_SPEAKER_FRONT_CENTER; // As far as we are mono only
		format_pcm.endianness = SL_BYTEORDER_LITTLEENDIAN;


		// configure audio sink
		SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, pa->outputMixObject};


		if (stream->dir & PJMEDIA_DIR_PLAYBACK) {

			const SLInterfaceID ids[2] = {SL_IID_BUFFERQUEUE, SL_IID_ANDROIDCONFIGURATION};
			const SLboolean req[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
			SLDataSource audioSrc = {&loc_bq, &format_pcm};
			SLDataSink audioSnk = {&loc_outmix, NULL};

			// create audio player
			result = (*pa->engineEngine)->CreateAudioPlayer(pa->engineEngine, &stream->playerObject, &audioSrc, &audioSnk, 2, ids, req);
			if(result != SL_RESULT_SUCCESS){
				PJ_LOG(1, (THIS_FILE, "Can't create player: %d", result));
				goto on_error;
			}

			SLAndroidConfigurationItf playerConfig;
			result = (*stream->playerObject)->GetInterface(stream->playerObject, SL_IID_ANDROIDCONFIGURATION, &playerConfig);
			if(result != SL_RESULT_SUCCESS){
				PJ_LOG(2, (THIS_FILE, "Can't get android configuration iface"));
				goto on_error;
			}
			SLint32 streamType = SL_ANDROID_STREAM_VOICE;
			result = (*playerConfig)->SetConfiguration(playerConfig, SL_ANDROID_KEY_STREAM_TYPE, &streamType, sizeof(SLint32));

			// realize the player
			result = (*stream->playerObject)->Realize(stream->playerObject, SL_BOOLEAN_FALSE);
			if(result != SL_RESULT_SUCCESS){
				PJ_LOG(1, (THIS_FILE, "Can't realize player : %d", result));
				goto on_error;
			}


			// get the play interface
			result = (*stream->playerObject)->GetInterface(stream->playerObject, SL_IID_PLAY, &stream->playerPlay);
			if(result != SL_RESULT_SUCCESS){
				PJ_LOG(1, (THIS_FILE, "Can't get play iface"));
				goto on_error;
			}

			// get the buffer queue interface
			result = (*stream->playerObject)->GetInterface(stream->playerObject, SL_IID_BUFFERQUEUE,
					&stream->playerBufferQueue);
			if(result != SL_RESULT_SUCCESS){
				PJ_LOG(1, (THIS_FILE, "Can't get buffer queue iface"));
				goto on_error;
			}

			// register callback on the buffer queue
			result = (*stream->playerBufferQueue)->RegisterCallback(stream->playerBufferQueue, bqPlayerCallback, (void *) stream);
			assert(SL_RESULT_SUCCESS == result);

		}




		if (stream->dir & PJMEDIA_DIR_CAPTURE) {

		    SLDataLocator_IODevice loc_dev = {SL_DATALOCATOR_IODEVICE, SL_IODEVICE_AUDIOINPUT,
		            SL_DEFAULTDEVICEID_AUDIOINPUT, NULL};
		    SLDataSource audioSrc = {&loc_dev, NULL};
		    SLDataSink audioSnk = {&loc_bq, &format_pcm};


		    const SLInterfaceID ids[2] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE, SL_IID_ANDROIDCONFIGURATION};
		    const SLboolean req[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};

			result = (*pa->engineEngine)->CreateAudioRecorder(pa->engineEngine, &stream->recorderObject, &audioSrc, &audioSnk, 2, ids, req);
			if(result != SL_RESULT_SUCCESS){
				PJ_LOG(1, (THIS_FILE, "Can't create recorder: %d", result));
				goto on_error;
			}


			SLAndroidConfigurationItf recorderConfig;
			result = (*stream->recorderObject)->GetInterface(stream->recorderObject, SL_IID_ANDROIDCONFIGURATION, &recorderConfig);
			if(result != SL_RESULT_SUCCESS){
				PJ_LOG(2, (THIS_FILE, "Can't get recorder config iface"));
				goto on_error;
			}

			SLint32 streamType = SL_ANDROID_RECORDING_PRESET_GENERIC;
			char sdk_version[PROP_VALUE_MAX];
			__system_property_get("ro.build.version.sdk", sdk_version);

			pj_str_t pj_sdk_version = pj_str(sdk_version);
			int sdk_v = pj_strtoul(&pj_sdk_version);
			if(sdk_v >= 10){
				streamType = 0x7;
			}

			PJ_LOG(3, (THIS_FILE, "We have a stream type %d Cause SDK : %d", streamType, sdk_v));
			result = (*recorderConfig)->SetConfiguration(recorderConfig, SL_ANDROID_KEY_RECORDING_PRESET, &streamType, sizeof(SLint32));



			// realize the recorder
			result = (*stream->recorderObject)->Realize(stream->recorderObject, SL_BOOLEAN_FALSE);
			if(result != SL_RESULT_SUCCESS){
				PJ_LOG(1, (THIS_FILE, "Can't realize recorder : %d", result));
				goto on_error;
			}


			// get the record interface
			result = (*stream->recorderObject)->GetInterface(stream->recorderObject, SL_IID_RECORD, &stream->recorderRecord);
			if(result != SL_RESULT_SUCCESS){
				PJ_LOG(1, (THIS_FILE, "Can't get record iface"));
				goto on_error;
			}

			// get the buffer queue interface
			result = (*stream->recorderObject)->GetInterface(stream->recorderObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &stream->recorderBufferQueue);
			if(result != SL_RESULT_SUCCESS){
				PJ_LOG(1, (THIS_FILE, "Can't get recorder buffer queue iface"));
				goto on_error;
			}




			// register callback on the buffer queue
			result = (*stream->recorderBufferQueue)->RegisterCallback(stream->recorderBufferQueue, bqRecorderCallback, (void *) stream);
			assert(SL_RESULT_SUCCESS == result);
		}



	}


	//OK, done
	*p_aud_strm = &stream->base;
	(*p_aud_strm)->op = &opensl_strm_op;
	return PJ_SUCCESS;

on_error:
	if(has_set_in_call == 1){
		on_teardown_audio_wrapper();
	}
	pj_pool_release(pool);
	return PJ_ENOMEM;
}

/* API: Get stream parameters */
static pj_status_t strm_get_param(pjmedia_aud_stream *s,
		pjmedia_aud_param *pi)
{

	PJ_LOG(4,(THIS_FILE, "Get stream params"));
	struct opensl_aud_stream *strm = (struct opensl_aud_stream*)s;
	PJ_ASSERT_RETURN(strm && pi, PJ_EINVAL);
	pj_memcpy(pi, &strm->param, sizeof(*pi));

	return PJ_SUCCESS;
}

/* API: get capability */
static pj_status_t strm_get_cap(pjmedia_aud_stream *s,
		pjmedia_aud_dev_cap cap,
		void *pval)
{
	PJ_LOG(4,(THIS_FILE, "Get stream caps"));
	struct opensl_aud_stream *strm = (struct opensl_aud_stream*)s;

	pj_status_t status = PJ_ENOTSUP;

	PJ_ASSERT_RETURN(s && pval, PJ_EINVAL);

	switch (cap) {
		case PJMEDIA_AUD_DEV_CAP_INPUT_VOLUME_SETTING:
			status = PJ_SUCCESS;
			break;
		case PJMEDIA_AUD_DEV_CAP_OUTPUT_VOLUME_SETTING:
			status = PJ_SUCCESS;
			break;
		default:
		break;
	}

	return status;
}

/* API: set capability */
static pj_status_t strm_set_cap(pjmedia_aud_stream *strm,
		pjmedia_aud_dev_cap cap,
		const void *value)
{
	PJ_UNUSED_ARG(strm);
	PJ_UNUSED_ARG(cap);
	PJ_UNUSED_ARG(value);
	PJ_LOG(4,(THIS_FILE, "Set stream cap"));
	/* Nothing is supported in fact */
	return PJMEDIA_EAUD_INVCAP;
}

/* API: start stream. */
static pj_status_t strm_start(pjmedia_aud_stream *s)
{
	struct opensl_aud_stream *stream = (struct opensl_aud_stream*)s;
	int i = 0;
	SLresult result;

	PJ_LOG(4,(THIS_FILE, "Starting %s stream..", stream->name.ptr));
	stream->quit_flag = 0;


	//Set media in call
	/*
	 *
	JNIEnv *jni_env = 0;
	ATTACH_JVM(jni_env);
	{

		jmethodID setincall_method = jni_env->GetStaticMethodID(stream->ua_class, "setAudioInCall", "()V");
		if(setincall_method == 0){
			return PJ_ENOMEM;
		}
		jni_env->CallStaticVoidMethod(stream->ua_class, setincall_method);
	}
	*/


	pj_status_t status;

	if(stream->recorderBufferQueue && stream->recorderRecord) {
		//Set the recorder buffer in the queue
		result = (*stream->recorderBufferQueue)->Enqueue(stream->recorderBufferQueue, stream->recorderBuffer, stream->recorderBufferSize);

		result = (*stream->recorderRecord)->SetRecordState(stream->recorderRecord, SL_RECORDSTATE_RECORDING);
		if(SL_RESULT_SUCCESS != result){
			//TODO : be more precise here...
			status = PJ_EINVAL;
			goto on_error;
		}
	}

	if(stream->playerPlay && stream->playerBufferQueue) {

		//Start to fill the buffer with some stuff to get callbacks each time buffer is read
		for( i=0; i < PREFILL_BUFFERS; i++){
			bqPlayerCallback(stream->playerBufferQueue, (void *)stream);
		}

		// set the player's state to playing
		result = (*stream->playerPlay)->SetPlayState(stream->playerPlay, SL_PLAYSTATE_PLAYING);
		if(SL_RESULT_SUCCESS != result){
			//TODO : be more precise here...
			status = PJ_EINVAL;
			goto on_error;
		}
	}


	PJ_LOG(4,(THIS_FILE, "Starting done"));
	return PJ_SUCCESS;

on_error:
	//DETACH_JVM(jni_env);
	if(status != PJ_SUCCESS){
		strm_destroy(&stream->base);
	}
	return status;
}

/* API: stop stream. */
static pj_status_t strm_stop(pjmedia_aud_stream *s)
{
	struct opensl_aud_stream *stream = (struct opensl_aud_stream*)s;
	int i;
	SLresult result;
	SLAndroidSimpleBufferQueueState state;

	//We assume that all jni calls are safe ... that's acceptable
	if(stream->quit_flag == 0){
		PJ_LOG(3, (THIS_FILE, "Stopping stream"));
	}else{
		PJ_LOG(2, (THIS_FILE, "Already stopped.... nothing to do here"));
		return PJ_SUCCESS;
	}

	stream->quit_flag = 1;



	if(stream->recorderBufferQueue && stream->recorderRecord) {
		(*stream->recorderRecord)->SetRecordState(stream->recorderRecord, SL_RECORDSTATE_STOPPED);
	}


	if(stream->playerBufferQueue && stream->playerPlay) {
		/* Wait until the PCM data is done playing, the buffer queue callback
		will continue to queue buffers until the entire PCM data has been
		played. This is indicated by waiting for the count member of the
		SLBufferQueueState to go to zero.
		*/
		result = (*stream->playerBufferQueue)->GetState(stream->playerBufferQueue, &state);
		//TODO : check error
		while(state.count){
			(*stream->playerBufferQueue)->GetState(stream->playerBufferQueue, &state);
		}
		(*stream->playerPlay)->SetPlayState(stream->playerPlay, SL_PLAYSTATE_STOPPED);


	}


	on_teardown_audio_wrapper();

	PJ_LOG(4,(THIS_FILE, "Stopping Done"));

	return PJ_SUCCESS;

}

/* API: destroy stream. */
static pj_status_t strm_destroy(pjmedia_aud_stream *s)
{

	PJ_LOG(4,(THIS_FILE, "Destroying stream"));

	//Stop the stream
	strm_stop(s);

	struct opensl_aud_stream *stream = (struct opensl_aud_stream*)s;

	if(stream->playerBufferQueue && stream->playerPlay) {
		/* Destroy the player */
		(*stream->playerObject)->Destroy(stream->playerObject);

		PJ_LOG(3,(THIS_FILE, "---> Released track"));
	}else{
		PJ_LOG(2,(THIS_FILE, "Nothing to release !!! track"));
	}

	pj_pool_release(stream->pool);
	PJ_LOG(3,(THIS_FILE, "Stream is destroyed"));

	return PJ_SUCCESS;
}

#endif	/* PJMEDIA_AUDIO_DEV_HAS_ANDROID */

