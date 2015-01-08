package net.gnobal.dindy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

public class ProfileNameDialogFragment extends DialogFragment {
	final static int DIALOG_RENAME_PROFILE = 100;
	final static int DIALOG_NEW_PROFILE = 101;

	interface Listener {
		void onSuccess(int type, String newProfileName, long newProfileId);
	}

	public ProfileNameDialogFragment() {
	}

	static ProfileNameDialogFragment newInstance(final int type, final String title,
		final String oldProfileName) {
		final ProfileNameDialogFragment f = new ProfileNameDialogFragment();
		final Bundle args = new Bundle();
		args.putInt("type", type);
		args.putString("title", title);
		args.putString("oldProfileName", oldProfileName);
		f.setArguments(args);
		return f;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return buildProfileNameDialog(getActivity());
	}

	@Override
	public void onResume() {
		super.onResume();
		prepareDialog(getDialog());
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mListener = (Listener) activity;
	}
	
	private Dialog buildProfileNameDialog(Context context) {
		LayoutInflater factory = LayoutInflater.from(context);
		final View textEntryView = factory.inflate(
				R.layout.profile_name_dialog, null);
		
		AlertDialog dialog = new AlertDialog.Builder(context)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(getArguments().getString("title"))
				.setView(textEntryView)
				.setPositiveButton(R.string.dialog_profile_name_ok,
						new OkClickListener())
				.setNegativeButton(R.string.dialog_profile_name_cancel,
						new CancelClickListener())
				.create();

		dialog.setOwnerActivity(getActivity());
		dialog.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		
		return dialog;
	}
	
	private void prepareDialog(Dialog dialog) {
		EditText editBox = (EditText) dialog.findViewById(
				R.id.dialog_profile_name_edit_box);
		editBox.addTextChangedListener(new ProfileNameTextWatcher(
				((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE),
				editBox));
		dialog.setTitle(getArguments().getString("title"));
		editBox.setText(getArguments().getString("oldProfileName"));
		editBox.setSelection(0, getArguments().getString("oldProfileName").length());
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
			// Note that we always trim the profile name. This is important because we assume that
			// names are unique
			String profileName = s.toString().trim();
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

		private final Button mOkButton;
		private final ProfilePreferencesHelper mPreferencesHelper;
		private final EditText mProfileNameEditBox;
		private boolean mIsFirstChange = true;
	}

	private class CancelClickListener implements DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			Log.d(Consts.LOGTAG, "Cancel click");
		}		
	}

	private class OkClickListener implements DialogInterface.OnClickListener {		
		public void onClick(DialogInterface dialog, int which) {
			Log.d(Consts.LOGTAG, "OK click");

			EditText edit = (EditText) ((AlertDialog) dialog).findViewById(
					R.id.dialog_profile_name_edit_box);
			// Note that we always trim the profile name. This is important because we assume that
			// names are unique
			final String newProfileName = edit.getText().toString().trim();
			ProfilePreferencesHelper preferencesHelper =
				ProfilePreferencesHelper.instance();
			if (preferencesHelper.profileExists(newProfileName)) {
				// TODO issue an error
				return;
			}
			
			long newProfileId = Consts.NOT_A_PROFILE_ID;
			switch (getArguments().getInt("type")) {
			case DIALOG_NEW_PROFILE:
				newProfileId = preferencesHelper.createNewProfile(
						newProfileName);
				break;
			
			case DIALOG_RENAME_PROFILE:
				if (getArguments().getString("oldProfileName").equals(newProfileName)) {
					// Nothing to do
					return;
				}
				preferencesHelper.renameProfile(getArguments().getString("oldProfileName"), newProfileName);
				newProfileId = preferencesHelper.getProfileIdFromName(newProfileName);
				break;
			}	

			// Success - inform whoever called us
			mListener.onSuccess(getArguments().getInt("type"), newProfileName, newProfileId);
		}
	}

	private Listener mListener; 
}