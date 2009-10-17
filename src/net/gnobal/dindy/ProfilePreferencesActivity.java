package net.gnobal.dindy;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;

public class ProfilePreferencesActivity extends PreferenceActivity {
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_ID_DONE, 0, 
				R.string.preferences_profile_menu_done).setIcon(
						android.R.drawable.ic_menu_save);// .setShortcut('3','d');
		menu.add(0, MENU_ID_RENAME, 0, 
				R.string.preferences_profile_menu_rename).setIcon(
						android.R.drawable.ic_menu_edit);// .setShortcut('7','r');
		menu.add(0, MENU_ID_DELETE, 0, 
				R.string.preferences_profile_menu_delete).setIcon(
						android.R.drawable.ic_menu_delete);//.setShortcut('3', 'd');
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		
		 switch (item.getItemId()) {
		 case MENU_ID_DONE:
		 {
			 finish();
			 return true; 
		 }

		 case MENU_ID_RENAME:
		 {
			 showDialog(ProfileNameDialogHelper.DIALOG_RENAME_PROFILE);
			 return true; 
		 }
		
		 case MENU_ID_DELETE:
		 {
			 mHelper.deleteProfile(mProfileName);
			 finish();
			 return true;
		 }
		 
		 } // switch
		
		 return false;
	}
	
	static final String EXTRA_PROFILE_NAME = "profile_name";
	static final String EXTRA_PROFILE_ID = "profile_id";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// TODO maybe use a shared resource after all, as in here:
		// http://google.com/codesearch/p?hl=en&sa=N&cd=3&ct=rc#kZ0MkhnKNzw/trunk/Photostream/src/com/google/android/photostream/SettingsActivity.java&q=setSharedPreferencesName&l=30

		Bundle bundle = getIntent().getExtras();
		mProfileName = bundle.getString(EXTRA_PROFILE_NAME);
		setTitleWithCurrentProfile();
		mProfileId = bundle.getLong(EXTRA_PROFILE_ID);
		// Root
		PreferenceManager pm = getPreferenceManager();
		final String sharedPreferencesName = 
			mHelper.getPreferencesFileNameForProfileId(mProfileId);
		pm.setSharedPreferencesName(sharedPreferencesName);
		SharedPreferences sharedPreferences = getSharedPreferences(
				sharedPreferencesName, Context.MODE_PRIVATE);
		PreferenceScreen root = pm.createPreferenceScreen(this);
		// This must be prior to creating preferences because otherwise 
		// preferences dependency doesn't work. See:
		// http://groups.google.com/group/android-developers/browse_thread/thread/9db470e8ecd65d86/1acee6c590246737?lnk=raot&pli=1
		setPreferenceScreen(root);

		// Set all defaults first because we need values to be available before
		// assigning them to preferences objects
		setPreferencesDefaultsIfNeeded(sharedPreferences);
		
		// SMS category
		PreferenceCategory smsCat = new PreferenceCategory(this);
		smsCat.setTitle(R.string.preferences_profile_sms);
		root.addPreference(smsCat);

		CheckBoxPreference enableSmsPref = new CheckBoxPreference(this);
		enableSmsPref.setKey(Consts.Prefs.Profile.KEY_ENABLE_SMS);
		enableSmsPref.setTitle(
				R.string.preferences_profile_enable_sms_sending_title);
		enableSmsPref.setSummary(
				R.string.preferences_profile_enable_sms_sending_summary);
		smsCat.addPreference(enableSmsPref);
		
		EditTextPreference smsMessagePref = new EditTextPreference(this);
		smsMessagePref.setKey(Consts.Prefs.Profile.KEY_SMS_MESSAGE);
		smsMessagePref.setTitle(R.string.preferences_profile_sms_message);
		smsMessagePref.setOnPreferenceChangeListener(
				new Preference.OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference p,
							Object newValue) {
						p.setSummary((String) newValue);
						return true;
					}
				});
		smsMessagePref.setOnPreferenceClickListener(
				new Preference.OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						EditTextPreference smsPref = 
							(EditTextPreference) preference;
						String text = getSharedPreferences(
								sharedPreferencesName, Context.MODE_PRIVATE)
								.getString(
										Consts.Prefs.Profile.KEY_SMS_MESSAGE,
										"");
						smsPref.getEditText().setText(text);
						smsPref.getEditText().setSelection(text.length());
						return true;
					}
				});

		// Example on inheriting EditTextPreference:
		// http://google.com/codesearch/p?hl=en&sa=N&cd=8&ct=rc#r4Q5vzOJY9U/src/com/android/phone/EditPinPreference.java&q=EditTextPreference
		String currMessage = sharedPreferences.getString(
				Consts.Prefs.Profile.KEY_SMS_MESSAGE, "");
		smsMessagePref.setSummary(currMessage);
		smsCat.addPreference(smsMessagePref);
		// Setting the dependency must happen after both involved preferences 
		// have been added with addPreference 
		smsMessagePref.setDependency(Consts.Prefs.Profile.KEY_ENABLE_SMS);
        
		// First call behavior
		PreferenceCategory firstRingCat = new PreferenceCategory(this);
		firstRingCat.setTitle(R.string.preferences_profile_first_ring);
		root.addPreference(firstRingCat);

		CheckBoxPreference firstRingSound = new CheckBoxPreference(this);
		firstRingSound.setKey(Consts.Prefs.Profile.KEY_FIRST_RING_SOUND);
		firstRingSound.setTitle(R.string.preferences_profile_first_ring_sound);
		firstRingCat.addPreference(firstRingSound);

		CheckBoxPreference firstRingVibrate = new CheckBoxPreference(this);
		firstRingVibrate.setKey(Consts.Prefs.Profile.KEY_FIRST_RING_VIBRATE);
		firstRingVibrate
				.setTitle(R.string.preferences_profile_first_ring_vibrate);
		firstRingCat.addPreference(firstRingVibrate);

		// Second call behavior
		PreferenceCategory secondRingCat = new PreferenceCategory(this);
		secondRingCat.setTitle(R.string.preferences_profile_second_ring);
		root.addPreference(secondRingCat);

		CheckBoxPreference secondRingSound = new CheckBoxPreference(this);
		secondRingSound.setKey(Consts.Prefs.Profile.KEY_SECOND_RING_SOUND);
		secondRingSound
				.setTitle(R.string.preferences_profile_second_ring_sound);
		secondRingCat.addPreference(secondRingSound);

		CheckBoxPreference secondRingVibrate = new CheckBoxPreference(this);
		secondRingVibrate.setKey(Consts.Prefs.Profile.KEY_SECOND_RING_VIBRATE);
		secondRingVibrate
				.setTitle(R.string.preferences_profile_second_ring_vibrate);
		secondRingCat.addPreference(secondRingVibrate);

		// Call settings category
		PreferenceCategory callCat = new PreferenceCategory(this);
		callCat.setTitle(R.string.preferences_profile_call_settings);
		root.addPreference(callCat);
				
		// Time between calls
        ListPreference timeBetweenCallsPref = new ListPreference(this);
        timeBetweenCallsPref.setEntries(
        		R.array.time_between_calls_array_strings);
        timeBetweenCallsPref.setEntryValues(
        		R.array.time_between_calls_array_values);
        timeBetweenCallsPref.setDialogTitle(
        		R.string.preferences_profile_time_between_calls_dialog_title);
        timeBetweenCallsPref.setKey(
        		Consts.Prefs.Profile.KEY_TIME_BETWEEN_CALLS_MINUTES);
        timeBetweenCallsPref.setTitle(
        		R.string.preferences_profile_time_between_calls_title);
		timeBetweenCallsPref.setOnPreferenceChangeListener(
				new SummaryWithValuePreferenceChangeListener(
						R.string.preferences_profile_time_between_calls_summary));
        callCat.addPreference(timeBetweenCallsPref);
        setListPrefernceSummaryWithValue(timeBetweenCallsPref,
        		R.string.preferences_profile_time_between_calls_summary,
        		timeBetweenCallsPref.getEntry());
		
        // Treat unknown callers as
        ListPreference treatUnknownCallersPref = new ListPreference(this);
        treatUnknownCallersPref.setEntries(
        		R.array.unknown_caller_behavior_array_strings);
        treatUnknownCallersPref.setEntryValues(
        		R.array.unknown_caller_behavior_array_values);
        treatUnknownCallersPref.setDialogTitle(
        		R.string.preferences_profile_unknown_caller_behavior_dialog_title);
        treatUnknownCallersPref.setKey(
        		Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_CALLERS);
        treatUnknownCallersPref.setTitle(
        		R.string.preferences_profile_unknown_caller_behavior_title);
		treatUnknownCallersPref.setOnPreferenceChangeListener(
				new SummaryWithValuePreferenceChangeListener(
						R.string.preferences_profile_unknown_caller_behavior_summary));
        callCat.addPreference(treatUnknownCallersPref);
        setListPrefernceSummaryWithValue(treatUnknownCallersPref,
        		R.string.preferences_profile_unknown_caller_behavior_summary,
        		treatUnknownCallersPref.getEntry());
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (Utils.isDindyServiceRunning(this)) {
			// The service is running so we check if it's running the profile
			// we're editing 
			SharedPreferences preferences = getSharedPreferences(
					Consts.Prefs.Main.NAME, Context.MODE_PRIVATE); 
			if (preferences.getLong(Consts.Prefs.Main.LAST_USED_PROFILE_ID,
					Consts.NOT_A_PROFILE_ID) == mProfileId) {
				// This means the profile we're editing is currently running, 
				// so we trigger a settings refresh, as onPause() means the 
				// user is done editing the profile
				Intent serviceIntent = new Intent(this, DindyService.class);
				serviceIntent.putExtra(DindyService.EXTRA_PROFILE_ID,
						mProfileId);
				startService(serviceIntent);
			}
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		super.onCreateDialog(id);
		
		if (id != ProfileNameDialogHelper.DIALOG_RENAME_PROFILE) {
			return null;
		}
		
		setNameDialogVariables(id);
		return ProfileNameDialogHelper.buildProfileNameDialog(this,
				android.R.drawable.ic_dialog_info, mRenameListener);
	}

	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		
		if (id != ProfileNameDialogHelper.DIALOG_RENAME_PROFILE) {
			return;
		}
		
		setNameDialogVariables(id);
		ProfileNameDialogHelper.prepareDialog(id, dialog);
	}

	private void setNameDialogVariables(int dialogId) {
		if (dialogId != ProfileNameDialogHelper.DIALOG_RENAME_PROFILE) {
			return;
		}
		
		if (mProfileName != null) {
			String title = getString(
					R.string.dialog_profile_name_rename_title_prefix)
					+ " " + mProfileName;
			String oldProfileName = mProfileName;
			ProfileNameDialogHelper.setDialogVariables(title,
					oldProfileName);
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
			for (int i = 0; i < values.length; ++ i) {
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
	
	private void setListPrefernceSummaryWithValue(
			ListPreference listPref, int stringResourceId, CharSequence value) {
		listPref.setSummary(
				new StringBuilder(getString(stringResourceId))
				.append(" (").append(getString(R.string.preferences_profile_list_preference_current_prefix))
				.append(" ").append(value).append(")").toString());		
	}

	private void setTitleWithCurrentProfile() {
		setTitle(getString(R.string.preferences_profile_editor) + " - " + 
				mProfileName);
	}
	
	private class RenameDialogListener implements 
		ProfileNameDialogHelper.Listener { 
		RenameDialogListener(ProfilePreferencesActivity parent) {
			mParent = parent;
		}
		
		public int getDialogType() {
			return ProfileNameDialogHelper.DIALOG_RENAME_PROFILE;
		}
	
		public void onSuccess(String newProfileName,
				long newProfileId) {
			mParent.mProfileName = newProfileName;
			mParent.setTitleWithCurrentProfile();
		}
		
		public Activity getOwnerActivity() {
			return mParent;
		}
		
		private ProfilePreferencesActivity mParent;
	}

	
	private void setPreferencesDefaultsIfNeeded(SharedPreferences sharedPreferences) {
		Editor editor = sharedPreferences.edit();
		if (!sharedPreferences.contains(Consts.Prefs.Profile.KEY_ENABLE_SMS)) {
			editor.putBoolean(Consts.Prefs.Profile.KEY_ENABLE_SMS, 
					Consts.Prefs.Profile.VALUE_ENABLE_SMS_DEFAULT);
		}
		if (!sharedPreferences.contains(Consts.Prefs.Profile.KEY_SMS_MESSAGE)) {
			editor.putString(Consts.Prefs.Profile.KEY_SMS_MESSAGE,
					getString(R.string.preferences_profile_sms_message_default_unset_value));
		}
		if (!sharedPreferences.contains(
				Consts.Prefs.Profile.KEY_FIRST_RING_SOUND)) {
			editor.putBoolean(Consts.Prefs.Profile.KEY_FIRST_RING_SOUND,
					Consts.Prefs.Profile.VALUE_FIRST_RING_SOUND_DEFAULT);
		}
		if (!sharedPreferences.contains(
				Consts.Prefs.Profile.KEY_FIRST_RING_VIBRATE)) {
			editor.putBoolean(Consts.Prefs.Profile.KEY_FIRST_RING_VIBRATE,
					Consts.Prefs.Profile.VALUE_FIRST_RING_VIBRATE_DEFAULT);
		}
		if (!sharedPreferences.contains(
				Consts.Prefs.Profile.KEY_SECOND_RING_SOUND)) {
			editor.putBoolean(Consts.Prefs.Profile.KEY_SECOND_RING_SOUND,
					Consts.Prefs.Profile.VALUE_SECOND_RING_SOUND_DEFAULT);
		}
		if (!sharedPreferences.contains(
				Consts.Prefs.Profile.KEY_SECOND_RING_VIBRATE)) {
			editor.putBoolean(Consts.Prefs.Profile.KEY_SECOND_RING_VIBRATE,
					Consts.Prefs.Profile.VALUE_SECOND_RING_VIBRATE_DEFAULT);
		}
		if (!sharedPreferences.contains(
				Consts.Prefs.Profile.KEY_TIME_BETWEEN_CALLS_MINUTES)) {
			editor.putString(Consts.Prefs.Profile.KEY_TIME_BETWEEN_CALLS_MINUTES,
					Consts.Prefs.Profile.VALUE_TIME_BETWEEN_CALLS_DEFAULT);
		}
		if (!sharedPreferences.contains(
				Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_CALLERS)) {
			editor.putString(Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_CALLERS,
					Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_DEFAULT);
		}
		editor.commit();
		editor = null;
	}
	
	private static final int MENU_ID_DONE = 0;
	private static final int MENU_ID_RENAME = 1;
	private static final int MENU_ID_DELETE = 2;

	private String mProfileName = null;
	private long mProfileId = Consts.NOT_A_PROFILE_ID;
	private ProfilePreferencesHelper mHelper =
		ProfilePreferencesHelper.instance(this);
	private RenameDialogListener mRenameListener = new RenameDialogListener(this);
}
