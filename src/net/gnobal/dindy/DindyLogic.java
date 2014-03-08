package net.gnobal.dindy;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
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
//    ringing (lock) +-> idle
//                   |
//                   +-> missed (should send) -> SMS -> SMS result (unlock)
// 2. Incoming call that was declined
//    ringing (lock) +-> idle
//                   |
//                   +-> incoming (unlock)
// 3. Incoming call that was answered
//    ringing (lock) -> off hook (unlock) +-> idle
//                                        |
//                                        +-> incoming (do not unlock)
// 4. Missed call that shouldn't get an SMS
//    ringing (lock) +-> idle
//                   |
//                   +-> missed (shouldn't send) -> unlock
// Sequences for outgoing calls:
// 5. Outgoing call
//    idle -> off hook (do not unlock)
// 
// The biggest problem we have here is whether to unlock when we get an incoming
// call in the calls DB - we don't know whether we got there from sequence 2 
// (so we should unlock) or 3 (should _not_ unlock)

class DindyLogic {
	public DindyLogic(Context context, ContentResolver resolver, DindySettings settings,
		AudioManager am, PowerManager pm) {
		mContext = context;
		mIncomingCalls = new IncomingCalls(context);
		mResolver = resolver;
		mSettings = settings;
		mAM = am;
		mPM = pm;
		mWakeLock = mPM.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Dindy");
	    mSentPendingIntent = PendingIntent.getBroadcast(
	    		mContext, 0, new Intent(SMS_PENDING_INTENT_NAME), 0);
	    mContext.registerReceiver(mOnSentReceiver, new IntentFilter(
	    		SMS_PENDING_INTENT_NAME));
	    mSM = SmsManager.getDefault();
	}

	void start(boolean rebuildIncomingCalls) {
		if (Consts.DEBUG) {
			Log.d(Consts.LOGTAG, "start: rebuildIncomingCalls=" + rebuildIncomingCalls);
		}
		if (rebuildIncomingCalls) {
			synchronized (mIncomingCalls) {
				mIncomingCalls.rebuild();
				final long currentTimeMillis = System.currentTimeMillis();
				Iterator<String> it = mIncomingCalls.callerIdNumbersIterator(); 
				while (it.hasNext()) {
					final String callerIdNumber = it.next();
					if (Consts.DEBUG) {
						Log.d(Consts.LOGTAG, "start: rebuilding caller ID number " +
							callerIdNumber);
					}
					final IncomingCallInfo callInfo = mIncomingCalls.get(callerIdNumber);
					if (callInfo.getAbsoluteWakeupTimeMillis() == Consts.NOT_A_TIME_LIMIT) {
						continue;
					}
					if (callInfo.getAbsoluteWakeupTimeMillis() < currentTimeMillis) {
						// Removal was in the past, so remove it now
						removeIncomingCallInfo(callerIdNumber);
						continue;
					}
					
					final long wakeupTimeMillis =
						callInfo.getAbsoluteWakeupTimeMillis() - currentTimeMillis;
					final RemoveCallInfoTask removalTask = new RemoveCallInfoTask(callerIdNumber);
					callInfo.associateRemovalTask(removalTask);
					mTimer.schedule(removalTask, wakeupTimeMillis);
				}
			}			
		} else {
			// Just to make sure we're starting fresh
			mIncomingCalls.clear();
		}
		setRingerAndVibrateModes(mSettings.mFirstRingSettings);
		mStarted = true;
	}
	
	void stop() {
		if (!mStarted) {
			return;
		}
		mIncomingCalls.clear();
		setRingerAndVibrateModes(mSettings.mUserSettings);
		mStarted = false;
	}
	
	void destroy() {
		mContext.unregisterReceiver(mOnSentReceiver);
		mSM = null;
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
		if (Consts.DEBUG) {
			Log.d(Consts.LOGTAG, "onCallStateChange: newState=" +
				Utils.incomingCallStateToString(newState) +	" number=" + number);
		}
		if (!mStarted) {
			// can happen if the service is started with a non-existing profile
			// and then stops itself
			if (Consts.DEBUG) {
				Log.d(Consts.LOGTAG,"onCallStateChange: called without being started");
			}
			return;
		}
		switch (newState) {
		case Consts.IncomingCallState.IDLE:
			onIdle(number);
			break;

		case Consts.IncomingCallState.RINGING:
			onRinging(number);
			mLastRingingNumber = getCallerIdNumber(number);
			if (mLastRingingNumber == null) {
				mLastRingingNumber = NOT_A_PHONE_NUMBER;
			}
			break;

		case Consts.IncomingCallState.OFFHOOK:
			onOffHook(number);
			break;

		default:
			if (Consts.DEBUG) {
				Log.d(Consts.LOGTAG, "onCallStateChange: unknown call state " + newState);
			}
			return;
		}

		mPreviousCallState = newState;
	}

	void onSmsMessage(String number) {
		mWakeLock.acquire();
		String callerIdNumber = getCallerIdNumber(number);
		if (callerIdNumber == null) {
			if (Consts.DEBUG) {
				Log.d(Consts.LOGTAG, "onSmsMessage: number " + number + " returned null callerId");
			}
			releaseWakeLockIfHeld("onSmsMessage");
			return;
		}
		
		if (isCallerIdNumberExist(callerIdNumber, "onSmsMessage")) {
			releaseWakeLockIfHeld("onSmsMessage");
			return;
		}

		NumberProperties numberProps = getNumberProperties(number);
		final boolean shouldRemember = (numberProps.mIsKnown ||
			Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_TEXTERS_AS_MOBILE_NO_SMS.equals(mSettings.mTreatUnknownTexters));
		if (!shouldRemember) {
			if (Consts.DEBUG) {
				Log.d(Consts.LOGTAG, "onSmsMessage: number " + number +
					" is not a number to remember. isMobile=" + numberProps.mIsMobile +
					" isKnown=" + numberProps.mIsKnown);
			}
			releaseWakeLockIfHeld("onSmsMessage");
			return;
		}

		addNumberAndScheduleRemoval("onSmsMessage", number, callerIdNumber);
		
		final boolean shouldSendSms =
			(mSettings.mEnableSmsReplyToSms && numberProps.mIsKnown);
		if (shouldSendSms) {
			sendTextMessage("onSmsMessage", number, mSettings.mMessageToTexters);
		} else {
			if (Consts.DEBUG) Log.d(Consts.LOGTAG,
					"onSmsMessage: number " + number +
					" need not be notified with SMS.");
			releaseWakeLockIfHeld("onSmsMessage");
		}
	}

	private void sendTextMessage(final String context, final String number, final String message) {
		if (Consts.DEBUG) Log.d(Consts.LOGTAG,
				context + ": number " + number + " has been notified with SMS.");
		final ArrayList<String> dividedMessage = mSM.divideMessage(message);
		final ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>(
			Collections.nCopies(dividedMessage.size(), mSentPendingIntent));
		// The intents will release the WakeLock
		mSM.sendMultipartTextMessage(number, null, dividedMessage, sentPendingIntents, null);
	}
	
	void onMissedCall(String number) {
		// The wakelock was acquired when the phone was ringing, so unless we 
		// send the SMS we _must_ release it on each return from the function
		// because this is our last chance to do so
		String callerIdNumber = getCallerIdNumber(number);
		if (callerIdNumber == null) {
			if (Consts.DEBUG) Log.d(Consts.LOGTAG,
				"onMissedCall: number " + number + " returned null callerId");
			releaseWakeLockIfHeld("onMissedCall");
			return;
		}
		if (Consts.DEBUG) Log.d(Consts.LOGTAG, 
			"onMissedCall: number " + number + ", callerIdNumber " + callerIdNumber);
		// if the number that's missed was the last one we remember as ringing
		// and a new missed call with the same number appeared in the call log -
		// it's safe to say that we found a missed call and we should probably 
		// send the SMS
		if (!callerIdNumber.equals(mLastRingingNumber)) {
			if (Consts.DEBUG) Log.d(Consts.LOGTAG,
				"onMissedCall: callerIdNumber " + callerIdNumber +
				" does not match last ringing number " + mLastRingingNumber);
			releaseWakeLockIfHeld("onMissedCall");
			return;
		}

		if (isCallerIdNumberExist(callerIdNumber, "onMissedCall")) {
			releaseWakeLockIfHeld("onMissedCall");
			return;
		}

		// A new missed call that we haven't saved yet
		// Is it a number we need to remember?
		NumberProperties numberProps = getNumberProperties(number);
		final boolean shouldRemember = numberProps.mIsMobile ||
			(numberProps.mIsKnown && Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_MOBILE_NO_SMS.equals(mSettings.mTreatNonMobileCallers)) ||
			(!numberProps.mIsKnown && Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_MOBILE_NO_SMS.equals(mSettings.mTreatUnknownCallers));
		if (!shouldRemember) {
			if (Consts.DEBUG) Log.d(Consts.LOGTAG,
				"onMissedCall: number " + number + " is not a number to remember. isMobile=" +
				numberProps.mIsMobile + " isKnown=" + numberProps.mIsKnown + " nonMobileCallers=" +
				mSettings.mTreatNonMobileCallers + " unknownCallers=" +
				mSettings.mTreatUnknownCallers);
			releaseWakeLockIfHeld("onMissedCall");
			return;
		}

		addNumberAndScheduleRemoval("onMissedCall", number, callerIdNumber);

		// Note that we may have gotten here because the user chose to treat the
		// number as mobile, but we should only send the text message if it's
		// indeed a mobile number
		final boolean shouldSendSms =
			(mSettings.mEnableSmsReplyToCall && numberProps.mIsMobile); 
		if (shouldSendSms) {
			sendTextMessage("onMissedCall", number, mSettings.mMessageToCallers);
		} else {
			if (Consts.DEBUG) Log.d(Consts.LOGTAG,
				"onMissedCall: number " + number + " need not be notified with SMS.");
			releaseWakeLockIfHeld("onMissedCall");
		}
	}

	// an incoming call - an indication of a call that was either accepted or
	// declined, but not missed (for this we have onMissedCall())
	void onIncomingCallInCallsDB(String number) {
		// Because of the sequences for incoming calls that were either
		// answered or declined we can't tell the difference between the two, so
		// we know we should unlock only according to the isHeld() status of
		// the lock 
		releaseWakeLockIfHeld("onIncomingCallInCallsDB");
	}

	void onOutgoingCall(String number) {
		if (number == null || number.trim().length() == 0) {
			return;
		}

		// Unknown numbers don't apply here because you can't call an unknown 
		// number
		String callerIdNumber = getCallerIdNumber(number);
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
			if (Consts.DEBUG) Log.d(Consts.LOGTAG,
					"onWakeupTimeChanged: wakeup timeout changed from infinite "
					+ "to " + newWakeupTimeMillis + ". Removing all numbers");
			synchronized (mIncomingCalls) {
				Iterator<String> it = mIncomingCalls.callerIdNumbersIterator(); 
					//mIncomingCalls.keySet().iterator();
				while (it.hasNext()) {
					// Each key is a caller ID number
					String callerIdNumber = it.next();
					if (Consts.DEBUG) Log.d(Consts.LOGTAG,
							"onWakeupTimeChanged: removing caller ID number " +
							callerIdNumber);
					removeIncomingCallInfo(callerIdNumber);
				}
			}			
		}
	}
	
	private boolean isCallerIdNumberExist(String callerIdNumber, String context) {
		synchronized (mIncomingCalls) {
			//if (mIncomingCalls.containsKey(callerIdNumber)) {
			if (mIncomingCalls.numberExists(callerIdNumber)) {
				if (Consts.DEBUG) Log.d(Consts.LOGTAG,
						context + ": callerIdNumber " + callerIdNumber +
						" already exists");
				// TODO in this case should we extend the time during which we
				// wait for the next call?
				return true;
			}
		}
		return false;
	}

	private void addNumberAndScheduleRemoval(String context,
			String number, String callerIdNumber) {
		RemoveCallInfoTask removalTask = null;
		if (mSettings.mWakeupTimeoutMillis != Consts.INFINITE_TIME) {
			removalTask = new RemoveCallInfoTask(callerIdNumber);
		}
		
		final IncomingCallInfo newCallInfo = new IncomingCallInfo(number,
			callerIdNumber,
			mSettings.mWakeupTimeoutMillis == Consts.INFINITE_TIME ?
				Consts.INFINITE_TIME :
				System.currentTimeMillis() + mSettings.mWakeupTimeoutMillis);
		newCallInfo.associateRemovalTask(removalTask);
		// Add to list of numbers to remember
		synchronized (mIncomingCalls) {
			mIncomingCalls.put(callerIdNumber, newCallInfo);
		}
		// Schedule removal from list, but only if we're asked to use timeouts
		if (mSettings.mWakeupTimeoutMillis != Consts.INFINITE_TIME) {
			mTimer.schedule(removalTask, mSettings.mWakeupTimeoutMillis);
		}

		if (Consts.DEBUG) Log.d(Consts.LOGTAG,
				context + ": number " + newCallInfo.getNumber() +
				", callerIdNumber " + newCallInfo.getCallerIdNumber() +
				" has been added. Timeout is " +
				mSettings.mWakeupTimeoutMillis);
	}

	
	private void onRinging(String number) {
		mWakeLock.acquire();
		if (Consts.DEBUG) Log.d(Consts.LOGTAG,
				"onRinging: WakeLock acquired, " + mWakeLock.toString());
		
		String callerIdNumber = getCallerIdNumber(number);
		if (callerIdNumber == null) {
			if (Consts.DEBUG) Log.d(Consts.LOGTAG,
					"onRinging: callerIdNumber is null for number " + number);
			return;
		}

		// Do we care about this call:
		// Is it a returning number? Should we treat as second call?
		synchronized (mIncomingCalls) {
			IncomingCallInfo currentCallInfo = mIncomingCalls.get(callerIdNumber); 
			if (currentCallInfo != null) {
				// If the number exists in the map there is still the 
				// possibility that it hasn't been removed by the timer thread
				// even if its time was up because the phone was sleeping. So
				// we do this cleanup here, but only if the call info structure
				// wasn't put in the map with an infinite timeout value
				long currentTimeMillis = System.currentTimeMillis();
				if (currentCallInfo.getAbsoluteWakeupTimeMillis() != Consts.INFINITE_TIME &&
					currentTimeMillis > currentCallInfo.getAbsoluteWakeupTimeMillis()) {
					if (Consts.DEBUG) Log.d(Consts.LOGTAG,
							"onRinging: " + callerIdNumber + " will be removed "
							+ "because it wasn't removed by the timer thread " + 
							"currentTimeMillis= " + currentTimeMillis +
							" absoluteWakeupTime=" +
							currentCallInfo.getAbsoluteWakeupTimeMillis());
					removeIncomingCallInfo(callerIdNumber);
				} else {
					if (Consts.DEBUG) Log.d(Consts.LOGTAG,
							"onRinging: setting second ring parameters for " +
							"callerId number " + callerIdNumber);
					setRingerAndVibrateModes(mSettings.mSecondRingSettings);
					return;
				}
			}
		}
		// It's not a number that called us. Let's see if it's a number that 
		// we're supposed to remember. If it is, we use the first ring settings.
		// If it's not, we use whatever the user asked us to in this case
		NumberProperties numberProps = getNumberProperties(number);
		if (numberProps.mIsMobile) {
			// Quiet the phone down according to user's setting
			setRingerAndVibrateModes(mSettings.mFirstRingSettings);
			return;
		}
		
		final String setting = numberProps.mIsKnown ?
				mSettings.mTreatNonMobileCallers :
				mSettings.mTreatUnknownCallers;
		
		if (Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_FIRST.equals(setting) ||
			Consts.Prefs.Profile.VALUE_TREAT_UNKNOWN_CALLERS_AS_MOBILE_NO_SMS.equals(setting)) {
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
		
		if (Consts.DEBUG) Log.d(Consts.LOGTAG,
			"onRinging() - code should never get here");

		// Should never get here, but just in case - let's behave
		setRingerAndVibrateModes(mSettings.mFirstRingSettings);		
	}

	private void onOffHook(String number) {
		// There is another case where we go off hook: from idle. But in that 
		// case we don't release the lock since we never acquired it.
		// In the other case (when the user calls an outgoing number) we
		// expect onOutgoingCall to be called
		if (mPreviousCallState == Consts.IncomingCallState.RINGING) {
			releaseWakeLockIfHeld("onOffHook");

			// When we get here we typically don't get a number from the 
			// notification, so let's use the number we saved when the 
			// phone was ringing
			if (!NOT_A_PHONE_NUMBER.equals(mLastRingingNumber)) {
				removeIncomingCallInfo(mLastRingingNumber);
				mLastRingingNumber = NOT_A_PHONE_NUMBER;
			}
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
		// didn't answer and we're about to get (or we already got) a missed 
		// call notification.
		// But if the user declined the call we also get here without getting 
		// the missed call notification (see "Sequences for incoming calls" at
		// the top of this source file)
	}

	private class NumberProperties
	{
		boolean mIsKnown = false;
		boolean mIsMobile = false;
	}
	
	private NumberProperties getNumberProperties(String number) {
		NumberProperties props = new NumberProperties();
		if (number == null || number.length() <= 0) {
			return props;
		}

		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		Cursor phonesCursor = mResolver.query(uri, PHONES_PROJECTION, null, null, null);
		int count = phonesCursor.getCount();
		props.mIsKnown = count >= 1;
		while (phonesCursor.moveToNext()) {
			if (phonesCursor.getInt(PHONE_TYPE_INDEX) == CommonDataKinds.Phone.TYPE_MOBILE) {
				props.mIsMobile = true;
				break;
			}
		}
		phonesCursor.deactivate();
		phonesCursor.close();
		phonesCursor = null;

		if (props.mIsKnown) {
			if (props.mIsMobile) {
				if (Consts.DEBUG) Log.d(Consts.LOGTAG,
					"getNumberProperties: " + number + " is a known mobile number");
			} else {
				if (Consts.DEBUG) Log.d(Consts.LOGTAG, 
					"getNumberProperties: known non-mobile number " + number);
			}
		} else {
			if (Consts.DEBUG) Log.d(Consts.LOGTAG, 
				"getNumberProperties: unknown number " + number);
		}

		return props;
	}

	private void setRingerAndVibrateModes(DindySettings.RingerVibrateSettings
			settings) {
		if (mAM.getRingerMode() != settings.mRingerMode) { 
			if (Consts.DEBUG) Log.d(Consts.LOGTAG,
					"setRingerAndVibrateModes: setting ringer=" +
					Utils.ringerModeToString(settings.mRingerMode));
			mAM.setRingerMode(settings.mRingerMode);
		}
		if (mAM.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER) != 
			settings.mVibrateModeRinger) {
			if (Consts.DEBUG) Log.d(Consts.LOGTAG,
					"setRingerAndVibrateModes: setting vibrateRinger=" +
					Utils.vibrationSettingToString(settings.mVibrateModeRinger));
			mAM.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,
					settings.mVibrateModeRinger);
		}
		if (mAM.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION) !=
			settings.mVibrateModeNotification) {
			if (Consts.DEBUG) Log.d(Consts.LOGTAG,
					"setRingerAndVibrateModes: setting vibrateNotification=" +
					Utils.vibrationSettingToString(settings.mVibrateModeNotification));
			mAM.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,
					settings.mVibrateModeNotification);
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
			if (Consts.DEBUG) Log.d(Consts.LOGTAG,
					"removeIncomingCallInfo: canceled for caller ID "
					+ callerIdNumber);
		} else {
			if (Consts.DEBUG) Log.d(Consts.LOGTAG,
					"removeIncomingCallInfo: call info for caller ID "
					+ callerIdNumber + " not found");
		}
	}

	private void releaseWakeLockIfHeld(String context) {
		while (mWakeLock.isHeld()) {
			mWakeLock.release();
			if (Consts.DEBUG) Log.d(Consts.LOGTAG, 
					context + ": WakeLock released, " + mWakeLock.toString());
		}		
	}
	
	private static String getCallerIdNumber(String number) {
		String callerIdNumber = UNKNOWN_PHONE_NUMBER_CALLER_ID;
		if (number != null && number.trim().length() > 0 &&
			!number.equals(UNKNOWN_PHONE_NUMBER_CALLER_ID)) { // avoid converting "-1" to "1"
			callerIdNumber = PhoneNumberUtils.toCallerIDMinMatch(number);
			if (callerIdNumber == null || callerIdNumber.length() <= 0) {
				// failed to convert the number we got to a caller ID number
				return null;
			}
		}
		
		return callerIdNumber;
	}
	
	class RemoveCallInfoTask extends TimerTask {
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
	// The unknown number "-1" is from tests in the emulator. We consider it a 
	// special case 
	private static final String UNKNOWN_PHONE_NUMBER_CALLER_ID = "-1";
	private static final String SMS_PENDING_INTENT_NAME = "DindySMS";
	private String mLastRingingNumber = NOT_A_PHONE_NUMBER;
	private boolean mStarted = false;
	private DindySettings mSettings = null;
	private SmsManager mSM = null;
	private Timer mTimer = new Timer();
	//private HashMap<String, IncomingCallInfo> mIncomingCalls =
	//	new HashMap<String, IncomingCallInfo>();
	private IncomingCalls mIncomingCalls;
	private int mPreviousCallState = Consts.IncomingCallState.IDLE;
	private Context mContext;
	private AudioManager mAM = null;
	private PowerManager mPM = null;
	private PowerManager.WakeLock mWakeLock = null;
	private static final String[] PHONES_PROJECTION = { PhoneLookup.TYPE };
	private static final int PHONE_TYPE_INDEX = 0; 
	private ContentResolver mResolver = null;
	private BroadcastReceiver mOnSentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final int resultCode = getResultCode();
			if (resultCode != Activity.RESULT_OK) {
				if (Consts.DEBUG)
					Log.e(Consts.LOGTAG, "onReceive: got error code on SMS send: " + resultCode);
			} else {
				if (Consts.DEBUG)
					Log.d(Consts.LOGTAG, "onReceive: SMS sent successfully");
			}
			// NOTE: Since we started dividing the SMS message, this intent might
			// be called several times for each message, so we can't do anything
			// here that isn't OK to do a few times
			if (intent.getAction().equals(SMS_PENDING_INTENT_NAME)) {
				releaseWakeLockIfHeld("onReceive (SMS sent)");
			}
		}
	};
	private PendingIntent mSentPendingIntent = null;
}
