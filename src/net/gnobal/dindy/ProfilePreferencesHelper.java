package net.gnobal.dindy;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import java.util.LinkedList;

class ProfilePreferencesHelper {
	static ProfilePreferencesHelper instance(Context context) {
		if (mInstance == null) {
			return new ProfilePreferencesHelper(context);
		}
		
		return mInstance;
	}
	
	boolean anyProfilesExist() {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		boolean exist = false;

		try {
			db = mDatabaseHelper.getReadableDatabase();
			cursor = db.rawQuery(ANY_PROFILES_EXIST_QUERY, null);
			if (cursor.getCount() > 0 && cursor.moveToNext()) {
				exist = cursor.getLong(0) > 0;
			}
		} finally {
			if (cursor != null) {
				cursor.deactivate();
				cursor.close();
				cursor = null;
			}
			if (db != null) {
				db.close();
				db = null;
			}
		}
		
		return exist;
	}
	
	LinkedList<String> getAllProfileNamesSorted() {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		LinkedList<String> names = null;

		try {
			db = mDatabaseHelper.getReadableDatabase();
			cursor = db.query(PROFILES_TABLE_NAME, PROFILE_NAME_COLUMNS,
					null, null, null, null, Profiles.NAME + " ASC");
			names = new LinkedList<String>();
			while (cursor.moveToNext()) {
				names.add(cursor.getString(PROFILE_NAME_INDEX));
			}
		} finally {
			if (cursor != null) {
				cursor.deactivate();
				cursor.close();
				cursor = null;
			}
			if (db != null) {
				db.close();
				db = null;
			}
		}
		
		return names;
	}

	String getProfielNameFromId(long profileId) {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		String name = null;
		
		try {
			db = mDatabaseHelper.getReadableDatabase();
			cursor = db.query(PROFILES_TABLE_NAME, PROFILE_NAME_COLUMNS,
					Profiles._ID + " = '" + profileId + "'", null, null, null,
					null, "1");
			if (cursor.getCount() > 0 && cursor.moveToNext()) {
				name = cursor.getString(PROFILE_NAME_INDEX);
			}
		} finally {
			if (cursor != null) {
				cursor.deactivate();
				cursor.close();
				cursor = null;
			}
			if (db != null) {
				db.close();
				db = null;
			}
		}

		return name;
	}
	
	long getProfileIdFromName(String name) {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		long profileId = Consts.NOT_A_PROFILE_ID;
		
		try {
			db = mDatabaseHelper.getReadableDatabase();
			cursor = db.query(PROFILES_TABLE_NAME, PROFILE_ID_COLUMNS,
					Profiles.NAME + " = '" + name + "'", null, null, null,
					null, "1");
			if (cursor.getCount() > 0 && cursor.moveToNext()) {
				profileId = cursor.getLong(PROFILE_ID_INDEX);				
			}
		} finally {
			if (cursor != null) {
				cursor.deactivate();
				cursor.close();
				cursor = null;
			}
			if (db != null) {
				db.close();
				db = null;
			}
		}

		return profileId;
	}
	
	boolean profileExists(String name) {
		return getProfileIdFromName(name) != Consts.NOT_A_PROFILE_ID; 
	}

	boolean profileExists(long profileId) {
		return getProfielNameFromId(profileId) != null;
	}

	
	long createNewProfile(String name) {
		SQLiteDatabase db = null;
		long profileId = Consts.NOT_A_PROFILE_ID;
		
		try {
			db = mDatabaseHelper.getWritableDatabase();
			ContentValues contentValues = new ContentValues();
			contentValues.put(Profiles.NAME, name);
			profileId = db.insert(PROFILES_TABLE_NAME, null, contentValues);
			if (profileId == -1) {
				profileId = Consts.NOT_A_PROFILE_ID;
			}
		} finally {
			if (db != null) {
				db.close();
				db = null;
			}
		}
		
		return profileId;
	}

	void deleteProfile(String name) {
		// First get the preferences object because getPreferencesForProfile()
		// tries to access the database and if we delete the profile name we
		// will fail
		SharedPreferences profilePreferences = null;
		SQLiteDatabase db = null;
		SharedPreferences.Editor editor = null;
		try {
			profilePreferences = getPreferencesForProfile(name,
					Context.MODE_PRIVATE);
			db = mDatabaseHelper.getWritableDatabase();
			/* int numDeleted = */db.delete(PROFILES_TABLE_NAME, Profiles.NAME
					+ " = '" + name + "'", null);
			// TODO check that numDelete == 1
			editor = profilePreferences.edit();
			editor.clear();
			editor.commit();
		} finally {
			editor = null;
			if (db != null) {
				db.close();
				db = null;
			}
		}
	}

	void renameProfile(String oldName, String newName) {
		SQLiteDatabase db = null; 
		try {
			db = mDatabaseHelper.getWritableDatabase();
			ContentValues contentValues = new ContentValues();
			contentValues.put(Profiles.NAME, newName);
			// TODO make sure only one row was affected
			db.update(PROFILES_TABLE_NAME, contentValues, Profiles.NAME + " = '"
					+ oldName + "'", null);
		} finally {
			if (db != null) {
				db.close();
				db = null;
			}
		}
	}

	String getPreferencesFileNameForProfile(String profileName) {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		long profileIdIndex = Consts.NOT_A_PROFILE_ID;
		try {
			db = mDatabaseHelper.getReadableDatabase();
			cursor = db.query(PROFILES_TABLE_NAME, PROFILE_ID_COLUMNS,
					Profiles.NAME + " = '" + profileName + "'", null, 
					null, null, null, "1");
			if (cursor.getCount() > 0) {
				cursor.moveToNext();
				profileIdIndex = cursor.getLong(PROFILE_ID_INDEX);
			}
		} finally {
			if (cursor != null) {
				cursor.deactivate();
				cursor.close();
				cursor = null;
			}
			if (db != null) {
				db.close();
				db = null;
			}
		}

		return getPreferencesFileNameForProfileId(profileIdIndex);
	}

	String getPreferencesFileNameForProfileId(long profileId) {
		return Consts.Prefs.Profile.PREFIX + profileId;
	}

	SharedPreferences getPreferencesForProfile(long profileId,
			int mode) {
		return mContext.getSharedPreferences(
				getPreferencesFileNameForProfileId(profileId), mode);
	}

	static final class Profiles implements BaseColumns {
		public static final String NAME = "name";
	}

	private SharedPreferences getPreferencesForProfile(String profileName,
			int mode) {
		return mContext.getSharedPreferences(
				getPreferencesFileNameForProfile(profileName), mode);
	}

	private class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + PROFILES_TABLE_NAME + " ("
					+ Profiles._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
					Profiles.NAME + " TEXT" + ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion,
				int newVersion) {
		}
	}

	private ProfilePreferencesHelper(Context context) {
		mDatabaseHelper = new DatabaseHelper(context);
		mContext = context;
	}
	
	private static final String DATABASE_NAME = "dindy.db";
	private static final int DATABASE_VERSION = 1;
	private static final String PROFILES_TABLE_NAME = "profiles";

	private static final String[] PROFILE_NAME_COLUMNS = { Profiles.NAME };
	private static final int PROFILE_NAME_INDEX = 0;

	private static final String[] PROFILE_ID_COLUMNS = { Profiles._ID };
	private static final int PROFILE_ID_INDEX = 0;

	private static final String ANY_PROFILES_EXIST_QUERY =  
		"SELECT COUNT(*) FROM " + PROFILES_TABLE_NAME + ";";
	
	private DatabaseHelper mDatabaseHelper;
	private Context mContext;
	
	private static ProfilePreferencesHelper mInstance = null;
}
