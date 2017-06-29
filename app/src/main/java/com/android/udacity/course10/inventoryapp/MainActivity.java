package com.android.udacity.course10.inventoryapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
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
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
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

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final String FILE_PROVIDER_AUTHORITY = "com.android.udacity.course10.inventoryapp.imageprovider";
    private InventoryDbHelper mDbHelper;
    InventoryAdapter mCursorAdapter;
    private static final int INVENTORY_LOADER = 0;
    private Bitmap mBitmap;
    private Uri mUri;
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    private static final String CAMERA_DIR = "/dcim/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditInventoryActivity.class);
                startActivity(intent);
            }
        });
        ListView inventoryListView = (ListView) findViewById(R.id.list);
        mDbHelper = new InventoryDbHelper(this);
        View emptyView = findViewById(R.id.empty_view);
        inventoryListView.setEmptyView(emptyView);
        mCursorAdapter = new InventoryAdapter(this, null);
        inventoryListView.setVisibility(View.VISIBLE);
        inventoryListView.setAdapter(mCursorAdapter);
        Log.v(LOG_TAG,"Hello World");
        //Implementing Detail screen and prepopulation Here
        inventoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long itemId) {
                Intent intent = new Intent(MainActivity.this, EditInventoryActivity.class);
                Uri currentItemUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, itemId);
                intent.setData(currentItemUri);
                startActivity(intent);
            }
        });
        getLoaderManager().initLoader(INVENTORY_LOADER, null, this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                //openImageSelector();
                takePicture();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllItemDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void openImageSelector() {
        //This method picks images from gallery
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
                Log.v(LOG_TAG, requestCode + ":>>>><<<456");
                mBitmap = getBitmapFromUri(mUri);
            }
            addInventory();
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Log.v(LOG_TAG, requestCode + ":>>>><<<123");
            mBitmap = getBitmapFromUri(mUri);
            addInventory();
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        //This method is used to handle bitmap images
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

    private void addInventory() {
        //This method is used to insert item in database
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_INVENTORY_NAME, "Macbook");
        values.put(InventoryEntry.COLUMN_INVENTORY_PRICE, "100");
        values.put(InventoryEntry.COLUMN_INVENTORY_QTY_AVLBL, 3);
        values.put(InventoryEntry.COLUMN_INVENTORY_SUPPLIER, "Apple");

        if (mBitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Bitmap scaledImage = Bitmap.createScaledBitmap(mBitmap, 50, 50, true);
            mBitmap.recycle();
            scaledImage.compress(Bitmap.CompressFormat.PNG, 50, stream);
            byte[] byteArray = stream.toByteArray();
            values.put(InventoryEntry.COLUMN_INVENTORY_PIC_PATH, byteArray);

        }
        Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

        Log.v("CatalogActivity", "New Row" + newUri);
    }

    public void takePicture() {
        //This method calls CAMERA INTENT and allows user to take and upload camera images of item
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
        return new CursorLoader(this, InventoryEntry.CONTENT_URI, projection, null, null, null);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    private void deleteAllItemDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteItem() {
        //This method is used to delete item from inventory store
        int rowsDeleted = getContentResolver().delete(InventoryEntry.CONTENT_URI, null, null);
        if (rowsDeleted == 0) {
            // If no rows were deleted, then there was an error with the delete.
            Toast.makeText(this, getString(R.string.editor_delete_all_items_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the delete was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_delete_all_items_success),
                    Toast.LENGTH_SHORT).show();
        }

    }

}
