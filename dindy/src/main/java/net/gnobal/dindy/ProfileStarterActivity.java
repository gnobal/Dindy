package net.gnobal.dindy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TimePicker;

import java.util.Calendar;

public class ProfileStarterActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPreferencesHelper = ProfilePreferencesHelper.instance();
		Bundle extras = getIntent().getExtras();
		mSelectedProfileId = extras.getLong(Consts.EXTRA_PROFILE_ID);
		mIntentSource = extras.getInt(Consts.EXTRA_INTENT_SOURCE);

		startDindyServiceWithTimeLimit();
	}

	void setDynamicViews(RadioGroup radioGroup, int hour, int minute,
			Button okButton, RadioButton timeOfDayButton) {
		final int selectedId = radioGroup.getCheckedRadioButtonId();
		if (selectedId != R.id.duration_radio_button &&
			selectedId != R.id.time_of_day_radio_button) {
			// Shouldn't happen, but let's make sure we behave
			okButton.setEnabled(true);
			timeOfDayButton.setText(R.string.time_of_day_radio_button_text);
			return;
		}

		if (selectedId == R.id.time_of_day_radio_button){
			// adjust to duration to see 
			int[] duration = durationFrom(Calendar.getInstance(), hour, minute);
			hour = duration[0];
			minute = duration[1];

			String timeOfDayText =
				getString(R.string.time_of_day_radio_button_text) +
				" (in " + hour + "h " + minute + "m)";
			timeOfDayButton.setText(timeOfDayText);
		}

		okButton.setEnabled(hour != 0 || minute != 0);
	}

	private static int[] durationFrom(Calendar c, int hour, int minute) {
		final long diffMinutes = Utils.diffTimeMillis(c,
			hour, minute, false) / Consts.MILLIS_IN_MINUTE;

		// Adjust to duration
		hour = ((int) (diffMinutes / Consts.MINUTES_IN_HOUR));
		minute = ((int) (diffMinutes % Consts.MINUTES_IN_HOUR));

		return new int[]{hour, minute};
	}

	private void startDindyServiceWithTimeLimit() {
		SharedPreferences profilePreferences =
			mPreferencesHelper.getPreferencesForProfile(getApplicationContext(),
				mSelectedProfileId);
		boolean usesTimeLimit = profilePreferences.getBoolean(
			Consts.Prefs.Profile.KEY_USE_TIME_LIMIT,
			Consts.Prefs.Profile.VALUE_USE_TIME_LIMIT_DEFAULT);
		boolean autoUseTimeLimit = profilePreferences.getBoolean(
			Consts.Prefs.Profile.KEY_AUTO_USE_LAST_TIME_LIMIT,
			Consts.Prefs.Profile.VALUE_AUTO_USE_LAST_TIME_LIMIT_DEFAULT);
		if (usesTimeLimit) {
			mTimeLimitType = Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_TYPE;
			mTimeLimitMinutes = Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_MINUTES;
			mTimeLimitHours = Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_HOURS;
			if (profilePreferences.contains(Consts.Prefs.Profile.KEY_LAST_TIME_LIMIT_TYPE)) {
				mTimeLimitType = profilePreferences.getInt(Consts.Prefs.Profile.KEY_LAST_TIME_LIMIT_TYPE,
					Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_TYPE);
				mTimeLimitMinutes = profilePreferences.getLong(
					Consts.Prefs.Profile.KEY_LAST_TIME_LIMIT_MINUTES,
					Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_MINUTES);
				mTimeLimitHours = profilePreferences.getLong(
					Consts.Prefs.Profile.KEY_LAST_TIME_LIMIT_HOURS,
					Consts.Prefs.Profile.VALUE_LAST_TIME_LIMIT_DEFAULT_HOURS);
			}

			if (autoUseTimeLimit) {
				final long timeLimitMillis = Utils.getTimeLimitMillis(mTimeLimitType,
					(int) mTimeLimitHours, (int) mTimeLimitMinutes);
				if (timeLimitMillis == 0) {
					finish();
					return;
				}

				startDindyServiceWithSelectedProfileId(timeLimitMillis);
				finish();
				return;
			} else {
				TimeLimitDialogFragment.newInstance().show(getFragmentManager(), "time_limit_dialog");
				return;
			}
		}

		startDindyServiceWithSelectedProfileId(Consts.NOT_A_TIME_LIMIT);
		finish();
	}

	public static class TimeLimitDialogFragment extends DialogFragment {
		public TimeLimitDialogFragment() {
		}

		static TimeLimitDialogFragment newInstance() {
			return new TimeLimitDialogFragment();
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			LayoutInflater factory = LayoutInflater.from(getActivity());
			View timeLimitView = factory.inflate(R.layout.time_limit_dialog, null);

			final TimeLimitDialogListener listener = new TimeLimitDialogListener();
			final AlertDialog dialog = new AlertDialog.Builder(getActivity())
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.time_limit_dialog_title)
				.setView(timeLimitView)
				.setPositiveButton(R.string.time_limit_dialog_ok_text, listener)
				.setNegativeButton(R.string.time_limit_dialog_cancel_text, listener)
				.create();
			dialog.setOwnerActivity(getActivity());

			return dialog;
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			super.onDismiss(dialog);

			if (getActivity() != null && !mButtonClicked) {
				getActivity().finish();
			}
			mButtonClicked = false;
		}

		@Override
		public void onResume() {
			super.onResume();
			final ProfileStarterActivity parentActivity = (ProfileStarterActivity) getActivity();
			final Dialog dialog = getDialog();

			RadioGroup radioGroup = (RadioGroup) dialog.findViewById(
					R.id.time_limit_radio_group);
			if (parentActivity.mTimeLimitType == Consts.Prefs.Profile.TimeLimitType.DURATION) {
				radioGroup.check(R.id.duration_radio_button);
			} else {
				radioGroup.check(R.id.time_of_day_radio_button);
			}
			radioGroup.setOnCheckedChangeListener(new RadioGroupCheckedChangeListener(dialog));

			TimePicker timePicker = (TimePicker) dialog.findViewById(
				R.id.time_limit_time_picker);
			timePicker.setIs24HourView(
					parentActivity.mTimeLimitType == Consts.Prefs.Profile.TimeLimitType.DURATION ||
				DateFormat.is24HourFormat(parentActivity.getApplicationContext()));
			timePicker.setCurrentHour((int) parentActivity.mTimeLimitHours);
			timePicker.setCurrentMinute((int) parentActivity.mTimeLimitMinutes);
			Button okButton = ((AlertDialog) dialog).getButton(
				AlertDialog.BUTTON_POSITIVE);
			RadioButton timeOfDayButton = (RadioButton) dialog.findViewById(
				R.id.time_of_day_radio_button);
			parentActivity.setDynamicViews(
				radioGroup,	timePicker.getCurrentHour(), timePicker.getCurrentMinute(),
				okButton, timeOfDayButton);

			timePicker.setOnTimeChangedListener(new TimeChangedListener(
				radioGroup, okButton, timeOfDayButton));
		}

		private class TimeLimitDialogListener implements DialogInterface.OnClickListener {
			TimeLimitDialogListener() {
				mParentActivity = (ProfileStarterActivity) getActivity();
			}

			public void onClick(DialogInterface dialog,
					int which) {

				mButtonClicked = true;
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					onPositiveButtonClicked(dialog);
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					onNegativeButtonClicked();
					break;
				default:
					//noinspection UnnecessaryReturnStatement
					return;
				}
			}

			private void onNegativeButtonClicked() {
				mParentActivity.startDindyServiceWithSelectedProfileId(Consts.NOT_A_TIME_LIMIT);
				mParentActivity.finish();
			}

			private void onPositiveButtonClicked(DialogInterface dialog) {
				TimePicker timePicker = (TimePicker) ((AlertDialog) dialog).findViewById(
						R.id.time_limit_time_picker);
				boolean autoUseTimeLimit = ((CheckBox) ((AlertDialog) dialog).findViewById(
					R.id.auto_use_time_limit_checkbox)).isChecked();
				RadioGroup radioGroup = (RadioGroup) ((AlertDialog) dialog).findViewById(
						R.id.time_limit_radio_group);
				int selectedTimeLimitType = Consts.Prefs.Profile.TimeLimitType.TIME_OF_DAY;
				if (radioGroup.getCheckedRadioButtonId() == R.id.duration_radio_button) {
					selectedTimeLimitType = Consts.Prefs.Profile.TimeLimitType.DURATION;
				}

				final int selectedTimeLimitHours = timePicker.getCurrentHour();
				final int selectedTimeLimitMinutes = timePicker.getCurrentMinute();
				final long newTimeLimitMillis = Utils.getTimeLimitMillis(selectedTimeLimitType,
					selectedTimeLimitHours, selectedTimeLimitMinutes);

				if (newTimeLimitMillis == 0) {
					mParentActivity.finish();
					return;
				}

				// Everything is OK, save the user's preferences
				SharedPreferences profilePrefs =
					mParentActivity.mPreferencesHelper.getPreferencesForProfile(
						mParentActivity.getApplicationContext(), mParentActivity.mSelectedProfileId
					);
				SharedPreferences.Editor editor = profilePrefs.edit();
				editor.putLong(Consts.Prefs.Profile.KEY_LAST_TIME_LIMIT_HOURS,
					selectedTimeLimitHours);
				editor.putInt(Consts.Prefs.Profile.KEY_LAST_TIME_LIMIT_TYPE,
					selectedTimeLimitType);
				editor.putLong(Consts.Prefs.Profile.KEY_LAST_TIME_LIMIT_MINUTES,
					selectedTimeLimitMinutes);
				editor.putBoolean(Consts.Prefs.Profile.KEY_AUTO_USE_LAST_TIME_LIMIT, autoUseTimeLimit);
				editor.apply();

				mParentActivity.startDindyServiceWithSelectedProfileId(newTimeLimitMillis);
				mParentActivity.finish();
			}

			private final ProfileStarterActivity mParentActivity;
		}

		private class RadioGroupCheckedChangeListener implements RadioGroup.OnCheckedChangeListener {
			RadioGroupCheckedChangeListener(Dialog dialog) {
				mParentActivity = (ProfileStarterActivity) getActivity();
				mDialog = dialog;
			}

			public void onCheckedChanged(RadioGroup group, int id) {
				TimePicker timePicker = (TimePicker) mDialog.findViewById(
						R.id.time_limit_time_picker);
		        final Calendar now = Calendar.getInstance();
				if (id == R.id.duration_radio_button) {
					// Must change to 24-hour view first so that we'll get the hour
					// and minute in 24-hour format
					timePicker.setIs24HourView(true);
					int[] duration = durationFrom(now, timePicker.getCurrentHour(),
							timePicker.getCurrentMinute());
					timePicker.setCurrentHour((duration[0]));
					timePicker.setCurrentMinute((duration[1]));
					RadioButton timeOfDayButton = (RadioButton) mDialog.findViewById(
							R.id.time_of_day_radio_button);
					timeOfDayButton.setText(R.string.time_of_day_radio_button_text);
				} else if (id == R.id.time_of_day_radio_button) {
					now.add(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
					now.add(Calendar.MINUTE, timePicker.getCurrentMinute());
			        timePicker.setCurrentHour(now.get(Calendar.HOUR_OF_DAY));
			        timePicker.setCurrentMinute(now.get(Calendar.MINUTE));
			        // Must disable 24-hour view last so that the calculations we made
			        // will be correct
					timePicker.setIs24HourView(
							DateFormat.is24HourFormat(mParentActivity.getApplicationContext()));
				}
			}

			private final ProfileStarterActivity mParentActivity;
			private final Dialog mDialog;
		}

		private class TimeChangedListener implements TimePicker.OnTimeChangedListener {
			TimeChangedListener(RadioGroup radioGroup, Button okButton, RadioButton timeOfDayButton) {
				mParentActivity = (ProfileStarterActivity) getActivity();
				mOkButton = okButton;
				mRadioGroup = radioGroup;
				mTimeOfDayButton = timeOfDayButton;
			}

			public void onTimeChanged(TimePicker timePicker, int hour, int minute) {
				mParentActivity.setDynamicViews(mRadioGroup, hour, minute, mOkButton, mTimeOfDayButton);
			}

			private final ProfileStarterActivity mParentActivity;
			private final Button mOkButton;
			private final RadioGroup mRadioGroup;
			private final RadioButton mTimeOfDayButton;
		}

		private boolean mButtonClicked = false;
	}

	private void startDindyServiceWithSelectedProfileId(long timeLimitMillis) {
		startService(DindyService.getStartServiceIntent(getApplicationContext(),
				mSelectedProfileId, null, mIntentSource,
				timeLimitMillis, false));
	}

	private ProfilePreferencesHelper mPreferencesHelper = null;
	private int mTimeLimitType;
	private long mTimeLimitMinutes;
	private long mTimeLimitHours;
	private long mSelectedProfileId;
	private int mIntentSource;
}