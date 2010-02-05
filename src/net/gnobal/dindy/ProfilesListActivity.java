package net.gnobal.dindy;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.LinkedList;
import java.util.Vector;

public class ProfilesListActivity extends ListActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPreferencesHelper = ProfilePreferencesHelper.instance();

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mListMode = extras.getInt(EXTRA_MODE_NAME);
		}

		mContextMenuStrings = new Vector<String>();
		if (mListMode == EXTRA_MODE_SELECT) {
			mContextMenuIndexSelect = 0;
		} else {
			mContextMenuIndexSelect = -1; // will not be included
		}
		mContextMenuIndexEdit = mContextMenuIndexSelect + 1;
		mContextMenuIndexRename = mContextMenuIndexEdit + 1;
		mContextMenuIndexDelete = mContextMenuIndexRename + 1;
		mContextMenuNumEntries = mContextMenuIndexDelete + 1;

		mContextMenuStrings.setSize(mContextMenuNumEntries);
		if (mListMode == EXTRA_MODE_SELECT) {
			mContextMenuStrings.set(mContextMenuIndexSelect, getString(
					R.string.preferences_profiles_list_context_menu_select));
		}
		mContextMenuStrings.set(mContextMenuIndexEdit, getString(
				R.string.preferences_profiles_list_context_menu_edit));
		mContextMenuStrings.set(mContextMenuIndexRename, getString(
				R.string.preferences_profiles_list_context_menu_rename));
		mContextMenuStrings.set(mContextMenuIndexDelete, getString(
				R.string.preferences_profiles_list_context_menu_delete));
		registerForContextMenu(getListView());
		mArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mListItems);
		setListAdapter(mArrayAdapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		fillList();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) menuInfo;
		if (adapterMenuInfo.position == 0) {
			// The first position is the "Add new profile..." item, so do not
			// create a context menu for it
			return;
		}
		super.onCreateContextMenu(menu, v, menuInfo);

		menu.setHeaderTitle(mArrayAdapter.getItem(adapterMenuInfo.position));
		if (mListMode == EXTRA_MODE_SELECT) {
			menu.add(0, mContextMenuIndexSelect, 0,
					mContextMenuStrings.get(mContextMenuIndexSelect));
					//.setShortcut('7', 's');
		}
		menu.add(0, mContextMenuIndexEdit, 0,
				mContextMenuStrings.get(mContextMenuIndexEdit));
		menu.add(0, mContextMenuIndexRename, 0,
				mContextMenuStrings.get(mContextMenuIndexRename));
		menu.add(0, mContextMenuIndexDelete, 0,
				mContextMenuStrings.get(mContextMenuIndexDelete));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_ID_NEW, 0,
				R.string.preferences_profiles_list_menu_new).setIcon(
					android.R.drawable.ic_menu_add);//.setShortcut('6', 'n');
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case MENU_ID_NEW:
			showDialog(ProfileNameDialogHelper.DIALOG_NEW_PROFILE);
			return true;
		}

		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		super.onCreateDialog(id);
		
		setNameDialogVariables(id);
		
		switch (id) {
		case ProfileNameDialogHelper.DIALOG_NEW_PROFILE: 
			return ProfileNameDialogHelper.buildProfileNameDialog(this,
					android.R.drawable.ic_dialog_info, mNewListener);

		case ProfileNameDialogHelper.DIALOG_RENAME_PROFILE:
			return ProfileNameDialogHelper.buildProfileNameDialog(this,
					android.R.drawable.ic_dialog_info, mRenameListener);

		}// switch

		return null;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = 
			(AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		mCurrentSelection = info.position;

		final int selectedItemId = item.getItemId();
		if (selectedItemId == mContextMenuIndexRename) {
			showDialog(ProfileNameDialogHelper.DIALOG_RENAME_PROFILE);
			return true;
		} else if (selectedItemId == mContextMenuIndexSelect) {
			finishWithSelectedProfile(mArrayAdapter.getItem(mCurrentSelection));
			return true;
		} else if (selectedItemId == mContextMenuIndexDelete) {
			String profileToRemove = mArrayAdapter.getItem(mCurrentSelection);
			mPreferencesHelper.deleteProfile(this, profileToRemove);
			mArrayAdapter.remove(profileToRemove);
			return true;
		} else if (selectedItemId == mContextMenuIndexEdit) {
			String profileToEdit = mArrayAdapter.getItem(mCurrentSelection);
			startProfileEditor(profileToEdit,
					mPreferencesHelper.getProfileIdFromName(profileToEdit));
			return true;
		}

		return super.onContextItemSelected(item);
	}

	static final String EXTRA_MODE_NAME = "mode";
	static final int EXTRA_MODE_SELECT = 0;
	static final int EXTRA_MODE_EDIT = 1;
	static final String EXTRA_SELECTED_PROFILE_ID = 
		"selected_profile_id";

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		setNameDialogVariables(id);
		ProfileNameDialogHelper.prepareDialog(id, dialog);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (position == 0) {
			// This is the special case of the "Add new profile"
			showDialog(ProfileNameDialogHelper.DIALOG_NEW_PROFILE);
			return;
		}
		if (mListMode == EXTRA_MODE_EDIT) {
			String profileName = l.getItemAtPosition(position).toString(); 
			startProfileEditor(profileName,
					mPreferencesHelper.getProfileIdFromName(profileName));
		} else if (mListMode == EXTRA_MODE_SELECT) {
			finishWithSelectedProfile(l.getItemAtPosition(position).toString());
		}
	}
	
	private void setNameDialogVariables(int dialogId) {
		String title = null;
		String oldProfileName = null;
		switch (dialogId) {
		case ProfileNameDialogHelper.DIALOG_NEW_PROFILE:
			title = getString(R.string.preferences_profiles_list_menu_new);
			oldProfileName = Consts.EMPTY_STRING;
			break;
		
		case ProfileNameDialogHelper.DIALOG_RENAME_PROFILE:
			if (mCurrentSelection != -1) {
				oldProfileName = mArrayAdapter.getItem(mCurrentSelection);
				title = getString(
						R.string.dialog_profile_name_rename_title_prefix) +
						" " + oldProfileName;
			}
			break;
		}

		if (title != null && oldProfileName != null) { 
			ProfileNameDialogHelper.setDialogVariables(title, oldProfileName);
		}
	}
	
	private void fillList() {
		// Be careful not to change the list returned from 
		// mPreferencesHelper.getAllProfileNamesSorted() because it may
		// be the cached list
		mListItems.clear();
		mListItems.addAll(mPreferencesHelper.getAllProfileNamesSorted());
		mListItems.addFirst(getString(R.string.preferences_profiles_list_item_new));
		mArrayAdapter.notifyDataSetChanged();
	}

	private void finishWithSelectedProfile(String profileName) {
		setResult(RESULT_OK, new Intent().putExtra(
				EXTRA_SELECTED_PROFILE_ID,
				mPreferencesHelper.getProfileIdFromName(profileName)));
		finish();
	}

	private void startProfileEditor(String profileName, long profileId) {
		Intent profilePreferencesIntent = new Intent(this,
				ProfilePreferencesActivity.class);
		profilePreferencesIntent.putExtra(
				ProfilePreferencesActivity.EXTRA_PROFILE_NAME, profileName);
		profilePreferencesIntent.putExtra(
				ProfilePreferencesActivity.EXTRA_PROFILE_ID, profileId);
		startActivity(profilePreferencesIntent);
	}

	private class RenameDialogListener implements 
		ProfileNameDialogHelper.Listener {
		RenameDialogListener(ProfilesListActivity parent) {
			mParent = parent;
		}
		
		public int getDialogType() {
			return ProfileNameDialogHelper.DIALOG_RENAME_PROFILE;
		}
	
		public void onSuccess(String newProfileName, long newProfileId) {
			mParent.fillList();
		}
		
		public Activity getOwnerActivity() {
			return mParent;
		}
		
		private ProfilesListActivity mParent;
	}

	private class NewDialogListener implements 
		ProfileNameDialogHelper.Listener {
		NewDialogListener(ProfilesListActivity parent) {
			mParent = parent;
		}

		public void onSuccess(String newProfileName,
				long newProfileId) {
			mParent.startProfileEditor(newProfileName, newProfileId);
		}

		public int getDialogType() {
			return ProfileNameDialogHelper.DIALOG_NEW_PROFILE;
		}

		public Activity getOwnerActivity() {
			return mParent;
		}
		
		private ProfilesListActivity mParent;
	}
	
	private int mCurrentSelection = -1;
	private LinkedList<String> mListItems = new LinkedList<String>();
	private ArrayAdapter<String> mArrayAdapter = null;
	private Vector<String> mContextMenuStrings = null;
	private ProfilePreferencesHelper mPreferencesHelper = null;
	// Context menu stuff. Indices are not final because the context
	// menu is dynamic - we only show the "select" option if we're in
	// selection mode (as opposed to "edit" mode)
	private int mContextMenuIndexSelect = -1;
	private int mContextMenuIndexEdit = -1;
	private int mContextMenuIndexRename = -1;
	private int mContextMenuIndexDelete = -1;
	private int mContextMenuNumEntries = -1;
	private int mListMode = EXTRA_MODE_EDIT;
	private static final int MENU_ID_NEW = 0;
	private RenameDialogListener mRenameListener = 
		new RenameDialogListener(this);
	private NewDialogListener mNewListener =
		new NewDialogListener(this);
}
