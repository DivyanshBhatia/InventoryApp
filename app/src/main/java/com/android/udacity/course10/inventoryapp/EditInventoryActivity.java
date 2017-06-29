package com.android.udacity.course10.inventoryapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.udacity.course10.inventoryapp.data.InventoryContract.InventoryEntry;
import com.android.udacity.course10.inventoryapp.data.InventoryDbHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.android.udacity.course10.inventoryapp.data.InventoryProvider.LOG_TAG;

/*
This activity is designed to delete, update, insert an item from inventory
 */
public class EditInventoryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_INVENTORY_LOADER = 1;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final String FILE_PROVIDER_AUTHORITY = "com.android.udacity.course10.inventoryapp.imageprovider";
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String CAMERA_DIR = "/dcim/";
    final Context context = this;
    private EditText mNameEditText;
    private EditText mPriceEditText;
    private EditText mQtyEditText;
    private EditText mSupplierEditText;
    private InventoryDbHelper mDbHelper;
    private Bitmap mBitmap;
    private Uri mUri;
    private EditText mSupplierEmailEditText;
    private EditText mSupplierPhoneEditText;
    private Uri mCurrentItemUri;
    private ImageView imagePreview;
    private boolean mItemHasChanged = false;
    private LinearLayout modifyView;
    private EditText modifyTxt;
    private View modifyViewDialog;

    //Based on the touch we determine whether the field is changed
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_inventory);
        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();


        mNameEditText = (EditText) findViewById(R.id.item_name_id);
        mPriceEditText = (EditText) findViewById(R.id.item_price_id);
        mSupplierEditText = (EditText) findViewById(R.id.item_supplier_id);
        mQtyEditText = (EditText) findViewById(R.id.item_inventory_id);
        mSupplierEmailEditText = (EditText) findViewById(R.id.supplier_email_id);
        mSupplierPhoneEditText = (EditText) findViewById(R.id.supplier_phone_id);
        imagePreview = (ImageView) findViewById(R.id.image_preview_id);

        mDbHelper = new InventoryDbHelper(this);
        Button galleryImageButton = (Button) findViewById(R.id.add_gallery_image);
        galleryImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImageSelector();
            }
        });


        Button cameraImageButton = (Button) findViewById(R.id.add_camera_image);
        cameraImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });

        Button saveDataButton = (Button) findViewById(R.id.add_inventory_id);
        saveDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveInventory();
            }
        });
        if (mCurrentItemUri == null) {
            saveDataButton.setText(getResources().getString(R.string.enter_inventory));
            setTitle(R.string.add_inventory_label);
            invalidateOptionsMenu();
        } else {
            saveDataButton.setText(getResources().getString(R.string.update_inventory));
            Log.v("CatalogActivity", "Failed New Row" + mCurrentItemUri);
            setTitle(R.string.edit_inventory_label);
            //Following code connects the above views to cursor loader data
            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);
        }

        Button inventorySaleButton = (Button) findViewById(R.id.inventory_sale_id);
        inventorySaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                modifyInventoryQty(-1);
            }
        });

        modifyView = (LinearLayout) findViewById(R.id.order_layout_id);
        if (mCurrentItemUri == null) {
            modifyView.setVisibility(View.GONE);
        } else {
            modifyView.setVisibility(View.VISIBLE);
        }


        Button inventoryModifyButton = (Button) findViewById(R.id.inventory_modify_id);
        inventoryModifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                modifyViewDialog = LayoutInflater.from(context).inflate(R.layout.modify_quantity_dialog, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                alertDialogBuilder.setView(modifyViewDialog);
                modifyTxt = (EditText) modifyViewDialog.findViewById(R.id.modify_qty_id);
                // set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // get user input and set it to result
                                        // edit text
                                        String currentQty = mQtyEditText.getText().toString();

                                        if (isInteger(currentQty)) {
                                            Log.v(LOG_TAG, ">>>>>" + modifyTxt.toString());
                                            Log.v(LOG_TAG, ">>>" + modifyTxt.getText().toString());
                                            modifyInventoryQty(Integer.parseInt(modifyTxt.getText().toString()));
                                        }
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alertDialog = alertDialogBuilder.create();
                // show it
                alertDialog.show();

            }
        });


        Button inventoryOrderButton = (Button) findViewById(R.id.inventory_order_id);
        inventoryOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendEmail();
            }
        });

        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mQtyEditText.setOnTouchListener(mTouchListener);
        mSupplierEmailEditText.setOnTouchListener(mTouchListener);
        mSupplierPhoneEditText.setOnTouchListener(mTouchListener);
        cameraImageButton.setOnTouchListener(mTouchListener);
        galleryImageButton.setOnTouchListener(mTouchListener);
    }

    public void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        try {
            File f = createImageFile();

            Log.d(LOG_TAG, "File: " + f.getAbsolutePath());

            mUri = FileProvider.getUriForFile(
                    this, FILE_PROVIDER_AUTHORITY, f);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);

            // Solution taken from http://stackoverflow.com/a/18332000/3346625
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, mUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveInventory() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        String itemNameString = mNameEditText.getText().toString();
        String itemPriceString = mPriceEditText.getText().toString();
        String inventoryDataQtyString = mQtyEditText.getText().toString();
        String inventorySupplierName = mSupplierEditText.getText().toString();
        String inventorySupplierEmail = mSupplierEmailEditText.getText().toString();
        String inventorySupplierPhone = mSupplierPhoneEditText.getText().toString();
        int inventoryDataQty = 0;
        int priceDataQty = 0;

        if (TextUtils.isEmpty(itemNameString) && TextUtils.isEmpty(itemPriceString) && TextUtils.isEmpty(inventoryDataQtyString) && TextUtils.isEmpty(inventorySupplierName)) {
            Toast.makeText(this, R.string.mandatory_fields_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (isInteger(inventoryDataQtyString))
            inventoryDataQty = Integer.parseInt(inventoryDataQtyString);

        if (!isInteger(inventoryDataQtyString) || inventoryDataQty < 0) {
            Toast.makeText(this, R.string.inventory_values_invalid_message, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isInteger(inventorySupplierPhone)) {
            priceDataQty = Integer.parseInt(inventoryDataQtyString);
        }

        if (!isInteger(itemPriceString) || priceDataQty < 0) {
            Toast.makeText(this, R.string.inventory_price_invalid_message, Toast.LENGTH_SHORT).show();
            return;
        }


        values.put(InventoryEntry.COLUMN_INVENTORY_NAME, itemNameString);
        values.put(InventoryEntry.COLUMN_INVENTORY_PRICE, itemPriceString);
        values.put(InventoryEntry.COLUMN_INVENTORY_QTY_AVLBL, inventoryDataQty);
        values.put(InventoryEntry.COLUMN_INVENTORY_SUPPLIER, inventorySupplierName);
        values.put(InventoryEntry.COLUMN_INVENTORY_SUPPLIER_EMAIL, inventorySupplierEmail);
        values.put(InventoryEntry.COLUMN_INVENTORY_SUPPLIER_PHONE, inventorySupplierPhone);

        if (mBitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Bitmap scaledImage = Bitmap.createScaledBitmap(mBitmap, 125, 125, true);
            mBitmap.recycle();
            scaledImage.compress(Bitmap.CompressFormat.PNG, 50, stream);
            byte[] byteArray = stream.toByteArray();
            values.put(InventoryEntry.COLUMN_INVENTORY_PIC_PATH, byteArray);

        }

        if (mCurrentItemUri == null) {
            Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            if (newUri != null) {
                Toast.makeText(this, R.string.insert_passed_message, Toast.LENGTH_SHORT).show();
                Log.v("CatalogActivity", "Added New Row" + newUri);
                this.finish();
            } else {
                Toast.makeText(this, R.string.insert_failed_message, Toast.LENGTH_SHORT).show();
                Log.v("CatalogActivity", "Failed New Row" + newUri);
            }
        } else {
            int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);
            if (rowsAffected == 0) {
                Toast.makeText(this, R.string.update_failed_message, Toast.LENGTH_SHORT).show();
                Log.v("CatalogActivity", getResources().getString(R.string.update_failed_message));
            } else {
                Toast.makeText(this, R.string.update_passed_message, Toast.LENGTH_SHORT).show();
                Log.v("CatalogActivity", getResources().getString(R.string.update_passed_message));
                this.finish();
            }
        }

    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = new File(Environment.getExternalStorageDirectory()
                    + CAMERA_DIR
                    + getString(R.string.app_name));

            Log.d(LOG_TAG, "Dir: " + storageDir);

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d(LOG_TAG, "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    public void openImageSelector() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        // Show only images, no videos or anything else
        Log.e(LOG_TAG, "Check write to external permissions");
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                mUri = resultData.getData();
                mBitmap = getBitmapFromUri(mUri);
            }

        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            mBitmap = getBitmapFromUri(mUri);
        }
        if (mBitmap != null) {

            imagePreview.setImageBitmap(Bitmap.createScaledBitmap(mBitmap, 250, 300, true));
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Error closing ParcelFile Descriptor");
            }
        }
    }

    //In the following method we handle back press
    @Override
    public void onBackPressed() {
        // If the item hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }
        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_INVENTORY_NAME,
                InventoryEntry.COLUMN_INVENTORY_PRICE,
                InventoryEntry.COLUMN_INVENTORY_QTY_AVLBL,
                InventoryEntry.COLUMN_INVENTORY_SUPPLIER,
                InventoryEntry.COLUMN_INVENTORY_PIC_PATH,
                InventoryEntry.COLUMN_INVENTORY_SUPPLIER_PHONE,
                InventoryEntry.COLUMN_INVENTORY_SUPPLIER_EMAIL
        };
        Log.v(LOG_TAG, "On create Loader " + mCurrentItemUri);
        return new CursorLoader(this, mCurrentItemUri, projection, null, null, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditInventoryActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditInventoryActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            Log.v(LOG_TAG, "On Load Finished cursor:" + cursor.getCount());
            return;
        }
        if (cursor.moveToFirst()) {
            Log.v(LOG_TAG, "On Load Finished cursor move first:" + cursor.getCount());
            int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_NAME);
            int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PRICE);
            int qtyColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_QTY_AVLBL);
            int supplierColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_SUPPLIER);
            int imageColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PIC_PATH);
            int supplierPhoneColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_SUPPLIER_PHONE);
            int supplierEmailColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_SUPPLIER_EMAIL);

            String itemName = cursor.getString(nameColumnIndex);
            Log.v(LOG_TAG, "On Load Finished cursor move first:" + nameColumnIndex + ":" + itemName);
            String itemPrice = cursor.getString(priceColumnIndex);
            int itemQty = cursor.getInt(qtyColumnIndex);
            String supplierName = cursor.getString(supplierColumnIndex);
            byte[] itemImage = cursor.getBlob(imageColumnIndex);
            String supplierPhone = cursor.getString(supplierPhoneColumnIndex);
            String supplierEmail = cursor.getString(supplierEmailColumnIndex);

            mNameEditText.setText(itemName);
            mPriceEditText.setText(itemPrice);
            mQtyEditText.setText(String.valueOf(itemQty));
            mSupplierEditText.setText(supplierName);
            if (itemImage != null && itemImage.length > 0)
                imagePreview.setImageBitmap(BitmapFactory.decodeByteArray(itemImage, 0, itemImage.length));
            mSupplierEmailEditText.setText(supplierEmail);
            mSupplierPhoneEditText.setText(supplierPhone);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.v(LOG_TAG, "Loader Invalidated");
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mQtyEditText.setText("");
        mSupplierEditText.setText("");
        mSupplierEmailEditText.setText("");
        mSupplierPhoneEditText.setText("");
        imagePreview.setImageBitmap(null);

    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog

                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            if (menuItem != null)
                menuItem.setVisible(false);
        }
        return true;
    }

    private void showDeleteConfirmationDialog() {
        // Shows AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog. Based on users' decision app deletes or does not delete dialogbox
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the item in the database.
     */
    private void deleteItem() {
        //This method is used to delete current item
        if (mCurrentItemUri != null) {
            int rowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_item_success),
                        Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

    private void modifyInventoryQty(int quantityModified) {
        int currentQty = Integer.parseInt(mQtyEditText.getText().toString());
        int newQty = currentQty + quantityModified;
        if(newQty < 0) {
            newQty = 0;
            Toast.makeText(this, getString(R.string.negative_qty_warning),
                    Toast.LENGTH_SHORT).show();
        }

        mQtyEditText.setText(String.valueOf(newQty));
    }

    protected void sendEmail() {
        //This method is to send email to supplier
        Log.i("Send email", "");
        String supplierEmail = null;
        String itemName = null;
        String itemquantity = null;
        int quantity = 0;
        if (mSupplierEmailEditText != null) {
            supplierEmail = mSupplierEmailEditText.getText().toString();
            itemName = mNameEditText.getText().toString();
            itemquantity = mQtyEditText.getText().toString();
            if (!itemquantity.isEmpty()) {
                quantity = Integer.parseInt(itemquantity);
            } else {
                Toast.makeText(this,
                        R.string.no_inventory_order, Toast.LENGTH_SHORT).show();
                return;
            }

        }
        if (quantity == 0) {
            Toast.makeText(this,
                    R.string.no_inventory_order, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!supplierEmail.isEmpty()) {
            String[] TO = {supplierEmail};

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.template_subject));
            emailIntent.putExtra(Intent.EXTRA_TEXT, "We are placing order for " + itemName + "\n Quantity:" + quantity + "\n Please complete the order at your earliest.");

            try {
                startActivity(Intent.createChooser(emailIntent, context.getResources().getString(R.string.send_mail_log)));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this,
                        R.string.no_email_client, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this,
                    R.string.no_supplier_email, Toast.LENGTH_SHORT).show();
        }
    }
}
