LOCAL_PATH := $(call my-dir)/../nativesrc

include $(CLEAR_VARS)
LOCAL_MODULE    := swig-glue
LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

PJ_ROOT_DIR := $(LOCAL_PATH)/../../pjsip/sources/
PJ_ANDROID_ROOT_DIR := $(LOCAL_PATH)/../../pjsip/android_sources/

# Include PJ interfaces
LOCAL_C_INCLUDES += $(PJ_ROOT_DIR)/pjsip/include $(PJ_ROOT_DIR)/pjlib-util/include/ \
	$(PJ_ROOT_DIR)/pjlib/include/ $(PJ_ROOT_DIR)/pjmedia/include \
	$(PJ_ROOT_DIR)/pjnath/include $(PJ_ROOT_DIR)/pjlib/include
# Include PJ_android interfaces
LOCAL_C_INCLUDES += $(PJ_ANDROID_ROOT_DIR)/pjmedia/include/pjmedia-audiodev \
	$(PJ_ANDROID_ROOT_DIR)/pjmedia/include/pjmedia-videodev

# Include CSipSimple interface
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../csipsimple-wrapper/include

# Self interface
LOCAL_C_INCLUDES += $(LOCAL_PATH)/ 

LOCAL_SRC_FILES := pjsua_wrap.cpp

include $(BUILD_STATIC_LIBRARY)