
%include "typemaps.i"
%include "enums.swg"
%include "arrays_java.i";
%include "carrays.i";

/* void* shall be handled as byte arrays */
%typemap(jni) void * "void *"
%typemap(jtype) void * "byte[]"
%typemap(jstype) void * "byte[]"
%typemap(javain) void * "$javainput"
%typemap(in) void * %{
	$1 = $input;
%}
%typemap(javadirectorin) void * "$jniinput"
%typemap(out) void * %{ 
	$result = $1; 
%}
%typemap(javaout) void * {
	return $jnicall;
}

