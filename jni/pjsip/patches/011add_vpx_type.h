Index: pjsip/sources/pjmedia/include/pjmedia-codec/types.h
===================================================================
--- pjsip.orig/sources/pjmedia/include/pjmedia-codec/types.h	2012-11-17 16:21:58.076972442 +0100
+++ pjsip/sources/pjmedia/include/pjmedia-codec/types.h	2012-11-24 23:55:19.661600236 +0100
@@ -72,6 +72,7 @@
     PJMEDIA_RTP_PT_AMRWB,			/**< AMRWB (6.6 - 23.85Kbps)*/
     PJMEDIA_RTP_PT_AMRWBE,			/**< AMRWBE		    */
     PJMEDIA_RTP_PT_OPUS,			/**< OPUS 	*/
+    PJMEDIA_RTP_PT_MPEG4,            /**< mpeg4-generic   */
     PJMEDIA_RTP_PT_G726_16,			/**< G726 @ 16Kbps	    */
     PJMEDIA_RTP_PT_G726_24,			/**< G726 @ 24Kbps	    */
     PJMEDIA_RTP_PT_G726_32,			/**< G726 @ 32Kbps	    */
@@ -125,6 +126,7 @@
      PJMEDIA_RTP_PT_H264_RSV2,
      PJMEDIA_RTP_PT_H264_RSV3,
      PJMEDIA_RTP_PT_H264_RSV4,
+     PJMEDIA_RTP_PT_VP8,
 
      /* Caution!
       * Ensure the value of the last pt above is <= 127.
