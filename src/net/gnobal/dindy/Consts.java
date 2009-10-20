package net.gnobal.dindy;

class Consts {

	class IncomingCallState
	{
		static final int IDLE = 0;
		static final int RINGING = 1;
		static final int OFFHOOK = 2;
		static final int MISSED = 3; // a missed call
		// an incoming call (an indication of a call that was either accepted or
		// declined)
		static final int INCOMING = 4;
	}

	class Prefs
	{
		class Main
		{
			static final String NAME = "main_preferences";
			static final String LAST_USED_PROFILE_ID = "last_profile_id";
			//static final String LAST_USED_PROFILE_ID_LONG =
			//	"last_profile_id_long";
			
			static final String KEY_PROFILES = "profiles";
			static final String KEY_FIRST_STARTUP = "first_startup";
			static final String KEY_SHOW_STARTUP_MESSAGE =
				"show_startup_message";
		}
		
		class Profile
		{
			static final String PREFIX = "_profile_";
			static final String KEY_TIME_BETWEEN_CALLS_MINUTES =
				"time_between_calls";
			
			static final String KEY_TREAT_UNKNOWN_CALLERS =
				"treat_unknown_callers";
			// NOTE the following 3 values match values in the array resource
			// unknown_caller_behavior_array_values. DO NOT change them unless
			// you know what you're doing
			static final String VALUE_TREAT_UNKNOWN_CALLERS_AS_FIRST =
				"first";
			static final String VALUE_TREAT_UNKNOWN_CALLERS_AS_SECOND =
				"second";
			static final String VALUE_TREAT_UNKNOWN_CALLERS_AS_NORMAL =
				"normal";
			static final String KEY_ENABLE_SMS = "enable_sms";			
			static final String KEY_SMS_MESSAGE = 
				"sms_message";
			static final String KEY_FIRST_RING_SOUND = 
				"first_ring_sound";
			static final String KEY_FIRST_RING_VIBRATE = 
				"first_ring_vibrate";
			static final String KEY_SECOND_RING_SOUND = 
				"second_ring_sound";
			static final String KEY_SECOND_RING_VIBRATE = 
				"second_ring_vibrate";

			// Default values for profile preferences
			static final boolean VALUE_ENABLE_SMS_DEFAULT = true; 
			static final boolean VALUE_FIRST_RING_SOUND_DEFAULT = false; 
			static final boolean VALUE_FIRST_RING_VIBRATE_DEFAULT = false; 
			static final boolean VALUE_SECOND_RING_SOUND_DEFAULT = true; 
			static final boolean VALUE_SECOND_RING_VIBRATE_DEFAULT = true; 
			// NOTE the following value must be one of the values in the array
			// resource time_between_calls_array_values
			static final String VALUE_TIME_BETWEEN_CALLS_DEFAULT = "5";
			static final String VALUE_TREAT_UNKNOWN_CALLERS_DEFAULT =
				VALUE_TREAT_UNKNOWN_CALLERS_AS_FIRST;

		}
	}
	
	static final long NOT_A_PROFILE_ID = -1;
	static final long INFINITE_TIME = -1;
	static final long MILLIS_IN_MINUTE = 60000;
	static final String LOGTAG = "Dindy";
	static final boolean DEBUG = false;
}
