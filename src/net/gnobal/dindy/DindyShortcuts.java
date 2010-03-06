package net.gnobal.dindy;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.Window;

public class DindyShortcuts extends ExternalSourceSelectionActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		final Intent intent = getIntent();
		if (intent != null) {
			final String action = intent.getAction();
			if (Consts.ACTION_START_DINDY_SERVICE.equals(action)) {
				Bundle extras = intent.getExtras();
				final long profileId = extras.getLong(Consts.EXTRA_PROFILE_ID,
						Consts.NOT_A_PROFILE_ID);
				startService(DindyService.getStartServiceIntent(
						getApplicationContext(), profileId));
				setResult(RESULT_CANCELED);
				finish();
				return;
			}
		}		
		// setTitle()
		
		showDialog(DIALOG_SELECT);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		return super.onCreateDialog(id);
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
			Intent shortcutIntent = null;
	        Intent returnIntent = new Intent();
			if (which == STOP_DINDY_ITEM_POSITION) {
				shortcutIntent = DindyService.getStopServiceIntent(
						getApplicationContext());
			} else {
				ProfilePreferencesHelper prefsHelper = 
					ProfilePreferencesHelper.instance();
				final long profileId = prefsHelper.getProfileIdFromName(text);
				//shortcutIntent = DindyService.getStartServiceBroadcastIntent(
				//		profileId);
				shortcutIntent = new Intent(Consts.ACTION_START_DINDY_SERVICE)
					.putExtra(Consts.EXTRA_PROFILE_ID, profileId)
					.setClass(getApplicationContext(), DindyShortcuts.class);
			}

			//shortcutIntent.setClassName(ShortcutCreationActivity.this,
			//		ShortcutCreationActivity.this.getClass().getName());
			returnIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
			returnIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, text);
	        //Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
	        //        this,  R.drawable.app_sample_code);
	        //intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
	        setResult(RESULT_OK, returnIntent);
			removeDialog(DIALOG_SELECT);
			finish();
		}
	};
}
