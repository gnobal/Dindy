package net.gnobal.dindy;

class DindySettings {
	class RingerVibrateSettings {
		int mVibrateModeNotification;
		int mVibrateModeRinger;
		int mRingerMode;
	}
	
	static class WidgetSettings {
		int mWidgetType = Consts.Prefs.Widget.Type.INVALID;
		// Single profile settings
		long mProfileId = Consts.NOT_A_PROFILE_ID;
	}
	
	RingerVibrateSettings mUserSettings = new RingerVibrateSettings(); 
	RingerVibrateSettings mFirstRingSettings = new RingerVibrateSettings(); 
	RingerVibrateSettings mSecondRingSettings = new RingerVibrateSettings(); 
	boolean mEnableSms;
	String mMessage;
	long mWakeupTimeoutMillis = 0;
	String mTreatUnknownCallers;
}
