package net.gnobal.dindy;

import android.media.AudioManager;
//import android.app.ActivityManager;
//import android.content.Context;
//import java.util.Iterator;
//import java.util.List;

class Utils {
//	static boolean isDindyServiceRunning(Context context) {
//		ActivityManager am = (ActivityManager) 
//			context.getSystemService(Context.ACTIVITY_SERVICE);
//		List<ActivityManager.RunningServiceInfo> serviceList = am
//				.getRunningServices(Integer.MAX_VALUE);
//		Iterator<ActivityManager.RunningServiceInfo> it = serviceList
//				.iterator();
//		while (it.hasNext()) {
//			ActivityManager.RunningServiceInfo info = it.next();
//			if (info.service.getClassName()
//					.equals(DindyService.class.getName())) {
//				return true;
//			}
//		}
//
//		return false;
//	}
	
	static String incomingCallStateToString(int state) {
		switch (state) {
		case Consts.IncomingCallState.IDLE: return "IDLE";
		case Consts.IncomingCallState.INCOMING: return "INCOMING";
		case Consts.IncomingCallState.MISSED: return "MISSED";
		case Consts.IncomingCallState.OFFHOOK: return "OFFHOOK";
		case Consts.IncomingCallState.RINGING: return "RINGING";
		default: return Integer.toString(state);
		}
	}
	
//	static String streamTypeToString(int streamType) {
//		switch (streamType) {
//		case AudioManager.STREAM_ALARM: return "ALARM";
//		case AudioManager.STREAM_MUSIC: return "MUSIC";
//		case AudioManager.STREAM_NOTIFICATION: return "NOTIFICATION";
//		case AudioManager.STREAM_RING: return "RING";
//		case AudioManager.STREAM_SYSTEM: return "SYSTEM";
//		case AudioManager.STREAM_VOICE_CALL: return "VOICE_CALL";
//		default: return Integer.toString(streamType);
//		}
//	}

//	static String vibrationTypeToString(int vibrationType) {
//		switch (vibrationType) {
//		case AudioManager.VIBRATE_TYPE_NOTIFICATION: return "NOTIFICATION";
//		case AudioManager.VIBRATE_TYPE_RINGER: return "RINGER";
//		default: return Integer.toString(vibrationType);
//		}
//	}

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
