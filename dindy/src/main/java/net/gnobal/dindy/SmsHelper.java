package net.gnobal.dindy;

import android.telephony.SmsMessage;
import android.util.Log;

class SmsHelper {
	static String[] getAddressesFromSmsPdus(byte[][] pdus) {
		int pduCount = pdus.length;
		String[] addresses = new String[pduCount];
		SmsMessage[] msgs = new SmsMessage[pduCount];
		for (int i = 0; i < pduCount; i++) {
			try {
				msgs[i] = SmsMessage.createFromPdu(pdus[i]);
				addresses[i] = msgs[i].getOriginatingAddress();
			} catch (Exception e) {
				Log.e(Consts.LOGTAG, ERROR_FAILED_TO_GET_MESSAGE_FROM_PDU, e);
			}
		}

		return addresses;
	}
	
	private static final String ERROR_FAILED_TO_GET_MESSAGE_FROM_PDU =
		"failed to get message from pdu";
}
