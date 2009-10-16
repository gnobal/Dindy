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
//import android.telephony.gsm.SmsManager;
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

// Sequences for incoming calls:
// 1. Missed call
//    ringing (lock) -> idle -> missed -> SMS -> SMS confirmation (unlock)
// 2. Incoming call that was declined
//    ringing (lock) -> idle -> incoming (unlock)
// 3. Incoming call that was answered
//    ringing (lock) -> off hook (unlock) -> idle -> incoming (do not unlock)
// Sequences for outgoing calls:
// 1. Outgoing call
//    idle -> off hook (do not unlock) 

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
	}

	void start() {
		setRingerAndVibrateModes(mSettings.mFirstRingSettings);
	}
	
	void stop() {
		setRingerAndVibrateModes(mSettings.mUserSettings);
	}
	
	void destroy() {
		mContext.unregisterReceiver(mOnSentReceiver);
		mOnSentReceiver = null;
		mContext = null;
		mTimer.cancel();
		synchronized (mIncomingCalls) {
			mIncomingCalls.clear();
		}
		if (mWakeLock.isHeld()) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, 
				"destroy: releasing WakeLock");
			mWakeLock.release();
		}
		mWakeLock = null;
		mPM = null;
		mAM = null;
		mResolver = null;
		mSettings = null;
	}

	void onCallStateChange(int newState, String number) {
		if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, "newState: " + 
				newState + ", number: " + number);
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
			onIncomingCall(number);
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
		if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
				"onRinging: acquiring WakeLock");
		mWakeLock.acquire();

		if (number == null || number.trim().length() == 0) {
			// Got an empty number. Nothing to do about that
			return;
		}
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
				if (currentCallInfo.getAbsoluteWakeupTimeMillis() != Consts.INFINITE_TIME &&
					currentCallInfo.getAbsoluteWakeupTimeMillis() > System.currentTimeMillis()) {
					if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
							callerIdNumber + " will be removed in onRinging() because it wasn't removed by the timer thread");
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
		if (shouldRememberNumber(callerIdNumber)) {
			// Quiet the phone down according to user's setting
			setRingerAndVibrateModes(mSettings.mFirstRingSettings);
			return;
		}
		
		if (mSettings.mTreatUnknownCallers.equals(
			Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_FIRST)) {
			setRingerAndVibrateModes(mSettings.mFirstRingSettings);
			return;			
		}
		
		if (mSettings.mTreatUnknownCallers.equals(
			Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_SECOND)) {
			setRingerAndVibrateModes(mSettings.mSecondRingSettings);
			return;
		}
		
		if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
				"onRinging() - code should never get here");

		if (mSettings.mTreatUnknownCallers.equals(
			Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_NORMAL)) {
			setRingerAndVibrateModes(mSettings.mUserSettings);
			return;
		}
		
		// Should never get here, but just in case - let's behave
		setRingerAndVibrateModes(mSettings.mFirstRingSettings);		
	}

	private void onOffHook(String number) {
		// There is another case where we go off hook: from idle. But in that 
		// case we don't release the lock since we never acquired it
		if (mPreviousCallState == Consts.IncomingCallState.RINGING) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, 
				"onOffHook: releasing WakeLock");
			mWakeLock.release();
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
		// the missed call notification, so for this case we acquired the lock 
		// with a 2 minutes timeout 
		// TODO fix this (for example by knowing when the user declined from 
		// the call log) 
	}

	private void onMissedCall(String number) {
		if (number == null || number.trim().length() <= 0) {
			return;
		}
		String callerIdNumber = PhoneNumberUtils.toCallerIDMinMatch(number);
		if (callerIdNumber == null || callerIdNumber.length() <= 0) {
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
			return;
		}

		synchronized (mIncomingCalls) {
			if (mIncomingCalls.containsKey(callerIdNumber)) {
				if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
						"onMissedCall: callerIdNumber " + callerIdNumber +
						" for number " + number + " already exists");
				// TODO in this case should we extend the time during which we
				// wait for the next call?
				return;
			}
		}

		// A new missed call that we haven't saved yet
		// Is it a number we need to remember?
		if (!shouldRememberNumber(callerIdNumber)) {
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
		/*
		 * // The missed call was found - it's someone who either we've notified
		 * // with an SMS before or we should notify if
		 * (incomingCallInfo.getHasBeenNotifiedWithSMS()) {
		 * Utils.LogD("onMissedCall: number \"" + number +
		 * "\" has already been notified"); return; }
		 */
		if (mSettings.mEnableSms) {
			/*
			SmsManager smsManager = SmsManager.getDefault();
			if (smsManager != null) {
				// TODO use the intent parameter to learn whether the SMS
				// sending succeeded
				smsManager.sendTextMessage(number, null, mSettings.mMessage,
						null, null);
				if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
						"number " + number + " has been notified with SMS.");
			}
			*/
		    
			
			SmsSender.getInstance().sendMessage(number, mSettings.mMessage,
					mSentPendingIntent);
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"number " + number + " has been notified with SMS.");
			// TODO should move to where we get a positive confirmation on
			// the SMS sending
			// newCallInfo.setHasBeenNotifiedWithSMS();
		}
	}

	private void onIncomingCall(String number) {
		// Because of the sequences for incoming calls that were either
		// answered or declined we can't tell the difference between the two, so
		// we know we should unlock only according to the isHeld() status of
		// the lock 
		if (mWakeLock.isHeld()) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, 
					"onIncomingCall: releasing WakeLock");
			mWakeLock.release();
		}
	}

	private boolean shouldRememberNumber(String callerIdNumber) {
		if (callerIdNumber == null || callerIdNumber.length() <= 0) {
			return false;
		}
		// Sample selection to select the rightmost MIN_MATCH (5) characters 
		// to filter the incoming call:
		// SUBSTR(number_key,1,5) = '54321' AND type = 2
		String query = new StringBuilder("SUBSTR(").append(Phones.NUMBER_KEY)
		.append(",1,").append(callerIdNumber.length())
		.append(") = '").append(callerIdNumber).append("' AND ")
		.append(PhonesColumns.TYPE).append(" = ")
		.append(PhonesColumns.TYPE_MOBILE).toString();
		Cursor phonesCursor = mResolver.query(Phones.CONTENT_URI, 
				PHONES_PROJECTION, 
				query,
				null, null);
		int count = phonesCursor.getCount();
		phonesCursor.deactivate();
		//if (Config.LOGD && Consts.DEBUG)  Log.d(Consts.LOGTAG, 
		//"query=" + query + ", count=" + count);
		phonesCursor.close();
		phonesCursor = null;
		if (count < 1) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, 
					"unknown callerId number " + callerIdNumber);
			return false;
		}
		
		if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, callerIdNumber +
				" is a known callerId number");
		return true;
	}

	private void setRingerAndVibrateModes(DindySettings.RingerVibrateSettings
			settings) {
		if (mAM.getRingerMode() != settings.mRingerMode) { 
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"setting ringer=" + settings.mRingerMode);
			mAM.setRingerMode(settings.mRingerMode);
			//++mNumSelfRingerVibrateChanges;
		}
		if (mAM.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER) != 
			settings.mVibrateModeRinger) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"setting vibrateRinger=" + settings.mVibrateModeRinger);
			mAM.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,
					settings.mVibrateModeRinger);
			//++mNumSelfRingerVibrateChanges;
		}
		if (mAM.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION) !=
			settings.mVibrateModeNotification) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"setting vibrateNotification=" +
					settings.mVibrateModeNotification);
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
		
		/*
		 * public void setHasBeenNotifiedWithSMS() { mHasBeenNotifiedWithSMS =
		 * true; }
		 * 
		 * public boolean getHasBeenNotifiedWithSMS() { return
		 * mHasBeenNotifiedWithSMS; }
		 */
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
	private DindySettings mSettings = null;
	private Timer mTimer = new Timer();
	private HashMap<String, IncomingCallInfo> mIncomingCalls =
		new HashMap<String, IncomingCallInfo>();
	private int mPreviousCallState = Consts.IncomingCallState.IDLE;
	private Context mContext;
	private AudioManager mAM = null;
	private PowerManager mPM = null;
	private PowerManager.WakeLock mWakeLock = null;
	private String[] PHONES_PROJECTION =
	{
		Phones.TYPE
	};
	private ContentResolver mResolver = null;
	private BroadcastReceiver mOnSentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// switch (getResultCode())
			if (intent.getAction().equals(SMS_PENDING_INTENT_NAME)) {
				if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
						"onReceive: releasing WakeLock, SMS sent intent arrived");
				mWakeLock.release();
			}
		}
	};
	private PendingIntent mSentPendingIntent = null;
}
