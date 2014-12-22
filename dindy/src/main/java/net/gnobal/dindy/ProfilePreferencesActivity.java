package net.gnobal.dindy;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class ProfilePreferencesActivity extends PreferenceActivity {
	static final String EXTRA_PROFILE_NAME = "profile_name";
	static final String EXTRA_PROFILE_ID = "profile_id";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final Bundle bundle = getIntent().getExtras();
		setTitle(bundle.getString(EXTRA_PROFILE_NAME));
		getFragmentManager().beginTransaction()
			.replace(android.R.id.content,
				ProfilePreferencesFragment.newInstance(bundle.getLong(EXTRA_PROFILE_ID)))
			.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.profile_editor_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_done_editing_profile:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}