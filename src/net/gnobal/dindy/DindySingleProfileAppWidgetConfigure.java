package net.gnobal.dindy;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class DindySingleProfileAppWidgetConfigure extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);

		Intent profilesListIntent = new Intent(getApplicationContext(),
				ProfilesListActivity.class);
		profilesListIntent.putExtra(ProfilesListActivity.EXTRA_MODE_NAME,
				ProfilesListActivity.EXTRA_MODE_SELECT);
		DindySingleProfileAppWidgetConfigure.this.startActivityForResult(
				profilesListIntent,
				PROFILE_SELECT_REQUEST_CODE);

        // Find the widget id from the intent. 
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // If they gave us an intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
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

		DindySettings.WidgetSettings widgetSettings =
			new DindySettings.WidgetSettings();
		widgetSettings.mProfileId = data.getExtras().getLong(
				Consts.EXTRA_SELECTED_PROFILE_ID);
		widgetSettings.mWidgetType = Consts.Prefs.Widget.Type.SINGLE_PROFILE;
		ProfilePreferencesHelper prefs = ProfilePreferencesHelper.instance();
		SharedPreferences widgetPreferences = getSharedPreferences(
				Consts.Prefs.Widget.NAME, MODE_PRIVATE);
		prefs.setWidgetSettings(widgetPreferences,
				mAppWidgetId, widgetSettings);
		
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(
				getApplicationContext());
		DindySingleProfileAppWidgetProvider.updateOneSingleProfileWidget(
				getApplicationContext(), appWidgetManager,
				getPackageName(), prefs, widgetSettings, mAppWidgetId,
				DindyService.getCurrentProfileId(), widgetSettings.mProfileId);
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);
		finish();
	}

	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private static final int PROFILE_SELECT_REQUEST_CODE = 1;
}
