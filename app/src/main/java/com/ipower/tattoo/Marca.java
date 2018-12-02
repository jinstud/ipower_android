package com.ipower.tattoo;

public class Marca {
	private String marca;
	private float volt;
	private String ls;
	private String stazione;
	
	public Marca(String marca,float volt, String ls,String sez){
		this.setMarca(marca);
		this.setVolt(volt);
		this.setLs(ls);
		this.setStazione(sez);
	}
	
	public String getMarca() {
		return marca;
	}
	public void setMarca(String marca) {
		this.marca = marca;
	}

	public float getVolt() {
		return volt;
	}

	public void setVolt(float volt) {
		this.volt = volt;
	}

	public String getLs() {
		return ls;
	}

	public void setLs(String ls) {
		this.ls = ls;
	}

	public String getStazione() {
		return stazione;
	}

	public void setStazione(String stazione) {
		this.stazione = stazione;
	}
}
