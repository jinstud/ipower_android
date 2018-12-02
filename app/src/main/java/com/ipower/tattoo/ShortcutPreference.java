package com.ipower.tattoo;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Formatter;

public class ShortcutPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener, OnClickListener {

    private static final String androidns="http://schemas.android.com/apk/res/android";

    private SeekBar mSeekBar;
    private TextView mSplashText, mValueText;
    private Context mContext;

    private String mDialogMessage, mSuffix;
    private int mDefault, mMax, mValue = 0;

    public ShortcutPreference(Context context, AttributeSet attributeSet) {

        super(context, attributeSet);
        mContext = context;

        int mDialogMessageId = attributeSet.getAttributeResourceValue(androidns, "dialogMessage", 0);
        if (mDialogMessageId == 0) {
            mDialogMessage = attributeSet.getAttributeValue(androidns, "dialogMessage");
        } else {
            mDialogMessage = mContext.getString(mDialogMessageId);
        }

        int mSuffixId = attributeSet.getAttributeResourceValue(androidns, "text", 0);
        if (mSuffixId == 0) {
            mSuffix = attributeSet.getAttributeValue(androidns, "text");
        } else {
            mSuffix = mContext.getString(mSuffixId);
        }

        mDefault = attributeSet.getAttributeIntValue(androidns, "defaultValue", 0);
        mMax = attributeSet.getAttributeIntValue(androidns, "max", 100);
    }

    @Override
    protected View onCreateDialogView() {

        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(iPowerApplication.dpToPx(20), iPowerApplication.dpToPx(20), iPowerApplication.dpToPx(20), iPowerApplication.dpToPx(30));

        mSplashText = new TextView(mContext);
        mSplashText.setPadding(0, 0, 0, iPowerApplication.dpToPx(10));
        if (mDialogMessage != null)
            mSplashText.setText(mDialogMessage);
        layout.addView(mSplashText);

        mValueText = new TextView(mContext);
        mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        mValueText.setTextSize(32);
        mValueText.setPadding(0, 0, 0, iPowerApplication.dpToPx(10));
        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(mValueText, params);

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setOnSeekBarChangeListener(this);
        layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (shouldPersist())
            mValue = getPersistedInt(mDefault);

        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);

        return layout;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        super.onSetInitialValue(restore, defaultValue);
        if (restore) {
            mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
        } else {
            mValue = (Integer) defaultValue;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int value, boolean fromTouch) {
        String text = String.format("%.2f", iPowerApplication.seekBarToVolts(seekBar));
        mValueText.setText(mSuffix == null ? text : text.concat(" " + mSuffix));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seek) {}

    @Override
    public void onStopTrackingTouch(SeekBar seek) {}

    public void setMax(int max) { mMax = max; }
    public int getMax() { return mMax; }

    public void setProgress(int progress) {
        mValue = progress;
        if (mSeekBar != null) {
            mSeekBar.setProgress(progress);
        }
    }

    public int getProgress() {
        return mValue;
    }

    public String getSuffix() {
        return mSuffix;
    }

    @Override
    public void showDialog(Bundle state) {

        super.showDialog(state);

        Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        if (shouldPersist()) {

            mValue = mSeekBar.getProgress();
            persistInt(mSeekBar.getProgress());
            callChangeListener(Integer.valueOf(mSeekBar.getProgress()));
        }

        getDialog().dismiss();
    }

}