#!/usr/bin/python
import re
import sys


def remove_rtti(text):
	return re.sub(r'dynamic_cast<(.* \*)>', r'(\1)', text)

def make_dalvik_compat(text):
	init_text = """/* Utility class for managing the JNI environment */
    class JNIEnvWrapper {
      const Director *director_;
      JNIEnv *jenv_;
    public:
      JNIEnvWrapper(const Director *director) : director_(director), jenv_(0) {
#if defined(SWIG_JAVA_ATTACH_CURRENT_THREAD_AS_DAEMON)
        // Attach a daemon thread to the JVM. Useful when the JVM should not wait for 
        // the thread to exit upon shutdown. Only for jdk-1.4 and later.
        director_->swig_jvm_->AttachCurrentThreadAsDaemon((void **) &jenv_, NULL);
#else
        director_->swig_jvm_->AttachCurrentThread((void **) &jenv_, NULL);
#endif
      }
      ~JNIEnvWrapper() {
#if !defined(SWIG_JAVA_NO_DETACH_CURRENT_THREAD)
        // Some JVMs, eg jdk-1.4.2 and lower on Solaris have a bug and crash with the DetachCurrentThread call.
        // However, without this call, the JVM hangs on exit when the thread was not created by the JVM and creates a memory leak.
        director_->swig_jvm_->DetachCurrentThread();
#endif
      }
      JNIEnv *getJNIEnv() const {
        return jenv_;
      }
    };"""
	final_text = """/* Utility class for managing the JNI environment */
    class JNIEnvWrapper {
      const Director *director_;
      JNIEnv *jenv_;
      int env_status;
      JNIEnv *g_env;
    public:
      JNIEnvWrapper(const Director *director) : director_(director), jenv_(0) {
    	env_status = director_->swig_jvm_->GetEnv( (void **) &g_env, JNI_VERSION_1_6);
#if defined(SWIG_JAVA_ATTACH_CURRENT_THREAD_AS_DAEMON)
        // Attach a daemon thread to the JVM. Useful when the JVM should not wait for
        // the thread to exit upon shutdown. Only for jdk-1.4 and later.
        director_->swig_jvm_->AttachCurrentThreadAsDaemon( &jenv_, NULL);
#else
        director_->swig_jvm_->AttachCurrentThread( &jenv_, NULL);
#endif
      }
      ~JNIEnvWrapper() {
#if !defined(SWIG_JAVA_NO_DETACH_CURRENT_THREAD)
        // Some JVMs, eg jdk-1.4.2 and lower on Solaris have a bug and crash with the DetachCurrentThread call.
        // However, without this call, the JVM hangs on exit when the thread was not created by the JVM and creates a memory leak.

    	 if( env_status == JNI_EDETACHED ){
			  director_->swig_jvm_->DetachCurrentThread();
    	 }

#endif
      }
      JNIEnv *getJNIEnv() const {
        return jenv_;
      }
    };"""
	return text.replace(init_text, final_text)

if __name__ == '__main__':
	filename = sys.argv[1]
	brut_code = open(filename).read()
	code_wo_rtti = remove_rtti(brut_code)
	code_dalvik_compat = make_dalvik_compat(code_wo_rtti)
	print(code_dalvik_compat)
