package net.gnobal.dindy;

public class Consts {

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
			static final String KEY_FIRST_STARTUP = "first_startup";
			static final String KEY_SHOW_STARTUP_MESSAGE =
				"show_startup_message";
		}
		
		class Widget
		{
			static final String NAME = "widget_preferences";
			static final String KEY_TYPE = "_type";
			static final String KEY_PROFILE_ID = "_profile";
			
			class Type
			{
			    // Never change these values - they're kept in preferences
			    final static int INVALID = -1;
			    final static int SINGLE_PROFILE = 1;
			    final static int ALL_PROFILES = 2;
			}
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

	/*
	class Action
	{
		static final String PROFILE_STARTED =
			"net.gnobal.dindy.action.PROFILE_STARTED";
		static final String PROFILE_STOPPED =
			"net.gnobal.dindy.action.PROFILE_STARTED";
	}*/
	public static final long NOT_A_PROFILE_ID = -1;
	static final long INFINITE_TIME = -1;
	static final long MILLIS_IN_MINUTE = 60000;
	static final String LOGTAG = "Dindy";
	static final boolean DEBUG = true;
	static final String EMPTY_STRING = "";
	public static final String EXTRA_SELECTED_PROFILE_ID = 
		"selected_profile_id";
}
