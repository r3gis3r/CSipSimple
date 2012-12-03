
/* File : pjsua.i */
%module (directors="1") pjsua


// Do not generate the default proxy constructor or destructor
%nodefaultctor pjmedia_port;
%nodefaultdtor pjmedia_port;

// Add in pure Java code proxy constructor
%typemap(javacode) pjmedia_port %{
  /** This constructor creates the proxy which initially does not create nor own any C memory */
  public pjmedia_port() {
    this(0, false);
  }
%}

// Type typemaps for marshalling pjmedia_port **
%typemap(jni) pjmedia_port **pp_port "jobject"
%typemap(jtype) pjmedia_port **pp_port "pjmedia_port"
%typemap(jstype) pjmedia_port **pp_port "pjmedia_port"

// Typemaps for pjmedia_port ** as a parameter output type
%typemap(in) pjmedia_port **pp_port (pjmedia_port *ppMediaPort = 0) %{
  $1 = &ppMediaPort;
%}
%typemap(argout) pjmedia_port **pp_port {
  // Give Java proxy the C pointer (of newly created object)
  jclass clazz = jenv->FindClass("org/pjsip/pjsua/pjmedia_port");
  jfieldID fid = jenv->GetFieldID(clazz, "swigCPtr", "J");
  jlong cPtr = 0;
  *(pjmedia_port **)&cPtr = *$1;
  jenv->SetLongField($input, fid, cPtr);
}
%typemap(javain) pjmedia_port **pp_port "$javainput"

/* Arguments like 'pjsua_acc_id *p_acc_id' should be considered output args */
%apply pjsua_acc_id *OUTPUT { pjsua_acc_id *p_acc_id };
%apply pjsua_call_id *OUTPUT { pjsua_call_id *p_call_id };
%apply pjsua_transport_id *OUTPUT { pjsua_transport_id *p_id };
%apply pjsua_recorder_id *OUTPUT { pjsua_recorder_id *p_id };
%apply pjsua_player_id *OUTPUT { pjsua_player_id *p_id };
%apply pjsua_acc_id *OUTPUT { pjsua_buddy_id *p_buddy_id };
%apply unsigned *INOUT { unsigned *count };
%apply int *OUTPUT { int *capture_dev };
%apply int *OUTPUT { int *playback_dev };
%apply pjsua_conf_port_id *OUTPUT { pjsua_conf_port_id *p_id };
%apply unsigned *OUTPUT { unsigned *tx_level };
%apply unsigned *OUTPUT { unsigned *rx_level };
//%pp_out(pjmedia_port)
/* We need to be able to pass arrays of pjmedia_tone_desc to pjmedia */
/* The array elements are passed by value (copied) */
JAVA_ARRAYSOFCLASSES(pjmedia_tone_desc)
%apply pjmedia_tone_desc[] {const pjmedia_tone_desc tones[]};
JAVA_ARRAYSOFCLASSES(pjmedia_tone_digit)
%apply pjmedia_tone_digit[] {const pjmedia_tone_digit tones[]};
JAVA_ARRAYSOFCLASSES(pj_str_t)
%apply pj_str_t[]{pj_str_t nameserver[4]};
JAVA_ARRAYSOFCLASSES(dynamic_factory)
%apply dynamic_factory[]{dynamic_factory extra_codecs[64]};

/* Nested struct silence -- Being aware this are not retrieved */
%nestedworkaround pjmedia_port::port_data;
%nestedworkaround pjsip_cred_info::ext;
%nestedworkaround pjsip_event::body;

%header %{

#include <pjsua-lib/pjsua.h>
#include "pj_callback.h"

extern struct pjsua_callback wrapper_callback_struct;

%}

%inline %{
pj_str_t pj_str_copy(const char *str) {
	size_t length = strlen(str) + 1;
	char* copy = (char*)calloc(length, sizeof(char));
	copy = strncpy(copy, str, length);
	return pj_str(copy);
}

%}
/* turn on director wrapping Callback */
%feature("director") Callback;

%constant struct pjsua_callback* WRAPPER_CALLBACK_STRUCT = &wrapper_callback_struct;
%include pj_callback.h

#define PJ_DECL(type) extern type

// The public API does not use lists, therefore we define it to nothing
// From pjlib/include/pj/list.h
#define PJ_DECL_LIST_MEMBER(type)

