package net.gnobal.dindy.locale;

import net.gnobal.dindy.Consts;
import net.gnobal.dindy.DindyService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class FireReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (!com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
			return;
		}

//		final String action1 = intent.getStringExtra(Consts.EXTRA_EXTERNAL_ACTION);
//		final long profileId1 = intent.getLongExtra(
//				Consts.EXTRA_PROFILE_ID,
//				Consts.NOT_A_PROFILE_ID);
//		final String profileName1 = intent.getStringExtra(
//				Consts.EXTRA_PROFILE_NAME);
//		
//		if (Config.LOGD ) Log.d("Dindy",
//				"acion=" + action1 + ", profileId=" + profileId1 + "profileName=" + profileName1);

		final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
		
		final String action = bundle.getString(Consts.EXTRA_EXTERNAL_ACTION);
		Intent serviceIntent = null;
		if (Consts.EXTRA_EXTERNAL_ACTION_START_SERVICE.equals(action)) {
			final long profileId = bundle.getLong(Consts.EXTRA_PROFILE_ID, Consts.NOT_A_PROFILE_ID);
			if (profileId == Consts.NOT_A_PROFILE_ID) {
				return;
			}
			final String profileName = bundle.getString(Consts.EXTRA_PROFILE_NAME);
			serviceIntent = DindyService.getStartServiceIntent(
				context, profileId, profileName, Consts.INTENT_SOURCE_LOCALE,
				Consts.NOT_A_TIME_LIMIT);
			context.startService(serviceIntent);
		} else if (Consts.EXTRA_EXTERNAL_ACTION_STOP_SERVICE.equals(action)) {
			context.stopService(DindyService.getStopServiceIntent(context));
		} 

//		final String action = intent.getStringExtra(Consts.EXTRA_EXTERNAL_ACTION);
//		Intent serviceIntent = null;
//		if (Consts.EXTRA_EXTERNAL_ACTION_START_SERVICE.equals(action)) {
//			final long profileId = intent.getLongExtra(
//					Consts.EXTRA_PROFILE_ID,
//					Consts.NOT_A_PROFILE_ID);
//			if (profileId == Consts.NOT_A_PROFILE_ID) {
//				return;
//			}
//			final String profileName = intent.getStringExtra(
//					Consts.EXTRA_PROFILE_NAME);
//			serviceIntent = DindyService.getStartServiceIntent(
//				context, profileId, profileName, Consts.INTENT_SOURCE_LOCALE,
//				Consts.NOT_A_TIME_LIMIT);
//			context.startService(serviceIntent);
//		} else if (Consts.EXTRA_EXTERNAL_ACTION_STOP_SERVICE.equals(action)) {
//			context.stopService(DindyService.getStopServiceIntent(context));
//		} 
		
		return;
	}
}
