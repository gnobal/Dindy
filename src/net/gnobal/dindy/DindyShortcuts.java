package net.gnobal.dindy;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Parcelable;

public class DindyShortcuts extends ExternalSourceSelectionActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final String action = intent.getAction();
		if (Consts.ACTION_START_DINDY_SERVICE.equals(action)) {
			Bundle extras = intent.getExtras();
			final long profileId = extras.getLong(Consts.EXTRA_PROFILE_ID,
					Consts.NOT_A_PROFILE_ID);
			if (DindyService.getCurrentProfileId() == profileId) {
				// The user clicked the shortcut that starts the same profile as
				// the one running now. We assume he/she wants to stop the 
				// profile from running
				stopDindyService();
				return;
			}
			
			final String profileName = extras.getString(
					Consts.EXTRA_PROFILE_NAME);
			startService(DindyService.getStartServiceIntent(
					getApplicationContext(), profileId, profileName,
					Consts.INTENT_SOURCE_SHORTCUT, Consts.NOT_A_TIME_LIMIT));
			setResult(RESULT_CANCELED);
			finish();
			return;
		} else if (Consts.ACTION_STOP_DINDY_SERVICE.equals(action)) {
			stopDindyService();
			return;
		}

		SharedPreferences preferences = getSharedPreferences(
				Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
		final boolean showShortcutsUsage =
			preferences.getBoolean(Consts.Prefs.Main.KEY_SHOW_SHORTCUTS_USAGE,
					true);
		if (!showShortcutsUsage) {
			DindyShortcutSelectionDialogFragment.newInstance().show(
				getFragmentManager(), DindyShortcutSelectionDialogFragment.TAG);
			return;
		}

		DindyShortcutSelectionUsageDialogFragment.newInstance().show(
			getFragmentManager(), "shortcuts_usage");
	}

	private void stopDindyService() {
		stopService(DindyService.getStopServiceIntent(
				getApplicationContext()));
		setResult(RESULT_CANCELED);
		finish();
	}

	public static class DindyShortcutSelectionUsageDialogFragment extends ExternalSourceSelectionUsageDialogFragment {
		public static DindyShortcutSelectionUsageDialogFragment newInstance() {
			return new DindyShortcutSelectionUsageDialogFragment();
		}

		public DindyShortcutSelectionUsageDialogFragment() {
			super(
				R.layout.shortcuts_usage_dialog,
				R.string.shortcuts_usage_dialog_title,
				R.id.shortcuts_usage_dialog_checkbox,
				Consts.Prefs.Main.KEY_SHOW_SHORTCUTS_USAGE);
		}

		@Override
		protected void showSelectionDialog() {
			DindyShortcutSelectionDialogFragment.newInstance().show(
				getFragmentManager(), DindyShortcutSelectionDialogFragment.TAG);
		}
	}

	public static class DindyShortcutSelectionDialogFragment extends ExternalSourceSelectionDialogFragment {
		public static DindyShortcutSelectionDialogFragment newInstance() {
			return new DindyShortcutSelectionDialogFragment();
		}

		public DindyShortcutSelectionDialogFragment() {
		}

		@Override
		protected OnClickListener createSelectionOnClickListener() {
			return new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String text = mListItems.get(which);
					Intent shortcutIntent = null;
			        Intent returnIntent = new Intent();
					if (which == STOP_DINDY_ITEM_POSITION) {
						shortcutIntent = new Intent(Consts.ACTION_STOP_DINDY_SERVICE)
							.setClass(getActivity().getApplicationContext(), DindyShortcuts.class);			
					} else {
						ProfilePreferencesHelper prefsHelper = 
							ProfilePreferencesHelper.instance();
						final long profileId = prefsHelper.getProfileIdFromName(text);
						shortcutIntent =
							DindyService.prepareStartServiceIntent(
									new Intent(Consts.ACTION_START_DINDY_SERVICE),
										profileId, text, Consts.INTENT_SOURCE_SHORTCUT,
										Consts.NOT_A_TIME_LIMIT)
							.setClass(getActivity().getApplicationContext(), DindyShortcuts.class);					
					}

					returnIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
					returnIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, text);
			        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
			        		getActivity(), R.drawable.shortcut_button);
			        returnIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
			        		iconResource);
			        getActivity().setResult(RESULT_OK, returnIntent);
					getActivity().finish();
				}			
			};
		}

		public static final String TAG = "shortcut_selection";
	}
}