package net.gnobal.dindy;

import java.util.HashMap;
import java.util.Iterator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

class IncomingCalls {
	IncomingCalls(Context context) {
		mDatabaseHelper = new Database(context);
	}

	boolean rebuild() {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		boolean success = true;

		try {
			db = mDatabaseHelper.getWritableDatabase();
			cursor = db.query(Database.INCOMING_CALLS_TABLE_NAME, INCOMING_CALLS_COLUMNS,
				null, null, null, null, Database.DbIncomingCallInfo.CALLER_ID_NUMBER + " ASC");
			mCallsMap.clear();
			while (cursor.moveToNext()) {
				final IncomingCallInfo callInfo = new IncomingCallInfo(
					cursor.getString(NUMBER_INDEX),
					cursor.getString(CALLER_ID_NUMBER_INDEX),
					cursor.getLong(ABSOLUTE_WAKEUP_TIME_MILLIS_INDEX));
				mCallsMap.put(cursor.getString(CALLER_ID_NUMBER_INDEX), callInfo);
			}
		} catch (Throwable t) {
			success = false;
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

		return success;
	}

	IncomingCallInfo get(String callerIdNumber) {
		return mCallsMap.get(callerIdNumber);
	}

	void put(String callerIdNumber, IncomingCallInfo callInfo) {
		SQLiteDatabase db = null;
		boolean success = true;
		long rowId = -1;
		try {
			db = mDatabaseHelper.getWritableDatabase();
			ContentValues contentValues = new ContentValues();
			contentValues.put(Database.DbIncomingCallInfo.CALLER_ID_NUMBER, callerIdNumber);
			contentValues.put(Database.DbIncomingCallInfo.NUMBER, callInfo.getNumber());
			contentValues.put(Database.DbIncomingCallInfo.ABSOLUTE_WAKEUP_TIME_MILLIS,
				callInfo.getAbsoluteWakeupTimeMillis());
			rowId = db.insert(Database.INCOMING_CALLS_TABLE_NAME, null, contentValues);
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

		if (!success) {
			return;
		}

		mCallsMap.put(callerIdNumber, callInfo);		
	}

	IncomingCallInfo remove(String callerIdNumber) {
		SQLiteDatabase db = null;
		boolean success = true;		
		try {
			db = mDatabaseHelper.getWritableDatabase();
			/* int numDeleted = */db.delete(Database.INCOMING_CALLS_TABLE_NAME,
				Database.DbIncomingCallInfo.CALLER_ID_NUMBER + " = '" + callerIdNumber + "'", null);
			// TODO check that numDelete == 1
		} catch (Throwable t) {
			success = false;
		} finally {
			if (db != null) {
				db.close();
				db = null;
			}
		}

		if (!success) {
			return null;
		}

		return mCallsMap.remove(callerIdNumber);
	}

	void clear() {
		SQLiteDatabase db = null;
		boolean success = true;		
		try {
			db = mDatabaseHelper.getWritableDatabase();
			db.delete(Database.INCOMING_CALLS_TABLE_NAME, null, null);
		} catch (Throwable t) {
			success = false;
		} finally {
			if (db != null) {
				db.close();
				db = null;
			}
		}

		if (!success) {
			return;
		}

		mCallsMap.clear();
	}

	boolean numberExists(String callerIdNumber) {
		return mCallsMap.containsKey(callerIdNumber);
	}

	Iterator<String> callerIdNumbersIterator() {
		return mCallsMap.keySet().iterator();
	}

	private HashMap<String, IncomingCallInfo> mCallsMap = new HashMap<String, IncomingCallInfo>();
	private Database mDatabaseHelper;

	private static final String[] INCOMING_CALLS_COLUMNS =
		{
			Database.DbIncomingCallInfo.CALLER_ID_NUMBER,
			Database.DbIncomingCallInfo.NUMBER,
			Database.DbIncomingCallInfo.ABSOLUTE_WAKEUP_TIME_MILLIS
		};
	private static final int CALLER_ID_NUMBER_INDEX = 0;
	private static final int NUMBER_INDEX = 1;
	private static final int ABSOLUTE_WAKEUP_TIME_MILLIS_INDEX = 2;
}