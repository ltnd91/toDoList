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

package com.delaroystudios.todolist;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SearchView;

import com.delaroystudios.todolist.R;
import com.delaroystudios.todolist.data.TaskContract;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

//-------------------------------------Global variables --------------------------------------------
    // Constants for logging and referring to a unique loader
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int TASK_LOADER_ID = 0;


    // Member variables for the adapter and RecyclerView
    private CustomCursorAdapter mAdapter;
    RecyclerView mRecyclerView;

    //global variables that are passed to other activities
    public static int color[]=new int[8]; // color[0] to color[5] = groups color[6] = background color[7] = text
    private String currentSpinnerText = "";
    public static ArrayList<String> arrayListOfList = new ArrayList<String>();

    //main variables only
    boolean filter[]=new boolean[8];
    Spinner mainSpinner;
    private String searchText = "";
//-------------------------------------Global variables --------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        colorSetter();
        onCreateSetCheckBoxes();

        onCreateConfigureTaskAdapter();
        taskItemListener();

        mainSpinner = (Spinner) findViewById(R.id.mainSpinner);
        mainSpinnerListener();
        onCreateFillarrayListOfList();
        mainSpinnerUpdate("trash",1);
        mainSpinner.setSelection(arrayListOfList.indexOf("trash"));

        searchTextListener();

        fabAddTaskListener();
        fabEditColorListener();
        fabDeleteListListener();
        fabAddListListener();
        /*
         Ensure a loader is initialized and active. If the loader doesn't already exist, one is
         created, otherwise the last created loader is re-used.
         */
        getSupportLoaderManager().initLoader(TASK_LOADER_ID, null, this);
    }

    public void onCreateConfigureTaskAdapter(){
        // Set the RecyclerView to its corresponding view
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerViewTasks);

        // Set the layout for the RecyclerView to be a linear layout, which measures and
        // positions items within a RecyclerView into a linear list
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter and attach it to the RecyclerView
        mAdapter = new CustomCursorAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
    }

    public void taskItemListener(){
        /*
         Add a touch helper to the RecyclerView to recognize when a user swipes to delete an item.
         An ItemTouchHelper enables touch behavior (like swipe and move) on each ViewHolder,
         and uses callbacks to signal when a user is performing these actions.
         */
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            // Called when a user swipes left or right on a ViewHolder
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                // Here is where you'll implement swipe to delete

                // COMPLETED (1) Construct the URI for the item to delete
                //[Hint] Use getTag (from the adapter code) to get the id of the swiped item
                // Retrieve the id of the task to delete
                int id = (int) viewHolder.itemView.getTag();

                if(!(currentSpinnerText.equals("trash")))
                {
                    TextView taskDescriptionView = (TextView) viewHolder.itemView.findViewById(R.id.taskDescription);
                    TextView priorityView = (TextView) viewHolder.itemView.findViewById(R.id.priorityTextView);
                    moveToTrash(taskDescriptionView.getText().toString(),
                            Integer.parseInt(priorityView.getText().toString()),
                            "trash");
                }

                // Build appropriate uri with String row id appended
                String stringId = Integer.toString(id);
                Uri uri = TaskContract.TaskEntry.CONTENT_URI;
                uri = uri.buildUpon().appendPath(stringId).build();

                // COMPLETED (2) Delete a single row of data using a ContentResolver
                getContentResolver().delete(uri, null, null);

                // COMPLETED (3) Restart the loader to re-query for all tasks after a deletion
                getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, MainActivity.this);
            }
        }).attachToRecyclerView(mRecyclerView);
    }

    public void fabAddTaskListener(){
        /*
         Set the Floating Action Button (FAB) to its corresponding View.
         Attach an OnClickListener to it, so that when it's clicked, a new intent will be created
         to launch the AddTaskActivity.
         */
        FloatingActionButton fabAddTask = (FloatingActionButton) findViewById(R.id.fabAddTask);

        fabAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create a new intent to start an AddTaskActivity
                Intent addTaskIntent = new Intent(MainActivity.this, AddTaskActivity.class);
                addTaskIntent.putExtra("editFlag", false);
                addTaskIntent.putStringArrayListExtra("arrayListOfList", arrayListOfList);
                addTaskIntent.putExtra("list", currentSpinnerText);
                startActivity(addTaskIntent);
            }
        });
    }

    public void fabEditColorListener(){
        FloatingActionButton fabEditColor = (FloatingActionButton) findViewById(R.id.fabEditColor);

        fabEditColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // we add editColorLoader temporarily because spinner color remains same
                // despite changing colors unless we switch to a different spinner item
                mainSpinnerUpdate("editColorLoader",1);
                mainSpinner.setSelection(arrayListOfList.indexOf("editColorLoader"));
                Intent colorIntent = new Intent(getApplicationContext(), AmbilWarnaDemoPreferenceActivity.class);
                startActivity(colorIntent);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Do something after 5s = 5000ms
                        mainSpinnerUpdate("editColorLoader",2);
                    }
                }, 1000);

            }
        });
    }

    public void fabDeleteListListener(){
        FloatingActionButton fabDeleteList = (FloatingActionButton) findViewById(R.id.fabDeleteList);

        fabDeleteList.setOnClickListener(new View.OnClickListener() {
            final String[] projection = {TaskContract.TaskEntry._ID,TaskContract.TaskEntry.COLUMN_DESCRIPTION,
                    TaskContract.TaskEntry.COLUMN_LIST,TaskContract.TaskEntry.COLUMN_PRIORITY};
            @Override

            public void onClick(View view) {
                if(arrayListOfList.size()>0){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(Html.fromHtml("<h1 style =\"font-size:100%;\"><font color='"+color[7]+"'>Do you want to delete list " +
                            mainSpinner.getSelectedItem().toString()+"</font></h1>"));
                    builder.setCancelable(false);
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            currentSpinnerText = mainSpinner.getSelectedItem().toString();
                            mainSpinnerUpdate(currentSpinnerText,2);
                            mainSpinner.setSelection(arrayListOfList.indexOf("trash"));
                            Cursor c = getContentResolver().query(TaskContract.TaskEntry.CONTENT_URI,
                                    projection,
                                    TaskContract.TaskEntry.COLUMN_LIST + " == '" + currentSpinnerText + "'",
                                    null,
                                    TaskContract.TaskEntry.COLUMN_PRIORITY);
                            if(c.moveToFirst()) {
                                do {
                                    if(!(currentSpinnerText.equals("trash"))){
                                        moveToTrash(c.getString(c.getColumnIndex(TaskContract.TaskEntry.COLUMN_DESCRIPTION)),
                                                c.getInt(c.getColumnIndex(TaskContract.TaskEntry.COLUMN_PRIORITY)),"trash");
                                    }
                                    String stringId = Integer.toString(c.getInt(c.getColumnIndex(TaskContract.TaskEntry._ID)));
                                    Uri uri = TaskContract.TaskEntry.CONTENT_URI;
                                    uri = uri.buildUpon().appendPath(stringId).build();
                                    getContentResolver().delete(uri, null, null);
                                } while (c.moveToNext());
                            }
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                    alert.getWindow().setBackgroundDrawable(new ColorDrawable(color[6]));
                    Button nbutton = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
                    nbutton.setTextColor(color[7]);
                    Button pbutton = alert.getButton(DialogInterface.BUTTON_POSITIVE);
                    pbutton.setTextColor(color[7]);
                }
            }
        });
    }

    public void fabAddListListener(){
        FloatingActionButton fabAddList = (FloatingActionButton) findViewById(R.id.fabAddList);

        fabAddList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(Html.fromHtml("<h1 style=\"font-size:100%;\"><font color='"+color[7]+"'>Add a list</font></h1>"));
// Set up the input
                final EditText input = new EditText(MainActivity.this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                builder.setView(input);
                input.setTextColor(color[7]);
                builder.setCancelable(false);
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
// Set up the buttons
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        currentSpinnerText = input.getText().toString();
                        mainSpinnerUpdate(currentSpinnerText,1);
                        mainSpinner.setSelection(arrayListOfList.indexOf(currentSpinnerText));
                        getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, MainActivity.this);
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                alert.getWindow().setBackgroundDrawable(new ColorDrawable(color[6]));
                Button nbutton = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
                nbutton.setTextColor(color[7]);
                Button pbutton = alert.getButton(DialogInterface.BUTTON_POSITIVE);
                pbutton.setTextColor(color[7]);

            }
        });
    }

    public void mainSpinnerListener(){
        mainSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                currentSpinnerText = mainSpinner.getSelectedItem().toString();
                getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, MainActivity.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    public void searchTextListener(){
        EditText Field1 = (EditText) findViewById(R.id.searchEditText);
        Field1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                searchText =  s.toString() ;
                getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, MainActivity.this);
            }
        });
    }

    public void mainSpinnerUpdate(String text, int code){
        if (code == 1 && !(arrayListOfList.contains(text)||text.equals(""))){
            arrayListOfList.add(text);
        }
        if(code == 2 && !(text.equals("trash")) ){
                arrayListOfList.remove(text);
        }
        ArrayAdapter<String> myAdapter = mainAdapterSpinner();
        mainSpinner.setAdapter(myAdapter);
    }

    public void moveToTrash(String description, int priority, String list){
        ContentValues contentValues = new ContentValues();
        contentValues.put(TaskContract.TaskEntry.COLUMN_DESCRIPTION, description);
        contentValues.put(TaskContract.TaskEntry.COLUMN_PRIORITY, priority);
        contentValues.put(TaskContract.TaskEntry.COLUMN_LIST,list);
        // Insert the content values via a ContentResolver
        getContentResolver().insert(TaskContract.TaskEntry.CONTENT_URI, contentValues);
    }

    public void colorSetter(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        View someView;
        String checkID; int resID;
        color[0]=preferences.getInt("pref1", Color.RED);
        color[1]=preferences.getInt("pref2", Color.YELLOW);
        color[2]=preferences.getInt("pref3", Color.GREEN);
        color[3]=preferences.getInt("pref4", Color.CYAN);
        color[4]=preferences.getInt("pref5", Color.MAGENTA);
        color[5]=preferences.getInt("pref6", Color.GRAY);
        color[6]=preferences.getInt("pref7", Color.LTGRAY);
        color[7]=preferences.getInt("pref8", Color.BLACK);
        for (int i = 0; i < 6; i++){
            //color[i]=preferences.getInt("pref"+(i+1),0);
            checkID = "checkP" + (i + 1);
            resID = getResources().getIdentifier(checkID, "id", getPackageName());
            someView = findViewById(resID);
            someView.setBackgroundColor(color[i]);
        }

        someView =findViewById(R.id.mainToolbar);
        someView.setBackgroundColor(color[6]);
        View root = someView.getRootView();
        root.setBackgroundColor(color[6]);
        TextView textView = (TextView) findViewById(R.id.searchEditText);
        textView.setHintTextColor(color[7]);
        textView.setTextColor(color[7]);
    }

    public void onCreateSetCheckBoxes(){
        for(int i=0;i<filter.length;i++){
            filter[i]=true;
        }
    }

    public void onCreateFillarrayListOfList(){
        Cursor c =getContentResolver().query(TaskContract.TaskEntry.CONTENT_URI,
                null,
                null,
                null,
                TaskContract.TaskEntry.COLUMN_PRIORITY);
        if(c.moveToFirst()) {
            do {
                final String text = c.getString(c.getColumnIndex(TaskContract.TaskEntry.COLUMN_LIST));
                mainSpinnerUpdate(text,1);
            } while (c.moveToNext());
        }
    }

    public ArrayAdapter<String> mainAdapterSpinner(){
        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(MainActivity.this,
                R.layout.support_simple_spinner_dropdown_item,
                arrayListOfList){
            @Override
            public View getDropDownView(int position, View convertView,ViewGroup parent) {
                // TODO Auto-generated method stub

                View view = super.getView(position, convertView, parent);
                view.setBackgroundColor(color[6]);
                TextView text = (TextView)view;
                text.setTextColor(color[7]);
                text.setTextSize(18);

                return view;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // TODO Auto-generated method stub

                View view = super.getView(position, convertView, parent);
                view.setBackgroundColor(color[6]);

                TextView text = (TextView)view;
                text.setTextColor(color[7]);
                text.setTextSize(18);

                return view;
            }
        };
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return myAdapter;
    }

    /**
     * This method is called after this activity has been paused or restarted.
     * Often, this is after new data has been inserted through an AddTaskActivity,
     * so this restarts the loader to re-query the underlying data for any changes.
     */

    @Override
    protected void onResume() {
        super.onResume();

        // re-queries for all tasks
        colorSetter();
        getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, this);
    }

    /**
     * Instantiates and returns a new AsyncTaskLoader with the given ID.
     * This loader will return task data as a Cursor or null if an error occurs.
     *
     * Implements the required callbacks to take care of loading data at all stages of loading.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, final Bundle loaderArgs) {

        return new AsyncTaskLoader<Cursor>(this) {

            // Initialize a Cursor, this will hold all the task data
            Cursor mTaskData = null;

            // onStartLoading() is called when a loader first starts loading data
            @Override
            protected void onStartLoading() {
                if (mTaskData != null) {
                    // Delivers any previously loaded data immediately
                    deliverResult(mTaskData);
                } else {
                    // Force a new load
                    forceLoad();
                }
            }

            // loadInBackground() performs asynchronous loading of data
            @Override
            public Cursor loadInBackground() {
                // Will implement to load data

                // Query and load all task data in the background; sort by priority
                // [Hint] use a try/catch block to catch any errors in loading data
                final String[] projection = {TaskContract.TaskEntry._ID,TaskContract.TaskEntry.COLUMN_DESCRIPTION,
                        TaskContract.TaskEntry.COLUMN_LIST,TaskContract.TaskEntry.COLUMN_PRIORITY};
                // make string array for loop assigning
                final String sa[] = new String[8]; //sa = selection argument
                for (int i = 0; i<6;i++){
                    sa[i]=filter[i]?""+(i+1)+"":"0";
                }
                sa[6]=searchText;sa[7]=currentSpinnerText;

                try {
                    return getContentResolver().query(TaskContract.TaskEntry.CONTENT_URI,
                            projection,
                            TaskContract.TaskEntry.COLUMN_PRIORITY + " IN(?,?,?,?,?,?) AND "
                                    + TaskContract.TaskEntry.COLUMN_DESCRIPTION + " LIKE '%" + sa[6] + "%' AND "
                                    + TaskContract.TaskEntry.COLUMN_LIST + " == '" + sa[7] + "'",
                            new String[] {sa[0], sa[1], sa[2], sa[3], sa[4], sa[5]},
                            TaskContract.TaskEntry.COLUMN_PRIORITY);

                } catch (Exception e) {
                    Log.e(TAG, "Failed to asynchronously load data.");
                    e.printStackTrace();
                    return null;
                }
            }

            // deliverResult sends the result of the load, a Cursor, to the registered listener
            public void deliverResult(Cursor data) {
                mTaskData = data;
                super.deliverResult(data);
            }
        };

    }


    /**
     * Called when a previously created loader has finished its load.
     *
     * @param loader The Loader that has finished.
     * @param data The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update the data that the adapter uses to create ViewHolders
        mAdapter.swapCursor(data);
    }


    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.
     * onLoaderReset removes any references this activity had to the loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.swapCursor(null);
        }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();
        // Check which checkbox was clicked
        String checkID; int resID;
        for (int i = 0; i < 6; i++){
            checkID = "checkP" + (i + 1);
            resID = getResources().getIdentifier(checkID, "id", getPackageName());
            if (view.getId()==resID) {
                filter[i] = checked ? true : false;
                getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, MainActivity.this);
                break;
            }
        }
    }
}