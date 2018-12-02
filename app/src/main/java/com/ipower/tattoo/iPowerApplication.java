package com.ipower.tattoo;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;

public class iPowerApplication extends Application {

    public static Context context;

    private ArrayList<byte[]> queue = new ArrayList<byte[]>();

    public void onCreate() {
        super.onCreate();
        iPowerApplication.context = getApplicationContext();
    }

	private static final float Vmin = (float)4.32;			//	Tensione di uscita minima prevista
	private static final float Vmax = (float)19.55;		//	Tensione di uscita massima prevista
	private final int NSTAZ = 5;			//	numero di stazioni
	private final byte CONFERMA = 0x44;	//	codice conferma
	private final byte RIFIUTO = 0x0f;	//	codice rifiuto
	private final int MSDELAYBT = 200;	//	ritardo tra due trasmissioni b�uetooth di byte consecutive in ms
	public ListaMarcheOpenHelper lmoh = new ListaMarcheOpenHelper(this);	//	database delle marche/modelli di ago
	public ListaNomiDevice lnd = new ListaNomiDevice(this);	//	database di nomi dispositivi associati agli indirizzi MAC
	private Marca[] mrStz = new Marca[NSTAZ];	//	array di Marche/modelli per le diverse stazioni di scelta rapida
	private int numStazioneL = 0;
	private boolean connState = false;	//	flag di stato per la connessione con un dispositivo
	private String password;			//	password selezionata per l'autenticazione
	private byte numIdTrasm;			//	numero identificativo della trasmissione bluetooth in corso
	private String modFoot;				//	modalit� pedale
	private boolean footToggleOn=false;		//	true se si � in modalit� toggle e l'ago � alimentato 	
	private String language;				//	lingua selezionata
	private boolean linguaAggiornata=false;
	private DeviceAdapter da;			//	oggetto DeviceAdapter per gestire la lista di dispositivi Bluetooth
	private BluetoothDevice mDevice = null;	//	device a cui si � connessi o a cui connettersi
	private boolean deviceJustConnected = false;	//	flag che informa se si� appena scelto un device al quale connettersi
	
	public BluetoothGattCharacteristic characteristicTx = null;

	public RBLService bluetoothLeService;	//	oggetto service bluetooth
	
	public static IntentFilter makeGattUpdateIntentFilter() 	//	filtro sugli eventi bluetoth ricevuti 
    {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(RBLService.ACTION_GATT_RSSI);
		return intentFilter;
	}
	
	public void sendBluetoothData(byte[] buf) {

		if (isConnState()) {

            //SystemClock.sleep(getMSDELAYBT());
            //characteristicTx.setValue(buf);
            //bluetoothLeService.writeCharacteristic(characteristicTx);

            queue.add(buf.clone());

            //Log.i("DEBUG0", "byte Tx: "+ iPowerApplication.byteToHex(buf[0])+String.valueOf((char)buf[1])+ iPowerApplication.byteToHex(buf[2]));

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    byte[] buf = queue.remove(0);
                    //Log.i("DEBUG", "byte Tx: "+ iPowerApplication.byteToHex(buf[0])+String.valueOf((char)buf[1])+ iPowerApplication.byteToHex(buf[2]));
                    characteristicTx.setValue(buf);
                    bluetoothLeService.writeCharacteristic(characteristicTx);
                }
            }, getMSDELAYBT() * queue.size());
		}
	}
	
	public Marca getMarcaStazione(int index){
		return mrStz[index];
	}
	
	public void setMarcaStazione(int index,Marca mr){
		mrStz[index]=mr;
	}
	
	public void setMarcaStazione(String nst,Marca mr){
		if (!nst.equals(""))
			mrStz[Integer.parseInt(nst)-1]=mr;
	}
	
	public byte getCONFERMA() {
		return CONFERMA;
	}

	public byte getRIFIUTO() {
		return RIFIUTO;
	}

	public int getMSDELAYBT() {
		return MSDELAYBT;
	}

	public static float getVmin() {
		return Vmin;
	}
	public static float getVmax() {
		return Vmax;
	}

	public int getNumStazioneL() {
		return numStazioneL;
	}

	public void setNumStazioneL(int numStazioneL) {
		this.numStazioneL = numStazioneL;
	}

	public int getNstaz() {
		return NSTAZ;
	}
	
	public boolean isConnState() {
		return connState;
	}

	public void setConnState(boolean connState) {
		this.connState = connState;
	}

	public String getModFoot() {
		return modFoot;
	}

	public void setModFoot(String modFoot) {
		this.modFoot = modFoot;
	}

	public boolean isFootToggleOn() {
		return footToggleOn;
	}

	public void setFootToggleOn(boolean footToggleOn) {
		this.footToggleOn = footToggleOn;
	}

	public String getPassword() {
		return password;
	}
	
	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public void setPassword(String password) {
		this.password = password;		
	}

	public byte getNumIdTrasm() {
		return numIdTrasm;
	}

	public void setNumIdTrasm(byte numIdTrasm) {
		this.numIdTrasm = numIdTrasm;
	}

    public static void makeToast(String textToDisplay) {
		Toast toast = Toast.makeText(context,
		    textToDisplay,
		    Toast.LENGTH_SHORT);
        //toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
	}

	public static void makeToast(int resourceId) {
		Toast toast = Toast.makeText(context,
                context.getResources().getString(resourceId),
		    Toast.LENGTH_SHORT);
        //toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
	}
	
	public static String byteArrayToHex(byte[] a) {
	   StringBuilder sb = new StringBuilder(a.length * 2);
	   for(byte b: a)
	      sb.append(String.format("%02x", b & 0xff));
	   return sb.toString();
	}
	
	public static String byteToHex(byte a) {
	   StringBuilder sb = new StringBuilder(2);		   
	   sb.append(String.format("%02x", a & 0xff));
	   return sb.toString();
	}
	
	public static int unsignedByte(byte a) {
		int v = (int)a;
		if (v < 0) {
			v = 256 + v;
		}
		return v;
	}
	
	public DeviceAdapter getDeviceAdapter() {
		return da;
	}

	public void setDeviceAdapter(DeviceAdapter da) {
		this.da = da;
	}

	public boolean deviceExists(BluetoothDevice device) {
		String deviceAddress = device.getAddress();
		boolean exists = false;
		for (int i=0; i < da.getCount(); i++){
			if (da.getItem(i).getAddress().equals(deviceAddress)){
                exists = true;
				break;
			}
		}
		return exists;
	}

	public BluetoothDevice getmDevice() {
		return mDevice;
	}

	public void setmDevice(BluetoothDevice mDevice) {
		this.mDevice = mDevice;
	}

	public boolean isDeviceJustConnected() {
		return deviceJustConnected;
	}

	public void setDeviceJustConnected(boolean deviceJustConnected) {
		this.deviceJustConnected = deviceJustConnected;
	}
	
	public void aggiornaLingua(Context context) {
	    Locale locJa = new Locale(getLanguage().trim());
	    Locale.setDefault(locJa);

	    Configuration config = new Configuration();
	    config.locale = locJa;

	    context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());

	    locJa = null;
	    config = null;
	}

	public boolean isLinguaAggiornata() {
		return linguaAggiornata;
	}

	public void setLinguaAggiornata(boolean linguaAggiornata) {
		this.linguaAggiornata = linguaAggiornata;
	}
	
	public String setLanguageCreateActivity(){
		String ln=getLanguage();//Log.i("lingua","v1 "+lingua);
		/*Resources res = myapp.getResources();
	    DisplayMetrics dm = res.getDisplayMetrics();
	    android.content.res.Configuration conf = res.getConfiguration();
	    conf.locale = new Locale(lingua);
		res.updateConfiguration(conf, dm);*/
		
		Locale myLocale = new Locale(ln); 
	    Resources res = getResources(); 
	    DisplayMetrics dm = res.getDisplayMetrics(); 
	    Configuration conf = res.getConfiguration(); 
	    conf.locale = myLocale; 
	    res.updateConfiguration(conf, dm);
	    return ln;
	}

    public static float seekBarToVolts(SeekBar seekBar) {
        float pas = (getVmax() - getVmin()) / seekBar.getMax();
        return (getVmin() + pas * seekBar.getProgress());
    }

    public static float intToVolts(int value) {
        float pas = (getVmax() - getVmin()) / 127;
        return (getVmin() + pas * value);
    }

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    private static GoogleApiClient googleApiClient = null;

    public static void setGoogleApiClient(GoogleApiClient client) {
        googleApiClient = client;
    }

    public static GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }
	
}
