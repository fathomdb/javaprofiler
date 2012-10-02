#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <signal.h>
#include <sys/time.h>
#include <ucontext.h>

#include <jvmti.h>

#ifndef JVMTI_THREAD_STATE_WAITING_FOR_NOTIFICATION
#define JVMTI_THREAD_STATE_WAITING_FOR_NOTIFICATION (0x80)
#endif

/* ------------------------------------------------------------------- */
/* Some constant maximum sizes */

#define MAX_TOKEN_LENGTH        16
#define MAX_THREAD_NAME_LENGTH  512
#define MAX_METHOD_NAME_LENGTH  1024

jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities capa;

/* Global agent data structure */

typedef struct {
	/* JVMTI Environment */
	jvmtiEnv *jvmti;
	jboolean vm_is_started;
	/* Data access Lock */
	jrawMonitorID lock;
} GlobalAgentData;

static GlobalAgentData *gdata;
JavaVM * globalJvm;

void jvmti_deallocate(void * p) {
	if (p == 0)
		return;

	int err;
	err = jvmti->Deallocate((unsigned char *) p);
	if (err != JVMTI_ERROR_NONE) {
		printf("Error deallocating %d\n", err);
	}
}

/* Every JVMTI interface returns an error code, which should be checked
 *   to avoid any cascading errors down the line.
 *   The interface GetErrorName() returns the actual enumeration constant
 *   name, making the error messages much easier to understand.
 */
static void check_jvmti_error(jvmtiEnv *jvmti, jvmtiError errnum,
		const char *str) {
	if (errnum != JVMTI_ERROR_NONE) {
		char *errnum_str;

		errnum_str = NULL;
		(void) jvmti->GetErrorName(errnum, &errnum_str);

		printf("ERROR: JVMTI: %d(%s): %s\n", errnum,
				(errnum_str == NULL ? "Unknown" : errnum_str),
				(str == NULL ? "" : str));
	}
}

/* Enter a critical section by doing a JVMTI Raw Monitor Enter */
static void enter_critical_section(jvmtiEnv *jvmti) {
	jvmtiError error;

	error = jvmti->RawMonitorEnter(gdata->lock);
	check_jvmti_error(jvmti, error, "Cannot enter with raw monitor");
}

/* Exit a critical section by doing a JVMTI Raw Monitor Exit */
static void exit_critical_section(jvmtiEnv *jvmti) {
	jvmtiError error;

	error = jvmti->RawMonitorExit(gdata->lock);
	check_jvmti_error(jvmti, error, "Cannot exit with raw monitor");
}

void describe(jvmtiError err) {
	jvmtiError err0;
	char *descr;
	err0 = jvmti->GetErrorName(err, &descr);
	if (err0 == JVMTI_ERROR_NONE) {
		printf(descr);
	} else {
		printf("error [%d]", err);
	}
}

extern void onJvmStart();
extern void onJvmStop();

// VM Death callback
static void JNICALL
callbackVMDeath(jvmtiEnv * jvmti_env, JNIEnv * jni_env)
{
	enter_critical_section(jvmti);
	{

		printf("Got VM Death event\n");

	}
	exit_critical_section(jvmti);
	
	onJvmStop();
}


// VM init callback
static void JNICALL
callbackVMInit(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread)
{
//	enter_critical_section(jvmti); {

//		char tname[MAX_THREAD_NAME_LENGTH];
//		static jvmtiEvent events[] = {JVMTI_EVENT_THREAD_START, JVMTI_EVENT_THREAD_END};
//		int i;
//		jvmtiFrameInfo frames[5];
//		jvmtiError err, err1;
//		jvmtiError error;
//		jint count;

	/* The VM has started. */
//		printf("Got VM init event\n");
//		get_thread_name(jvmti_env , thread, tname, sizeof(tname));
//		printf("callbackVMInit:  %s thread\n", tname);
//error = jvmti->SetEventNotificationMode( JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION, (jthread)NULL);
//check_jvmti_error(jvmti_env, error, "Cannot set event notification");
//	}exit_critical_section(jvmti);
	onJvmStart();
}

/* JVMTI callback function. */

void JNICALL
OnClassPrepare(jvmtiEnv *jvmti_env,
		JNIEnv* jni_env,
		jthread thread,
		jclass klass) {
// We need to do this to "prime the pump",
// as it were -- make sure that all of the
// methodIDs have been initialized internally,
// for AsyncGetCallTrace.  I imagine it slows
// down class loading a mite, but honestly,
// how fast does class loading have to be?

//	char *classSig = 0;
//	char *genericSig = 0;

	jvmtiError err;
//	err = jvmti_env->GetClassSignature(klass,
//			&classSig, &genericSig);
//	if (err!= JVMTI_ERROR_NONE ) {
//		printf("OnClassPrepare GetClassSignature error %d\n", err);
//	}
//	else {
//		printf("Class %p %s %s\n", klass, classSig, genericSig ? genericSig :"-");
//	}
//
//	jvmti_deallocate(classSig);
//	jvmti_deallocate(genericSig);

	jint method_count;
	jmethodID *methods;

	err = jvmti_env->GetClassMethods(klass, &method_count, &methods);
	if (err!= JVMTI_ERROR_NONE ) {
		printf("OnClassPrepare error %d\n", err);
	}
//	else {
//		for (int i =0; i< method_count; i++) {
//			jvmtiError err;
//			char *name;
//			char *sig;
//			char *gsig;
//
//			err = jvmti->GetMethodName(methods[i], &name, &sig, &gsig);
//			if (err != JVMTI_ERROR_NONE) {
//				printf("Method err:%d\n", err);
//			}
//			else {
//				printf("Method %p %s %s %s\n", methods[i], name, sig, gsig);
//			}
//
//			jvmti_deallocate(name);
//			jvmti_deallocate(sig);
//			jvmti_deallocate(gsig);
//		}
//	}

	/*err =*/ jvmti->Deallocate((unsigned char*)methods);
}

void JNICALL
OnClassLoad(jvmtiEnv *jvmti_env,
		JNIEnv* jni_env,
		jthread thread,
		jclass klass) {
}

JNIEXPORT jint
JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
	static GlobalAgentData data;
	jvmtiError error;
	jint res;
	jvmtiEventCallbacks callbacks;

	/* Setup initial global agent data area
	 *   Use of static/extern data should be handled carefully here.
	 *   We need to make sure that we are able to cleanup after ourselves
	 *     so anything allocated in this library needs to be freed in
	 *     the Agent_OnUnload() function.
	 */
	(void) memset((void*) &data, 0, sizeof(data));
	gdata = &data;

	/*  We need to first get the jvmtiEnv* or JVMTI environment */

	res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_0);

	if (res != JNI_OK || jvmti == NULL) {
		/* This means that the VM was unable to obtain this version of the
		 *   JVMTI interface, this is a fatal error.
		 */
		printf("ERROR: Unable to access JVMTI Version 1 (0x%x),"
				" is your J2SE a 1.5 or newer version?"
				" JNIEnv's GetEnv() returned %d\n", JVMTI_VERSION_1, res);

	}

	globalJvm = jvm;

	/* Here we save the jvmtiEnv* for Agent_OnUnload(). */
	gdata->jvmti = jvmti;

	(void) memset(&capa, 0, sizeof(jvmtiCapabilities));
//	capa.can_signal_thread = 1;
//	capa.can_get_owned_monitor_info = 1;
//	capa.can_generate_method_entry_events = 1;
//	capa.can_generate_exception_events = 1;
//	capa.can_generate_vm_object_alloc_events = 1;
	capa.can_get_line_numbers = 1;
	capa.can_get_source_debug_extension = 1;
//	capa.can_tag_objects = 1;

	error = jvmti->AddCapabilities(&capa);
	check_jvmti_error(jvmti, error,
			"Unable to get necessary JVMTI capabilities.");

	(void) memset(&callbacks, 0, sizeof(callbacks));
	callbacks.VMInit = &callbackVMInit; /* JVMTI_EVENT_VM_INIT */
	callbacks.VMDeath = &callbackVMDeath; /* JVMTI_EVENT_VM_DEATH */
//callbacks.Exception = &callbackException;/* JVMTI_EVENT_EXCEPTION */
//callbacks.VMObjectAlloc = &callbackVMObjectAlloc;/* JVMTI_EVENT_VM_OBJECT_ALLOC */
	callbacks.ClassPrepare = &OnClassPrepare;
	callbacks.ClassLoad = &OnClassLoad;

	error = jvmti->SetEventCallbacks(&callbacks, (jint) sizeof(callbacks));
	check_jvmti_error(jvmti, error, "Cannot set jvmti callbacks");

	/* At first the only initial events we are interested in are VM
	 *   initialization, VM death, and Class File Loads.
	 *   Once the VM is initialized we will request more events.
	 */
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT,
			(jthread) NULL);
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH,
			(jthread) NULL);
//error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_OBJECT_ALLOC, (jthread) NULL);
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
			JVMTI_EVENT_CLASS_PREPARE, (jthread) NULL);
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
			JVMTI_EVENT_CLASS_LOAD, NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");

	/* Here we create a raw monitor for our use in this agent to
	 *   protect critical sections of code.
	 */
	error = jvmti->CreateRawMonitor("agent data", &(gdata->lock));
	check_jvmti_error(jvmti, error, "Cannot create raw monitor");

	/* We return JNI_OK to signify success */
	return JNI_OK;

}

/* Agent_OnUnload: This is called immediately before the shared library is
 *   unloaded. This is the last code executed.
 */
JNIEXPORT
void JNICALL
Agent_OnUnload(JavaVM * vm)
{
	/* Make sure all malloc/calloc/strdup space is freed */

}

extern "C" {

// Really just an easy way to see if we are loaded
JNIEXPORT jlong JNICALL Java_com_fathomdb_profiler_Profiler_getMagic0(
		JNIEnv *env) {
	jlong magic = 12345678;
	return magic;
}


JNIEXPORT jobjectArray JNICALL Java_com_fathomdb_profiler_Profiler_getMethodInfo0(
		JNIEnv *env, jobject obj, jlong methodId) {
	jobjectArray ret = 0;

	jvmtiError err;
	char *name = 0;
	char *sig = 0;
	char *gsig = 0;
	char *className = 0;

	err = jvmti->GetMethodName((jmethodID) methodId, &name, &sig, &gsig);

	jclass clazz;
	err = jvmti->GetMethodDeclaringClass((jmethodID) methodId, &clazz);
	if (!err) {
		err = jvmti->GetClassSignature(clazz, &className, NULL);
	}

	ret = (jobjectArray) env->NewObjectArray(4,
			env->FindClass("java/lang/String"), env->NewStringUTF(""));

	if (className)
		env->SetObjectArrayElement(ret, 0, env->NewStringUTF(className));
	if (name)
		env->SetObjectArrayElement(ret, 1, env->NewStringUTF(name));
	if (sig)
		env->SetObjectArrayElement(ret, 2, env->NewStringUTF(sig));
	if (gsig)
		env->SetObjectArrayElement(ret, 3, env->NewStringUTF(gsig));

	jvmti_deallocate(className);
	jvmti_deallocate(name);
	jvmti_deallocate(sig);
	jvmti_deallocate(gsig);

	return (ret);
}

JNIEXPORT jlongArray JNICALL Java_com_fathomdb_profiler_Profiler_getLineNumberTable0(
		JNIEnv *env, jobject obj, jlong methodId) {
	jlongArray ret = 0;

	jvmtiError err;

//	typedef struct {
//		jlocation start_location;
//		jint line_number;
//	} jvmtiLineNumberEntry;

	jint entryCount = 0;
	jvmtiLineNumberEntry* tablePtr = 0;
	err = jvmti->GetLineNumberTable((jmethodID) methodId, &entryCount,
			&tablePtr);

	if (!err) {
		ret = env->NewLongArray(entryCount * 2);
		if (!ret) {
			return ret; /* out of memory error thrown */
		}

		// TODO: Use a bigger buffer??
		jlong buf[2];
		for (int i = 0; i < entryCount; i++) {
			buf[0] = tablePtr[i].start_location;
			buf[1] = tablePtr[i].line_number;
			env->SetLongArrayRegion(ret, i * 2, 2, buf);
		}
	}

	jvmti_deallocate(tablePtr);

	return (ret);
}

}
