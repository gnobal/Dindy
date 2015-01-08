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
	
	final RingerVibrateSettings mUserSettings = new RingerVibrateSettings();
	final RingerVibrateSettings mFirstRingSettings = new RingerVibrateSettings();
	final RingerVibrateSettings mSecondRingSettings = new RingerVibrateSettings();
	boolean mEnableSmsReplyToCall;
	String mMessageToCallers;
	boolean mEnableSmsReplyToSms;
	String mMessageToTexters;
	long mWakeupTimeoutMillis = 0;
	String mTreatNonMobileCallers;
	String mTreatUnknownCallers;
	String mTreatUnknownTexters;
	String mTreatWhitelistCallers;
}
