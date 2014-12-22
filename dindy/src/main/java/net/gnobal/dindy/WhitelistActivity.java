package net.gnobal.dindy;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

public class WhitelistActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.whitelist_activity);
		final SharedPreferences preferences = getSharedPreferences(
				Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
		if (preferences.getBoolean(Consts.Prefs.Main.KEY_SHOW_WHITELIST_USAGE, true)) {
			WhitelistUsageDialogFragment.newInstance().show(getFragmentManager(), "whitelist_usage");
		}
	}

	public static class WhitelistUsageDialogFragment extends MessageWithCheckboxDialogFragmentBase {
		public WhitelistUsageDialogFragment() {
			super(
				R.string.whitelist_usage_text,
				R.string.whitelist_usage_title,
				Consts.Prefs.Main.KEY_SHOW_WHITELIST_USAGE);
		}
		
		public static WhitelistUsageDialogFragment newInstance() {
			return new WhitelistUsageDialogFragment();
		}
	}
}