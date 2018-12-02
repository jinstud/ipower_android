package com.ipower.tattoo;

import java.util.Locale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

public class LanguageTat extends Activity {
	
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() 
	{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			final String action = intent.getAction();
			//iPowerApplication myapp=((iPowerApplication)getApplicationContext());
			if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) 
			{
				Toast.makeText(getApplicationContext(), "Disconnected",Toast.LENGTH_SHORT).show();
				//setButtonDisable();
			}	
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_language_tat);
		
		Button cf=(Button)findViewById(R.id.conferma_language);
		cf.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Log.i("lingua", "start");
				RadioGroup ft=(RadioGroup)findViewById(R.id.language);
				iPowerApplication myapp=((iPowerApplication)getApplicationContext());
				int idf=ft.getCheckedRadioButtonId();
				Resources res = myapp.getResources();
			    // Change locale settings in the app.
			    DisplayMetrics dm = res.getDisplayMetrics();
			    Log.i("lingua", "mezzo");
			    android.content.res.Configuration conf = res.getConfiguration();			    
				if (idf==R.id.italiano)
				{
					myapp.setLanguage("it");
					conf.locale = new Locale("it");
				}
				else if (idf==R.id.spagnolo)
				{
					myapp.setLanguage("es");
					conf.locale = new Locale("es");
				}
				else if (idf==R.id.francese)
				{
					myapp.setLanguage("fr");
					conf.locale = new Locale("fr");
				}
				else if (idf==R.id.portoghese)
				{
					myapp.setLanguage("pt");
					conf.locale = new Locale("pt");
				}
				else if (idf==R.id.tedesco)
				{
					myapp.setLanguage("de");
					conf.locale = new Locale("de");
				}
				else if (idf==R.id.russo)
				{
					myapp.setLanguage("ru");
					conf.locale = new Locale("ru");
				}
				else if (idf==R.id.cinese)
				{
					myapp.setLanguage("zh");
					conf.locale = new Locale("zh");
				}
				else if (idf==R.id.giapponese)
				{
					myapp.setLanguage("ja");
					conf.locale = new Locale("ja");
				}
				else
				{
					myapp.setLanguage("en");
					conf.locale = new Locale("en");
				}
				res.updateConfiguration(conf, dm);
				Log.i("lingua", "fine");
				SharedPreferences sharedPref = getSharedPreferences("preferenze",Context.MODE_PRIVATE);
		        SharedPreferences.Editor editor = sharedPref.edit();
				editor.putString("lingua",myapp.getLanguage());
				editor.commit();
				Log.i("lingua", "fine conferma");
				myapp.setLinguaAggiornata(false);
				if (myapp.isConnState())
				{
					myapp.setDeviceJustConnected(true);
				}
				finish();
			}			
		});		
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		Log.i("lingua", "resume activity");
		iPowerApplication myapp=((iPowerApplication)getApplicationContext());
		String lg=myapp.getLanguage();
		Log.i("lingua", lg);
		RadioGroup ft=(RadioGroup)findViewById(R.id.language);
		if (lg.equals("it"))
		{
			ft.check(R.id.italiano);
		}
		else if (lg.equals("es"))
		{
			ft.check(R.id.spagnolo);
		}
		else if (lg.equals("fr"))
		{
			ft.check(R.id.francese);
		}
		else if (lg.equals("pt"))
		{
			ft.check(R.id.portoghese);
		}
		else if (lg.equals("de"))
		{
			ft.check(R.id.tedesco);
		}
		else if (lg.equals("ru"))
		{
			ft.check(R.id.russo);
		}
		else if (lg.equals("ja"))
		{
			ft.check(R.id.giapponese);
		}
		else if (lg.equals("zh"))
		{
			ft.check(R.id.cinese);
		}
		else
		{
			ft.check(R.id.inglese);
		}		
		registerReceiver(mGattUpdateReceiver, iPowerApplication.makeGattUpdateIntentFilter());
	}
	
	@Override
	protected void onStop()
	{
		super.onStop();
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.language_tat, menu);
		return false;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
