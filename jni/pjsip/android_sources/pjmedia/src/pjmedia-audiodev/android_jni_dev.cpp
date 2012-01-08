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

#include "android_dev.h"
#include "audio_dev_wrap.h"
#include <sys/system_properties.h>

#if PJMEDIA_AUDIO_DEV_HAS_ANDROID

#define USE_CSIPSIMPLE 1
#define COMPATIBLE_ALSA 1


#define THIS_FILE	"android_jni_dev.cpp"
#define DRIVER_NAME	"ANDROID"

#include "pj_loader.h"

struct android_aud_factory
{
	pjmedia_aud_dev_factory base;
	pj_pool_factory *pf;
	pj_pool_t *pool;
};

/* 
 * Sound stream descriptor.
 * This struct may be used for both unidirectional or bidirectional sound
 * streams.
 */
struct android_aud_stream
{
	pjmedia_aud_stream base;

	pj_pool_t *pool;
	pj_str_t name;
	pjmedia_dir dir;
	pjmedia_aud_param param;

	int bytes_per_sample;
	pj_uint32_t samples_per_sec;
	unsigned samples_per_frame;
	int channel_count;
	void *user_data;

	pj_bool_t quit_flag;

	//Record
	jobject	record;
	jclass 	record_class;
	pjmedia_aud_rec_cb rec_cb;
	pj_bool_t rec_thread_exited;
	//pj_thread_desc rec_thread_desc;
	pj_thread_t *rec_thread;

	//Track
	jobject	track;
	jclass 	track_class;
	pjmedia_aud_play_cb play_cb;
	pj_bool_t play_thread_exited;
	//pj_thread_desc play_thread_desc;
	pj_thread_t *play_thread;

};

/* Factory prototypes */
static pj_status_t android_init(pjmedia_aud_dev_factory *f);
static pj_status_t android_destroy(pjmedia_aud_dev_factory *f);
static pj_status_t android_refresh(pjmedia_aud_dev_factory *f);
static unsigned android_get_dev_count(pjmedia_aud_dev_factory *f);
static pj_status_t android_get_dev_info(pjmedia_aud_dev_factory *f,
		unsigned index,
		pjmedia_aud_dev_info *info);
static pj_status_t android_default_param(pjmedia_aud_dev_factory *f,
		unsigned index,
		pjmedia_aud_param *param);
static pj_status_t android_create_stream(pjmedia_aud_dev_factory *f,
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

static pjmedia_aud_dev_factory_op android_op =
{
	&android_init,
	&android_destroy,
	&android_get_dev_count,
	&android_get_dev_info,
	&android_default_param,
	&android_create_stream,
    &android_refresh
};

static pjmedia_aud_stream_op android_strm_op =
{
	&strm_get_param,
	&strm_get_cap,
	&strm_set_cap,
	&strm_start,
	&strm_stop,
	&strm_destroy
};

// Thread priority utils
// TODO : port it to pj_thread functions
#define THREAD_PRIORITY_AUDIO -16
#define THREAD_PRIORITY_URGENT_AUDIO -19

pj_status_t set_android_thread_priority(int priority){
	jclass process_class;
	jmethodID set_prio_method;
	jthrowable exc;
	JNIEnv *jni_env = 0;
	ATTACH_JVM(jni_env);
	pj_status_t result = PJ_SUCCESS;

	//Get pointer to the java class
	process_class = (jclass)jni_env->NewGlobalRef(jni_env->FindClass("android/os/Process"));
	if (process_class == 0) {
		PJ_LOG(1, (THIS_FILE, "Not able to find os process class"));
		result = PJ_EIGNORED;
		goto on_finish;
	}

	PJ_LOG(4, (THIS_FILE, "We have the class for process"));

	//Get the set priority function
	set_prio_method = jni_env->GetStaticMethodID(process_class, "setThreadPriority", "(I)V");
	if (set_prio_method == 0) {
		PJ_LOG(1, (THIS_FILE, "Not able to find setThreadPriority method"));
		result = PJ_EIGNORED;
		goto on_finish;
	}
	PJ_LOG(4, (THIS_FILE, "We have the method for setThreadPriority"));

	//Call it
	jni_env->CallStaticVoidMethod(process_class, set_prio_method, priority);

	exc = jni_env->ExceptionOccurred();
	if (exc) {
		jni_env->ExceptionDescribe();
		jni_env->ExceptionClear();
		PJ_LOG(2, (THIS_FILE, "Impossible to set priority using java API, fallback to setpriority"));
		setpriority(PRIO_PROCESS, 0, priority);
	}

	on_finish:
		DETACH_JVM(jni_env);
		return result;

}


static int PJ_THREAD_FUNC AndroidRecorderCallback(void* userData){
	struct android_aud_stream *stream = (struct android_aud_stream*) userData;
	JNIEnv *jni_env = 0;
	ATTACH_JVM(jni_env);

	jmethodID read_method=0, record_method=0;
	int bytesRead;
	int size =  stream->samples_per_frame * stream->bytes_per_sample;
	int nframes = stream->samples_per_frame / stream->channel_count;
	jbyte* buf;
	pj_status_t status = 0;
	jbyteArray inputBuffer;
	pj_timestamp tstamp, now, last_frame;

	int elapsed_time = 0;
	//Frame time in ms
	int frame_time = nframes * 1000 / stream->samples_per_sec;
	int missed_time = frame_time;
	int to_wait = 0;

	PJ_LOG(3,(THIS_FILE, "<< Enter recorder thread"));

	if(!stream->record){
		goto on_break;
	}


	//Get methods ids
	read_method = jni_env->GetMethodID(stream->record_class,"read", "([BII)I");
	record_method = jni_env->GetMethodID(stream->record_class,"startRecording", "()V");
	if(read_method==0 || record_method==0) {
		goto on_break;
	}

	//Create a buffer for frames read
	inputBuffer = jni_env->NewByteArray(size);
	if (inputBuffer == 0) {
		PJ_LOG(2, (THIS_FILE, "Not able to allocate a buffer for input read process"));
		goto on_break;
	}


	//start recording
	//setpriority(PRIO_PROCESS, 0, -19 /*ANDROID_PRIORITY_AUDIO*/);
	// set priority is probably not enough cause does not change the thread group in scheduler
	// Temporary solution is to call the java api to set the thread priority.
	// A cool solution would be to port (if possible) the code from the android os regarding set_sched groups
	set_android_thread_priority(THREAD_PRIORITY_URGENT_AUDIO);

	buf = jni_env->GetByteArrayElements(inputBuffer, 0);

	//Init everything
	tstamp.u64 = 0;
	pj_bzero (buf, size);


	jni_env->CallVoidMethod(stream->record, record_method);
	pj_get_timestamp(&last_frame);

	while ( !stream->quit_flag ) {
		pj_bzero (buf, size);

#if COMPATIBLE_ALSA
		pj_get_timestamp(&now);
		// Time between now and last frame next frame (ms)
		elapsed_time = pj_elapsed_msec(&last_frame, &now);

		pj_get_timestamp(&last_frame);

		//PJ_LOG (4, (THIS_FILE, "Elapsed time is %d | missed time is %d | frame time %d", elapsed_time, missed_time, frame_time));
		//Update missed time
		// Positif if we are late
		// negatif if we are earlier
		// dividing by 2 is empiric result
		// on N1 if not we get buffer overflow I assume that it fill packets faster than the frequency
		missed_time =  missed_time/2 + elapsed_time - frame_time;

		//PJ_LOG (4, (THIS_FILE, "And now :: Elapsed time is %d | missed time is %d", elapsed_time, missed_time));

		//If we go faster than the buffer filling we have to wait no
		if( missed_time <= 0 ){
			//if(elapsed_time < frame_time){
				to_wait = - missed_time - 2;
				if(to_wait > 0){
			//		PJ_LOG (4, (THIS_FILE, "Wait for %d / %d", to_wait, frame_time));
					pj_thread_sleep(to_wait);
				}
			//}
		}
/*
		//PJ_LOG (4, (THIS_FILE, "Next frame %d", next_frame_in));
		if (next_frame_in-2 > 0) {
			//PJ_LOG (4, (THIS_FILE, "Wait for buffer %d", next_frame_in));
			pj_thread_sleep(next_frame_in-5);
			//Reset the delay we have regarding next frame
			retard = 0;
		}else{
			if(next_frame_in < 0){
				retard += next_frame_in;
			}
		}
*/
#endif

		bytesRead = jni_env->CallIntMethod(stream->record, read_method,
					inputBuffer,
					0,
					size);


		if(bytesRead<=0){
			PJ_LOG (3, (THIS_FILE, "Record thread : error while reading data... is there something we can do here? %d", bytesRead));
			continue;
		}
		if(stream->quit_flag){
			break;
		}
		if(bytesRead != size){
			PJ_LOG(3, (THIS_FILE, "Overrun..."));
			continue;
		}

	//	PJ_LOG(4,(THIS_FILE, "Valid record frame read"));
		//jni_env->GetByteArrayRegion(inputBuffer, 0, size, buf );

		pjmedia_frame frame;

		frame.type = PJMEDIA_FRAME_TYPE_AUDIO;
		frame.size =  size;
		frame.bit_info = 0;
		frame.buf = (void*) buf;
		frame.timestamp.u64 = tstamp.u64;

	//	PJ_LOG(3, (THIS_FILE, "New audio record frame to treat : %d <size : %d>", frame.type, frame.size));

		status = (*stream->rec_cb)(stream->user_data, &frame);
	//	PJ_LOG(4,(THIS_FILE, "Valid record frame sent to network stack"));

		if (status != PJ_SUCCESS){
			PJ_LOG(1, (THIS_FILE, "Error in record callback"));
			goto on_finish;
		}


		//Update for next step
		tstamp.u64 += nframes;
	};


	on_finish:
		jni_env->ReleaseByteArrayElements(inputBuffer, buf, 0);
		jni_env->DeleteLocalRef(inputBuffer);

	on_break:
		DETACH_JVM(jni_env);
		PJ_LOG(3,(THIS_FILE, ">> Record thread stopped"));
//		pj_sem_post(stream->audio_launch_sem);
		stream->rec_thread_exited = 1;
		return 0;
}


static int PJ_THREAD_FUNC AndroidTrackCallback(void* userData){
	struct android_aud_stream *stream = (struct android_aud_stream*) userData;
	JNIEnv *jni_env = 0;
	ATTACH_JVM(jni_env);
	jmethodID write_method=0, play_method=0;
	//jmethodID get_state_method=0;
	pj_status_t status = 0;
	//jint track_state;
	int size =  stream->samples_per_frame * stream->bytes_per_sample;
	int nframes = stream->samples_per_frame / stream->channel_count;
	jbyte* buf;
	jbyteArray outputBuffer;
	pj_timestamp tstamp;

	PJ_LOG(3,(THIS_FILE, "<< Enter player thread"));

	if(!stream->track){
		goto on_break;
	}

	//Get methods ids
	write_method = jni_env->GetMethodID(stream->track_class,"write", "([BII)I");
	play_method = jni_env->GetMethodID(stream->track_class,"play", "()V");
	/*
	get_state_method =  jni_env->GetMethodID(stream->track_class,"getState", "()I");
	if(get_state_method==0) {
		goto on_break;
	}*/

	/*
	track_state = jni_env->CallIntMethod(stream->track, get_state_method);
	PJ_LOG(3,(THIS_FILE, "Player state is now %d", track_state));
	if((int)track_state != 1){
		PJ_LOG(1, (THIS_FILE, "Bad player state !!! %d", track_state));
		goto on_break;
	}*/

	outputBuffer = jni_env->NewByteArray(size);
	if (outputBuffer == 0) {
		PJ_LOG(2, (THIS_FILE, "Not able to allocate a buffer for input play process"));
		goto on_break;
	}

	buf = jni_env->GetByteArrayElements(outputBuffer, 0);

	set_android_thread_priority(THREAD_PRIORITY_URGENT_AUDIO);
	//setpriority(PRIO_PROCESS, 0, -19 /*ANDROID_PRIORITY_URGENT_AUDIO*/);

	//start playing
	jni_env->CallVoidMethod(stream->track, play_method);

	//Init everything
	tstamp.u64 = 0;
	pj_bzero (buf, size);

//	pj_sem_post(stream->audio_launch_sem);

	while ( !stream->quit_flag ) {
		pj_bzero (buf, size);
		pjmedia_frame frame;

		frame.type = PJMEDIA_FRAME_TYPE_AUDIO;
		frame.size = size;
		frame.buf = (void *) buf;
		frame.timestamp.u64 = tstamp.u64;
		frame.bit_info = 0;

		//Fill frame from pj
		status = (*stream->play_cb)(stream->user_data, &frame);
		if (status != PJ_SUCCESS){
			goto on_finish;
		}

		if (frame.type != PJMEDIA_FRAME_TYPE_AUDIO){
			pj_bzero(frame.buf, frame.size);
			PJ_LOG(3, (THIS_FILE, "Hey, not an audio frame !!!"));
			continue;
		}

	//	PJ_LOG(4,(THIS_FILE, "Valid play frame get from network stack"));
		/*
		if(size != frame.size){
			PJ_LOG(2, (THIS_FILE, "Frame size doesn't match : %d vs %d", frame.size, size) );
		}
		*/
		//PJ_LOG(4, (THIS_FILE, "New audio track frame to treat : %d <size : %d>", frame.type, frame.size));

		//Write to the java buffer
		//jni_env->SetByteArrayRegion(outputBuffer, 0, frame.size, (jbyte*)frame.buf);

		//Write to the device output
		status = jni_env->CallIntMethod(stream->track, write_method,
				outputBuffer,
				0,
				frame.size);

		if(status < 0){
			PJ_LOG(1, (THIS_FILE, "Error while writing %d ", status));
			//goto on_finish;
			continue;
		}else if(size != status){
			PJ_LOG(2, (THIS_FILE, "Not everything written"));
		}

	//	PJ_LOG(4,(THIS_FILE, "Valid play frame sent to the audio layer"));

		tstamp.u64 += nframes;
	};

	on_finish:
	jni_env->ReleaseByteArrayElements(outputBuffer, buf, 0);
		jni_env->DeleteLocalRef(outputBuffer);


	on_break:
		DETACH_JVM(jni_env);
//		pj_sem_post(stream->audio_launch_sem);
		PJ_LOG(3,(THIS_FILE, ">> Play thread stopped"));
		stream->play_thread_exited = 1;
		return 0;
}

/*
 * Init Android audio driver.
 */
pjmedia_aud_dev_factory* pjmedia_android_factory(pj_pool_factory *pf)
{
	struct android_aud_factory *f;
	pj_pool_t *pool;

	pool = pj_pool_create(pf, "android", 64, 64, NULL);
	f = PJ_POOL_ZALLOC_T(pool, struct android_aud_factory);
	f->pf = pf;
	f->pool = pool;
	f->base.op = &android_op;

	return &f->base;
}

/* API: Init factory */
static pj_status_t android_init(pjmedia_aud_dev_factory *f)
{
	int err;

	PJ_UNUSED_ARG(f);

	PJ_LOG(4,(THIS_FILE, "Android sound library initialized"));
	PJ_LOG(4,(THIS_FILE, "Sound device count=%d", android_get_dev_count(f)));

	return PJ_SUCCESS;
}


/* API: refresh the list of devices */
static pj_status_t android_refresh(pjmedia_aud_dev_factory *f)
{
    PJ_UNUSED_ARG(f);
    return PJ_SUCCESS;
}


/* API: Destroy factory */
static pj_status_t android_destroy(pjmedia_aud_dev_factory *f)
{
	struct android_aud_factory *pa = (struct android_aud_factory*)f;
	pj_pool_t *pool;
	int err;

	PJ_LOG(4,(THIS_FILE, "Android sound library shutting down.."));

	pool = pa->pool;
	pa->pool = NULL;
	pj_pool_release(pool);

	return PJ_SUCCESS;
}

/* API: Get device count. */
static unsigned android_get_dev_count(pjmedia_aud_dev_factory *f)
{
	int count = 1;
	PJ_UNUSED_ARG(f);
	return count < 0 ? 0 : count;
}

/* API: Get device info. */
static pj_status_t android_get_dev_info(pjmedia_aud_dev_factory *f,
		unsigned index,
		pjmedia_aud_dev_info *info)
{

	PJ_UNUSED_ARG(f);

	pj_bzero(info, sizeof(*info));

	pj_ansi_strcpy(info->name, "Android Audio");
	info->default_samples_per_sec = 8000;
	info->caps = PJMEDIA_AUD_DEV_CAP_INPUT_VOLUME_SETTING |
				 PJMEDIA_AUD_DEV_CAP_OUTPUT_VOLUME_SETTING;
	info->input_count = 1;
	info->output_count = 1;

	return PJ_SUCCESS;
}

/* API: fill in with default parameter. */
static pj_status_t android_default_param(pjmedia_aud_dev_factory *f,
		unsigned index,
		pjmedia_aud_param *param)
{

	PJ_LOG(4,(THIS_FILE, "Default params"));
	pjmedia_aud_dev_info adi;
	pj_status_t status;

	PJ_UNUSED_ARG(f);

	status = android_get_dev_info(f, index, &adi);
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
static pj_status_t android_create_stream(pjmedia_aud_dev_factory *f,
		const pjmedia_aud_param *param,
		pjmedia_aud_rec_cb rec_cb,
		pjmedia_aud_play_cb play_cb,
		void *user_data,
		pjmedia_aud_stream **p_aud_strm)
{

	PJ_LOG(4,(THIS_FILE, "Creating stream"));
	struct android_aud_factory *pa = (struct android_aud_factory*)f;
	pj_pool_t *pool;
	struct android_aud_stream *stream;
	pj_status_t status;
	int has_set_in_call = 0;
	int state = 0;

	PJ_ASSERT_RETURN(play_cb && rec_cb && p_aud_strm, PJ_EINVAL);


	// Only supports for mono channel for now
	PJ_ASSERT_RETURN(param->channel_count == 1, PJ_EINVAL);


	pool = pj_pool_create(pa->pf, "sndstream", 1024, 1024, NULL);
	if (!pool) {
		return PJ_ENOMEM;
	}

	stream = PJ_POOL_ZALLOC_T(pool, struct android_aud_stream);
	stream->pool = pool;
	pj_strdup2_with_null(pool, &stream->name, "Android stream");
	stream->dir = PJMEDIA_DIR_CAPTURE_PLAYBACK;
	stream->param = *param;
	stream->user_data = user_data;
	stream->samples_per_sec = param->clock_rate;
	stream->samples_per_frame = param->samples_per_frame;
	stream->bytes_per_sample = param->bits_per_sample / 8;
	stream->channel_count = param->channel_count;
	stream->rec_cb = rec_cb;
	stream->play_cb = play_cb;

	PJ_LOG(3, (THIS_FILE, "Create stream : %d samples/sec, %d samples/frame, %d bytes/sample", stream->samples_per_sec, stream->samples_per_frame, stream->bytes_per_sample));

/*
	if(pj_sem_create(pool, NULL, 0, 2, &stream->audio_launch_sem) != PJ_SUCCESS){
		pj_pool_release(pool);
		return PJ_ENOMEM;
	}
*/

	int inputBuffSize=0, inputBuffSizePlay, inputBuffSizeRec;
	int sampleFormat;

	//TODO : return codes should be better
	JNIEnv *jni_env = 0;
	ATTACH_JVM(jni_env);
	jmethodID constructor_method=0, get_min_buffer_size_method = 0, method_id = 0;


	status = on_setup_audio_wrapper(param->clock_rate);
	if(status != PJ_SUCCESS){
		return PJMEDIA_EAUD_INVOP;
	}
	has_set_in_call = 1;

/*
	if (attachResult != 0) {
		PJ_LOG(1, (THIS_FILE, "Not able to attach the jvm"));
		pj_pool_release(pool);
		return PJ_ENOMEM;
	}
*/
	if (param->bits_per_sample == 8) {
		sampleFormat = 3; //ENCODING_PCM_8BIT
	} else if (param->bits_per_sample == 16) {
		sampleFormat = 2; //ENCODING_PCM_16BIT
	} else {
		pj_pool_release(pool);
		return PJMEDIA_EAUD_SAMPFORMAT;
	}

	PJ_LOG(3, (THIS_FILE, "Sample format is : %d for %d ", sampleFormat, param->bits_per_sample));



	if (stream->dir & PJMEDIA_DIR_CAPTURE) {
		//Get pointer to the java class
		stream->record_class = (jclass)jni_env->NewGlobalRef(jni_env->FindClass("android/media/AudioRecord"));
		if (stream->record_class == 0) {
			PJ_LOG(2, (THIS_FILE, "Not able to find audio record class"));
			goto on_error;
		}

		PJ_LOG(3, (THIS_FILE, "We have the class"));

		//Get the min buffer function
		get_min_buffer_size_method = jni_env->GetStaticMethodID(stream->record_class, "getMinBufferSize", "(III)I");
		if (get_min_buffer_size_method == 0) {
			PJ_LOG(2, (THIS_FILE, "Not able to find audio record getMinBufferSize method"));
			goto on_error;
		}
		PJ_LOG(3, (THIS_FILE, "We have the buffer method"));
		//Call it
		inputBuffSizeRec = jni_env->CallStaticIntMethod(stream->record_class, get_min_buffer_size_method,
				param->clock_rate, 2, sampleFormat);

		if(inputBuffSizeRec <= 0){
			PJ_LOG(2, (THIS_FILE, "Min buffer size is not a valid value"));
			goto on_error;
		}

		if(inputBuffSizeRec <= 4096){
			inputBuffSizeRec = 4096 * 3/2;
		}
		int frameSizeInBytes = (param->bits_per_sample == 8) ? 1 : 2;
		if ( inputBuffSizeRec % frameSizeInBytes != 0 ){
			inputBuffSizeRec ++;
		}

		PJ_LOG(3, (THIS_FILE, "Min record buffer %d", inputBuffSizeRec));

		if(inputBuffSizeRec > inputBuffSize){
			inputBuffSize = inputBuffSizeRec;
		}

	}

	if (stream->dir & PJMEDIA_DIR_PLAYBACK) {
		//Get pointer to the java class
		stream->track_class = (jclass)jni_env->NewGlobalRef(jni_env->FindClass("android/media/AudioTrack"));
		if (stream->track_class == 0) {
			PJ_LOG(2, (THIS_FILE, "Not able to find audio track class"));
			goto on_error;
		}

		PJ_LOG(3, (THIS_FILE, "We have the track class"));

		//Get the min buffer function
		get_min_buffer_size_method = jni_env->GetStaticMethodID(stream->track_class, "getMinBufferSize", "(III)I");
		if (get_min_buffer_size_method == 0) {
			PJ_LOG(2, (THIS_FILE, "Not able to find audio record getMinBufferSize method"));
			goto on_error;
		}
		PJ_LOG(3, (THIS_FILE, "We have the buffer method"));
		//Call it
		inputBuffSizePlay = jni_env->CallStaticIntMethod(stream->track_class, get_min_buffer_size_method,
				param->clock_rate, 2, sampleFormat);

		if(inputBuffSizePlay < 0){
			PJ_LOG(2, (THIS_FILE, "Min buffer size is not a valid value"));
			goto on_error;
		}

		//Not sure that's a good idea

		if(inputBuffSizePlay < 2*2*1024*param->clock_rate/8000){
			inputBuffSizePlay = 2*2*1024*param->clock_rate/8000;
		}

		int frameSizeInBytes = (param->bits_per_sample == 8) ? 1 : 2;
		if ( inputBuffSizePlay % frameSizeInBytes != 0 ){
			inputBuffSizePlay ++;
		}

		//inputBuffSizePlay = inputBuffSizePlay << 1;
		PJ_LOG(3, (THIS_FILE, "Min play buffer %d", inputBuffSizePlay));

		if(inputBuffSizePlay > inputBuffSize){
			inputBuffSize = inputBuffSizePlay;
		}
	}

	PJ_LOG(3, (THIS_FILE, "Min buffer %d", inputBuffSize));



	if (stream->dir & PJMEDIA_DIR_CAPTURE) {
		//Get pointer to the constructor
		constructor_method = jni_env->GetMethodID(stream->record_class,"<init>", "(IIIII)V");
		if (constructor_method == 0) {
			PJ_LOG(2, (THIS_FILE, "Not able to find audio record class constructor"));
			goto on_error;
		}

		int mic_source = on_set_micro_source_wrapper();
		if(mic_source == 0){
			mic_source = 1;
			char sdk_version[PROP_VALUE_MAX];
			__system_property_get("ro.build.version.sdk", sdk_version);

			pj_str_t pj_sdk_version = pj_str(sdk_version);
			int sdk_v = pj_strtoul(&pj_sdk_version);
			if(sdk_v >= 10){
				mic_source = 7;
			}
		}
		PJ_LOG(3, (THIS_FILE, "Use micro source : %d", mic_source));

		stream->record =  jni_env->NewObject(stream->record_class, constructor_method,
					mic_source, // Mic input source:  1 = MIC / 7 = VOICE_COMMUNICATION
					param->clock_rate,
					2, // CHANNEL_CONFIGURATION_MONO
					sampleFormat,
					inputBuffSizeRec);


		if (stream->record == 0) {
			PJ_LOG(1, (THIS_FILE, "Not able to instantiate record class"));
			goto on_error;
		}
		jthrowable exc = jni_env->ExceptionOccurred();
		if (exc) {
			jni_env->ExceptionDescribe();
			jni_env->ExceptionClear();
			PJ_LOG(2, (THIS_FILE, "The micro source was probably not valid"));
			// Try to fallback on MIC source -- lazy failure
			if(mic_source != 1){
				PJ_LOG(4, (THIS_FILE, "Try default source"));
				stream->record =  jni_env->NewObject(stream->record_class, constructor_method,
							1, // Mic input source:  1 = MIC / 7 = VOICE_COMMUNICATION
							param->clock_rate,
							2, // CHANNEL_CONFIGURATION_MONO
							sampleFormat,
							inputBuffSizeRec);
				if (stream->record == 0) {
					PJ_LOG(1, (THIS_FILE, "Not able to instantiate record class"));
					goto on_error;
				}
			}else{
				PJ_LOG(1, (THIS_FILE, "Not able to instantiate record class"));
				goto on_error;
			}
		}
		// Check state
		method_id = jni_env->GetMethodID(stream->record_class,"getState", "()I");
		state = jni_env->CallIntMethod(stream->record, method_id);
		if(state == 0){ /* STATE_UNINITIALIZED */
			// Try to fallback on MIC source -- lazy failure
			if(mic_source != 1){
				PJ_LOG(4, (THIS_FILE, "Try default source"));
				stream->record =  jni_env->NewObject(stream->record_class, constructor_method,
							1, // Mic input source:  1 = MIC / 7 = VOICE_COMMUNICATION
							param->clock_rate,
							2, // CHANNEL_CONFIGURATION_MONO
							sampleFormat,
							inputBuffSizeRec);
				if (stream->record == 0) {
					PJ_LOG(1, (THIS_FILE, "Not able to instantiate record class"));
					goto on_error;
				}
			}else{
				PJ_LOG(1, (THIS_FILE, "Not able to instantiate record class"));
				goto on_error;
			}
		}

		stream->record = jni_env->NewGlobalRef(stream->record);

		PJ_LOG(3, (THIS_FILE, "We have capture the instance done"));

	}




	if (stream->dir & PJMEDIA_DIR_PLAYBACK) {

		//Get pointer to the constructor
		constructor_method = jni_env->GetMethodID(stream->track_class,"<init>", "(IIIIII)V");
		if (constructor_method == 0) {
			PJ_LOG(2, (THIS_FILE, "Not able to find audio track class constructor"));
			goto on_error;
		}

		stream->track =  jni_env->NewObject(stream->track_class, constructor_method,
					0, // VOICE_CALL
				//	3, //MUSIC
					param->clock_rate,
					2, // CHANNEL_CONFIGURATION_MONO
					sampleFormat,
					inputBuffSizePlay /**2*/,
					1); // MODE_STREAM


		stream->track = jni_env->NewGlobalRef(stream->track);
		if (stream->track == 0) {
			PJ_LOG(1, (THIS_FILE, "Not able to instantiate track class"));
			goto on_error;
		}

		//TODO check if initialized properly

		PJ_LOG(3, (THIS_FILE, "We have the track instance done"));

	}




	//OK, done
	*p_aud_strm = &stream->base;
	(*p_aud_strm)->op = &android_strm_op;
	DETACH_JVM(jni_env);

	return PJ_SUCCESS;

on_error:

	if(has_set_in_call == 1){
		on_teardown_audio_wrapper();
	}
	DETACH_JVM(jni_env);
	pj_pool_release(pool);
	return PJ_ENOMEM;
}

/* API: Get stream parameters */
static pj_status_t strm_get_param(pjmedia_aud_stream *s,
		pjmedia_aud_param *pi)
{

	PJ_LOG(4,(THIS_FILE, "Get stream params"));
	struct android_aud_stream *strm = (struct android_aud_stream*)s;
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
	struct android_aud_stream *strm = (struct android_aud_stream*)s;

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
	struct android_aud_stream *stream = (struct android_aud_stream*)s;


	PJ_LOG(4,(THIS_FILE, "Starting %s stream..", stream->name.ptr));
	stream->quit_flag = 0;

	JNIEnv *jni_env = 0;
	ATTACH_JVM(jni_env);

	pj_status_t status;

	//Start threads
	if(stream->record){

		status = pj_thread_create(stream->pool, "android_recorder", &AndroidRecorderCallback, stream, 0, 0,  &stream->rec_thread);
		if (status != PJ_SUCCESS) {
			goto on_error;
		}
//		pj_sem_wait(stream->audio_launch_sem);
	}

	if(stream->track){
		status = pj_thread_create(stream->pool, "android_track", &AndroidTrackCallback, stream, 0, 0,  &stream->play_thread);
		if (status != PJ_SUCCESS) {
			goto on_error;
		}
//		pj_sem_wait(stream->audio_launch_sem);
	}

	PJ_LOG(4,(THIS_FILE, "Starting done"));

	status = PJ_SUCCESS;

on_error:
	DETACH_JVM(jni_env);
	if(status != PJ_SUCCESS){
		strm_destroy(&stream->base);
	}
	return status;
}

/* API: stop stream. */
static pj_status_t strm_stop(pjmedia_aud_stream *s)
{
	struct android_aud_stream *stream = (struct android_aud_stream*)s;
	int i;
	//We assume that all jni calls are safe ... that's acceptable
	if(stream->quit_flag == 0){
		PJ_LOG(3, (THIS_FILE, "Stopping stream"));
	}else{
		PJ_LOG(2, (THIS_FILE, "Already stopped.... nothing to do here"));
		return PJ_SUCCESS;
	}

	JNIEnv *jni_env = 0;
	ATTACH_JVM(jni_env);
	jmethodID method_id;

	stream->quit_flag = 1;

	/*
	if (result != 0) {
		PJ_LOG(1, (THIS_FILE, "Not able to attach the jvm"));
		return PJ_ENOMEM;
	}
	*/

	if(stream->record){
		//stop recording
		method_id = jni_env->GetMethodID(stream->record_class, "stop", "()V");
		jni_env->CallVoidMethod(stream->record, method_id);

		if(stream->rec_thread){
			pj_thread_join(stream->rec_thread);
			pj_thread_destroy(stream->rec_thread);
			stream->rec_thread = NULL;
		}
	}


	if(stream->track){
		method_id = jni_env->GetMethodID(stream->track_class,"flush", "()V");
		jni_env->CallVoidMethod(stream->track, method_id);
		method_id = jni_env->GetMethodID(stream->track_class, "stop", "()V");
		jni_env->CallVoidMethod(stream->track, method_id);

		if(stream->play_thread){
			pj_thread_join(stream->play_thread);
			pj_thread_destroy(stream->play_thread);
			stream->play_thread = NULL;
		}
	}



	PJ_LOG(4,(THIS_FILE, "Stopping Done"));

	DETACH_JVM(jni_env);
	return PJ_SUCCESS;

}

/* API: destroy stream. */
static pj_status_t strm_destroy(pjmedia_aud_stream *s)
{

	PJ_LOG(4,(THIS_FILE, "Destroying stream"));

	//Stop the stream
	strm_stop(s);

	struct android_aud_stream *stream = (struct android_aud_stream*)s;
	JNIEnv *jni_env = 0;
	ATTACH_JVM(jni_env);
	jmethodID release_method=0;

	if(stream->record){
		//release recording - we assume the release method exists
		release_method = jni_env->GetMethodID(stream->record_class,"release", "()V");
		jni_env->CallVoidMethod(stream->record, release_method);

		jni_env->DeleteGlobalRef(stream->record);
		jni_env->DeleteGlobalRef(stream->record_class);
		stream->record = NULL;
		stream->record_class = NULL;
		PJ_LOG(3,(THIS_FILE, "---> Released recorder"));
	}else{
		PJ_LOG(2,(THIS_FILE, "Nothing to release !!! rec"));
	}

	if(stream->track){
		//release recording - we assume the release method exists
		release_method = jni_env->GetMethodID(stream->track_class,"release", "()V");
		jni_env->CallVoidMethod(stream->track, release_method);

		jni_env->DeleteGlobalRef(stream->track);
		jni_env->DeleteGlobalRef(stream->track_class);
		stream->track = NULL;
		stream->track_class = NULL;
		PJ_LOG(3,(THIS_FILE, "---> Released track"));
	}else{
		PJ_LOG(2,(THIS_FILE, "Nothing to release !!! track"));
	}

	//Unset media in call
	on_teardown_audio_wrapper();

//	pj_sem_destroy(stream->audio_launch_sem);
	pj_pool_release(stream->pool);
	PJ_LOG(3,(THIS_FILE, "Stream is destroyed"));

	DETACH_JVM(jni_env);
	return PJ_SUCCESS;
}

#endif	/* PJMEDIA_AUDIO_DEV_HAS_ANDROID */

