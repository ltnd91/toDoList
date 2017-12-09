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

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.delaroystudios.todolist.R;
import com.delaroystudios.todolist.data.TaskContract;

import java.util.ArrayList;

//12/7/17 did not refactor because no time can do later
public class AddTaskActivity extends AppCompatActivity {

    // Declare a member variable to keep track of a task's selected mPriority
    private int mPriority;
    Spinner addTaskSpinner;
    ArrayList<String> arrayListOfList = new ArrayList<String>();
    String selectedList;
    String taskDescription;
    String stringId;
    Boolean editTask;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);
        getOtherActivityExtras();
        addTaskSpinner = (Spinner) findViewById(R.id.addTaskSpinner);
        configureAddTaskSpinner();
        addTaskSpinnerListener();
        if (editTask)
            onCreateEditTaskValues();
        else
            onCreateNewTaskValues();
        colorSetter();
    }


    /**
     * onClickAddTask is called when the "ADD" button is clicked.
     * It retrieves user input and inserts that new task data into the underlying database.
     */
    public void onClickAddTask(View view) {
        // Not yet implemented


        if(editTask)
            onClickEditTaskDeleteOldCopy();
        onClickAddTaskToTable();

        // Display the URI that's returned with a Toast
        // [Hint] Don't forget to call finish() to return to MainActivity after this insert is complete
        //if(uri != null) {
           // Toast.makeText(getBaseContext(), uri.toString(), Toast.LENGTH_LONG).show();
        //}

        // Finish activity (this returns back to MainActivity)
        finish();

    }


    /**
     * onPrioritySelected is called whenever a priority button is clicked.
     * It changes the value of mPriority based on the selected button.
     */
    public void onPrioritySelected(View view) {
        String radButtonID; int resID;
        for(int i = 1; i<7; i++){
            radButtonID = "radButton" + i;
            resID = getResources().getIdentifier(radButtonID, "id", getPackageName());
            if (((RadioButton) findViewById(resID)).isChecked()) {
                mPriority = i;
                updateEditTextBackgroundColor();
                break;
            }
        }
    }

    public void updateEditTextBackgroundColor(){
        TextView textView = (TextView) findViewById(R.id.editTextTaskDescription);
        textView.setBackgroundColor(MainActivity.color[(mPriority-1)]);
    }

    public void getOtherActivityExtras(){
        Intent iin = getIntent();
        Bundle b = iin.getExtras();
        arrayListOfList = b.getStringArrayList("arrayListOfList");
        selectedList = b.get("list").toString();
        editTask = (Boolean) b.get("editFlag");
        if(editTask){
            mPriority = Integer.parseInt(b.get("priority").toString());
            taskDescription = b.get("description").toString();
            stringId = b.get("id").toString();
        }
    }

    public void colorSetter(){
        View someView;
        String buttonID; int resID;
        for(int i = 0;i<6;i++){
            buttonID = "buttonColor" + (i + 1);
            resID = getResources().getIdentifier(buttonID, "id", getPackageName());
            someView = findViewById(resID);
            someView.setBackgroundColor(MainActivity.color[i]);
        }
        someView = findViewById(R.id.addTaskSpinner);
        someView.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
        View root = someView.getRootView();
        root.setBackgroundColor(MainActivity.color[6]);
        TextView textView = (TextView) findViewById(R.id.editTextTaskDescription);
        textView.setTextColor(MainActivity.color[7]);
        textView.setHintTextColor(MainActivity.color[7]);
        textView = (TextView) findViewById(R.id.priorityLabel);
        textView.setTextColor(MainActivity.color[7]);
        textView = (TextView) findViewById(R.id.listLabel);
        textView.setTextColor(MainActivity.color[7]);
        updateEditTextBackgroundColor();
    }

    public void configureAddTaskSpinner(){
        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(AddTaskActivity.this,
                R.layout.support_simple_spinner_dropdown_item,
                arrayListOfList){
            @Override
            public View getDropDownView(int position, View convertView,ViewGroup parent) {
                // TODO Auto-generated method stub

                View view = super.getView(position, convertView, parent);

                TextView text = (TextView)view;
                text.setTextColor(Color.WHITE);
                text.setTextSize(25);
                return view;

            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // TODO Auto-generated method stub

                View view = super.getView(position, convertView, parent);

                TextView text = (TextView)view;
                text.setTextColor(Color.WHITE);
                text.setTextSize(25);

                return view;

            }
        };
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addTaskSpinner.setAdapter(myAdapter);
        addTaskSpinner.setSelection(arrayListOfList.indexOf(selectedList));
    }

    public void onCreateNewTaskValues(){
        ((RadioButton) findViewById(R.id.radButton1)).setChecked(true);
        mPriority = 1;

    }

    public void onCreateEditTaskValues(){
            ((EditText) findViewById(R.id.editTextTaskDescription)).setText(taskDescription);
            String radButtonID; int resID;
            for(int i = 1; i<7; i++){
                radButtonID = "radButton" + i;
                resID = getResources().getIdentifier(radButtonID, "id", getPackageName());
                if(mPriority==i){
                    ((RadioButton) findViewById(resID)).setChecked(true);
                    break;
                }
            }
    }

    public void onClickEditTaskDeleteOldCopy(){
        // Build appropriate uri with String row id appended
        Uri uri = TaskContract.TaskEntry.CONTENT_URI;
        uri = uri.buildUpon().appendPath(stringId).build();

        // COMPLETED (2) Delete a single row of data using a ContentResolver
        getContentResolver().delete(uri, null, null);
    }

    public void onClickAddTaskToTable(){
        // Check if EditText is empty, if not retrieve input and store it in a ContentValues object
        // If the EditText input is empty -> don't create an entry
        String input = ((EditText) findViewById(R.id.editTextTaskDescription)).getText().toString();
        if (input.length() == 0) {
            return;
        }
        // Put the task description and selected mPriority into the ContentValues
        // Insert new task data via a ContentResolver
        // Create new empty ContentValues object
        ContentValues contentValues = new ContentValues();
        contentValues.put(TaskContract.TaskEntry.COLUMN_DESCRIPTION, input);
        contentValues.put(TaskContract.TaskEntry.COLUMN_PRIORITY, mPriority);
        contentValues.put(TaskContract.TaskEntry.COLUMN_LIST,selectedList);
        // Insert the content values via a ContentResolver
        getContentResolver().insert(TaskContract.TaskEntry.CONTENT_URI, contentValues);
    }

    public void addTaskSpinnerListener(){
        addTaskSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedList = addTaskSpinner.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

}
