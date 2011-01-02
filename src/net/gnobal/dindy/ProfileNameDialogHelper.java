package net.gnobal.dindy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Config;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

class ProfileNameDialogHelper {
	interface Listener {
		int getDialogType();
		
		void onSuccess(String newProfileName, long newProfileId);

		Activity getOwnerActivity();
	}
	
	static void setDialogVariables(String title, String oldProfileName) {
		mTitle = title;
		mOldProfileName = oldProfileName;
	}
	
	private static String mTitle = null;
	private static String mOldProfileName = null;
		
	static Dialog buildProfileNameDialog(Context context,
			int iconResource, Listener listener) {
		LayoutInflater factory = LayoutInflater.from(context);
		final View textEntryView = factory.inflate(
				R.layout.profile_name_dialog, null);
		
		AlertDialog dialog = new AlertDialog.Builder(context)
				.setIcon(iconResource)
				.setTitle(mTitle)
				.setView(textEntryView)
				.setPositiveButton(R.string.dialog_profile_name_ok,
						new OkClickListener(listener))
				.setNegativeButton(R.string.dialog_profile_name_cancel,
						new CancelClickListener())
				.create();

		dialog.setOwnerActivity(listener.getOwnerActivity());
		dialog.setOnDismissListener(new DismissListener(listener));
		
		return dialog;
	}
	
	static void prepareDialog(int id, Dialog dialog) {
		EditText editBox = (EditText) dialog.findViewById(
				R.id.dialog_profile_name_edit_box);
		editBox.addTextChangedListener(new ProfileNameTextWatcher(
				((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE),
				editBox));
		dialog.setTitle(mTitle);
		editBox.setText(mOldProfileName);
		editBox.setSelection(0, mOldProfileName.length());
	}

	private static class ProfileNameTextWatcher implements TextWatcher {
		ProfileNameTextWatcher(Button okButton, EditText profileNameEditBox) {
			mOkButton = okButton;
			mPreferencesHelper = ProfilePreferencesHelper.instance();
			mProfileNameEditBox = profileNameEditBox;
		}

		public void afterTextChanged(Editable s) {
			// If you look in the code for View.setEnable() you'll see that it's
			// not optimized for the case where the view is already in the right
			// state so we need to do this
			int illegalNameReasonStringId = -1;
			boolean isNameLegal = false;
			String profileName = s.toString();
			if (profileName.length() <= 0) {
				illegalNameReasonStringId =
					R.string.dialog_profile_name_illegal_name_reason_empty;
			} else if (profileName.contains("'")) {
				illegalNameReasonStringId =
					R.string.dialog_profile_name_illegal_name_reason_apostrophe;
			} else if (mPreferencesHelper.profileExists(profileName)) {
				illegalNameReasonStringId =
					R.string.dialog_profile_name_illegal_name_reason_already_exists;
			} else {
				isNameLegal = true;
			}
			if (isNameLegal || mIsFirstChange) {
				mProfileNameEditBox.setError(null);
			} else {
				mProfileNameEditBox.setError(mProfileNameEditBox.getContext().getString(
						illegalNameReasonStringId));
			}
			final boolean needsEnable = 
				mOkButton.isEnabled() != isNameLegal;
			if (needsEnable) {
				mOkButton.setEnabled(isNameLegal);
			}
			
			mIsFirstChange = false;
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}

		private Button mOkButton;
		private ProfilePreferencesHelper mPreferencesHelper;
		private EditText mProfileNameEditBox;
		private boolean mIsFirstChange = true;
	}

	private static class CancelClickListener implements DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG,
					"Cancel click");
		}		
	}
	
	private static class OkClickListener implements DialogInterface.OnClickListener {
		OkClickListener(Listener listener) {
			mListener = listener;
		}
		
		public void onClick(DialogInterface dialog, int which) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, "OK click");

			EditText edit = (EditText) ((AlertDialog) dialog).findViewById(
					R.id.dialog_profile_name_edit_box);
			String newProfileName = edit.getText().toString();
			ProfilePreferencesHelper preferencesHelper =
				ProfilePreferencesHelper.instance();
			if (preferencesHelper.profileExists(newProfileName)) {
				// TODO issue an error
				return;
			}
			
			long newProfileId = Consts.NOT_A_PROFILE_ID;
			switch (mListener.getDialogType()) {
			case DIALOG_NEW_PROFILE:
				newProfileId = preferencesHelper.createNewProfile(
						newProfileName);
				break;
			
			case DIALOG_RENAME_PROFILE:
				if (mOldProfileName.equals(newProfileName)) {
					// Nothing to do
					return;
				}
				preferencesHelper.renameProfile(mOldProfileName,
						newProfileName);
				break;
			}	

			// Success - inform whoever called us
			mListener.onSuccess(newProfileName, newProfileId);
		}
		
		private Listener mListener;
	}
	
	private static class DismissListener implements DialogInterface.OnDismissListener {
		DismissListener(Listener listener) {
			mListener = listener;
		}

		public void onDismiss(DialogInterface dialog) {
			if (Config.LOGD && Consts.DEBUG) Log.d(Consts.LOGTAG, "Dismiss");
			mListener.getOwnerActivity().removeDialog(mListener.getDialogType());
		}
		
		private Listener mListener;
	}

	
	final static int DIALOG_RENAME_PROFILE = 0;
	final static int DIALOG_NEW_PROFILE = 1;
}
