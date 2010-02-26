package net.gnobal.dindy.locale;

import java.util.LinkedList;

import com.twofortyfouram.SharedResources;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
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
		showDialog(DIALOG_SELECT);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		super.onCreateDialog(id);
		
		switch (id) {
		case DIALOG_SELECT:
			ProfilePreferencesHelper prefsHelper = 
				ProfilePreferencesHelper.instance(); 
			mListItems.clear();
			mListItems.addAll(prefsHelper.getAllProfileNamesSorted());
			mListItems.addFirst(getString(R.string.locale_dialog_stop_dindy_text));
			mArrayAdapter.notifyDataSetChanged();
				
			AlertDialog dialog = new AlertDialog.Builder(this)
				.setTitle(R.string.app_name)
				.setAdapter(mArrayAdapter, mOnItemClickListener)
				.setNegativeButton(R.string.locale_dialog_cancel_text,
					mOnCancelListener)
				.create();

			// Only good if we were showing an actual activity and not just a
			// dialog
			//dialog.getWindow().setBackgroundDrawable(
			//		SharedResources.getDrawableResource(getPackageManager(),
			//				SharedResources.DRAWABLE_LOCALE_BORDER));
			dialog.setOwnerActivity(this);
			//dialog.getListView().set
			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					setResult(RESULT_CANCELED);
					finish();
					removeDialog(DIALOG_SELECT);
				}
			});
			
			return dialog;
			
		default:
			return null;	
		}
	}
	
	private DialogInterface.OnClickListener mOnCancelListener =
		new  DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			setResult(RESULT_CANCELED);
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
			// Intent = 
			if (which == STOP_DINDY_ITEM_POSITION) {
				storeAndForwardExtras.putString(Consts.EXTRA_LOCALE_ACTION,
					Consts.EXTRA_LOCALE_ACTION_STOP_SERVICE);
			} else {
				storeAndForwardExtras.putString(Consts.EXTRA_LOCALE_ACTION,
					Consts.EXTRA_LOCALE_ACTION_START_SERVICE);
				ProfilePreferencesHelper prefsHelper = 
					ProfilePreferencesHelper.instance();
				final long profileId = prefsHelper.getProfileIdFromName(text);
				storeAndForwardExtras.putLong(Consts.EXTRA_SELECTED_PROFILE_ID,
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
			finish();
		}		
	};  
	
	private ArrayAdapter<String> mArrayAdapter = null;
	private LinkedList<String> mListItems = new LinkedList<String>();
	private static final int DIALOG_SELECT = 1;
	private static final int STOP_DINDY_ITEM_POSITION = 0; 
}
