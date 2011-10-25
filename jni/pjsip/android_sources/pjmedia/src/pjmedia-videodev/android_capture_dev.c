/* $Id: v4l2_dev.c 3459 2011-03-17 11:25:19Z bennylp $ */
/*
 * Copyright (C) 2008-2010 Teluu Inc. (http://www.teluu.com)
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
#include <pjmedia-videodev/videodev_imp.h>
#include <pjmedia/errno.h>
#include <pj/assert.h>
#include <pj/errno.h>
#include <pj/file_access.h>
#include <pj/log.h>
#include <pj/os.h>
#include <pj/rand.h>

#if PJMEDIA_VIDEO_DEV_HAS_ANDROID_CAPTURE

#include <jni.h>



#define THIS_FILE		"android_capture_dev.c"
#define DRIVER_NAME		"acapture"
#define DEFAULT_WIDTH		352//320
#define DEFAULT_HEIGHT		288//240
#define DEFAULT_FPS		15
#define DEFAULT_CLOCK_RATE	30000



/* android device info */
typedef struct android_cam_dev_info
{
    pjmedia_vid_dev_info	 info;
    char			 dev_name[32];
} android_cam_dev_info;

/* android factory */
typedef struct android_cam_factory
{
    pjmedia_vid_dev_factory	 base;
    pj_pool_t			*pool;
    pj_pool_factory		*pf;

    unsigned			 dev_count;
    android_cam_dev_info		*dev_info;
} android_cam_factory;

/* Video stream. */
typedef struct android_cam_stream
{
    pjmedia_vid_dev_stream	 base;		/**< Base stream	*/
    pjmedia_vid_param	 	 param;		/**< Settings		*/
    pj_pool_t           	*pool;		/**< Memory pool.	*/

    char			 name[64];	/**< Name for log	*/

    //Buffer
    pjmedia_video_apply_fmt_param    vafp;
    void* imageData;

    pj_mutex_t* frame_mutex;

    pj_time_val			 start_time;	/**< Time when started	*/

    void                	*user_data;	/**< Application data 	*/

    pj_timestamp	 frame_ts;
    unsigned int ts_inc;

} android_cam_stream;




//TODO : should be get and retrieve from current context
static android_cam_stream *current_capture_stream = NULL;

/* Prototypes */
static pj_status_t android_cam_factory_init(pjmedia_vid_dev_factory *f);
static pj_status_t android_cam_factory_destroy(pjmedia_vid_dev_factory *f);
static unsigned    android_cam_factory_get_dev_count(pjmedia_vid_dev_factory *f);
static pj_status_t android_cam_factory_get_dev_info(pjmedia_vid_dev_factory *f,
					        unsigned index,
					        pjmedia_vid_dev_info *info);
static pj_status_t android_cam_factory_default_param(pj_pool_t *pool,
                                                 pjmedia_vid_dev_factory *f,
					         unsigned index,
					         pjmedia_vid_param *param);
static pj_status_t android_cam_factory_create_stream(pjmedia_vid_dev_factory *f,
						 	 pjmedia_vid_param *prm,
					         const pjmedia_vid_cb *cb,
					         void *user_data,
					         pjmedia_vid_dev_stream **p);

static pj_status_t android_cam_stream_get_param(pjmedia_vid_dev_stream *strm,
					    pjmedia_vid_param *param);
static pj_status_t android_cam_stream_get_cap(pjmedia_vid_dev_stream *strm,
				          pjmedia_vid_dev_cap cap,
				          void *value);
static pj_status_t android_cam_stream_set_cap(pjmedia_vid_dev_stream *strm,
				          pjmedia_vid_dev_cap cap,
				          const void *value);
static pj_status_t android_cam_stream_get_frame(pjmedia_vid_dev_stream *strm,
                                            pjmedia_frame *frame);
static pj_status_t android_cam_stream_start(pjmedia_vid_dev_stream *strm);
static pj_status_t android_cam_stream_stop(pjmedia_vid_dev_stream *strm);
static pj_status_t android_cam_stream_destroy(pjmedia_vid_dev_stream *strm);

/* Operations */
static pjmedia_vid_dev_factory_op factory_op =
{
    &android_cam_factory_init,
    &android_cam_factory_destroy,
    &android_cam_factory_get_dev_count,
    &android_cam_factory_get_dev_info,
    &android_cam_factory_default_param,
    &android_cam_factory_create_stream
};


static pjmedia_vid_dev_stream_op stream_op =
{
    &android_cam_stream_get_param,
    &android_cam_stream_get_cap,
    &android_cam_stream_set_cap,
    &android_cam_stream_start,
    &android_cam_stream_get_frame,
    NULL,
    &android_cam_stream_stop,
    &android_cam_stream_destroy
};



/****************************************************************************
 * Factory operations
 */
/*
 * Factory creation function.
 */
pjmedia_vid_dev_factory* pjmedia_android_cam_factory(pj_pool_factory *pf)
{
    android_cam_factory *f;
    pj_pool_t *pool;


	PJ_LOG(4, (THIS_FILE, "Android cam factory"));

    pool = pj_pool_create(pf, DRIVER_NAME, 512, 512, NULL);
    f = PJ_POOL_ZALLOC_T(pool, android_cam_factory);
    f->pf = pf;
    f->pool = pool;
    f->base.op = &factory_op;

    return &f->base;
}


/* API: init factory */
static pj_status_t android_cam_factory_init(pjmedia_vid_dev_factory *f) {
	android_cam_factory *cf = (android_cam_factory*) f;
	pj_status_t status;

	struct android_cam_dev_info *qdi;
	unsigned i, l;

	/* Initialize input devices here */
	cf->dev_info = (android_cam_dev_info*) pj_pool_calloc(cf->pool, 1,
			sizeof(android_cam_dev_info));

	cf->dev_count = 1;

	PJ_LOG(4, (THIS_FILE, "Android capture init..."));

    //TODO : implement using jni
	qdi = &cf->dev_info[0];
	pj_bzero(qdi, sizeof(*qdi));
	strcpy(qdi->info.name, "Android camera");
	strcpy(qdi->info.driver, "Android");
	qdi->info.dir = PJMEDIA_DIR_CAPTURE;
	qdi->info.has_callback = PJ_FALSE;
	qdi->info.caps = PJMEDIA_VID_DEV_CAP_FORMAT;

	qdi->info.fmt_cnt = 1;
	qdi->info.fmt = (pjmedia_format*)  pj_pool_calloc(cf->pool, qdi->info.fmt_cnt,
 				   sizeof(pjmedia_format));
    for (i = 0; i < qdi->info.fmt_cnt; i++) {
        pjmedia_format *fmt = &qdi->info.fmt[i];

        pjmedia_format_init_video(fmt, PJMEDIA_FORMAT_NV21,
				  DEFAULT_WIDTH, DEFAULT_HEIGHT,
				  DEFAULT_FPS, 1);


    }

	PJ_LOG(4, (THIS_FILE, "Android video initialized with %d devices",
					cf->dev_count));

	return PJ_SUCCESS;
}

/* API: destroy factory */
static pj_status_t android_cam_factory_destroy(pjmedia_vid_dev_factory *f)
{
    android_cam_factory *cf = (android_cam_factory*)f;
    pj_pool_t *pool = cf->pool;

    if (cf->pool) {
	cf->pool = NULL;
	pj_pool_release(pool);
    }

    return PJ_SUCCESS;
}

/* API: get number of devices */
static unsigned android_cam_factory_get_dev_count(pjmedia_vid_dev_factory *f)
{
    android_cam_factory *cf = (android_cam_factory*)f;
    return cf->dev_count;
}

/* API: get device info */
static pj_status_t android_cam_factory_get_dev_info(pjmedia_vid_dev_factory *f,
					     unsigned index,
					     pjmedia_vid_dev_info *info)
{
    android_cam_factory *cf = (android_cam_factory*)f;

    PJ_ASSERT_RETURN(index < cf->dev_count, PJMEDIA_EVID_INVDEV);

    pj_memcpy(info, &cf->dev_info[index].info, sizeof(*info));

    return PJ_SUCCESS;
}

/* API: create default device parameter */
static pj_status_t android_cam_factory_default_param(pj_pool_t *pool,
                                                 pjmedia_vid_dev_factory *f,
                                                 unsigned index,
                                                 pjmedia_vid_param *param)
{
    android_cam_factory *cf = (android_cam_factory*)f;

    PJ_ASSERT_RETURN(index < cf->dev_count, PJMEDIA_EVID_INVDEV);

    pj_bzero(param, sizeof(*param));
    param->dir = PJMEDIA_DIR_CAPTURE;
    param->cap_id = index;
    param->rend_id = PJMEDIA_VID_INVALID_DEV;
    param->flags = PJMEDIA_VID_DEV_CAP_FORMAT;
    param->clock_rate = DEFAULT_CLOCK_RATE;


	PJ_LOG(4, (THIS_FILE, "getting default params %d", cf->dev_info[index].info.fmt[0].id));

    pjmedia_format_copy(&param->fmt, &cf->dev_info[index].info.fmt[0]);

    return PJ_SUCCESS;
}

/* API: create stream */
static pj_status_t android_cam_factory_create_stream(pjmedia_vid_dev_factory *f,
				      pjmedia_vid_param *param,
				      const pjmedia_vid_cb *cb,
				      void *user_data,
				      pjmedia_vid_dev_stream **p_vid_strm)
{
    android_cam_factory *cf = (android_cam_factory*)f;
    pj_pool_t *pool;
    android_cam_stream *stream;
    android_cam_dev_info *vdi;
    pjmedia_video_apply_fmt_param vafp;
    const pjmedia_video_format_detail *vfd;
    const pjmedia_video_format_info *fmt_info;
    pj_status_t status = PJ_SUCCESS;



    PJ_ASSERT_RETURN(f && param && p_vid_strm, PJ_EINVAL);
    PJ_ASSERT_RETURN(param->fmt.type == PJMEDIA_TYPE_VIDEO &&
		     param->fmt.detail_type == PJMEDIA_FORMAT_DETAIL_VIDEO,
		     PJ_EINVAL);
    PJ_ASSERT_RETURN(param->cap_id >= 0 && param->cap_id < cf->dev_count,
		     PJMEDIA_EVID_INVDEV);


    pj_bzero(&vafp, sizeof(vafp));


    fmt_info = pjmedia_get_video_format_info(NULL, param->fmt.id);

    vafp.size = param->fmt.det.vid.size;
    if (fmt_info->apply_fmt(fmt_info, &vafp) != PJ_SUCCESS){
        return PJMEDIA_EVID_BADFORMAT;
    }
    vdi = &cf->dev_info[param->cap_id];
    vfd = pjmedia_format_get_video_format_detail(&param->fmt, PJ_TRUE);


    /* Create and Initialize stream descriptor */
    pool = pj_pool_create(cf->pf, vdi->info.name, 512, 512, NULL);
    PJ_ASSERT_RETURN(pool != NULL, PJ_ENOMEM);

    stream = PJ_POOL_ZALLOC_T(pool, android_cam_stream);
    pj_memcpy(&stream->param, param, sizeof(*param));
    stream->pool = pool;
    strncpy(stream->name, vdi->info.name, sizeof(stream->name));
    stream->name[sizeof(stream->name)-1] = '\0';
    stream->user_data = user_data;
    pj_memcpy(&stream->vafp, &vafp, sizeof(vafp));

    stream->ts_inc = PJMEDIA_SPF2(param->clock_rate, &vfd->fps, 1);

    PJ_LOG(4,(THIS_FILE, "Try to android device %s: format=%s.. %d x %d",
	      stream->name, fmt_info->name, vfd->size.w, vfd->size.h));

    //Ensure we agree with android supported frame size -- for now do it directly like that but should be changed
    PJ_ASSERT_RETURN(vfd->size.w == DEFAULT_WIDTH && vfd->size.h == DEFAULT_HEIGHT, PJ_EINVAL);

    stream->imageData = pj_pool_alloc(cf->pool, vafp.framebytes);
	pj_bzero(stream->imageData, vafp.framebytes);
	pj_mutex_create_simple(stream->pool, "android-cam", &stream->frame_mutex);


    PJ_LOG(4,(THIS_FILE, "Opening android device %s: format=%s..",
	      stream->name, fmt_info->name));



    /* Done */
    stream->base.op = &stream_op;
    *p_vid_strm = &stream->base;

    return PJ_SUCCESS;

on_error:

    android_cam_stream_destroy(&stream->base);
    return status;
}

/* API: Get stream info. */
static pj_status_t android_cam_stream_get_param(pjmedia_vid_dev_stream *s,
					    pjmedia_vid_param *pi)
{
    android_cam_stream *strm = (android_cam_stream*)s;

    PJ_ASSERT_RETURN(strm && pi, PJ_EINVAL);

    pj_memcpy(pi, &strm->param, sizeof(*pi));

    return PJ_SUCCESS;
}

/* API: get capability */
static pj_status_t android_cam_stream_get_cap(pjmedia_vid_dev_stream *s,
                                          pjmedia_vid_dev_cap cap,
                                          void *pval)
{
    android_cam_stream *strm = (android_cam_stream*)s;

    PJ_UNUSED_ARG(strm);

    PJ_ASSERT_RETURN(s && pval, PJ_EINVAL);

    if (cap==PJMEDIA_VID_DEV_CAP_INPUT_SCALE)
    {
        return PJMEDIA_EVID_INVCAP;
//	return PJ_SUCCESS;
    } else {
	return PJMEDIA_EVID_INVCAP;
    }
}

/* API: set capability */
static pj_status_t android_cam_stream_set_cap(pjmedia_vid_dev_stream *s,
                                          pjmedia_vid_dev_cap cap,
                                          const void *pval)
{
    android_cam_stream *strm = (android_cam_stream*)s;


    PJ_ASSERT_RETURN(s && pval, PJ_EINVAL);

    /*
    if (cap==PJMEDIA_VID_DEV_CAP_INPUT_SCALE)
    {
	return PJ_SUCCESS;
    }
    */
    PJ_UNUSED_ARG(strm);
    PJ_UNUSED_ARG(cap);
    PJ_UNUSED_ARG(pval);

    return PJMEDIA_EVID_INVCAP;
}


/* API: Get frame from stream */
static pj_status_t android_cam_stream_get_frame(pjmedia_vid_dev_stream *strm,
                                            pjmedia_frame *frame)
{
    android_cam_stream *stream = (android_cam_stream*)strm;

    frame->type = PJMEDIA_FRAME_TYPE_VIDEO;

    PJ_LOG(5, (THIS_FILE, "Ask for frame size : %d -- we have %d", frame->size, stream->vafp.framebytes));

    PJ_ASSERT_RETURN(frame->size == stream->vafp.framebytes, PJ_ENOMEM);

    frame->timestamp.u64 = stream->frame_ts.u64;
    stream->frame_ts.u64 += stream->ts_inc;


	pj_mutex_lock(stream->frame_mutex);
    pj_memcpy(frame->buf, stream->imageData, stream->vafp.framebytes);
	pj_mutex_unlock(stream->frame_mutex);


    return PJ_SUCCESS;
}

/* API: Start stream. */
static pj_status_t android_cam_stream_start(pjmedia_vid_dev_stream *strm)
{
    android_cam_stream *stream = (android_cam_stream*)strm;
    unsigned i;
    pj_status_t status;

    PJ_LOG(4, (THIS_FILE, "Starting android camera stream %s", stream->name));

    current_capture_stream = stream;

    pj_gettimeofday(&stream->start_time);



    return PJ_SUCCESS;

on_error:
    return status;
}

/* API: Stop stream. */
static pj_status_t android_cam_stream_stop(pjmedia_vid_dev_stream *strm)
{
    android_cam_stream *stream = (android_cam_stream*)strm;
    pj_status_t status;
    PJ_LOG(4, (THIS_FILE, "Stopping android camera stream %s", stream->name));

    pj_mutex_lock(stream->frame_mutex);
    current_capture_stream = NULL;
    pj_mutex_unlock(stream->frame_mutex);

	return PJ_SUCCESS;


}


/* API: Destroy stream. */
static pj_status_t android_cam_stream_destroy(pjmedia_vid_dev_stream *strm)
{
    android_cam_stream *stream = (android_cam_stream*)strm;
    unsigned i;

    PJ_ASSERT_RETURN(stream != NULL, PJ_EINVAL);

    android_cam_stream_stop(strm);

    PJ_LOG(4, (THIS_FILE, "Destroying android camera stream %s", stream->name));

    pj_pool_release(stream->pool);

    return PJ_SUCCESS;
}


/*
 * comes from the java stack
 */


JNIEXPORT void JNICALL Java_com_csipsimple_ui_camera_VideoProducer_pushToNative
  (JNIEnv* env, jobject object, jbyteArray pinArray, jint size) {

    	PJ_LOG(5, (THIS_FILE, "Cb from java << "));

        jbyte *inArray;
        //Do not copy since copy is done in current stream
        inArray = (*env)->GetByteArrayElements(env, pinArray, JNI_FALSE);

    	if( current_capture_stream != NULL ){
    		pj_mutex_lock(current_capture_stream->frame_mutex);
    		//PJ_LOG(5, (THIS_FILE, "We have a stream here push data into it : size %d vs %d", current_capture_stream->vafp.framebytes, size));
    		pj_memcpy(current_capture_stream->imageData, inArray, current_capture_stream->vafp.framebytes);
    		pj_mutex_unlock(current_capture_stream->frame_mutex);
    	}

        //release arrays:
        (*env)->ReleaseByteArrayElements(env, pinArray, inArray, 0);
}



#endif	/* PJMEDIA_VIDEO_DEV_HAS_ANDROID_CAPTURE */
