package com.ipower.tattoo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ListaMarcheOpenHelper extends SQLiteOpenHelper{
	private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "ListaMarcheDB";

    ListaMarcheOpenHelper(Context context) {
        super(context, TABLE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
		String	sql	="";
		sql	+=	"CREATE	TABLE "+TABLE_NAME+" (";
		sql	+=	"_id		INTEGER	PRIMARY	KEY,";
		sql	+=	"marca		TEXT	NOT	NULL,";
		sql	+=	"volt		REAL	NOT	NULL,";
		sql	+=	"ls			TEXT	NOT	NULL,";
		sql	+=	"stazione	TEXT	NOT	NULL";
		sql	+=	")";
   	 	db.execSQL(sql);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db,int	 oldVersion, int newVersion)
    {
    	
    }

}
