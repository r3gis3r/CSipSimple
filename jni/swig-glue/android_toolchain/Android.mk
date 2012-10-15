LOCAL_PATH := $(call my-dir)/..

SWIG_GLUE_PATH := $(LOCAL_PATH)
SWIG_GLUE_NATIVE_PATH := $(SWIG_GLUE_PATH)/nativesrc
SWIG_GLUE_NATIVE_FILE := pjsua_wrap.cpp
JAVA_MODULE := pjsua
JAVA_PACKAGE := org.pjsip.pjsua
JAVA_PACKAGE_DIR := src/$(subst .,/,$(JAVA_PACKAGE))

PJ_ROOT_DIR := $(SWIG_GLUE_PATH)/../pjsip/sources
CSS_WRAPPER_ROOT_DIR := $(SWIG_GLUE_PATH)/../csipsimple-wrapper

# For pjsua
PJ_SWIG_HEADERS := $(PJ_ROOT_DIR)/pjsip/include/pjsua-lib/pjsua.h 
# For CSipSimple
PJ_SWIG_HEADERS += $(CSS_WRAPPER_ROOT_DIR)/include/pjsua_jni_addons.h \
	$(CSS_WRAPPER_ROOT_DIR)/include/zrtp_android.h \
	$(CSS_WRAPPER_ROOT_DIR)/include/csipsimple_codecs_utils.h  \
	$(CSS_WRAPPER_ROOT_DIR)/include/call_recorder.h
	
SWIG_PYTHON_TOOLS := $(SWIG_GLUE_PATH)/clean_source_for_android.py \
					$(SWIG_GLUE_PATH)/clean_callback_for_android.py \
					$(SWIG_GLUE_PATH)/JavaJNI2CJNI_Load.py

CONCAT_PJSUA_FILE := $(SWIG_GLUE_PATH)/.$(JAVA_MODULE).i

INTERFACES_FILES := $(SWIG_GLUE_PATH)/generic_java.i \
					$(SWIG_GLUE_PATH)/pjsua_header.i \
					$(SWIG_GLUE_PATH)/pjsip_header.i \
					$(CONCAT_PJSUA_FILE)
					

CONCAT_INTERFACE_FILE := $(SWIG_GLUE_PATH)/.interface.i


# Swig generation target
$(SWIG_GLUE_NATIVE_PATH)/$(SWIG_GLUE_NATIVE_FILE) :: $(CONCAT_INTERFACE_FILE) $(SWIG_PYTHON_TOOLS) $(SWIG_GLUE_PATH)/pj_loader.c.template
	@mkdir -p $(SWIG_GLUE_NATIVE_PATH)
	@$(RM) -r $(JAVA_PACKAGE_DIR)
	mkdir -p $(JAVA_PACKAGE_DIR)
	$(SWIG) $(MY_PJSIP_FLAGS) \
		-o $@ \
		-outdir $(JAVA_PACKAGE_DIR) -java -package $(JAVA_PACKAGE) \
		-c++ $(CONCAT_INTERFACE_FILE)
	# Clean source for android target
	@$(PYTHON) $(SWIG_GLUE_PATH)/clean_source_for_android.py $@ > $@.tmp
	@mv $@.tmp $@
	@for callbackFile in $(JAVA_PACKAGE_DIR)/*Callback.java; do \
		$(PYTHON) $(SWIG_GLUE_PATH)/clean_callback_for_android.py $$callbackFile > $$callbackFile.tmp; \
		mv $$callbackFile.tmp $$callbackFile; \
	done;
	# Add pjloader
	@$(PYTHON) $(SWIG_GLUE_PATH)/JavaJNI2CJNI_Load.py \
		-i $(JAVA_PACKAGE_DIR)/$(JAVA_MODULE)JNI.java \
		-o $(SWIG_GLUE_PATH)/.pj_loader.c \
		-t $(SWIG_GLUE_PATH)/pj_loader.c.template \
		-m $(JAVA_MODULE) \
		-p $(JAVA_PACKAGE)
	@cat $(SWIG_GLUE_PATH)/.pj_loader.c >> $@
	@$(RM) $(SWIG_GLUE_PATH)/.pj_loader.c

# Intemediate swig i files
.INTERMEDIATE: $(CONCAT_INTERFACE_FILE) $(CONCAT_PJSUA_FILE)

$(CONCAT_INTERFACE_FILE) :: $(INTERFACES_FILES) $(SWIG_HEADERS) $(CONCAT_PJSUA_FILE)
	cat $(filter %.i, $^) > $@

$(CONCAT_PJSUA_FILE) :: $(PJ_SWIG_HEADERS) $(SWIG_GLUE_PATH)/clean_header_for_swig.py
	echo > $@
	for f in $(filter %.h, $^); \
		do $(PYTHON) $(SWIG_GLUE_PATH)/clean_header_for_swig.py $$f >> $@;\
	done


# Clean target addition
privatecleantarget := clean-swig-glue-$(TARGET_ARCH_ABI)
.PHONY: $(privatecleantarget)
$(privatecleantarget)::
	@$(RM) -rf $(JAVA_PACKAGE_DIR)
	@$(RM) -rf $(SWIG_GLUE_NATIVE_PATH)


# The swig-glue module
include $(CLEAR_VARS)
LOCAL_PATH := $(SWIG_GLUE_PATH)
LOCAL_MODULE    := swig-glue
LOCAL_CFLAGS := $(MY_PJSIP_FLAGS) -fno-strict-aliasing

PJ_ANDROID_ROOT_DIR := $(LOCAL_PATH)/../pjsip/android_sources

# Include PJ interfaces
LOCAL_C_INCLUDES += $(PJ_ROOT_DIR)/pjsip/include $(PJ_ROOT_DIR)/pjlib-util/include/ \
	$(PJ_ROOT_DIR)/pjlib/include/ $(PJ_ROOT_DIR)/pjmedia/include \
	$(PJ_ROOT_DIR)/pjnath/include $(PJ_ROOT_DIR)/pjlib/include 
# Include PJ_android interfaces
LOCAL_C_INCLUDES += $(PJ_ANDROID_ROOT_DIR)/pjmedia/include/pjmedia-audiodev \
	$(PJ_ANDROID_ROOT_DIR)/pjmedia/include/pjmedia-videodev

# Include CSipSimple interface
LOCAL_C_INCLUDES += $(CSS_WRAPPER_ROOT_DIR)/include

# Self interface
LOCAL_C_INCLUDES += $(LOCAL_PATH)

LOCAL_SRC_FILES := nativesrc/$(SWIG_GLUE_NATIVE_FILE) pj_callback.cpp

include $(BUILD_STATIC_LIBRARY)

