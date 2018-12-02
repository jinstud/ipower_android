package com.ipower.tattoo;

public class BluetoothRunnable implements Runnable {
    public byte[] buf;

    public BluetoothRunnable(byte[] buf) {
        this.buf = buf;
    }

    public void run() {}
}