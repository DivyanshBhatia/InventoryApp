package com.android.udacity.course10.inventoryapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.android.udacity.course10.inventoryapp.data.InventoryContract.InventoryEntry;
/**
 * Created by dnbhatia on 11/15/2016.
 */
public class InventoryDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME="inventory.db";
    private static final int DATABASE_VERSION=1;
    public InventoryDbHelper(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_INVENTORY_TABLE="CREATE TABLE "+ InventoryEntry.TABLE_NAME+ "("+InventoryEntry._id +" INTEGER PRIMARY KEY AUTOINCREMENT, "+
                InventoryEntry.COLUMN_INVENTORY_NAME+" TEXT NOT NULL, "+
                InventoryEntry.COLUMN_INVENTORY_SUPPLIER+" TEXT, "+
                InventoryEntry.COLUMN_INVENTORY_PRICE+" TEXT, "+
                InventoryEntry.COLUMN_INVENTORY_QTY_AVLBL+" INTEGER NOT NULL DEFAULT 0,"+
                InventoryEntry.COLUMN_INVENTORY_PIC_PATH+" BLOB,"+
                InventoryEntry.COLUMN_INVENTORY_SUPPLIER_EMAIL+" TEXT, "+
                InventoryEntry.COLUMN_INVENTORY_SUPPLIER_PHONE+" TEXT);";
        db.execSQL(SQL_CREATE_INVENTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
