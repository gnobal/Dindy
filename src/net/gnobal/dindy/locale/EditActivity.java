package net.gnobal.dindy.locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import java.util.LinkedList;
import net.gnobal.dindy.Consts;
import net.gnobal.dindy.ProfilePreferencesHelper;
import net.gnobal.dindy.R;

public class EditActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		final String breadcrumbString = getIntent().getStringExtra(
				com.twofortyfouram.Intent.EXTRA_STRING_BREADCRUMB);
		if (breadcrumbString != null) {
			setTitle(String.format("%s%s%s", breadcrumbString,
					com.twofortyfouram.Intent.BREADCRUMB_SEPARATOR,
					getString(R.string.locale_plugin_name)));
		}
		
		mArrayAdapter = new ArrayAdapter<String>(this,
			android.R.layout.select_dialog_item, mListItems);
		
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
		super.onCreateDialog(id);
		
		AlertDialog dialog = null;
		
		switch (id) {
		case DIALOG_SELECT:
			ProfilePreferencesHelper prefsHelper = 
				ProfilePreferencesHelper.instance(); 
			mListItems.clear();
			mListItems.addAll(prefsHelper.getAllProfileNamesSorted());
			mListItems.addFirst(getString(R.string.locale_dialog_stop_dindy_text));
			mArrayAdapter.notifyDataSetChanged();
				
			dialog = new AlertDialog.Builder(this)
				.setTitle(R.string.app_name)
				.setAdapter(mArrayAdapter, mOnItemClickListener)
				.setNegativeButton(R.string.locale_dialog_cancel_text,
					mOnCancelListener)
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						setResult(RESULT_CANCELED);
						removeDialog(DIALOG_SELECT);
						finish();
					}
				})
				.create();

			// Only good if we were showing an actual activity and not just a
			// dialog
			//dialog.getWindow().setBackgroundDrawable(
			//		SharedResources.getDrawableResource(getPackageManager(),
			//				SharedResources.DRAWABLE_LOCALE_BORDER));
			break;
			
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
	
	private DialogInterface.OnClickListener mOnCancelListener =
		new  DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			setResult(RESULT_CANCELED);
			removeDialog(DIALOG_SELECT);
			finish();
		}
	};  
	
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
	
	private ArrayAdapter<String> mArrayAdapter = null;
	private LinkedList<String> mListItems = new LinkedList<String>();
	private static final int DIALOG_SELECT = 1;
	private static final int DIALOG_USAGE = 2;
	private static final int STOP_DINDY_ITEM_POSITION = 0; 
}
