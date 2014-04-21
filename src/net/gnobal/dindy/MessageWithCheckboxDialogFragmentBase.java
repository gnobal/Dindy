package net.gnobal.dindy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

abstract class MessageWithCheckboxDialogFragmentBase extends DialogFragment {
	protected MessageWithCheckboxDialogFragmentBase(
		int dialogTextResId,
		int dialogTitleResId,
		String dialogPreferenceKey) {
		mDialogTextResId = dialogTextResId;
		mDialogTitleResId = dialogTitleResId;
		mDialogPreferenceKey = dialogPreferenceKey;			
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		LayoutInflater factory = LayoutInflater.from(getActivity());
		final View dialogView = factory.inflate(
				R.layout.message_with_checkbox_dialog, null);
		final TextView messageView = (TextView) dialogView.findViewById(
				R.id.message_with_checkbox_dialog_text_view);
		messageView.setText(mDialogTextResId);
		AlertDialog dialog = new AlertDialog.Builder(getActivity())
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(mDialogTitleResId)
			.setView(dialogView)
			.setPositiveButton(R.string.message_dialog_ok_text,
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int which) {
					onPositiveButtonClicked(dialog);
				}})
			.create();
		dialog.setOwnerActivity(getActivity());
		return dialog;
	}

	protected void onPositiveButtonClicked(DialogInterface dialog) {
		SharedPreferences preferences = getActivity().getSharedPreferences(
				Consts.Prefs.Main.NAME, Context.MODE_PRIVATE);
		CheckBox checkBox = (CheckBox) 
			((AlertDialog) dialog).findViewById(
				R.id.message_with_checkbox_dialog_checkbox);
		SharedPreferences.Editor editor = 
			preferences.edit();
		editor.putBoolean(
				mDialogPreferenceKey,
				!checkBox.isChecked());
		editor.commit();
	}
	
	private final int mDialogTextResId;
	private final int mDialogTitleResId;
	private final String mDialogPreferenceKey;
}