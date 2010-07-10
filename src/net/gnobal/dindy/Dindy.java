package net.gnobal.dindy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Config;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.LinkedList;

//TODO fix/add in future versions:
//- test plan
//- use alarm ( http://developer.android.com/reference/android/app/AlarmManager.html )
//  instead of Java Timer because it wakes up even if the phone is sleeping
//  (use one of the *_WAKEUP types). Maybe use Handler.
// - phone shutdown - restore to user's settings or restore Dindy by registering
//   to start at boot time
// - Allow parameters in SMS like {Caller} {Time}
// - auto stop Dindy in driving mode
// - shortcuts for buttons
// - Control screen flashing as well
// - allow multiple messages per profile (Yossi)
// - Hang up on a caller if we know we want to send the SMS (Albert)
// - Voice response instead of SMS (Albert)
// - Show a summary of the current profile on the main screen (Roman)
// - know when in a car dock by using the intent ACTION_DOCK_EVENT/EXTRA_DOCK_STATE/EXTRA_DOCK_STATE_CAR/EXTRA_DOCK_STATE_DESK/EXTRA_DOCK_STATE_UNDOCKED 
// - "smart mode" - when a number that isn't mobile calls send the SMS to the most called-to mobile number of the same person
// - Deal with crashes and restart by saving the latest settings and restoring them

public class Dindy extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		IntentFilter filter = new IntentFilter();
		filter.addAction(Consts.SERVICE_STARTED);
		filter.addAction(Consts.SERVICE_STOPPED);
		registerReceiver(mBroadcastReceiver, filter);
		
		mPreferencesHelper = ProfilePreferencesHelper.instance();
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
		if (preferences.contains(Consts.Prefs.Main.LAST_USED_PROFILE_ID)) {
			long lastSelectedProfile = Consts.NOT_A_PROFILE_ID;
			try {
				lastSelectedProfile = preferences.getLong(
						Consts.Prefs.Main.LAST_USED_PROFILE_ID,
						Consts.NOT_A_PROFILE_ID);
			} catch (Exception e) {
				if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
						"getting last used profile as int for backward compatibility");
				lastSelectedProfile = preferences.getInt(
						Consts.Prefs.Main.LAST_USED_PROFILE_ID,
						(int) Consts.NOT_A_PROFILE_ID);
			}
					
			if (mPreferencesHelper.profileExists(lastSelectedProfile)) {
				mSelectedProfileId = lastSelectedProfile;
			} else if (!DindyService.isRunning()) {
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
			!DindyService.isRunning()) {
			showDialog(DIALOG_STARTUP_MESSAGE);
		}

		setDynamicButtons(DindyService.isRunning());
	}

	@Override
	public void onResume() {
		super.onResume();
		final boolean serviceRunning = DindyService.isRunning();
		if (mPendingStartServiceRequest) {
			// Just turn it off for next time. We'll use mSelectedProfileId
			// that the user selected
			mPendingStartServiceRequest = false;				
		} else {
			if (serviceRunning) {
				mSelectedProfileId = DindyService.getCurrentProfileId();	
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mBroadcastReceiver);
	}
	
	static PendingIntent getPendingIntent(Context context) {
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
				new Intent(context, Dindy.class)
					.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_CLEAR_TOP)
					.setAction(Intent.ACTION_MAIN), 0);
		return pendingIntent;
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
				.setPositiveButton(R.string.message_dialog_ok_text,
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
			dialog.setOwnerActivity(this);
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
						removeDialog(DIALOG_HELP);
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
		
		boolean dindyServiceIsRunning = DindyService.isRunning();

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
			final long previousProfile = mSelectedProfileId;
			mSelectedProfileId = data.getExtras().getLong(
					Consts.EXTRA_PROFILE_ID);
			if (dindyServiceIsRunning) {
				if (mSelectedProfileId != previousProfile) {
					// Make the service use the new profile
					startDindyServiceWithTimeLimit();
					mPendingStartServiceRequest = true;
				}
			} else {
				setDynamicButtons(false);
			}
		}
	}

	private void startProfileEditor() {
		Intent profileManagerIntent = new Intent(
				getApplicationContext(), 
				ProfilesListActivity.class);
		profileManagerIntent.putExtra(
				ProfilesListActivity.EXTRA_MODE_NAME,
				ProfilesListActivity.EXTRA_MODE_EDIT);
		startActivityForResult(profileManagerIntent,
				PROFILE_MANAGE_REQUEST_CODE);
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
		//Button selectProfileButton = (Button) findViewById(
		//		R.id.profile_select_button);
		// TODO rework this. Now that both buttons are almost always enabled,
		// it matters less whether the service is running or not.

		// We have to use the database here because mSelectedProfileId may be 
		// NOT_A_PROFILE_ID if the service is running but the profile used to
		// run it was deleted, so we can't trust mSelectedProfileId to tell
		// us whether there are profile to choose from or not
		//selectProfileButton.setEnabled(mPreferencesHelper.anyProfilesExist());
		//selectProfileButton.setEnabled(true);
		if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
			"profile ID=" + mSelectedProfileId + ", name=" +
			mPreferencesHelper.getProfielNameFromId(mSelectedProfileId));

		if (isDindyServiceRunning) {
			startStopTextView.setText(getString(R.string.main_stop));

			if (mSelectedProfileId != Consts.NOT_A_PROFILE_ID) {
				profileTextView.setText(" " + getString(R.string.main_profile) 
						+ " ");
				profileNameView.setText(mPreferencesHelper.getProfielNameFromId(
						mSelectedProfileId)); 
			} else {
				profileTextView.setText(Consts.EMPTY_STRING);
				profileNameView.setText(Consts.EMPTY_STRING);
			}
			powerButton.setEnabled(true);
			powerButton.setImageResource(R.drawable.power_button_selector_off);
		} else {
			if (mSelectedProfileId != Consts.NOT_A_PROFILE_ID) {
				startStopTextView.setText(getString(R.string.main_start));
				profileTextView.setText(" " + getString(R.string.main_profile) +
						" ");
				profileNameView.setText(
						mPreferencesHelper.getProfielNameFromId(
								mSelectedProfileId));
				powerButton.setEnabled(true);
				powerButton.setImageResource(R.drawable.power_button_selector_on);
			} else {
				startStopTextView.setText(R.string.main_no_available_profile);
				profileTextView.setText(Consts.EMPTY_STRING);
				profileNameView.setText(Consts.EMPTY_STRING);
				powerButton.setEnabled(false);
				powerButton.setImageResource(R.drawable.power_button_disabled);
			}			
		}
	}

	private OnClickListener mStartStopListener = new OnClickListener() {
		public void onClick(View v) {
			final boolean isServiceRunning = DindyService.isRunning();
			if (isServiceRunning) {
				stopService(DindyService.getStopServiceIntent(
						getApplicationContext()));
				// If the selected profile was deleted while the service was 
				// running, set a new profile for the user to select
				if (mSelectedProfileId == Consts.NOT_A_PROFILE_ID ||
					!mPreferencesHelper.profileExists(mSelectedProfileId)) {
					setFirstAvailableProfile();
				}
				//setDynamicButtons(false);
			} else {
				startDindyServiceWithTimeLimit();
			}
		}
	};
	
	private void startDindyServiceWithTimeLimit() {
		Intent profileStartIntent = new Intent(getApplicationContext(),
				ProfileStarterActivity.class);
		profileStartIntent
			.putExtra(Consts.EXTRA_PROFILE_ID, mSelectedProfileId)
			.putExtra(Consts.EXTRA_INTENT_SOURCE, Consts.INTENT_SOURCE_APP_MAIN);
		Dindy.this.startActivity(profileStartIntent);
	}

	private OnClickListener mSelectProfileListener = new OnClickListener() {
		public void onClick(View v) {
			Intent profilesListIntent = new Intent(getApplicationContext(),
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
//			Intent i = new Intent(getApplicationContext(),
//					net.gnobal.dindy.locale.EditActivity.class);
//			Dindy.this.startActivity(i);
		}
	};

	private class DindyBroadcastReceiver extends BroadcastReceiver {
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
			if (Consts.SERVICE_STARTED.equals(action)) {
				mSelectedProfileId = DindyService.getCurrentProfileId();
				setDynamicButtons(true);
			} else if (Consts.SERVICE_STOPPED.equals(action)) {
				setDynamicButtons(false);
			}
		}
	}

	
	private DindyBroadcastReceiver mBroadcastReceiver = new DindyBroadcastReceiver();
	private ProfilePreferencesHelper mPreferencesHelper = null;
	private long mSelectedProfileId = Consts.NOT_A_PROFILE_ID;
	// We use this indicator to know whether to rely on what 
	// DindyService.getCurrentProfileId() returns or on our own 
	// mSelectedProfileId (see onResume()) 
	private boolean mPendingStartServiceRequest = false;
	// We use this to know the time limit to use in the time limit dialog that's
	// about to show up
	private static final int PROFILE_SELECT_REQUEST_CODE = 1;
	private static final int PROFILE_MANAGE_REQUEST_CODE = 2;
	private static final int DIALOG_STARTUP_MESSAGE = 0;
	private static final int DIALOG_HELP = 1;
}
