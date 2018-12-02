package com.ipower.tattoo;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class DeviceAdapter extends ArrayAdapter<Device> {

    private Context context;
	
	public DeviceAdapter(Context context, int textViewResourceId, List<Device> objects) {
		super(context, textViewResourceId, objects);
		this.context = context;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)getContext()
                 .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_found_device, null);
            viewHolder = new ViewHolder();
            viewHolder.name = (TextView)convertView.findViewById(R.id.name);
            viewHolder.address = (TextView)convertView.findViewById(R.id.address);
            viewHolder.address = (TextView)convertView.findViewById(R.id.address);
            viewHolder.connect = (Button)convertView.findViewById(R.id.buttonConnect);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.connect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)context).connectToDevice(position);
            }
        });

        Device device = getItem(position);
        viewHolder.name.setText(device.getName());
        viewHolder.address.setText(device.getAddress());

        return convertView;
	}

	private class ViewHolder {
	   public TextView name;
	   public TextView address;
	   public Button connect;
	}
}
