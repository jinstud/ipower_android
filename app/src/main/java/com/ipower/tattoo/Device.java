package com.ipower.tattoo;

import android.bluetooth.BluetoothDevice;

public class Device {
	private BluetoothDevice device;
	private String name = null;
	
	public Device(BluetoothDevice device) {
        this.device = device;
	}
	
	public BluetoothDevice getDevice(){
        return this.device;
	}
	
	public String getName(){
		if (name == null) {
			return device.getName();
		} else {
			return name;
		}
	}
	
	public void setName(String name){
        this.name = name;
	}
	
	public String getAddress(){
        return device.getAddress();
	}
}
