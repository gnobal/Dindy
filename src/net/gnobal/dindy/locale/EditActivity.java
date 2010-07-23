package net.gnobal.dindy.locale;

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
				com.twofortyfouram.Intent.EXTRA_STRING_BREADCRUMB);
		if (breadcrumbString != null) {
			setTitle(String.format("%s%s%s", breadcrumbString,
					com.twofortyfouram.Intent.BREADCRUMB_SEPARATOR,
					getString(R.string.locale_plugin_name)));
		}
		
		SharedPreferences preferences = getSharedPreferences(
				Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
		final boolean showLocaleUsage =
			preferences.getBoolean(Consts.Prefs.Main.KEY_SHOW_LOCALE_USAGE, true);
		if (!showLocaleUsage) {
			showDialog(DIALOG_SELECT);
			return;
		}
		
		showDialog(DIALOG_USAGE);
	}

	@Override
	protected OnClickListener getOnClickListener() {
		return mOnItemClickListener;
	}
	
	@Override
	protected int getUsageDialogCheckboxResId() {
		return R.id.locale_usage_dialog_checkbox;
	}
	
	@Override
	protected int getUsageDialogLayoutResId() {
		return R.layout.locale_usage_dialog;
	}
	
	@Override
	protected String getUsageDialogPreferenceKey() {
		return Consts.Prefs.Main.KEY_SHOW_LOCALE_USAGE;
	}

	@Override
	protected int getUsageDialogTitleResId() {
		return R.string.locale_usage_dialog_title;
	}  
	
	private DialogInterface.OnClickListener mOnItemClickListener = 
		new DialogInterface.OnClickListener() {
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

			if (text.length() >
				com.twofortyfouram.Intent.MAXIMUM_BLURB_LENGTH) {
				returnIntent.putExtra(
					com.twofortyfouram.Intent.EXTRA_STRING_BLURB,
					text.substring(
						0, com.twofortyfouram.Intent.MAXIMUM_BLURB_LENGTH));
			} else {
				returnIntent.putExtra(
					com.twofortyfouram.Intent.EXTRA_STRING_BLURB, text);
			}

			returnIntent.putExtra(com.twofortyfouram.Intent.EXTRA_BUNDLE,
				storeAndForwardExtras);
			setResult(RESULT_OK, returnIntent);
			removeDialog(DIALOG_SELECT);
			finish();
		}		
	};

	//private static final int DIALOG_USAGE = 2;
}
