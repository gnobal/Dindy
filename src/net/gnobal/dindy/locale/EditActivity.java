package net.gnobal.dindy.locale;

import com.twofortyfouram.locale.BreadCrumber;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import net.gnobal.dindy.Consts;
import net.gnobal.dindy.ProfilePreferencesHelper;
import net.gnobal.dindy.R;

public class EditActivity extends net.gnobal.dindy.ExternalSourceSelectionActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final String breadcrumbString = getIntent().getStringExtra(
				com.twofortyfouram.locale.Intent.EXTRA_STRING_BREADCRUMB);
		if (breadcrumbString != null) {
			setTitle(BreadCrumber.generateBreadcrumb(getApplicationContext(),
				getIntent(), getString(R.string.locale_plugin_name)));
		}

		SharedPreferences preferences = getSharedPreferences(
				Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
		final boolean showLocaleUsage =
			preferences.getBoolean(Consts.Prefs.Main.KEY_SHOW_LOCALE_USAGE, true);
		if (!showLocaleUsage) {
			EditActivitySelectionDialogFragment.newInstance().show(
				getFragmentManager(), EditActivitySelectionDialogFragment.TAG);
			return;
		}

		EditActivitySelectionUsageDialogFragment.newInstance().show(
			getFragmentManager(), "locale_usage");
	}

	public static class EditActivitySelectionUsageDialogFragment extends ExternalSourceSelectionUsageDialogFragment {
		public static EditActivitySelectionUsageDialogFragment newInstance() {
			return new EditActivitySelectionUsageDialogFragment();
		}
		
		public EditActivitySelectionUsageDialogFragment() {
			super(
				R.layout.locale_usage_dialog,
				R.string.locale_usage_dialog_title,
				R.id.locale_usage_dialog_checkbox,
				Consts.Prefs.Main.KEY_SHOW_LOCALE_USAGE);
		}

		@Override
		protected void showSelectionDialog() {
			EditActivitySelectionDialogFragment.newInstance().show(
				getFragmentManager(), EditActivitySelectionDialogFragment.TAG);			
		}
	}

	public static class EditActivitySelectionDialogFragment extends ExternalSourceSelectionDialogFragment {
		public static EditActivitySelectionDialogFragment newInstance() {
			return new EditActivitySelectionDialogFragment();
		}

		public EditActivitySelectionDialogFragment() {
		}
		
		@Override
		protected OnClickListener createSelectionOnClickListener() {
			return new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String text = mListItems.get(which);
					Intent returnIntent = new Intent();
					final Bundle storeAndForwardExtras = new Bundle();
					if (which == STOP_DINDY_ITEM_POSITION) {
						storeAndForwardExtras.putString(Consts.EXTRA_EXTERNAL_ACTION,
							Consts.EXTRA_EXTERNAL_ACTION_STOP_SERVICE);
						returnIntent.setData(
								Uri.withAppendedPath(Uri.parse("dindy://profile/id/locale/"),
										String.valueOf(Consts.NOT_A_PROFILE_ID)));
					} else {
						storeAndForwardExtras.putString(Consts.EXTRA_EXTERNAL_ACTION,
							Consts.EXTRA_EXTERNAL_ACTION_START_SERVICE);
						ProfilePreferencesHelper prefsHelper = 
							ProfilePreferencesHelper.instance();
						final long profileId = prefsHelper.getProfileIdFromName(text);
						storeAndForwardExtras.putLong(Consts.EXTRA_PROFILE_ID,
							profileId);	
						storeAndForwardExtras.putString(
								Consts.EXTRA_PROFILE_NAME, text);
						returnIntent.setData(
								Uri.withAppendedPath(Uri.parse("dindy://profile/id/locale/"),
										String.valueOf(profileId)));
					}

					
					final int maxBlurbLength =
						getResources().getInteger(R.integer.twofortyfouram_locale_maximum_blurb_length); 
					if (text.length() > maxBlurbLength) {
						returnIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB,
							text.substring(0, maxBlurbLength));
					} else {
						returnIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, text);
					}

					returnIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE,
						storeAndForwardExtras);
					getActivity().setResult(RESULT_OK, returnIntent);
					getActivity().finish();
				}
			};
		}

		public static final String TAG = "locale_selection";
	}
}