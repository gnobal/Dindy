package net.gnobal.dindy;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.PowerManager;
import android.provider.Contacts.Phones;
import android.provider.Contacts.PhonesColumns;
import android.telephony.PhoneNumberUtils;
import android.util.Config;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

// Notes:
// If a call comes in and the caller OR the user hangs up we simply get:
//08-20 19:19:46.911: DEBUG/Dindy(809): state: 1, number: 0002
//08-20 19:19:49.201: DEBUG/Dindy(809): state: 0, number: 0002
//
// If a call comes in and the user answers and then the remote hangs up we get:
//08-20 19:21:14.091: DEBUG/Dindy(809): state: 1, number: 0003
//08-20 19:21:17.161: DEBUG/Dindy(809): state: 2, number: 
//08-20 19:21:25.761: DEBUG/Dindy(809): state: 0, number: 
//
// If a call comes in and the user answers and then hangs up we get:
//08-20 19:23:09.181: DEBUG/Dindy(809): state: 1, number: 0004
//08-20 19:23:11.371: DEBUG/Dindy(809): state: 2, number: 
//08-20 19:23:13.892: DEBUG/Dindy(809): state: 0, number: 

// Sequences for incoming calls and the WakeLock:
// 1. Missed call that should get an SMS
//    ringing (lock) -> idle -> missed (should send) -> SMS -> SMS result (unlock)
// 2. Incoming call that was declined
//    ringing (lock) -> idle -> incoming (unlock)
// 3. Incoming call that was answered
//    ringing (lock) -> off hook (unlock) -> idle -> incoming (do not unlock)
// 4. Missed call that shouldn't get an SMS
//    ringing (lock) -> idle -> missed (shouldn't send) -> unlock 
// Sequences for outgoing calls:
// 5. Outgoing call
//    idle -> off hook (do not unlock)
// 
// The biggest problem we have here is whether to unlock when we get an incoming
// call in the calls DB - we don't know whether we got there from sequence 2 
// (so we should unlock) or 3 (should _not_ unlock)

class DindyLogic {
	public DindyLogic(Context context, ContentResolver resolver,
			DindySettings settings, AudioManager am, PowerManager pm) {
		mContext = context;
		mResolver = resolver;
		mSettings = settings;
		mAM = am;
		mPM = pm;
		mWakeLock = mPM.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Dindy");
	    mSentPendingIntent = PendingIntent.getBroadcast(
	    		mContext, 0, new Intent(SMS_PENDING_INTENT_NAME), 0);
	    mContext.registerReceiver(mOnSentReceiver, new IntentFilter(
	    		SMS_PENDING_INTENT_NAME));
	    mSmsSender = new SmsSender();
	}

	void start() {
		setRingerAndVibrateModes(mSettings.mFirstRingSettings);
		mStarted = true;
	}
	
	void stop() {
		if (!mStarted) {
			return;
		}
		setRingerAndVibrateModes(mSettings.mUserSettings);
		mStarted = false;
	}
	
	void destroy() {
		mContext.unregisterReceiver(mOnSentReceiver);
		mSmsSender = null;
		mOnSentReceiver = null;
		mContext = null;
		mTimer.cancel();
		synchronized (mIncomingCalls) {
			mIncomingCalls.clear();
		}
		releaseWakeLockIfHeld("destroy");
		mWakeLock = null;
		mPM = null;
		mAM = null;
		mResolver = null;
		mSettings = null;
	}

	void onCallStateChange(int newState, String number) {
		if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, "newState: " + 
				Utils.incomingCallStateToString(newState) + 
				", number: " + number);
		if (!mStarted) {
			// can happen if the service is started with a non-existing profile
			// and then stops itself
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, 
					"onCallStateChange: called without being started");
			return;
		}
		switch (newState) {
		case Consts.IncomingCallState.IDLE:
			onIdle(number);
			break;

		case Consts.IncomingCallState.RINGING:
			onRinging(number);
			mLastRingingNumber = PhoneNumberUtils.toCallerIDMinMatch(number);
			break;

		case Consts.IncomingCallState.OFFHOOK:
			onOffHook(number);
			break;

		case Consts.IncomingCallState.MISSED:
			onMissedCall(number);
			break;

		case Consts.IncomingCallState.INCOMING:
			onIncomingCallInCallsDB(number);
			break;

		default:
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, 
					"unknown call state " + newState);
			return;
		}

		mPreviousCallState = newState;
	}

	void onOutgoingCall(String number) {
		if (number == null || number.trim().length() == 0) {
			return;
		}

		String callerIdNumber = PhoneNumberUtils.toCallerIDMinMatch(number);
		removeIncomingCallInfo(callerIdNumber);
	}
	
	void onWakeupTimeChanged(long oldWakeupTimeMillis,
			long newWakeupTimeMillis) {
		// Currently we only handle the case where the user changes the wakeup
		// time from infinite to non-infinite. In this case we remove all the
		// entries in the incoming calls map because otherwise they will never
		// expire
		// TODO a better behavior might be to set a timer for them when this 
		// happens. Also maybe change existing entries and shorten/prolong their
		// wakeup time according to the new setting by remembering what the 
		// original time we inserted them into the map
		if (newWakeupTimeMillis != Consts.INFINITE_TIME &&
			oldWakeupTimeMillis == Consts.INFINITE_TIME) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"wakeup timeout changed from infinite to " +
					newWakeupTimeMillis + ". Removing all numbers");
			synchronized (mIncomingCalls) {
				Iterator<String> it = mIncomingCalls.keySet().iterator();
				while (it.hasNext()) {
					// Each key is a caller ID number
					String callerIdNumber = it.next();
					if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
							"removing caller ID number " + callerIdNumber);
					removeIncomingCallInfo(callerIdNumber);
				}
			}			
		}
	}
	
/*
	void onRingerModeChanged(int newRingerMode) {
		if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
			"onRingerModeChanged: new ringer mode " + newRingerMode);
		if (mNumSelfRingerVibrateChanges > 0) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
				"onRingerModeChanged: self change. Not updating");
			mNumSelfRingerVibrateChanges -= 1;
			return;
		}
		
		mSettings.mUserRingerMode = newRingerMode;
	}
	
	void onVibrateSettingChanged(int vibrateType, int newVibrateSetting) {
		if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
			"onVibrateSettingChanged: vibrate type " + vibrateType + 
				" vibrateSetting " + newVibrateSetting);
		if (mNumSelfRingerVibrateChanges > 0) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
				"onVibrateSettingChanged: self change. Not updating");
			mNumSelfRingerVibrateChanges -= 1;
			return;
		}
		
		if (vibrateType == AudioManager.VIBRATE_TYPE_NOTIFICATION) {
			mSettings.mUserVibrateModeNotification = newVibrateSetting;
		} else if (vibrateType == AudioManager.VIBRATE_TYPE_RINGER) {
			mSettings.mUserVibrateModeRinger = newVibrateSetting;
		}
	}
*/	
	private void onRinging(String number) {
		mWakeLock.acquire();
		if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
				"onRinging: WakeLock acquired, " + mWakeLock.toString());
		
		NumberProperties numberProps = new NumberProperties();
		if (number == null || number.trim().length() == 0) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"onRinging: the number is empty, treating as unknown");
		}
		else
		{
		String callerIdNumber = PhoneNumberUtils.toCallerIDMinMatch(number);
		if (callerIdNumber == null || callerIdNumber.length() <= 0) {
			return;
		}

		// Do we care about this call:
		// Is it a returning number? Should we treat as second call?
		synchronized (mIncomingCalls) {
			IncomingCallInfo currentCallInfo =
				mIncomingCalls.get(callerIdNumber); 
			if (currentCallInfo != null) {
				// If the number exists in the map there is still the 
				// possibility that it hasn't been removed by the timer thread
				// even if its time was up because the phone was sleeping. So
				// we do this cleanup here, but only if the call info structure
				// wasn't put in the map with an infinite timeout value
				long currentTimeMillis = System.currentTimeMillis();
				if (currentCallInfo.getAbsoluteWakeupTimeMillis() != Consts.INFINITE_TIME &&
					currentTimeMillis > currentCallInfo.getAbsoluteWakeupTimeMillis()) {
					if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
							callerIdNumber + " will be removed in onRinging() because it wasn't removed by the timer thread" + 
							"currentTimeMillis: " + currentTimeMillis + ", absoluteWakeupTime: " +
							currentCallInfo.getAbsoluteWakeupTimeMillis());
					removeIncomingCallInfo(callerIdNumber);
				} else {
					if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
							"setting second ring parameters for callerId number " +
							callerIdNumber);
					setRingerAndVibrateModes(mSettings.mSecondRingSettings);
					return;
				}
			}
		}
		// It's not a number that called us. Let's see if it's a number that 
		// we're supposed to remember. If it is, we use the first ring settings.
		// If it's not, we use whatever the user asked us to in this case
		numberProps = getNumberProperties(callerIdNumber);
		if (numberProps.mShouldRememberNumber) {
			// Quiet the phone down according to user's setting
			setRingerAndVibrateModes(mSettings.mFirstRingSettings);
			return;
		}
		}
		
		final String setting = numberProps.mIsKnown ?
				mSettings.mTreatNonMobileCallers :
				mSettings.mTreatUnknownCallers;
		
		if (Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_FIRST.equals(setting)) {
			setRingerAndVibrateModes(mSettings.mFirstRingSettings);
			return;			
		}
		
		if (Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_SECOND.equals(setting)) {
			setRingerAndVibrateModes(mSettings.mSecondRingSettings);
			return;
		}
		
		if (Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_NORMAL.equals(setting)) {
			setRingerAndVibrateModes(mSettings.mUserSettings);
			return;
		}
		
		if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
			"onRinging() - code should never get here");

		// Should never get here, but just in case - let's behave
		setRingerAndVibrateModes(mSettings.mFirstRingSettings);		
	}

	private void onOffHook(String number) {
		// There is another case where we go off hook: from idle. But in that 
		// case we don't release the lock since we never acquired it
		if (mPreviousCallState == Consts.IncomingCallState.RINGING) {
			releaseWakeLockIfHeld("onOffHook");
		}
		
		if (number != null && number.trim().length() > 0) {
			// User either answered the call or called this number so remove
			// the call info
			removeIncomingCallInfo(
					PhoneNumberUtils.toCallerIDMinMatch(number));
			return;
		}

		// We didn't get a number from the notification, so if the latest
		// call state was ringing let's use the number we saved
		if (!mLastRingingNumber.equals(NOT_A_PHONE_NUMBER) &&
			mPreviousCallState == Consts.IncomingCallState.RINGING) {
			removeIncomingCallInfo(mLastRingingNumber);
			mLastRingingNumber = NOT_A_PHONE_NUMBER;
			return;
		}
	}

	private void onIdle(String number) {
		// Sometimes we get onIdle notification when the application loads. We
		// make sure here that whatever the latest ringing number we remember
		// is reset in case we didn't come from the expected state
		if (mPreviousCallState != Consts.IncomingCallState.RINGING) {
			mLastRingingNumber = NOT_A_PHONE_NUMBER;
		}

		// Return the vibrate/ringer settings to the way they should be while
		// the service is running
		setRingerAndVibrateModes(mSettings.mFirstRingSettings);
		
		// NOTE: we don't do anything about the WakeLock here because we don't 
		// know why we're idle - in most cases it will be because the user 
		// didn't answer and we're about to get a missed call notification.
		// But if the user declined the call we also get here without getting 
		// the missed call notification (see "Sequences for incoming calls" at
		// the top of this source file)
	}

	private void onMissedCall(String number) {
		// The wakelock was acquired when the phone was ringing, so unless we 
		// send the SMS we _must_ release it on each return from the function
		// because this is our last chance to do so
		if (number == null || number.trim().length() <= 0) {
			releaseWakeLockIfHeld("onMissedCall 1");
			return;
		}
		String callerIdNumber = PhoneNumberUtils.toCallerIDMinMatch(number);
		if (callerIdNumber == null || callerIdNumber.length() <= 0) {
			releaseWakeLockIfHeld("onMissedCall 2");
			return;
		}
		if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, 
				"onMissedCall: number " + number + ", callerIdNumber " +
				callerIdNumber);
		// if the number that's missed was the last one we remember as ringing
		// and we came from an idle state and a new missed call with the same
		// number appeared in the call log - it's safe to say that we found a
		// missed call and we should probably send the SMS
		if (mPreviousCallState != Consts.IncomingCallState.IDLE ||
			!callerIdNumber.equals(mLastRingingNumber)) {
			releaseWakeLockIfHeld("onMissedCall 3");
			return;
		}

		synchronized (mIncomingCalls) {
			if (mIncomingCalls.containsKey(callerIdNumber)) {
				if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
						"onMissedCall: callerIdNumber " + callerIdNumber +
						" for number " + number + " already exists");
				// TODO in this case should we extend the time during which we
				// wait for the next call?
				releaseWakeLockIfHeld("onMissedCall 4");
				return;
			}
		}

		// A new missed call that we haven't saved yet
		// Is it a number we need to remember?
		NumberProperties numberProps = getNumberProperties(callerIdNumber);
		if (!numberProps.mShouldRememberNumber) {
			releaseWakeLockIfHeld("onMissedCall 5");
			return;
		}

		RemoveCallInfoTask removalTask = null;
		if (mSettings.mWakeupTimeoutMillis != Consts.INFINITE_TIME) {
			removalTask = new RemoveCallInfoTask(callerIdNumber);
		}
		
		IncomingCallInfo newCallInfo = new IncomingCallInfo(number,
				callerIdNumber, removalTask,
				mSettings.mWakeupTimeoutMillis == Consts.INFINITE_TIME ?
					Consts.INFINITE_TIME :
					System.currentTimeMillis() + mSettings.mWakeupTimeoutMillis);
		// Add to list of numbers to remember
		synchronized (mIncomingCalls) {
			mIncomingCalls.put(callerIdNumber, newCallInfo);
		}
		// Schedule removal from list, but only if we're asked to use timeouts
		if (mSettings.mWakeupTimeoutMillis != Consts.INFINITE_TIME) {
			mTimer.schedule(removalTask, mSettings.mWakeupTimeoutMillis);
		}

		if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, "number " +
				newCallInfo.getNumber() + ", callerIdNumber " +
				newCallInfo.getCallerIdNumber() +
				" has been added. Timeout is " +
				mSettings.mWakeupTimeoutMillis);

		if (mSettings.mEnableSms) {
			// The intent will release the WakeLock
			mSmsSender.sendMessage(number, mSettings.mMessage,
					mSentPendingIntent);
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"number " + number + " has been notified with SMS.");
		} else {
			releaseWakeLockIfHeld("onMissedCall 6");
		}
	}

	private void onIncomingCallInCallsDB(String number) {
		// Because of the sequences for incoming calls that were either
		// answered or declined we can't tell the difference between the two, so
		// we know we should unlock only according to the isHeld() status of
		// the lock 
		releaseWakeLockIfHeld("onIncomingCallInCallsDB");
	}

	private class NumberProperties
	{
		boolean mIsKnown = false;
		boolean mShouldRememberNumber = false;
	}
	
	private NumberProperties getNumberProperties(String callerIdNumber) {
		NumberProperties props = new NumberProperties();
		if (callerIdNumber == null || callerIdNumber.length() <= 0) {
			return props;
		}
		// Sample selection to select the rightmost MIN_MATCH (5) characters 
		// to filter the incoming call:
		// SUBSTR(number_key,1,5) = '54321' AND type = 2
		String query = new StringBuilder("SUBSTR(").append(Phones.NUMBER_KEY)
		.append(",1,").append(callerIdNumber.length())
		.append(") = '").append(callerIdNumber).append("'").toString();
		//.append("' AND ")
		//.append(PhonesColumns.TYPE).append(" = ")
		//.append(PhonesColumns.TYPE_MOBILE).toString();
		Cursor phonesCursor = mResolver.query(Phones.CONTENT_URI, 
				PHONES_PROJECTION, 
				query,
				null, null);
		int count = phonesCursor.getCount();
		props.mIsKnown = count >= 1;
		while (phonesCursor.moveToNext()) {
			if (phonesCursor.getInt(PHONE_TYPE_INDEX) == PhonesColumns.TYPE_MOBILE) {
				props.mShouldRememberNumber = true;
				break;
			}
		}
		phonesCursor.deactivate();
		phonesCursor.close();
		phonesCursor = null;

		if (props.mIsKnown) {
			if (props.mShouldRememberNumber) {
				if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
						callerIdNumber + " is a known mobile callerId number");
			} else {
				if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, 
						"known non-mobile callerId number " + callerIdNumber);
			}
		} else {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, 
					"unknown callerId number " + callerIdNumber);
		}

		return props;
	}

	private void setRingerAndVibrateModes(DindySettings.RingerVibrateSettings
			settings) {
		if (mAM.getRingerMode() != settings.mRingerMode) { 
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"setting ringer=" +
					Utils.ringerModeToString(settings.mRingerMode));
			mAM.setRingerMode(settings.mRingerMode);
			//++mNumSelfRingerVibrateChanges;
		}
		if (mAM.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER) != 
			settings.mVibrateModeRinger) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"setting vibrateRinger=" +
					Utils.vibrationSettingToString(settings.mVibrateModeRinger));
			mAM.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,
					settings.mVibrateModeRinger);
			//++mNumSelfRingerVibrateChanges;
		}
		if (mAM.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION) !=
			settings.mVibrateModeNotification) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"setting vibrateNotification=" +
					Utils.vibrationSettingToString(settings.mVibrateModeNotification));
			mAM.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,
					settings.mVibrateModeNotification);
			//++mNumSelfRingerVibrateChanges;
		}
	}

	private void removeIncomingCallInfo(String callerIdNumber) {
		if (callerIdNumber == null || callerIdNumber.length() <= 0) {
			return;
		}
		IncomingCallInfo removedInfo = null;
		synchronized (mIncomingCalls) {
			removedInfo = mIncomingCalls.remove(callerIdNumber);
			if (removedInfo != null) {
				RemoveCallInfoTask removalTask = 
					removedInfo.getAssociatedRemovalTask();
				// The removal task can be null if the timeout is INFINITE_TIME
				if (removalTask != null) {
					removalTask.cancel();
					mTimer.purge();
				}
			}
		}
		if (removedInfo != null) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"removeIncomingCallInfo: canceled for caller ID "
					+ callerIdNumber);
		} else {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"removeIncomingCallInfo: call info for caller ID "
					+ callerIdNumber + " not found");
		}
	}

	private void releaseWakeLockIfHeld(String context) {
		while (mWakeLock.isHeld()) {
			mWakeLock.release();
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, 
					context + ": WakeLock released, " + mWakeLock.toString());
		}		
	}
	
	private class IncomingCallInfo {
		public IncomingCallInfo(String number,
				String callerIdNumber,
				RemoveCallInfoTask associatedRemovalTask,
				long absoluteWakeupTimeMillis) {
			mNumber = number;
			mCallerIdNumber = callerIdNumber;
			mAssociatedRemovalTask = associatedRemovalTask;
			mAbsoluteWakeupTimeMillis = absoluteWakeupTimeMillis;
		}

		public String getNumber() {
			return mNumber;
		}

		public String getCallerIdNumber() {
			return mCallerIdNumber;
		}
		
		public RemoveCallInfoTask getAssociatedRemovalTask() {
			return mAssociatedRemovalTask;
		}

		public long getAbsoluteWakeupTimeMillis() {
			return mAbsoluteWakeupTimeMillis;
		}
		
		private String mNumber;
		private String mCallerIdNumber;
		// private boolean mHasBeenNotifiedWithSMS = false;
		private RemoveCallInfoTask mAssociatedRemovalTask;
		private long mAbsoluteWakeupTimeMillis;
	}

	private class RemoveCallInfoTask extends TimerTask {
		RemoveCallInfoTask(String numberToRemove) {
			mNumberToRemove = numberToRemove;
		}

		@Override
		public void run() {
			removeIncomingCallInfo(mNumberToRemove);
			cancel();
			mTimer.purge();
		}

		private String mNumberToRemove;
	}

	private static final String NOT_A_PHONE_NUMBER = "NOT_A_PHONE_NUMBER";
	private static final String SMS_PENDING_INTENT_NAME = "DindySMS";
	private String mLastRingingNumber = NOT_A_PHONE_NUMBER;
	private boolean mStarted = false;
	private DindySettings mSettings = null;
	private SmsSender mSmsSender = null;
	private Timer mTimer = new Timer();
	private HashMap<String, IncomingCallInfo> mIncomingCalls =
		new HashMap<String, IncomingCallInfo>();
	private int mPreviousCallState = Consts.IncomingCallState.IDLE;
	private Context mContext;
	private AudioManager mAM = null;
	private PowerManager mPM = null;
	private PowerManager.WakeLock mWakeLock = null;
	private static final String[] PHONES_PROJECTION = { Phones.TYPE };
	private static final int PHONE_TYPE_INDEX = 0; 
	private ContentResolver mResolver = null;
	private BroadcastReceiver mOnSentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO use the result code to learn whether the SMS sending
			// succeeded
			// switch (getResultCode())
			if (intent.getAction().equals(SMS_PENDING_INTENT_NAME)) {
				releaseWakeLockIfHeld("onReceive (SMS sent)");
			}
		}
	};
	private PendingIntent mSentPendingIntent = null;
}
