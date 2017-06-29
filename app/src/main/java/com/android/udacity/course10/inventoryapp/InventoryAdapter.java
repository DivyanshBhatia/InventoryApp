package com.android.udacity.course10.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.udacity.course10.inventoryapp.data.InventoryContract;
import com.android.udacity.course10.inventoryapp.data.InventoryContract.InventoryEntry;

import java.util.Arrays;

import static com.android.udacity.course10.inventoryapp.data.InventoryProvider.LOG_TAG;


/**
 * Created by dnbhatia on 11/14/2016.
 */

public class InventoryAdapter extends CursorAdapter {

    private Context ctx;
    private Cursor mCursor;
    public InventoryAdapter(Context context, Cursor c) {

        super(context, c, 0);
        this.ctx=context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    private int itemId;
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        mCursor=cursor;
        TextView nameTextView = (TextView) view.findViewById(R.id.item_name_view);
        final TextView qtyTextView = (TextView) view.findViewById(R.id.item_qty_view);
        TextView priceTextView = (TextView) view.findViewById(R.id.item_price_view);
        TextView supplierTextView = (TextView) view.findViewById(R.id.item_supplier_view);
        ImageView itemImgView = (ImageView) view.findViewById(R.id.item_image);
        Button itemButtonView = (Button) view.findViewById(R.id.inventory_sale_id);
        itemId=cursor.getColumnIndex(InventoryEntry._id);
        int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_NAME);
        int qtyColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_QTY_AVLBL);
        int supplyColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_SUPPLIER);
        int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PRICE);
        int imageColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PIC_PATH);

        final Long inventoryItemId=cursor.getLong(itemId);
        String inventoryName = cursor.getString(nameColumnIndex);
        int inventoryQty = cursor.getInt(qtyColumnIndex);

        String inventorySupplier = cursor.getString(supplyColumnIndex);
        String inventoryPrice = cursor.getString(priceColumnIndex);


        Log.v(LOG_TAG, inventoryItemId+":"+inventoryName + ":" + inventorySupplier + ":qty=" + inventoryQty + ":" + inventoryPrice);

        nameTextView.setText(inventoryName);
        qtyTextView.setText(String.valueOf(inventoryQty));
        priceTextView.setText(inventoryPrice);
        priceTextView.append(context.getResources().getString(R.string.dollar_currency));
        supplierTextView.setText(inventorySupplier);
        String[] colname = cursor.getColumnNames();
        Log.v(LOG_TAG, ">>>>>" + Arrays.asList(colname).toString());
        Log.v(LOG_TAG, inventoryName + ":" + inventorySupplier + ":qty=" + inventoryQty + ":" + inventoryPrice);
        byte[] imageByte = cursor.getBlob(imageColumnIndex);
        if (imageByte != null && imageByte.length > 0) {
            Log.v(LOG_TAG, imageByte + ">>>>" + imageByte.length);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.length);
            itemImgView.setImageBitmap(bitmap);
        } else {
            itemImgView.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.no_item_image, null));
        }

        itemButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int inventoryQty = Integer.parseInt(qtyTextView.getText().toString())-1;
                if(inventoryQty < 0){
                    inventoryQty = 0;
                    Toast.makeText(ctx, "Inventory being sold is more than that is available. Please check.",
                            Toast.LENGTH_SHORT).show();
                } else{
                    ContentValues values = new ContentValues();
                    values.put(InventoryEntry.COLUMN_INVENTORY_QTY_AVLBL,inventoryQty);
                    Uri uri=ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, inventoryItemId);
                    int rowsUpdated=ctx.getContentResolver().update(uri, values, null, null);
                    Log.v(LOG_TAG,mCursor.getInt(itemId)+">>>>"+rowsUpdated);
                    Toast.makeText(ctx, "Inventory sold is updated by 1.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
