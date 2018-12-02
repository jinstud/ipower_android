package com.ipower.tattoo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class ListaNomiDevice extends SQLiteOpenHelper{
	private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "ListaNomiDeviceDB";

    ListaNomiDevice(Context context) {
        super(context, TABLE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
		String	sql	="";
		sql	+=	"CREATE	TABLE "+TABLE_NAME+" (";
		sql	+=	"_id		INTEGER	PRIMARY	KEY,";
		sql	+=	"indirizzo		TEXT	NOT	NULL,";
		sql	+=	"nome			TEXT	NOT	NULL";
		sql	+=	")";
   	 	db.execSQL(sql);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db,int	 oldVersion, int newVersion)
    {
    	
    }
}
