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


#include <sys/ioctl.h>
#include <linux/fb.h>
#include <fcntl.h>



#include "android_screen_capture_dev.h"
#define THIS_FILE		"android_screen_capture_dev.c"



/* cap_screen_ device info */
struct cap_screen_dev_info {
	pjmedia_vid_dev_info info;
	char screen_id[256];
};

/* cap_screen_ factory */
struct cap_screen_factory {
	pjmedia_vid_dev_factory base;
	pj_pool_t *pool;
	pj_pool_factory *pf;

};

/* Video stream. */
struct cap_screen_stream {
	pjmedia_vid_dev_stream base; /**< Base stream	    */
	pjmedia_vid_dev_param param; /**< Settings	    */
	pj_pool_t *pool; /**< Memory pool.       */

	pjmedia_vid_dev_cb vid_cb; /**< Stream callback.   */
	void *user_data; /**< Application data.  */

    pj_bool_t			 capturing; /** < True if we should be capturing frames for this stream */

    int fd_fb;
	struct fb_var_screeninfo fb_varinfo;

	int xres,yres, fmt_bps;
	char *buffer;
	struct fb_cmap *colormap;
	char bps,gray;

    pj_timestamp		     ts;
    unsigned			     ts_inc;

};

/* Prototypes */
static pj_status_t cap_screen_factory_init(pjmedia_vid_dev_factory *f);
static pj_status_t cap_screen_factory_destroy(pjmedia_vid_dev_factory *f);
static pj_status_t cap_screen_factory_refresh(pjmedia_vid_dev_factory *f);
static unsigned cap_screen_factory_get_dev_count(pjmedia_vid_dev_factory *f);
static pj_status_t cap_screen_factory_get_dev_info(pjmedia_vid_dev_factory *f,
		unsigned index, pjmedia_vid_dev_info *info);
static pj_status_t cap_screen_factory_default_param(pj_pool_t *pool,
		pjmedia_vid_dev_factory *f, unsigned index,
		pjmedia_vid_dev_param *param);
static pj_status_t cap_screen_factory_create_stream(pjmedia_vid_dev_factory *f,
		pjmedia_vid_dev_param *param, const pjmedia_vid_dev_cb *cb,
		void *user_data, pjmedia_vid_dev_stream **p_vid_strm);

static pj_status_t cap_screen_stream_get_param(pjmedia_vid_dev_stream *strm,
		pjmedia_vid_dev_param *param);
static pj_status_t cap_screen_stream_get_cap(pjmedia_vid_dev_stream *strm,
		pjmedia_vid_dev_cap cap, void *value);
static pj_status_t cap_screen_stream_set_cap(pjmedia_vid_dev_stream *strm,
		pjmedia_vid_dev_cap cap, const void *value);
static pj_status_t cap_screen_stream_start(pjmedia_vid_dev_stream *strm);
static pj_status_t cap_screen_stream_get_frame(pjmedia_vid_dev_stream *strm,
pjmedia_frame *frame);
static pj_status_t cap_screen_stream_stop(pjmedia_vid_dev_stream *strm);
static pj_status_t cap_screen_stream_destroy(pjmedia_vid_dev_stream *strm);

/* Operations */
static pjmedia_vid_dev_factory_op factory_op = {
		&cap_screen_factory_init,
		&cap_screen_factory_destroy,
		&cap_screen_factory_get_dev_count,
		&cap_screen_factory_get_dev_info,
		&cap_screen_factory_default_param,
		&cap_screen_factory_create_stream,
		&cap_screen_factory_refresh
};

static pjmedia_vid_dev_stream_op stream_op = {
		&cap_screen_stream_get_param,
		&cap_screen_stream_get_cap,
		&cap_screen_stream_set_cap,
		&cap_screen_stream_start,
		&cap_screen_stream_get_frame,
		NULL,
		&cap_screen_stream_stop,
		&cap_screen_stream_destroy
};


/**
 * FB utilities from http://code.google.com/p/android-screenshot-library/source/browse/native/fbshot.c
 */
#define ANDROID_FB_DEVICE "/dev/graphics/fb0"

/****************************************************************************
 * Factory operations
 */
/*
 * Init cap_screen_ video driver.
 */
pjmedia_vid_dev_factory* pjmedia_webrtc_vid_capture_factory(
		pj_pool_factory *pf) {
	struct cap_screen_factory *f;
	pj_pool_t *pool;

	pool = pj_pool_create(pf, "webrtc camera", 512, 512, NULL);
	f = PJ_POOL_ZALLOC_T(pool, struct cap_screen_factory);
	f->pf = pf;
	f->pool = pool;
	f->base.op = &factory_op;


	return &f->base;
}

/* API: init factory */
static pj_status_t cap_screen_factory_init(pjmedia_vid_dev_factory *f) {
	struct cap_screen_factory *cf = (struct cap_screen_factory*) f;


	return PJ_SUCCESS;
}

/* API: destroy factory */
static pj_status_t cap_screen_factory_destroy(pjmedia_vid_dev_factory *f) {
	struct cap_screen_factory *cf = (struct cap_screen_factory*) f;
	pj_pool_t *pool = cf->pool;

	cf->pool = NULL;
	pj_pool_release(pool);

	return PJ_SUCCESS;
}

/* API: refresh the list of devices */
static pj_status_t cap_screen_factory_refresh(pjmedia_vid_dev_factory *f) {
	PJ_UNUSED_ARG(f);
	return PJ_SUCCESS;
}

/* API: get number of devices */
static unsigned cap_screen_factory_get_dev_count(pjmedia_vid_dev_factory *f) {
	struct cap_screen_factory *cf = (struct cap_screen_factory*) f;
	return 1;
}

/* API: get device info */
static pj_status_t cap_screen_factory_get_dev_info(pjmedia_vid_dev_factory *f,
		unsigned index, pjmedia_vid_dev_info *info) {
	struct cap_screen_factory *cf = (struct cap_screen_factory*) f;

	PJ_ASSERT_RETURN(index < cf->dev_count, PJMEDIA_EVID_INVDEV);

	// Copy to pj struct
	pj_ansi_strncpy(info->name, "Screen capture", sizeof(info->name));
	pj_ansi_strncpy(info->driver, "Capture screen", sizeof(info->driver));

	info->dir = PJMEDIA_DIR_CAPTURE;
	info->has_callback = PJ_FALSE;
	info->caps = PJMEDIA_VID_DEV_CAP_FORMAT
			| PJMEDIA_VID_DEV_CAP_INPUT_PREVIEW
			| PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW;



	return PJ_SUCCESS;
}

/* API: create default device parameter */
static pj_status_t cap_screen_factory_default_param(pj_pool_t *pool,
		pjmedia_vid_dev_factory *f, unsigned index,
		pjmedia_vid_dev_param *param) {
	struct cap_screen_factory *cf = (struct cap_screen_factory*) f;

	PJ_ASSERT_RETURN(index < 1, PJMEDIA_EVID_INVDEV);

	PJ_UNUSED_ARG(pool);

	pj_bzero(param, sizeof(*param));
	param->dir = PJMEDIA_DIR_CAPTURE;
	param->cap_id = index;
	param->rend_id = PJMEDIA_VID_INVALID_DEV;
	param->flags = PJMEDIA_VID_DEV_CAP_FORMAT |
			PJMEDIA_VID_DEV_CAP_INPUT_PREVIEW |
			PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW;
	// -- we should get max else of : di->info.fmt[0].det.vid.fps.num;
	// But simplier is to over sample to 30 000 -- not seen any android cam @higher rate for now
	param->clock_rate = 30000;
	param->native_preview = PJ_TRUE;
	pjmedia_format_init_video(&param->fmt, PJMEDIA_FORMAT_RGB32,
			480, 800,
			1 * 1000, 1);

	return PJ_SUCCESS;
}

/* API: create stream */
static pj_status_t cap_screen_factory_create_stream(pjmedia_vid_dev_factory *f,
		pjmedia_vid_dev_param *param,
		const pjmedia_vid_dev_cb *cb,
		void *user_data,
		pjmedia_vid_dev_stream **p_vid_strm) {

	struct cap_screen_factory *cf = (struct cap_screen_factory*) f;
	pj_pool_t *pool;
	struct cap_screen_stream *strm;
    const pjmedia_video_format_detail *vfd;

	pj_status_t status = PJ_SUCCESS;

	PJ_ASSERT_RETURN(f && param && p_vid_strm, PJ_EINVAL);PJ_ASSERT_RETURN(param->fmt.type == PJMEDIA_TYPE_VIDEO &&
			param->fmt.detail_type == PJMEDIA_FORMAT_DETAIL_VIDEO &&
			param->dir == PJMEDIA_DIR_CAPTURE,
			PJ_EINVAL);


	/* Create and Initialize stream descriptor */
	pool = pj_pool_create(cf->pf, "webrtc-capture-dev", 512, 512, NULL);
	PJ_ASSERT_RETURN(pool != NULL, PJ_ENOMEM);


	strm = PJ_POOL_ZALLOC_T(pool, struct cap_screen_stream);
	pj_memcpy(&strm->param, param, sizeof(*param));
	strm->pool = pool;
	pj_memcpy(&strm->vid_cb, cb, sizeof(*cb));
	strm->user_data = user_data;


	// Get infos from fb
	struct fb_fix_screeninfo fb_fixinfo;
	int fd;
	PJ_LOG(4, (THIS_FILE, "About to open screen dev"));
	fd = open(ANDROID_FB_DEVICE, O_RDONLY);
	if (fd == -1) {
		PJ_LOG(1, (THIS_FILE, "Can't open Frame buffer"));
		return PJ_EINVAL;
	}
	PJ_LOG(4, (THIS_FILE, "About to get vscreen info %d", fd));
	if (ioctl(fd, FBIOGET_VSCREENINFO, &strm->fb_varinfo)){
		return PJ_EINVAL;
	}
	PJ_LOG(4, (THIS_FILE, "About to get screen info in %d", fd));
	if (ioctl(fd, FBIOGET_FSCREENINFO, &fb_fixinfo)){
		return PJ_EINVAL;
	}

	strm->xres = strm->fb_varinfo.xres;
	strm->yres = strm->fb_varinfo.yres;
	strm->bps = strm->fb_varinfo.bits_per_pixel;
	strm->gray = strm->fb_varinfo.grayscale;
	PJ_LOG(4, (THIS_FILE, "About to get visual pseudo color"));
	if (fb_fixinfo.visual == FB_VISUAL_PSEUDOCOLOR) {
		strm->colormap = PJ_POOL_ZALLOC_T(pool, struct fb_cmap);
		// TODO : use pool
		strm->colormap->red = (__u16 *) malloc(
				sizeof(__u16 ) * (1 << strm->bps));
		strm->colormap->green = (__u16 *) malloc(
				sizeof(__u16 ) * (1 << strm->bps));
		strm->colormap->blue = (__u16 *) malloc(
				sizeof(__u16 ) * (1 << strm->bps));
		strm->colormap->transp = (__u16 *) malloc(
				sizeof(__u16 ) * (1 << strm->bps));
		strm->colormap->start = 0;
		strm->colormap->len = 1 << strm->bps;
		if (ioctl(fd, FBIOGETCMAP, strm->colormap)) {
			return PJ_EINVAL;
		}
	}
	switch (strm->bps) {
	case 15:
		strm->fmt_bps = 2;
		break;
	default:
		strm->fmt_bps = strm->bps >> 3;
		break;
	}
	close (fd);

	PJ_LOG(4, (THIS_FILE, "Capture screen possible with %d x %d @%d bpp", strm->xres, strm->yres, strm->bps));



	// Setup correct choosen values for opened stream
	int freq = 10;


	pjmedia_format_init_video(&param->fmt, PJMEDIA_FORMAT_RGB32,
			strm->xres, strm->yres,
			freq * 1000, 1);
    vfd = pjmedia_format_get_video_format_detail(&param->fmt, PJ_TRUE);
	strm->ts_inc = PJMEDIA_SPF2(param->clock_rate, &vfd->fps, 1);
	strm->ts.u64 = 0;
	param->clock_rate = freq * 1000;
	param->native_preview = PJ_TRUE;

	// Raz infos about stream starting
	strm->capturing = PJ_FALSE;

	/* Done */
	strm->base.op = &stream_op;
	*p_vid_strm = &strm->base;


	return status;
}

/* API: Get stream info. */
static pj_status_t cap_screen_stream_get_param(pjmedia_vid_dev_stream *s,
		pjmedia_vid_dev_param *pi) {
	struct cap_screen_stream *strm = (struct cap_screen_stream*) s;

	PJ_ASSERT_RETURN(strm && pi, PJ_EINVAL);

	pj_memcpy(pi, &strm->param, sizeof(*pi));

	return PJ_SUCCESS;
}

/* API: get capability */
static pj_status_t cap_screen_stream_get_cap(pjmedia_vid_dev_stream *s,
		pjmedia_vid_dev_cap cap, void *pval) {
	struct cap_screen_stream *strm = (struct cap_screen_stream*) s;

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
static pj_status_t cap_screen_stream_set_cap(pjmedia_vid_dev_stream *s,
		pjmedia_vid_dev_cap cap, const void *pval) {
	struct cap_screen_stream *strm = (struct cap_screen_stream*) s;

	PJ_ASSERT_RETURN(s, PJ_EINVAL);

	if (cap == PJMEDIA_VID_DEV_CAP_INPUT_SCALE) {
		return PJ_SUCCESS;
	} else if(cap == PJMEDIA_VID_DEV_CAP_INPUT_PREVIEW){
		return PJ_SUCCESS;
	} else if(cap == PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW){

		return PJ_SUCCESS;
	}

	return PJMEDIA_EVID_INVCAP;
}


/* API: Start stream. */
static pj_status_t cap_screen_stream_start(pjmedia_vid_dev_stream *strm) {
	struct cap_screen_stream *stream = (struct cap_screen_stream*) strm;


	stream->buffer = pj_pool_alloc(stream->pool,
			stream->xres * stream->yres * stream->fmt_bps);

	stream->capturing = PJ_TRUE;

	return PJ_SUCCESS;

}

unsigned int create_bitmask(struct fb_bitfield* bf) {
	return ~(~0u << bf->length) << bf->offset;
}

// Unifies the picture's pixel format to be 32-bit ARGB
void unify(const __u32* in, __u32* out, struct cap_screen_stream *stream) {

	__u32 red_mask, green_mask, blue_mask;
	__u32 c;
	__u32 r, g, b;
	int i, j = 0, bytes_pp;

	// build masks for extracting colour bits
	red_mask = create_bitmask(&stream->fb_varinfo.red);
	green_mask = create_bitmask(&stream->fb_varinfo.green);
	blue_mask = create_bitmask(&stream->fb_varinfo.blue);

	// go through the image and put the bits in place
	bytes_pp = stream->bps >> 3;
	for (i = 0; i < stream->xres * stream->yres * bytes_pp; i += bytes_pp) {
		//PJ_LOG(4, (THIS_FILE, "Walking on %d" , i));
		//memcpy(((char*) &c) + (sizeof(__u32) - bytes_pp), in + i, bytes_pp);
		c = *(in + (i / 4));

		// get the colors
		r = ((c & red_mask) >> stream->fb_varinfo.red.offset) & ~(~0u << stream->fb_varinfo.red.length);
		g = ((c & green_mask) >> stream->fb_varinfo.green.offset) & ~(~0u << stream->fb_varinfo.green.length);
		b = ((c & blue_mask) >> stream->fb_varinfo.blue.offset) & ~(~0u << stream->fb_varinfo.blue.length);

		// format the new pixel
		out[j++] = (0xFF << 24) | (b << 16) | (g << 8) | r;
	}

}


static pj_status_t cap_screen_stream_get_frame(pjmedia_vid_dev_stream *strm, pjmedia_frame *frame){

	struct cap_screen_stream *stream = (struct cap_screen_stream*)strm;
	int been_read = 0;

	frame->type = PJMEDIA_FRAME_TYPE_VIDEO;
	frame->bit_info = 0;
    frame->timestamp = stream->ts;
    stream->ts.u64 += stream->ts_inc;

	int fd = open(ANDROID_FB_DEVICE, O_RDONLY);
	if(fd > 0){
		been_read = read(fd, stream->buffer, (stream->xres * stream->yres) * stream->fmt_bps);
		//PJ_LOG(4, (THIS_FILE, "Has read %d", been_read));
		unify((__u32*)stream->buffer, (__u32*)frame->buf, stream);
	}

	return PJ_SUCCESS;
}

/* API: Stop stream. */
static pj_status_t cap_screen_stream_stop(pjmedia_vid_dev_stream *strm) {
	struct cap_screen_stream *stream = (struct cap_screen_stream*) strm;
	if(stream->fd_fb > 0){

		close(stream->fd_fb);
	}
	stream->capturing = PJ_FALSE;
	return PJ_SUCCESS;
}

/* API: Destroy stream. */
static pj_status_t cap_screen_stream_destroy(pjmedia_vid_dev_stream *strm) {
	struct cap_screen_stream *stream = (struct cap_screen_stream*) strm;

	PJ_ASSERT_RETURN(stream != NULL, PJ_EINVAL);

	PJ_LOG(4, (THIS_FILE, "Destroy webrtc capture"));
	if(stream->capturing){
		cap_screen_stream_stop(strm);
	}

	pj_pool_release(stream->pool);

	return PJ_SUCCESS;
}

