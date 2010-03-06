package net.gnobal.dindy;

import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public abstract class ExternalSourceSelectionActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		
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
			mListItems.addFirst(getString(R.string.locale_dialog_stop_dindy_text));
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

			// Only good if we were showing an actual activity and not just a
			// dialog
			//dialog.getWindow().setBackgroundDrawable(
			//		SharedResources.getDrawableResource(getPackageManager(),
			//				SharedResources.DRAWABLE_LOCALE_BORDER));
			break;

		default:
			return null;	
		}
		
		dialog.setOwnerActivity(this);

		return dialog;
	}
	
	protected static final int DIALOG_SELECT = 1;
	protected LinkedList<String> mListItems = new LinkedList<String>();
	protected static final int STOP_DINDY_ITEM_POSITION = 0; 
	protected abstract DialogInterface.OnClickListener getOnClickListener();
	
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
