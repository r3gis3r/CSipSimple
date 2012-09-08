# Java target
JAVA_PROJECT_DIR=../../
JAVA_PACKAGE_DIR=$(JAVA_PROJECT_DIR)/src/org/pjsip/pjsua

# C target
C_TARGET_DIR=nativesrc
C_TARGET_FILE=$(C_TARGET_DIR)/pjsua_wrap.cpp

# To generate with SWIG
PJSIP_DIR=../pjsip/sources
PJSUA_HEADERS=$(PJSIP_DIR)/pjsip/include/pjsua-lib/pjsua.h 
# For CSipSimple
PJSUA_HEADERS+=../csipsimple-wrapper/include/pjsua_jni_addons.h \
	../csipsimple-wrapper/include/zrtp_android.h \
	../csipsimple-wrapper/include/csipsimple_codecs_utils.h  \
	../csipsimple-wrapper/include/call_recorder.h 
CFLAGS=-DPJMEDIA_HAS_SRTP=1 -DPJ_HAS_SSL_SOCK=1 -DPJMEDIA_HAS_ZRTP=1 -DUSE_CSIPSIMPLE=1

# Swig version to use - may be changed according to OS
SWIG=swig2.0
PYTHON=python

all : $(C_TARGET_FILE) $(JAVA_PACKAGE_DIR)

$(C_TARGET_FILE) $(JAVA_PACKAGE_DIR): pjsua.i clean_source_for_android.py clean_callback_for_android.py JavaJNI2CJNI_Load.py
	@mkdir -p $(C_TARGET_DIR)
	@$(RM) -r $(JAVA_PACKAGE_DIR)
	@mkdir -p ${JAVA_PACKAGE_DIR}
	@${SWIG} ${CFLAGS} \
		-o $(C_TARGET_FILE) \
		-outdir $(JAVA_PACKAGE_DIR) -java -package org.pjsip.pjsua \
		-c++ pjsua.i 
	
	
	#Clean source for android target
	@$(PYTHON) clean_source_for_android.py $(C_TARGET_FILE) > $(C_TARGET_FILE).tmp
	@mv $(C_TARGET_FILE).tmp $(C_TARGET_FILE)
	@for callbackFile in $(JAVA_PACKAGE_DIR)/*Callback.java; do \
		$(PYTHON) clean_callback_for_android.py $$callbackFile > $$callbackFile.tmp; \
		mv $$callbackFile.tmp $$callbackFile; \
	done;
	
	
	#Build pj loader
	@$(PYTHON) JavaJNI2CJNI_Load.py -i $(JAVA_PACKAGE_DIR)/pjsuaJNI.java -o $(C_TARGET_DIR)/pj_loader.c -t ./pj_loader.c.template
	@cat $(C_TARGET_DIR)/pj_loader.c >> $(C_TARGET_FILE)
	@$(RM) $(C_TARGET_DIR)/pj_loader.c


pjsua.i : $(PJSUA_HEADERS) pjsua.i_header pjsua.i_footer
	@echo > pjsua.i_body
	@for f in $(PJSUA_HEADERS); do $(PYTHON) ./clean_header_for_swig.py $$f >> pjsua.i_body; done
	@cat pjsua.i_header pjsua.i_body pjsua.i_footer > pjsua.i
	@$(RM) pjsua.i_body || true
	
clean :
	@if [ -f pjsua.i ]; then $(RM) pjsua.i; fi;
	@if [ -d $(C_TARGET_DIR) ]; then $(RM) -r $(C_TARGET_DIR); fi;
	@if [ -d $(JAVA_PACKAGE_DIR) ]; then $(RM) -r $(JAVA_PACKAGE_DIR); fi;
