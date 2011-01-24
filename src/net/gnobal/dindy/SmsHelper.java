package net.gnobal.dindy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.PendingIntent;
import android.util.Config;
import android.util.Log;

public class SmsHelper {
	void sendMessage(String number, String message,
			PendingIntent sentPendingIntent) {
		if (mSmsManager == null || mSendTextMessageMethod == null) {
			return;
		}
		
		try {
			mSendTextMessageMethod.invoke(mSmsManager,
					new Object[]{number, null, message, sentPendingIntent,
						null});
		} catch (IllegalArgumentException e) {
			Log.e(Consts.LOGTAG, ERROR_FAILED_TO_SEND_SMS_MESSAGE, e);
		} catch (IllegalAccessException e) {
			Log.e(Consts.LOGTAG, ERROR_FAILED_TO_SEND_SMS_MESSAGE, e);
		} catch (InvocationTargetException e) {
			Log.e(Consts.LOGTAG, ERROR_FAILED_TO_SEND_SMS_MESSAGE, e);
		}
	}
	
	String[] getAddressesFromSmsPdus(byte[][] pdus) {
        int pduCount = pdus.length;
        String[] addresses = new String[pduCount];
		Object[] msgs = new Object[pduCount];
        for (int i = 0; i < pduCount; i++) {
            try {
            	msgs[i] = mCreateFromPduMethod.invoke(null,
            			new Object[]{pdus[i]});
            	addresses[i] = (String) mGetOriginatingAddressMethod.invoke(
            			msgs[i], new Object[]{});
    		} catch (IllegalArgumentException e) {
    			Log.e(Consts.LOGTAG, ERROR_FAILED_TO_GET_MESSAGE_FROM_PDU, e);
    		} catch (IllegalAccessException e) {
    			Log.e(Consts.LOGTAG, ERROR_FAILED_TO_GET_MESSAGE_FROM_PDU, e);
    		} catch (InvocationTargetException e) {
    			Log.e(Consts.LOGTAG, ERROR_FAILED_TO_GET_MESSAGE_FROM_PDU, e);
    		}
        }
        
        return addresses;
	}
	
	SmsHelper() {
		Class<?> smsManagerClass = null;
		try {
			// Try to get newer implementation
			smsManagerClass = Class.forName(SMS_MANAGER_CLASS_NAME_16);
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"got 1.6 SmsManager class");
			mSmsMessageClass = Class.forName(SMS_MESSAGE_CLASS_NAME_16);
		} catch (ClassNotFoundException e1) {
			try {
				// Try to get older implementation
				smsManagerClass = Class.forName(SMS_MANAGER_CLASS_NAME_15);
				if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"got 1.5 SmsManager class");
				mSmsMessageClass = Class.forName(SMS_MASSAGE_CLASS_NAME_15);
			} catch (ClassNotFoundException e2) {
				Log.e(Consts.LOGTAG, "failed to get SmsManager class");
				e2.printStackTrace();
				return;
			}
		} 

		boolean success = false;
		try {
			Method getDefaultMethod = smsManagerClass.getMethod(
					SMS_MANAGER_GET_DEFAULT_METHOD_NAME,
					new Class[]{});
			mSendTextMessageMethod = smsManagerClass.getMethod(
					SMS_MANAGER_SEND_TEXT_MESSAGE_METHOD_NAME,
					new Class[] {String.class, String.class, String.class,
					PendingIntent.class, PendingIntent.class});
			mSmsManager = getDefaultMethod.invoke(null, (Object[]) null);
			
			mCreateFromPduMethod = mSmsMessageClass.getMethod(
					SMS_MESSAGE_CREATE_FROM_PDU_METHOD_NAME,
					new Class[]{byte[].class});
			mGetOriginatingAddressMethod = mSmsMessageClass.getMethod(
					SMS_MESSAGE_GET_ORIGINATING_ADDRESS_METHOD_NAME,
					new Class[]{});
			
			success = true;
		} catch (SecurityException e) {
			Log.e(Consts.LOGTAG, ERROR_FAILED_TO_INIT_SMS_SENDER, e);
		} catch (NoSuchMethodException e) {
			Log.e(Consts.LOGTAG, ERROR_FAILED_TO_INIT_SMS_SENDER, e);
		} catch (IllegalArgumentException e) {
			Log.e(Consts.LOGTAG, ERROR_FAILED_TO_INIT_SMS_SENDER, e);
		} catch (IllegalAccessException e) {
			Log.e(Consts.LOGTAG, ERROR_FAILED_TO_INIT_SMS_SENDER, e);
		} catch (InvocationTargetException e) {
			Log.e(Consts.LOGTAG, ERROR_FAILED_TO_INIT_SMS_SENDER, e);
		} finally {
			if (!success) {
				mSendTextMessageMethod = null;
				mSmsManager = null;
			}
		}
	}
	
	private Object mSmsManager;
	private Method mSendTextMessageMethod;
	private Class<?> mSmsMessageClass = null;
	private Method mCreateFromPduMethod;
	private Method mGetOriginatingAddressMethod;
	
	private static final String SMS_MANAGER_CLASS_NAME_15 =
		"android.telephony.gsm.SmsManager";
	private static final String SMS_MANAGER_CLASS_NAME_16 =
		"android.telephony.SmsManager";
	private static final String SMS_MASSAGE_CLASS_NAME_15 =
		"android.telephony.gsm.SmsMessage";
	private static final String SMS_MESSAGE_CLASS_NAME_16 =
		"android.telephony.SmsMessage";
	private static final String SMS_MANAGER_GET_DEFAULT_METHOD_NAME =
		"getDefault";
	private static final String SMS_MANAGER_SEND_TEXT_MESSAGE_METHOD_NAME =
		"sendTextMessage";
	private static final String SMS_MESSAGE_CREATE_FROM_PDU_METHOD_NAME =
		"createFromPdu";
	private static final String SMS_MESSAGE_GET_ORIGINATING_ADDRESS_METHOD_NAME =
		"getOriginatingAddress";
	private static final String ERROR_FAILED_TO_INIT_SMS_SENDER =
		"failed to initialize sms sender class";
	private static final String ERROR_FAILED_TO_SEND_SMS_MESSAGE =
		"failed to send sms message";
	private static final String ERROR_FAILED_TO_GET_MESSAGE_FROM_PDU =
		"failed to get message from pdu";
}
