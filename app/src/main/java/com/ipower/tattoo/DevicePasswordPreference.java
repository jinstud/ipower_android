package com.ipower.tattoo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class DevicePasswordPreference extends DialogPreference implements OnClickListener {

    private static final String androidns="http://schemas.android.com/apk/res/android";

    private TextView mSplashText;
    private EditText mPasswordEditText;
    private Context mContext;

    private String mDialogMessage, mValue;

    private boolean currentPasswordTimeout, newPasswordTimeout;
    private String currentPassword, newPassword;

    private ProgressDialog loadingIndicator;

    public DevicePasswordPreference(Context context, AttributeSet attributeSet) {

        super(context, attributeSet);
        mContext = context;

        int mDialogMessageId = attributeSet.getAttributeResourceValue(androidns, "dialogMessage", 0);
        if (mDialogMessageId == 0) {
            mDialogMessage = attributeSet.getAttributeValue(androidns, "dialogMessage");
        } else {
            mDialogMessage = mContext.getString(mDialogMessageId);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);
        mValue = sharedPreferences.getString(getKey(), "");

        loadingIndicator = new ProgressDialog(context);
        loadingIndicator.setMessage(iPowerApplication.context.getResources().getString(R.string.processing_dots));
        loadingIndicator.setCancelable(false);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        final iPowerApplication app = (iPowerApplication)mContext.getApplicationContext();

        if (app.isConnState()) {

            builder.setNeutralButton("Device", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    getDialog().dismiss();

                    final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("Device password");

                    LinearLayout layout = new LinearLayout(mContext);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(iPowerApplication.dpToPx(20), iPowerApplication.dpToPx(20), iPowerApplication.dpToPx(20), iPowerApplication.dpToPx(30));

                    TextView mSplashText = new TextView(mContext);
                    mSplashText.setPadding(0, 0, 0, iPowerApplication.dpToPx(10));
                    mSplashText.setText("Set iPower device password:");
                    layout.addView(mSplashText);


                    final EditText mPasswordEditText = new EditText(mContext);
                    mPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    //mPasswordEditText.setGravity(Gravity.CENTER_HORIZONTAL);

                    final EditText mCurrentPasswordEditText = new EditText(mContext);

                    if (!sharedPreferences.getString("setting_device_password", "").isEmpty()) {
                        mCurrentPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        mCurrentPasswordEditText.setHint(R.string.current_password);
                        //mCurrentPasswordEditText.setGravity(Gravity.CENTER_HORIZONTAL);
                        layout.addView(mCurrentPasswordEditText);

                        mPasswordEditText.setHint(R.string.new_password);
                    } else {
                        mPasswordEditText.setHint(R.string.password);
                    }

                    layout.addView(mPasswordEditText);

                    final EditText mConfirmPasswordEditText = new EditText(mContext);
                    mConfirmPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    mConfirmPasswordEditText.setHint(R.string.confirm_password);
                    //mConfirmPasswordEditText.setGravity(Gravity.CENTER_HORIZONTAL);
                    layout.addView(mConfirmPasswordEditText);

                    builder.setView(layout);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            try {
                                mContext.unregisterReceiver(mGattUpdateReceiver);
                            } catch (Exception e) { }
                        }
                    });

                    final AlertDialog changeDialog = builder.create();

                    changeDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialog) {
                            mContext.registerReceiver(mGattUpdateReceiver, iPowerApplication.makeGattUpdateIntentFilter());
                        }
                    });

                    changeDialog.show();

                    changeDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (sharedPreferences.getString("setting_device_password", "").isEmpty()) {
                                currentPassword = "tattoo";
                            } else {
                                currentPassword = mCurrentPasswordEditText.getText().toString();
                            }

                            String password = mPasswordEditText.getText().toString();
                            String confirmPassword = mConfirmPasswordEditText.getText().toString();

                            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(
                                    Context.INPUT_METHOD_SERVICE);

                            if (mCurrentPasswordEditText.hasFocus()) {
                                mCurrentPasswordEditText.clearFocus();
                                imm.hideSoftInputFromWindow(mCurrentPasswordEditText.getWindowToken(), 0);
                            }

                            if (mPasswordEditText.hasFocus()) {
                                mPasswordEditText.clearFocus();
                                imm.hideSoftInputFromWindow(mPasswordEditText.getWindowToken(), 0);
                            }

                            if (mConfirmPasswordEditText.hasFocus()) {
                                mConfirmPasswordEditText.clearFocus();
                                imm.hideSoftInputFromWindow(mConfirmPasswordEditText.getWindowToken(), 0);
                            }

                            if (currentPassword.isEmpty()) {
                                mCurrentPasswordEditText.requestFocus();
                                iPowerApplication.makeToast(R.string.error_password_current);
                            } else if (password.isEmpty() || password.length() > 15) {
                                mPasswordEditText.requestFocus();
                                iPowerApplication.makeToast(R.string.error_password_length);
                            } else if (!password.equals(confirmPassword)) {
                                mConfirmPasswordEditText.requestFocus();
                                iPowerApplication.makeToast(R.string.error_password_match);
                            } else {
                                newPassword = password;

                                loadingIndicator.setOnShowListener(new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface dialog) {
                                        sendCurrentPassword();

                                        changeDialog.dismiss();
                                    }
                                });

                                loadingIndicator.show();
                            }
                        }
                    });
                }
            });
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

        mPasswordEditText = new EditText(mContext);
        mPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mPasswordEditText.setText(mValue);
        mPasswordEditText.setHint(R.string.password);
        //mPassowrdEditText.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(mPasswordEditText);

        return layout;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mPasswordEditText.setText(mValue);
    }

    @Override
    public void showDialog(Bundle state) {

        super.showDialog(state);

        Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(this);

        Button neutralButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_NEUTRAL);
        if (neutralButton != null) {
            Drawable drawable = mContext.getResources().getDrawable(R.drawable._settings_device_password);

            drawable.setBounds((int) (drawable.getIntrinsicWidth() * 0.5),
                    0, (int) (drawable.getIntrinsicWidth() * 1.5),
                    drawable.getIntrinsicHeight());

            neutralButton.setCompoundDrawables(drawable, null, null, null);
        }
    }

    @Override
    public void onClick(View view) {

        if (shouldPersist()) {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            mValue = mPasswordEditText.getText().toString();

            if (mValue.length() > 0) {
                editor.putString(getKey(), mValue);
            } else {
                editor.remove(getKey());
            }

            editor.commit();
        }

        getDialog().dismiss();
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {

            } else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] data = intent.getByteArrayExtra(RBLService.EXTRA_DATA);

                if (currentPasswordTimeout) {
                    readCurrentPasswordResponse(data);
                } else if (newPasswordTimeout) {
                    readNewPasswordResponse(data);
                }
            }
        }
    };

    private void sendCurrentPassword() {

        iPowerApplication app = (iPowerApplication)mContext.getApplicationContext();

        byte buf[] = new byte[] {(byte)0x09, (byte)0x00, (byte)0x00};

        Random random = new Random();
        app.setNumIdTrasm((byte)random.nextInt(256));

        buf[1] = (byte)currentPassword.length();
        buf[2] = app.getNumIdTrasm();

        //SystemClock.sleep(app.getMSDELAYBT());

        app.sendBluetoothData(buf);

        for (int i = 0; i < currentPassword.length(); i++) {

            //SystemClock.sleep(app.getMSDELAYBT());

            buf[0] = (byte)(((i+1)<<4)+9);
            buf[1] = (byte)currentPassword.charAt(i);

            app.sendBluetoothData(buf);
        }

        currentPasswordTimeout = true;

        (new Timer()).schedule(new TimerTask() {
            @Override
            public void run() {
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (currentPasswordTimeout) {
                            currentPasswordTimeout = false;
                            loadingIndicator.dismiss();

                            try {
                                mContext.unregisterReceiver(mGattUpdateReceiver);
                            } catch (Exception e) { }

                            iPowerApplication.makeToast(R.string.error_password_change_timeout);
                        }
                    }
                });
            }
        }, 10000);
    }

    private void readCurrentPasswordResponse(byte[] data) {
        for (int i = 0; i < data.length; i += 3) {
            if (data[i] == 0x0A) {
                iPowerApplication app = (iPowerApplication)mContext.getApplicationContext();
                if (data[i + 2] == app.getNumIdTrasm()) {
                    currentPasswordTimeout = false;
                    if (data[i + 1] == app.getCONFERMA()) {
                        sendNewPassword();
                    } else {
                        loadingIndicator.dismiss();

                        try {
                            mContext.unregisterReceiver(mGattUpdateReceiver);
                        } catch (Exception e) { }

                        app.bluetoothLeService.disconnect();
                        app.bluetoothLeService.close();

                        iPowerApplication.makeToast(R.string.error_password_incorrect);
                    }
                }
            }
        }
    }

    private void sendNewPassword() {
        iPowerApplication app = (iPowerApplication)mContext.getApplicationContext();

        byte buf[] = new byte[] {(byte)0x0D, (byte)0x00, (byte)0x00};

        Random random = new Random();
        app.setNumIdTrasm((byte)random.nextInt(256));

        buf[1] = (byte)newPassword.length();
        buf[2]= app.getNumIdTrasm();

        //SystemClock.sleep(app.getMSDELAYBT());

        app.sendBluetoothData(buf);

        for (int i=0; i < newPassword.length(); i++) {
            //SystemClock.sleep(app.getMSDELAYBT());

            buf[0] = (byte)(((i+1)<<4)+13);
            buf[1] = (byte)newPassword.charAt(i);
            app.sendBluetoothData(buf);

            Log.i("DEBUG0", "byte Tx: " + iPowerApplication.byteToHex(buf[0]) + String.valueOf((char) buf[1]) + iPowerApplication.byteToHex(buf[2]));
        }

        newPasswordTimeout = true;

        (new Timer()).schedule(new TimerTask() {
            @Override
            public void run() {
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (newPasswordTimeout) {
                            newPasswordTimeout = false;
                            loadingIndicator.dismiss();

                            try {
                                mContext.unregisterReceiver(mGattUpdateReceiver);
                            } catch (Exception e) { }

                            iPowerApplication.makeToast(R.string.error_password_response_timeout);
                        }
                    }
                });
            }
        }, 10000);
    }

    private void readNewPasswordResponse(byte[] data) {
        for (int i = 0; i < data.length; i += 3) {
            if (data[i] == 0x0C) {
                iPowerApplication app = (iPowerApplication)iPowerApplication.context;
                if (data[i + 2] == app.getNumIdTrasm()) {
                    loadingIndicator.dismiss();

                    try {
                        mContext.unregisterReceiver(mGattUpdateReceiver);
                    } catch (Exception e) { }

                    if (data[i + 1] == app.getCONFERMA()) {
                        app.setPassword(newPassword);
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("setting_device_password", newPassword);
                        editor.commit();
                        iPowerApplication.makeToast(R.string.password_changed);
                    } else {
                        iPowerApplication.makeToast(R.string.error_password_response_current);
                    }

                    newPasswordTimeout = false;
                }
            }
        }
    }

}