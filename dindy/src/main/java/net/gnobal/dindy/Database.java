package net.gnobal.dindy;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

class Database extends SQLiteOpenHelper {
	Database(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + PROFILES_TABLE_NAME + " (" +
			Profiles._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
			Profiles.NAME + " TEXT" + ");");
		
		createIncomingCallsTable(db);
		createWhitelistTable(db);
		addWhitelistLookupKeyColumn(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			// Upgrade from version 1 to 2
			createIncomingCallsTable(db);
		}
		if (oldVersion < 3) {
			// Upgrade from version 2 to 3
			createWhitelistTable(db);
		}
		
		if (oldVersion < 4) {
			// Upgrade from version 3 to 4
			addWhitelistLookupKeyColumn(db);
		}
	}

	static final class Profiles implements BaseColumns {
		public static final String NAME = "name";
	}

	static final class DbIncomingCallInfo implements BaseColumns {
		public static final String CALLER_ID_NUMBER = "caller_id_number";
		public static final String NUMBER = "number";
		public static final String ABSOLUTE_WAKEUP_TIME_MILLIS = "abs_wakeup_time_millis";
	}

	static final class DbWhitelist implements BaseColumns {
		public static final String CONTACT_ID = "contact_id";
		public static final String CONTACT_LOOKUP_KEY = "contact_lookup_key";
	}

	static final String PROFILES_TABLE_NAME = "profiles";
	static final String INCOMING_CALLS_TABLE_NAME = "incoming_calls";
	static final String WHITELIST_TABLE_NAME = "whitelist";

	private void createIncomingCallsTable(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + INCOMING_CALLS_TABLE_NAME + " (" +
			DbIncomingCallInfo._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
			DbIncomingCallInfo.CALLER_ID_NUMBER + " TEXT," +
			DbIncomingCallInfo.NUMBER + " TEXT," + 
			DbIncomingCallInfo.ABSOLUTE_WAKEUP_TIME_MILLIS + " BIGINT" +
			");");
	}

	private void createWhitelistTable(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + WHITELIST_TABLE_NAME + " (" +
			DbWhitelist._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
			DbWhitelist.CONTACT_ID + " INTEGER UNIQUE" +
			");");
	}

	private void addWhitelistLookupKeyColumn(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + WHITELIST_TABLE_NAME +
			" ADD COLUMN " + DbWhitelist.CONTACT_LOOKUP_KEY + " TEXT");
	}

	private static final String DATABASE_NAME = "dindy.db";
	private static final int DATABASE_VERSION = 4;
}