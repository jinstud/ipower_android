package com.ipower.tattoo;

import java.util.LinkedList;
import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.ListView;

public class DevicesListActivity extends Activity {

	private List<Device> list;
	//public DeviceAdapter da;
	//private ListaMarcheOpenHelper lmoh=((iPowerApplication)getApplicationContext()).lmoh;
	private ListView lv;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		iPowerApplication app = (iPowerApplication)getApplicationContext();
		app.setLanguageCreateActivity();
		setContentView(R.layout.activity_devices_list);
		
		lv = (ListView)findViewById(R.id.lista_device);
		list = new LinkedList<Device>();
        app.setDeviceAdapter(new DeviceAdapter(this,R.layout.riga_device,list));
        app.getDeviceAdapter().clear();
        lv.setAdapter(app.getDeviceAdapter());

        //registerForContextMenu(lv);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.lista_device_tata, menu);
		return false;
	}
	
	void connettiDevice(final int index_item){
		iPowerApplication app=((iPowerApplication)getApplicationContext());
        app.setmDevice(app.getDeviceAdapter().getItem(index_item).getDevice());
        app.setDeviceJustConnected(true);
		finish();
	}
}
