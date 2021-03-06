package net.gnobal.dindy;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

public class DindySingleProfileAppWidgetProvider extends AppWidgetProvider {
    @Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		String ids = "";
		for (int appWidgetId : appWidgetIds) {
			ids += appWidgetId + ", ";
		}
		Log.d(Consts.LOGTAG, "onUpdate: " + ids);

		updateAllSingleProfileWidgets(context, Consts.NOT_A_PROFILE_ID, 
				Consts.NOT_A_PROFILE_ID);
    }
    
    @Override
	public void onDeleted(Context context, int[] appWidgetIds) {
    	SharedPreferences widgetPrefs = context.getSharedPreferences(
				Consts.Prefs.Widget.NAME, Context.MODE_PRIVATE);
    	ProfilePreferencesHelper prefsHelper = ProfilePreferencesHelper.instance();
		for (int appWidgetId : appWidgetIds) {
			Log.d(Consts.LOGTAG, "deleting widget ID " + appWidgetId);
			prefsHelper.deleteWidgetSettings(widgetPrefs, appWidgetId);
		}
	}

    // See
    // http://groups.google.com/group/android-developers/browse_thread/thread/365d1ed3aac30916
	@Override 
	public void onReceive(Context context, Intent intent) { 
		final String action = intent.getAction(); 
		if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				this.onDeleted(context, new int[] { appWidgetId });
			}
		} else {
			super.onReceive(context, intent);
		}
	}

	static void updateAllSingleProfileWidgets(Context context,
			long activeProfileId, long previousProfileId) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(
				context);
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
				new ComponentName(context,
						DindySingleProfileAppWidgetProvider.class));
		
    	SharedPreferences widgetPrefs = context.getSharedPreferences(
    			Consts.Prefs.Widget.NAME, Context.MODE_PRIVATE);
    	ProfilePreferencesHelper prefsHelper = 
    		ProfilePreferencesHelper.instance();
    	String packageName = context.getPackageName();
    	final int N = appWidgetIds.length;
    	for (int i = N-1; i >=0; --i) {
			Log.d(Consts.LOGTAG, "updating widget ID " + appWidgetIds[i] + " active profile ID "
					+ activeProfileId + " previous profile ID " + previousProfileId);
    		DindySettings.WidgetSettings widgetSettings = 
    			prefsHelper.getWidgetSettings(widgetPrefs, appWidgetIds[i]);
    		if (widgetSettings == null) {
    			continue;
    		}
    		if (widgetSettings.mWidgetType == Consts.Prefs.Widget.Type.SINGLE_PROFILE) {
    			updateOneSingleProfileWidget(context, appWidgetManager, packageName,
    					prefsHelper, widgetSettings, appWidgetIds[i],
    					activeProfileId, previousProfileId);
    		}
    	}
	}

	static void updateOneSingleProfileWidget(Context context,
    		AppWidgetManager appWidgetManager, String packageName, 
    		ProfilePreferencesHelper prefsHelper,
    		DindySettings.WidgetSettings widgetSettings, int widgetId,
    		long activeProfileId, long previousProfileId) {
    	RemoteViews views;
    	PendingIntent pendingIntent;
    	boolean profileExists = true;
    	// NOTE: the order of this if-else clause matters:
    	// First, we want to check whether the widget's profile ID is the active
    	// one. If it is, we draw an active widget
    	// Second, we check whether the profile ID even exists. If it doesn't,
    	// we disable the widget entirely
    	// Third, if it's not currently running, we draw an inactive widget 
    	// that can be clicked.
    	//
    	// When called from the DindySingleProfileAppWidgetConfigure class
    	// widgetSettings.mProfileId == previousProfileId is always (!) true
    	if (widgetSettings.mProfileId == activeProfileId) {
    		views = new RemoteViews(packageName,
    				R.layout.single_profile_appwidget);
    		views.setImageViewResource(R.id.single_profile_app_widget_image_button,
    				R.drawable.app_widget_button_selector_off);
    		views.setContentDescription(R.id.single_profile_app_widget_image_button,
    				context.getString(R.string.stop));
			pendingIntent = PendingIntent.getService(context, 0,
					DindyService.getStopServiceIntentForPendingIntent(context), 0);
    	} else if (!(profileExists = prefsHelper.profileExists(widgetSettings.mProfileId))) {
    		views = new RemoteViews(packageName,
    				R.layout.single_profile_appwidget);
    		views.setImageViewResource(
    				R.id.single_profile_app_widget_image_button,
    				R.drawable.app_widget_power_button_disabled);
    		// Doesn't work. Only methods with the annotation 
    		// RemotableViewMethod work
    		//views.setBoolean(R.id.single_profile_app_widget, "setEnabled",
    		//		false);
    		
    		// Prepare an activity intent even though we're disabling the 
    		// widget. This is required so that the test for 
    		// (pendingIntent != null) later won't fail
    		pendingIntent = Dindy.getPendingIntent(context);
    	} else if (widgetSettings.mProfileId == previousProfileId ||
    			   Consts.NOT_A_PROFILE_ID == previousProfileId) {
    		views = new RemoteViews(packageName,
    				R.layout.single_profile_appwidget);
    		views.setImageViewResource(R.id.single_profile_app_widget_image_button,
    				R.drawable.app_widget_button_selector_on);
    		views.setContentDescription(R.id.single_profile_app_widget_image_button,
    				context.getString(R.string.start));
        	//pendingIntent = PendingIntent.getService(context, 0, 
            //		DindyService.getStartServiceIntent(context,
            //				widgetSettings.mProfileId, null, 
            //				Consts.INTENT_SOURCE_WIDGET, Consts.NOT_A_TIME_LIMIT), 0);
    		pendingIntent = PendingIntent.getActivity(context, 0,
    				new Intent(context, ProfileStarterActivity.class)
					.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
					| Intent.FLAG_ACTIVITY_CLEAR_TOP)
					.setAction(Intent.ACTION_MAIN)
    				//.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
    				//| Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
					.putExtra(Consts.EXTRA_PROFILE_ID, widgetSettings.mProfileId)
					.putExtra(Consts.EXTRA_INTENT_SOURCE, Consts.INTENT_SOURCE_WIDGET)
					.setData(Uri.withAppendedPath(Uri.parse("dindy://profile/id/"),
						String.valueOf(widgetSettings.mProfileId))), 0);
    	} else {
    		return;
    	}
    	
    	if (pendingIntent != null) {
    		String profileName;
    		if (profileExists) {
    			profileName = ProfilePreferencesHelper.instance().getProfielNameFromId(
    					widgetSettings.mProfileId);
    		} else {
    			profileName = context.getString(
    					R.string.app_widget_profile_deleted);
        	}
    		views.setTextViewText(R.id.single_profile_app_widget_text,
    				profileName);
    		views.setOnClickPendingIntent(R.id.single_profile_app_widget,
    				pendingIntent);
    		views.setOnClickPendingIntent(
    				R.id.single_profile_app_widget_image_button,
    				pendingIntent);
    		Log.d(Consts.LOGTAG, "updating widget with profile " + profileName);
    		appWidgetManager.updateAppWidget(widgetId, views);
    	}
    }
}
