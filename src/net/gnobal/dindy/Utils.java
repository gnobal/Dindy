package net.gnobal.dindy;

import android.media.AudioManager;

class Utils {
	static String incomingCallStateToString(int state) {
		switch (state) {
		case Consts.IncomingCallState.IDLE: return "IDLE";
		case Consts.IncomingCallState.OFFHOOK: return "OFFHOOK";
		case Consts.IncomingCallState.RINGING: return "RINGING";
		default: return Integer.toString(state);
		}
	}

	@SuppressWarnings("deprecation")
	static String vibrationSettingToString(int vibrationSetting) {
		switch (vibrationSetting) {
		case AudioManager.VIBRATE_SETTING_OFF: return "OFF";
		case AudioManager.VIBRATE_SETTING_ON: return "ON";
		case AudioManager.VIBRATE_SETTING_ONLY_SILENT: return "ONLY_SILENT";
		default: return Integer.toString(vibrationSetting);
		}
	}

	static String ringerModeToString(int ringerMode) {
		switch (ringerMode) {
		case AudioManager.RINGER_MODE_NORMAL: return "NORMAL";
		case AudioManager.RINGER_MODE_SILENT: return "SILENT";
		case AudioManager.RINGER_MODE_VIBRATE: return "VIBRATE";
		default: return Integer.toString(ringerMode);
		}
	}
}