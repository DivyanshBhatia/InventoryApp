package com.android.udacity.course10.inventoryapp.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by dnbhatia on 11/13/2016.
 */

public class InventoryContract {

    private InventoryContract(){}
    public static final String CONTENT_AUTHORITY = "com.android.udacity.course10.inventoryapp";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_INVENTORY = "inventory";

    public static final class InventoryEntry implements BaseColumns {
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_INVENTORY;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_INVENTORY;
        public final static String TABLE_NAME="inventory";
        public final static String _id=BaseColumns._ID;
        public final static String COLUMN_INVENTORY_NAME="name";
        public final static String COLUMN_INVENTORY_PIC_PATH="pic_path";
        public final static String COLUMN_INVENTORY_PRICE="price";
        public final static String COLUMN_INVENTORY_SUPPLIER="supplier";
        public final static String COLUMN_INVENTORY_QTY_AVLBL="inventory_qty";
        public static final String COLUMN_INVENTORY_SUPPLIER_EMAIL = "supplier_email" ;
        public static final String COLUMN_INVENTORY_SUPPLIER_PHONE = "supplier_phone" ;
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_INVENTORY);

    }
}
