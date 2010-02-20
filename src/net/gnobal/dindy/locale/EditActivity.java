package net.gnobal.dindy.locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import net.gnobal.dindy.ProfilePreferencesHelper;
import net.gnobal.dindy.ProfilesListActivity;
import net.gnobal.dindy.Consts;

public class EditActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setResult(RESULT_CANCELED);
		
		Intent profilesListIntent = new Intent(getApplicationContext(),
				ProfilesListActivity.class);
		profilesListIntent.putExtra(ProfilesListActivity.EXTRA_MODE_NAME,
				ProfilesListActivity.EXTRA_MODE_SELECT);
		startActivityForResult(profilesListIntent, PROFILE_SELECT_REQUEST_CODE);

	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_CANCELED) {
			finish();
			return;
		}
		
		if (requestCode != PROFILE_SELECT_REQUEST_CODE) {
			return;
		}
		
		Intent returnIntent = new Intent();
		final Bundle storeAndForwardExtras = new Bundle();
		final long selectedProfileId =
			data.getExtras().getLong(Consts.EXTRA_SELECTED_PROFILE_ID);
		storeAndForwardExtras.putLong(Consts.EXTRA_SELECTED_PROFILE_ID,
				selectedProfileId);
		ProfilePreferencesHelper prefsHelper = 
			ProfilePreferencesHelper.instance();
		final String selectedProfileName = prefsHelper.getProfielNameFromId(
				selectedProfileId);
		if (selectedProfileName.length() >
			com.twofortyfouram.Intent.MAXIMUM_BLURB_LENGTH) {
			returnIntent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_BLURB,
					selectedProfileName.substring(0,
							com.twofortyfouram.Intent.MAXIMUM_BLURB_LENGTH));
		} else {
			returnIntent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_BLURB,
					selectedProfileName);
		}
		returnIntent.putExtra(com.twofortyfouram.Intent.EXTRA_BUNDLE,
				storeAndForwardExtras);
		setResult(RESULT_OK, returnIntent);
		finish();
	}

	private final int PROFILE_SELECT_REQUEST_CODE = 1;
}
