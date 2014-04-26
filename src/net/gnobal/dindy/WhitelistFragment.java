package net.gnobal.dindy;

import java.util.List;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class WhitelistFragment extends ListFragment implements
	LoaderManager.LoaderCallbacks<Cursor> {

	public WhitelistFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
		Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		return inflater.inflate(R.layout.whitelist_fragment, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mWhitelistView = getListView();
		mDbHelper = new WhitelistHelper(getActivity());
		mAdapter = new WhitelistCursorAdapter(
				getActivity(), R.layout.whitelist_item, null, FROM_COLUMNS, TO_IDS, 0);
		mWhitelistView.setAdapter(mAdapter);
		getLoaderManager().initLoader(WHITELIST_LOADER_ID, null, this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.whitelist_actions, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_done_editing_whitelist:
			getActivity().finish();
			return true;
		case R.id.action_whitelist_add:
			startActivityForResult(new Intent(
				Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), PICK_CONTACT_REQUEST_CODE);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != Activity.RESULT_OK) {
			return;
		}

		if (requestCode != PICK_CONTACT_REQUEST_CODE) {
			return;
		}

		final Uri contactData = data.getData();
		final Cursor c =  new CursorLoader(
				getActivity(), contactData, null, null, null, null).loadInBackground();
		if (!c.moveToFirst()) {
			return;
		}
		final String contactLookupKey = c.getString(
				c.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY));
		c.close();
		if (contactLookupKey == null || contactLookupKey.equals(Consts.EMPTY_STRING)) {
			Toast.makeText(getActivity(), R.string.whitelist_contact_cannot_be_added, Toast.LENGTH_LONG).show();
			return;
		}
		if (mDbHelper.isInWhitelist(contactLookupKey)) {
			Toast.makeText(getActivity(), R.string.whitelist_contact_exists, Toast.LENGTH_SHORT).show();
			return;
		}
		mDbHelper.addContact(contactLookupKey);
		getLoaderManager().restartLoader(WHITELIST_LOADER_ID, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
		// TODO: Use a dynamic selection and selection args (argument after SELECTION)
		// to get all the IDs we want
		final List<String> allContactLookupKeys = mDbHelper.getAllContactLookupKeys();
		final StringBuilder contactsString = new StringBuilder();
		for (String contactLookupKey : allContactLookupKeys) {
			contactsString.append('\'').append(contactLookupKey).append('\'').append(',');
		}
		if (contactsString.length() > 0) {
			contactsString.deleteCharAt(contactsString.length() - 1);
		}
		final String selection = ContactsContract.Data.LOOKUP_KEY + " IN (" + contactsString.toString() + ")";
		return new CursorLoader(
				getActivity(), Contacts.CONTENT_URI, PROJECTION, selection,	null, ORDER);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.swapCursor(cursor);
		if (mAdapter.getCount() <= 0) {
			((TextView) getActivity().findViewById(android.R.id.empty)).setText(R.string.whitelist_empty);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	private class WhitelistCursorAdapter extends SimpleCursorAdapter {
		public WhitelistCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final View v = super.newView(context, cursor, parent);
			v.setTag(cursor.getString(CONTACT_LOOKUP_KEY_COLUMN_INDEX));
			return v;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			super.bindView(view, context, cursor);
			
			final View removeView =
					view.findViewById(R.id.whitelist_remove_contact_button);
			removeView.setOnClickListener(mRemoveClickListener);
			removeView.setTag(cursor.getString(CONTACT_LOOKUP_KEY_COLUMN_INDEX));
		}
	}

	private View.OnClickListener mRemoveClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			final String contactLookupKey = (String) v.getTag();
			mDbHelper.removeContact(contactLookupKey);
			getLoaderManager().restartLoader(WHITELIST_LOADER_ID, null, WhitelistFragment.this);
		}
	};
	
	private static final String[] FROM_COLUMNS = {
		Contacts.DISPLAY_NAME_PRIMARY
	};
	private static final String ORDER =
		"LOWER(" + ContactsContract.Data.DISPLAY_NAME_PRIMARY + ") ASC";
	private static final int[] TO_IDS = {
		android.R.id.text1
	};
	private static final String[] PROJECTION =
	{
			ContactsContract.Data._ID,
			ContactsContract.Data.LOOKUP_KEY,
			ContactsContract.Data.DISPLAY_NAME_PRIMARY,
	};
	private static final int CONTACT_LOOKUP_KEY_COLUMN_INDEX = 1;
	
	private static final int WHITELIST_LOADER_ID = 0;
	private static final int PICK_CONTACT_REQUEST_CODE = 0;

	private ListView mWhitelistView;
	private WhitelistCursorAdapter mAdapter;
	private WhitelistHelper mDbHelper;
}