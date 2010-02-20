package net.gnobal.dindy;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.Collections;
import java.util.LinkedList;

public class ProfilePreferencesHelper {
	static void createInstance(Context context) {
		if (mInstance != null) {
			// This should not happen - should be created only once
			return;
		}
		mInstance = new ProfilePreferencesHelper(context);
	}

	public static ProfilePreferencesHelper instance() {
		return mInstance;
	}

	boolean anyProfilesExist() {
		if (!mCacheIsOutdated) {
			return !mCachedNames.isEmpty();
		}
		
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
		if (mCacheIsOutdated) {
			loadCache();
		}

		return mCachedNames;
	}

	public String getProfielNameFromId(long profileId) {
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
		if (!mCacheIsOutdated) {
			return mCachedNames.contains(name);
		}
		return getProfileIdFromName(name) != Consts.NOT_A_PROFILE_ID; 
	}

	boolean profileExists(long profileId) {
		return getProfielNameFromId(profileId) != null;
	}

	long createNewProfile(String name) {
		SQLiteDatabase db = null;
		long profileId = Consts.NOT_A_PROFILE_ID;
		boolean success = true;
		
		try {
			db = mDatabaseHelper.getWritableDatabase();
			ContentValues contentValues = new ContentValues();
			contentValues.put(Profiles.NAME, name);
			profileId = db.insert(PROFILES_TABLE_NAME, null, contentValues);
			if (profileId == -1) {
				profileId = Consts.NOT_A_PROFILE_ID;
			}
		} catch (Throwable t) {
			success = false;
		} finally {
			if (db != null) {
				db.close();
				db = null;
			}			
		}

		if (mCacheIsOutdated) {
			// no reason to even try update the cache
			return profileId;
		}
		if (!success) {
			mCacheIsOutdated = true;
			return profileId;
		}
		mCachedNames.add(name);
		Collections.sort(mCachedNames);
		return profileId;
	}

	void deleteProfile(Context context, String name) {
		// First get the preferences object because getPreferencesForProfile()
		// tries to access the database and if we delete the profile name we
		// will fail
		SharedPreferences profilePreferences = null;
		SQLiteDatabase db = null;
		SharedPreferences.Editor editor = null;
		boolean success = true;
		
		try {
			profilePreferences = getPreferencesForProfile(context, name,
					Context.MODE_PRIVATE);
			db = mDatabaseHelper.getWritableDatabase();
			/* int numDeleted = */db.delete(PROFILES_TABLE_NAME, Profiles.NAME
					+ " = '" + name + "'", null);
			// TODO check that numDelete == 1
			editor = profilePreferences.edit();
			editor.clear();
			editor.commit();
		} catch (Throwable t) {
			success = false;
		} finally {
			editor = null;
			if (db != null) {
				db.close();
				db = null;
			}
		}
		
		if (mCacheIsOutdated) {
			// no reason to even try update the cache
			return;
		}
		if (!success || !mCachedNames.remove(name)) {
			mCacheIsOutdated = true;
		}
	}

	void renameProfile(String oldName, String newName) {
		SQLiteDatabase db = null;
		boolean success = true;
		
		try {
			db = mDatabaseHelper.getWritableDatabase();
			ContentValues contentValues = new ContentValues();
			contentValues.put(Profiles.NAME, newName);
			// TODO make sure only one row was affected
			db.update(PROFILES_TABLE_NAME, contentValues, Profiles.NAME + " = '"
					+ oldName + "'", null);
		} catch (Throwable t) {
			success = false;
		} finally {
			if (db != null) {
				db.close();
				db = null;
			}
		}
		
		if (mCacheIsOutdated) {
			// no reason to even try update the cache
			return;
		}
		if (!success || !mCachedNames.remove(oldName)) {
			mCacheIsOutdated = true;
			return;
		}
		mCachedNames.add(newName);
		Collections.sort(mCachedNames);
	}

	String getPreferencesFileNameForProfile(String profileName) {
		long profileId  = getProfileIdFromName(profileName);
		return getPreferencesFileNameForProfileId(profileId);
	}

	String getPreferencesFileNameForProfileId(long profileId) {
		return Consts.Prefs.Profile.PREFIX + profileId;
	}

	SharedPreferences getPreferencesForProfile(Context context, long profileId,
			int mode) {
		return context.getSharedPreferences(
				getPreferencesFileNameForProfileId(profileId), mode);
	}

	static final class Profiles implements BaseColumns {
		public static final String NAME = "name";
	}

	private boolean loadCache() {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		boolean success = true;
		
		try {
			db = mDatabaseHelper.getReadableDatabase();
			cursor = db.query(PROFILES_TABLE_NAME, PROFILE_NAME_COLUMNS,
					null, null, null, null, Profiles.NAME + " ASC");
			mCachedNames.clear();
			while (cursor.moveToNext()) {
				mCachedNames.add(cursor.getString(PROFILE_NAME_INDEX));
			}
		} catch (Throwable t) {
			success = false;
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
		
		if (success) {
			mCacheIsOutdated = false;
		}
		
		return success;
	}
	
	private SharedPreferences getPreferencesForProfile(Context context,
			String profileName,	int mode) {
		return context.getSharedPreferences(
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
		// mContext = context;
	}
	
	private static final String DATABASE_NAME = "dindy.db";
	private static final int DATABASE_VERSION = 1;
	private static final String PROFILES_TABLE_NAME = "profiles";

	private static final String[] PROFILE_NAME_COLUMNS = { Profiles.NAME };
	private static final int PROFILE_NAME_INDEX = 0;

	private static final String[] PROFILE_ID_COLUMNS = { Profiles._ID };
	private static final int PROFILE_ID_INDEX = 0;

	private static final String ANY_PROFILES_EXIST_QUERY =  
		"SELECT COUNT(*) FROM " + PROFILES_TABLE_NAME;
	
	private DatabaseHelper mDatabaseHelper;
	// private Context mContext;
	private boolean mCacheIsOutdated = true;
	private LinkedList<String> mCachedNames = new LinkedList<String>(); 
	
	private static ProfilePreferencesHelper mInstance = null;
}
