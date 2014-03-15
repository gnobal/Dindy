package net.gnobal.dindy;

import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

public abstract class ExternalSourceSelectionActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		setTheme(R.style.Theme_Transparent);
		mArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.select_dialog_item, mListItems);
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
			mListItems.addFirst(getString(R.string.stop_dindy));
			mArrayAdapter.notifyDataSetChanged();
				
			dialog = new AlertDialog.Builder(this)
				.setTitle(R.string.app_name)
				.setAdapter(mArrayAdapter, getOnClickListener())
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
			break;

		case DIALOG_USAGE:
			LayoutInflater factory = LayoutInflater.from(this);
			final View startupMessageView = factory.inflate(
					getUsageDialogLayoutResId(), null);
			dialog = new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(getUsageDialogTitleResId())
				.setView(startupMessageView)
				.setPositiveButton(R.string.message_dialog_ok_text,
					new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						SharedPreferences preferences = getSharedPreferences(
								Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
						CheckBox checkBox = (CheckBox) 
							((AlertDialog) dialog).findViewById(
								getUsageDialogCheckboxResId());
						SharedPreferences.Editor editor = 
							preferences.edit();
						editor.putBoolean(
								getUsageDialogPreferenceKey(),
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
	
	protected static final int DIALOG_SELECT = 1;
	protected static final int DIALOG_USAGE = 2;
	protected LinkedList<String> mListItems = new LinkedList<String>();
	protected static final int STOP_DINDY_ITEM_POSITION = 0; 
	
	protected abstract DialogInterface.OnClickListener getOnClickListener();
	
	protected abstract int getUsageDialogLayoutResId();
	
	protected abstract int getUsageDialogTitleResId();
	
	protected abstract int getUsageDialogCheckboxResId();
	
	protected abstract String getUsageDialogPreferenceKey();
	
	private DialogInterface.OnClickListener mOnCancelListener =
		new  DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			setResult(RESULT_CANCELED);
			removeDialog(DIALOG_SELECT);
			finish();
		}
	};  

	private ArrayAdapter<String> mArrayAdapter = null;
}
