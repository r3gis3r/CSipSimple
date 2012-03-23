#include "zrtp_android.h"
#include "pjsua_jni_addons.h"

/*
 * ZRTP stuff
 */

#define THIS_FILE		"zrtp_android.c"
#if defined(PJMEDIA_HAS_ZRTP) && PJMEDIA_HAS_ZRTP!=0


////I know I should not do that here
void on_zrtp_show_sas_wrapper(void* data, char* sas, int verified);

#include "transport_zrtp.h"
#include "libzrtpcpp/ZrtpCWrapper.h"

const char* InfoCodes[] =
{
    "EMPTY",
    "Hello received, preparing a Commit",
    "Commit: Generated a public DH key",
    "Responder: Commit received, preparing DHPart1",
    "DH1Part: Generated a public DH key",
    "Initiator: DHPart1 received, preparing DHPart2",
    "Responder: DHPart2 received, preparing Confirm1",
    "Initiator: Confirm1 received, preparing Confirm2",
    "Responder: Confirm2 received, preparing Conf2Ack",
    "At least one retained secrets matches - security OK",
    "Entered secure state",
    "No more security for this session"
};

/**
 * Sub-codes for Warning
 */
const char* WarningCodes [] =
{
    "EMPTY",
    "Commit contains an AES256 cipher but does not offer a Diffie-Helman 4096",
    "Received a GoClear message",
    "Hello offers an AES256 cipher but does not offer a Diffie-Helman 4096",
    "No retained shared secrets available - must verify SAS",
    "Internal ZRTP packet checksum mismatch - packet dropped",
    "Dropping packet because SRTP authentication failed!",
    "Dropping packet because SRTP replay check failed!",
    "Valid retained shared secrets availabe but no matches found - must verify SAS"
};

/**
 * Sub-codes for Severe
 */
const char* SevereCodes[] =
{
    "EMPTY",
    "Hash HMAC check of Hello failed!",
    "Hash HMAC check of Commit failed!",
    "Hash HMAC check of DHPart1 failed!",
    "Hash HMAC check of DHPart2 failed!",
    "Cannot send data - connection or peer down?",
    "Internal protocol error occured!",
    "Cannot start a timer - internal resources exhausted?",
    "Too much retries during ZRTP negotiation - connection or peer down?"
};



typedef struct zrtp_cb_user_data {
	pjsua_call_id call_id;
	pjmedia_transport *zrtp_tp;
	pj_str_t sas;
	pj_str_t cipher;
} zrtp_cb_user_data;

static void zrtpShowSas(void* data, char* sas, int verified){
	zrtp_cb_user_data* zrtp_data = (zrtp_cb_user_data*) data;
	pj_strdup2_with_null(css_var.pool, &zrtp_data->sas, sas);
	on_zrtp_show_sas_wrapper(data, sas, verified);
}

static void zrtpSecureOn(void* data, char* cipher){
	zrtp_cb_user_data* zrtp_data = (zrtp_cb_user_data*) data;
	pj_strdup2_with_null(css_var.pool, &zrtp_data->cipher, cipher);
	on_zrtp_update_transport_wrapper(data);
}

static void zrtpSecureOff(void* data){
	on_zrtp_update_transport_wrapper(data);
}

static void confirmGoClear(void* data)
{
    PJ_LOG(3,(THIS_FILE, "GoClear????????"));
}
static void showMessage(void* data, int32_t sev, int32_t subCode)
{
    switch (sev)
    {
    case zrtp_Info:
        PJ_LOG(3,(THIS_FILE, "ZRTP info message: %s", InfoCodes[subCode]));
        if(subCode == zrtp_InfoSecureStateOn
        		|| subCode == zrtp_InfoSecureStateOff){
        	on_zrtp_update_transport_wrapper(data);
        }
        break;

    case zrtp_Warning:
        PJ_LOG(3,(THIS_FILE, "ZRTP warning message: %s", WarningCodes[subCode]));
        break;

    case zrtp_Severe:
        PJ_LOG(3,(THIS_FILE, "ZRTP severe message: %s", SevereCodes[subCode]));
        break;

    case zrtp_ZrtpError:
        PJ_LOG(3,(THIS_FILE, "ZRTP Error: severity: %d, subcode: %x", sev, subCode));
        break;
    }
}
static void zrtpNegotiationFailed(void* data, int32_t severity, int32_t subCode)
{
    PJ_LOG(3,(THIS_FILE, "ZRTP failed: %d, subcode: %d", severity, subCode));
}
static void zrtpNotSuppOther(void* data)
{
    PJ_LOG(3,(THIS_FILE, "ZRTP not supported by other peer"));
}
static void zrtpAskEnrollment(void* data, int32_t info)
{
    PJ_LOG(3,(THIS_FILE, "ZRTP - Ask PBX enrollment"));
}
static void zrtpInformEnrollment(void* data, int32_t info)
{
    PJ_LOG(3,(THIS_FILE, "ZRTP - Inform PBX enrollement"));
}
static void signSAS(void* data, char* sas)
{
    PJ_LOG(3,(THIS_FILE, "ZRTP - sign SAS"));
}
static int32_t checkSASSignature(void* data, char* sas)
{
    PJ_LOG(3,(THIS_FILE, "ZRTP - check SAS signature"));
    return 0;
}




/* Initialize the ZRTP transport and the user callbacks */
pjmedia_transport* on_zrtp_transport_created(pjsua_call_id call_id,
	unsigned media_idx,
	pjmedia_transport *base_tp,
	unsigned flags) {

		pjmedia_transport *zrtp_tp = NULL;
		pj_status_t status;
		pjmedia_endpt* endpt = pjsua_get_pjmedia_endpt();

		status = pjmedia_transport_zrtp_create(endpt, NULL, base_tp,
											   &zrtp_tp, (flags & PJSUA_MED_TP_CLOSE_MEMBER));



		if(status == PJ_SUCCESS){
			PJ_LOG(3,(THIS_FILE, "ZRTP transport created"));

			// Build callback data ponter
			zrtp_cb_user_data* zrtp_cb_data = PJ_POOL_ZALLOC_T(css_var.pool, zrtp_cb_user_data);
			zrtp_cb_data->zrtp_tp = zrtp_tp;
			zrtp_cb_data->call_id = call_id;
			zrtp_cb_data->cipher = pj_str("");
			zrtp_cb_data->sas = pj_str("");


			// Build callback struct
			zrtp_UserCallbacks* zrtp_cbs = PJ_POOL_ZALLOC_T(css_var.pool, zrtp_UserCallbacks);
			zrtp_cbs->zrtp_secureOn = &zrtpSecureOn;
			zrtp_cbs->zrtp_secureOff = &zrtpSecureOff;
			zrtp_cbs->zrtp_showSAS = &zrtpShowSas;
			zrtp_cbs->zrtp_confirmGoClear = &confirmGoClear;
			zrtp_cbs->zrtp_showMessage = &showMessage;
			zrtp_cbs->zrtp_zrtpNegotiationFailed = &zrtpNegotiationFailed;
			zrtp_cbs->zrtp_zrtpNotSuppOther = &zrtpNotSuppOther;
			zrtp_cbs->zrtp_zrtpAskEnrollment = &zrtpAskEnrollment;
			zrtp_cbs->zrtp_zrtpInformEnrollment = &zrtpInformEnrollment;
			zrtp_cbs->zrtp_signSAS = &signSAS;
			zrtp_cbs->zrtp_checkSASSignature = &checkSASSignature;
			zrtp_cbs->userData = zrtp_cb_data;

			pjmedia_transport_zrtp_setUserCallback(zrtp_tp, zrtp_cbs);


			/*
			* Initialize the transport. Just the filename of the ZID file that holds
			* our partners ZID, shared data etc. If the files does not exists it will
			* be created an initialized. The ZRTP configuration is not yet implemented
			* thus the parameter is NULL.
			*/
			pjmedia_transport_zrtp_initialize(zrtp_tp, css_var.zid_file, PJ_TRUE);

			return zrtp_tp;
		} else {
			PJ_LOG(1, (THIS_FILE, "ZRTP transport problem : %d", status));
			return base_tp;
		}
}

PJ_DECL(void) jzrtp_SASVerified(long zrtp_data_p) {
	zrtp_cb_user_data* zrtp_data = (zrtp_cb_user_data*) zrtp_data_p;
	ZrtpContext* ctxt = pjmedia_transport_zrtp_getZrtpContext(zrtp_data->zrtp_tp);
	zrtp_SASVerified(ctxt);
}

PJ_DECL(int) jzrtp_getCallId(long zrtp_data_p){
	zrtp_cb_user_data* zrtp_data = (zrtp_cb_user_data*) zrtp_data_p;
	return zrtp_data->call_id;

}


pj_str_t jzrtp_getInfo(pjmedia_transport* tp){
	pj_str_t result;

	char msg[512];

	ZrtpContext *ctx = pjmedia_transport_zrtp_getZrtpContext(tp);
	int32_t state = zrtp_inState(ctx, SecureState);

	zrtp_cb_user_data* zrtp_cb_data = (zrtp_cb_user_data*) pjmedia_transport_zrtp_getUserData(tp);

	if (state) {
		pj_ansi_snprintf(msg, sizeof(msg), "ZRTP - %s\n%.*s\n%.*s", "OK",
				zrtp_cb_data->sas.slen, zrtp_cb_data->sas.ptr,
				zrtp_cb_data->cipher.slen, zrtp_cb_data->cipher.ptr);
	} else {
		pj_ansi_snprintf(msg, sizeof(msg), "");
	}
	pj_strdup2_with_null(css_var.pool, &result, msg);


	PJ_LOG(4, (THIS_FILE, "ZRTP getInfos : %s", msg));

	return result;
}

#else
PJ_DECL(void) jzrtp_SASVerified(long zrtp_data_p) {
	//TODO : log
}

PJ_DECL(int) jzrtp_getCallId(long zrtp_data_p){
	return -1;
}
#endif
