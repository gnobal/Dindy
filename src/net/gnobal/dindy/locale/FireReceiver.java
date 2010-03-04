package net.gnobal.dindy.locale;

import net.gnobal.dindy.Consts;
import net.gnobal.dindy.DindyService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class FireReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (!com.twofortyfouram.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
			return;
		}

		final String action = intent.getStringExtra(Consts.EXTRA_EXTERNAL_ACTION);
		Intent serviceIntent = null;
		if (Consts.EXTRA_EXTERNAL_ACTION_START_SERVICE.equals(action)) {
			final long profileId = intent.getLongExtra(
					Consts.EXTRA_PROFILE_ID,
					Consts.NOT_A_PROFILE_ID);
			if (profileId == Consts.NOT_A_PROFILE_ID) {
				return;
			}
			serviceIntent = DindyService.getStartServiceIntent(
				context, profileId);
			context.startService(serviceIntent);
		} else if (Consts.EXTRA_EXTERNAL_ACTION_STOP_SERVICE.equals(action)) {
			context.stopService(DindyService.getStopServiceIntent(context));
		} 
		
		return;
	}
}
