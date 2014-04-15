package net.gnobal.dindy;

import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

public class ExternalSourceSelectionActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		setTheme(R.style.Theme_Transparent);
	}

	protected static abstract class ExternalSourceSelectionUsageDialogFragment extends DialogFragment {
		protected ExternalSourceSelectionUsageDialogFragment(
			int usageDialogLayoutResId, int usageDialogTitleResId,
			int usageDialogCheckboxResId, String usageDialogPreferenceKey) {
			mUsageDialogLayoutResId = usageDialogLayoutResId;
			mUsageDialogTitleResId = usageDialogTitleResId;
			mUsageDialogCheckboxResId = usageDialogCheckboxResId;
			mUsageDialogPreferenceKey = usageDialogPreferenceKey;			
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			super.onCreateDialog(savedInstanceState);
			LayoutInflater factory = LayoutInflater.from(getActivity());
			final View startupMessageView = factory.inflate(
					mUsageDialogLayoutResId, null);
			AlertDialog dialog = new AlertDialog.Builder(getActivity())
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(mUsageDialogTitleResId)
				.setView(startupMessageView)
				.setPositiveButton(R.string.message_dialog_ok_text,
					new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						SharedPreferences preferences = getActivity().getSharedPreferences(
								Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
						CheckBox checkBox = (CheckBox) 
							((AlertDialog) dialog).findViewById(
								mUsageDialogCheckboxResId);
						SharedPreferences.Editor editor = 
							preferences.edit();
						editor.putBoolean(
								mUsageDialogPreferenceKey,
								!checkBox.isChecked());
						editor.commit();
						showSelectionDialog();
					}})
				.create();
			dialog.setOwnerActivity(getActivity());
			return dialog;
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			getActivity().setResult(RESULT_CANCELED);
			getActivity().finish();			
		}

		protected abstract void showSelectionDialog();
		
		private final int mUsageDialogLayoutResId;
		private final int mUsageDialogTitleResId;
		private final int mUsageDialogCheckboxResId;
		private final String mUsageDialogPreferenceKey;
	}

	protected static abstract class ExternalSourceSelectionDialogFragment extends DialogFragment {
		protected ExternalSourceSelectionDialogFragment() {
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);

			mListItems = new LinkedList<String>();
			mArrayAdapter = new ArrayAdapter<String>(activity,
				android.R.layout.select_dialog_item, mListItems);			
		};
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			super.onCreateDialog(savedInstanceState);
			ProfilePreferencesHelper prefsHelper = 
					ProfilePreferencesHelper.instance(); 
				mListItems.clear();
				mListItems.addAll(prefsHelper.getAllProfileNamesSorted());
				mListItems.addFirst(getString(R.string.stop_dindy));
				mArrayAdapter.notifyDataSetChanged();
					
				AlertDialog dialog = new AlertDialog.Builder(getActivity())
					.setTitle(R.string.app_name)
					.setAdapter(mArrayAdapter, createSelectionOnClickListener())
					.setNegativeButton(R.string.locale_dialog_cancel_text,
						mOnCancelListener)
					.create();
				dialog.setOwnerActivity(getActivity());
				return dialog;
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			mOnCancelListener.onClick(dialog, -1);
		}

		protected abstract DialogInterface.OnClickListener createSelectionOnClickListener();
		
		protected LinkedList<String> mListItems;
		protected static final int STOP_DINDY_ITEM_POSITION = 0; 

		private ArrayAdapter<String> mArrayAdapter = null;
		private DialogInterface.OnClickListener mOnCancelListener =
			new  DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				getActivity().setResult(RESULT_CANCELED);
				getActivity().finish();
			}
		};
	}
}