/**
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.csipsimple.ui;

import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsua_msg_data;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.method.DialerKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import com.csipsimple.R;

public class Dialer extends Activity implements OnClickListener,
		OnLongClickListener {

	

	//private static final int TONE_LENGTH_MS = 150;
	private ToneGenerator mToneGenerator;
	private Object mToneGeneratorLock = new Object();
	private static final int TONE_RELATIVE_VOLUME = 80;
	private static final int DIAL_TONE_STREAM_TYPE = AudioManager.STREAM_MUSIC;

	private static final String THIS_FILE = "Dialer";

	private Drawable mDigitsBackground;
	private Drawable mDigitsEmptyBackground;
	private EditText digitsView;
	private ImageButton dialButton, deleteButton;

	//private Vibrator mVibrator;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dialer_activity);

		Resources r = getResources();
		mDigitsBackground = r.getDrawable(R.drawable.btn_dial_textfield_active);
		mDigitsEmptyBackground = r.getDrawable(R.drawable.btn_dial_textfield_normal);
		dialButton = (ImageButton) findViewById(R.id.dialButton);
		deleteButton = (ImageButton) findViewById(R.id.deleteButton);
		digitsView = (EditText) findViewById(R.id.digitsText);

		initButtons();
		

	}

	private void setupButton(int id) {
		ImageButton button = (ImageButton) findViewById(id);
		button.setOnClickListener(this);

		if (id == R.id.button0 || id == R.id.button1 || id == R.id.deleteButton) {
			button.setOnLongClickListener(this);
		}
	}

	private void initButtons() {
		setupButton(R.id.button0);
		setupButton(R.id.button1);
		setupButton(R.id.button2);
		setupButton(R.id.button3);
		setupButton(R.id.button4);
		setupButton(R.id.button5);
		setupButton(R.id.button6);
		setupButton(R.id.button7);
		setupButton(R.id.button8);
		setupButton(R.id.button9);
		setupButton(R.id.buttonstar);
		setupButton(R.id.buttonpound);
		setupButton(R.id.dialButton);
		setupButton(R.id.deleteButton);

		digitsView.setOnClickListener(this);
		digitsView.setKeyListener(DialerKeyListener.getInstance());
		digitsView.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
		digitsView.setInputType(android.text.InputType.TYPE_NULL);
		toggleDrawable();
	}

	

	@Override
	protected void onResume() {
		super.onResume();

		// if the mToneGenerator creation fails, just continue without it. It is
		// a local audio signal, and is not as important as the dtmf tone
		// itself.
		synchronized (mToneGeneratorLock) {
			if (mToneGenerator == null) {
				try {
					// we want the user to be able to control the volume of the
					// dial tones
					// outside of a call, so we use the stream type that is also
					// mapped to the
					// volume control keys for this activity
					mToneGenerator = new ToneGenerator(DIAL_TONE_STREAM_TYPE,
							TONE_RELATIVE_VOLUME);
					setVolumeControlStream(DIAL_TONE_STREAM_TYPE);
				} catch (RuntimeException e) {
					mToneGenerator = null;
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		synchronized (mToneGeneratorLock) {
			if (mToneGenerator != null) {
				mToneGenerator.release();
				mToneGenerator = null;
			}
		}
	}

	void playTone(int tone) {
		// if (!true) {
		// return;
		// }

		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int ringerMode = audioManager.getRingerMode();
		if ((ringerMode == AudioManager.RINGER_MODE_SILENT)
				|| (ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
			return;
		}

		synchronized (mToneGeneratorLock) {
			if (mToneGenerator == null) {
				return;
			}
			//mToneGenerator.startTone(tone, TONE_LENGTH_MS);
		}
	}

	private void keyPressed(int keyCode) {
		// vibrate();
		KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
		digitsView.onKeyDown(keyCode, event);
	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.button0: {
			playTone(ToneGenerator.TONE_DTMF_0);
			keyPressed(KeyEvent.KEYCODE_0);
			break;
		}
		case R.id.button1: {
			playTone(ToneGenerator.TONE_DTMF_1);
			keyPressed(KeyEvent.KEYCODE_1);
			break;
		}
		case R.id.button2: {
			playTone(ToneGenerator.TONE_DTMF_2);
			keyPressed(KeyEvent.KEYCODE_2);
			break;
		}
		case R.id.button3: {
			playTone(ToneGenerator.TONE_DTMF_3);
			keyPressed(KeyEvent.KEYCODE_3);
			break;
		}
		case R.id.button4: {
			playTone(ToneGenerator.TONE_DTMF_4);
			keyPressed(KeyEvent.KEYCODE_4);
			break;
		}
		case R.id.button5: {
			playTone(ToneGenerator.TONE_DTMF_5);
			keyPressed(KeyEvent.KEYCODE_5);
			break;
		}
		case R.id.button6: {
			playTone(ToneGenerator.TONE_DTMF_6);
			keyPressed(KeyEvent.KEYCODE_6);
			break;
		}
		case R.id.button7: {
			playTone(ToneGenerator.TONE_DTMF_7);
			keyPressed(KeyEvent.KEYCODE_7);
			break;
		}
		case R.id.button8: {
			playTone(ToneGenerator.TONE_DTMF_8);
			keyPressed(KeyEvent.KEYCODE_8);
			break;
		}
		case R.id.button9: {
			playTone(ToneGenerator.TONE_DTMF_9);
			keyPressed(KeyEvent.KEYCODE_9);
			break;
		}
		case R.id.buttonpound: {
			playTone(ToneGenerator.TONE_DTMF_P);
			keyPressed(KeyEvent.KEYCODE_POUND);
			break;
		}
		case R.id.buttonstar: {
			playTone(ToneGenerator.TONE_DTMF_S);
			keyPressed(KeyEvent.KEYCODE_STAR);
			break;
		}
		case R.id.deleteButton: {
			keyPressed(KeyEvent.KEYCODE_DEL);
			break;
		}
		case R.id.dialButton: {
			doCall();
			break;
		}
		case R.id.digitsText: {
			digitsView.setCursorVisible(false);
			if (digitsView.length() != 0) {
				digitsView.setCursorVisible(true);
			}
			break;
		}
		}
		toggleDrawable();
	}

	public boolean onLongClick(View view) {
		boolean result = false;
		switch (view.getId()) {
			case R.id.button0: {
				keyPressed(KeyEvent.KEYCODE_PLUS);
				result = true;
				break;
			}
			case R.id.deleteButton: {
				removeAll();
				deleteButton.setPressed(false);
				result = true;
				break;
			}
		}
		toggleDrawable();
		return result;
	}

	private void removeAll() {
		digitsView.getText().clear();
	}

	private void doCall() {
		byte[] user_data = new byte[1];
		pjsua_msg_data msg = new pjsua_msg_data();
		int[] call_id = new int[1];

		pjsua.call_make_call(0, pjsua.pj_str_copy("sip:"
				+ digitsView.getText().toString() + "@freephonie.net"), 0,
				user_data, msg, call_id);
	}

	private void toggleDrawable() {
		final boolean notEmpty = digitsView.length() != 0;
		Log.d(THIS_FILE, "Here : "+notEmpty);
		if (notEmpty) {
			digitsView.setBackgroundDrawable(mDigitsBackground);
			dialButton.setEnabled(true);
			deleteButton.setEnabled(true);
		} else {
			digitsView.setCursorVisible(false);
			digitsView.setBackgroundDrawable(mDigitsEmptyBackground);
			dialButton.setEnabled(false);
			deleteButton.setEnabled(false);
		}
	}

}
