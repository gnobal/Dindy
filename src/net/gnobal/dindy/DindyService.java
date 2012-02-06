package net.gnobal.dindy;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class DindyService extends Service {
	@Override
	public void onCreate() {
		super.onCreate();
		if (mLogic != null) {
			// already created
			return;
		}

		mStateListener = new CallStateChangeListener();
		mBroadcastReceiver = new DindyServiceBroadcastReceiver();
		mTM = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		mAuM = (AudioManager) getSystemService(AUDIO_SERVICE);
		mAlM = (AlarmManager) getSystemService(ALARM_SERVICE);
		mPM = (PowerManager) getSystemService(POWER_SERVICE);
		// Start listening to call state change events
		mCallLogObserver = new CallLogObserver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
		filter.addAction(Consts.ACTION_STOP_DINDY_SERVICE);
		filter.addAction(SMS_RECEIVED_ACTION);
		registerReceiver(mBroadcastReceiver, filter);
		mPreferencesHelper = ProfilePreferencesHelper.instance();
		mPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
			DindyService.getStopServiceBroadcastIntent(), 0);	

		mLogic =
			new DindyLogic(getApplicationContext(), getContentResolver(), mSettings, mAuM, mPM);
		// TODO must be after setting mLogic, since registration 
		// immediately sends us an IDLE notification that goes straight to 
		// mLogic. If the user is clicking fast enough on the start/stop 
		// button and this line was before setting mLogic, we would crash
		// because of a null exception
		mTM.listen(mStateListener, PhoneStateListener.LISTEN_CALL_STATE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		mIsRunning = true;
		final long previousProfileId = mCurrentProfileId;
		final boolean firstStart = (mCurrentProfileId == Consts.NOT_A_PROFILE_ID);
		final boolean restartAfterKill = (intent == null);
		
		if (!restartAfterKill) {
			final Bundle extras = intent.getExtras();
			if (extras == null) {
				if (Consts.DEBUG) {
					Log.d(Consts.LOGTAG, "error! no extras sent to service");
				}
				stopSelf();
				return START_STICKY;
			}
			storeLastStartupSettings(firstStart, extras);
		}
		final Bundle startupSettings = retrieveLastStartupSettings();
		
		
		// Whoever started the service gave us a profile ID to use so we use
		// it blindly
		mCurrentProfileId = startupSettings.getLong(Consts.Prefs.Main.KEY_LAST_STARTUP_PROFILE_ID);
		final String profileName = startupSettings.getString(Consts.Prefs.Main.KEY_LAST_STARTUP_PROFILE_NAME);
		if (!mPreferencesHelper.profileExists(mCurrentProfileId)) {
			if (Consts.DEBUG) {
				Log.d(Consts.LOGTAG, "profile ID " + mCurrentProfileId + " doesn't exist");
			}
			if (firstStart) {
				Toast.makeText(getApplicationContext(),
					R.string.toast_text_profile_doesnt_exist_exit,
					Toast.LENGTH_LONG).show();
				stopSelf();
				return START_STICKY;
			} else {
				Toast.makeText(getApplicationContext(),
					R.string.toast_text_profile_doesnt_exist_continue,
					Toast.LENGTH_LONG).show();
				mCurrentProfileId = previousProfileId;
			}
		}
		if (previousProfileId != mCurrentProfileId) {
			DindySingleProfileAppWidgetProvider.updateAllSingleProfileWidgets(
				getApplicationContext(), mCurrentProfileId, previousProfileId);
		}
		
		// We need to remember the user's settings in the following cases:
		// 1. This is the first startup of the service
		// 2. The service was killed and restarted. In this case we store whatever we stored when
		//    when the service was first started
		// In any other case we avoid remembering the user settings because they may have been 
		// set by us
		refreshSettings(mCurrentProfileId, firstStart || restartAfterKill, startupSettings);
		saveLastUsedProfileId();
		
		mLogic.start(restartAfterKill);
		mAlM.cancel(mPendingIntent);
		final long absoluteTimeLimitMillis = startupSettings.getLong(
			Consts.Prefs.Main.KEY_LAST_STARTUP_INTENT_ABS_TIME_LIMIT_MILLIS); 
		if (absoluteTimeLimitMillis != Consts.NOT_A_TIME_LIMIT) {
			mAlM.set(AlarmManager.RTC_WAKEUP, absoluteTimeLimitMillis, mPendingIntent);
		}

		if (Consts.DEBUG) {
			final long currentTimeMillis = System.currentTimeMillis();
			Log.d(Consts.LOGTAG, "starting profile " + mCurrentProfileId +
				", absolute time limit " + absoluteTimeLimitMillis +
				" millis, current time millis " + currentTimeMillis + ", diff " +
				(absoluteTimeLimitMillis - currentTimeMillis));
		}

		// Display a notification about us starting. We put an icon in the
		// status bar.
		if (firstStart) {
			showNotification();
		} else {
			String refreshText = getString(R.string.dindy_service_refreshed_text);
			if (profileName != null && profileName.length() > 0) {
				refreshText = refreshText + " (" + profileName + ")";
			}
			final int source = startupSettings.getInt(
				Consts.Prefs.Main.KEY_LAST_STARTUP_INTENT_SOURCE); 				
			if (source == Consts.INTENT_SOURCE_SHORTCUT ||
				source == Consts.INTENT_SOURCE_APP_PROFILE_PREFS) {
				Toast.makeText(getApplicationContext(), refreshText, Toast.LENGTH_SHORT).show();
			}
		}
		
		sendBroadcast(new Intent().setAction(Consts.SERVICE_STARTED));
		
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (Consts.DEBUG) {
			Log.d(Consts.LOGTAG, "stopping profile " + mCurrentProfileId);
		}

		DindySingleProfileAppWidgetProvider.updateAllSingleProfileWidgets(
			getApplicationContext(), Consts.NOT_A_PROFILE_ID, mCurrentProfileId);

		// Stop listening to call state change events
		//mHandler.removeCallbacks(mStopServiceCallback);
		mAlM.cancel(mPendingIntent);
		unregisterReceiver(mBroadcastReceiver);
		mTM.listen(mStateListener, PhoneStateListener.LISTEN_NONE);
		stopForeground(true);
		mLogic.stop();
		mLogic.destroy();
		mCallLogObserver.destroy();
		mStateListener = null;
		mTM = null;
		mCallLogObserver = null;
		mAuM = null;
		mAlM = null;
		mPM = null;
		mBroadcastReceiver = null;
		mPreferencesHelper = null;
		mLogic = null;
		// Must happen last because it's used here
		mCurrentProfileId = Consts.NOT_A_PROFILE_ID;
		mIsRunning = false;
		sendBroadcast(new Intent().setAction(Consts.SERVICE_STOPPED));
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		public DindyService getService() {
			return DindyService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public static boolean isRunning() {
		return mIsRunning;
	}
	
	public static long getCurrentProfileId() {
		return mCurrentProfileId;
	}
	
	public static Intent getStartServiceIntent(Context context,
			long profileId, String profileName, int source,
			long timeLimitMillis) {
		return prepareStartServiceIntent(
				new Intent(context, DindyService.class), profileId, profileName,
					source, timeLimitMillis);
	}
	
	public static Intent prepareStartServiceIntent(
			Intent intent, long profileId, String profileName, int source,
			long timeLimitMillis) {
		if (profileName == null) {
			// Try to find out the profile name from the preferences
			profileName = ProfilePreferencesHelper.instance().getProfielNameFromId(profileId);
		}
		return intent
			.putExtra(Consts.EXTRA_PROFILE_ID, profileId)
			.putExtra(Consts.EXTRA_PROFILE_NAME, profileName)
			.putExtra(Consts.EXTRA_INTENT_TIME_LIMIT_MILLIS, timeLimitMillis)
			// See:
			// http://www.developer.com/ws/article.php/3837531/Handling-User-Interaction-with-Android-App-Widgets.htm
			.putExtra(Consts.EXTRA_INTENT_SOURCE, source)
			.setData(Uri.withAppendedPath(Uri.parse("dindy://profile/id/"),
				String.valueOf(profileId)));
	}

	public static Intent getStopServiceBroadcastIntent() {
		return new Intent(Consts.ACTION_STOP_DINDY_SERVICE);
	}
	
	public static Intent getStopServiceIntent(Context context) {
		return new Intent(context, DindyService.class);
	}
	
	private void saveLastUsedProfileId() {
		SharedPreferences preferences = getSharedPreferences(
			Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong(Consts.Prefs.Main.LAST_USED_PROFILE_ID, mCurrentProfileId);
		editor.commit();
		editor = null;
		preferences = null;
	}
	
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.dindy_service_started);// + " (" + 
			//mPreferencesHelper.getProfielNameFromId(mCurrentProfileId) + ")";

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(
			R.drawable.notification_icon, text, System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent =
			Dindy.getPendingIntent(getApplicationContext());
		
		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(getApplicationContext(),
			getText(R.string.dindy_service_label), text, contentIntent);

		// Send the notification.
		// We use a layout id because it is a unique number
		startForeground(R.string.dindy_service_started, notification);
	}

	private void refreshSettings(long selectedProfileId, boolean rememberUserSettings,
		Bundle startupSettings) {
		SharedPreferences profilePreferences = mPreferencesHelper.getPreferencesForProfile(
			this, selectedProfileId, Context.MODE_PRIVATE);

		final boolean firstRingSound = profilePreferences.getBoolean(
			Consts.Prefs.Profile.KEY_FIRST_EVENT_SOUND, false);
		final boolean firstRingVibrate = profilePreferences.getBoolean(
			Consts.Prefs.Profile.KEY_FIRST_EVENT_VIBRATE, false);
		final boolean secondRingSound = profilePreferences.getBoolean(
			Consts.Prefs.Profile.KEY_SECOND_EVENT_SOUND, true);
		final boolean secondRingVibrate = profilePreferences.getBoolean(
			Consts.Prefs.Profile.KEY_SECOND_EVENT_VIBRATE, true);

		mSettings.mFirstRingSettings.mRingerMode = firstRingSound ? 
			AudioManager.RINGER_MODE_NORMAL
			: (firstRingVibrate ? 
				AudioManager.RINGER_MODE_VIBRATE 
				: AudioManager.RINGER_MODE_SILENT);
		mSettings.mFirstRingSettings.mVibrateModeNotification =
		mSettings.mFirstRingSettings.mVibrateModeRinger = firstRingVibrate ? 
			AudioManager.VIBRATE_SETTING_ON
			: AudioManager.VIBRATE_SETTING_OFF;
		mSettings.mSecondRingSettings.mRingerMode = secondRingSound ? 
			AudioManager.RINGER_MODE_NORMAL
			: (secondRingVibrate ? 
				AudioManager.RINGER_MODE_VIBRATE 
				: AudioManager.RINGER_MODE_SILENT);
		mSettings.mSecondRingSettings.mVibrateModeNotification =
		mSettings.mSecondRingSettings.mVibrateModeRinger = secondRingVibrate ? 
			AudioManager.VIBRATE_SETTING_ON
			: AudioManager.VIBRATE_SETTING_OFF;
		
		mSettings.mEnableSmsReplyToCall = profilePreferences.getBoolean(
			Consts.Prefs.Profile.KEY_ENABLE_SMS_CALLERS,
			Consts.Prefs.Profile.VALUE_ENABLE_SMS_CALLERS_DEFAULT);
		mSettings.mMessageToCallers = profilePreferences.getString(
			Consts.Prefs.Profile.KEY_SMS_MESSAGE_CALLERS, getString(
				R.string.preferences_profile_sms_message_callers_default_unset_value));
		mSettings.mEnableSmsReplyToSms = profilePreferences.getBoolean(
			Consts.Prefs.Profile.KEY_ENABLE_SMS_TEXTERS,
			Consts.Prefs.Profile.VALUE_ENABLE_SMS_TEXTERS_DEFAULT);
		mSettings.mMessageToTexters = profilePreferences.getString(
			Consts.Prefs.Profile.KEY_SMS_MESSAGE_TEXTERS, getString(
				R.string.preferences_profile_sms_message_texters_default_unset_value));
		
		// First let's see what the new value is. We need to notify DindyLogic
		// about this change so we need to keep the new setting in a different
		// variable
		final long newWakeupTimeoutMinutes = 
			Long.parseLong(profilePreferences.getString(
				Consts.Prefs.Profile.KEY_TIME_BETWEEN_EVENTS_MINUTES,
				Consts.Prefs.Profile.VALUE_TIME_BETWEEN_EVENTS_DEFAULT));
		long newWakeupTimeoutMillis = Consts.INFINITE_TIME;
		if (newWakeupTimeoutMinutes != Consts.INFINITE_TIME) {
			newWakeupTimeoutMillis = newWakeupTimeoutMinutes * Consts.MILLIS_IN_MINUTE;
		}
		mLogic.onWakeupTimeChanged(mSettings.mWakeupTimeoutMillis, newWakeupTimeoutMillis);
		mSettings.mWakeupTimeoutMillis = newWakeupTimeoutMillis;

		mSettings.mTreatUnknownCallers = profilePreferences.getString(
			Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_CALLERS,
			Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_DEFAULT);
		// Non-mobile callers was added in 1.1.1 and takes the default from the
		// existing unknown callers setting
		mSettings.mTreatNonMobileCallers = profilePreferences.getString(
			Consts.Prefs.Profile.KEY_TREAT_NON_MOBILE_CALLERS, mSettings.mTreatUnknownCallers);
		mSettings.mTreatUnknownTexters = profilePreferences.getString(
			Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_TEXTERS,
			Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_TEXTERS_DEFAULT);
		
		if (rememberUserSettings) {
			mSettings.mUserSettings.mVibrateModeNotification = 
				//mAuM.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION);
				startupSettings.getInt(Consts.Prefs.Main.KEY_LAST_STARTUP_VIBRATE_TYPE_NOTIFICATION);
			mSettings.mUserSettings.mVibrateModeRinger =
				//mAuM.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
				startupSettings.getInt(Consts.Prefs.Main.KEY_LAST_STARTUP_VIBRATE_TYPE_RINGER);
			mSettings.mUserSettings.mRingerMode =
				//mAuM.getRingerMode();
				startupSettings.getInt(Consts.Prefs.Main.KEY_LAST_STARTUP_RINGER_MODE);
		}
	}

	private class CallStateChangeListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			int incomingCallState = Consts.IncomingCallState.IDLE;
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				incomingCallState = Consts.IncomingCallState.IDLE;
				break;

			case TelephonyManager.CALL_STATE_RINGING:
//				// HACK the following line exists so that we'll update the call
//				// log observer in case the user cleared the call log and we
//				// didn't get the notification about it because of a bug in
//				// Android.
//				// This is no longer needed starting with Android 2.0.1
//				if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.DONUT) {
//					mCallLogObserver.dispatchChange(true);
//				}
//				// HACK END
				incomingCallState = Consts.IncomingCallState.RINGING;
				break;

			case TelephonyManager.CALL_STATE_OFFHOOK:
				incomingCallState = Consts.IncomingCallState.OFFHOOK;
				break;

			default:
				break;
			}
			
			mLogic.onCallStateChange(incomingCallState, incomingNumber);
		}
	}

	public class DindyServiceBroadcastReceiver extends BroadcastReceiver {
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
			if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
				Bundle extras = intent.getExtras(); 
				String phoneNumber = extras.getString(
					Intent.EXTRA_PHONE_NUMBER);
				if (Consts.DEBUG) {
					Log.d(Consts.LOGTAG, "outgoing call number: " + phoneNumber);
				}
				mLogic.onOutgoingCall(phoneNumber);
			} else if (Consts.ACTION_STOP_DINDY_SERVICE.equals(action)) {
				DindyService.this.stopSelf();
			} else if (SMS_RECEIVED_ACTION.equals(action)) {
				if (Consts.DEBUG) {
					Log.d(Consts.LOGTAG, "SMS message(s) received");
				}
				String[] addresses = getAddressesFromSmsIntent(intent);
				for (int i = 0; i < addresses.length; ++ i) {
					if (Consts.DEBUG) {
						Log.d(Consts.LOGTAG, "message: address=" + addresses[i]);
					}
					mLogic.onSmsMessage(addresses[i]);
				}
			}
		}
	}

	private abstract class AbstractObserver extends ContentObserver {
		protected AbstractObserver() {
			super(new Handler());
			mCursor = getContentResolver().query(getContentUri(),
				getProjection(), getQuery(), null, getSortOrder());
			mCursor.registerContentObserver(this);
		}
		
		public void destroy() {
			mCursor.unregisterContentObserver(this);
			mCursor.deactivate();
			mCursor.close();
			mCursor = null;
		}
		
		abstract Uri getContentUri();
		abstract String[] getProjection();
		abstract String getQuery();
		abstract String getSortOrder();
		
		protected Cursor mCursor = null;
	}
	
	private class CallLogObserver extends AbstractObserver {
		CallLogObserver() {
			super();
			mPreviousCursorCount = mCursor.getCount();
		}
		
		@Override
		public void destroy() {
			super.destroy();
			mPreviousCursorCount = Integer.MAX_VALUE;
		}
		
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			if (Consts.DEBUG) 
				Log.d(Consts.LOGTAG, "CallLog: database changed");
			if (!mCursor.requery()) {
				if (Consts.DEBUG) Log.d(Consts.LOGTAG, 
						"CallLog: requery() failed. Cursor is invalid");
				// TODO recreate cursor
				return;
			}

			final int currentCursorCount = mCursor.getCount();
			if (currentCursorCount <= mPreviousCursorCount) {
				// This means that there isn't really new data in the cursor for
				// us. It can happen when the user hangs up on the caller while
				// the phone is ringing - we get a change notification even
				// though there isn't really new data for us.
				// Had there been new data, the count would have grown. We
				// still update the previous cursor count to make this correct
				// for the next time as well (for example if the user deleted
				// one missed call from the calls log).
				if (Consts.DEBUG) {
					Log.d(Consts.LOGTAG, "CallLog: onChange: changing previous cursor count "
						+ "from " + mPreviousCursorCount + " to " + currentCursorCount);
				}
				mPreviousCursorCount = currentCursorCount;
				return;
			}
			mPreviousCursorCount = currentCursorCount;

			if (!mCursor.moveToNext()) {
				if (Consts.DEBUG) {
					Log.d(Consts.LOGTAG, "CallLog: moveToNext() failed. No missed calls");
				}
				return;
			}

			int callType = mCursor.getInt(CALL_LOG_FIELD_TYPE);
			if (Consts.DEBUG) {
				Log.d(Consts.LOGTAG, "CallLog: callType=" + callType);
			}
			switch (callType) {
			case CallLog.Calls.MISSED_TYPE:
				mLogic.onMissedCall(mCursor.getString(CALL_LOG_FIELD_NUMBER));
				break;
			
			case CallLog.Calls.INCOMING_TYPE:
				mLogic.onIncomingCallInCallsDB(mCursor.getString(CALL_LOG_FIELD_NUMBER));
				break;
			}
		}

		@Override
		protected Uri getContentUri() { return CallLog.Calls.CONTENT_URI; }
		
		@Override
		protected String[] getProjection() { return mCallLogProjection; }
		
		@Override
		protected String getQuery() { return CALL_LOG_QUERY; }

		@Override
		protected String getSortOrder() { return CALL_LOG_SORT_ORDER; } 

		protected int mPreviousCursorCount = Integer.MAX_VALUE;
	}

    private String[] getAddressesFromSmsIntent(Intent intent) {
        Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
        byte[][] pduObjs = new byte[messages.length][];

        for (int i = 0; i < messages.length; i++) {
            pduObjs[i] = (byte[]) messages[i];
        }
        byte[][] pdus = new byte[pduObjs.length][];
        int pduCount = pdus.length;
        for (int i = 0; i < pduCount; i++) {
            pdus[i] = pduObjs[i];
        }
        
        final String[] addresses = SmsHelper.getAddressesFromSmsPdus(pdus);
        return addresses;
    }


    private void storeLastStartupSettings(boolean firstStart, Bundle startupExtras) {
		SharedPreferences preferences = getSharedPreferences(
			Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong(Consts.Prefs.Main.KEY_LAST_STARTUP_PROFILE_ID,
			startupExtras.getLong(Consts.EXTRA_PROFILE_ID));
   		final String profileName = startupExtras.getString(Consts.EXTRA_PROFILE_NAME);
		editor.putString(Consts.Prefs.Main.KEY_LAST_STARTUP_PROFILE_NAME,
			profileName == null ? "" : profileName);
		final int intentSource = startupExtras.getInt(
			Consts.EXTRA_INTENT_SOURCE, Consts.INTENT_SOURCE_UNKNOWN);
		editor.putInt(Consts.Prefs.Main.KEY_LAST_STARTUP_INTENT_SOURCE, intentSource);
		final long relativeTimeLimitMillis = startupExtras.getLong(
			Consts.EXTRA_INTENT_TIME_LIMIT_MILLIS, Consts.NOT_A_TIME_LIMIT);
		editor.putLong(Consts.Prefs.Main.KEY_LAST_STARTUP_INTENT_ABS_TIME_LIMIT_MILLIS,
			relativeTimeLimitMillis == Consts.NOT_A_TIME_LIMIT ?
			Consts.NOT_A_TIME_LIMIT : System.currentTimeMillis() + relativeTimeLimitMillis);
		
		if (firstStart) {
			// Only store these if this is the first startup. Otherwise we'll read our own settings
			editor.putInt(Consts.Prefs.Main.KEY_LAST_STARTUP_VIBRATE_TYPE_RINGER,
				mAuM.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER));
			editor.putInt(Consts.Prefs.Main.KEY_LAST_STARTUP_VIBRATE_TYPE_NOTIFICATION,
				mAuM.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION));
			editor.putInt(Consts.Prefs.Main.KEY_LAST_STARTUP_RINGER_MODE, mAuM.getRingerMode());
		}
		
		editor.commit();
		editor = null;
		preferences = null;

    }

    private Bundle retrieveLastStartupSettings() {
		SharedPreferences preferences = getSharedPreferences(
			Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
		final Bundle startupSettings = new Bundle();
		startupSettings.putLong(Consts.Prefs.Main.KEY_LAST_STARTUP_PROFILE_ID, preferences.getLong(
			Consts.Prefs.Main.KEY_LAST_STARTUP_PROFILE_ID, Consts.NOT_A_PROFILE_ID));
		startupSettings.putString(Consts.Prefs.Main.KEY_LAST_STARTUP_PROFILE_NAME,
			preferences.getString(Consts.Prefs.Main.KEY_LAST_STARTUP_PROFILE_NAME, null));
		startupSettings.putInt(Consts.Prefs.Main.KEY_LAST_STARTUP_INTENT_SOURCE,
			preferences.getInt(Consts.Prefs.Main.KEY_LAST_STARTUP_INTENT_SOURCE,
				Consts.INTENT_SOURCE_UNKNOWN));
		startupSettings.putLong(Consts.Prefs.Main.KEY_LAST_STARTUP_INTENT_ABS_TIME_LIMIT_MILLIS,
			preferences.getLong(Consts.Prefs.Main.KEY_LAST_STARTUP_INTENT_ABS_TIME_LIMIT_MILLIS,
				Consts.NOT_A_TIME_LIMIT));
		
		startupSettings.putInt(Consts.Prefs.Main.KEY_LAST_STARTUP_VIBRATE_TYPE_RINGER,
			preferences.getInt(Consts.Prefs.Main.KEY_LAST_STARTUP_VIBRATE_TYPE_RINGER,
				AudioManager.VIBRATE_SETTING_ON));
		startupSettings.putInt(Consts.Prefs.Main.KEY_LAST_STARTUP_VIBRATE_TYPE_NOTIFICATION,
			preferences.getInt(Consts.Prefs.Main.KEY_LAST_STARTUP_VIBRATE_TYPE_NOTIFICATION,
				AudioManager.VIBRATE_SETTING_ON));
		startupSettings.putInt(Consts.Prefs.Main.KEY_LAST_STARTUP_RINGER_MODE, preferences.getInt(
			Consts.Prefs.Main.KEY_LAST_STARTUP_RINGER_MODE,
			AudioManager.RINGER_MODE_NORMAL));
		preferences = null;
		
		return startupSettings;
    }
    
	private DindyLogic mLogic = null;
	private TelephonyManager mTM = null;
	private AudioManager mAuM = null;
	private AlarmManager mAlM = null;
	private PowerManager mPM = null;
	private CallStateChangeListener mStateListener = null;
	private DindyServiceBroadcastReceiver mBroadcastReceiver = null;
	private ProfilePreferencesHelper mPreferencesHelper = null;
	private DindySettings mSettings = new DindySettings();
	private PendingIntent mPendingIntent = null;
	private static long mCurrentProfileId = Consts.NOT_A_PROFILE_ID;
	private static boolean mIsRunning = false;
	private final IBinder mBinder = new LocalBinder();
	private CallLogObserver mCallLogObserver = null;
	//private SmsObserver mSmsObserver = null;
	private static final String SMS_RECEIVED_ACTION =
		"android.provider.Telephony.SMS_RECEIVED";
	
	private static final String[] mCallLogProjection = 
	{ 
		CallLog.Calls.NUMBER, // 0
		CallLog.Calls.TYPE   // 1
		//CallLog.Calls.DATE    // 2

	};
	private static final int CALL_LOG_FIELD_NUMBER = 0;
	private static final int CALL_LOG_FIELD_TYPE = 1;
	//private static final int CALL_LOG_FIELD_DATE = 2;
	private static final String CALL_LOG_QUERY = 
		CallLog.Calls.TYPE + " = " + CallLog.Calls.MISSED_TYPE + 
		" OR " + CallLog.Calls.TYPE + " = " + CallLog.Calls.INCOMING_TYPE;
	private static final String CALL_LOG_SORT_ORDER = CallLog.Calls.DATE + " DESC";

}
