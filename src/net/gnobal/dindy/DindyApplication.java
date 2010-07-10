package net.gnobal.dindy;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class DindyApplication extends Application {
    public void onCreate() {
    	super.onCreate();
    	
    	ProfilePreferencesHelper.createInstance(getApplicationContext());
    	
		SharedPreferences preferences = getSharedPreferences(
				Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
		if (preferences.getBoolean(Consts.Prefs.Main.KEY_FIRST_STARTUP, true)) {
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean(Consts.Prefs.Main.KEY_FIRST_STARTUP, false);
			editor.commit();
			createProfile(R.string.default_profile_away_name,
					R.string.default_profile_away_sms_message, false, false,
					false, false, Consts.INFINITE_TIME,
					Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_FIRST,
					false, Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_TYPE,
					Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_HOURS,
					Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_MINUTES);
			createProfile(R.string.default_profile_busy_name,
					R.string.default_profile_busy_sms_message, false, false,
					false, false, Consts.INFINITE_TIME,
					Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_FIRST,
					false, Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_TYPE,
					Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_HOURS,
					Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_MINUTES);
			createProfile(R.string.default_profile_car_name,
					R.string.default_profile_car_sms_message, false, false,
					true, false, 5,
					Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_NORMAL,
					false, Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_TYPE,
					Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_HOURS,
					Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_MINUTES);
			createProfile(R.string.default_profile_meeting_name,
					R.string.default_profile_meeting_sms_message, false, false,
					false, true, 10,
					Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_FIRST,
					false, Consts.Prefs.Profile.TimeLimitType.DURATION,
					1, 0);
			createProfile(R.string.default_profile_night_name,
					R.string.default_profile_night_sms_message, false, false,
					true, true, Consts.INFINITE_TIME,
					Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_SECOND,
					false, Consts.Prefs.Profile.TimeLimitType.TIME_OF_DAY,
					7, 30);
		}
    }
    
	private void createProfile(int profileNameResource, int textMessageResource, 
			boolean firstRingPlaySound, boolean firstRingVibrate,
			boolean secondRingPlaySound, boolean secondRingVibrate,
			long timeBetweenCallsMinutes,
			String treatUnknownAndNonMobileCallers,
			boolean useTimeLimit, int timeLimitType, long hour, long minute) {
		String profileName = getString(profileNameResource);
		ProfilePreferencesHelper prefsHelper =
			ProfilePreferencesHelper.instance();
		if (prefsHelper.profileExists(profileName)) {
			return;
		}
		long newProfileId = prefsHelper.createNewProfile(
				profileName);
		if (newProfileId == Consts.NOT_A_PROFILE_ID) {
			return;
		}
		SharedPreferences newProfilePrefs =
			prefsHelper.getPreferencesForProfile(this, newProfileId,
					MODE_PRIVATE);
		SharedPreferences.Editor editor = newProfilePrefs.edit();
		editor.putBoolean(Consts.Prefs.Profile.KEY_ENABLE_SMS, true);
		editor.putString(Consts.Prefs.Profile.KEY_SMS_MESSAGE,
				getString(textMessageResource));
		editor.putBoolean(Consts.Prefs.Profile.KEY_FIRST_RING_SOUND,
				firstRingPlaySound);
		editor.putBoolean(Consts.Prefs.Profile.KEY_FIRST_RING_VIBRATE,
				firstRingVibrate);
		editor.putBoolean(Consts.Prefs.Profile.KEY_SECOND_RING_SOUND,
				secondRingPlaySound);
		editor.putBoolean(Consts.Prefs.Profile.KEY_SECOND_RING_VIBRATE,
				secondRingVibrate);
		editor.putString(Consts.Prefs.Profile.KEY_TIME_BETWEEN_CALLS_MINUTES,
				Long.toString(timeBetweenCallsMinutes));
		editor.putString(Consts.Prefs.Profile.KEY_TREAT_NON_MOBILE_CALLERS,
				treatUnknownAndNonMobileCallers);
		editor.putString(Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_CALLERS,
				treatUnknownAndNonMobileCallers);
		editor.putBoolean(Consts.Prefs.Profile.KEY_USE_TIME_LIMIT, useTimeLimit);
		editor.putInt(Consts.Prefs.Profile.KEY_LAST_TIME_LIMIT_TYPE, timeLimitType);
		editor.putLong(Consts.Prefs.Profile.KEY_LAST_TIME_LIMIT_HOURS, hour);
		editor.putLong(Consts.Prefs.Profile.KEY_LAST_TIME_LIMIT_MINUTES, minute);
		editor.commit();
	}
}
