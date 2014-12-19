package net.gnobal.dindy;

import android.media.AudioManager;

// This class holds all the deprecated stuff from AudioManager that we still have to use.
// The Android documentation says about the methods:
// "This method should only be used by applications that replace the platform-wide management
// of audio settings or the main telephony application."
// Dindy answers that definition, so we have no choice but to use this deprecated functionality
@SuppressWarnings("deprecation")
class AudioManagerCompat {
	final static int VIBRATE_TYPE_RINGER = AudioManager.VIBRATE_TYPE_RINGER;
	final static int VIBRATE_TYPE_NOTIFICATION = AudioManager.VIBRATE_TYPE_NOTIFICATION;
	final static int VIBRATE_SETTING_ON = AudioManager.VIBRATE_SETTING_ON;
	final static int VIBRATE_SETTING_OFF = AudioManager.VIBRATE_SETTING_OFF;
	final static int VIBRATE_SETTING_ONLY_SILENT = AudioManager.VIBRATE_SETTING_ONLY_SILENT;

	static int getVibrateSetting(AudioManager am, int vibrateType) {
		return am.getVibrateSetting(vibrateType);
	}

	static void setVibrateSetting(AudioManager am, int vibrateType, int vibrateSetting) {
		am.setVibrateSetting(vibrateType, vibrateSetting);
	}
}