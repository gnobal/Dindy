package net.gnobal.dindy;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract.Contacts;
import android.content.CursorLoader;
import android.util.Log;

class WhitelistHelper {
	WhitelistHelper(Context context) {
		mDb = new Database(context);
		// To prepare for consistent backup across devices we needed to switch from storing
		// contact IDs to storing lookup keys, which are resilient to sync and aggregation
		// See: https://developer.android.com/reference/android/provider/ContactsContract.ContactsColumns.html#LOOKUP_KEY
		// Row IDs in general aren't safe to use and may change
		updateIdsToLookupKeys(context);
	}

	void updateIdsToLookupKeys(Context context) {
		SQLiteDatabase db = null;
		Cursor cursorRowsWithoutKey = null;

		try {
			db = mDb.getWritableDatabase();
			cursorRowsWithoutKey = db.query(Database.WHITELIST_TABLE_NAME,
					new String[] { Database.DbWhitelist.CONTACT_ID },
					Database.DbWhitelist.CONTACT_LOOKUP_KEY + " IS NULL",
					null, null, null, null);
			while (cursorRowsWithoutKey.moveToNext()) {
				Cursor cursorLookupKey = null;
				try {
					final long contactId = cursorRowsWithoutKey.getLong(0);
					cursorLookupKey = new CursorLoader(
						context, Contacts.CONTENT_URI, new String[] { Contacts.LOOKUP_KEY },
						Contacts._ID + " = " + contactId, null, null).loadInBackground();
					if (cursorLookupKey.moveToNext()) {
						
						final String contactLookupKey = cursorLookupKey.getString(0);
						// Update the table with contactLookupKey where contact ID equals contactId
						ContentValues contentValues = new ContentValues();
						contentValues.put(Database.DbWhitelist.CONTACT_LOOKUP_KEY, contactLookupKey);
						int numRows = db.update(Database.WHITELIST_TABLE_NAME, contentValues,
								Database.DbWhitelist.CONTACT_ID + " = "+ contactId, null);
						if (Consts.DEBUG) {
							if (numRows == 1) {
								Log.d(Consts.LOGTAG, "Successfully updated contact ID " + 
										contactId + " with lookup key " + contactLookupKey);
							} else {
								Log.d(Consts.LOGTAG, "Failed to update contact ID " + 
										contactId + " with lookup key " + contactLookupKey);
							}
						}
					}
				} catch (Throwable t) {
				} finally {
					if (cursorLookupKey != null) {
						cursorLookupKey.close();
						cursorLookupKey = null;
					}
				}
			}
		} catch (Throwable t) {
		} finally {
			if (cursorRowsWithoutKey != null) {
				cursorRowsWithoutKey.close();
				cursorRowsWithoutKey = null;
			}
			if (db != null) {
				db.close();
				db = null;
			}
		}
	}

	List<String> getAllContactLookupKeys() {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		final ArrayList<String> contactLookupKeys = new ArrayList<String>();

		try {
			db = mDb.getWritableDatabase();
			cursor = db.query(Database.WHITELIST_TABLE_NAME, CONTACT_LOOKUP_KEY_COLUMNS,
					null, null, null, null, null);
			while (cursor.moveToNext()) {
				contactLookupKeys.add(cursor.getString(CONTACT_LOOKUP_KEY_INDEX));
			}
		} catch (Throwable t) {
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
			if (db != null) {
				db.close();
				db = null;
			}
		}

		return contactLookupKeys;
	}

	boolean addContact(String contectLookupKey) {
		SQLiteDatabase db = null;
		boolean success = true;

		try {
			db = mDb.getWritableDatabase();
			ContentValues contentValues = new ContentValues();
			contentValues.put(Database.DbWhitelist.CONTACT_LOOKUP_KEY, contectLookupKey);
			final long rowId = db.insert(Database.WHITELIST_TABLE_NAME, null, contentValues);
			if (rowId == -1) {
				success = false;
			}
		} catch (Throwable t) {
			success = false;
		} finally {
			if (db != null) {
				db.close();
				db = null;
			}			
		}

		return success;
	}

	void removeContact(String contactLookupKey) {
		SQLiteDatabase db = null;

		try {
			db = mDb.getWritableDatabase();
			db.delete(Database.WHITELIST_TABLE_NAME, Database.DbWhitelist.CONTACT_LOOKUP_KEY
					+ " = '" + contactLookupKey + "'", null);
		} catch (Throwable t) {
		} finally {
			if (db != null) {
				db.close();
				db = null;
			}
		}
	}

	boolean isInWhitelist(String contactLookupKey) {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		
		try {
			db = mDb.getWritableDatabase();
			cursor = db.query(Database.WHITELIST_TABLE_NAME, CONTACT_LOOKUP_KEY_COLUMNS,
					Database.DbWhitelist.CONTACT_LOOKUP_KEY + " = '" + contactLookupKey + "'", null, null, null,
					null, "1");
			return cursor.getCount() > 0;
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
			if (db != null) {
				db.close();
				db = null;
			}
		}
	}

	private static final String[] CONTACT_LOOKUP_KEY_COLUMNS = { Database.DbWhitelist.CONTACT_LOOKUP_KEY };
	private static final int CONTACT_LOOKUP_KEY_INDEX = 0;

	private Database mDb;
}