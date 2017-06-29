package com.android.udacity.course10.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.android.udacity.course10.inventoryapp.data.InventoryContract.InventoryEntry;

/**
 * Created by dnbhatia on 11/15/2016.
 */

public class InventoryProvider extends ContentProvider {
    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();
    private InventoryDbHelper mDbHelper;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int ITEMS = 100;
    private static final int ITEM_ID = 101;

    static {
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY, ITEMS);
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY + "/#", ITEM_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case ITEM_ID:
                selection = InventoryEntry._id + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI" + uri);
        }
        Log.v(LOG_TAG, "query:" + uri);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return insertItem(uri, contentValues);
            default:
                throw new IllegalArgumentException("Cannot query unknown URI" + uri);
        }
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        //This method is used for deleting items
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsDeleted = 0;
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, s, strings);
                break;
            case ITEM_ID:
                // Delete a single row given by the ID in the URI
                s = InventoryEntry._ID + "=?";
                strings = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, s, strings);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);

        }
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return updateItem(uri, contentValues, selection, selectionArgs);
            case ITEM_ID:
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateItem(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }

    }


    private int updateItem(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // check that the name value is not null.
        if (values.containsKey(InventoryEntry.COLUMN_INVENTORY_NAME)) {
            String name = values.getAsString(InventoryEntry.COLUMN_INVENTORY_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Inventory requires a name");
            }
        }

        // check that the weight value is valid.
        if (values.containsKey(InventoryEntry.COLUMN_INVENTORY_QTY_AVLBL)) {
            // Check that the weight is greater than or equal to 0 kg
            Integer qtyAvlbl = values.getAsInteger(InventoryEntry.COLUMN_INVENTORY_QTY_AVLBL);
            if (qtyAvlbl != null && qtyAvlbl < 0) {
                throw new IllegalArgumentException("Inventory requires valid qty");
            }
        }
        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsUpdated = database.update(InventoryEntry.TABLE_NAME, values, selection, selectionArgs);
        Log.v(LOG_TAG, "Rows:" + rowsUpdated);
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        // Returns the number of database rows affected by the update statement
        return rowsUpdated;
    }

    private Uri insertItem(Uri uri, ContentValues values) {
        // Check that the name is not null
        String name = values.getAsString(InventoryEntry.COLUMN_INVENTORY_NAME);
        Integer qty = values.getAsInteger(InventoryEntry.COLUMN_INVENTORY_QTY_AVLBL);
        String price = values.getAsString(InventoryEntry.COLUMN_INVENTORY_PRICE);
        String supplier = values.getAsString(InventoryEntry.COLUMN_INVENTORY_SUPPLIER);
        String supplierEmail = values.getAsString(InventoryEntry.COLUMN_INVENTORY_SUPPLIER_EMAIL);
        String supplierPhone = values.getAsString(InventoryEntry.COLUMN_INVENTORY_SUPPLIER_PHONE);

        if (name == null) {
            throw new IllegalArgumentException("Inventory Item requires a name");
        } else if (qty == null || (qty != null && qty < 0)) {
            throw new IllegalArgumentException("Inventory Item qty invalid requires a min qty of 0");
        } else if (price == null || !isInteger(price)) {
            throw new IllegalArgumentException("Inventory Item Price should be integer");
        } else if (supplier == null) {
            throw new IllegalArgumentException("Inventory Item Supplier requires a value");
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        long id = database.insert(InventoryEntry.TABLE_NAME, null, values);
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert uri " + uri);
            return null;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        Log.e(LOG_TAG, "Id of element: " + id);

        return ContentUris.withAppendedId(uri, id);
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }
}
