# Java target
JAVA_PROJECT_DIR=${CURDIR}/../../
JAVA_PACKAGE_DIR=${JAVA_PROJECT_DIR}/src/org/pjsip/pjsua

# C target
C_TARGET_DIR=${CURDIR}/nativesrc/

# To generate with SWIG
PJSIP_DIR=${CURDIR}/../pjsip/sources/
PJSUA_HEADERS=${PJSIP_DIR}/pjsip/include/pjsua-lib/pjsua.h 
# For CSipSimple
PJSUA_HEADERS+=${CURDIR}/../csipsimple-wrapper/include/pjsua_jni_addons.h \
	${CURDIR}/../csipsimple-wrapper/include/zrtp_android.h 
#	${PJSIP_DIR}/jni/include/opengl_video_dev.h
LINUX_INCLUDE=-I /usr/lib/jvm/java-6-sun/include -I /usr/lib/jvm/java-6-sun/include/linux -I ${PJSIP_DIR}/pjsip/include -I ${PJSIP_DIR}/jni/include
LINUX_LIBDIR=
CFLAGS=-DPJMEDIA_HAS_SRTP=1 -DPJ_HAS_SSL_SOCK=1 -DPJMEDIA_HAS_ZRTP=1 -DUSE_CSIPSIMPLE=1

# Swig version to use - may be changed according to OS
SWIG=swig2.0

all : ${C_TARGET_DIR}/pjsua_wrap.cpp ${JAVA_PACKAGE_DIR}

${C_TARGET_DIR}/pjsua_wrap.cpp ${JAVA_PACKAGE_DIR}: pjsua.i
	@mkdir -p ${C_TARGET_DIR}
	@rm -rf ${JAVA_PACKAGE_DIR}
	@mkdir -p ${JAVA_PACKAGE_DIR}
	@${SWIG} ${CFLAGS} \
		-o ${C_TARGET_DIR}/pjsua_wrap.cpp \
		-outdir ${JAVA_PACKAGE_DIR} -java -package org.pjsip.pjsua \
		-c++ pjsua.i 
	
	
	#Clean source for android target // TODO : condition
	@./clean_source_for_android.py ${C_TARGET_DIR}/pjsua_wrap.cpp > ${C_TARGET_DIR}/pjsua_wrap.cpp.tmp
	@mv ${C_TARGET_DIR}/pjsua_wrap.cpp.tmp ${C_TARGET_DIR}/pjsua_wrap.cpp
	@./clean_callback_for_android.py ${JAVA_PACKAGE_DIR}/Callback.java > ${JAVA_PACKAGE_DIR}/Callback.java.tmp
	@mv ${JAVA_PACKAGE_DIR}/Callback.java.tmp ${JAVA_PACKAGE_DIR}/Callback.java
	
	
	#Build pj loader
	@./JavaJNI2CJNI_Load.py -i ${JAVA_PACKAGE_DIR}/pjsuaJNI.java -o ${C_TARGET_DIR}/pj_loader.c -t ./pj_loader.c.template
	@cat ${C_TARGET_DIR}/pj_loader.c >> ${C_TARGET_DIR}/pjsua_wrap.cpp
	@rm ${C_TARGET_DIR}/pj_loader.c


pjsua.i : ${PJSUA_HEADERS} pjsua.i_header pjsua.i_footer
	@echo > pjsua.i_body
	@for f in ${PJSUA_HEADERS}; do ./clean_header_for_swig.py $$f >> pjsua.i_body; done
	@cat pjsua.i_header pjsua.i_body pjsua.i_footer > pjsua.i
	@rm pjsua.i_body || true
	
clean :
	@if [ -f pjsua.i ]; then rm pjsua.i; fi;
	@if [ -d $(C_TARGET_DIR) ]; then  rm -r $(C_TARGET_DIR); fi;
	@if [ -d $(JAVA_PACKAGE_DIR) ]; then rm -r $(JAVA_PACKAGE_DIR); fi;
