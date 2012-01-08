/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of pjsip_android.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <jni.h>

#ifndef JVM_WRAPPER_H_
#define JVM_WRAPPER_H_

extern JavaVM *android_jvm;

#ifdef __cplusplus
#define ATTACH_JVM(jni_env)  \
	JNIEnv *g_env;\
	int env_status = android_jvm->GetEnv((void **)&g_env, JNI_VERSION_1_6); \
	jint attachResult = android_jvm->AttachCurrentThread(&jni_env,NULL);

#define DETACH_JVM(jni_env)   if( env_status == JNI_EDETACHED ){ android_jvm->DetachCurrentThread(); }
#else
#define ATTACH_JVM(jni_env)  \
	JNIEnv *g_env;\
	int env_status = (*android_jvm)->GetEnv(android_jvm, (void **)&g_env, JNI_VERSION_1_6); \
	jint attachResult = (*android_jvm)->AttachCurrentThread(android_jvm, &jni_env,NULL);

#define DETACH_JVM(jni_env)   if( env_status == JNI_EDETACHED ){ (*android_jvm)->DetachCurrentThread(android_jvm); }
#endif

#endif /* JVM_WRAPPER_H_ */
