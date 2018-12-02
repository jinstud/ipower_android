package com.ipower.tattoo;

import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
//import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

public class Foot extends Activity {

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() 
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
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
		setContentView(R.layout.activity_foot);
		
		Button cf=(Button)findViewById(R.id.conferma_foot);
		cf.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				RadioGroup ft=(RadioGroup)findViewById(R.id.foot);
				iPowerApplication myapp=((iPowerApplication)getApplicationContext());
				int idf=ft.getCheckedRadioButtonId();
				byte buf[] = new byte[] {(byte) 0x03, (byte) 0x00, (byte) 0x55};
				if (idf==R.id.continuous)
				{
					myapp.setModFoot("continuous");
					buf[0]=0x13;
				}
				else if (idf==R.id.toggle)
				{
					myapp.setModFoot("toggle");
					buf[0]=0x53;
				}
				else
				{
					myapp.setModFoot("toggle_phone");
					buf[0]=0x73;
				}
				SharedPreferences sharedPref = getSharedPreferences("preferenze",Context.MODE_PRIVATE);
		        SharedPreferences.Editor editor = sharedPref.edit();
				editor.putString("foot",myapp.getModFoot());
				editor.commit();
				
				myapp.sendBluetoothData(buf);
				finish();
			}			
		});		
		
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		iPowerApplication myapp=((iPowerApplication)getApplicationContext());
		String nm=myapp.getModFoot();
		RadioGroup ft=(RadioGroup)findViewById(R.id.foot);
		if (nm.equals("continuous"))
		{
			ft.check(R.id.continuous);
		}
		else if (nm.equals("toggle"))
		{
			ft.check(R.id.toggle);
		}
		else
		{
			ft.check(R.id.toggle_phone);
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
		getMenuInflater().inflate(R.menu.foot, menu);
		return false;
	}
}
