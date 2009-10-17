package net.gnobal.dindy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.LinkedList;

// TODO fix for this version:
// - test plan

//TODO fix/add in future versions:
// - phone shutdown - restore to user's settings or restore Dindy by registering
//   to start at boot time
// - Allow parameters in SMS like {Caller} {Time}
// - auto stop Dindy in driving mode
// - shortcuts for buttons
// - Control screen flashing as well
// - allow multiple messages per profile (from Yossi)
// - Hang up on a caller if we know we want to send the SMS
// - Voice response instead of SMS

public class Dindy extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		ImageButton powerButton = (ImageButton)
			findViewById(R.id.main_power_button);
		powerButton.setOnClickListener(mStartStopListener);
		Button button = (Button) findViewById(R.id.profile_select_button);
		button.setOnClickListener(mSelectProfileListener);
		button = (Button) findViewById(R.id.main_edit_profiles_button);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) { startProfileEditor(); }
		});
		button = (Button) findViewById(R.id.main_help_button);
		button.setOnClickListener(mHelpListener);
		
		SharedPreferences preferences = getSharedPreferences(
				Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
		if (preferences.getBoolean(Consts.Prefs.Main.KEY_FIRST_STARTUP, true)) {
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean(Consts.Prefs.Main.KEY_FIRST_STARTUP, false);
			editor.commit();
			createProfile(R.string.default_profile_car_name,
					R.string.default_profile_car_sms_message, false, false,
					true, false, 5,
					Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_NORMAL);
			createProfile(R.string.default_profile_meeting_name,
					R.string.default_profile_meeting_sms_message, false, false,
					false, true, 10,
					Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_FIRST);
			createProfile(R.string.default_profile_night_name,
					R.string.default_profile_night_sms_message, false, false,
					true, true, Consts.INFINITE_TIME,
					Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_SECOND);
		}
		
		if (preferences.contains(Consts.Prefs.Main.LAST_USED_PROFILE_ID)) {
			int lastSelectedProfile = preferences.getInt(
					Consts.Prefs.Main.LAST_USED_PROFILE_ID,
					Consts.NOT_A_PROFILE_ID);
			if (mPreferencesHelper.profileExists(lastSelectedProfile)) {
				mSelectedProfileId = lastSelectedProfile;
			} else if (!Utils.isDindyServiceRunning(this)) {
				// Why only if the service isn't running? Because the service is
				// currently running with a profile that was deleted, so we
				// don't want to "cheat" the user that a different profile is
				// running by showing "Stop profile X" in setDynamicButtons
				setFirstAvailableProfile();
			}
		} else {
			// A profile was never started (this is _not_ the same as saying 
			// that the program was never run 
			setFirstAvailableProfile();
		}

		// Only show the startup warning message if the user hasn't clicked the
		// "don't show again" checkbox, hasn't started this activity yet (this
		// is done by the save instance bundle) and the service isn't running
		final boolean userPreferenceShowMessage =
			preferences.getBoolean(Consts.Prefs.Main.KEY_SHOW_STARTUP_MESSAGE, true);
		final boolean thisInstanceShowMessage =
			savedInstanceState == null ? true :
				savedInstanceState.getBoolean(Consts.Prefs.Main.KEY_SHOW_STARTUP_MESSAGE, true);
		if (userPreferenceShowMessage && thisInstanceShowMessage &&
			!Utils.isDindyServiceRunning(this)) {
			showDialog(DIALOG_STARTUP_MESSAGE);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		setDynamicButtons(Utils.isDindyServiceRunning(this));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_ID_PREFERENCES, 0, R.string.main_menu_preferences)
				.setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case MENU_ID_PREFERENCES:
			startProfileEditor();
			return true;
		}

		return false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (outState == null) {
			return;
		}
		
		outState.putBoolean(Consts.Prefs.Main.KEY_SHOW_STARTUP_MESSAGE, false);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		super.onCreateDialog(id);
		
		switch (id) {
		case DIALOG_STARTUP_MESSAGE:
		{
			LayoutInflater factory = LayoutInflater.from(this);
			final View startupMessageView = factory.inflate(
					R.layout.startup_message_dialog, null);
			AlertDialog dialog = new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(R.string.startup_message_dialog_title)
				.setView(startupMessageView)
				.setPositiveButton(R.string.startup_message_dialog_ok_text,
					new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						SharedPreferences preferences = getSharedPreferences(
								Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
						CheckBox checkBox = (CheckBox) 
							((AlertDialog) dialog).findViewById(
								R.id.startup_message_dialog_checkbox);
						SharedPreferences.Editor editor = 
							preferences.edit();
						editor.putBoolean(
								Consts.Prefs.Main.KEY_SHOW_STARTUP_MESSAGE,
								!checkBox.isChecked());
						editor.commit();
					}})
				.create();
			
			return dialog;
		}
		
		case DIALOG_HELP:
		{
			LayoutInflater factory = LayoutInflater.from(this);
			View helpView = factory.inflate(R.layout.help_dialog, null);
			TextView helpTextView = (TextView) helpView.findViewById(
					R.id.help_dialog_text_view);
			helpTextView.setText(Html.fromHtml(getString(
	        		R.string.help_dialog_text)));
			helpTextView.setMovementMethod(LinkMovementMethod.getInstance());
			
			AlertDialog dialog = new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(R.string.help_dialog_title)
				.setView(helpView)
				.setPositiveButton(R.string.help_dialog_ok_text,
					new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
					}})
				.create();
			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					removeDialog(DIALOG_HELP);
				}
			});
			dialog.setOwnerActivity(this);
			return dialog;
		}
		}
		
		return null;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode != PROFILE_SELECT_REQUEST_CODE &&
			requestCode != PROFILE_MANAGE_REQUEST_CODE) {
			return;
		}
		
		boolean dindyServiceIsRunning = Utils.isDindyServiceRunning(this);

		// The currently selected profile may have been deleted so in this case
		// first we update mSelectedProfileId to NOT_A_PROFILE_ID  
		if (mSelectedProfileId != Consts.NOT_A_PROFILE_ID && 
			!mPreferencesHelper.profileExists(mSelectedProfileId)) {
			mSelectedProfileId = Consts.NOT_A_PROFILE_ID;
		}

		// If the activity was canceled, set a valid profile because the user 
		// may have deleted the one he used before or even all of them, but
		// only if we're not running (because this will make us show the wrong
		// text)
		if (resultCode == RESULT_CANCELED) {
			if (!dindyServiceIsRunning &&
				(mSelectedProfileId == Consts.NOT_A_PROFILE_ID ||
				!mPreferencesHelper.profileExists(mSelectedProfileId))) {
				setFirstAvailableProfile();
			}

			return;
		}

		// The user didn't cancel and the list was in "select" mode so let's
		// use the user's selection
		if (requestCode == PROFILE_SELECT_REQUEST_CODE) {
			final int previousProfile = mSelectedProfileId; 
			mSelectedProfileId = data.getExtras().getInt(
					ProfilesListActivity.EXTRA_SELECTED_PROFILE_ID);
			if (mSelectedProfileId != previousProfile &&
				dindyServiceIsRunning) {
				// Make the service use the new profile
				startDindyServiceWithSelectedProfileId();
			}
		}
	}

	private void startProfileEditor() {
		Intent profileManagerIntent = new Intent(Dindy.this, 
				ProfilesListActivity.class);
		profileManagerIntent.putExtra(
				ProfilesListActivity.EXTRA_MODE_NAME,
				ProfilesListActivity.EXTRA_MODE_EDIT);
		startActivityForResult(profileManagerIntent,
				PROFILE_MANAGE_REQUEST_CODE);
	}
	
	private void createProfile(int profileNameResource, int textMessageResource, 
			boolean firstRingPlaySound, boolean firstRingVibrate,
			boolean secondRingPlaySound, boolean secondRingVibrate,
			long timeBetweenCallsMinutes, String treatUnknownCallers) {
		String profileName = getString(profileNameResource);
		if (mPreferencesHelper.profileExists(profileName)) {
			return;
		}
		long newProfileId = mPreferencesHelper.createNewProfile(
				profileName);
		if (newProfileId == Consts.NOT_A_PROFILE_ID) {
			return;
		}
		SharedPreferences newProfilePrefs =
			mPreferencesHelper.getPreferencesForProfile(newProfileId,
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
		editor.putString(Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_CALLERS,
				treatUnknownCallers);
		editor.commit();
	}
	
	private void setFirstAvailableProfile() {
		LinkedList<String> allProfiles = 
			mPreferencesHelper.getAllProfileNamesSorted();
		if (allProfiles.isEmpty()) {
			mSelectedProfileId = Consts.NOT_A_PROFILE_ID;
		} else {
			mSelectedProfileId = mPreferencesHelper.getProfileIdFromName(
					allProfiles.get(0));
		}
	}
	
	private void setDynamicButtons(boolean isDindyServiceRunning) {
		ImageButton powerButton = (ImageButton) 
			findViewById(R.id.main_power_button);
		TextView startStopTextView = (TextView) findViewById(
				R.id.main_start_stop_text);
		TextView profileTextView = (TextView) findViewById(
				R.id.main_profile_text);
		TextView profileNameView = (TextView) findViewById(
				R.id.main_profile_name);
		Button selectProfileButton = (Button) findViewById(
				R.id.profile_select_button);
		// TODO rework this. Now that both buttons are almost always enabled,
		// it matters less whether the service is running or not.

		// We have to use the database here because mSelectedProfileId may be 
		// NOT_A_PROFILE_ID if the service is running but the profile used to
		// run it was deleted, so we can't trust mSelectedProfileId to tell
		// us whether there are profile to choose from or not
		selectProfileButton.setEnabled(
				!mPreferencesHelper.getAllProfileNamesSorted().isEmpty());
		if (isDindyServiceRunning) {
			startStopTextView.setText(getString(R.string.main_stop));

			if (mSelectedProfileId != Consts.NOT_A_PROFILE_ID) {
				profileTextView.setText(" " + getString(R.string.main_profile) 
						+ " ");
				//startStopText += " (" + 
				//mPreferencesHelper.getProfielNameFromId(mSelectedProfileId) + 
				//")";
				profileNameView.setText(mPreferencesHelper.getProfielNameFromId(
						mSelectedProfileId)); 
			} else {
				profileTextView.setText("");
				profileNameView.setText("");
				//selectProfileButton.setText(
				//		R.string.main_button_add_new_profile);
			}
			powerButton.setEnabled(true);
			powerButton.setImageResource(R.drawable.power_button_off);
		} else {
			/*
			 * no need for this code, as we're doing this work in 
			 * onActivityResult()

			// It can happen that mSelectedProfileId != NOT_A_PROFILE_ID and 
			// the profile doesn't exist if the user went to the profile 
			// manager and deleted it. The service still runs fine, but when 
			// we stop it we need to update correctly
			if (mSelectedProfileId == Consts.NOT_A_PROFILE_ID ||
				!mPreferencesHelper.profileExists(mSelectedProfileId)) {
				setFirstAvailableProfile();
			}*/
			if (mSelectedProfileId != Consts.NOT_A_PROFILE_ID) {
				startStopTextView.setText(getString(R.string.main_start));
				profileTextView.setText(" " + getString(R.string.main_profile) +
						" ");
				profileNameView.setText(
						mPreferencesHelper.getProfielNameFromId(
								mSelectedProfileId));
				powerButton.setEnabled(true);
				powerButton.setImageResource(R.drawable.power_button_on);
			} else {
				startStopTextView.setText(R.string.main_no_available_profile);
				profileTextView.setText("");
				profileNameView.setText("");
				powerButton.setEnabled(false);
				powerButton.setImageResource(R.drawable.power_button_disabled);
			}			
		}
	}

	private OnClickListener mStartStopListener = new OnClickListener() {
		public void onClick(View v) {
			final boolean isServiceRunning = Utils.isDindyServiceRunning(
					Dindy.this);
			if (isServiceRunning) {
				stopService(new Intent(Dindy.this, DindyService.class));
				// If the selected profile was deleted while the service was 
				// running, set a new profile for the user to select
				if (mSelectedProfileId == Consts.NOT_A_PROFILE_ID ||
					!mPreferencesHelper.profileExists(mSelectedProfileId)) {
					setFirstAvailableProfile();
				}
			} else {
				startDindyServiceWithSelectedProfileId();
			}
			setDynamicButtons(!isServiceRunning);
		}
	};

	private void startDindyServiceWithSelectedProfileId() {
		// Remember last profile we started with
		SharedPreferences preferences = getSharedPreferences(
				Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(Consts.Prefs.Main.LAST_USED_PROFILE_ID,
				mSelectedProfileId);
		editor.commit();
		editor = null;
		preferences = null;
		Intent serviceIntent = new Intent(Dindy.this,
				DindyService.class);
		serviceIntent.putExtra(DindyService.EXTRA_PROFILE_ID,
				mSelectedProfileId);
		startService(serviceIntent);
	}
	
	private OnClickListener mSelectProfileListener = new OnClickListener() {
		public void onClick(View v) {
			Intent profilesListIntent = new Intent(Dindy.this,
					ProfilesListActivity.class);
			profilesListIntent.putExtra(ProfilesListActivity.EXTRA_MODE_NAME,
					ProfilesListActivity.EXTRA_MODE_SELECT);
			Dindy.this.startActivityForResult(profilesListIntent,
					PROFILE_SELECT_REQUEST_CODE);
		}
	};

	private OnClickListener mHelpListener = new OnClickListener() {
		public void onClick(View v) {
			Dindy.this.showDialog(DIALOG_HELP);
		}
	};

	private ProfilePreferencesHelper mPreferencesHelper =
		ProfilePreferencesHelper.instance(this);
	private int mSelectedProfileId = Consts.NOT_A_PROFILE_ID;
	private static final int MENU_ID_PREFERENCES = 0;
	private static final int PROFILE_SELECT_REQUEST_CODE = 1;
	private static final int PROFILE_MANAGE_REQUEST_CODE = 2;
	private static final int DIALOG_STARTUP_MESSAGE = 0;
	private static final int DIALOG_HELP = 1;
}
