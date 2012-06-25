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


#include "webrtc_android_render_dev.h"

#define THIS_FILE		"webrtc_android_render_dev.cpp"

#include "pj_loader.h"
#include "video_render.h"
#include "trace.h"
#include "tick_util.h"

using namespace webrtc;

#define DEFAULT_CLOCK_RATE	90000
#define DEFAULT_WIDTH		352
#define DEFAULT_HEIGHT		288
#define DEFAULT_FPS		25

typedef struct webrtcR_fmt_info {
	pjmedia_format_id fmt_id;
} webrtcR_fmt_info;

static webrtcR_fmt_info webrtcR_fmts[] = {
		{PJMEDIA_FORMAT_I420},
};

/* webrtcR_ device info */
struct webrtcR_dev_info {
	pjmedia_vid_dev_info info;
};

/* webrtcR_ factory */
struct webrtcR_factory {
	pjmedia_vid_dev_factory base;
	pj_pool_t *pool;
	pj_pool_factory *pf;

	unsigned dev_count;
	struct webrtcR_dev_info *dev_info;

};

/* Video stream. */
struct webrtcR_stream {
	pjmedia_vid_dev_stream base; /**< Base stream	    */
	pjmedia_vid_dev_param param; /**< Settings	    */
	pj_pool_t *pool; /**< Memory pool.       */
    pj_mutex_t		       *mutex;

	pjmedia_vid_dev_cb vid_cb; /**< Stream callback.   */
	void *user_data; /**< Application data.  */

	struct webrtcR_factory *sf;
	pj_bool_t is_running;
	pj_timestamp last_ts;

	VideoRender* _renderModule;
	VideoRenderCallback* _renderProvider;
	VideoFrame _videoFrame;
	void* _renderWindow;

};

/* Prototypes */
static pj_status_t webrtcR_factory_init(pjmedia_vid_dev_factory *f);
static pj_status_t webrtcR_factory_destroy(pjmedia_vid_dev_factory *f);
static pj_status_t webrtcR_factory_refresh(pjmedia_vid_dev_factory *f);
static unsigned webrtcR_factory_get_dev_count(pjmedia_vid_dev_factory *f);
static pj_status_t webrtcR_factory_get_dev_info(pjmedia_vid_dev_factory *f,
		unsigned index, pjmedia_vid_dev_info *info);
static pj_status_t webrtcR_factory_default_param(pj_pool_t *pool,
		pjmedia_vid_dev_factory *f, unsigned index,
		pjmedia_vid_dev_param *param);
static pj_status_t webrtcR_factory_create_stream(pjmedia_vid_dev_factory *f,
		pjmedia_vid_dev_param *param, const pjmedia_vid_dev_cb *cb,
		void *user_data, pjmedia_vid_dev_stream **p_vid_strm);

static pj_status_t webrtcR_stream_get_param(pjmedia_vid_dev_stream *strm,
		pjmedia_vid_dev_param *param);
static pj_status_t webrtcR_stream_get_cap(pjmedia_vid_dev_stream *strm,
		pjmedia_vid_dev_cap cap, void *value);
static pj_status_t webrtcR_stream_set_cap(pjmedia_vid_dev_stream *strm,
		pjmedia_vid_dev_cap cap, const void *value);
static pj_status_t webrtcR_stream_put_frame(pjmedia_vid_dev_stream *strm,
		const pjmedia_frame *frame);
static pj_status_t webrtcR_stream_start(pjmedia_vid_dev_stream *strm);
static pj_status_t webrtcR_stream_stop(pjmedia_vid_dev_stream *strm);
static pj_status_t webrtcR_stream_destroy(pjmedia_vid_dev_stream *strm);

/* Operations */
static pjmedia_vid_dev_factory_op factory_op = { &webrtcR_factory_init,
		&webrtcR_factory_destroy, &webrtcR_factory_get_dev_count,
		&webrtcR_factory_get_dev_info, &webrtcR_factory_default_param,
		&webrtcR_factory_create_stream, &webrtcR_factory_refresh };

static pjmedia_vid_dev_stream_op stream_op = { &webrtcR_stream_get_param,
		&webrtcR_stream_get_cap, &webrtcR_stream_set_cap, &webrtcR_stream_start,
		NULL, &webrtcR_stream_put_frame, &webrtcR_stream_stop,
		&webrtcR_stream_destroy };

/****************************************************************************
 * Factory operations
 */
/*
 * Init webrtc_render video driver.
 */
pjmedia_vid_dev_factory* pjmedia_webrtc_vid_render_factory(pj_pool_factory *pf) {
	struct webrtcR_factory *f;
	pj_pool_t *pool;

	pool = pj_pool_create(pf, "WebRTC video", 1000, 1000, NULL);
	f = PJ_POOL_ZALLOC_T(pool, struct webrtcR_factory);
	f->pf = pf;
	f->pool = pool;
	f->base.op = &factory_op;

	return &f->base;
}

/* API: init factory */
static pj_status_t webrtcR_factory_init(pjmedia_vid_dev_factory *f) {
	struct webrtcR_factory *sf = (struct webrtcR_factory*) f;
	struct webrtcR_dev_info *ddi;
	unsigned i, j;
	pj_status_t status;

	sf->dev_count = 1;
	sf->dev_info = (struct webrtcR_dev_info*) pj_pool_calloc(sf->pool,
			sf->dev_count, sizeof(struct webrtcR_dev_info));

	ddi = &sf->dev_info[0];
	pj_bzero(ddi, sizeof(*ddi));
	strncpy(ddi->info.name, "WebRTC renderer", sizeof(ddi->info.name));
	ddi->info.name[sizeof(ddi->info.name) - 1] = '\0';
	ddi->info.fmt_cnt = PJ_ARRAY_SIZE(webrtcR_fmts);

	for (i = 0; i < sf->dev_count; i++) {
		ddi = &sf->dev_info[i];
		strncpy(ddi->info.driver, "WebRTC", sizeof(ddi->info.driver));
		ddi->info.driver[sizeof(ddi->info.driver) - 1] = '\0';
		ddi->info.dir = PJMEDIA_DIR_RENDER;
		ddi->info.has_callback = PJ_FALSE;
		ddi->info.caps = PJMEDIA_VID_DEV_CAP_FORMAT;
		ddi->info.caps |= PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW;
		ddi->info.caps |= PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW_FLAGS;

		for (j = 0; j < ddi->info.fmt_cnt; j++) {
			pjmedia_format *fmt = &ddi->info.fmt[j];
			pjmedia_format_init_video(fmt, webrtcR_fmts[j].fmt_id, DEFAULT_WIDTH,
					DEFAULT_HEIGHT, DEFAULT_FPS, 1);
		}
	}

	// Init JVM
	status = VideoRender::SetAndroidObjects(android_jvm);
	Trace::SetLevelFilter(kTraceAll);


	PJ_LOG(4, (THIS_FILE, "WebRTC initialized and init android objects done : %d", status));

	return PJ_SUCCESS;
}

/* API: destroy factory */
static pj_status_t webrtcR_factory_destroy(pjmedia_vid_dev_factory *f) {
	struct webrtcR_factory *sf = (struct webrtcR_factory*) f;
	pj_pool_t *pool = sf->pool;
	pj_status_t status;

	sf->pool = NULL;
	pj_pool_release(pool);

	return PJ_SUCCESS;
}

/* API: refresh the list of devices */
static pj_status_t webrtcR_factory_refresh(pjmedia_vid_dev_factory *f) {
	PJ_UNUSED_ARG(f);
	return PJ_SUCCESS;
}

/* API: get number of devices */
static unsigned webrtcR_factory_get_dev_count(pjmedia_vid_dev_factory *f) {
	struct webrtcR_factory *sf = (struct webrtcR_factory*) f;
	return sf->dev_count;
}

/* API: get device info */
static pj_status_t webrtcR_factory_get_dev_info(pjmedia_vid_dev_factory *f,
		unsigned index, pjmedia_vid_dev_info *info) {
	struct webrtcR_factory *sf = (struct webrtcR_factory*) f;

	PJ_ASSERT_RETURN(index < sf->dev_count, PJMEDIA_EVID_INVDEV);

	pj_memcpy(info, &sf->dev_info[index].info, sizeof(*info));

	return PJ_SUCCESS;
}

/* API: create default device parameter */
static pj_status_t webrtcR_factory_default_param(pj_pool_t *pool,
		pjmedia_vid_dev_factory *f, unsigned index,
		pjmedia_vid_dev_param *param) {
	struct webrtcR_factory *sf = (struct webrtcR_factory*) f;
	struct webrtcR_dev_info *di = &sf->dev_info[index];

	PJ_ASSERT_RETURN(index < sf->dev_count, PJMEDIA_EVID_INVDEV);

	PJ_UNUSED_ARG(pool);

	pj_bzero(param, sizeof(*param));
	param->dir = PJMEDIA_DIR_RENDER;
	param->rend_id = index;
	param->cap_id = PJMEDIA_VID_INVALID_DEV;

	/* Set the device capabilities here */
	param->flags = PJMEDIA_VID_DEV_CAP_FORMAT | PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW;
	param->fmt.type = PJMEDIA_TYPE_VIDEO;
	param->clock_rate = DEFAULT_CLOCK_RATE;
	pj_memcpy(&param->fmt, &di->info.fmt[0], sizeof(param->fmt));

	return PJ_SUCCESS;
}


static pj_status_t init_stream(struct webrtcR_stream *strm){
	pj_status_t status = PJ_EINVAL;
    pj_mutex_lock(strm->mutex);
	if(strm->_renderWindow){

		strm->_renderModule = VideoRender::CreateVideoRender(0,
				strm->_renderWindow, false);

		PJ_LOG(4, (THIS_FILE, "Render module created %x", strm->_renderModule));
		strm->_renderProvider = strm->_renderModule->AddIncomingRenderStream(0,
				0, 0.0f, 0.0f, 1.0f, 1.0f);
		PJ_LOG(4, (THIS_FILE, "Render provider created %x", strm->_renderProvider));


		int stat = strm->_renderModule->StartRender(0);
		PJ_LOG(4, (THIS_FILE, "Render thread started %d", stat));

		// Deliver one fake frame to init renderer
		VideoFrame toFrame;
		toFrame.VerifyAndAllocate(3);
		toFrame.SetHeight(1);
		toFrame.SetWidth(1);
		toFrame.SetLength(3);
		memset(toFrame.Buffer(), 0, 3);
		WebRtc_Word64 nowMs = TickTime::MillisecondTimestamp();
		toFrame.SetRenderTime(nowMs);
		strm->_renderProvider->RenderFrame(0, toFrame);

		status = PJ_SUCCESS;
	}

    pj_mutex_unlock(strm->mutex);
	return status;
}

static pj_status_t destroy_stream(struct webrtcR_stream *strm){

    pj_mutex_lock(strm->mutex);

	if(strm->_renderModule){
		strm->_renderModule->StopRender(0);
		strm->_renderModule->DeleteIncomingRenderStream(0);
		VideoRender::DestroyVideoRender(strm->_renderModule);
		strm->_renderModule = NULL;
	}
    pj_mutex_unlock(strm->mutex);
	return PJ_SUCCESS;
}


/* API: Put frame from stream */
static pj_status_t webrtcR_stream_put_frame(pjmedia_vid_dev_stream *strm,
		const pjmedia_frame *frame) {
	struct webrtcR_stream *stream = (struct webrtcR_stream*) strm;
	pj_status_t status;

	stream->last_ts.u64 = frame->timestamp.u64;
    pj_mutex_lock(stream->mutex);
	if (!stream->is_running) {
		pj_mutex_unlock(stream->mutex);
		return PJ_EINVALIDOP;
	}

	if (frame->size == 0 || frame->buf == NULL
			|| frame->size == 0){
		pj_mutex_unlock(stream->mutex);
		return PJ_SUCCESS;
	}

	if(stream->_renderWindow == NULL || stream->_renderProvider == NULL){
		// We have nothing to show things in yet
		pj_mutex_unlock(stream->mutex);
		return PJ_SUCCESS;
	}

    pjmedia_video_format_detail *vfd;
    vfd = pjmedia_format_get_video_format_detail(&stream->param.fmt, PJ_TRUE);

    // w * h * 1.5 = frame size normally because I420
    unsigned width = vfd->size.w;
    unsigned height = vfd->size.h;
    pj_size_t theoric_size =  width * height;
    theoric_size += (theoric_size >> 1);
    if(theoric_size != frame->size){
    	PJ_LOG(2, (THIS_FILE, "Unexpected frame size regarding params %d vs %dx%d", frame->size, vfd->size.w, vfd->size.h));
		pj_mutex_unlock(stream->mutex);
    	return PJ_EINVALIDOP;
    }


	stream->_videoFrame.VerifyAndAllocate( frame->size );
	stream->_videoFrame.SetWidth(width);
	stream->_videoFrame.SetHeight(height);
	stream->_videoFrame.SetLength(stream->_videoFrame.Size());

	//PJ_LOG(4, (THIS_FILE, "Will render video frame : %dx%d (%d) for %x",
	//	vfd->size.w, vfd->size.h, frame->size, stream->base));

	memcpy(stream->_videoFrame.Buffer(), frame->buf, frame->size);

	WebRtc_Word64 nowMs = TickTime::MillisecondTimestamp();
	stream->_videoFrame.SetRenderTime( nowMs );
	stream->_renderProvider->RenderFrame(0, stream->_videoFrame);

	//PJ_LOG(4, (THIS_FILE, "Rendering @%lld > %lld", frame->timestamp.u64, nowMs));

	pj_mutex_unlock(stream->mutex);

	return PJ_SUCCESS;
}


/* API: create stream */
static pj_status_t webrtcR_factory_create_stream(pjmedia_vid_dev_factory *f,
		pjmedia_vid_dev_param *param, const pjmedia_vid_dev_cb *cb,
		void *user_data, pjmedia_vid_dev_stream **p_vid_strm) {
	struct webrtcR_factory *sf = (struct webrtcR_factory*) f;
	pj_pool_t *pool;
	struct webrtcR_stream *strm;
	pj_status_t status;

	PJ_ASSERT_RETURN(param->dir == PJMEDIA_DIR_RENDER, PJ_EINVAL);

	/* Create and Initialize stream descriptor */
	pool = pj_pool_create(sf->pf, "webrtc-render-dev", 1000, 1000, NULL);
	PJ_ASSERT_RETURN(pool != NULL, PJ_ENOMEM);

	strm = PJ_POOL_ZALLOC_T(pool, struct webrtcR_stream);
	pj_memcpy(&strm->param, param, sizeof(*param));

    pjmedia_video_format_detail *vfd;
    vfd = pjmedia_format_get_video_format_detail(&strm->param.fmt, PJ_TRUE);
	PJ_LOG(4, (THIS_FILE, "Apply stream params %dx%d @ %x in %x", vfd->size.w,
			vfd->size.h,
			strm, pool));
	strm->pool = pool;
	strm->sf = sf;
	pj_memcpy(&strm->vid_cb, cb, sizeof(*cb));
	strm->user_data = user_data;


    /* Create mutex. */
    status = pj_mutex_create_simple(strm->pool, "render_stream",
				    &strm->mutex);

	/* Create render stream here */
	if (param->dir & PJMEDIA_DIR_RENDER) {
		init_stream(strm);
	}

	/* Done */
	strm->base.op = &stream_op;
	*p_vid_strm = &strm->base;

	return PJ_SUCCESS;

	on_error: webrtcR_stream_destroy(&strm->base);
	return status;
}

/* API: Get stream info. */
static pj_status_t webrtcR_stream_get_param(pjmedia_vid_dev_stream *s,
		pjmedia_vid_dev_param *pi) {
	struct webrtcR_stream *strm = (struct webrtcR_stream*) s;

	PJ_ASSERT_RETURN(strm && pi, PJ_EINVAL);

	pj_memcpy(pi, &strm->param, sizeof(*pi));

	if (webrtcR_stream_get_cap(s, PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW,
			&pi->window) == PJ_SUCCESS) {
		pi->flags |= PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW;
	}
	if (webrtcR_stream_get_cap(s, PJMEDIA_VID_DEV_CAP_OUTPUT_POSITION,
			&pi->window_pos) == PJ_SUCCESS) {
		pi->flags |= PJMEDIA_VID_DEV_CAP_OUTPUT_POSITION;
	}
	if (webrtcR_stream_get_cap(s, PJMEDIA_VID_DEV_CAP_OUTPUT_RESIZE,
			&pi->disp_size) == PJ_SUCCESS) {
		pi->flags |= PJMEDIA_VID_DEV_CAP_OUTPUT_RESIZE;
	}
	if (webrtcR_stream_get_cap(s, PJMEDIA_VID_DEV_CAP_OUTPUT_HIDE,
			&pi->window_hide) == PJ_SUCCESS) {
		pi->flags |= PJMEDIA_VID_DEV_CAP_OUTPUT_HIDE;
	}
	if (webrtcR_stream_get_cap(s, PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW_FLAGS,
			&pi->window_flags) == PJ_SUCCESS) {
		pi->flags |= PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW_FLAGS;
	}

	return PJ_SUCCESS;
}

/* API: get capability */
static pj_status_t webrtcR_stream_get_cap(pjmedia_vid_dev_stream *s,
		pjmedia_vid_dev_cap cap, void *pval) {
	struct webrtcR_stream *strm = (struct webrtcR_stream*) s;
	pj_status_t status;

	PJ_ASSERT_RETURN(s && pval, PJ_EINVAL);

	if (cap == PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW) {
		*(void**)pval = strm->_renderWindow;
		return PJ_SUCCESS;
	} else if (cap == PJMEDIA_VID_DEV_CAP_FORMAT) {
		// TODO
	} else if (cap == PJMEDIA_VID_DEV_CAP_OUTPUT_RESIZE) {
		// TODO
	}

	return PJMEDIA_EVID_INVCAP;

}

/* API: set capability */
static pj_status_t webrtcR_stream_set_cap(pjmedia_vid_dev_stream *s,
		pjmedia_vid_dev_cap cap, const void *pval) {
	struct webrtcR_stream *strm = (struct webrtcR_stream*) s;
	pj_status_t status;

	PJ_ASSERT_RETURN(s, PJ_EINVAL);

	if (cap == PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW) {
		// Only do that if we have no window set currently
		if(!strm->_renderWindow || !pval){
			if(!pval){
				destroy_stream(strm);
			}
			strm->_renderWindow = (void *)pval;

			PJ_LOG(4, (THIS_FILE, "Setup window to => %x", pval));
			init_stream(strm);
			return PJ_SUCCESS;
		}
		return PJ_EEXISTS;
	} else if (cap == PJMEDIA_VID_DEV_CAP_FORMAT) {
		// TODO
		PJ_LOG(4, (THIS_FILE, "try to change format...."));
		return PJMEDIA_EVID_ERR;
	} else if (cap == PJMEDIA_VID_DEV_CAP_OUTPUT_RESIZE) {
		// TODO -- thread safe it

		pjmedia_rect_size *new_disp_size = (pjmedia_rect_size *)pval;
		if(new_disp_size){
			PJ_LOG(4, (THIS_FILE, "RESCALE %dx%d", new_disp_size->w, new_disp_size->h));
			strm->param.disp_size.w = new_disp_size->w;
			strm->param.disp_size.h = new_disp_size->h;
		}
		return PJ_SUCCESS;
	}

	return PJMEDIA_EVID_INVCAP;
}

/* API: Start stream. */
static pj_status_t webrtcR_stream_start(pjmedia_vid_dev_stream *strm) {
	struct webrtcR_stream *stream = (struct webrtcR_stream*) strm;

	PJ_LOG(4, (THIS_FILE, "Starting webRTC video stream"));

    pj_mutex_lock(stream->mutex);
	stream->is_running = PJ_TRUE;
    pj_mutex_unlock(stream->mutex);

	return PJ_SUCCESS;
}

/* API: Stop stream. */
static pj_status_t webrtcR_stream_stop(pjmedia_vid_dev_stream *strm) {
	struct webrtcR_stream *stream = (struct webrtcR_stream*) strm;

	PJ_LOG(4, (THIS_FILE, "Stop webrtc renderer"));

    pj_mutex_lock(stream->mutex);
	if(stream->_renderModule){
		stream->_renderModule->StopRender(0);
		stream->_renderModule->DeleteIncomingRenderStream(0);
	}

	stream->is_running = PJ_FALSE;
    pj_mutex_unlock(stream->mutex);



	return PJ_SUCCESS;
}

/* API: Destroy stream. */
static pj_status_t webrtcR_stream_destroy(pjmedia_vid_dev_stream *strm) {
	struct webrtcR_stream *stream = (struct webrtcR_stream*) strm;
	pj_status_t status;

	PJ_ASSERT_RETURN(stream != NULL, PJ_EINVAL);
	destroy_stream(stream);

    pj_mutex_lock(stream->mutex);
	stream->is_running = PJ_FALSE;
    pj_mutex_unlock(stream->mutex);


	pj_mutex_destroy(stream->mutex);
	stream->mutex = NULL;
	pj_pool_release(stream->pool);

	return PJ_SUCCESS;
}



