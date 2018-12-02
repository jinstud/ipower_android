package com.ipower.tattoo;

import java.util.LinkedList;
import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SettingTat extends Activity {

	private List<Marca> list;	//	lista di marche di aghi
	public MarcaAdapter ma;		//	adapter per gestire la lista di marche
	private ListView lv;		//	oggetto listview al quale associare la lista
	
	private float Vdf;
	
	private static final int ADD_MARCA=0;
	private static final int MODIFICA_MARCA=1;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting_tat);
		lv = (ListView)findViewById(R.id.lista_marche);
		list = new LinkedList<Marca>();
		ma = new MarcaAdapter(this,R.layout.riga_marca,list);	
        lv.setAdapter(ma);
        Cursor crs=((iPowerApplication)getApplicationContext()).lmoh.getReadableDatabase()
        		.query("ListaMarcheDB", null, null, null, null, null, "_id ASC");
        while (crs.moveToNext())
		{
			ma.add(new Marca(crs.getString(1),crs.getFloat(2),crs.getString(3),crs.getString(4)));
		}
        registerForContextMenu(lv);             
        
        Button addMarca = (Button) findViewById(R.id.add_marca);        
        addMarca.setOnClickListener(new OnClickListener(){     	
        	@Override
        	public void onClick(View v)
        	{     
        		creaDialogMarca(ADD_MARCA,0);        		
        	}
        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.setting_tat, menu);
		return false;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v,ContextMenu.ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		getMenuInflater().inflate(R.menu.lista_marche, menu);
	}
	
	public boolean onContextItemSelected(MenuItem item) 
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) 
		{
			case R.id.rimuovi_marca:
				deleteMarca(info.position);				
				return true;
			case R.id.modifica_marca:
				creaDialogMarca(MODIFICA_MARCA,info.position);
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}
	
	void createDialogLS(final int index_item){
		AlertDialog.Builder builder = new AlertDialog.Builder(SettingTat.this);
	    builder.setTitle(R.string.scegli_ls)
	           .setItems(R.array.scegli_ls, new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int which) {
	               // The 'which' argument contains the index position
	               // of the selected item
	            	   	Marca mr=ma.getItem(index_item);
	            	   	int ons;
	            	    if (mr.getStazione().equals(""))
	            	    {
	            	    	ons=0;
	            	    }
	            	    else
	            	    {
	            	    	ons=Integer.parseInt(mr.getStazione());
	            	    }
	            	   	if (which==0)
	            	   		modMarca(index_item,new Marca(mr.getMarca(),mr.getVolt(),"L1",mr.getStazione()));
						else if (which==1)
							modMarca(index_item,new Marca(mr.getMarca(),mr.getVolt(),"L2",mr.getStazione()));
	            	    iPowerApplication myapp=((iPowerApplication)getApplicationContext());
	            	    if (ons==myapp.getNumStazioneL())
	            	    {
	            	    	myapp.setNumStazioneL(0);
	            	    }
	               }	               
	    });
	    AlertDialog alert=builder.create();
   		alert.show();
	}
	
	void createDialogStazione(final int index_item){
		AlertDialog.Builder builder = new AlertDialog.Builder(SettingTat.this);
	    builder.setTitle(R.string.scegli_stazione)   
	           .setItems(R.array.scegli_stazione, new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int which) {
	               // The 'which' argument contains the index position
	               // of the selected item
	            	    String ns=String.valueOf(which+1);	
	            	    int nel=ma.getCount();
	            	    Marca mr=ma.getItem(index_item);	            	    
	            	    int ons;
	            	    if (mr.getStazione().equals(""))
	            	    {
	            	    	ons=0;
	            	    }
	            	    else
	            	    {
	            	    	ons=Integer.parseInt(mr.getStazione());
	            	    }
	            	    int i;	            	    
	            	    for (i=0;i<nel;i++)
	            	    {  
	            	    	mr=ma.getItem(i);	            	    	
	            	    	if (ns.equals(mr.getStazione()))
	            	    	{
	            	    		modMarca(i,new Marca(mr.getMarca(),mr.getVolt(),mr.getLs(),""));
	            	    	}
	            	    }	            	    
	            	    mr=ma.getItem(index_item);
	            	    //mr.setStazione(ns);
	            	    modMarca(index_item,new Marca(mr.getMarca(),mr.getVolt(),mr.getLs(),ns));
	            	    iPowerApplication myapp=((iPowerApplication)getApplicationContext());
	            	    if (which+1==myapp.getNumStazioneL() || ons==myapp.getNumStazioneL())
	            	    {
	            	    	myapp.setNumStazioneL(0);
	            	    }    	    
	               }	               
	    });
	    AlertDialog alert=builder.create();
   		alert.show();
	}
	
	private void creaDialogMarca(final int operazione,final int index_item){
		//ready=false;
		final Dialog dialog=new Dialog(SettingTat.this);
		dialog.setContentView(R.layout.dialog_new_marca);
		dialog.setTitle(R.string.lb_new_marca);
		
		SeekBar sb_volt = (SeekBar)dialog.findViewById(R.id.sb_volt_marca);
        sb_volt.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
				// TODO Auto-generated method stub
				int np=seekBar.getMax();
				iPowerApplication myapp=((iPowerApplication)getApplicationContext());
				float pas=(myapp.getVmax()-myapp.getVmin())/(float)np;
				Vdf=myapp.getVmin()+pas*(float)progress;
				TextView new_volt = (TextView)dialog.findViewById(R.id.new_volt);
				//new_volt.setText(String.valueOf(Vdf)+" V");
				new_volt.setText(String.format("%.2f V",Vdf));
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}        	
        }); 
		
		EditText nomeMarca=(EditText)dialog.findViewById(R.id.new_nome_marca);
		switch (operazione)
		{
		case ADD_MARCA:
			sb_volt.setProgress(sb_volt.getMax()/2);
			nomeMarca.setText("");
			//staz_ok=false;
			//ls_ok=false;
			break;
		case MODIFICA_MARCA:
			Marca mr=ma.getItem(index_item);			
			nomeMarca.setText(mr.getMarca());
			iPowerApplication myapp=((iPowerApplication)getApplicationContext());
			float pas=(myapp.getVmax()-myapp.getVmin())/(float)sb_volt.getMax();
			sb_volt.setProgress((int)((mr.getVolt()-myapp.getVmin())/pas));
			break;
		}                           		
		
		Button canc = (Button)dialog.findViewById(R.id.cancel_mod);
		canc.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				SeekBar sb_volt=(SeekBar)dialog.findViewById(R.id.sb_volt_marca);
				sb_volt.setProgress(sb_volt.getMax()/2);
				EditText nomeMarca = (EditText)dialog.findViewById(R.id.new_nome_marca);
				nomeMarca.setText("");
			}
		});		
        
        Button conf=(Button)dialog.findViewById(R.id.conferma_marca);
        conf.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View v){          		
        		EditText nomeMarca = (EditText)dialog.findViewById(R.id.new_nome_marca);
        		String nmm=nomeMarca.getText().toString();
        		if (nmm.length()<=0)
        		{
        			AlertDialog.Builder builder=new AlertDialog.Builder(SettingTat.this);
            		builder.setTitle("Avviso");
            		builder.setMessage("Inserire il nome della marca");
            		builder.setCancelable(true);
            		AlertDialog alert=builder.create();
            		alert.show();
        		}
        		else
        		{        			
        			String ls="L1",nstaz="";
        			switch (operazione)
        			{
        			case ADD_MARCA:
        				addMarca(new Marca(nmm,Vdf,ls,nstaz));        				
        				break;
        			case MODIFICA_MARCA:
        				Marca mr=ma.getItem(index_item);
        				modMarca(index_item,new Marca(nmm,Vdf,mr.getLs(),mr.getStazione()));       				
        				break;
        			}        			
        			dialog.dismiss();
        		}
        	}
        });		
		dialog.show();
	}
	
	private void addMarca(Marca mr)
	{
		ma.add(mr);
		ContentValues cv=new ContentValues();
		cv.put("marca", mr.getMarca());
		cv.put("volt", mr.getVolt());
		cv.put("ls", mr.getLs());
		cv.put("stazione", mr.getStazione());
		((iPowerApplication)getApplicationContext())
		.lmoh.getWritableDatabase().insert("ListaMarcheDB", null, cv);
	}
	
	private void deleteMarca(int index)
	{
		Marca omr=ma.getItem(index);
		ma.remove(omr);
		String	whereClause	= "marca=?";				
		String[] whereArgs= {omr.getMarca()};
		iPowerApplication myapp=((iPowerApplication)getApplicationContext());
		myapp.lmoh.getWritableDatabase().delete("ListaMarcheDB", whereClause, whereArgs);
		myapp.setMarcaStazione(omr.getStazione(), null);
	}
	
	private void modMarca(int index,Marca mr)
	{
		deleteMarca(index);
		ma.insert(mr, index);
		ContentValues cv=new ContentValues();
		cv.put("marca", mr.getMarca());
		cv.put("volt", mr.getVolt());
		cv.put("ls", mr.getLs());
		cv.put("stazione", mr.getStazione());
		iPowerApplication myapp=((iPowerApplication)getApplicationContext());
		myapp.lmoh.getWritableDatabase().insert("ListaMarcheDB", null, cv);
		myapp.setMarcaStazione(mr.getStazione(), mr);
	}
}


