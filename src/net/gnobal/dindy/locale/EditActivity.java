package net.gnobal.dindy.locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
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
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = super.onCreateDialog(id);
		
		if (dialog != null) {
			return dialog;
		}
		
		switch (id) {
		case DIALOG_USAGE:
			LayoutInflater factory = LayoutInflater.from(this);
			final View startupMessageView = factory.inflate(
					R.layout.locale_usage_dialog, null);
			dialog = new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(R.string.locale_dialog_title)
				.setView(startupMessageView)
				.setPositiveButton(R.string.locale_dialog_ok_text,
					new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						SharedPreferences preferences = getSharedPreferences(
								Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
						CheckBox checkBox = (CheckBox) 
							((AlertDialog) dialog).findViewById(
								R.id.locale_usage_dialog_checkbox);
						SharedPreferences.Editor editor = 
							preferences.edit();
						editor.putBoolean(
								Consts.Prefs.Main.KEY_SHOW_LOCALE_USAGE,
								!checkBox.isChecked());
						editor.commit();
						removeDialog(DIALOG_USAGE);
						showDialog(DIALOG_SELECT);
					}})
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						setResult(RESULT_CANCELED);
						finish();
						removeDialog(DIALOG_USAGE);
					}
				})
				.create();
			break;

		default:
			return null;	
		}
		
		dialog.setOwnerActivity(this);

		return dialog;
	}
	
	@Override
	protected OnClickListener getOnClickListener() {
		return mOnItemClickListener;
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
			} else {
				storeAndForwardExtras.putString(Consts.EXTRA_EXTERNAL_ACTION,
					Consts.EXTRA_EXTERNAL_ACTION_START_SERVICE);
				ProfilePreferencesHelper prefsHelper = 
					ProfilePreferencesHelper.instance();
				final long profileId = prefsHelper.getProfileIdFromName(text);
				storeAndForwardExtras.putLong(Consts.EXTRA_PROFILE_ID,
					profileId);	
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
	
	private static final int DIALOG_USAGE = 2;
}
