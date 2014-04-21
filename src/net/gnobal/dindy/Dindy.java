package net.gnobal.dindy;

import java.util.LinkedList;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

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
public class Dindy extends Activity implements ProfileNameDialogFragment.Listener {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final IntentFilter filter = new IntentFilter();
		filter.addAction(Consts.SERVICE_STARTED);
		filter.addAction(Consts.SERVICE_STOPPED);
		registerReceiver(mBroadcastReceiver, filter);
		
		mPreferencesHelper = ProfilePreferencesHelper.instance();
		// See:
		// http://wptrafficanalyzer.in/blog/adding-drop-down-navigation-to-action-bar-in-android/
		mProfileNames = new LinkedList<String>(mPreferencesHelper.getAllProfileNamesSorted());
		mProfilesAdapter = new ArrayAdapter<String>(getBaseContext(),
				android.R.layout.simple_spinner_dropdown_item, mProfileNames);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(mProfilesAdapter, mNavigationListener);

		setContentView(R.layout.main);
		ImageButton powerButton = (ImageButton)
			findViewById(R.id.main_power_button);
		powerButton.setOnClickListener(mStartStopListener);

		SharedPreferences preferences = getSharedPreferences(
				Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
		if (preferences.contains(Consts.Prefs.Main.LAST_USED_PROFILE_ID)) {
			long lastSelectedProfile = Consts.NOT_A_PROFILE_ID;
			try {
				lastSelectedProfile = preferences.getLong(
						Consts.Prefs.Main.LAST_USED_PROFILE_ID,
						Consts.NOT_A_PROFILE_ID);
			} catch (Exception e) {
				if (Consts.DEBUG) Log.d(Consts.LOGTAG,
						"getting last used profile as int for backward compatibility");
				lastSelectedProfile = preferences.getInt(
						Consts.Prefs.Main.LAST_USED_PROFILE_ID,
						(int) Consts.NOT_A_PROFILE_ID);
			}

			if (mPreferencesHelper.profileExists(lastSelectedProfile)) {
				setSelectedProfileId(lastSelectedProfile);
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
			StartupMessageDialogFragment.newInstance().show(getFragmentManager(), "startup_message");
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		// We do this here because if the ProfileStarterActivity was called and the user
		// selected not to start the profile (e.g. if a time limit dialog was presented
		// and the user cancelled) we need to re-update the UI to reflect the old
		// profile that was still running before
		if (DindyService.isRunning()) {
			mSelectedProfileId = DindyService.getCurrentProfileId();
			setSelectedProfileId(mSelectedProfileId);
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
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		// Don't allow delete if there's only one profile left
		menu.findItem(R.id.action_delete_profile).setEnabled(mProfilesAdapter.getCount() != 1);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_help:
			HelpDialogFragment.newInstance().show(getFragmentManager(), "help");
			return true;
		case R.id.action_add_profile:
			ProfileNameDialogFragment.newInstance(ProfileNameDialogFragment.DIALOG_NEW_PROFILE,
				getString(R.string.new_profile_dialog_title), Consts.EMPTY_STRING)
				.show(getFragmentManager(), "add_profile");
			return true;
		case R.id.action_delete_profile:
			if (DindyService.isRunning()) {
				stopService(DindyService.getStopServiceIntent(getApplicationContext()));
				mSelectedProfileId = Consts.NOT_A_PROFILE_ID;
			}
			final int selectedNavigationIndex = getActionBar().getSelectedNavigationIndex(); 
			final String profileToRemove = mProfilesAdapter.getItem(selectedNavigationIndex);
			boolean lastProfileInListDeleted = false;
			if (selectedNavigationIndex == mProfilesAdapter.getCount() - 1) {
				lastProfileInListDeleted = true;
			}
			mPreferencesHelper.deleteProfile(this, profileToRemove);
			mProfilesAdapter.remove(profileToRemove);
			DindySingleProfileAppWidgetProvider.updateAllSingleProfileWidgets(
					getApplicationContext(), DindyService.getCurrentProfileId(),
					Consts.NOT_A_PROFILE_ID);
			if (!lastProfileInListDeleted) {
				// If a profile on the list gets deleted and there's another profile after it,
				// we won't get an onNavigationItemSelected notification because the index
				// doesn't change. We change the profile ID manually
				setSelectedProfileId(mPreferencesHelper.getProfileIdFromName(
						mProfilesAdapter.getItem(selectedNavigationIndex)));
			}
			return true;
		case R.id.action_rename_profile:
			final String oldProfileName = mPreferencesHelper.getProfielNameFromId(mSelectedProfileId);
			final String title = getString(
					R.string.dialog_profile_name_rename_title_prefix) +
					" " + oldProfileName;
			ProfileNameDialogFragment.newInstance(ProfileNameDialogFragment.DIALOG_RENAME_PROFILE,
					title, oldProfileName).show(getFragmentManager(), "add_profile");
			return true;
		case R.id.action_edit_profile:
			startProfileEditor(mPreferencesHelper.getProfielNameFromId(
				mSelectedProfileId), mSelectedProfileId);
			return true;
		case R.id.action_whitelist:
			startActivity(new Intent(getApplicationContext(), WhitelistActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void startProfileEditor(final String profileName, final long profileId) {
		final Intent profilePreferencesIntent = new Intent(this,
				ProfilePreferencesActivity.class);
		profilePreferencesIntent.putExtra(
				ProfilePreferencesActivity.EXTRA_PROFILE_NAME, profileName);
		profilePreferencesIntent.putExtra(
				ProfilePreferencesActivity.EXTRA_PROFILE_ID, profileId);
		startActivityForResult(profilePreferencesIntent, PROFILE_EDIT_REQUEST_CODE);
	}

	private void setSelectedProfileId(long profileId) {
		final boolean dindyServiceIsRunning = DindyService.isRunning();
		final long previousProfile = mSelectedProfileId;
		mSelectedProfileId = profileId;
		if (dindyServiceIsRunning &&
			mSelectedProfileId != previousProfile &&
			previousProfile != Consts.NOT_A_PROFILE_ID) {
				// Make the service use the new profile
				startDindyServiceWithTimeLimit();
		}

		setDynamicButtons(dindyServiceIsRunning);
		getActionBar().setSelectedNavigationItem(mProfilesAdapter.getPosition(
				mPreferencesHelper.getProfielNameFromId(profileId)));
	}
	
	private void setFirstAvailableProfile() {
		if (mProfilesAdapter.getCount() > 0) {
			getActionBar().setSelectedNavigationItem(0);
		} else {
			mSelectedProfileId = Consts.NOT_A_PROFILE_ID;
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
		// TODO rework this. Now that both buttons are almost always enabled,
		// it matters less whether the service is running or not.

		// We have to use the database here because mSelectedProfileId may be 
		// NOT_A_PROFILE_ID if the service is running but the profile used to
		// run it was deleted, so we can't trust mSelectedProfileId to tell
		// us whether there are profile to choose from or not
		if (Consts.DEBUG) Log.d(Consts.LOGTAG,
			"profile ID=" + mSelectedProfileId + ", name=" +
			mPreferencesHelper.getProfielNameFromId(mSelectedProfileId));

		if (isDindyServiceRunning) {
			startStopTextView.setText(getString(R.string.stop));

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
			powerButton.setContentDescription(getString(R.string.stop));
		} else {
			if (mSelectedProfileId != Consts.NOT_A_PROFILE_ID) {
				startStopTextView.setText(getString(R.string.start));
				profileTextView.setText(" " + getString(R.string.main_profile) +
						" ");
				profileNameView.setText(
						mPreferencesHelper.getProfielNameFromId(
								mSelectedProfileId));
				powerButton.setEnabled(true);
				powerButton.setImageResource(R.drawable.power_button_selector_on);
				powerButton.setContentDescription(getString(R.string.start));
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
		startActivity(profileStartIntent);
	}

	private class DindyBroadcastReceiver extends BroadcastReceiver {
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
			if (Consts.SERVICE_STARTED.equals(action)) {
				setSelectedProfileId(DindyService.getCurrentProfileId());
				setDynamicButtons(true);
			} else if (Consts.SERVICE_STOPPED.equals(action)) {
				setDynamicButtons(false);
			}
		}
	}

	@Override
	public void onSuccess(int type, String newProfileName, long newProfileId) {
		switch (type) {
		case ProfileNameDialogFragment.DIALOG_NEW_PROFILE:
			mProfileNames.clear();
			mProfileNames.addAll(mPreferencesHelper.getAllProfileNamesSorted());
			mProfilesAdapter.notifyDataSetChanged();
			setSelectedProfileId(mPreferencesHelper.getProfileIdFromName(newProfileName));
			startProfileEditor(newProfileName, newProfileId);
			invalidateOptionsMenu();
			break;
			
		case ProfileNameDialogFragment.DIALOG_RENAME_PROFILE:
			mProfileNames.set(getActionBar().getSelectedNavigationIndex(), newProfileName);
			mProfilesAdapter.notifyDataSetChanged();
			setDynamicButtons(DindyService.isRunning());
			DindySingleProfileAppWidgetProvider.updateAllSingleProfileWidgets(
					getApplicationContext(), DindyService.getCurrentProfileId(),
					Consts.NOT_A_PROFILE_ID);
			break;

		default:
			if (Consts.DEBUG) Log.d(Consts.LOGTAG,
					"Unknown dialog type " + type);
			return;
		}
	}

	
	private ActionBar.OnNavigationListener mNavigationListener = new OnNavigationListener() {
		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			setSelectedProfileId(mPreferencesHelper.getProfileIdFromName(
				mProfilesAdapter.getItem(itemPosition)));
			return true;
		}
	};

	public static class StartupMessageDialogFragment extends MessageWithCheckboxDialogFragmentBase {
		public StartupMessageDialogFragment() {
			super(
				R.string.startup_message_dialog_text,
				R.string.startup_message_dialog_title,
				Consts.Prefs.Main.KEY_SHOW_STARTUP_MESSAGE);
		}
		
		public static StartupMessageDialogFragment newInstance() {
			return new StartupMessageDialogFragment();
		}
	}

	public static class HelpDialogFragment extends DialogFragment {
		public HelpDialogFragment() {
		}
		
		public static HelpDialogFragment newInstance() {
			return new HelpDialogFragment();
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			LayoutInflater factory = LayoutInflater.from(getActivity());
			View helpView = factory.inflate(R.layout.help_dialog, null);
			TextView helpTextView = (TextView) helpView.findViewById(
					R.id.help_dialog_text_view);
			helpTextView.setText(Html.fromHtml(getString(
	        		R.string.help_dialog_text)));
			helpTextView.setMovementMethod(LinkMovementMethod.getInstance());
			
			AlertDialog dialog = new AlertDialog.Builder(getActivity())
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(R.string.help_dialog_title)
				.setView(helpView)
				.setPositiveButton(R.string.help_dialog_ok_text,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {}
					})
				.create();
			dialog.setOwnerActivity(getActivity());
			return dialog;
		}
	}

	private LinkedList<String> mProfileNames;
	private ArrayAdapter<String> mProfilesAdapter;
	private DindyBroadcastReceiver mBroadcastReceiver = new DindyBroadcastReceiver();
	private ProfilePreferencesHelper mPreferencesHelper = null;
	private long mSelectedProfileId = Consts.NOT_A_PROFILE_ID;

	private static final int PROFILE_EDIT_REQUEST_CODE = 2;
}