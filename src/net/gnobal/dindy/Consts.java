package net.gnobal.dindy;

public class Consts {

	static class IncomingCallState
	{
		static final int IDLE = 0;
		static final int RINGING = 1;
		static final int OFFHOOK = 2;
	}

	public static class Prefs
	{
		public static class Main
		{
			public static final String NAME = "main_preferences";
			static final String LAST_USED_PROFILE_ID = "last_profile_id";
			static final String KEY_FIRST_STARTUP = "first_startup";
			static final String KEY_SHOW_STARTUP_MESSAGE =
				"show_startup_message";
			public static final String KEY_SHOW_LOCALE_USAGE = "show_locale_usage";
			public static final String KEY_SHOW_SHORTCUTS_USAGE = "show_shortcuts_usage";
			public static final String KEY_SHOW_WHITELIST_USAGE = "show_whitelist_usage";
			
			// The following keys should match the keys associated with EXTRA_* below
			public static final String KEY_LAST_STARTUP_PROFILE_ID =
				"last_startup_profile_id";
			public static final String KEY_LAST_STARTUP_PROFILE_NAME =
				"last_startup_profile_name";
			public static final String KEY_LAST_STARTUP_INTENT_SOURCE =
				"last_startup_intent_source";
			// The time limit as expressed in absolute time in the future (same as
			// System.currentTimeMillis())
			public static final String KEY_LAST_STARTUP_INTENT_ABS_TIME_LIMIT_MILLIS =
				"last_startup_time_limit";
			
			public static final String KEY_LAST_STARTUP_VIBRATE_TYPE_NOTIFICATION =
				"last_startup_vibrate_type_notification";
			public static final String KEY_LAST_STARTUP_VIBRATE_TYPE_RINGER =
				"last_startup_vibrate_type_ringer";
			public static final String KEY_LAST_STARTUP_RINGER_MODE =
				"last_startup_ringer_mode";
		}
		
		static class Widget
		{
			static final String NAME = "widget_preferences";
			static final String KEY_TYPE = "_type";
			static final String KEY_PROFILE_ID = "_profile";
			
			static class Type
			{
			    // Never change these values - they're kept in preferences
			    final static int INVALID = -1;
			    final static int SINGLE_PROFILE = 1;
			    final static int ALL_PROFILES = 2;
			}
		}
		
		static class Profile
		{
			static final String PREFIX = "_profile_";
			static final String KEY_TIME_BETWEEN_EVENTS_MINUTES =
				"time_between_calls";
			
			static final String KEY_TREAT_NON_MOBILE_CALLERS =
				"treat_non_mobile_callers";
			static final String KEY_TREAT_UNKNOWN_CALLERS =
				"treat_unknown_callers";
			static final String KEY_TREAT_UNKNOWN_TEXTERS =
				"treat_unknown_texters";
			static final String KEY_TREAT_WHITELIST_CALLERS =
				"treat_whitelist_callers";
			// NOTE the following 3 values match values in the array resource
			// unknown_caller_behavior_array_values. DO NOT change them unless
			// you know what you're doing
			static final String VALUE_TREAT_UNKNOWN_CALLERS_AS_FIRST =
				"first";
			static final String VALUE_TREAT_UNKNOWN_CALLERS_AS_SECOND =
				"second";
			static final String VALUE_TREAT_UNKNOWN_CALLERS_AS_NORMAL =
				"normal";
			static final String VALUE_TREAT_UNKNOWN_CALLERS_AS_MOBILE_NO_SMS =
				"mobile_no_sms";
			//static final String VALUE_TREAT_UNKNOWN_TEXTERS_AS_MOBILE_WITH_SMS =
			//	"mobile_with_sms";
			static final String VALUE_TREAT_UNKNOWN_TEXTERS_IGNORE =
				"ignore";
			static final String VALUE_TREAT_UNKNOWN_TEXTERS_AS_MOBILE_NO_SMS =
				"mobile_no_sms";
			static final String VALUE_TREAT_WHITELIST_CALLERS_AS_SECOND =
				"second";
			static final String VALUE_TREAT_WHITELIST_CALLERS_AS_NORMAL =
				"normal";
			static final String VALUE_TREAT_WHITELIST_CALLERS_AS_NO_PRIORITY =
				"no_priority";
			static final String KEY_ENABLE_SMS_CALLERS = "enable_sms";			
			static final String KEY_SMS_MESSAGE_CALLERS = "sms_message";
			static final String KEY_ENABLE_SMS_TEXTERS = "enable_sms_texters";
			static final String KEY_SMS_MESSAGE_TEXTERS = "sms_message_texters";
			static final String KEY_FIRST_EVENT_SOUND = "first_ring_sound";
			static final String KEY_FIRST_EVENT_VIBRATE = "first_ring_vibrate";
			static final String KEY_SECOND_EVENT_SOUND =  "second_ring_sound";
			static final String KEY_SECOND_EVENT_VIBRATE = "second_ring_vibrate";
			static class TimeLimitType
			{
				static final int DURATION = 1;
				static final int TIME_OF_DAY = 2;
			}
			
			static final String KEY_USE_TIME_LIMIT = "use_time_limit";
			static final String KEY_LAST_TIME_LIMIT_TYPE =
				"last_time_limit_type";
			static final String KEY_LAST_TIME_LIMIT_HOURS = 
				"last_time_limit_hours";
			static final String KEY_LAST_TIME_LIMIT_MINUTES =
				"last_time_limit_minutes";
			
			// Default values for profile preferences
			static final boolean VALUE_ENABLE_SMS_CALLERS_DEFAULT = true; 
			static final boolean VALUE_ENABLE_SMS_TEXTERS_DEFAULT = true; 
			static final boolean VALUE_FIRST_EVENT_SOUND_DEFAULT = false; 
			static final boolean VALUE_FIRST_EVENT_VIBRATE_DEFAULT = false; 
			static final boolean VALUE_SECOND_EVENT_SOUND_DEFAULT = true; 
			static final boolean VALUE_SECOND_EVENT_VIBRATE_DEFAULT = true; 
			// NOTE the following value must be one of the values in the array
			// resource time_between_calls_array_values
			static final String VALUE_TIME_BETWEEN_EVENTS_DEFAULT = "5";
			static final String VALUE_TREAT_UNKNOWN_CALLERS_DEFAULT =
				VALUE_TREAT_UNKNOWN_CALLERS_AS_FIRST;
			static final String VALUE_TREAT_NON_MOBILE_CALLERS_DEFAULT =
				VALUE_TREAT_UNKNOWN_CALLERS_DEFAULT;
			static final String VALUE_TREAT_UNKNOWN_TEXTERS_DEFAULT =
				VALUE_TREAT_UNKNOWN_TEXTERS_IGNORE;
			static final String VALUE_TREAT_WHITELIST_CALLERS_DEFAULT =
				VALUE_TREAT_WHITELIST_CALLERS_AS_SECOND;
			static final int VALUE_LAST_TIME_LIMIT_DEFAULT_TYPE =
				TimeLimitType.DURATION;
			static final long VALUE_LAST_TIME_LIMIT_DEFAULT_MINUTES = 0;
			static final long VALUE_LAST_TIME_LIMIT_DEFAULT_HOURS = 1;
		}
	}

	public static final long NOT_A_PROFILE_ID = -1;
	// Do not change the following! If the user selects zero hour and zero 
	// minute duration we will treat it as unlimited 
	public static final long NOT_A_TIME_LIMIT = 0;
	static final long INFINITE_TIME = -1;
	static final long MILLIS_IN_MINUTE = 60000;
	static final long MINUTES_IN_HOUR = 60; 
	static final String LOGTAG = "Dindy";
	static final boolean DEBUG = false;
	static final String EMPTY_STRING = "";
	
	static final String SERVICE_STARTED = "net.gnobal.dindy.action.SERVICE_STARTED";
	static final String SERVICE_STOPPED = "net.gnobal.dindy.action.SERVICE_STOPPED";
	
	// NOTE the following  constants are used from outside (e.g. Locale, 
	// shortcuts) and cannot be changed. External actions are for when the 
	// action isn't really the intent's action, but is stored under a 
	// different intent (like in Locale)
	// NEVER CHANGE THESE VALUES OR THEIR MEANING
	public static final String ACTION_START_DINDY_SERVICE =
		"net.gnobal.dindy.ACTION_START_DINDY_SERVICE";
	public static final String ACTION_STOP_DINDY_SERVICE =
		"net.gnobal.dindy.ACTION_STOP_DINDY_SERVICE";
	// NOTE: If you add extras here, they should also be added to the KEY_LAST_STARTUP_* constants
	// above and handled properly
	public static final String EXTRA_PROFILE_ID = "profile_id";
	public static final String EXTRA_PROFILE_NAME =  "profile_name";
	public static final String EXTRA_INTENT_SOURCE = "intent_source";
	public static final String EXTRA_INTENT_TIME_LIMIT_MILLIS = "time_limit";
	
	public static final int INTENT_SOURCE_UNKNOWN = 0;
	public static final int INTENT_SOURCE_SHORTCUT = 1;
	public static final int INTENT_SOURCE_WIDGET = 2;
	public static final int INTENT_SOURCE_APP_MAIN = 3;
	public static final int INTENT_SOURCE_APP_PROFILE_PREFS = 4;
	public static final int INTENT_SOURCE_LOCALE = 5;

	public static final String EXTRA_EXTERNAL_ACTION =
		"net.gnobal.dindy.EXTERNAL_ACTION";
	public static final String EXTRA_EXTERNAL_ACTION_START_SERVICE =
		"net.gnobal.dindy.EXTERNAL_ACTION_START_SERVICE";
	public static final String EXTRA_EXTERNAL_ACTION_STOP_SERVICE =
		"net.gnobal.dindy.EXTERNAL_ACTION_STOP_SERVICE";
}