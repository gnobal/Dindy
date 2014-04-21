package net.gnobal.dindy;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

class WhitelistHelper {
	WhitelistHelper(Context context) {
		mDb = new Database(context);
	}

	List<Long> getAllContactIds() {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		final ArrayList<Long> contactIds = new ArrayList<Long>();

		try {
			db = mDb.getWritableDatabase();
			cursor = db.query(Database.WHITELIST_TABLE_NAME, CONTACT_ID_COLUMNS,
					null, null, null, null, null);
			while (cursor.moveToNext()) {
				contactIds.add(cursor.getLong(CONTACT_ID_INDEX));
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

		return contactIds;
	}

	boolean addContact(long contactId) {
		SQLiteDatabase db = null;
		long rowId = Consts.NOT_A_PROFILE_ID;
		boolean success = true;

		try {
			db = mDb.getWritableDatabase();
			ContentValues contentValues = new ContentValues();
			contentValues.put(Database.DbWhitelist.CONTACT_ID, contactId);
			rowId = db.insert(Database.WHITELIST_TABLE_NAME, null, contentValues);
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

	void removeContact(long contactId) {
		SQLiteDatabase db = null;

		try {
			db = mDb.getWritableDatabase();
			db.delete(Database.WHITELIST_TABLE_NAME, Database.DbWhitelist.CONTACT_ID
					+ " = " + contactId, null);
		} catch (Throwable t) {
		} finally {
			if (db != null) {
				db.close();
				db = null;
			}
		}
	}

	boolean isInWhitelist(long contactId) {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		
		try {
			db = mDb.getWritableDatabase();
			cursor = db.query(Database.WHITELIST_TABLE_NAME, CONTACT_ID_COLUMNS,
					Database.DbWhitelist.CONTACT_ID + " = " + contactId, null, null, null,
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

	private static final String[] CONTACT_ID_COLUMNS = { Database.DbWhitelist.CONTACT_ID };
	private static final int CONTACT_ID_INDEX = 0;

	private Database mDb;
}