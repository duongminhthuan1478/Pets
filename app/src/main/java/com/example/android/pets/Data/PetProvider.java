package com.example.android.pets.Data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import com.example.android.pets.Data.PetContract.PetEntry;

public class PetProvider extends ContentProvider {

    /** Tag for the log messages */
    public static final String LOG_TAG = PetProvider.class.getSimpleName();
    private PetDbHelper mPetDbHelper;

    /** URI matcher code for the content URI for the pets table */
    private static final int PETS = 100;

    /** URI matcher code for the content URI for a single pet in the pets table */
    private static final int PET_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static{
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);
        // /# đại diện cho ID bất kì
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#", PET_ID);
    }

    @Override
    public boolean onCreate() {
        mPetDbHelper = new PetDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteDatabase database = mPetDbHelper.getReadableDatabase();
        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch (match){
            case PETS:
                // SELECT * FROM PETS
               cursor = database.query(PetEntry.TABLE_NAME,
                       projection,
                       selection,
                       selectionArgs,
                       null,
                       null,
                       sortOrder
                        );
                break;
            case PET_ID:
                // SELECT * FROM PETS where ID =
                selection = PetEntry._ID + "=?";
                // selectionArgs là một string
                // phân tích uri và lấy số ID cuối cùng để đưa vào dấu selection = PetEntry._ID + "=?" ngay dấu ?
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(PetEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return insertPet(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }

    }


    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case PETS:
                return updatePet(uri, contentValues, selection, selectionArgs);
            case PET_ID:
                // For the PET_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = PetEntry._ID + "=?";
                // selectionArgs là một string
                // phân tích uri và lấy số ID cuối cùng để đưa vào dấu selection = PetEntry._ID + "=?" ngay dấu ?
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updatePet(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }



    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mPetDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        // Track the number of rows that were deleted
        int rowsDeleted;
        switch (match) {
            case PETS:
                // Delete all rows that match the selection and selection args
                //return database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
                rowsDeleted = database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PET_ID:
                // Delete a single row given by the ID in the URI
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        // Return the number of rows deleted
        return rowsDeleted;

    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(Uri uri) {
        //The purpose of this method is to return a String that describes the type of the data stored at the input Uri.
        // This String is known as the MIME type, which can also be referred to as content type.
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    /**
     * Insert a pet into the database with the given content values.
     * Return the new content URI for that specific row in the database.
     */
    private Uri insertPet(Uri uri, ContentValues values) {
        // Check các giá trị đầu vào
        String name = values.getAsString(PetEntry.COLUMN_PET_NAME);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Pet requires a name");
        }

        Integer gender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
        if(gender == null|| !PetEntry.isValidGender(gender)){
            throw new IllegalArgumentException("Pet requires valid gender");
        }

        Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
        // weight null = default set = 0
        if (weight != null && weight < 0) {
            throw new IllegalArgumentException("Pet requires valid weight");
        }

        // ghi vào dtb
        SQLiteDatabase database = mPetDbHelper.getWritableDatabase();
        long id = database.insert(PetEntry.TABLE_NAME, null, values);

        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the pet content URI
        getContext().getContentResolver().notifyChange(uri, null);


        // Return the new URI with the ID (of the newly inserted row) appended at the end
        // uri lấy CONTENT_URI và add id vào
        // VD: content://com.example.android.pets/pets/6
        return ContentUris.withAppendedId(uri, id);
    }

    private int updatePet(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        // Check các giá trị sửa đổi
        String name = contentValues.getAsString(PetEntry.COLUMN_PET_NAME);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Pet requires a name");
        }

        Integer gender = contentValues.getAsInteger(PetEntry.COLUMN_PET_GENDER);
        if(gender == null|| !PetEntry.isValidGender(gender)){
            throw new IllegalArgumentException("Pet requires valid gender");
        }

        Integer weight = contentValues.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
        // weight null = default set = 0
        if (weight != null && weight < 0) {
            throw new IllegalArgumentException("Pet requires valid weight");
        }
        // If there are no values to update, then don't try to update the database
        if (contentValues.size() == 0) {
            return 0;
        }

        SQLiteDatabase database = mPetDbHelper.getWritableDatabase();

        //return database.update(PetEntry.TABLE_NAME, contentValues, selection, selectionArgs);

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(PetEntry.TABLE_NAME, contentValues, selection, selectionArgs);
        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        // Return the number of rows updated
        return rowsUpdated;


    }
}
