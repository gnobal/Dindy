package net.gnobal.dindy;

import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class ExternalSourceSelectionActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		setTheme(R.style.Theme_Transparent);
	}

	protected static abstract class ExternalSourceSelectionUsageDialogFragment
		extends MessageWithCheckboxDialogFragmentBase {
		protected ExternalSourceSelectionUsageDialogFragment(
			int usageDialogTextResId,
			int usageDialogTitleResId,
			String usageDialogPreferenceKey) {
			super(usageDialogTextResId, usageDialogTitleResId, usageDialogPreferenceKey);			
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			super.onCancel(dialog);
			getActivity().setResult(RESULT_CANCELED);
			getActivity().finish();			
		}

		@Override
		protected void onPositiveButtonClicked(DialogInterface dialog) {
			super.onPositiveButtonClicked(dialog);
			showSelectionDialog();
		}

		protected abstract void showSelectionDialog();
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