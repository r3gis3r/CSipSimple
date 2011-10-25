#include <pj/timer.h>
#include <pj/pool.h>
#include <pj/os.h>
#include <pj/string.h>
#include <pj/assert.h>
#include <pj/errno.h>
#include <pj/lock.h>
#include <pj/log.h>
#include <jni.h>
extern JavaVM *android_jvm;

#define ATTACH_JVM(jni_env)  \
	JNIEnv *g_env;\
	int env_status = android_jvm->GetEnv((void **)&g_env, JNI_VERSION_1_6); \
	jint attachResult = android_jvm->AttachCurrentThread(&jni_env,NULL);

#define DETACH_JVM(jni_env)   if( env_status == JNI_EDETACHED ){ android_jvm->DetachCurrentThread(); }



#define HEAP_PARENT(X)	(X == 0 ? 0 : (((X) - 1) / 2))
#define HEAP_LEFT(X)	(((X)+(X))+1)


#define DEFAULT_MAX_TIMED_OUT_PER_POLL  (64)

#define THIS_FILE "timer_android.c"


#define MAX_HEAPS 64
#define MAX_ENTRY_PER_HEAP 512

/**
 * The implementation of timer heap.
 */
struct pj_timer_heap_t
{

	unsigned heap_id;

    /** Pool from which the timer heap resize will get the storage from */
    pj_pool_t *pool;


    /** Max timed out entries to process per poll. */
    unsigned max_entries_per_poll;

    /** Lock object. */
    pj_lock_t *lock;

    /** Autodelete lock. */
    pj_bool_t auto_delete_lock;

    pj_timer_entry* entries[MAX_ENTRY_PER_HEAP];

    /** Callback to be called when a timer expires. */
    pj_timer_heap_callback *callback;

    jmethodID schedule_method;
    jmethodID cancel_method;
};


static pj_timer_heap_t* sHeaps[MAX_HEAPS];
static int sCurrentHeap = 0;

jclass timer_class = 0;

PJ_INLINE(void) lock_timer_heap( pj_timer_heap_t *ht )
{
    if (ht->lock) {
	pj_lock_acquire(ht->lock);
    }
}

PJ_INLINE(void) unlock_timer_heap( pj_timer_heap_t *ht )
{
    if (ht->lock) {
	pj_lock_release(ht->lock);
    }
}


// protected by timer heap lock
static pj_status_t schedule_entry( pj_timer_heap_t *ht,
				   pj_timer_entry *entry, 
				   const pj_time_val *future_time,
				   const pj_time_val *delay) {
	unsigned i = 0;
	unsigned timer_id = -1;
	for(i=0; i<MAX_ENTRY_PER_HEAP; i++){
		if(ht->entries[i] == NULL){
			ht->entries[i] = entry;
			timer_id = i;
			break;
		}
	}

    if (timer_id >= 0) {
		// Obtain the next unique sequence number.
		// Set the entry
		entry->_timer_id = timer_id;
		entry->_timer_value = *future_time;

		pj_uint32_t ft = PJ_TIME_VAL_MSEC(*delay);

		PJ_LOG(5, (THIS_FILE, "Scheduling timer %d in %ld ms", entry->_timer_id, ft));
		/*
		pj_parsed_time pt;
		pj_time_decode(&entry->_timer_value, &pt);
		PJ_LOG(4,(THIS_FILE,
				  "...%04d-%02d-%02d %02d:%02d:%02d.%03d",
				  pt.year, pt.mon, pt.day,
				  pt.hour, pt.min, pt.sec, pt.msec));
		 */

		JNIEnv *jni_env = 0;
		ATTACH_JVM(jni_env);

		jni_env->CallStaticIntMethod(timer_class, ht->schedule_method, (void*)entry, (void*)entry, ft);
		DETACH_JVM(jni_env);

		return 0;
    } else{
    	return PJ_ETOOMANY;
    }
}

// Protected by timer heap lock
static int cancel(pj_timer_heap_t *ht, pj_timer_entry *entry, int dont_call) {

	PJ_CHECK_STACK();

	// Check to see if the timer_id is out of range
	if (entry->_timer_id < 0 || (pj_size_t) entry->_timer_id > MAX_ENTRY_PER_HEAP) {
		PJ_LOG(4, (THIS_FILE, "Ask to cancel something already fired or cancelled : %d", entry->_timer_id));
		return 0;
	}


	PJ_LOG(5, (THIS_FILE, "Cancel timer %d", entry->_timer_id));
	if(ht->entries[entry->_timer_id] == NULL){
		PJ_LOG(1, (THIS_FILE, "Huh, pj is cancelling something already unknown... : %d", entry->_timer_id));
		return 0;
	}

	// Java stuff
	JNIEnv *jni_env = 0;
	ATTACH_JVM(jni_env);
	int cancelCount = jni_env->CallStaticIntMethod(timer_class, ht->cancel_method, (void*)entry, (void*)entry);
	DETACH_JVM(jni_env);

	if(cancelCount > 0){
		// Remove from table
		ht->entries[entry->_timer_id] = NULL;
		entry->_timer_id = -1;
	}

	if (dont_call == 0) {
		// Call the close hook.
		(*ht->callback)(ht, entry);
	}

	return cancelCount;

}


/*
 * Calculate memory size required to create a timer heap.
 */
PJ_DEF(pj_size_t) pj_timer_heap_mem_size(pj_size_t count)
{
    return /* size of the timer heap itself: */
           sizeof(pj_timer_heap_t) + 
           /* size of each entry: */
           (count+2) * (sizeof(pj_timer_entry*)+sizeof(pj_timer_id_t)) +
           /* lock, pool etc: */
           132;
}

/*
 * Create a new timer heap.
 */
PJ_DEF(pj_status_t) pj_timer_heap_create( pj_pool_t *pool,
					  pj_size_t size,
                                          pj_timer_heap_t **p_heap)
{
    pj_timer_heap_t *ht;
    pj_size_t i;

    PJ_ASSERT_RETURN(pool && p_heap, PJ_EINVAL);

    *p_heap = NULL;

    /* Magic? */
    size += 2;

    /* Allocate timer heap data structure from the pool */
    ht = PJ_POOL_ALLOC_T(pool, pj_timer_heap_t);
    if (!ht)
        return PJ_ENOMEM;

    /* Initialize timer heap sizes */
    ht->max_entries_per_poll = DEFAULT_MAX_TIMED_OUT_PER_POLL;
    ht->pool = pool;

    /* Lock. */
    ht->lock = NULL;
    ht->auto_delete_lock = 0;


    for(i = sCurrentHeap; i < MAX_HEAPS; i++){
    	if(sHeaps[i] == NULL){
    		ht->heap_id = i;
    		sHeaps[i] = ht;
    		sCurrentHeap = i;
    		break;
    	}
    }


    pj_bzero(ht->entries, MAX_ENTRY_PER_HEAP * sizeof(pj_timer_entry*));

    PJ_LOG(4, (THIS_FILE, "Will connect JVM"));
    // Init glue to java classes
    JNIEnv *jni_env = 0;
    ATTACH_JVM(jni_env);

	//Get pointer to the java class
    PJ_LOG(4, (THIS_FILE, "JVM CONNECTED %x", jni_env));

    if(timer_class == 0){
    	timer_class = (jclass)/*jni_env->NewGlobalRef(*/jni_env->FindClass("com/csipsimple/utils/TimerWrapper")/*)*/;
    }
	PJ_LOG(4, (THIS_FILE, "Timer class..."));
	if (timer_class == 0) {
		PJ_LOG(1, (THIS_FILE, "Timer class not found"));
		jni_env->ExceptionDescribe();
		jni_env->ExceptionClear();
		DETACH_JVM(jni_env);
		return PJ_ENOMEM;
	}

	PJ_LOG(5, (THIS_FILE, "We have the class for process"));

	//Get the set priority function
	ht->schedule_method = jni_env->GetStaticMethodID(timer_class, "schedule", "(III)I");
	ht->cancel_method = jni_env->GetStaticMethodID(timer_class, "cancel", "(II)I");
	if (ht->schedule_method == 0 || ht->cancel_method == 0 ) {
		PJ_LOG(1, (THIS_FILE, "Method for timer not found"));
		jni_env->ExceptionClear();
		DETACH_JVM(jni_env);
		return PJ_ENOMEM;
	}
	PJ_LOG(5, (THIS_FILE, "We have the method for setThreadPriority"));


	DETACH_JVM(jni_env);


    *p_heap = ht;
    return PJ_SUCCESS;
}

PJ_DEF(void) pj_timer_heap_destroy( pj_timer_heap_t *ht )
{
	int i;
    lock_timer_heap(ht);
	for(i=0; i<MAX_ENTRY_PER_HEAP; i++){
		if(ht->entries[i] != NULL){
			pj_timer_entry *entry = ht->entries[i];
		    cancel(ht, entry, 1);
		}
	}
    unlock_timer_heap(ht);

    if (ht->lock && ht->auto_delete_lock) {
        pj_lock_destroy(ht->lock);
        ht->lock = NULL;
    }
    sCurrentHeap ++;
    sCurrentHeap = sCurrentHeap % MAX_HEAPS;
    sHeaps[ht->heap_id] = NULL;
}

PJ_DEF(void) pj_timer_heap_set_lock(  pj_timer_heap_t *ht,
                                      pj_lock_t *lock,
                                      pj_bool_t auto_del )
{
    if (ht->lock && ht->auto_delete_lock)
        pj_lock_destroy(ht->lock);

    ht->lock = lock;
    ht->auto_delete_lock = auto_del;
}


PJ_DEF(unsigned) pj_timer_heap_set_max_timed_out_per_poll(pj_timer_heap_t *ht,
                                                          unsigned count )
{
    unsigned old_count = ht->max_entries_per_poll;
    ht->max_entries_per_poll = count;
    return old_count;
}

PJ_DEF(pj_timer_entry*) pj_timer_entry_init( pj_timer_entry *entry,
                                             int id,
                                             void *user_data,
                                             pj_timer_heap_callback *cb )
{
    pj_assert(entry && cb);

    entry->_timer_id = -1;
    entry->id = id;
    entry->user_data = user_data;
    entry->cb = cb;

    return entry;
}

PJ_DEF(pj_status_t) pj_timer_heap_schedule( pj_timer_heap_t *ht,
					    pj_timer_entry *entry, 
					    const pj_time_val *delay)
{
    pj_status_t status;
    pj_time_val expires;

    PJ_ASSERT_RETURN(ht && entry && delay, PJ_EINVAL);
    PJ_ASSERT_RETURN(entry->cb != NULL, PJ_EINVAL);

    /* Prevent same entry from being scheduled more than once */
    PJ_ASSERT_RETURN(entry->_timer_id < 1, PJ_EINVALIDOP);

    pj_gettickcount(&expires);
    PJ_TIME_VAL_ADD(expires, *delay);
    
    lock_timer_heap(ht);
    status = schedule_entry(ht, entry, &expires, delay);
    unlock_timer_heap(ht);

    return status;
}

PJ_DEF(int) pj_timer_heap_cancel( pj_timer_heap_t *ht,
				  pj_timer_entry *entry)
{
    int count;

    PJ_ASSERT_RETURN(ht && entry, PJ_EINVAL);

    lock_timer_heap(ht);
    count = cancel(ht, entry, 1);
    unlock_timer_heap(ht);

    return count;
}

PJ_DEF(unsigned) pj_timer_heap_poll( pj_timer_heap_t *ht, 
                                     pj_time_val *next_delay )
{
    /* Polling is not necessary on Android, since all async activities
     * are registered to alarmManager.
     */
    PJ_UNUSED_ARG(ht);
    if (next_delay) {
    	next_delay->sec = 1;
    	next_delay->msec = 0;
    }
    return 0;
}

PJ_DEF(pj_size_t) pj_timer_heap_count( pj_timer_heap_t *ht )
{
    PJ_ASSERT_RETURN(ht, 0);
    unsigned count = 0;
    unsigned i;
    for(i=0; i<MAX_ENTRY_PER_HEAP; i++){
		if(ht->entries[i] != NULL){
			count ++;
		}
	}
    return count;
}

PJ_DEF(pj_status_t) pj_timer_heap_earliest_time( pj_timer_heap_t * ht,
					         pj_time_val *timeval)
{
    /* We don't support this! */
    PJ_UNUSED_ARG(ht);

    timeval->sec = 1;
    timeval->msec = 0;

    return PJ_SUCCESS;
}

PJ_BEGIN_DECL
PJ_DEF(pj_status_t) pj_timer_fire(long entry_ptr){


    pj_thread_desc  a_thread_desc;
    pj_thread_t         *a_thread;
    int j;

    pj_timer_entry *entry = (pj_timer_entry *) entry_ptr;

	if(entry != 0x0){

		if (!pj_thread_is_registered()) {
			char thread_name[160];
			int len = pj_ansi_snprintf(thread_name, sizeof(thread_name),
								 "timer_thread_%d", entry_ptr);
			thread_name[len] = '\0';
			pj_thread_register(thread_name, a_thread_desc, &a_thread);
			PJ_LOG(5, (THIS_FILE, "Registered thread %s", thread_name));
		}




		if(entry->_timer_id != -1){
			// Check that belong to current heap
			pj_timer_heap_t *ht = NULL;
			for(int i=0; i < MAX_HEAPS; i++){
				pj_timer_heap_t *tHeap = sHeaps[i];
				if(tHeap != NULL){
				    lock_timer_heap(tHeap);
					for(j=0; j < MAX_ENTRY_PER_HEAP; j++){
						if(tHeap->entries[j] == entry){
							ht = tHeap;
							break;
						}
					}
				    unlock_timer_heap(tHeap);
				}
				if(ht != NULL){
					break;
				}
			}

			if(ht != NULL){
				PJ_LOG(4, (THIS_FILE, "FIRE timer %d at %x", entry->_timer_id, entry));

				pj_timer_heap_callback* cb = NULL;

				lock_timer_heap(ht);
				// Get callback if entry still valid
				if(entry->_timer_id >= 0){
					cb = entry->cb;
				}
				// Release slot
				ht->entries[entry->_timer_id] = NULL;
				entry->_timer_id = -1;
				unlock_timer_heap(ht);

				// Callback
				if(cb){
					cb(ht, entry);
				}


				PJ_LOG(4, (THIS_FILE, "FIRE done and released"));

			}else{
				PJ_LOG(3, (THIS_FILE, "FIRE Ignore since no heap found for this entry %x", entry));
			}
		}else{
			PJ_LOG(3, (THIS_FILE, "FIRE Ignored : %d", entry->_timer_id));
		}
	}
	return PJ_SUCCESS;
}
PJ_END_DECL
