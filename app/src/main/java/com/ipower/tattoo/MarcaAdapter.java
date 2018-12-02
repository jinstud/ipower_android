package com.ipower.tattoo;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class MarcaAdapter extends ArrayAdapter<Marca> {
	
	private Context contextActivity;
	
	public MarcaAdapter(Context context, int textViewResourceId, List<Marca> objects) {
		super(context, textViewResourceId, objects);
		contextActivity=context;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		   if (convertView == null) {
		       LayoutInflater inflater = (LayoutInflater) getContext()
		                 .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		       convertView = inflater.inflate(R.layout.riga_marca, null);
		       viewHolder = new ViewHolder();
		       viewHolder.marca = (TextView)convertView.findViewById(R.id.item_marca);
		       viewHolder.volt = (TextView)convertView.findViewById(R.id.volt_marca);
		       viewHolder.ls = (Button)convertView.findViewById(R.id.sel_LS);
		       viewHolder.stazione = (Button)convertView.findViewById(R.id.sel_stazione);
		       
		       convertView.setTag(viewHolder);
		       convertView.setLongClickable(true);
		   } else {
		       viewHolder = (ViewHolder) convertView.getTag();
		   }
		   viewHolder.ls.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					((SettingTat)contextActivity).createDialogLS(position);
				}		    	   
	       });
	       viewHolder.stazione.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					((SettingTat)contextActivity).createDialogStazione(position);
				}		    	   
	       });
		   Marca marca = getItem(position);
		   viewHolder.marca.setText(marca.getMarca());
		   //viewHolder.volt.setText(String.valueOf(marca.getVolt())+" V");
		   viewHolder.volt.setText(String.format("%.2f V",marca.getVolt()));		   
		   viewHolder.ls.setText(marca.getLs());
		   viewHolder.stazione.setText(marca.getStazione());
		   return convertView;
	}

	private class ViewHolder {
	   public TextView 	marca;
	   public TextView 	volt;
	   public Button	ls;
	   public Button	stazione;
	}

}
