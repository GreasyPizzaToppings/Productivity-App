//TODO
/*
Learn how to share data between this fragment and the adapter, so that I can see what item the user taps in the dialogue, to be able to do schedule the correct item.

 */

package com.example.productivityappprototype;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.sql.Time;
import java.util.Calendar;
import java.util.LinkedList;

public class ScheduleFragment extends Fragment implements View.OnClickListener, ScheduleItemListAdapter.ScheduleAdapterInterface {
    private SharedPreferences sharedPreferences;
    private String sharedPreferencesFile = "com.example.productivityappprototype";
    private final String ITEM_NOT_FOUND = "";
    private final String baseItemKey = "item:"; //The base key used to store the items in the bundle
    public LinkedList<String> itemList = new LinkedList<>(); //The list which stores a copy of the items in the item list tab
    private String selectedItemInDialog; //The string which holds the name of the item the user selects in the AlertDialog
    private EditText editTextOneTimeItem;
    private AlertDialog scheduleDialog;
    private TimePickerDialog timePickerDialog;
    private long defaultStartEndTime = 0xDEADBEEF;
    private long startTimeInMs, endTimeInMs = defaultStartEndTime; //The starting and ending time of an item if the user specifies them. Default value is 0xdeadbeef

    public ScheduleFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.schedule_fragment, container, false);

        //---Underline the two header text views---
        //Get a handle on the "item" header text view for the schedule tab
        TextView itemHeaderTextView = v.findViewById(R.id.text_item);

        //Set a spanning string item for the "item" header to make it underlined
        String itemHeader = getContext().getString(R.string.text_item);
        SpannableString itemHeaderSpannable = new SpannableString(itemHeader);
        itemHeaderSpannable.setSpan(new UnderlineSpan(), 0, itemHeader.length(), 0); //Make the text underlined

        itemHeaderTextView.setText(itemHeaderSpannable); //Update the text and make it underlined

        //Repeat the process for the "scheduled time" header text view
        TextView timeHeaderTextView = v.findViewById(R.id.text_time);

        String timeHeader = getContext().getString(R.string.text_time);
        Spannable timeHeaderSpannable = new SpannableString(timeHeader);
        timeHeaderSpannable.setSpan(new UnderlineSpan(), 0, timeHeader.length(), 0); //Make the text underlined

        timeHeaderTextView.setText(timeHeaderSpannable); //Update the text and make it underlined

        //Get a handle to the floating action button and set its on click listener
        FloatingActionButton scheduleTaskButton = v.findViewById(R.id.fb_schedule_item);
        scheduleTaskButton.setOnClickListener(this);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    }

    //This method will read in the current SharedPreferences file and create the item list to be used by the dialog popup used to schedule an item
    public void UpdateItemList() {
        //--Read the shared preferences file and build an item list for use for the dialog--
        sharedPreferences = getActivity().getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE); //Initialise the SharedPreferences object and read in the file

        //Read in the data from the file to build the item list
        if (!sharedPreferencesFile.isEmpty()) {
            itemList.clear(); //Clear the old list

            //Use the bundle size as the linked list gets reinitialised to size 0 after every config change. -1 because there is always a bool variable stored as well
            for (int item = 0; item < sharedPreferencesFile.length(); item++) {
                String fullItemKey = baseItemKey + item; //Build the key used to predictably store the items in the file
                String restoredItem = sharedPreferences.getString(fullItemKey, ITEM_NOT_FOUND); //Blank default values are the error case as you cannot have an item with no length

                //Restore items that were found in the restored shared preference
                if (restoredItem != ITEM_NOT_FOUND) {
                    itemList.add(item, restoredItem);
                }
            }
        }
    }


    //The on click listener for the floating action button which displays a dialog and to allow the user to schedule a task
    @Override
    public void onClick(final View v) {
        //--Create and display the dialogue with the custom layout--
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        //Set dialog characteristics
        builder.setTitle("Schedule An Item");
        //builder.setMessage("Select an item from the item list, or a one-time item. Optionally provide either a start time, and end time, or both.");

        //Add the custom layout using an inflater
        LayoutInflater inflater = LayoutInflater.from(v.getContext());
        final View dialogView = inflater.inflate(R.layout.schedule_dialog, null); //Inflate the layout

        //--Initiate the recycler view in the dialog layout to display the item list items--
        UpdateItemList(); //Read in the SharedPreferences file again to ensure both item lists are identical
        RecyclerView dialogRecyclerView = dialogView.findViewById(R.id.recyclerview_dialog); //Get a handle to the recyclerview
        com.example.productivityappprototype.ScheduleItemListAdapter adapter = new ScheduleItemListAdapter(this, itemList, this); //Create the correct adapter and supply the data with the custom adapter
        dialogRecyclerView.setAdapter(adapter); //Set the adapter
        dialogRecyclerView.setLayoutManager(new LinearLayoutManager(dialogView.getContext())); //Assign the recyclerview a default layout manager

        builder.setView(dialogView); //Set the now-created layout to the dialog

        //Set the dialog buttons
        builder.setPositiveButton("Schedule Item", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //If the user selected an item from the item list before pressing 'schedule item'
                if (selectedItemInDialog != null) {
                    //Try and schedule the item (already have access to the name of the selected item)s


                    //Reset the selected item string so that it can update
                    selectedItemInDialog = null;
                }


                else { //See if the user entered an item in the one-time option editText
                    //In the case where they entered a one-time item
                    if(editTextOneTimeItem.getText().toString().length() != 0) {
                        selectedItemInDialog = editTextOneTimeItem.getText().toString(); //Extract the name of the item the user entered
                        //Schedule the item



                        selectedItemInDialog = null; //Reset the item
                    }

                    else {
                        //The case where the user pressed 'schedule item' without selecting an item in any way.
                        return;
                    }
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        //Build and show the dialog
        scheduleDialog = builder.create();
        scheduleDialog.show();

        //Initially disable the positive button until the user selects an item in some way
        scheduleDialog.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        editTextOneTimeItem = dialogView.findViewById(R.id.edit_one_time_item); //Get a handle to the edit text for the one-time item
        //--Set the listeners for the text in the edit text changing--
        editTextOneTimeItem.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //If the user's item has changed and is still a valid item
                if(editTextOneTimeItem.getText().length() >= 1) {
                    scheduleDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); //Enable the schedule item button, as a valid item is entered
                }

                //When the user deletes all of their text in the edit text
                else {
                    scheduleDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); //disable the schedule item button, as no valid item exists
                }
            }
        });

        //--Set the functionality of Time Setting buttons--
        //Initialise the calendar for the time setting buttons
        final Calendar currentTime = Calendar.getInstance();
        final long midDayInMs = 12*60*60*1000; //Used to account for the default value of mid day

        //Get a handle on the start time button
        final Button startTimeButton = dialogView.findViewById(R.id.button_start_time);
        //Set the listener for the start time button to allow for time picking to occur
        startTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View buttonView) {
                //Get the current time when the user pressed the button that opens the picker
                int buttonStartHour = currentTime.get(Calendar.HOUR_OF_DAY);
                int buttonStartMinute = currentTime.get(Calendar.MINUTE);

                //Initialise the time picker dialog and its listener
                timePickerDialog = new TimePickerDialog(v.getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        //Convert the entered time to milliseconds to avoid a deprecated constructor for time object
                        startTimeInMs = (hourOfDay * 60 * 60 * 1000) + (minute * 60 * 1000);

                        //Get the current time again for more accuracy, as time can change in between opening the picker and actually selecting a time.
                        int currentHour = currentTime.get(Calendar.HOUR_OF_DAY);
                        int currentMinute = currentTime.get(Calendar.MINUTE);
                        long currentTimeInMs = (currentHour * 60 * 60 * 1000) + (currentMinute * 60 * 1000);

                        TextView startTimeSelected = dialogView.findViewById(R.id.text_start_time_selected);

                        //If the specified time is possible
                        if(ScheduledTimeLegitimate(startTimeInMs, endTimeInMs, currentTimeInMs, v.getContext())) {
                            //Convert the selected time to a readable format
                            Time startTime = new Time(startTimeInMs - midDayInMs); //Set the correct time, adjusting for a default value of mid day.

                            //Update the start time text view
                            startTimeSelected.setText(startTime.toString()); //Show the user what time they selected
                        }

                        //When the time is not legit
                        else {
                            startTimeInMs = defaultStartEndTime ; //Reset the selected time to default value
                            startTimeSelected.setText(R.string.awaiting_selection); //Reset to default value
                        }

                    }
                }, buttonStartHour, buttonStartMinute, true);

                timePickerDialog.setTitle("Select A Start Time");
                timePickerDialog.show();
            }
        });

        //Get a handle on the end time button
        Button endTimeButton = dialogView.findViewById(R.id.button_end_time);
        //Set the listener for the end time button to allow for time picking to occur
        endTimeButton.setOnClickListener(new View.OnClickListener() {
            //Get the current time when the user pressed the button that opens the picker
            int buttonStartHour = currentTime.get(Calendar.HOUR_OF_DAY);
            int buttonStartMinute = currentTime.get(Calendar.MINUTE);
            @Override
            public void onClick(View buttonView) {

                //Initialise the time picker dialog and its listener
                timePickerDialog = new TimePickerDialog(v.getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        //Convert the entered time to milliseconds to avoid a deprecated constructor for time object
                        endTimeInMs = (hourOfDay * 60 * 60 * 1000) + (minute * 60 * 1000);

                        //Obtain the current hour and minute to calculate the current time in Ms to check for the legitimacy of the selected time
                        int currentHour = currentTime.get(Calendar.HOUR_OF_DAY);
                        int currentMinute = currentTime.get(Calendar.MINUTE);
                        long currentTimeInMs = (currentHour * 60 * 60 * 1000) + (currentMinute * 60 * 1000);

                        TextView endTimeSelected = dialogView.findViewById(R.id.text_end_time_selected); //Get a handle on the text view which displays the selected time

                        //If the specified time is possible
                        if(ScheduledTimeLegitimate(startTimeInMs, endTimeInMs, currentTimeInMs, v.getContext())) {
                            //Store the entered time value
                            Time endTime = new Time(endTimeInMs - midDayInMs); //Set the correct time, adjusting for a default value of mid day.

                            //Update the end time text view
                            endTimeSelected.setText(endTime.toString()); //Show the user what time they selected
                        }

                        //When the time is not legit
                        else {
                            endTimeInMs = defaultStartEndTime; //Reset the selected time to default value
                            endTimeSelected.setText(R.string.awaiting_selection); //Reset to default value
                        }
                    }
                }, buttonStartHour, buttonStartMinute, true);

                timePickerDialog.setTitle("Select An End Time");
                timePickerDialog.show();
            }
        });
    }

    /* This method performs a series of checks to ensure that the schedule time(s) are able to be realistically completed. If the time is confirmed to be legit, true is returned.
    Appropriate error messages are displayed via Toasts when the selected time is not legit. */
    public Boolean ScheduledTimeLegitimate(long startTimeInMs, long endTimeInMs, long currentTimeInMs, Context context) {

        //Check the cases when both a start time and an end time are entered
        if(startTimeInMs != defaultStartEndTime && endTimeInMs != defaultStartEndTime) {
            //1. Check if the user has planned to start or end an item before the current time (in the past)
            if(startTimeInMs < currentTimeInMs  || endTimeInMs < currentTimeInMs) {
                Toast.makeText(context, "You cannot schedule an item to start or end before the current time!", Toast.LENGTH_LONG).show(); //Display an appropriate error message to let user know what was wrong about their selection
                return false;
            }

            //2. If the start time is after the end time it is not a legit time
            if(startTimeInMs > endTimeInMs) {
                Toast.makeText(context, "Your start time needs to be before your end time!", Toast.LENGTH_LONG).show(); //Display an appropriate error message to let user know what was wrong about their selection
                return false;
            }

            //3. If the two times do not have their default values and if they are same, it is not legit
            if((startTimeInMs == endTimeInMs )) {
                Toast.makeText(context, "You cannot start and end an item at the same time!", Toast.LENGTH_LONG).show(); //Display an appropriate error message to let user know what was wrong about their selection
                return false;
            }
        }

        //When the user has only selected a start time or an end time, but not both
        else {
            //The earliest time the user should be able to start an item is directly after they press set item, hence < and not <= like with endTimeInMs.
            if(startTimeInMs != defaultStartEndTime && startTimeInMs < currentTimeInMs) {
                Toast.makeText(context, "You cannot start an item in the past!", Toast.LENGTH_LONG).show(); //Display an appropriate error message to let user know what was wrong about their selection
                return false;
            }

            //The user cannot end an item just as they schedule it, hence <=.
            if (endTimeInMs != defaultStartEndTime && endTimeInMs <= currentTimeInMs) {
                Toast.makeText(context, "You cannot end an item in the past!", Toast.LENGTH_LONG).show(); //Display an appropriate error message to let user know what was wrong about their selection
                return false;
            }
        }

        return true; //If this point is reached, the time is presumed to be legitimate
    }


    @Override
    public void OnClickDialogItem(String itemName, boolean selected) {
        //On the case of an item being selected in the item list
        if(selected) {
            selectedItemInDialog = itemName; //Store the name of the item they selected
            //Disable the editText for the input of a one-time item, as the user has already selected an item
            editTextOneTimeItem.setEnabled(false);
            scheduleDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); //Enable the schedule item button, as an item is now selected
        }

        //On the case of an item being deselected in the item list
        else {
            selectedItemInDialog = null;
            editTextOneTimeItem.setEnabled(true); //re-enable the edit text to allow the user to enter an item this way

            //When the user deselects a recycler view item, and the edit text has no valid item entered, disable the schedule item button.
            if(editTextOneTimeItem.getText().length() < 1)
            {
                scheduleDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); //Disable the schedule item button, as an item is now un-selected
            }
        }
    }
}
