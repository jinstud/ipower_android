package com.ipower.tattoo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DeviceNamePreference extends DialogPreference implements OnClickListener {

    private static final String androidns="http://schemas.android.com/apk/res/android";

    private TextView mSplashText;
    private EditText mNameEditText;
    private Context mContext;

    private String mDialogMessage, mValue;

    public DeviceNamePreference(Context context, AttributeSet attributeSet) {

        super(context, attributeSet);
        mContext = context;

        int mDialogMessageId = attributeSet.getAttributeResourceValue(androidns, "dialogMessage", 0);
        if (mDialogMessageId == 0) {
            mDialogMessage = attributeSet.getAttributeValue(androidns, "dialogMessage");
        } else {
            mDialogMessage = mContext.getString(mDialogMessageId);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);
        iPowerApplication app = (iPowerApplication)mContext.getApplicationContext();

        if (app.isConnState()) {
            mValue = sharedPreferences.getString(getKey().concat("_" + app.getmDevice().getAddress()), "");
        } else {
            mValue = "";
        }
    }

    @Override
    protected View onCreateDialogView() {

        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(iPowerApplication.dpToPx(20), iPowerApplication.dpToPx(20), iPowerApplication.dpToPx(20), iPowerApplication.dpToPx(30));

        mSplashText = new TextView(mContext);
        mSplashText.setPadding(0, 0, 0, iPowerApplication.dpToPx(10));
        if (mDialogMessage != null)
            mSplashText.setText(mDialogMessage);
        layout.addView(mSplashText);

        mNameEditText = new EditText(mContext);
        mNameEditText.setText(mValue);
        mNameEditText.setHint(R.string.name);
        //mNameEditText.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(mNameEditText);

        return layout;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mNameEditText.setText(mValue);
    }

    @Override
    public void showDialog(Bundle state) {

        super.showDialog(state);

        Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        iPowerApplication app = (iPowerApplication)mContext.getApplicationContext();

        if (app.isConnState()) {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            mValue = mNameEditText.getText().toString();

            if (mValue.length() > 0) {
                editor.putString(getKey().concat("_" + app.getmDevice().getAddress()), mValue);
            } else {
                editor.remove(getKey().concat("_" + app.getmDevice().getAddress()));
            }

            editor.commit();

            getDialog().dismiss();
        } else {
            iPowerApplication.makeToast(R.string.device_name_not_connected);
        }
    }

}