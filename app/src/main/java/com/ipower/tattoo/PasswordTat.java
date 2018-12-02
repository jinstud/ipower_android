package com.ipower.tattoo;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class PasswordTat extends Activity {
	
	private byte[] data = new byte[3];
	private boolean attesa_convalida_pswd_attuale=false;
	private boolean attesa_convalida_pswd_nuova=false;
	private String password_nuova,password_attuale;
	private final String carattere_proibito="�";

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
			else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) 
			{
				data = intent.getByteArrayExtra(RBLService.EXTRA_DATA);
				if (attesa_convalida_pswd_attuale)
				{
					readResponsoRichiestaCambioPswdAttuale(data);
				}
				else if (attesa_convalida_pswd_nuova)
				{
					readResponsoRichiestaCambioPswd(data);
				}
				Log.i("data available","da");
			}			
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		iPowerApplication myapp=((iPowerApplication)getApplicationContext());
		myapp.setLanguageCreateActivity();
		setContentView(R.layout.activity_password_tat);
		
		Button imp_psw=(Button)findViewById(R.id.imposta_pswd);
		imp_psw.setOnClickListener(new OnClickListener(){     	
        	@Override
        	public void onClick(View v)
        	{     
        		final Dialog dialog=new Dialog(PasswordTat.this);
        		dialog.setContentView(R.layout.dialog_imposta_pswd);
        		dialog.setTitle(R.string.imposta_pswd);
        		
        		Button conf_pswd=(Button)dialog.findViewById(R.id.conferma_imposta_pswd);
        		conf_pswd.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						EditText et=(EditText)dialog.findViewById(R.id.pswd);
						String pswd=et.getText().toString();
						iPowerApplication myapp=((iPowerApplication)getApplicationContext());
						if (pswd.length()>15)
						{
							myapp.makeToast(getString(R.string.pswd_lunga));
						}
						else if (pswd.length()==0)
						{
							myapp.makeToast(getString(R.string.inserire_pswd));
						}
						else if (pswd.contains(carattere_proibito))
						{
							myapp.makeToast("The character "+carattere_proibito+" is not avalaible");
						}
						else
						{
							myapp.setPassword(pswd);
							SharedPreferences sharedPref = getSharedPreferences("preferenze",Context.MODE_PRIVATE);
							SharedPreferences.Editor editor = sharedPref.edit();
							editor.putString("pswd", pswd);
							editor.commit();
							dialog.dismiss();
						}
					}        			
        		});
        		
        		dialog.show();
        	}
        });
		
		Button cmb_psw=(Button)findViewById(R.id.cambia_pswd);
		cmb_psw.setOnClickListener(new OnClickListener(){     	
        	@Override
        	public void onClick(View v)
        	{     
        		iPowerApplication myapp=((iPowerApplication)getApplicationContext());
        		if (myapp.isConnState())
        		{
	        		final Dialog dialog=new Dialog(PasswordTat.this);
	        		dialog.setContentView(R.layout.dialog_cambio_pswd);
	        		dialog.setTitle(R.string.cambia_pswd);
	        		
	        		Button conf_pswd=(Button)dialog.findViewById(R.id.esegui_cambio_pswd);
	        		conf_pswd.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							EditText et=(EditText)dialog.findViewById(R.id.pswd_attuale);
							String pswd_attuale=et.getText().toString();
							et=(EditText)dialog.findViewById(R.id.pswd_nuova);
							String pswd_nuova=et.getText().toString();
							et=(EditText)dialog.findViewById(R.id.conferma_pswd_nuova);
							String conferma_pswd_nuova=et.getText().toString();
							iPowerApplication myapp=((iPowerApplication)getApplicationContext());
							if (pswd_attuale.length()==0 || pswd_nuova.length()==0 || conferma_pswd_nuova.length()==0)
							{
								myapp.makeToast(getString(R.string.inserire_pswd));								
							}
							else if (pswd_attuale.length()>15 || pswd_nuova.length()>15 || conferma_pswd_nuova.length()>15)
							{
								myapp.makeToast(getString(R.string.pswd_lunga));
							}
							else if (pswd_attuale.contains(carattere_proibito))
							{
								myapp.makeToast("The character "+carattere_proibito+" is not avalaible");
							}
							else if (!pswd_nuova.equals(conferma_pswd_nuova))
							{
								myapp.makeToast(getString(R.string.conferma_pswd_diversa));
							}
							else
							{
								password_nuova=conferma_pswd_nuova;
								password_attuale=pswd_attuale;
								//	procedura di cambio password
								//	trasmissione password attuale per cambio password (id 0x09)
								trasmettiPasswordAttuale();
								//	attesa ricezione conferma (id 0x0A)
								//	trasmissione nuova password (id 0x0D)
								//	attesa ricezione conferma (id 0x0C)
								dialog.dismiss();
							}
						}        			
	        		});
	        		
	        		dialog.show();
	        		
	        		
        		}
        		else
        		{
        			myapp.makeToast(getString(R.string.no_cambio_pswd));
        		}
        	}
        });	
		
		
		Button cam_nom=(Button)findViewById(R.id.cambia_nome);
		cam_nom.setOnClickListener(new OnClickListener(){     	
        	@Override
        	public void onClick(View v)
        	{     
        		iPowerApplication myapp=((iPowerApplication)getApplicationContext());
        		if (myapp.isConnState())
        		{
	        		final Dialog dialog=new Dialog(PasswordTat.this);
	        		dialog.setContentView(R.layout.dialog_cambia_nome);
	        		dialog.setTitle(R.string.cambia_nome);
	        		
	        		Button conf_nome=(Button)dialog.findViewById(R.id.conferma_nuovo_nome);
	        		conf_nome.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							EditText et=(EditText)dialog.findViewById(R.id.nuovo_nome);
							String nm=et.getText().toString();
							iPowerApplication myapp=((iPowerApplication)getApplicationContext());
							if (nm.length()==0)
							{
								myapp.makeToast(getString(R.string.inserire_nome));
							}
							else
							{
								String[] colonne=new String[]{"indirizzo"};
								String whereClause = "indirizzo = ?";
								String[] whereArgs = new String[] {myapp.getmDevice().getAddress()};
								Cursor crs=myapp.lnd.getReadableDatabase()
						        		.query("ListaNomiDeviceDB", colonne, whereClause, whereArgs, null, null, "_id ASC");
								ContentValues cv=new ContentValues();
								cv.put("nome", nm);
								if (crs.getCount()>0)
								{
									myapp.lnd.getWritableDatabase().update("ListaNomiDeviceDB", cv, whereClause, whereArgs);
								}
								else
								{									
									cv.put("indirizzo", myapp.getmDevice().getAddress());
									myapp.lnd.getWritableDatabase().insert("ListaNomiDeviceDB", null, cv);
								}								
								dialog.dismiss();
							}
						}        			
	        		});
	        		
	        		dialog.show();
        		}
        		else
        		{
        			myapp.makeToast(getString(R.string.no_cambio_nome));
        		}
        	}
        });
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		registerReceiver(mGattUpdateReceiver, iPowerApplication.makeGattUpdateIntentFilter());
	}
	
	@Override
	protected void onStop()
	{
		super.onStop();
		unregisterReceiver(mGattUpdateReceiver);
	}
	
	private void trasmettiPasswordAttuale(){
		iPowerApplication myapp=((iPowerApplication)getApplicationContext());
		//String pswd=myapp.getPassword();
		byte buf[] = new byte[] { (byte) 0x09, (byte) 0x00, (byte) 0x00 };
		Random random = new Random();
		myapp.setNumIdTrasm((byte) random.nextInt(256)); // numero identificativo trasmissione
		buf[2]=myapp.getNumIdTrasm();
		buf[0]=(byte)0x09;
		buf[1]=(byte)password_attuale.length();	//	trasmissione numero caratteri password (max=15)
		SystemClock.sleep(myapp.getMSDELAYBT());
		myapp.sendBluetoothData(buf);
		int i;
		for (i=0;i<password_attuale.length();i++)
		{
			SystemClock.sleep(myapp.getMSDELAYBT());
			buf[0]=(byte)(((i+1)<<4)+9);	//	codice trasmissione password e numero pacchetto
			buf[1]=(byte)password_attuale.charAt(i);	//	trasmissione carattere password
			myapp.sendBluetoothData(buf);
		}
		Log.i("debug galasso pswd","paswd attuale trasmessa: "+password_attuale);
		attesa_convalida_pswd_attuale=true;
		// se il responso non arriva in tempi ragionevoli bisogna chiudere la trasmissione
		//	avvio di un timer di 10 secondi entro il quale deve essere arrivata la risposta  
		Timer mTimer = new Timer();
		mTimer.schedule
		(
			new TimerTask() 
			{
				@Override
				public void run() 
				{
					iPowerApplication myapp=((iPowerApplication)getApplicationContext());
					if (attesa_convalida_pswd_attuale)
					{
						//myapp.mBluetoothLeService.disconnect();
						//myapp.mBluetoothLeService.close();
						//setButtonDisable();
						myapp.makeToast("Conferma password attuale non ricevuta");
						Log.i("debug galasso pswd","conferma password attuale non ricevuta");
						attesa_convalida_pswd_attuale=false;
					}
				}
			}, 10000);
	}

	private void readResponsoRichiestaCambioPswdAttuale(byte[] data) 
	{
		// ricezione responso
		// se il responso � negativo bisogna chiudere la trasmissione		
		// se il responso � positivo pu� continuare la trasmissione
		for (int i = 0; i < data.length; i += 3) 
		{
			if (data[i] == 0x0A) // codice responso
			{
				iPowerApplication myapp=((iPowerApplication)getApplicationContext());
				if (data[i + 2]==myapp.getNumIdTrasm())
				{
					attesa_convalida_pswd_attuale=false;
					if (data[i + 1]==myapp.getCONFERMA())	// conferma identificazione
					{
						Log.i("debug galasso pswd","conferma identificazione per cambio pswd");
						trasmettiPasswordNuova();
					}
					else	// rifiuto identificazione
					{
						Log.i("debug galasso pswd","rifiuto identificazione per cambio paswd");
						myapp.bluetoothLeService.disconnect();
						myapp.bluetoothLeService.close();
						myapp.makeToast("Password attuale non riconosciuta");
						//setButtonDisable();
					}
					//myapp.setAttesaConvalidaPswdAttuale(false);					
				}
			} 
		}
	}
	
	private void trasmettiPasswordNuova(){
		iPowerApplication myapp=((iPowerApplication)getApplicationContext());
		byte buf[] = new byte[] { (byte) 0x0D, (byte) 0x00, (byte) 0x00 };
		Random random = new Random();
		myapp.setNumIdTrasm((byte) random.nextInt(256)); // numero identificativo trasmissione
		buf[2]=myapp.getNumIdTrasm();
		buf[0]=(byte)0x0D;
		buf[1]=(byte)password_nuova.length();	//	trasmissione numero caratteri password (max=15)
		SystemClock.sleep(myapp.getMSDELAYBT());
		myapp.sendBluetoothData(buf);
		int i;
		for (i=0;i<password_nuova.length();i++)
		{
			SystemClock.sleep(myapp.getMSDELAYBT());
			buf[0]=(byte)(((i+1)<<4)+13);	//	codice trasmissione password e numero pacchetto
			buf[1]=(byte)password_nuova.charAt(i);	//	trasmissione carattere password
			myapp.sendBluetoothData(buf);
		}
		Log.i("debug galasso pswd","paswd nuova trasmessa: "+password_nuova);
		attesa_convalida_pswd_nuova=true;
		// se il responso non arriva in tempi ragionevoli bisogna chiudere la trasmissione
		//	avvio di un timer di 10 secondi entro il quale deve essere arrivata la risposta  
		Timer mTimer = new Timer();
		mTimer.schedule
		(
			new TimerTask() 
			{
				@Override
				public void run() 
				{
					iPowerApplication myapp=((iPowerApplication)getApplicationContext());
					if (attesa_convalida_pswd_nuova)
					{
						//myapp.mBluetoothLeService.disconnect();
						//myapp.mBluetoothLeService.close();
						//setButtonDisable();
						attesa_convalida_pswd_nuova=false;
						//dialog.dismiss();
						myapp.makeToast("Conferma password nuova non ricevuta");
						Log.i("debug galasso pswd","conferma password nuova non ricevuta");
					}
				}
			}, 10000);
	}
	
	private void readResponsoRichiestaCambioPswd(byte[] data) 
	{
		// ricezione responso
		// se il responso � negativo bisogna chiudere la trasmissione		
		// se il responso � positivo pu� continuare la trasmissione
		for (int i = 0; i < data.length; i += 3) 
		{
			if (data[i] == 0x0C) // codice responso
			{
				iPowerApplication myapp=((iPowerApplication)getApplicationContext());
				if (data[i + 2]==myapp.getNumIdTrasm())
				{
					if (data[i + 1]==myapp.getCONFERMA())	// conferma identificazione
					{
						myapp.setPassword(password_nuova);
						SharedPreferences sharedPref = getSharedPreferences("preferenze",Context.MODE_PRIVATE);
						SharedPreferences.Editor editor = sharedPref.edit();
						editor.putString("pswd", password_nuova);
						editor.commit();
						myapp.makeToast("Cambio password effettuato correttamente");
						Log.i("debug galasso pswd","Cambio password effettuato correttamente");
					}
					else	// rifiuto identificazione
					{
						//myapp.mBluetoothLeService.disconnect();
						//myapp.mBluetoothLeService.close();
						myapp.makeToast("Password nuova non settata");
						Log.i("debug galasso pswd","Password nuova non settata");
						//setButtonDisable();
					}
					//myapp.setAttesaConvalidaPswdAttuale(false);
					attesa_convalida_pswd_nuova=false;
				}
			} 
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.password_tat, menu);
		return false;
	}

}
