package com.ipower.tattoo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DeviceSetPasswordPreference extends DialogPreference implements OnClickListener {

    private static final String androidns="http://schemas.android.com/apk/res/android";

    private TextView mSplashText;
    private EditText mCurrentPasswordEditText, mPassowordEditText, mConfirmPassowordEditText;
    private Context mContext;

    private String mDialogMessage, mValue;

    public DeviceSetPasswordPreference(Context context, AttributeSet attributeSet) {

        super(context, attributeSet);
        mContext = context;

        if (attributeSet != null) {
            int mDialogMessageId = attributeSet.getAttributeResourceValue(androidns, "dialogMessage", 0);
            if (mDialogMessageId == 0) {
                mDialogMessage = attributeSet.getAttributeValue(androidns, "dialogMessage");
            } else {
                mDialogMessage = mContext.getString(mDialogMessageId);
            }
        }
    }

    @Override
    public void setDialogMessage(CharSequence dialogMessage) {
        mDialogMessage = dialogMessage.toString();
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

        mCurrentPasswordEditText = new EditText(mContext);
        mCurrentPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mCurrentPasswordEditText.setHint(R.string.current_password);
        mCurrentPasswordEditText.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(mCurrentPasswordEditText);

        mPassowordEditText = new EditText(mContext);
        mPassowordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mPassowordEditText.setHint(R.string.new_password);
        mPassowordEditText.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(mPassowordEditText);

        mConfirmPassowordEditText = new EditText(mContext);
        mConfirmPassowordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mConfirmPassowordEditText.setHint(R.string.confirm_password);
        mConfirmPassowordEditText.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(mConfirmPassowordEditText);

        return layout;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
    }

    @Override
    public void showDialog(Bundle state) {

        super.showDialog(state);

        Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        if (shouldPersist()) {


        }

        getDialog().dismiss();
    }

}