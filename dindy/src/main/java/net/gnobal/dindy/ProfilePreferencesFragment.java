package net.gnobal.dindy;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class ProfilePreferencesFragment extends PreferenceFragment {
	public static ProfilePreferencesFragment newInstance(long profileId) {
		final ProfilePreferencesFragment f = new ProfilePreferencesFragment();
		final Bundle args = new Bundle();
		args.putLong("profile_id", profileId);
		f.setArguments(args);
		return f;
	}

	public ProfilePreferencesFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mProfileId = getArguments().getLong("profile_id");
		// TODO maybe use a shared resource after all, as in here:
		// http://google.com/codesearch/p?hl=en&sa=N&cd=3&ct=rc#kZ0MkhnKNzw/trunk/Photostream/src/com/google/android/photostream/SettingsActivity.java&q=setSharedPreferencesName&l=30
		mHelper = ProfilePreferencesHelper.instance();

		// Root
		PreferenceManager pm = getPreferenceManager();
		final String sharedPreferencesName = mHelper
				.getPreferencesFileNameForProfileId(mProfileId);
		pm.setSharedPreferencesName(sharedPreferencesName);
		SharedPreferences sharedPreferences = getActivity()
				.getSharedPreferences(sharedPreferencesName,
						Context.MODE_PRIVATE);
		PreferenceScreen root = pm.createPreferenceScreen(getActivity());
		// This must be prior to creating preferences because otherwise
		// preferences dependency doesn't work. See:
		// http://groups.google.com/group/android-developers/browse_thread/thread/9db470e8ecd65d86/1acee6c590246737?lnk=raot&pli=1
		setPreferenceScreen(root);

		// Set all defaults first because we need values to be available before
		// assigning them to preferences objects
		setPreferencesDefaultsIfNeeded(sharedPreferences);

		// SMS reply-to-call category
		PreferenceCategory smsReplyToCallCat = new PreferenceCategory(
				getActivity());
		smsReplyToCallCat
				.setTitle(R.string.preferences_profile_sms_reply_to_call);
		root.addPreference(smsReplyToCallCat);

		CheckBoxPreference enableSmsReplyToCallPref = new CheckBoxPreference(
				getActivity());
		enableSmsReplyToCallPref
				.setKey(Consts.Prefs.Profile.KEY_ENABLE_SMS_CALLERS);
		enableSmsReplyToCallPref
				.setTitle(R.string.preferences_profile_enable_sms_reply_to_call_title);
		enableSmsReplyToCallPref
				.setSummary(R.string.preferences_profile_enable_sms_reply_to_call_summary);
		smsReplyToCallCat.addPreference(enableSmsReplyToCallPref);

		EditTextPreference smsReplyToCallMessagePref = new EditTextPreference(
				getActivity());
		smsReplyToCallMessagePref
				.setKey(Consts.Prefs.Profile.KEY_SMS_MESSAGE_CALLERS);
		smsReplyToCallMessagePref
				.setTitle(R.string.preferences_profile_sms_message_callers);
		smsReplyToCallMessagePref
				.setOnPreferenceChangeListener(new SmsMessageChangeListener());
		smsReplyToCallMessagePref
				.setOnPreferenceClickListener(new SmsMessageClickListener(
						sharedPreferencesName,
						Consts.Prefs.Profile.KEY_SMS_MESSAGE_CALLERS));
		// Example on inheriting EditTextPreference:
		// http://google.com/codesearch/p?hl=en&sa=N&cd=8&ct=rc#r4Q5vzOJY9U/src/com/android/phone/EditPinPreference.java&q=EditTextPreference
		String currCallerMessage = sharedPreferences.getString(
				Consts.Prefs.Profile.KEY_SMS_MESSAGE_CALLERS,
				Consts.EMPTY_STRING);
		smsReplyToCallMessagePref.setSummary(currCallerMessage);
		smsReplyToCallCat.addPreference(smsReplyToCallMessagePref);
		// Setting the dependency must happen after both involved preferences
		// have been added with addPreference
		smsReplyToCallMessagePref
				.setDependency(Consts.Prefs.Profile.KEY_ENABLE_SMS_CALLERS);

		// SMS reply-to-SMS category
		PreferenceCategory smsReplyToSmsCat = new PreferenceCategory(
				getActivity());
		smsReplyToSmsCat
				.setTitle(R.string.preferences_profile_sms_reply_to_sms);
		root.addPreference(smsReplyToSmsCat);

		CheckBoxPreference enableSmsReplyToSmsPref = new CheckBoxPreference(
				getActivity());
		enableSmsReplyToSmsPref
				.setKey(Consts.Prefs.Profile.KEY_ENABLE_SMS_TEXTERS);
		enableSmsReplyToSmsPref
				.setTitle(R.string.preferences_profile_enable_sms_reply_to_sms_title);
		enableSmsReplyToSmsPref
				.setSummary(R.string.preferences_profile_enable_sms_reply_to_sms_summary);
		smsReplyToSmsCat.addPreference(enableSmsReplyToSmsPref);

		EditTextPreference smsReplyToSmsMessagePref = new EditTextPreference(
				getActivity());
		smsReplyToSmsMessagePref
				.setKey(Consts.Prefs.Profile.KEY_SMS_MESSAGE_TEXTERS);
		smsReplyToSmsMessagePref
				.setTitle(R.string.preferences_profile_sms_message_texters);
		smsReplyToSmsMessagePref
				.setOnPreferenceChangeListener(new SmsMessageChangeListener());
		smsReplyToSmsMessagePref
				.setOnPreferenceClickListener(new SmsMessageClickListener(
						sharedPreferencesName,
						Consts.Prefs.Profile.KEY_SMS_MESSAGE_TEXTERS));
		// Example on inheriting EditTextPreference:
		// http://google.com/codesearch/p?hl=en&sa=N&cd=8&ct=rc#r4Q5vzOJY9U/src/com/android/phone/EditPinPreference.java&q=EditTextPreference
		String currTexterMessage = sharedPreferences.getString(
				Consts.Prefs.Profile.KEY_SMS_MESSAGE_TEXTERS,
				Consts.EMPTY_STRING);
		smsReplyToSmsMessagePref.setSummary(currTexterMessage);
		smsReplyToSmsCat.addPreference(smsReplyToSmsMessagePref);
		// Setting the dependency must happen after both involved preferences
		// have been added with addPreference
		smsReplyToSmsMessagePref
				.setDependency(Consts.Prefs.Profile.KEY_ENABLE_SMS_TEXTERS);

		// First event behavior
		PreferenceCategory firstEventCat = new PreferenceCategory(getActivity());
		firstEventCat.setTitle(R.string.preferences_profile_first_event);
		root.addPreference(firstEventCat);

		CheckBoxPreference firstRingSound = new CheckBoxPreference(
				getActivity());
		firstRingSound.setKey(Consts.Prefs.Profile.KEY_FIRST_EVENT_SOUND);
		firstRingSound.setTitle(R.string.preferences_profile_first_event_sound);
		firstEventCat.addPreference(firstRingSound);

		CheckBoxPreference firstRingVibrate = new CheckBoxPreference(
				getActivity());
		firstRingVibrate.setKey(Consts.Prefs.Profile.KEY_FIRST_EVENT_VIBRATE);
		firstRingVibrate
				.setTitle(R.string.preferences_profile_first_event_vibrate);
		firstEventCat.addPreference(firstRingVibrate);

		// Second event behavior
		PreferenceCategory secondEventCat = new PreferenceCategory(
				getActivity());
		secondEventCat.setTitle(R.string.preferences_profile_second_event);
		root.addPreference(secondEventCat);

		CheckBoxPreference secondRingSound = new CheckBoxPreference(
				getActivity());
		secondRingSound.setKey(Consts.Prefs.Profile.KEY_SECOND_EVENT_SOUND);
		secondRingSound
				.setTitle(R.string.preferences_profile_second_event_sound);
		secondEventCat.addPreference(secondRingSound);

		CheckBoxPreference secondRingVibrate = new CheckBoxPreference(
				getActivity());
		secondRingVibrate.setKey(Consts.Prefs.Profile.KEY_SECOND_EVENT_VIBRATE);
		secondRingVibrate
				.setTitle(R.string.preferences_profile_second_event_vibrate);
		secondEventCat.addPreference(secondRingVibrate);

		// Incoming callers settings category
		PreferenceCategory callersCat = new PreferenceCategory(getActivity());
		callersCat.setTitle(R.string.preferences_profile_callers);
		root.addPreference(callersCat);

		// Treat whitelist callers as
		ListPreference whitelistPref = new ListPreference(getActivity());
		whitelistPref.setEntries(R.array.whitelist_caller_behavior_array_strings);
		whitelistPref.setEntryValues(R.array.whitelist_caller_behavior_array_values);
		whitelistPref.setDialogTitle(R.string.preferences_profile_whitelist_caller_behavior_dialog_title);
		whitelistPref.setKey(Consts.Prefs.Profile.KEY_TREAT_WHITELIST_CALLERS);
		whitelistPref.setTitle(R.string.preferences_profile_whitelist_caller_behavior_title);
		whitelistPref.setOnPreferenceChangeListener(new SummaryWithValuePreferenceChangeListener(
				R.string.preferences_profile_whitelist_caller_behavior_summary));
		callersCat.addPreference(whitelistPref);
		setListPrefernceSummaryWithValue(whitelistPref,
				R.string.preferences_profile_whitelist_caller_behavior_summary,
				whitelistPref.getEntry());

		// Treat non-mobile callers as
		ListPreference treatNonMobileCallersPref = new ListPreference(
				getActivity());
		treatNonMobileCallersPref
				.setEntries(R.array.unknown_caller_behavior_array_strings);
		treatNonMobileCallersPref
				.setEntryValues(R.array.unknown_caller_behavior_array_values);
		treatNonMobileCallersPref
				.setDialogTitle(R.string.preferences_profile_non_mobile_caller_behavior_dialog_title);
		treatNonMobileCallersPref
				.setKey(Consts.Prefs.Profile.KEY_TREAT_NON_MOBILE_CALLERS);
		treatNonMobileCallersPref
				.setTitle(R.string.preferences_profile_non_mobile_caller_behavior_title);
		treatNonMobileCallersPref
				.setOnPreferenceChangeListener(new SummaryWithValuePreferenceChangeListener(
						R.string.preferences_profile_non_mobile_caller_behavior_summary));
		callersCat.addPreference(treatNonMobileCallersPref);
		setListPrefernceSummaryWithValue(
				treatNonMobileCallersPref,
				R.string.preferences_profile_non_mobile_caller_behavior_summary,
				treatNonMobileCallersPref.getEntry());

		// Treat unknown callers as
		ListPreference treatUnknownCallersPref = new ListPreference(
				getActivity());
		treatUnknownCallersPref
				.setEntries(R.array.unknown_caller_behavior_array_strings);
		treatUnknownCallersPref
				.setEntryValues(R.array.unknown_caller_behavior_array_values);
		treatUnknownCallersPref
				.setDialogTitle(R.string.preferences_profile_unknown_caller_behavior_dialog_title);
		treatUnknownCallersPref
				.setKey(Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_CALLERS);
		treatUnknownCallersPref
				.setTitle(R.string.preferences_profile_unknown_caller_behavior_title);
		treatUnknownCallersPref
				.setOnPreferenceChangeListener(new SummaryWithValuePreferenceChangeListener(
						R.string.preferences_profile_unknown_caller_behavior_summary));
		callersCat.addPreference(treatUnknownCallersPref);
		setListPrefernceSummaryWithValue(treatUnknownCallersPref,
				R.string.preferences_profile_unknown_caller_behavior_summary,
				treatUnknownCallersPref.getEntry());

		// Incoming callers settings category
		PreferenceCategory textersCat = new PreferenceCategory(getActivity());
		textersCat.setTitle(R.string.preferences_profile_texters);
		root.addPreference(textersCat);

		// Treat unknown callers as
		ListPreference treatUnknownTextersPref = new ListPreference(
				getActivity());
		treatUnknownTextersPref
				.setEntries(R.array.unknown_texter_behavior_array_strings);
		treatUnknownTextersPref
				.setEntryValues(R.array.unknown_texter_behavior_array_values);
		treatUnknownTextersPref
				.setDialogTitle(R.string.preferences_profile_unknown_texter_behavior_dialog_title);
		treatUnknownTextersPref
				.setKey(Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_TEXTERS);
		treatUnknownTextersPref
				.setTitle(R.string.preferences_profile_unknown_texter_behavior_title);
		treatUnknownTextersPref
				.setOnPreferenceChangeListener(new SummaryWithValuePreferenceChangeListener(//
						R.string.preferences_profile_unknown_texter_behavior_summary));
		textersCat.addPreference(treatUnknownTextersPref);
		setListPrefernceSummaryWithValue(
				treatUnknownTextersPref,//
				R.string.preferences_profile_unknown_texter_behavior_summary,
				treatUnknownTextersPref.getEntry());

		// General settings category
		PreferenceCategory generalCat = new PreferenceCategory(getActivity());
		generalCat.setTitle(R.string.preferences_profile_general);
		root.addPreference(generalCat);

		// Time between calls
		ListPreference timeBetweenCallsPref = new ListPreference(getActivity());
		timeBetweenCallsPref
				.setEntries(R.array.time_between_calls_array_strings);
		timeBetweenCallsPref
				.setEntryValues(R.array.time_between_calls_array_values);
		timeBetweenCallsPref
				.setDialogTitle(R.string.preferences_profile_time_between_events_dialog_title);
		timeBetweenCallsPref
				.setKey(Consts.Prefs.Profile.KEY_TIME_BETWEEN_EVENTS_MINUTES);
		timeBetweenCallsPref
				.setTitle(R.string.preferences_profile_time_between_events_title);
		timeBetweenCallsPref
				.setOnPreferenceChangeListener(new SummaryWithValuePreferenceChangeListener(
						R.string.preferences_profile_time_between_events_summary));
		generalCat.addPreference(timeBetweenCallsPref);
		setListPrefernceSummaryWithValue(timeBetweenCallsPref,
				R.string.preferences_profile_time_between_events_summary,
				timeBetweenCallsPref.getEntry());

		CheckBoxPreference useTimeLimitPref = new CheckBoxPreference(
				getActivity());
		useTimeLimitPref.setKey(Consts.Prefs.Profile.KEY_USE_TIME_LIMIT);
		useTimeLimitPref
				.setTitle(R.string.preferences_profile_use_time_limit_title);
		useTimeLimitPref
				.setSummary(R.string.preferences_profile_use_time_limit_summary);
		generalCat.addPreference(useTimeLimitPref);
	}

	private class SmsMessageChangeListener implements
			Preference.OnPreferenceChangeListener {
		public boolean onPreferenceChange(Preference p, Object newValue) {
			p.setSummary((String) newValue);
			return true;
		}
	}

	private class SmsMessageClickListener implements
			Preference.OnPreferenceClickListener {
		SmsMessageClickListener(String sharedPreferencesName, String messageKey) {
			mSharedPrefsName = sharedPreferencesName;
			mMessageKey = messageKey;
		}

		public boolean onPreferenceClick(Preference preference) {
			EditTextPreference smsPref = (EditTextPreference) preference;
			String text = getActivity().getSharedPreferences(mSharedPrefsName,
					Context.MODE_PRIVATE).getString(mMessageKey,
					Consts.EMPTY_STRING);
			smsPref.getEditText().setText(text);
			smsPref.getEditText().setSelection(text.length());
			return true;
		}

		String mSharedPrefsName;
		String mMessageKey;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (DindyService.isRunning()) {
			// The service is running so we check if it's running the profile
			// we're editing
			// SharedPreferences preferences = getSharedPreferences(
			// Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
			// if (preferences.getLong(Consts.Prefs.Main.LAST_USED_PROFILE_ID,
			// Consts.NOT_A_PROFILE_ID) == mProfileId) {
			if (DindyService.getCurrentProfileId() == mProfileId) {
				// This means the profile we're editing is currently running,
				// so we trigger a settings refresh, as onPause() means the
				// user is done editing the profile
				// Intent serviceIntent = new Intent(getApplicationContext(),
				// DindyService.class);
				// serviceIntent.putExtra(DindyService.EXTRA_PROFILE_ID,
				// mProfileId);
				// startService(serviceIntent);
				getActivity().startService(
						DindyService.getStartServiceIntent(getActivity()
								.getApplicationContext(), mProfileId, null,
								Consts.INTENT_SOURCE_APP_PROFILE_PREFS,
								Consts.NOT_A_TIME_LIMIT, false));
			}
		}
	}

	// Prepare a change listener that sets a preference's summary according
	// to the user's selected value
	private class SummaryWithValuePreferenceChangeListener implements
			Preference.OnPreferenceChangeListener {

		SummaryWithValuePreferenceChangeListener(int stringResourceId) {
			mStringResourceId = stringResourceId;
		}

		public boolean onPreferenceChange(Preference p, Object newValue) {
			ListPreference listPref = (ListPreference) p;
			CharSequence[] values = listPref.getEntryValues();
			for (int i = 0; i < values.length; ++i) {
				if (newValue.equals(values[i])) {
					setListPrefernceSummaryWithValue(listPref,
							mStringResourceId, listPref.getEntries()[i]);
					return true;
				}
			}

			return true;
		}

		private int mStringResourceId;
	}

	private void setListPrefernceSummaryWithValue(ListPreference listPref,
			int stringResourceId, CharSequence value) {
		listPref.setSummary(new StringBuilder(getString(stringResourceId))
				.append(" (")
				.append(getString(R.string.preferences_profile_list_preference_current_prefix))
				.append(" ").append(value).append(")").toString());
	}

	private void setPreferencesDefaultsIfNeeded(
			SharedPreferences sharedPreferences) {
		Editor editor = sharedPreferences.edit();
		if (!sharedPreferences
				.contains(Consts.Prefs.Profile.KEY_ENABLE_SMS_CALLERS)) {
			editor.putBoolean(Consts.Prefs.Profile.KEY_ENABLE_SMS_CALLERS,
					Consts.Prefs.Profile.VALUE_ENABLE_SMS_CALLERS_DEFAULT);
		}
		if (!sharedPreferences
				.contains(Consts.Prefs.Profile.KEY_SMS_MESSAGE_CALLERS)) {
			editor.putString(
					Consts.Prefs.Profile.KEY_SMS_MESSAGE_CALLERS,
					getString(R.string.preferences_profile_sms_message_callers_default_unset_value));
		}
		if (!sharedPreferences
				.contains(Consts.Prefs.Profile.KEY_ENABLE_SMS_TEXTERS)) {
			editor.putBoolean(Consts.Prefs.Profile.KEY_ENABLE_SMS_TEXTERS,
					Consts.Prefs.Profile.VALUE_ENABLE_SMS_TEXTERS_DEFAULT);
		}
		if (!sharedPreferences
				.contains(Consts.Prefs.Profile.KEY_SMS_MESSAGE_TEXTERS)) {
			editor.putString(
					Consts.Prefs.Profile.KEY_SMS_MESSAGE_TEXTERS,
					getString(R.string.preferences_profile_sms_message_texters_default_unset_value));
		}
		if (!sharedPreferences
				.contains(Consts.Prefs.Profile.KEY_FIRST_EVENT_SOUND)) {
			editor.putBoolean(Consts.Prefs.Profile.KEY_FIRST_EVENT_SOUND,
					Consts.Prefs.Profile.VALUE_FIRST_EVENT_SOUND_DEFAULT);
		}
		if (!sharedPreferences
				.contains(Consts.Prefs.Profile.KEY_FIRST_EVENT_VIBRATE)) {
			editor.putBoolean(Consts.Prefs.Profile.KEY_FIRST_EVENT_VIBRATE,
					Consts.Prefs.Profile.VALUE_FIRST_EVENT_VIBRATE_DEFAULT);
		}
		if (!sharedPreferences
				.contains(Consts.Prefs.Profile.KEY_SECOND_EVENT_SOUND)) {
			editor.putBoolean(Consts.Prefs.Profile.KEY_SECOND_EVENT_SOUND,
					Consts.Prefs.Profile.VALUE_SECOND_EVENT_SOUND_DEFAULT);
		}
		if (!sharedPreferences
				.contains(Consts.Prefs.Profile.KEY_SECOND_EVENT_VIBRATE)) {
			editor.putBoolean(Consts.Prefs.Profile.KEY_SECOND_EVENT_VIBRATE,
					Consts.Prefs.Profile.VALUE_SECOND_EVENT_VIBRATE_DEFAULT);
		}
		if (!sharedPreferences
				.contains(Consts.Prefs.Profile.KEY_TIME_BETWEEN_EVENTS_MINUTES)) {
			editor.putString(
					Consts.Prefs.Profile.KEY_TIME_BETWEEN_EVENTS_MINUTES,
					Consts.Prefs.Profile.VALUE_TIME_BETWEEN_EVENTS_DEFAULT);
		}
		if (!sharedPreferences
				.contains(Consts.Prefs.Profile.KEY_TREAT_WHITELIST_CALLERS)) {
			editor.putString(
					Consts.Prefs.Profile.KEY_TREAT_WHITELIST_CALLERS, 
					Consts.Prefs.Profile.VALUE_TREAT_WHITELIST_CALLERS_DEFAULT);
		}
		if (!sharedPreferences
				.contains(Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_CALLERS)) {
			editor.putString(Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_CALLERS,
					Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_DEFAULT);
		}
		// Non-mobile callers added in 1.1.1 and gets the default from unknown
		// callers so it must come after it
		if (!sharedPreferences
				.contains(Consts.Prefs.Profile.KEY_TREAT_NON_MOBILE_CALLERS)) {
			editor.putString(
					Consts.Prefs.Profile.KEY_TREAT_NON_MOBILE_CALLERS,
					sharedPreferences
							.getString(
									Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_CALLERS,
									Consts.Prefs.Profile.VALUE_TREAT_NON_MOBILE_CALLERS_DEFAULT));
		}
		if (!sharedPreferences
				.contains(Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_TEXTERS)) {
			editor.putString(Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_TEXTERS,
					Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_TEXTERS_DEFAULT);
		}
		editor.commit();
		editor = null;
	}

	private long mProfileId = Consts.NOT_A_PROFILE_ID;
	private ProfilePreferencesHelper mHelper = null;
}