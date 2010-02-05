package net.gnobal.dindy;

import android.app.Notification;
import android.app.NotificationManager;
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
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Config;
import android.util.Log;

public class DindyService extends Service {
	@Override
	public void onCreate() {
		super.onCreate();
		if (mLogic != null) {
			// already created
			return;
		}

		mStateListener = new CallStateChangeListener();
		mBroadcastReceiver = new DindyBroadcastReceiver();
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mTM = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		mAM = (AudioManager) getSystemService(AUDIO_SERVICE);
		mPM = (PowerManager) getSystemService(POWER_SERVICE);
		// Start listening to call state change events
		mCallLogObserver = new CallLogObserver();
		mCallLogCursor = getContentResolver().query(
				CallLog.Calls.CONTENT_URI, mCallLogProjection,
				CALL_LOG_QUERY, null, CallLog.Calls.DATE + " DESC");
		mPreviousCursorCount = mCallLogCursor.getCount();
		mCallLogCursor.registerContentObserver(mCallLogObserver);
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
		//filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
		//filter.addAction(AudioManager.VIBRATE_SETTING_CHANGED_ACTION);
		registerReceiver(mBroadcastReceiver, filter);
		mPreferencesHelper = ProfilePreferencesHelper.instance();
		mLogic = new DindyLogic(getApplicationContext(), getContentResolver(),
				mSettings, mAM, mPM);
		// TODO must be after setting mLogic, since registration 
		// immediately sends us an IDLE notification that goes straight to 
		// mLogic. If the user is clicking fast enough on the start/stop 
		// button and this line was before setting mLogic, we would crash
		// because of a null exception
		mTM.listen(mStateListener, PhoneStateListener.LISTEN_CALL_STATE);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		mIsRunning = true;
		final boolean firstStart = 
			(mCurrentProfileId == Consts.NOT_A_PROFILE_ID);
		Bundle extras = intent.getExtras();
		if (extras != null) {
			// Whoever started the service gave us a profile ID to use so we use
			// it blindly
			mCurrentProfileId = extras.getLong(EXTRA_PROFILE_ID);
			refreshSettings(mCurrentProfileId, firstStart);
		} else {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, 
					"error! no extras sent to service");
		}
		
		mLogic.start();

		// Display a notification about us starting. We put an icon in the
		// status bar.
		if (firstStart) {
			showNotification();
		} else {
			// TODO should we display a "Dindy refreshed" toast? 
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop listening to call state change events
		unregisterReceiver(mBroadcastReceiver);
		mTM.listen(mStateListener, PhoneStateListener.LISTEN_NONE);
		mNM.cancel(R.string.dindy_service_started);
		mCallLogCursor.unregisterContentObserver(mCallLogObserver);
		mCallLogCursor.deactivate();
		mCallLogCursor.close();
		mLogic.stop();
		mLogic.destroy();
		mStateListener = null;
		mTM = null;
		mCallLogCursor = null;
		mPreviousCursorCount = Integer.MAX_VALUE;
		mCallLogObserver = null;
		mNM = null;
		mAM = null;
		mPM = null;
		mBroadcastReceiver = null;
		mPreferencesHelper = null;
		mLogic = null;
		mCurrentProfileId = Consts.NOT_A_PROFILE_ID;
		mIsRunning = false;
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
	
	public static final String EXTRA_PROFILE_ID = "profile_id";

	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.dindy_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(
				R.drawable.notification_icon, text, System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(
				getApplicationContext(), 0,
				new Intent(getApplicationContext(), Dindy.class)
					.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_CLEAR_TOP)
					.setAction(Intent.ACTION_MAIN), 0);
		
		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(getApplicationContext(),
				getText(R.string.dindy_service_label), text, contentIntent);

		// Send the notification.
		// We use a layout id because it is a unique number. We use it later
		// to cancel.
		mNM.notify(R.string.dindy_service_started, notification);
	}

	private void refreshSettings(long selectedProfileId, boolean firstStart) {
		SharedPreferences profilePreferences = 
			mPreferencesHelper.getPreferencesForProfile(this, selectedProfileId,
					Context.MODE_PRIVATE);

		final boolean firstRingSound = profilePreferences.getBoolean(
				Consts.Prefs.Profile.KEY_FIRST_RING_SOUND, false);
		final boolean firstRingVibrate = profilePreferences.getBoolean(
				Consts.Prefs.Profile.KEY_FIRST_RING_VIBRATE, false);
		final boolean secondRingSound = profilePreferences.getBoolean(
				Consts.Prefs.Profile.KEY_SECOND_RING_SOUND, true);
		final boolean secondRingVibrate = profilePreferences.getBoolean(
				Consts.Prefs.Profile.KEY_SECOND_RING_VIBRATE, true);

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
		
		mSettings.mEnableSms = profilePreferences.getBoolean(
				Consts.Prefs.Profile.KEY_ENABLE_SMS, true);
		mSettings.mMessage = profilePreferences.getString(
				Consts.Prefs.Profile.KEY_SMS_MESSAGE, Consts.EMPTY_STRING);
		
		// First let's see what the new value is. We need to notify DindyLogic
		// about this change so we need to keep the new setting in a different
		// variable
		final long newWakeupTimeoutMinutes = 
			Long.parseLong(profilePreferences.getString(
					Consts.Prefs.Profile.KEY_TIME_BETWEEN_CALLS_MINUTES,
					Consts.Prefs.Profile.VALUE_TIME_BETWEEN_CALLS_DEFAULT));
		long newWakeupTimeoutMillis = Consts.INFINITE_TIME;
		if (newWakeupTimeoutMinutes != Consts.INFINITE_TIME) {
			newWakeupTimeoutMillis =
				newWakeupTimeoutMinutes * Consts.MILLIS_IN_MINUTE;
		}
		mLogic.onWakeupTimeChanged(
				mSettings.mWakeupTimeoutMillis,
				newWakeupTimeoutMillis);
		mSettings.mWakeupTimeoutMillis = newWakeupTimeoutMillis;

		mSettings.mTreatUnknownCallers = profilePreferences.getString(
				Consts.Prefs.Profile.KEY_TREAT_UNKNOWN_CALLERS,
				Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_FIRST);
		
		if (firstStart) {
			// Only if it's a first start we get the user's settings. Otherwise
			// we might read our own settings
			mSettings.mUserSettings.mVibrateModeNotification = 
				mAM.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION);
			mSettings.mUserSettings.mVibrateModeRinger =
				mAM.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
			mSettings.mUserSettings.mRingerMode = mAM.getRingerMode();
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
				// HACK the following line exists so that we'll update the call
				// log observer in case the user cleared the call log and we
				// didn't get the notification about it because of a bug in
				// Android.
				// This is no longer needed starting with Android 2.0.1
				mCallLogObserver.dispatchChange(true);
				// HACK END
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

	private class DindyBroadcastReceiver extends BroadcastReceiver {
		public void onReceive(final Context context, final Intent intent) {
			String action = intent.getAction();
			Bundle extras = intent.getExtras(); 
			if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
				String phoneNumber = extras.getString(
					Intent.EXTRA_PHONE_NUMBER);
				if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
						"outgoing call number: " + phoneNumber);
				mLogic.onOutgoingCall(phoneNumber);
			}/* else if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
				mLogic.onRingerModeChanged(extras.getInt(
					AudioManager.EXTRA_RINGER_MODE));
			} else if (action.equals(AudioManager.VIBRATE_SETTING_CHANGED_ACTION)) {
				mLogic.onVibrateSettingChanged(
						extras.getInt(AudioManager.EXTRA_VIBRATE_TYPE),
						extras.getInt(AudioManager.EXTRA_VIBRATE_SETTING));
			}*/

		}
	}

	private class CallLogObserver extends ContentObserver {
		public CallLogObserver() {
			super(new Handler());
		}

		@Override
		public void onChange(boolean selfChange) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, "call log database changed");
			if (!mCallLogCursor.requery()) {
				if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, 
						"requery() failed. Cursor is invalid");
				// TODO recreate cursor
				return;
			}

			final int currentCursorCount = mCallLogCursor.getCount();
			if (currentCursorCount <= mPreviousCursorCount) {
				// This means that there isn't really new data in the cursor for
				// us. It can happen when the user hangs up on the caller while
				// the phone is ringing - we get a change notification even
				// though there isn't really new data for us.
				// Had there been new data, the count would have grown. We
				// still update the previous cursor count to make this correct
				// for the next time as well (for example if the user deleted
				// one missed call from the calls log).
				if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
						"onChange: changing previous cursor count from "
						+ mPreviousCursorCount + " to " + currentCursorCount);
				mPreviousCursorCount = currentCursorCount;
				return;
			}
			mPreviousCursorCount = currentCursorCount;

			if (!mCallLogCursor.moveToNext()) {
				if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, 
						"moveToNext() failed. No missed calls");
				return;
			}

			int callType = mCallLogCursor.getInt(CALL_LOG_FIELD_TYPE);
			switch (callType) {
			case CallLog.Calls.MISSED_TYPE:
				mLogic.onCallStateChange(Consts.IncomingCallState.MISSED,
						mCallLogCursor.getString(CALL_LOG_FIELD_NUMBER));
				break;
			
			case CallLog.Calls.INCOMING_TYPE:
				mLogic.onCallStateChange(Consts.IncomingCallState.INCOMING,
						mCallLogCursor.getString(CALL_LOG_FIELD_NUMBER));
				break;
			}
		}
	}

	private DindyLogic mLogic = null;
	private NotificationManager mNM = null;
	private TelephonyManager mTM = null;
	private AudioManager mAM = null;
	private PowerManager mPM = null;
	private CallStateChangeListener mStateListener = null;
	private DindyBroadcastReceiver mBroadcastReceiver = null;
	private CallLogObserver mCallLogObserver = null;
	private Cursor mCallLogCursor = null;
	private int mPreviousCursorCount = Integer.MAX_VALUE;
	private ProfilePreferencesHelper mPreferencesHelper = null;
	private DindySettings mSettings = new DindySettings();
	private long mCurrentProfileId = Consts.NOT_A_PROFILE_ID;
	private static boolean mIsRunning = false;

	private final IBinder mBinder = new LocalBinder();
	private final String[] mCallLogProjection = 
	{ 
		CallLog.Calls.NUMBER, // 0
		CallLog.Calls.TYPE    // 1
	};
	private final int CALL_LOG_FIELD_NUMBER = 0;
	private final int CALL_LOG_FIELD_TYPE = 1;
	private final String CALL_LOG_QUERY = 
		CallLog.Calls.TYPE + " = " + CallLog.Calls.MISSED_TYPE + 
		" OR " + CallLog.Calls.TYPE + " = " + CallLog.Calls.INCOMING_TYPE; 
}
