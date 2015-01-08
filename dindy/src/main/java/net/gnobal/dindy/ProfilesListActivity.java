package net.gnobal.dindy;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.LinkedList;

public class ProfilesListActivity extends ListActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPreferencesHelper = ProfilePreferencesHelper.instance();
		mArrayAdapter = new ArrayAdapter<>(this,
				android.R.layout.simple_list_item_1, mListItems);
		setListAdapter(mArrayAdapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		fillList();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		finishWithSelectedProfile(l.getItemAtPosition(position).toString());
	}
	
	private void fillList() {
		// Be careful not to change the list returned from 
		// mPreferencesHelper.getAllProfileNamesSorted() because it may
		// be the cached list
		mListItems.clear();
		mListItems.addAll(mPreferencesHelper.getAllProfileNamesSorted());
		mArrayAdapter.notifyDataSetChanged();
	}

	private void finishWithSelectedProfile(String profileName) {
		setResult(RESULT_OK, new Intent().putExtra(
				Consts.EXTRA_PROFILE_ID,
				mPreferencesHelper.getProfileIdFromName(profileName)));
		finish();
	}

	private final LinkedList<String> mListItems = new LinkedList<>();
	private ArrayAdapter<String> mArrayAdapter = null;
	private ProfilePreferencesHelper mPreferencesHelper = null;
}