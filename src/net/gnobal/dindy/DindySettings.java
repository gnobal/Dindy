package net.gnobal.dindy;

class DindySettings {
	class RingerVibrateSettings {
		int mVibrateModeNotification;
		int mVibrateModeRinger;
		int mRingerMode;
	}
	
	RingerVibrateSettings mUserSettings = new RingerVibrateSettings(); 
	RingerVibrateSettings mFirstRingSettings = new RingerVibrateSettings(); 
	RingerVibrateSettings mSecondRingSettings = new RingerVibrateSettings(); 
	boolean mEnableSms;
	String mMessage;
	long mWakeupTimeoutMillis = 0;
	String mTreatUnknownCallers;
}
