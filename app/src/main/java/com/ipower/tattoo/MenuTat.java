package com.ipower.tattoo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.facebook.Session;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.Plus;

import java.util.Arrays;

public class MenuTat extends Activity {
	
	//private Button btnSett;
	private String lingua;
    private GoogleApiClient googleApiClient;

    final Context context = this;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN).build();
		
		iPowerApplication myapp=((iPowerApplication)getApplicationContext());
		lingua=myapp.setLanguageCreateActivity();
		
		setContentView(R.layout.activity_menu_tat);
		
		Button btnSett = (Button)findViewById(R.id.bt_setting);
		btnSett.setOnClickListener
                (
                        new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(MenuTat.this, SettingTat.class);
                                startActivity(intent);
                            }
                        }
                );
		
		Button btnPswd = (Button)findViewById(R.id.bt_password);
		btnPswd.setOnClickListener
		(
			new OnClickListener() 
			{
				@Override
				public void onClick(View v)
				{
					Intent intent = new Intent(MenuTat.this, PasswordTat.class);
					startActivity(intent);
				}
			}
		);
		
		Button btnFt=(Button)findViewById(R.id.bt_foot);
		btnFt.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MenuTat.this, Foot.class);
				startActivity(intent);
			}
			
		});
		
		Button btnLn=(Button)findViewById(R.id.bt_lingua);
		btnLn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MenuTat.this, LanguageTat.class);
				startActivity(intent);
			}
			
		});

		Button buttonSignOut = (Button)findViewById(R.id.bt_sign_out);
        buttonSignOut.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
                String login = Auth.preferences.getString("setting_account_login", null);
                if (login.equals("facebook")) {
                    if (Session.getActiveSession() != null) {
                        Session.getActiveSession().closeAndClearTokenInformation();
                    }
                } else if (login.equals("google")) {
                    revokeGplusAccess();
                    //signOutFromGplus();
                }
                Auth.logged_out();

                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("finish", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
		});
	}

    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    protected void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }
	
	@Override
	protected void onResume()
	{
		super.onResume();
		iPowerApplication myapp = ((iPowerApplication)getApplicationContext());
		//myapp.aggiornaLingua(getApplicationContext());
		if (!lingua.equals(myapp.getLanguage()))
		{
			Log.i("lingua","lingua aggiornata mainactivity");
			lingua=myapp.getLanguage();Log.i("lingua","v2 "+lingua);
			Intent refresh = new Intent(this, MenuTat.class); 
			finish();
			startActivity(refresh);			
		}
		
		/*Locale locale = new Locale("en_US");
		Locale.setDefault(locale);
		Configuration config = new Configuration();
		config.locale = locale;
		getApplicationContext().getApplicationContext().getResources().updateConfiguration(config, null);*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_tat, menu);
		return false;
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
