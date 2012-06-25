/*
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
#include <pjmedia-videodev/videodev_imp.h>
#include <pj/assert.h>
#include <pj/log.h>
#include <pj/os.h>
#include <pj/rand.h>

#include "webrtc_android_capture_dev.h"
#define THIS_FILE		"webrtc_android_capture_dev.c"

#define __TRACE 1

#include "pj_loader.h"
#include "csipsimple_internal.h"

#include "video_capture_factory.h"


using namespace webrtc;


/* webrtc_cap_ device info */
struct webrtc_cap_dev_info {
	pjmedia_vid_dev_info info;
	char webrtc_id[256];
	VideoCaptureCapability _capability[PJMEDIA_VID_DEV_INFO_FMT_CNT]; /**< The capability to use for this stream */
	VideoCaptureRotation orientation;
};

/* webrtc_cap_ factory */
struct webrtc_cap_factory {
	pjmedia_vid_dev_factory base;
	pj_pool_t *pool;
	pj_pool_factory *pf;

	unsigned dev_count;
	struct webrtc_cap_dev_info *dev_info;
	VideoCaptureModule::DeviceInfo* _deviceInfo;
};

/* Video stream. */
struct webrtc_cap_stream {
	pjmedia_vid_dev_stream base; /**< Base stream	    */
	pjmedia_vid_dev_param param; /**< Settings	    */
	pj_pool_t *pool; /**< Memory pool.       */

	pjmedia_vid_dev_cb vid_cb; /**< Stream callback.   */
	void *user_data; /**< Application data.  */

	//const pjmedia_video_format_info *vfi;
	//pjmedia_video_apply_fmt_param vafp;
    pj_bool_t		     cap_thread_initialized;
    pj_bool_t			 window_available; /** < True if a window preview is available and if start stream is useful*/
    pj_bool_t			 capturing; /** < True if we should be capturing frames for this stream */

	VideoCaptureModule* _videoCapture;
	VideoCaptureCapability* _capability; /**< The capability to use for this stream */
	VideoCaptureDataCallback* _captureDataCb;
	//pj_timestamp ts;
	//unsigned ts_inc;
};

/* Prototypes */
static pj_status_t webrtc_cap_factory_init(pjmedia_vid_dev_factory *f);
static pj_status_t webrtc_cap_factory_destroy(pjmedia_vid_dev_factory *f);
static pj_status_t webrtc_cap_factory_refresh(pjmedia_vid_dev_factory *f);
static unsigned webrtc_cap_factory_get_dev_count(pjmedia_vid_dev_factory *f);
static pj_status_t webrtc_cap_factory_get_dev_info(pjmedia_vid_dev_factory *f,
		unsigned index, pjmedia_vid_dev_info *info);
static pj_status_t webrtc_cap_factory_default_param(pj_pool_t *pool,
		pjmedia_vid_dev_factory *f, unsigned index,
		pjmedia_vid_dev_param *param);
static pj_status_t webrtc_cap_factory_create_stream(pjmedia_vid_dev_factory *f,
		pjmedia_vid_dev_param *param, const pjmedia_vid_dev_cb *cb,
		void *user_data, pjmedia_vid_dev_stream **p_vid_strm);

static pj_status_t webrtc_cap_stream_get_param(pjmedia_vid_dev_stream *strm,
		pjmedia_vid_dev_param *param);
static pj_status_t webrtc_cap_stream_get_cap(pjmedia_vid_dev_stream *strm,
		pjmedia_vid_dev_cap cap, void *value);
static pj_status_t webrtc_cap_stream_set_cap(pjmedia_vid_dev_stream *strm,
		pjmedia_vid_dev_cap cap, const void *value);
static pj_status_t webrtc_cap_stream_start(pjmedia_vid_dev_stream *strm);
static pj_status_t webrtc_cap_stream_stop(pjmedia_vid_dev_stream *strm);
static pj_status_t webrtc_cap_stream_destroy(pjmedia_vid_dev_stream *strm);

/* Operations */
static pjmedia_vid_dev_factory_op factory_op = {
		&webrtc_cap_factory_init,
		&webrtc_cap_factory_destroy,
		&webrtc_cap_factory_get_dev_count,
		&webrtc_cap_factory_get_dev_info,
		&webrtc_cap_factory_default_param,
		&webrtc_cap_factory_create_stream,
		&webrtc_cap_factory_refresh
};

static pjmedia_vid_dev_stream_op stream_op = {
		&webrtc_cap_stream_get_param,
		&webrtc_cap_stream_get_cap,
		&webrtc_cap_stream_set_cap,
		&webrtc_cap_stream_start,
		NULL,
		NULL,
		&webrtc_cap_stream_stop,
		&webrtc_cap_stream_destroy
};


/****************************************************************************
 * Factory operations
 */
/*
 * Init webrtc_cap_ video driver.
 */
pjmedia_vid_dev_factory* pjmedia_webrtc_vid_capture_factory(
		pj_pool_factory *pf) {
	struct webrtc_cap_factory *f;
	pj_pool_t *pool;

	pool = pj_pool_create(pf, "webrtc camera", 512, 512, NULL);
	f = PJ_POOL_ZALLOC_T(pool, struct webrtc_cap_factory);
	f->pf = pf;
	f->pool = pool;
	f->base.op = &factory_op;

	// Init webRTC with what we know
	VideoCaptureFactory::SetAndroidObjects(android_jvm, css_var.context);

	return &f->base;
}

/* API: init factory */
static pj_status_t webrtc_cap_factory_init(pjmedia_vid_dev_factory *f) {
	struct webrtc_cap_factory *cf = (struct webrtc_cap_factory*) f;
	struct webrtc_cap_dev_info *ddi;
	unsigned d, i;

	PJ_LOG(4, (THIS_FILE, "Init webrtc Capture factory"));

	cf->_deviceInfo = VideoCaptureFactory::CreateDeviceInfo(0);

	cf->dev_count = cf->_deviceInfo->NumberOfDevices();
	cf->dev_info = (struct webrtc_cap_dev_info*) pj_pool_calloc(cf->pool,
			cf->dev_count, sizeof(struct webrtc_cap_dev_info));

	for (d = 0; d < cf->dev_count; d++) {
		ddi = &cf->dev_info[d];
		pj_bzero(ddi, sizeof(*ddi));

		// Get infos from webRTC
		char name[256];
		// Reverse index to find front camera before
		unsigned cam_index = (cf->dev_count - d - 1 );
		//unsigned cam_index = d;

		cf->_deviceInfo->GetDeviceName(cam_index, name, 256, ddi->webrtc_id, sizeof(ddi->webrtc_id));

		// Copy to pj struct
		pj_ansi_strncpy(ddi->info.name, (char *) name, sizeof(ddi->info.name));
		ddi->info.name[sizeof(ddi->info.name) - 1] = '\0';

		pj_ansi_strncpy(ddi->info.driver, (char *) ddi->webrtc_id,
				sizeof(ddi->info.driver));
		ddi->info.driver[sizeof(ddi->info.driver) - 1] = '\0';

		ddi->info.dir = PJMEDIA_DIR_CAPTURE;
		ddi->info.has_callback = PJ_TRUE;
		ddi->info.caps = PJMEDIA_VID_DEV_CAP_FORMAT
				| PJMEDIA_VID_DEV_CAP_INPUT_PREVIEW
				| PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW;

		PJ_LOG(4, (THIS_FILE, "Found %2d > %s aka %s", d, ddi->info.name, ddi->info.driver));

		cf->_deviceInfo->GetOrientation(ddi->webrtc_id, ddi->orientation);

		pj_log_push_indent();
		// Capabilities as pj formats
		unsigned nbrOfCaps = cf->_deviceInfo->NumberOfCapabilities(ddi->webrtc_id);

		ddi->info.fmt_cnt = 0;
		for (i = 0; i < nbrOfCaps && ddi->info.fmt_cnt < PJMEDIA_VID_DEV_INFO_FMT_CNT; i++) {
			// TODO : we should have one cap per actual possible cap
			cf->_deviceInfo->GetCapability(ddi->webrtc_id, i, ddi->_capability[ddi->info.fmt_cnt]);
			pjmedia_format *fmt = &ddi->info.fmt[ddi->info.fmt_cnt];

			PJ_LOG(5, (THIS_FILE, "Type %d - Codec %d, %dx%d @%dHz" , ddi->_capability[ddi->info.fmt_cnt].rawType,
					ddi->_capability[ddi->info.fmt_cnt].codecType,
					ddi->_capability[ddi->info.fmt_cnt].width,
					ddi->_capability[ddi->info.fmt_cnt].height,
					ddi->_capability[ddi->info.fmt_cnt].maxFPS));
			pj_uint32_t fmt_id;

			if(ddi->_capability[ddi->info.fmt_cnt].codecType == kVideoCodecUnknown){
				// WebRTC automatically transform once from rawType to I420
				// So we don't need to convert here
				// BTW, for now we ignore optimized video codecs since seems touchy to add to pjsip
				// And anyway no device in my hands supports that for now
				pjmedia_format_init_video(fmt, PJMEDIA_FORMAT_I420,
						ddi->_capability[ddi->info.fmt_cnt].width, ddi->_capability[ddi->info.fmt_cnt].height,
						ddi->_capability[ddi->info.fmt_cnt].maxFPS * 1000, 1);
				ddi->info.fmt_cnt++;
			}
		}
		pj_log_pop_indent();

		PJ_LOG(
				4,
				(THIS_FILE, "WebRTC video src initialized with %d device(s):", cf->dev_count));
	}

	return PJ_SUCCESS;
}

/* API: destroy factory */
static pj_status_t webrtc_cap_factory_destroy(pjmedia_vid_dev_factory *f) {
	struct webrtc_cap_factory *cf = (struct webrtc_cap_factory*) f;
	pj_pool_t *pool = cf->pool;

	cf->pool = NULL;
	pj_pool_release(pool);

	delete cf->_deviceInfo;

	return PJ_SUCCESS;
}

/* API: refresh the list of devices */
static pj_status_t webrtc_cap_factory_refresh(pjmedia_vid_dev_factory *f) {
	PJ_UNUSED_ARG(f);
	return PJ_SUCCESS;
}

/* API: get number of devices */
static unsigned webrtc_cap_factory_get_dev_count(pjmedia_vid_dev_factory *f) {
	struct webrtc_cap_factory *cf = (struct webrtc_cap_factory*) f;
	return cf->dev_count;
}

/* API: get device info */
static pj_status_t webrtc_cap_factory_get_dev_info(pjmedia_vid_dev_factory *f,
		unsigned index, pjmedia_vid_dev_info *info) {
	struct webrtc_cap_factory *cf = (struct webrtc_cap_factory*) f;

	PJ_ASSERT_RETURN(index < cf->dev_count, PJMEDIA_EVID_INVDEV);

	pj_memcpy(info, &cf->dev_info[index].info, sizeof(*info));

	return PJ_SUCCESS;
}

/* API: create default device parameter */
static pj_status_t webrtc_cap_factory_default_param(pj_pool_t *pool,
		pjmedia_vid_dev_factory *f, unsigned index,
		pjmedia_vid_dev_param *param) {
	struct webrtc_cap_factory *cf = (struct webrtc_cap_factory*) f;
	struct webrtc_cap_dev_info *di = &cf->dev_info[index];

	PJ_ASSERT_RETURN(index < cf->dev_count, PJMEDIA_EVID_INVDEV);

	PJ_UNUSED_ARG(pool);

	pj_bzero(param, sizeof(*param));
	param->dir = PJMEDIA_DIR_CAPTURE;
	param->cap_id = index;
	param->rend_id = PJMEDIA_VID_INVALID_DEV;
	param->flags = PJMEDIA_VID_DEV_CAP_FORMAT |
			PJMEDIA_VID_DEV_CAP_INPUT_PREVIEW |
			PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW;
	PJ_LOG(4, (THIS_FILE, "Default frequency should be %d", di->info.fmt[0].det.vid.fps.num));
	// -- we should get max else of : di->info.fmt[0].det.vid.fps.num;
	// But simplier is to over sample to 30 000 -- not seen any android cam @higher rate for now
	param->clock_rate = 30000;
	param->native_preview = PJ_TRUE;
	pj_memcpy(&param->fmt, &di->info.fmt[0], sizeof(param->fmt));

	return PJ_SUCCESS;
}

/* API: create stream */
static pj_status_t webrtc_cap_factory_create_stream(pjmedia_vid_dev_factory *f,
		pjmedia_vid_dev_param *param,
		const pjmedia_vid_dev_cb *cb,
		void *user_data,
		pjmedia_vid_dev_stream **p_vid_strm) {

	struct webrtc_cap_factory *cf = (struct webrtc_cap_factory*) f;
	pj_pool_t *pool;
	struct webrtc_cap_stream *strm;
	char* webrtc_id;
	VideoCaptureRotation rot;
	unsigned i,
		oWidth, oHeight, oFps,
		nWidth, nHeight, nFps,
		tWidth, tHeight/*, tFps*/;

	pj_status_t status = PJ_SUCCESS;

	PJ_ASSERT_RETURN(f && param && p_vid_strm, PJ_EINVAL);PJ_ASSERT_RETURN(param->fmt.type == PJMEDIA_TYPE_VIDEO &&
			param->fmt.detail_type == PJMEDIA_FORMAT_DETAIL_VIDEO &&
			param->dir == PJMEDIA_DIR_CAPTURE,
			PJ_EINVAL);


	/* Create and Initialize stream descriptor */
	pool = pj_pool_create(cf->pf, "webrtc-capture-dev", 512, 512, NULL);
	PJ_ASSERT_RETURN(pool != NULL, PJ_ENOMEM);

	JNIEnv *jni_env = 0;
	ATTACH_JVM(jni_env);


	strm = PJ_POOL_ZALLOC_T(pool, struct webrtc_cap_stream);
	pj_memcpy(&strm->param, param, sizeof(*param));
	strm->pool = pool;
	pj_memcpy(&strm->vid_cb, cb, sizeof(*cb));
	strm->user_data = user_data;

	 webrtc_cap_dev_info *ddi = &cf->dev_info[param->cap_id];

	webrtc_id = ddi->webrtc_id;
	strm->_videoCapture = VideoCaptureFactory::Create(0, webrtc_id);
	if(!strm->_videoCapture){
		PJ_LOG(4, (THIS_FILE, "%s : Impossible to create !!!", webrtc_id));
		status = PJ_ENOMEM;
		goto on_finish;
	}
	// Hold ref on video capture.
	strm->_videoCapture->AddRef();

	rot = cf->dev_info[param->cap_id].orientation;

	strm->_videoCapture->SetCaptureRotation(rot);

	PJ_LOG(4, (THIS_FILE, "Create for %s with idx %d", webrtc_id, param->cap_id));
	// WARNING : we should NEVER create a capability here because webRTC here is not necessarily
	// Launched in main thread, and as consequence loader may not contains org.webrtc packages.
	// So we can't use the utility tool from webrtc

	strm->_capability = NULL;
	tWidth = param->fmt.det.vid.size.w;
	tHeight = param->fmt.det.vid.size.h;
//	if(param->fmt.det.vid.fps.denum > 0){
//		tFps = param->fmt.det.vid.fps.num * 1.0 / param->fmt.det.vid.fps.denum;
//	}else{
//		tFps = 15;
//	}
	for(i = 0; i < ddi->info.fmt_cnt; i++){
		if(!strm->_capability){
			strm->_capability = &ddi->_capability[i];
			continue;
		}

		oWidth = strm->_capability->width;
		oHeight = strm->_capability->height;
		oFps = strm->_capability->maxFPS;

		nWidth = ddi->_capability[i].width;
		nHeight = ddi->_capability[i].height;
		nFps = ddi->_capability[i].maxFPS;

		PJ_LOG(4, (THIS_FILE, "Compare : %dx%d@%d to %dx%d@%d with target %dx%d@%d",
				oWidth, oHeight, oFps,
				nWidth, nHeight, nFps,
				tWidth, tHeight, param->clock_rate));

		if( abs(nHeight - tHeight) <= abs(oHeight - tHeight) /*&& !(nHeight < tHeight && oHeight >= tHeight)*/){
			// We have better or equal height
			if(abs(nHeight - tHeight) == abs(oHeight - tHeight)){
				// Same height, check on width
				if( abs(nWidth - tWidth) <= abs(oWidth - tWidth) ){
					// We have better or equal height
					if(abs(nWidth - tWidth) == abs(oWidth - tWidth)){
						// Same height, check on fps
						if( nFps > oFps ){
							// Well if we reach this point, probably it's the same or better ;)
							strm->_capability = &ddi->_capability[i];
							continue;
						}
					}
					// Not same width : no doubt, choose the new cap
					strm->_capability = &ddi->_capability[i];
					continue;
				}

			}
			// Not same height : no doubt, choose the new cap
			strm->_capability = &ddi->_capability[i];
			continue;
		}
	}

	if(!strm->_capability){
		status = PJ_EAFNOTSUP;
		goto on_finish;
	}

	// Setup correct choosen values for opened stream
	param->fmt.det.vid.size.w = strm->_capability->width;
	param->fmt.det.vid.size.h = strm->_capability->height;
	param->fmt.det.vid.fps.num = strm->_capability->maxFPS * 1000;
	param->fmt.det.vid.fps.denum = 1;
	param->clock_rate = strm->_capability->maxFPS * 1000;
	param->native_preview = PJ_TRUE;


	// Raz infos about stream starting
    strm->capturing = PJ_FALSE;
    strm->window_available = PJ_FALSE;

	/* Done */
	strm->base.op = &stream_op;
	*p_vid_strm = &strm->base;


	on_finish:
	DETACH_JVM(jni_env);
	return status;
}

/* API: Get stream info. */
static pj_status_t webrtc_cap_stream_get_param(pjmedia_vid_dev_stream *s,
		pjmedia_vid_dev_param *pi) {
	struct webrtc_cap_stream *strm = (struct webrtc_cap_stream*) s;

	PJ_ASSERT_RETURN(strm && pi, PJ_EINVAL);

	pj_memcpy(pi, &strm->param, sizeof(*pi));

	return PJ_SUCCESS;
}

/* API: get capability */
static pj_status_t webrtc_cap_stream_get_cap(pjmedia_vid_dev_stream *s,
		pjmedia_vid_dev_cap cap, void *pval) {
	struct webrtc_cap_stream *strm = (struct webrtc_cap_stream*) s;

	PJ_UNUSED_ARG(strm);

	PJ_ASSERT_RETURN(s && pval, PJ_EINVAL);

	if (cap == PJMEDIA_VID_DEV_CAP_INPUT_SCALE) {
		return PJMEDIA_EVID_INVCAP;
//	return PJ_SUCCESS;
	} else if(cap == PJMEDIA_VID_DEV_CAP_INPUT_PREVIEW){
		*(pj_bool_t *)pval = PJ_TRUE;
		return PJ_SUCCESS;
	} else if(cap == PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW){
		// Actually useless
		*(void **)pval = NULL;
		return PJ_SUCCESS;
	} else {
		return PJMEDIA_EVID_INVCAP;
	}
}

/* API: set capability */
static pj_status_t webrtc_cap_stream_set_cap(pjmedia_vid_dev_stream *s,
		pjmedia_vid_dev_cap cap, const void *pval) {
	struct webrtc_cap_stream *strm = (struct webrtc_cap_stream*) s;

	PJ_ASSERT_RETURN(s, PJ_EINVAL);

	if (cap == PJMEDIA_VID_DEV_CAP_INPUT_SCALE) {
		return PJ_SUCCESS;
	} else if(cap == PJMEDIA_VID_DEV_CAP_INPUT_PREVIEW){
		return PJ_SUCCESS;
	} else if(cap == PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW){
		if(pval){
			// Consider that as the fact window has been set and we could try to re-attach ourself
			if(!strm->window_available){
				PJ_LOG(4, (THIS_FILE, "We had no window available previously and now one is avail"));
				//we can start right now :)
				strm->window_available = PJ_TRUE;
				// We are capturing frames, but previously we had no window
				// Just start capture now
				if(strm->capturing){
					PJ_LOG(4, (THIS_FILE, "We should start capturing right now"));
					webrtc_cap_stream_start(s);
				}
			}
		}else{
			strm->window_available = PJ_FALSE;
			// We are capturing frames, but previously we had no window
			// Just start capture now
			if(strm->capturing){
				PJ_LOG(4, (THIS_FILE, "We should start capturing right now"));
				if(strm->_videoCapture != NULL && strm->_videoCapture->CaptureStarted()){
					WebRtc_Word32 status;
					status = strm->_videoCapture->DeRegisterCaptureDataCallback();
					status = strm->_videoCapture->StopCapture();

					PJ_LOG(4, (THIS_FILE, "Stop webrtc capture %d", status));
				}
			}
		}
		return PJ_SUCCESS;
	}

	return PJMEDIA_EVID_INVCAP;
}



class PjVideoCaptureDataCallback : public VideoCaptureDataCallback {
private:
	webrtc_cap_stream *stream;
public:
	PjVideoCaptureDataCallback(webrtc_cap_stream*);
	void OnIncomingCapturedFrame(const WebRtc_Word32 id,
	                                         VideoFrame& videoFrame,
	                                         VideoCodecType codecType);
	void OnCaptureDelayChanged(const WebRtc_Word32 id,
	                                       const WebRtc_Word32 delay);
};

PjVideoCaptureDataCallback::PjVideoCaptureDataCallback(webrtc_cap_stream* strm):
	VideoCaptureDataCallback(), stream(strm){}


void PjVideoCaptureDataCallback::OnIncomingCapturedFrame(int id, VideoFrame &videoFrame, enum VideoCodecType codecType){

	pjmedia_frame frame = {PJMEDIA_FRAME_TYPE_VIDEO};
	// For now there is only one thread for incoming frames in webRTC implementation
	// so that's fine
	if (stream->cap_thread_initialized == 0 || !pj_thread_is_registered()) {
		pj_status_t status;
		pj_thread_t* cap_thread;
		pj_thread_desc cap_thread_desc;
		status = pj_thread_register("webrtc_cap", cap_thread_desc, &cap_thread);
		if (status != PJ_SUCCESS){
			return;
		}
		stream->cap_thread_initialized = 1;
		PJ_LOG(5,(THIS_FILE, "Capture thread started"));
	}

	//PJ_LOG(4, (THIS_FILE, "We have a frame size of %d", videoFrame.Size()));
	frame.bit_info = 0;
	frame.size = videoFrame.Size();
	frame.timestamp.u64 = videoFrame.TimeStamp();
	frame.buf = videoFrame.Buffer();

	if (stream->vid_cb.capture_cb){
		(*stream->vid_cb.capture_cb)(&stream->base, stream->user_data, &frame);
	}
}

void PjVideoCaptureDataCallback::OnCaptureDelayChanged(const WebRtc_Word32 id,
        const WebRtc_Word32 delay){
	// WARNING : if we want to log, we must be attached to thread !
	//PJ_LOG(4, (THIS_FILE, "Delay changed : %d", id));
}

/* API: Start stream. */
static pj_status_t webrtc_cap_stream_start(pjmedia_vid_dev_stream *strm) {
	struct webrtc_cap_stream *stream = (struct webrtc_cap_stream*) strm;

	if(stream->window_available){
		if(!stream->_videoCapture->CaptureStarted()){
			PJ_LOG(4, (THIS_FILE, "Starting webrtc capture video : %dx%d @%d",
					stream->_capability->width, stream->_capability->height,
					stream->_capability->maxFPS));

			stream->_captureDataCb = new PjVideoCaptureDataCallback(stream);
			stream->_videoCapture->RegisterCaptureDataCallback(*stream->_captureDataCb);
			stream->_videoCapture->StartCapture(*stream->_capability);
		}
	}

	stream->capturing = PJ_TRUE;
	return PJ_SUCCESS;
}

/* API: Stop stream. */
static pj_status_t webrtc_cap_stream_stop(pjmedia_vid_dev_stream *strm) {
	struct webrtc_cap_stream *stream = (struct webrtc_cap_stream*) strm;

	if(stream->_videoCapture != NULL && stream->_videoCapture->CaptureStarted()){
		WebRtc_Word32 status;
		status = stream->_videoCapture->DeRegisterCaptureDataCallback();

		status = stream->_videoCapture->StopCapture();

		PJ_LOG(4, (THIS_FILE, "Stop webrtc capture %d", status));
	}
	stream->capturing = PJ_FALSE;
	// TODO : webrtc error could be converted here
	return PJ_SUCCESS;
}

/* API: Destroy stream. */
static pj_status_t webrtc_cap_stream_destroy(pjmedia_vid_dev_stream *strm) {
	struct webrtc_cap_stream *stream = (struct webrtc_cap_stream*) strm;

	PJ_ASSERT_RETURN(stream != NULL, PJ_EINVAL);

	PJ_LOG(4, (THIS_FILE, "Destroy webrtc capture"));
	if(stream->capturing){
		webrtc_cap_stream_stop(strm);
	}

	int32_t remains = stream->_videoCapture->Release();
	PJ_LOG(4, (THIS_FILE, "Remaining : %d", remains));

	pj_pool_release(stream->pool);

	return PJ_SUCCESS;
}

