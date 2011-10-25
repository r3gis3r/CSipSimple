#include "zrtp_android.h"

/*
 * ZRTP stuff
 */

#define THIS_FILE		"zrtp_android.c"

#if defined(PJMEDIA_HAS_ZRTP) && PJMEDIA_HAS_ZRTP!=0


////I know I should not do that here
void on_zrtp_show_sas_wrapper(void* data, char* sas, int verified);
void on_zrtp_secure_on_wrapper(void* data, char* cipher);
void on_zrtp_secure_off_wrapper(void* data);

#include "transport_zrtp.h"

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
static void zrtpAskEnrollment(void* data, char* info)
{
    PJ_LOG(3,(THIS_FILE, "ZRTP - Ask PBX enrollment"));
}
static void zrtpInformEnrollment(void* data, char* info)
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
}




static zrtp_UserCallbacks wrapper_zrtp_callback_struct = {
    &on_zrtp_secure_on_wrapper,
    &on_zrtp_secure_off_wrapper,
    &on_zrtp_show_sas_wrapper,
    &confirmGoClear,
    &showMessage,
    &zrtpNegotiationFailed,
    &zrtpNotSuppOther,
    &zrtpAskEnrollment,
    &zrtpInformEnrollment,
    &signSAS,
    &checkSASSignature,
    NULL
};

pjmedia_transport *current_zrtp;

/* Initialize the ZRTP transport and the user callbacks */
pjmedia_transport* on_zrtp_transport_created(pjsua_call_id call_id,
	unsigned media_idx,
	pjmedia_transport *base_tp,
	unsigned flags) {

		pjmedia_transport *zrtp_tp = NULL;
		pj_status_t status;
		pjmedia_endpt* endpt = pjsua_get_pjmedia_endpt();

		status = pjmedia_transport_zrtp_create(endpt, NULL, base_tp,
											   &zrtp_tp, flags);

		if(status == PJ_SUCCESS){
			PJ_LOG(3,(THIS_FILE, "ZRTP transport created"));
			/*
			* this is optional but highly recommended to enable the application
			* to report status information to the user, such as verfication status,
			* SAS code, etc
			*/
			wrapper_zrtp_callback_struct.userData = zrtp_tp;
			pjmedia_transport_zrtp_setUserCallback(zrtp_tp, &wrapper_zrtp_callback_struct);


			/*
			* Initialize the transport. Just the filename of the ZID file that holds
			* our partners ZID, shared data etc. If the files does not exists it will
			* be created an initialized. The ZRTP configuration is not yet implemented
			* thus the parameter is NULL.
			*/
			pjmedia_transport_zrtp_initialize(zrtp_tp, "/sdcard/simple.zid", PJ_TRUE);
			current_zrtp = zrtp_tp;

			return zrtp_tp;
		} else {
			PJ_LOG(1, (THIS_FILE, "ZRTP transport problem : %d", status));
			return base_tp;
		}
}

// TODO : that's not clean should be able to manage
// several transport -- but for first implementation ...
PJ_DECL(void) jzrtp_SASVerified() {
	ZrtpContext* ctxt = pjmedia_transport_zrtp_getZrtpContext(current_zrtp);
	zrtp_SASVerified(ctxt);
}
//pjmedia_transport_zrtp_getZrtpContext

// * @see zrtp_SASVerified()
// * @see zrtp_resetSASVerified()
#else
PJ_DECL(void) jzrtp_SASVerified() {
	//TODO : log
}
#endif
