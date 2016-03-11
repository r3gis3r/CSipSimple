/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.csipsimple.service;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.widget.Toast;

import com.csipsimple.R;
import com.csipsimple.api.ISipConfiguration;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.MediaState;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipManager.PresenceStatus;
import com.csipsimple.api.SipMessage;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipProfileState;
import com.csipsimple.api.SipUri;
import com.csipsimple.db.DBProvider;
import com.csipsimple.models.Filter;
import com.csipsimple.pjsip.PjSipCalls;
import com.csipsimple.pjsip.PjSipService;
import com.csipsimple.pjsip.UAStateReceiver;
import com.csipsimple.service.receiver.DynamicReceiver4;
import com.csipsimple.service.receiver.DynamicReceiver5;
import com.csipsimple.ui.incall.InCallMediaControl;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.CustomDistribution;
import com.csipsimple.utils.ExtraPlugins;
import com.csipsimple.utils.ExtraPlugins.DynActivityPlugin;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.PreferencesWrapper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SipService extends Service {

	
	// static boolean creating = false;
	private static final String THIS_FILE = "SIP SRV";

	protected static final String TAG = SipService.class.getSimpleName();

	private SipWakeLock sipWakeLock;
	private boolean autoAcceptCurrent = false;
	public boolean supportMultipleCalls = false;
	
	// For video testing -- TODO : remove
	private static SipService singleton = null;
	

	// Implement public interface for the service
	private final ISipService.Stub binder = new ISipService.Stub() {
        /**
         * {@inheritDoc}
         */
		@Override
		public void sipStart() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "Start required from third party app/serv");
			getExecutor().execute(new StartRunnable());
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void sipStop() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new StopRunnable());
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void forceStopService() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "Try to force service stop");
			cleanStop();
			//stopSelf();
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void askThreadedRestart() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "Restart required from third part app/serv");
			getExecutor().execute(new RestartRunnable());
		};

        /**
         * {@inheritDoc}
         */
		@Override
		public void addAllAccounts() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				public void doRun() throws SameThreadException {
					SipService.this.addAllAccounts();
				}
			});
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void removeAllAccounts() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				public void doRun() throws SameThreadException {
					SipService.this.unregisterAllAccounts(true);
				}
			});
		}


        /**
         * {@inheritDoc}
         */
		@Override
		public void reAddAllAccounts() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				public void doRun() throws SameThreadException {
					SipService.this.reAddAllAccounts();
					
				}
			});
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void setAccountRegistration(int accountId, int renew) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			
			final SipProfile acc = getAccount(accountId);
			if(acc != null) {
				final int ren = renew;
				getExecutor().execute(new SipRunnable() {
					@Override
					public void doRun() throws SameThreadException {
						SipService.this.setAccountRegistration(acc, ren, false);
					}
				});
			}
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public SipProfileState getSipProfileState(int accountId) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			return SipService.this.getSipProfileState(accountId);
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void switchToAutoAnswer() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "Switch to auto answer");
			setAutoAnswerNext(true);
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void makeCall(final String callee, final int accountId) throws RemoteException {
			makeCallWithOptions(callee, accountId, null);
		}
		

        @Override
        public void makeCallWithOptions(final String callee, final int accountId, final Bundle options)
                throws RemoteException {
        	Log.i(TAG,  "callee: " + callee);
        	Log.i(TAG,  "accountId: " + accountId);
            SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
            //We have to ensure service is properly started and not just binded
            SipService.this.startService(new Intent(SipService.this, SipService.class));
            
            if(pjService == null) {
                Log.e(THIS_FILE, "Can't place call if service not started");
                // TODO - we should return a failing status here
                return;
            }
            
            if(!supportMultipleCalls) {
                // Check if there is no ongoing calls if so drop this request by alerting user
                SipCallSession activeCall = pjService.getActiveCallInProgress();
                if(activeCall != null) {
                    if(!CustomDistribution.forceNoMultipleCalls()) {
                        notifyUserOfMessage(R.string.not_configured_multiple_calls);
                    }
                    return;
                }
            }
            
            Intent intent = new Intent(SipManager.ACTION_SIP_CALL_LAUNCH);
            intent.putExtra(SipProfile.FIELD_ID, accountId);
            intent.putExtra(SipManager.EXTRA_SIP_CALL_TARGET, callee);
            intent.putExtra(SipManager.EXTRA_SIP_CALL_OPTIONS, options);
            sendOrderedBroadcast (intent , SipManager.PERMISSION_USE_SIP, mPlaceCallResultReceiver, null,  Activity.RESULT_OK, null, null);
            
        }
		
        private BroadcastReceiver mPlaceCallResultReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, final Intent intent) {
                final Bundle extras =  intent.getExtras();
                final String action = intent.getAction();
                if(extras == null) {
                    Log.e(THIS_FILE, "No data in intent retrieved for call");
                    return;
                }
                if(!SipManager.ACTION_SIP_CALL_LAUNCH.equals(action)) {
                    Log.e(THIS_FILE, "Received invalid action " + action);
                    return;
                }
                
                Log.i(TAG, "broadcast receiver: " + action);

                final int accountId = extras.getInt(SipProfile.FIELD_ID, -2);
                final String callee = extras.getString(SipManager.EXTRA_SIP_CALL_TARGET);
                final Bundle options = extras.getBundle(SipManager.EXTRA_SIP_CALL_OPTIONS);
                if(accountId == -2 || callee == null) {
                    Log.e(THIS_FILE, "Invalid rewrite " + accountId);
                    return;
                }
                
                getExecutor().execute(new SipRunnable() {
                    @Override
                    protected void doRun() throws SameThreadException {
                        pjService.makeCall(callee, accountId, options);
                    }
                });
            }
        };
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void sendMessage(final String message, final String callee, final long accountId) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			//We have to ensure service is properly started and not just binded
			SipService.this.startService(new Intent(SipService.this, SipService.class));
			
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					Log.d(THIS_FILE, "will sms " + callee);
					if(pjService != null) {
    					ToCall called = pjService.sendMessage(callee, message, accountId);
    					if(called!=null) {
    						SipMessage msg = new SipMessage(SipMessage.SELF, 
    								SipUri.getCanonicalSipContact(callee), SipUri.getCanonicalSipContact(called.getCallee()), 
    								message, "text/plain", System.currentTimeMillis(), 
    								SipMessage.MESSAGE_TYPE_QUEUED, called.getCallee());
    						msg.setRead(true);
    						getContentResolver().insert(SipMessage.MESSAGE_URI, msg.getContentValues());
    						Log.d(THIS_FILE, "Inserted "+msg.getTo());
    					}else {
    						SipService.this.notifyUserOfMessage( getString(R.string.invalid_sip_uri)+ " : "+callee );
    					}
					}else {
					    SipService.this.notifyUserOfMessage( getString(R.string.connection_not_valid) );
					}
				}
			});
		}


        /**
         * {@inheritDoc}
         */
		@Override
		public int answer(final int callId, final int status) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			ReturnRunnable action = new ReturnRunnable() {
				@Override
				protected Object runWithReturn() throws SameThreadException {
					return (Integer) pjService.callAnswer(callId, status);
				}
			};
			getExecutor().execute(action);
			//return (Integer) action.getResult();
			return SipManager.SUCCESS;
		}


        /**
         * {@inheritDoc}
         */
		@Override
		public int hangup(final int callId, final int status) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			ReturnRunnable action = new ReturnRunnable() {
				@Override
				protected Object runWithReturn() throws SameThreadException {
					return (Integer) pjService.callHangup(callId, status);
				}
			};
			getExecutor().execute(action);
			//return (Integer) action.getResult();
			
			return SipManager.SUCCESS;
		}
		
        /**
         * {@inheritDoc}
         */
		@Override
		public int xfer(final int callId, final String callee) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "XFER");
			ReturnRunnable action = new ReturnRunnable() {
				@Override
				protected Object runWithReturn() throws SameThreadException {
					return (Integer) pjService.callXfer(callId, callee);
				}
			};
			getExecutor().execute(action);
			return (Integer) action.getResult();
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public int xferReplace(final int callId, final int otherCallId, final int options) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "XFER-replace");
			ReturnRunnable action = new ReturnRunnable() {
				@Override
				protected Object runWithReturn() throws SameThreadException {
					return (Integer) pjService.callXferReplace(callId, otherCallId, options);
				}
			};
			getExecutor().execute(action);
			return (Integer) action.getResult();
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public int sendDtmf(final int callId, final int keyCode) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);

			ReturnRunnable action = new ReturnRunnable() {
				@Override
				protected Object runWithReturn() throws SameThreadException {
					return (Integer) pjService.sendDtmf(callId, keyCode);
				}
			};
			getExecutor().execute(action);
			return (Integer) action.getResult();
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public int hold(final int callId) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "HOLDING");
			ReturnRunnable action = new ReturnRunnable() {
				@Override
				protected Object runWithReturn() throws SameThreadException {
					return (Integer) pjService.callHold(callId);
				}
			};
			getExecutor().execute(action);
			return (Integer) action.getResult();
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public int reinvite(final int callId, final boolean unhold) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "REINVITING");
			ReturnRunnable action = new ReturnRunnable() {
				@Override
				protected Object runWithReturn() throws SameThreadException {
					return (Integer) pjService.callReinvite(callId, unhold);
				}
			};
			getExecutor().execute(action);
			return (Integer) action.getResult();
		}
		
        /**
         * {@inheritDoc}
         */
		@Override
		public SipCallSession getCallInfo(final int callId) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			return new SipCallSession(pjService.getCallInfo(callId));
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void setBluetoothOn(final boolean on) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.setBluetoothOn(on);
				}
			});
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void setMicrophoneMute(final boolean on) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.setMicrophoneMute(on);
				}
			});
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void setSpeakerphoneOn(final boolean on) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.setSpeakerphoneOn(on);
				}
			});
		}


        /**
         * {@inheritDoc}
         */
		@Override
		public SipCallSession[] getCalls() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			if(pjService != null) {
				SipCallSession[] listOfCallsImpl = pjService.getCalls();
				SipCallSession[] result = new SipCallSession[listOfCallsImpl.length];
				for(int sessIdx = 0; sessIdx < listOfCallsImpl.length; sessIdx++) {
				    result[sessIdx] = new SipCallSession(listOfCallsImpl[sessIdx]);
				}
				return result;
			}
			return new SipCallSession[0];
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void confAdjustTxLevel(final int port, final float value) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					if(pjService == null) {
		    			return;
		    		}
					pjService.confAdjustTxLevel(port, value);
				}
			});
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void confAdjustRxLevel(final int port, final float value) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					if(pjService == null) {
		    			return;
		    		}
					pjService.confAdjustRxLevel(port, value);
				}
			});
			
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void adjustVolume(SipCallSession callInfo, int direction, int flags) throws RemoteException {

			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			
			if(pjService == null) {
    			return;
    		}
			
    		boolean ringing = callInfo.isIncoming() && callInfo.isBeforeConfirmed();
        	// Mode ringing
    		if(ringing) {
	        	// What is expected here is to silence ringer
    			//pjService.adjustStreamVolume(AudioManager.STREAM_RING, direction, AudioManager.FLAG_SHOW_UI);
    			pjService.silenceRinger();
    		}else {
	        	// Mode in call
	        	if(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_SOFT_VOLUME)) {
	        		Intent adjustVolumeIntent = new Intent(SipService.this, InCallMediaControl.class);
	        		adjustVolumeIntent.putExtra(Intent.EXTRA_KEY_EVENT, direction);
	        		adjustVolumeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        		startActivity(adjustVolumeIntent);
	        	}else {
	        		pjService.adjustStreamVolume(Compatibility.getInCallStream(pjService.mediaManager.doesUserWantBluetooth()), direction, flags);
	        	}
    		}
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void setEchoCancellation(final boolean on) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			if(pjService == null) {
                return;
            }
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.setEchoCancellation(on);
				}
			});
		}

        /**
         * {@inheritDoc}
         */
        @Override
        public void startRecording(final int callId, final int way) throws RemoteException {
            SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
            if (pjService == null) {
                return;
            }
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    pjService.startRecording(callId, way);
                }
            });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void stopRecording(final int callId) throws RemoteException {
            SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
            if (pjService == null) {
                return;
            }
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    pjService.stopRecording(callId);
                }
            });
        }

        /**
         * {@inheritDoc}
         */
		@Override
		public boolean isRecording(int callId) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			if(pjService == null) {
				return false;
			}
			
			SipCallSession info = pjService.getCallInfo(callId);
			if(info != null) {
			    return info.isRecording();
			}
			return false;
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public boolean canRecord(int callId) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			if(pjService == null) {
                return false;
            }
			return pjService.canRecord(callId);
		}

        /**
         * {@inheritDoc}
         */
        @Override
        public void playWaveFile(final String filePath, final int callId, final int way) throws RemoteException {
            SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
            if(pjService == null) {
                return;
            }
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    pjService.playWaveFile(filePath, callId, way);
                }
            });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setPresence(final int presenceInt, final String statusText, final long accountId) throws RemoteException {
            SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
            if(pjService == null) {
                return;
            }
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    presence = PresenceStatus.values()[presenceInt];
                    pjService.setPresence(presence, statusText, accountId);
                }
            });
        }
        

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPresence(long accountId) throws RemoteException {
            // TODO Auto-generated method stub
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getPresenceStatus(long accountId) throws RemoteException {
            // TODO Auto-generated method stub
            return null;
        }

        /**
         * {@inheritDoc}
         */
		@Override
		public void zrtpSASVerified(final int callId) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.zrtpSASVerified(callId);
					pjService.zrtpReceiver.updateZrtpInfos(callId);
				}
			});
		}
		
        /**
         * {@inheritDoc}
         */
        @Override
        public void zrtpSASRevoke(final int callId) throws RemoteException {
            SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    pjService.zrtpSASRevoke(callId);
                    pjService.zrtpReceiver.updateZrtpInfos(callId);
                }
            });
        }

        /**
         * {@inheritDoc}
         */
		@Override
		public MediaState getCurrentMediaState() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			MediaState ms = new MediaState();
			if(pjService != null && pjService.mediaManager != null) {
				ms = pjService.mediaManager.getMediaState();
			}
			return ms;
		}


        /**
         * {@inheritDoc}
         */
		@Override
		public int getVersion() throws RemoteException {
			return SipManager.CURRENT_API;
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public String showCallInfosDialog(final int callId) throws RemoteException {
			ReturnRunnable action = new ReturnRunnable() {
				@Override
				protected Object runWithReturn() throws SameThreadException {
					String infos = PjSipCalls.dumpCallInfo(callId);
					Log.d(THIS_FILE, infos);
					return infos;
				}
			};
			
			getExecutor().execute(action);
			return (String) action.getResult();
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public int startLoopbackTest() throws RemoteException {
			if(pjService == null) {
				return SipManager.ERROR_CURRENT_NETWORK;
			}
			SipRunnable action = new SipRunnable() {
				
				@Override
				protected void doRun() throws SameThreadException {
				    pjService.startLoopbackTest();
				}
			};
			
			getExecutor().execute(action);
			return SipManager.SUCCESS;
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public int stopLoopbackTest() throws RemoteException {
			if(pjService == null) {
				return SipManager.ERROR_CURRENT_NETWORK;
			}
			SipRunnable action = new SipRunnable() {
				
				@Override
				protected void doRun() throws SameThreadException {
				    pjService.stopLoopbackTest();
				}
			};
			
			getExecutor().execute(action);
			return SipManager.SUCCESS;
		}

        /**
         * {@inheritDoc}
         */
        @Override
        public long confGetRxTxLevel(final int port) throws RemoteException {
            ReturnRunnable action = new ReturnRunnable() {
                @Override
                protected Object runWithReturn() throws SameThreadException {
                    return (Long) pjService.getRxTxLevel(port);
                }
            };
            getExecutor().execute(action);
            return (Long) action.getResult();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void ignoreNextOutgoingCallFor(String number) throws RemoteException {
            OutgoingCall.ignoreNext = number;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void updateCallOptions(final int callId, final Bundle options) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    pjService.updateCallOptions(callId, options);
                }
            });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getLocalNatType() throws RemoteException {
            ReturnRunnable action = new ReturnRunnable() {
                @Override
                protected Object runWithReturn() throws SameThreadException {
                    return (String) pjService.getDetectedNatType();
                }
            };
            getExecutor().execute(action);
            return (String) action.getResult();
        }



		
	};

	private final ISipConfiguration.Stub binderConfiguration = new ISipConfiguration.Stub() {

		@Override
		public void setPreferenceBoolean(String key, boolean value) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_CONFIGURE_SIP, null);
			prefsWrapper.setPreferenceBooleanValue(key, value);
		}

		@Override
		public void setPreferenceFloat(String key, float value) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_CONFIGURE_SIP, null);
			prefsWrapper.setPreferenceFloatValue(key, value);

		}

		@Override
		public void setPreferenceString(String key, String value) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_CONFIGURE_SIP, null);
			prefsWrapper.setPreferenceStringValue(key, value);

		}

		@Override
		public String getPreferenceString(String key) throws RemoteException {
			return prefsWrapper.getPreferenceStringValue(key);
			
		}

		@Override
		public boolean getPreferenceBoolean(String key) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_CONFIGURE_SIP, null);
			return prefsWrapper.getPreferenceBooleanValue(key);
			
		}

		@Override
		public float getPreferenceFloat(String key) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_CONFIGURE_SIP, null);
			return prefsWrapper.getPreferenceFloatValue(key);
		}

	};

	private WakeLock wakeLock;
	private WifiLock wifiLock;
	private DynamicReceiver4 deviceStateReceiver;
	private PreferencesProviderWrapper prefsWrapper;
	private ServicePhoneStateReceiver phoneConnectivityReceiver;
	private TelephonyManager telephonyManager;
//	private ConnectivityManager connectivityManager;

	public SipNotifications notificationManager;
	private SipServiceExecutor mExecutor;
	private static PjSipService pjService;
	private static HandlerThread executorThread;
	
	private AccountStatusContentObserver statusObserver = null;
    //public PresenceManager presenceMgr;
    private BroadcastReceiver serviceReceiver;
	
	class AccountStatusContentObserver extends ContentObserver {
		public AccountStatusContentObserver(Handler h) {
			super(h);
		}

		public void onChange(boolean selfChange) {
			Log.d(THIS_FILE, "Accounts status.onChange( " + selfChange + ")");
			updateRegistrationsState();
		}
	}
	

    public SipServiceExecutor getExecutor() {
        // create mExecutor lazily
        if (mExecutor == null) {
        	mExecutor = new SipServiceExecutor(this);
        }
        return mExecutor;
    }

	private class ServicePhoneStateReceiver extends PhoneStateListener {
		
		//private boolean ignoreFirstConnectionState = true;
		private boolean ignoreFirstCallState = true;
		/*
		@Override
		public void onDataConnectionStateChanged(final int state) {
			if(!ignoreFirstConnectionState) {
				Log.d(THIS_FILE, "Data connection state changed : " + state);
				Thread t = new Thread("DataConnectionDetach") {
					@Override
					public void run() {
						if(deviceStateReceiver != null) {
							deviceStateReceiver.onChanged("MOBILE", state == TelephonyManager.DATA_CONNECTED);
						}
					}
				};
				t.start();
			}else {
				ignoreFirstConnectionState = false;
			}
			super.onDataConnectionStateChanged(state);
		}
		*/

		@Override
		public void onCallStateChanged(final int state, final String incomingNumber) {
			if(!ignoreFirstCallState) {
				Log.d(THIS_FILE, "Call state has changed !" + state + " : " + incomingNumber);
				getExecutor().execute(new SipRunnable() {
					
					@Override
					protected void doRun() throws SameThreadException {
						if(pjService != null) {
							pjService.onGSMStateChanged(state, incomingNumber);
						}
					}
				});
			}else {
				ignoreFirstCallState = false;
			}
			super.onCallStateChanged(state, incomingNumber);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;

		Log.i(THIS_FILE, "Create SIP Service");
		prefsWrapper = new PreferencesProviderWrapper(this);
		Log.setLogLevel(prefsWrapper.getLogLevel());
		
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//		connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		notificationManager = new SipNotifications(this);
		notificationManager.onServiceCreate();
		sipWakeLock = new SipWakeLock((PowerManager) getSystemService(Context.POWER_SERVICE));
		
		boolean hasSetup = prefsWrapper.getPreferenceBooleanValue(PreferencesProviderWrapper.HAS_ALREADY_SETUP_SERVICE, false);
		Log.d(THIS_FILE, "Service has been setup ? "+ hasSetup);
		
		//presenceMgr = new PresenceManager();
        registerServiceBroadcasts();
		
		if(!hasSetup) {
			Log.e(THIS_FILE, "RESET SETTINGS !!!!");
			prefsWrapper.resetAllDefaultValues();
		}



	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(THIS_FILE, "Destroying SIP Service");
		unregisterBroadcasts();
		unregisterServiceBroadcasts();
		notificationManager.onServiceDestroy();
		getExecutor().execute(new FinalizeDestroyRunnable());
	}
	
	public void cleanStop () {
		getExecutor().execute(new DestroyRunnable());
	}
	
	private void applyComponentEnablingState(boolean active) {
	    int enableState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
	    if(active && prefsWrapper.getPreferenceBooleanValue(SipConfigManager.INTEGRATE_TEL_PRIVILEGED) ) {
            // Check whether we should register for stock tel: intents
            // Useful for devices without gsm
            enableState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
	    }
        PackageManager pm = getPackageManager();
        
        ComponentName cmp = new ComponentName(this, "com.csipsimple.ui.PrivilegedOutgoingCallBroadcaster");
        try {
            if (pm.getComponentEnabledSetting(cmp) != enableState) {
                pm.setComponentEnabledSetting(cmp, enableState, PackageManager.DONT_KILL_APP);
            }
        } catch (IllegalArgumentException e) {
            Log.d(THIS_FILE,
                    "Current manifest has no PrivilegedOutgoingCallBroadcaster -- you can ignore this if voluntary", e);
        }
	}
	
	private void registerServiceBroadcasts() {
	    if(serviceReceiver == null) {
	        IntentFilter intentfilter = new IntentFilter();
            intentfilter.addAction(SipManager.ACTION_DEFER_OUTGOING_UNREGISTER);
            intentfilter.addAction(SipManager.ACTION_OUTGOING_UNREGISTER);
            serviceReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if(action.equals(SipManager.ACTION_OUTGOING_UNREGISTER)){
                        unregisterForOutgoing((ComponentName) intent.getParcelableExtra(SipManager.EXTRA_OUTGOING_ACTIVITY));
                    } else if(action.equals(SipManager.ACTION_DEFER_OUTGOING_UNREGISTER)){
                        deferUnregisterForOutgoing((ComponentName) intent.getParcelableExtra(SipManager.EXTRA_OUTGOING_ACTIVITY));
                    }
                }
                
            };
            registerReceiver(serviceReceiver, intentfilter);
	   }
	}
	
	private void unregisterServiceBroadcasts() {
	    if(serviceReceiver != null) {
	        unregisterReceiver(serviceReceiver);
	        serviceReceiver = null;
	    }
	}
	
	/**
	 * Register broadcast receivers.
	 */
	private void registerBroadcasts() {
		// Register own broadcast receiver
		if (deviceStateReceiver == null) {
			IntentFilter intentfilter = new IntentFilter();
			intentfilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
			intentfilter.addAction(SipManager.ACTION_SIP_ACCOUNT_CHANGED);
            intentfilter.addAction(SipManager.ACTION_SIP_ACCOUNT_DELETED);
			intentfilter.addAction(SipManager.ACTION_SIP_CAN_BE_STOPPED);
			intentfilter.addAction(SipManager.ACTION_SIP_REQUEST_RESTART);
			intentfilter.addAction(DynamicReceiver4.ACTION_VPN_CONNECTIVITY);
			if(Compatibility.isCompatible(5)) {
			    deviceStateReceiver = new DynamicReceiver5(this);
			}else {
			    deviceStateReceiver = new DynamicReceiver4(this);
			}
			registerReceiver(deviceStateReceiver, intentfilter);
			deviceStateReceiver.startMonitoring();
		}
		// Telephony
		if (phoneConnectivityReceiver == null) {
			Log.d(THIS_FILE, "Listen for phone state ");
			phoneConnectivityReceiver = new ServicePhoneStateReceiver();
			
			telephonyManager.listen(phoneConnectivityReceiver, /*PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
					| */PhoneStateListener.LISTEN_CALL_STATE );
		}
		// Content observer
		if(statusObserver == null) {
        	statusObserver = new AccountStatusContentObserver(serviceHandler);
    		getContentResolver().registerContentObserver(SipProfile.ACCOUNT_STATUS_URI, true, statusObserver);
		}
		
	}

	/**
	 * Remove registration of broadcasts receiver.
	 */
	private void unregisterBroadcasts() {
		if(deviceStateReceiver != null) {
			try {
				Log.d(THIS_FILE, "Stop and unregister device receiver");
				deviceStateReceiver.stopMonitoring();
				unregisterReceiver(deviceStateReceiver);
				deviceStateReceiver = null;
			} catch (IllegalArgumentException e) {
				// This is the case if already unregistered itself
				// Python style usage of try ;) : nothing to do here since it could
				// be a standard case
				// And in this case nothing has to be done
				Log.d(THIS_FILE, "Has not to unregister telephony receiver");
			}
		}
		if (phoneConnectivityReceiver != null) {
			Log.d(THIS_FILE, "Unregister telephony receiver");
			telephonyManager.listen(phoneConnectivityReceiver, PhoneStateListener.LISTEN_NONE);
			phoneConnectivityReceiver = null;
		}
		if(statusObserver != null) {
    		getContentResolver().unregisterContentObserver(statusObserver);
    		statusObserver = null;
    	}
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("deprecation")
    @Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if(intent != null) {
    		Parcelable p = intent.getParcelableExtra(SipManager.EXTRA_OUTGOING_ACTIVITY);
    		if(p != null) {
    		    ComponentName outActivity = (ComponentName) p;
    		    registerForOutgoing(outActivity);
    		}
		}
		
        // Check connectivity, else just finish itself
        if (!isConnectivityValid()) {
            notifyUserOfMessage(R.string.connection_not_valid);
            Log.d(THIS_FILE, "Harakiri... we are not needed since no way to use self");
            cleanStop();
            return;
        }
		
		// Autostart the stack - make sure started and that receivers are there
		// NOTE : the stack may also be autostarted cause of phoneConnectivityReceiver
		if (!loadStack()) {
			return;
		}
		
		
		//if(directConnect) {
			Log.d(THIS_FILE, "Direct sip start");
			getExecutor().execute(new StartRunnable());
			/*
		}else {
			Log.d(THIS_FILE, "Defered SIP start !!");
			NetworkInfo netInfo = (NetworkInfo) connectivityManager.getActiveNetworkInfo();
			if(netInfo != null) {
				String type = netInfo.getTypeName();
				NetworkInfo.State state = netInfo.getState();
				if(state == NetworkInfo.State.CONNECTED) {
					Log.d(THIS_FILE, ">> on changed connected");
					deviceStateReceiver.onChanged(type, true);
				}else if(state == NetworkInfo.State.DISCONNECTED) {
					Log.d(THIS_FILE, ">> on changed disconnected");
					deviceStateReceiver.onChanged(type, false);
				}
			}else {
				deviceStateReceiver.onChanged(null, false);
				Log.d(THIS_FILE, ">> on changed disconnected");
			}
		}
		*/
	}
	
	
	private List<ComponentName> activitiesForOutgoing = new ArrayList<ComponentName>();
    private List<ComponentName> deferedUnregisterForOutgoing = new ArrayList<ComponentName>();
	public void registerForOutgoing(ComponentName activityKey) {
	    if(!activitiesForOutgoing.contains(activityKey)) {
	        activitiesForOutgoing.add(activityKey);
	    }
	}
	public void unregisterForOutgoing(ComponentName activityKey) {
	    activitiesForOutgoing.remove(activityKey);
	    
	    if(!isConnectivityValid()) {
	        cleanStop();
	    }
	}
	public void deferUnregisterForOutgoing(ComponentName activityKey) {
	    if(!deferedUnregisterForOutgoing.contains(activityKey)) {
	        deferedUnregisterForOutgoing.add(activityKey);
	    }
	}
	public void treatDeferUnregistersForOutgoing() {
	    for(ComponentName cmp : deferedUnregisterForOutgoing) {
	        activitiesForOutgoing.remove(cmp);
	    }
	    deferedUnregisterForOutgoing.clear();
        if(!isConnectivityValid()) {
            cleanStop();
        }
	}
	
	public boolean isConnectivityValid() {
	    if(prefsWrapper.getPreferenceBooleanValue(PreferencesWrapper.HAS_BEEN_QUIT, false)) {
	        return false;
	    }
	    boolean valid = prefsWrapper.isValidConnectionForIncoming();
	    if(activitiesForOutgoing.size() > 0) {
	        valid |= prefsWrapper.isValidConnectionForOutgoing();
	    }
	    return valid;
	}
	
	

	private boolean loadStack() {
		//Ensure pjService exists
		if(pjService == null) {
			pjService = new PjSipService();
		}
		pjService.setService(this);
		
		if (pjService.tryToLoadStack()) {
			return true;
		}
		return false;
	}


	@Override
	public IBinder onBind(Intent intent) {

		String serviceName = intent.getAction();
		Log.d(THIS_FILE, "Action is " + serviceName);
		if (serviceName == null || serviceName.equalsIgnoreCase(SipManager.INTENT_SIP_SERVICE)) {
			Log.d(THIS_FILE, "Service returned");
			return binder;
		} else if (serviceName.equalsIgnoreCase(SipManager.INTENT_SIP_CONFIGURATION)) {
			Log.d(THIS_FILE, "Conf returned");
			return binderConfiguration;
		}
		Log.d(THIS_FILE, "Default service (SipService) returned");
		return binder;
	}

	//private KeepAliveTimer kaAlarm;
	// This is always done in SipExecutor thread
	private void startSipStack() throws SameThreadException {
		//Cache some prefs
		supportMultipleCalls = prefsWrapper.getPreferenceBooleanValue(SipConfigManager.SUPPORT_MULTIPLE_CALLS);
		
		if(!isConnectivityValid()) {
		    notifyUserOfMessage(R.string.connection_not_valid);
			Log.e(THIS_FILE, "No need to start sip");
			return;
		}
		Log.d(THIS_FILE, "Start was asked and we should actually start now");
		if(pjService == null) {
			Log.d(THIS_FILE, "Start was asked and pjService in not there");
			if(!loadStack()) {
				Log.e(THIS_FILE, "Unable to load SIP stack !! ");
				return;
			}
		}
		Log.d(THIS_FILE, "Ask pjservice to start itself");
		

        //presenceMgr.startMonitoring(this);
		if(pjService.sipStart()) {
		    // This should be done after in acquire resource
		    // But due to http://code.google.com/p/android/issues/detail?id=21635
		    // not a good idea
	        applyComponentEnablingState(true);
	        
	        registerBroadcasts();
			Log.d(THIS_FILE, "Add all accounts");
			addAllAccounts();
		}
	}
	
	/**
	 * Safe stop the sip stack
	 * @return true if can be stopped, false if there is a pending call and the sip service should not be stopped
	 */
	public boolean stopSipStack() throws SameThreadException {
		Log.d(THIS_FILE, "Stop sip stack");
		boolean canStop = true;
		if(pjService != null) {
			canStop &= pjService.sipStop();
			/*
			if(canStop) {
				pjService = null;
			}
			*/
		}
		if(canStop) {
		    //if(presenceMgr != null) {
		    //    presenceMgr.stopMonitoring();
		    //}
		    
		    // Due to http://code.google.com/p/android/issues/detail?id=21635
            // exclude 14 and upper from auto disabling on stop.
            if(!Compatibility.isCompatible(14)) {
                applyComponentEnablingState(false);
            }

            unregisterBroadcasts();
			releaseResources();
		}

		return canStop;
	}
	

    public void restartSipStack() throws SameThreadException {
        if(stopSipStack()) {
            startSipStack();
        }else {
            Log.e(THIS_FILE, "Can't stop ... so do not restart ! ");
        }
    }
	
    /**
     * Notify user from a message the sip stack wants to transmit.
     * For now it shows a toaster
     * @param msg String message to display
     */
	public void notifyUserOfMessage(String msg) {
		serviceHandler.sendMessage(serviceHandler.obtainMessage(TOAST_MESSAGE, msg));
	}
	/**
	 * Notify user from a message the sip stack wants to transmit.
     * For now it shows a toaster
	 * @param resStringId The id of the string resource to display
	 */
	public void notifyUserOfMessage(int resStringId) {
	    serviceHandler.sendMessage(serviceHandler.obtainMessage(TOAST_MESSAGE, resStringId, 0));
	}
	
	private boolean hasSomeActiveAccount = false;
	/**
	 * Add accounts from database
	 */
	private void addAllAccounts() throws SameThreadException {
		Log.d(THIS_FILE, "We are adding all accounts right now....");

		boolean hasSomeSuccess = false;
		Cursor c = getContentResolver().query(SipProfile.ACCOUNT_URI, DBProvider.ACCOUNT_FULL_PROJECTION, 
				SipProfile.FIELD_ACTIVE + "=?", new String[] {"1"}, null);
		if (c != null) {
			try {
				int index = 0;
				if(c.getCount() > 0) {
    				c.moveToFirst();
    				do {
    					SipProfile account = new SipProfile(c);
    					if (pjService != null && pjService.addAccount(account) ) {
    						hasSomeSuccess = true;
    					}
    					index ++;
    				} while (c.moveToNext() && index < 10);
				}
			} catch (Exception e) {
				Log.e(THIS_FILE, "Error on looping over sip profiles", e);
			} finally {
				c.close();
			}
		}
		
		hasSomeActiveAccount = hasSomeSuccess;

		if (hasSomeSuccess) {
			acquireResources();
			
		} else {
			releaseResources();
			if (notificationManager != null) {
				notificationManager.cancelRegisters();
			}
		}
	}

	

	public boolean setAccountRegistration(SipProfile account, int renew, boolean forceReAdd) throws SameThreadException {
		boolean status = false;
		if(pjService != null) {
			status = pjService.setAccountRegistration(account, renew, forceReAdd);
		}		
		
		return status;
	}

	/**
	 * Remove accounts from database
	 */
	private void unregisterAllAccounts(boolean cancelNotification) throws SameThreadException {

		releaseResources();
		
		Log.d(THIS_FILE, "Remove all accounts");
		
		Cursor c = getContentResolver().query(SipProfile.ACCOUNT_URI, DBProvider.ACCOUNT_FULL_PROJECTION, null, null, null);
		if (c != null) {
			try {
				c.moveToFirst();
				do {
					SipProfile account = new SipProfile(c);
					setAccountRegistration(account, 0, false);
				} while (c.moveToNext() );
			} catch (Exception e) {
				Log.e(THIS_FILE, "Error on looping over sip profiles", e);
			} finally {
				c.close();
			}
		}


		if (notificationManager != null && cancelNotification) {
			notificationManager.cancelRegisters();
		}
	}

	private void reAddAllAccounts() throws SameThreadException {
		Log.d(THIS_FILE, "RE REGISTER ALL ACCOUNTS");
		unregisterAllAccounts(false);
		addAllAccounts();
	}


	
	
	public SipProfileState getSipProfileState(int accountDbId) {
		final SipProfile acc = getAccount(accountDbId);
		if(pjService != null && acc != null) {
			return pjService.getProfileState(acc);
		}
		return null;
	}

	public void updateRegistrationsState() {
		Log.d(THIS_FILE, "Update registration state");
		ArrayList<SipProfileState> activeProfilesState = new ArrayList<SipProfileState>();
		Cursor c = getContentResolver().query(SipProfile.ACCOUNT_STATUS_URI, null, null, null, null);
		if (c != null) {
			try {
				if(c.getCount() > 0) {
					c.moveToFirst();
					do {
						SipProfileState ps = new SipProfileState(c);
						if(ps.isValidForCall()) {
							activeProfilesState.add(ps);
						}
					} while ( c.moveToNext() );
				}
			} catch (Exception e) {
				Log.e(THIS_FILE, "Error on looping over sip profiles", e);
			} finally {
				c.close();
			}
		}
		
		Collections.sort(activeProfilesState, SipProfileState.getComparator());
		
		

		// Handle status bar notification
		if (activeProfilesState.size() > 0 && 
				prefsWrapper.getPreferenceBooleanValue(SipConfigManager.ICON_IN_STATUS_BAR, true)) {
		// Testing memory / CPU leak as per issue 676
		//	for(int i=0; i < 10; i++) {
		//		Log.d(THIS_FILE, "Notify ...");
				notificationManager.notifyRegisteredAccounts(activeProfilesState, prefsWrapper.getPreferenceBooleanValue(SipConfigManager.ICON_IN_STATUS_BAR_NBR));
		//		try {
		//			Thread.sleep(6000);
		//		} catch (InterruptedException e) {
		//			e.printStackTrace();
		//		}
		//	}
		} else {
			notificationManager.cancelRegisters();
		}
		
		if(hasSomeActiveAccount) {
			acquireResources();
		}else {
			releaseResources();
		}
	}
	
	/**
	 * Get the currently instanciated prefsWrapper (to be used by
	 * UAStateReceiver)
	 * 
	 * @return the preferenceWrapper instanciated
	 */
	public PreferencesProviderWrapper getPrefs() {
		// Is never null when call so ok, just not check...
		return prefsWrapper;
	}
	
	//Binders for media manager to sip stack
	/**
	 * Adjust tx software sound level
	 * @param speakVolume volume 0.0 - 1.0
	 */
	public void confAdjustTxLevel(float speakVolume) throws SameThreadException {
		if(pjService != null) {
			pjService.confAdjustTxLevel(0, speakVolume);
		}
	}
	/**
	 * Adjust rx software sound level
	 * @param speakVolume volume 0.0 - 1.0
	 */
	public void confAdjustRxLevel(float speakVolume) throws SameThreadException {
		if(pjService != null) {
			pjService.confAdjustRxLevel(0, speakVolume);
		}
	}


    /**
     * Add a buddy to list 
     * @param buddyUri the sip uri of the buddy to add
     */
    public int addBuddy(String buddyUri) throws SameThreadException {
        int retVal = -1;
        if(pjService != null) {
            Log.d(THIS_FILE, "Trying to add buddy " + buddyUri);
            retVal = pjService.addBuddy(buddyUri);
        }
        return retVal;
    }

    /**
     * Remove a buddy from buddies
     * @param buddyUri the sip uri of the buddy to remove
     */
    public void removeBuddy(String buddyUri) throws SameThreadException  {
        if(pjService != null) {
            pjService.removeBuddy(buddyUri);
        }
    }
	
	private boolean holdResources = false;
	/**
	 * Ask to take the control of the wifi and the partial wake lock if
	 * configured
	 */
	private synchronized void acquireResources() {
		if(holdResources) {
			return;
		}
		
		// Add a wake lock for CPU if necessary
		if (prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_PARTIAL_WAKE_LOCK)) {
			PowerManager pman = (PowerManager) getSystemService(Context.POWER_SERVICE);
			if (wakeLock == null) {
				wakeLock = pman.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.csipsimple.SipService");
				wakeLock.setReferenceCounted(false);
			}
			// Extra check if set reference counted is false ???
			if (!wakeLock.isHeld()) {
				wakeLock.acquire();
			}
		}

		// Add a lock for WIFI if necessary
		WifiManager wman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (wifiLock == null) {
			int mode = WifiManager.WIFI_MODE_FULL;
			if(Compatibility.isCompatible(9) && prefsWrapper.getPreferenceBooleanValue(SipConfigManager.LOCK_WIFI_PERFS)) {
				mode = 0x3; // WIFI_MODE_FULL_HIGH_PERF 
			}
			wifiLock = wman.createWifiLock(mode, "com.csipsimple.SipService");
			wifiLock.setReferenceCounted(false);
		}
		if (prefsWrapper.getPreferenceBooleanValue(SipConfigManager.LOCK_WIFI) && !wifiLock.isHeld()) {
			WifiInfo winfo = wman.getConnectionInfo();
			if (winfo != null) {
				DetailedState dstate = WifiInfo.getDetailedStateOf(winfo.getSupplicantState());
				// We assume that if obtaining ip addr, we are almost connected
				// so can keep wifi lock
				if (dstate == DetailedState.OBTAINING_IPADDR || dstate == DetailedState.CONNECTED) {
					if (!wifiLock.isHeld()) {
						wifiLock.acquire();
					}
				}
			}
		}
		holdResources = true;
	}

	private synchronized void releaseResources() {
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
		}
		if (wifiLock != null && wifiLock.isHeld()) {
			wifiLock.release();
		}
		holdResources = false;
	}


	

	private static final int TOAST_MESSAGE = 0;

	private Handler serviceHandler = new ServiceHandler(this);
	        
	private static class ServiceHandler extends Handler {
	    WeakReference<SipService> s;
		public ServiceHandler(SipService sipService) {
		    s = new WeakReference<SipService>(sipService);
        }

        @Override
		public void handleMessage(Message msg) {
            super.handleMessage(msg);
            SipService sipService = s.get();
            if(sipService == null) {
                return;
            }
			if (msg.what == TOAST_MESSAGE) {
				if (msg.arg1 != 0) {
					Toast.makeText(sipService, msg.arg1, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(sipService, (String) msg.obj, Toast.LENGTH_LONG).show();
				}
			}
		}
	};

	
	public UAStateReceiver getUAStateReceiver() {
		return pjService.userAgentReceiver;
	}



	public int getGSMCallState() {
		return telephonyManager.getCallState();
	}

	public static final class ToCall {
		private Integer pjsipAccountId;
		private String callee;
		private String dtmf;
		
		public ToCall(Integer acc, String uri) {
			pjsipAccountId = acc;
			callee = uri;
		}
		public ToCall(Integer acc, String uri, String dtmfChars) {
            pjsipAccountId = acc;
            callee = uri;
            dtmf = dtmfChars;
        }
		/**
		 * @return the pjsipAccountId
		 */
		public Integer getPjsipAccountId() {
			return pjsipAccountId;
		}
		/**
		 * @return the callee
		 */
		public String getCallee() {
			return callee;
		}
		/**
		 * @return the dtmf sequence to automatically dial for this call
		 */
		public String getDtmf() {
            return dtmf;
        }
	};
	
	public SipProfile getAccount(long accountId) {
		// TODO : create cache at this point to not requery each time as far as it's a service query
		return SipProfile.getProfileFromDbId(this, accountId, DBProvider.ACCOUNT_FULL_PROJECTION);
	}
	

    // Auto answer feature

	public void setAutoAnswerNext(boolean auto_response) {
		autoAcceptCurrent = auto_response;
	}
	
	/**
	 * Should a current incoming call be answered.
	 * A call to this method will reset internal state
	 * @param remContact The remote contact to test
	 * @param acc The incoming guessed account
	 * @return the sip code to auto-answer with. If > 0 it means that an auto answer must be fired
	 */
	public int shouldAutoAnswer(String remContact, SipProfile acc, Bundle extraHdr) {

		Log.d(THIS_FILE, "Search if should I auto answer for " + remContact);
		int shouldAutoAnswer = 0;
		
		if(autoAcceptCurrent) {
			Log.d(THIS_FILE, "I should auto answer this one !!! ");
			autoAcceptCurrent = false;
			return 200;
		}
		
		if(acc != null) {
			Pattern p = Pattern.compile("^(?:\")?([^<\"]*)(?:\")?[ ]*(?:<)?sip(?:s)?:([^@]*@[^>]*)(?:>)?", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(remContact);
			String number = remContact;
			if (m.matches()) {
				number = m.group(2);
			}
			shouldAutoAnswer = Filter.isAutoAnswerNumber(this, acc.id, number, extraHdr);
			
		}else {
			Log.e(THIS_FILE, "Oupps... that come from an unknown account...");
			// If some user need to auto hangup if comes from unknown account, just needed to add local account and filter on it.
		}
		return shouldAutoAnswer;
	}
	
	// Media direct binders
	public void setNoSnd() throws SameThreadException {
		if (pjService != null) {
			pjService.setNoSnd();
		}
	}
	
	public void setSnd() throws SameThreadException {
		if (pjService != null) {
			pjService.setSnd();
		}
	}

	
    private static Looper createLooper() {
    //	synchronized (executorThread) {
	    	if(executorThread == null) {
	    		Log.d(THIS_FILE, "Creating new handler thread");
	    		// ADT gives a fake warning due to bad parse rule.
		        executorThread = new HandlerThread("SipService.Executor");
		        executorThread.start();
	    	}
	//	}
        return executorThread.getLooper();
    }
    
    

    // Executes immediate tasks in a single executorThread.
    // Hold/release wake lock for running tasks
    public static class SipServiceExecutor extends Handler {
        WeakReference<SipService> handlerService;
        
        SipServiceExecutor(SipService s) {
            super(createLooper());
            handlerService = new WeakReference<SipService>(s);
        }

        public void execute(Runnable task) {
            SipService s = handlerService.get();
            if(s != null) {
                s.sipWakeLock.acquire(task);
            }
            Message.obtain(this, 0/* don't care */, task).sendToTarget();
        }

        @Override
        public void handleMessage(Message msg) {
	        if (msg.obj instanceof Runnable) {
                executeInternal((Runnable) msg.obj);
            } else {
                Log.w(THIS_FILE, "can't handle msg: " + msg);
            }
        }

        private void executeInternal(Runnable task) {
            try {
                task.run();
            } catch (Throwable t) {
                Log.e(THIS_FILE, "run task: " + task, t);
            } finally {

                SipService s = handlerService.get();
                if(s != null) {
                    s.sipWakeLock.release(task);
                }
            }
        }
    }
	
    
    class StartRunnable extends SipRunnable {
		@Override
		protected void doRun() throws SameThreadException {
    		startSipStack();
    	}
    }
    

    class SyncStartRunnable extends ReturnRunnable {
        @Override
        protected Object runWithReturn() throws SameThreadException {
            startSipStack();
            return null;
        }
    }
	
    class StopRunnable extends SipRunnable {
		@Override
		protected void doRun() throws SameThreadException {
    		stopSipStack();
    	}
    }

    class SyncStopRunnable extends ReturnRunnable {
        @Override
        protected Object runWithReturn() throws SameThreadException {
            stopSipStack();
            return null;
        }
    }
    
	class RestartRunnable extends SipRunnable {
		@Override
		protected void doRun() throws SameThreadException {
			if(stopSipStack()) {
				startSipStack();
			}else {
				Log.e(THIS_FILE, "Can't stop ... so do not restart ! ");
			}
		}
	}
	
	class SyncRestartRunnable extends ReturnRunnable {
	    @Override
	    protected Object runWithReturn() throws SameThreadException {
	        if(stopSipStack()) {
                startSipStack();
            }else {
                Log.e(THIS_FILE, "Can't stop ... so do not restart ! ");
            }
	        return null;
	    }
	}
	
	class DestroyRunnable extends SipRunnable {
		@Override
		protected void doRun() throws SameThreadException {
			if(stopSipStack()) {
				stopSelf();
			}
		}
	}
	
	class FinalizeDestroyRunnable extends SipRunnable {
		@Override
		protected void doRun() throws SameThreadException {
			
			mExecutor = null;
			
			Log.d(THIS_FILE, "Destroy sip stack");
			
			sipWakeLock.reset();
			
			if(stopSipStack()) {
				notificationManager.cancelAll();
				notificationManager.cancelCalls();
			}else {
				Log.e(THIS_FILE, "Somebody has stopped the service while there is an ongoing call !!!");
			}
			/* If we activate that we can get two concurrent executorThread 
			synchronized (executorThread) {
				HandlerThread currentHandlerThread = executorThread;
				executorThread = null;
				System.gc();
				// This is a little bit crappy, we are cutting were we sit.
				Threading.stopHandlerThread(currentHandlerThread, false);
			}
			*/
			
			// We will not go longer
			Log.i(THIS_FILE, "--- SIP SERVICE DESTROYED ---");
		}
	}
	
	// Enforce same thread contract to ensure we do not call from somewhere else
	public class SameThreadException extends Exception {
		private static final long serialVersionUID = -905639124232613768L;

		public SameThreadException() {
			super("Should be launched from a single worker thread");
		}
	}

	public abstract static class SipRunnable  implements Runnable {
		protected abstract void doRun() throws SameThreadException;
		
		public void run() {
			try {
				doRun();
			}catch(SameThreadException e) {
				Log.e(THIS_FILE, "Not done from same thread");
			}
		}
	}
	

    public abstract class ReturnRunnable extends SipRunnable {
    	private Semaphore runSemaphore;
    	private Object resultObject;
    	
    	public ReturnRunnable() {
			super();
			runSemaphore = new Semaphore(0);
		}
    	
    	public Object getResult() {
    		try {
				runSemaphore.acquire();
			} catch (InterruptedException e) {
				Log.e(THIS_FILE, "Can't acquire run semaphore... problem...");
			}
    		return resultObject;
    	}
    	
    	protected abstract Object runWithReturn() throws SameThreadException;
    	
    	@Override
    	public void doRun() throws SameThreadException {
    		setResult(runWithReturn());
    	}
    	
    	private void setResult(Object obj) {
    		resultObject = obj;
    		runSemaphore.release();
    	}
    }
    
    private static String UI_CALL_PACKAGE = null;
    public static Intent buildCallUiIntent(Context ctxt, SipCallSession callInfo) {
        // Resolve the package to handle call.
        if(UI_CALL_PACKAGE == null) {
            UI_CALL_PACKAGE = ctxt.getPackageName();
            try {
                Map<String, DynActivityPlugin> callsUis = ExtraPlugins.getDynActivityPlugins(ctxt, SipManager.ACTION_SIP_CALL_UI);
                String preferredPackage  = SipConfigManager.getPreferenceStringValue(ctxt, SipConfigManager.CALL_UI_PACKAGE, UI_CALL_PACKAGE);
                String packageName = null;
                boolean foundPref = false;
                for(String activity : callsUis.keySet()) {
                    packageName = activity.split("/")[0];
                    if(preferredPackage.equalsIgnoreCase(packageName)) {
                        UI_CALL_PACKAGE = packageName;
                        foundPref = true;
                        break;
                    }
                }
                if(!foundPref && !TextUtils.isEmpty(packageName)) {
                    UI_CALL_PACKAGE = packageName;
                }
            }catch(Exception e) {
                Log.e(THIS_FILE, "Error while resolving package", e);
            }
        }
        SipCallSession toSendInfo = new SipCallSession(callInfo);
        Intent intent = new Intent(SipManager.ACTION_SIP_CALL_UI);
        intent.putExtra(SipManager.EXTRA_CALL_INFO, toSendInfo);
        intent.setPackage(UI_CALL_PACKAGE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    public static Intent buildCallUiBlankIntent(Context ctxt, SipCallSession callInfo) {
        // Resolve the package to handle call.
        if(UI_CALL_PACKAGE == null) {
            UI_CALL_PACKAGE = ctxt.getPackageName();
            try {
                Map<String, DynActivityPlugin> callsUis = ExtraPlugins.getDynActivityPlugins(ctxt, SipManager.ACTION_SIP_CALL_UI);
                String preferredPackage  = SipConfigManager.getPreferenceStringValue(ctxt, SipConfigManager.CALL_UI_PACKAGE, UI_CALL_PACKAGE);
                String packageName = null;
                boolean foundPref = false;
                for(String activity : callsUis.keySet()) {
                    packageName = activity.split("/")[0];
                    if(preferredPackage.equalsIgnoreCase(packageName)) {
                        UI_CALL_PACKAGE = packageName;
                        foundPref = true;
                        break;
                    }
                }
                if(!foundPref && !TextUtils.isEmpty(packageName)) {
                    UI_CALL_PACKAGE = packageName;
                }
            }catch(Exception e) {
                Log.e(THIS_FILE, "Error while resolving package", e);
            }
        }
        SipCallSession toSendInfo = new SipCallSession(callInfo);
        Intent intent = new Intent();
        intent.putExtra(SipManager.EXTRA_CALL_INFO, toSendInfo);
        intent.setPackage(UI_CALL_PACKAGE);
        return intent;
    }
    
    
    public static void setVideoWindow(int callId, SurfaceView window, boolean local) {
        if(singleton != null) {
            if(local) {
                singleton.setCaptureVideoWindow(window);
            }else {
                singleton.setRenderVideoWindow(callId, window);
            }
        }
    }

    private void setRenderVideoWindow(final int callId, final SurfaceView window) {
        getExecutor().execute(new SipRunnable() {
            @Override
            protected void doRun() throws SameThreadException {
                pjService.setVideoAndroidRenderer(callId, window);
            }
        });
    }
    private void setCaptureVideoWindow(final SurfaceView window) {
        getExecutor().execute(new SipRunnable() {
            @Override
            protected void doRun() throws SameThreadException {
                pjService.setVideoAndroidCapturer(window);
            }
        });
    }
    
    private PresenceStatus presence = SipManager.PresenceStatus.ONLINE;
    /**
     * Get current last status for the user
     * @return
     */
    public PresenceStatus getPresence() {
        return presence;
    }

}
