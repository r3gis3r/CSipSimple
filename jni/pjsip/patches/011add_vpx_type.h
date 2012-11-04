Index: pjsip/sources/pjmedia/include/pjmedia-codec/types.h
===================================================================
--- pjsip.orig/sources/pjmedia/include/pjmedia-codec/types.h	2012-11-03 16:30:43.531567072 +0100
+++ pjsip/sources/pjmedia/include/pjmedia-codec/types.h	2012-11-03 23:30:18.862389999 +0100
@@ -125,6 +125,7 @@
      PJMEDIA_RTP_PT_H264_RSV2,
      PJMEDIA_RTP_PT_H264_RSV3,
      PJMEDIA_RTP_PT_H264_RSV4,
+     PJMEDIA_RTP_PT_VP8,
 
      /* Caution!
       * Ensure the value of the last pt above is <= 127.
