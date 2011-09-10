package com.csipsimple.wizards.impl;

//import android.text.InputType;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

public class Sipnet extends SimpleImplementation {

	@Override
    protected String getDomain() {
        return "sipnet.ru";
    }

    @Override
    protected String getDefaultName() {
        return "SIPNET";
    }

    @Override
    protected boolean canTcp() {
        return false;
    }

    public boolean needRestart() {
        return true;
    }
	
    public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);	
		
		//prefs.setPreferenceBooleanValue(PreferencesWrapper.IS_ADVANCED_USER, true);
		
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_DNS_SRV, true);
		prefs.setPreferenceBooleanValue(SipConfigManager.ECHO_CANCELLATION, true);
		//prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_TLS, true);
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, false);
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_ICE, false);
		//prefs.setPreferenceStringValue(SipConfigManager.LOG_LEVEL, "4");
		
		//wb codecs 
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB,"245");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_WB,"244");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "243");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB,"242");
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB,"241");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB,"240");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB,"239");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB,"0");
        
        //nb codecs
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB,"245");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB,"244");
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"243");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB,"242");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "241");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB,"240");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB,"0");
        
	}
    
    @Override
    public SipProfile buildAccount(SipProfile account) {
        account = super.buildAccount(account);
        
        account.reg_uri = "sip:sipnet.ru";
        account.realm = "*";
        //account.transport = SipProfile.TRANSPORT_TLS;
        account.transport = SipProfile.TRANSPORT_TCP;
        account.use_srtp = 0;
        account.use_zrtp = 1;
        account.allow_contact_rewrite = false;
        account.contact_rewrite_method = 1;
        
        return account;
    }
    
}
