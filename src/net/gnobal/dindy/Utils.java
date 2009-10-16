package net.gnobal.dindy;

import android.app.ActivityManager;
import android.content.Context;
import java.util.Iterator;
import java.util.List;

class Utils {
	static boolean isDindyServiceRunning(Context context) {
		ActivityManager am = (ActivityManager) 
			context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceList = am
				.getRunningServices(Integer.MAX_VALUE);
		Iterator<ActivityManager.RunningServiceInfo> it = serviceList
				.iterator();
		while (it.hasNext()) {
			ActivityManager.RunningServiceInfo info = it.next();
			if (info.service.getClassName()
					.equals(DindyService.class.getName())) {
				return true;
			}
		}

		return false;
	}
}
