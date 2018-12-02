package com.ipower.tattoo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.Session;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.Plus;

public class AccountPreference extends DialogPreference implements OnClickListener {

    private static final String androidns="http://schemas.android.com/apk/res/android";

    private TextView mSplashText;
    private Context mContext;
    private GoogleApiClient googleApiClient;

    private String mDialogMessage;

    public AccountPreference(Context context, AttributeSet attributeSet) {

        super(context, attributeSet);
        mContext = context;

        int mDialogMessageId = attributeSet.getAttributeResourceValue(androidns, "dialogMessage", 0);
        if (mDialogMessageId == 0) {
            mDialogMessage = attributeSet.getAttributeValue(androidns, "dialogMessage");
        } else {
            mDialogMessage = mContext.getString(mDialogMessageId);
        }

        googleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN).build();

        googleApiClient.connect();
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

        return layout;
    }

    @Override
    public void showDialog(Bundle state) {

        super.showDialog(state);

        Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);
        String login = sharedPreferences.getString("setting_account_login", "");

        if (login.equals("facebook")) {
            if (Session.getActiveSession() != null) {
                Session.getActiveSession().closeAndClearTokenInformation();
            }
        } else if (login.equals("google")) {
            revokeGplusAccess();
        }

        Auth.logged_out();

        getDialog().dismiss();

        SettingsActivity current = (SettingsActivity)mContext;

        Intent intent = new Intent(current, MainActivity.class);
        intent.putExtra("finish", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK);

        current.startActivity(intent);
        current.finish();
    }

    private void signOutFromGplus() {
        if (googleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(googleApiClient);
            googleApiClient.disconnect();
            googleApiClient.connect();
        }
    }

    private void revokeGplusAccess() {
        if (googleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(googleApiClient);
            Plus.AccountApi.revokeAccessAndDisconnect(googleApiClient)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status arg0) {
                            googleApiClient.connect();
                        }
                    });
        }
    }

}