/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.pets;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import com.example.android.pets.Data.PetContract.PetEntry;
import com.example.android.pets.Data.PetDbHelper;

/**
 * Displays list of pets that were entered and stored in the app.
 */
public class CatalogActivity extends AppCompatActivity implements
        android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor>{

    private PetDbHelper mDbHelper;

    private PetCursorAdapter mPetCursorAdapter;

    private final static int PET_LOADER = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });
        mDbHelper = new PetDbHelper(this);
        displayDatabaseInfo();

    }

    /**
     * Sau khi người dùng rời khỏi activity và quay lại thì hiển thị lại danh sách mới
     */
    @Override
    protected void onStart() {
        super.onStart();
        displayDatabaseInfo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertPet();
                displayDatabaseInfo();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                // Do nothing for now
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void insertPet() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues ct = new ContentValues();
        ct.put(PetEntry.COLUMN_PET_NAME, "Toto");
        ct.put(PetEntry.COLUMN_PET_BREED, "Terrier");
        ct.put(PetEntry.COLUMN_PET_GENDER, PetEntry.GENDER_MALE);
        ct.put(PetEntry.COLUMN_PET_WEIGHT , 7);
//        long newRowID = db.insert(PetEntry.TABLE_NAME , null, ct);
//        Log.v("CatalogActivity","New Row ID " + newRowID);
        Uri newUri = getContentResolver().insert(PetEntry.CONTENT_URI, ct);
    }

    private void displayDatabaseInfo() {
        //Column
        String projection [] = { PetEntry._ID,
                PetEntry.COLUMN_PET_NAME,
                PetEntry.COLUMN_PET_BREED,
                PetEntry.COLUMN_PET_GENDER,
                PetEntry.COLUMN_PET_WEIGHT
        };

        Cursor cursor = getContentResolver().query(PetEntry.CONTENT_URI, projection, null, null, null);

        ListView petListView = (ListView) findViewById(R.id.listView);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        // View rỗng
        View emptyView = findViewById(R.id.empty_view);
        petListView.setEmptyView(emptyView);

        mPetCursorAdapter = new PetCursorAdapter(this, cursor);
        petListView.setAdapter(mPetCursorAdapter);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String projection [] = {
                PetEntry._ID,
                PetEntry.COLUMN_PET_NAME,
                PetEntry.COLUMN_PET_BREED,
        };
        return new CursorLoader(this,
                PetEntry.CONTENT_URI,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link PetCursorAdapter} with this new Cursor containing updated pet data
        mPetCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        mPetCursorAdapter.swapCursor(null);
    }
}
