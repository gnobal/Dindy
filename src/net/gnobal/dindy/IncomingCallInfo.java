package net.gnobal.dindy;

import net.gnobal.dindy.DindyLogic.RemoveCallInfoTask;

class IncomingCallInfo {
	IncomingCallInfo(String number,
			String callerIdNumber,
			long absoluteWakeupTimeMillis) {
		mNumber = number;
		mCallerIdNumber = callerIdNumber;
		mAbsoluteWakeupTimeMillis = absoluteWakeupTimeMillis;
	}

	void associateRemovalTask(RemoveCallInfoTask associatedRemovalTask) {
		mAssociatedRemovalTask = associatedRemovalTask;		
	}
	
	String getNumber() {
		return mNumber;
	}

	String getCallerIdNumber() {
		return mCallerIdNumber;
	}
	
	RemoveCallInfoTask getAssociatedRemovalTask() {
		return mAssociatedRemovalTask;
	}

	long getAbsoluteWakeupTimeMillis() {
		return mAbsoluteWakeupTimeMillis;
	}
	
	private String mNumber;
	private String mCallerIdNumber;
	// private boolean mHasBeenNotifiedWithSMS = false;
	private RemoveCallInfoTask mAssociatedRemovalTask;
	private long mAbsoluteWakeupTimeMillis;
}