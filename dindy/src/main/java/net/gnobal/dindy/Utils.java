package net.gnobal.dindy;

import android.media.AudioManager;
import android.util.Log;

import java.util.Calendar;

class Utils {
	static String incomingCallStateToString(int state) {
		switch (state) {
		case Consts.IncomingCallState.IDLE: return "IDLE";
		case Consts.IncomingCallState.OFFHOOK: return "OFFHOOK";
		case Consts.IncomingCallState.RINGING: return "RINGING";
		default: return Integer.toString(state);
		}
	}

	static String vibrationSettingToString(int vibrationSetting) {
		switch (vibrationSetting) {
		case AudioManagerCompat.VIBRATE_SETTING_OFF: return "OFF";
		case AudioManagerCompat.VIBRATE_SETTING_ON: return "ON";
		case AudioManagerCompat.VIBRATE_SETTING_ONLY_SILENT: return "ONLY_SILENT";
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

	static long diffTimeMillis(Calendar c, int toHour, int toMinute,
	                           boolean adjustToMinuteStart) {
		Calendar selected = Calendar.getInstance();
		selected.set(Calendar.HOUR_OF_DAY, toHour);
		selected.set(Calendar.MINUTE, toMinute);
		if (c.get(Calendar.HOUR_OF_DAY) == toHour &&
				c.get(Calendar.MINUTE) == toMinute) {
			Log.d(Consts.LOGTAG, "current time selected");
			return 0;
		}

		if (adjustToMinuteStart) {
			selected.set(Calendar.SECOND, 0);
			selected.set(Calendar.MILLISECOND, 0);
		}
		if (selected.before(c)) {
			selected.add(Calendar.DATE, 1);
		}

		return selected.getTimeInMillis() - c.getTimeInMillis();
	}

	// Returns zero if the time limit is now (so there's no reason to start the profile)
	static long getTimeLimitMillis(int selectedTimeLimitType, int selectedTimeLimitHours,
	                               int selectedTimeLimitMinutes) {
		long timeLimitMillis = 0;
		if (selectedTimeLimitType == Consts.Prefs.Profile.TimeLimitType.DURATION) {
			timeLimitMillis = (selectedTimeLimitHours * Consts.MINUTES_IN_HOUR
					+ selectedTimeLimitMinutes) * Consts.MILLIS_IN_MINUTE;
		} else {
			Calendar now = Calendar.getInstance();
			timeLimitMillis = diffTimeMillis(
					now,
					selectedTimeLimitHours,
					selectedTimeLimitMinutes,
					true);
			if (timeLimitMillis != 0) {
				// Adding 3 seconds to the calculation, to make sure the minute
				// doesn't switch exactly when the user starts the profile,
				// causing Dindy to stop only )after nearly 24 hours
				timeLimitMillis += 3000;
			}
		}

		return timeLimitMillis;
	}
}