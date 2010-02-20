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

		final long profileId = intent.getLongExtra(
				Consts.EXTRA_SELECTED_PROFILE_ID, Consts.NOT_A_PROFILE_ID);

		if (profileId == Consts.NOT_A_PROFILE_ID) {
			return;
		}

		Intent serviceIntent = new Intent(context, DindyService.class);
		serviceIntent.putExtra(DindyService.EXTRA_PROFILE_ID,
				profileId);
		context.startService(serviceIntent);
	}
}
