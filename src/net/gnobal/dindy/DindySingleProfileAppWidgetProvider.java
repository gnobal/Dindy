package net.gnobal.dindy;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Config;
import android.util.Log;
import android.widget.RemoteViews;

public class DindySingleProfileAppWidgetProvider extends AppWidgetProvider {
    @Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		if (Config.LOGD && Consts.DEBUG) {
			String ids = "";
			for (int i = 0; i < appWidgetIds.length; ++i) {
				ids += appWidgetIds[i] + ", ";
			}
			Log.d(Consts.LOGTAG, "onUpdate: " + ids);
		}

		updateAllSingleProfileWidgets(context, appWidgetManager, appWidgetIds,
				Consts.NOT_A_PROFILE_ID, Consts.NOT_A_PROFILE_ID);
    }
    
    @Override
	public void onDeleted(Context context, int[] appWidgetIds) {
    	SharedPreferences widgetPrefs = context.getSharedPreferences(
    			Consts.Prefs.Widget.NAME, Context.MODE_PRIVATE);
    	ProfilePreferencesHelper prefsHelper = 
    		ProfilePreferencesHelper.instance();
    	final int N = appWidgetIds.length;
    	for (int i = 0; i < N; ++i) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"deleting widget ID " + appWidgetIds[i]);
   			prefsHelper.deleteWidgetSettings(widgetPrefs, appWidgetIds[i]);
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
			AppWidgetManager appWidgetManager, int[] appWidgetIds,
			long activeProfileId, long previousProfileId) {
    	SharedPreferences widgetPrefs = context.getSharedPreferences(
    			Consts.Prefs.Widget.NAME, Context.MODE_PRIVATE);
    	ProfilePreferencesHelper prefsHelper = 
    		ProfilePreferencesHelper.instance();
    	String packageName = context.getPackageName();
    	final int N = appWidgetIds.length;
    	for (int i = N-1; i >=0; --i) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"updating widget ID " + appWidgetIds[i]);
    		DindySettings.WidgetSettings widgetSettings = 
    			prefsHelper.getWidgetSettings(widgetPrefs, appWidgetIds[i]);
    		if (widgetSettings == null) {
    			continue;
    		}
    		if (widgetSettings.mWidgetType == Consts.Prefs.Widget.Type.SINGLE_PROFILE) {
    			updateOneSingleProfileWidget(context, appWidgetManager, packageName,
    					widgetSettings, appWidgetIds[i],
    					activeProfileId, previousProfileId);
    		}
    	}
	}

	static void updateOneSingleProfileWidget(Context context,
    		AppWidgetManager appWidgetManager, String packageName, 
    		DindySettings.WidgetSettings widgetSettings, int widgetId,
    		long activeProfileId, long previousProfileId) {
    	RemoteViews views = null;
    	Intent serviceIntent = null;
    	PendingIntent pendingIntent = null;
    	// NOTE: the order of this if-else clause matters! When called from the
    	// DindySingleProfileAppWidgetConfigure class
    	// widgetSettings.mProfileId == previousProfileId is always (!) true, 
    	// but if widgetSettings.mProfileId == activeProfileId is true then we 
    	// want the if branch to happen (not the else branch)
    	if (widgetSettings.mProfileId == activeProfileId) {
    		views = new RemoteViews(packageName,
    				R.layout.single_profile_appwidget);
    		views.setImageViewResource(R.id.single_profile_app_widget_image_button,
    				R.drawable.app_widget_button_selector_off);
    		serviceIntent = new Intent(DindyService.ACTION_STOP_DINDY_SERVICE);
    		pendingIntent = PendingIntent.getBroadcast(context, 0,
    				serviceIntent, 0);
    	} else if (widgetSettings.mProfileId == previousProfileId ||
    			   Consts.NOT_A_PROFILE_ID == previousProfileId) {
    		views = new RemoteViews(packageName,
    				R.layout.single_profile_appwidget);
    		views.setImageViewResource(R.id.single_profile_app_widget_image_button,
    				R.drawable.app_widget_button_selector_on);
            serviceIntent = new Intent(context, DindyService.class);
    		serviceIntent.putExtra(DindyService.EXTRA_PROFILE_ID,
    				widgetSettings.mProfileId);
    		// See:
    		// http://www.developer.com/ws/article.php/3837531/Handling-User-Interaction-with-Android-App-Widgets.htm
    		serviceIntent.setData(
    				Uri.withAppendedPath(Uri.parse("dindy://profile/id/"),
    				String.valueOf(widgetSettings.mProfileId)));
        	pendingIntent = PendingIntent.getService(context, 0, 
            		serviceIntent, 0);
    	} else {
    		return;
    	}
    	
    	if (pendingIntent != null) {
        	String profileName = ProfilePreferencesHelper.instance().getProfielNameFromId(
        			widgetSettings.mProfileId);
        	views.setTextViewText(R.id.single_profile_app_widget_text,
        			profileName);
    		views.setOnClickPendingIntent(
    				R.id.single_profile_app_widget_image_button,
    				pendingIntent);

    		if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
    				"updating widget with profile " + profileName);
    		appWidgetManager.updateAppWidget(widgetId, views);
    	}
    }
}
