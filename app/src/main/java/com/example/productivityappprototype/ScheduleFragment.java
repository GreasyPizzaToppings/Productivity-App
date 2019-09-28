package com.example.productivityappprototype;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Paint;
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
import android.util.DisplayMetrics;
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

public class ScheduleFragment extends Fragment implements View.OnClickListener, ScheduleDialogItemListAdapter.AddScheduledItemInterface, ScheduleItemListAdapter.UpdateScheduledItemInterface {
    //Shared preferences objects
    private SharedPreferences rawScheduledItemsSharedPrefs;
    private SharedPreferences formattedScheduledItemsSharedPrefs;
    private SharedPreferences startTimesSharedPrefs;
    private SharedPreferences endTimesSharedPrefs;
    private SharedPreferences scheduledItemsCompletionStatusSharedPrefs;

    private LinkedList<String> rawScheduledItems = new LinkedList<>(); //The list of raw item names (Not including times) that the user has scheduled through the scheduling dialog. Some items may not appear in the item list.
    private LinkedList<String> formattedScheduledItemsWithTimes = new LinkedList<>(); //The list of formatted items, including the times which the user has or has not selected.
    private LinkedList<String> scheduledItemsStartTimes = new LinkedList<>(); //Holds the associated start time for each scheduled item. If the user didn't specify a start time, then the value is "n/a"
    private LinkedList<String> scheduledItemsEndTimes = new LinkedList<>(); //Holds the associated end time for each scheduled item. If the user didn't specify a start time, then the value is "n/a"
    private LinkedList<String> scheduledItemsCompletionStatus = new LinkedList<>(); //String instead of boolean for simplicity and for compatibility with methods that work on strings

    private String existingItemSelected; //The string which holds the name of the item the user selects from the recycler view / item list in the AlertDialog
    private EditText editTextOneTimeItem;
    private AlertDialog scheduleDialog;
    private TimePickerDialog timePickerDialog;
    private long defaultStartEndTimeInMs = 0xDEADBEEF;
    private long startTimeInMs = defaultStartEndTimeInMs; //The starting and ending time of an item is a default value. Not using 0 because 0 is a legitimate time
    private long endTimeInMs = defaultStartEndTimeInMs;
    private Time startTime, endTime;
    private final int MAX_ITEM_LENGTH = 100;
    private final int MIN_ITEM_LENGTH = 1;
    private final int MAX_SCHEDULED_ITEMS = 50;
    private TextView timeHeaderTextView;
    private TextView tutorialMessage;
    private FloatingActionButton unscheduleAllButton;
    float timeTextWidth; //Used for boundary calculations
    private Paint scheduledItemPaint;

    //class objects
    private com.example.productivityappprototype.ScheduleItemListAdapter scheduleItemListAdapter;
    private ItemFormatter itemFormatter;
    private SharedPreferencesFileManager sharedPrefsManager;

    public ScheduleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(dm); //Obtain the metrics of the display
        outState.putInt("displayWidth", dm.widthPixels); //Save the width of the current device configuration. This is used to determine whether the formatted items need to be re-formatted to fit the new screen width
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.schedule_fragment, container, false);

        //---Underline the two header text views---
        //Get a handle on the "item" header text view for the schedule tab
        TextView itemHeaderTextView = v.findViewById(R.id.text_item_heading);

        //Set a spanning string item for the "item" header to make it underlined
        String itemHeader = getContext().getString(R.string.text_item);
        SpannableString itemHeaderSpannable = new SpannableString(itemHeader);
        itemHeaderSpannable.setSpan(new UnderlineSpan(), 0, itemHeader.length(), 0); //Make the text underlined

        itemHeaderTextView.setText(itemHeaderSpannable); //Update the text and make it underlined

        //Repeat the process for the "scheduled time" header text view
        timeHeaderTextView = v.findViewById(R.id.text_scheduled_time_heading);

        String timeHeader = getContext().getString(R.string.text_time);
        Spannable timeHeaderSpannable = new SpannableString(timeHeader);
        timeHeaderSpannable.setSpan(new UnderlineSpan(), 0, timeHeader.length(), 0); //Make the text underlined

        timeHeaderTextView.setText(timeHeaderSpannable); //Update the text and make it underlined

        //Get a handle to the schedule item floating button and set its on click listener
        FloatingActionButton scheduleTaskButton = v.findViewById(R.id.fb_schedule_item);
        scheduleTaskButton.setOnClickListener(this);

        //Get a handle to the unschedule all items floating button and set its on click listener
        unscheduleAllButton = v.findViewById(R.id.fb_unschedule_all);
        unscheduleAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //create and show a dialog to warn them about the action
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(v.getContext());
                dialogBuilder.setTitle("Clear Schedule");
                dialogBuilder.setMessage("Are you sure you want to unschedule all your items?");
                dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //Unschedule all their items if they have any.
                                if(rawScheduledItems.size() != 0) clearSchedule();
                            }
                        });
                dialogBuilder.setNegativeButton("No", null);
                dialogBuilder.create().show();
            }
        });

        //Get a handle on the tutorial message textview
        tutorialMessage = v.findViewById(R.id.text_no_scheduled_items);

        sharedPrefsManager = new SharedPreferencesFileManager(this.getContext()); //Initialise object which manipulates and reads from the shared prefs files

        //sharedPrefsManager.clearSharedPreferences(getContext()); //useful when the shared preferences files are wrong, causing errors.
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //Get a handle to the recycler view for the scheduled items
        RecyclerView scheduleRecyclerView = getView().findViewById(R.id.recyclerview_scheduled_item_list);

        //Create the adapter and supply the data to be displayed
        scheduleItemListAdapter = new ScheduleItemListAdapter(this, rawScheduledItems, formattedScheduledItemsWithTimes, scheduledItemsStartTimes, scheduledItemsEndTimes,scheduledItemsCompletionStatus , this);
        scheduleRecyclerView.setAdapter(scheduleItemListAdapter); //Attach the adapter to the recycler view

        //Assign the recycler view a default layout manager
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));

        //Get a handle on the textview that all items go into to allow measuring of width of text
        TextView itemWidthMeasuringContainer = view.findViewById(R.id.text_width_measuring);

        //Get a handle on the textview to calculate the boundary which will allow for the time text to be placed underneath the "Scheduled Time" header.
        timeHeaderTextView = view.findViewById(R.id.text_scheduled_time_heading);

        //Initialise paint objects used by the 3 related scheduled item formatting methods
        Paint timeHeaderPaint = timeHeaderTextView.getPaint(); //Used to calculate the width of the header to calculate the boundary
        scheduledItemPaint = itemWidthMeasuringContainer.getPaint();


        //Initialise the shared pref objects
        //Shared preferences files
        String rawScheduledItemsSharedPrefsFile = "com.example.productivityappprototype.raw";
        rawScheduledItemsSharedPrefs = getActivity().getSharedPreferences(rawScheduledItemsSharedPrefsFile, Context.MODE_PRIVATE);
        String formattedScheduledItemsSharedPrefsFile = "com.example.productivityappprorotype.formatted";
        formattedScheduledItemsSharedPrefs = getActivity().getSharedPreferences(formattedScheduledItemsSharedPrefsFile, Context.MODE_PRIVATE);
        String startTimesSharedPrefsFile = "com.example.productivityappprototype.startTimes";
        startTimesSharedPrefs = getActivity().getSharedPreferences(startTimesSharedPrefsFile, Context.MODE_PRIVATE);
        String endTimesSharedPrefsFile = "com.example.productivityappprototype.endTimes";
        endTimesSharedPrefs = getActivity().getSharedPreferences(endTimesSharedPrefsFile, Context.MODE_PRIVATE);
        String scheduledItemsCompletionStatusSharedPrefsFile = "com.example.productivityappprototype.completion";
        scheduledItemsCompletionStatusSharedPrefs = getActivity().getSharedPreferences(scheduledItemsCompletionStatusSharedPrefsFile, Context.MODE_PRIVATE);

        //---Restore the data from the shared preferences files, except for the formatted items---
        sharedPrefsManager.restoreStringsFromFile(rawScheduledItemsSharedPrefs, rawScheduledItems);
        sharedPrefsManager.restoreStringsFromFile(startTimesSharedPrefs, scheduledItemsStartTimes);
        sharedPrefsManager.restoreStringsFromFile(endTimesSharedPrefs, scheduledItemsEndTimes);
        sharedPrefsManager.restoreStringsFromFile(scheduledItemsCompletionStatusSharedPrefs, scheduledItemsCompletionStatus);

        //Initialise display metrics object to get relevant display data
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(dm); //Obtain the metrics of the display

        //Initialise the item formatter object
        itemFormatter = new ItemFormatter(this.getContext(), scheduledItemPaint, timeHeaderPaint, dm.widthPixels);

        //Look at bundle data to determine if items need to be reformatted or just read from the file
        if(savedInstanceState != null) {
            //If the old display width does not match the current display width
            if(savedInstanceState.getInt("displayWidth") != dm.widthPixels) reformatScheduledItems();

            //Else just load the old data
            else formattedScheduledItemsWithTimes = sharedPrefsManager.restoreStringsFromFile(formattedScheduledItemsSharedPrefs, formattedScheduledItemsWithTimes);
        }
        else reformatScheduledItems(); //Ensure they have the right formatting

        if(scheduleItemListAdapter != null) scheduleItemListAdapter.notifyDataSetChanged();

        //Toggle the message and the unschedule all button now that the data has been restored
        ToggleTutorialMessage();
        ToggleUnscheduleAllButton();
    }

    //This unschedules all the scheduled items, clearing the schedule for the user
    public void clearSchedule() {
        int maxItemIndex = rawScheduledItems.size();

        //Iterate through the items and unschedule each one, from the back to the start
        for(int itemIndex = maxItemIndex -1; itemIndex >= 0; itemIndex--) {
            onUnscheduleItem(itemIndex);
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
        final View dialogView = inflater.inflate(R.layout.schedule_new_item_dialog, null); //Inflate the layout

        //--Initiate the recycler view in the dialog layout to display the item list items--
        //Relevant linked lists for the data
        //The list which holds items found in the item list tab. Note: does not include items the user enters through the edit text in the schedule item dialog.
        LinkedList<String> itemList = sharedPrefsManager.getItemList(); //Get the item list data that is saved in shared preferences
        RecyclerView dialogRecyclerView = dialogView.findViewById(R.id.recyclerview_existing_items); //Get a handle to the recyclerview
        ScheduleDialogItemListAdapter adapter = new ScheduleDialogItemListAdapter(this, itemList, this); //Create the correct adapter and supply the data with the custom adapter
        dialogRecyclerView.setAdapter(adapter); //Set the adapter
        dialogRecyclerView.setLayoutManager(new LinearLayoutManager(dialogView.getContext())); //Assign the recyclerview a default layout manager

        builder.setView(dialogView); //Set the now-created layout to the dialog

        //Set the dialog buttons
        builder.setPositiveButton("Schedule Item", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //When the user has scheduled too many items
                if(formattedScheduledItemsWithTimes.size() >= MAX_SCHEDULED_ITEMS) { //>= otherwise the no. would go over the max after this item is scheduled
                    Toast.makeText(getContext(), "Error. You cannot have more than " + MAX_SCHEDULED_ITEMS + " scheduled items. Clean up your schedule.", Toast.LENGTH_LONG).show();
                    return;
                }

                //If the user selected an item in some way before pressing 'schedule item'
                if (existingItemSelected != null) {
                    rawScheduledItems.addLast(existingItemSelected); //Add the raw name to the list, before it is scheduled and reset to null
                    scheduleItem(existingItemSelected); //Schedule the item with the given data
                    return;
                }

                //See if the user entered an item in the one-time option editText instead of selecting an item in the recyclerview
                else {
                    //Extract and store the name of the item the user entered
                    String oneTimeItemName = editTextOneTimeItem.getText().toString();

                    //When the item entered this way is legitimate (to stop extremely long names)
                    if (oneTimeItemName.length() >= MIN_ITEM_LENGTH && oneTimeItemName.length() <= MAX_ITEM_LENGTH) {
                        scheduleItem(oneTimeItemName);
                        rawScheduledItems.addLast(oneTimeItemName); //Add the raw name to the list, before it is scheduled and reset to null
                        return;
                    }

                    //Inform the user about the error case about having an item greater than the maximum length
                    if (oneTimeItemName.length() > MAX_ITEM_LENGTH) {
                        Toast.makeText(getContext(), "Your item name needs to be under 100 characters long!", Toast.LENGTH_LONG).show();
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                //If the user's item has changed and is still a valid item
                if(editTextOneTimeItem.getText().length() >= 1) scheduleDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); //Enable the schedule item button, as a valid item is entered

                //When the user deletes all of their text in the edit text
                else scheduleDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); //disable the schedule item button, as no valid item exists
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

                        TextView startTimeSelected = dialogView.findViewById(R.id.text_start_time);

                        //If the specified time is possible
                        if(scheduledTimeLegitimate(startTimeInMs, endTimeInMs, currentTimeInMs, v.getContext())) {
                            //Convert the selected time to a readable format
                            startTime = new Time(startTimeInMs - midDayInMs); //Set the correct time, adjusting for a default value of mid day.
                            //Update the start time text view
                            startTimeSelected.setText(startTime.toString()); //Show the user what time they selected
                        }

                        //When the time is not legit
                        else {
                            startTimeInMs = defaultStartEndTimeInMs ; //Reset the selected time to default value
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
            @Override
            public void onClick(View buttonView) {
                //Get the current time when the user pressed the button that opens the picker
                int buttonEndHour = currentTime.get(Calendar.HOUR_OF_DAY);
                if(buttonEndHour <= 23) buttonEndHour++; //Set the default end time to be an hour in advance, which is a practical time.

                int buttonEndMinute = currentTime.get(Calendar.MINUTE);

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

                        TextView endTimeSelected = dialogView.findViewById(R.id.text_end_time); //Get a handle on the text view which displays the selected time

                        //If the specified time is possible
                        if(scheduledTimeLegitimate(startTimeInMs, endTimeInMs, currentTimeInMs, v.getContext())) {
                            //Store the entered time value
                            endTime = new Time(endTimeInMs - midDayInMs); //Set the correct time, adjusting for a default value of mid day.

                            //Update the end time text view
                            endTimeSelected.setText(endTime.toString()); //Show the user what time they selected
                        }

                        //When the time is not legit
                        else {
                            endTimeInMs = defaultStartEndTimeInMs; //Reset the selected time to default value
                            endTimeSelected.setText(R.string.awaiting_selection); //Reset to default value
                        }
                    }
                }, buttonEndHour, buttonEndMinute, true);

                timePickerDialog.setTitle("Select An End Time");
                timePickerDialog.show();
            }
        });
    }

   //This method uses the available data to input into the main recycler view in the schedule tab. It will format the entered so that it is displayed effectively.
    public void scheduleItem(String itemToSchedule) {
        String startTimeInText;
        String endTimeInText;

        //---Calculate the width of the times the user entered. Also add the times to the linkedlists---
        //When user enters start and end time
        if(startTime != null && endTime != null) {
            timeTextWidth = scheduledItemPaint.measureText(startTime + " - " + endTime); //Measure the width of the start and end time
            scheduledItemsStartTimes.addLast(startTime.toString());
            scheduledItemsEndTimes.addLast(endTime.toString());

            startTimeInText = startTime.toString();
            endTimeInText = endTime.toString();
        }

        //When the user only enters a start time or an end time
        else if(startTime != null ) {
            timeTextWidth = scheduledItemPaint.measureText(startTime + " - n/a");
            scheduledItemsStartTimes.addLast(startTime.toString());
            scheduledItemsEndTimes.addLast("n/a");

            startTimeInText = startTime.toString();
            endTimeInText = "n/a";
        }

        else if(endTime != null) {
            timeTextWidth = scheduledItemPaint.measureText("n/a - " + endTime);
            scheduledItemsStartTimes.addLast("n/a");
            scheduledItemsEndTimes.addLast(endTime.toString());

            startTimeInText = "n/a";
            endTimeInText = endTime.toString();
        }

        else {
            timeTextWidth = scheduledItemPaint.measureText("n/a - n/a"); //When user enters no time values
            scheduledItemsStartTimes.addLast("n/a");
            scheduledItemsEndTimes.addLast("n/a");

            startTimeInText = "n/a";
            endTimeInText = "n/a";
        }

        String fullFormattedItem = itemFormatter.FormatSchedulingItem(itemToSchedule, startTimeInText, endTimeInText); //Format the full unformatted item with times if they were entered

        //Add the formatted item to the list of scheduled items
        formattedScheduledItemsWithTimes.addLast(fullFormattedItem);
        scheduleItemListAdapter.notifyDataSetChanged(); //Refresh the recycler view to make the item appear

        //Add the completion status of uncomplete to the linked list
        scheduledItemsCompletionStatus.addLast("false");

        //---Write the data to the shared preferences files---
        int newScheduledItemIndex = formattedScheduledItemsWithTimes.size() - 1;
        sharedPrefsManager.updateItemAt(newScheduledItemIndex, itemToSchedule, rawScheduledItemsSharedPrefs);
        sharedPrefsManager.updateItemAt(newScheduledItemIndex, fullFormattedItem, formattedScheduledItemsSharedPrefs);
        sharedPrefsManager.updateItemAt(newScheduledItemIndex, startTimeInText, startTimesSharedPrefs);
        sharedPrefsManager.updateItemAt(newScheduledItemIndex, endTimeInText, endTimesSharedPrefs);
        sharedPrefsManager.updateItemAt(newScheduledItemIndex, "false", scheduledItemsCompletionStatusSharedPrefs);

        ToggleTutorialMessage(); //Hide the tutorial message if it was there
        ToggleUnscheduleAllButton(); //Enable the button if necessary

        //Reset the values that were used so that successive dialog uses can work fine
        existingItemSelected = null;
        startTime = null;
        startTimeInMs = defaultStartEndTimeInMs;
        endTime = null;
        endTimeInMs = defaultStartEndTimeInMs;
    }

    //This method will reformat the scheduled items to appropriately fit the change in device orientation and therefore landscape resolution
    private void reformatScheduledItems() {
        //Iterate through each item
        for(int itemIndex = 0; itemIndex < rawScheduledItems.size(); itemIndex++) {
            //Re-format each item and update the linked list
            String reformattedScheduledItem = itemFormatter.FormatSchedulingItem(rawScheduledItems.get(itemIndex), scheduledItemsStartTimes.get(itemIndex), scheduledItemsEndTimes.get(itemIndex));
            formattedScheduledItemsWithTimes.addLast(reformattedScheduledItem);

            //Update the shared prefs file with the new formatted items
            sharedPrefsManager.updateItemAt(itemIndex, reformattedScheduledItem, formattedScheduledItemsSharedPrefs);
        }
    }

    /* This method performs a series of checks to ensure that the schedule time(s) are able to be realistically completed. If the time is confirmed to be legit, true is returned.
    Appropriate error messages are displayed via Toasts when the selected time is not legit. */
    public Boolean scheduledTimeLegitimate(long startTimeInMs, long endTimeInMs, long currentTimeInMs, Context context) {
        //Check the cases when both a start time and an end time are entered
        if(startTimeInMs != defaultStartEndTimeInMs && endTimeInMs != defaultStartEndTimeInMs) {
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
            if(startTimeInMs != defaultStartEndTimeInMs && startTimeInMs < currentTimeInMs) {
                Toast.makeText(context, "You cannot start an item in the past!", Toast.LENGTH_LONG).show(); //Display an appropriate error message to let user know what was wrong about their selection
                return false;
            }

            //The user cannot end an item just as they schedule it, hence <=.
            if (endTimeInMs != defaultStartEndTimeInMs && endTimeInMs <= currentTimeInMs) {
                Toast.makeText(context, "You cannot end an item in the past!", Toast.LENGTH_LONG).show(); //Display an appropriate error message to let user know what was wrong about their selection
                return false;
            }
        }

        return true; //If this point is reached, the time is presumed to be legitimate
    }

    @Override
    public void onClickDialogItem(String itemName, boolean selected) {
        //On the case of an item being selected in the item list in the schedule new item dialog
        if(selected) {
            existingItemSelected = itemName; //Store the name of the item they selected
            //Disable the editText for the input of a one-time item, as the user has already selected an item
            editTextOneTimeItem.setEnabled(false);
            scheduleDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); //Enable the schedule item button, as an item is now selected
        }

        //On the case of an item being deselected in the item list
        else {
            existingItemSelected = null;
            editTextOneTimeItem.setEnabled(true); //re-enable the edit text to allow the user to enter an item this way

            //When the user deselects a recycler view item, and the edit text has no valid item entered, disable the schedule item button.
            if(editTextOneTimeItem.getText().length() < 1) scheduleDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); //Disable the schedule item button, as an item is now un-selected
        }
    }

    @Override
    public void onUpdateStartTime(String updatedStartTime, int scheduledItemIndex) {
        //Update the local linked list with the new time
        scheduledItemsStartTimes.set(scheduledItemIndex, updatedStartTime);

        //Get the new formatted string for the whole item
        String updatedFormattedScheduledItem = itemFormatter.FormatSchedulingItem(rawScheduledItems.get(scheduledItemIndex), scheduledItemsStartTimes.get(scheduledItemIndex), scheduledItemsEndTimes.get(scheduledItemIndex));

        //Update the formatted item in the recycler view
        formattedScheduledItemsWithTimes.set(scheduledItemIndex, updatedFormattedScheduledItem);
        scheduleItemListAdapter.notifyDataSetChanged();

        //Update the start time in the shared preferences file
        sharedPrefsManager.updateItemAt(scheduledItemIndex, updatedStartTime, startTimesSharedPrefs);

        //Update the new whole formatted item in the shared preferences file
        sharedPrefsManager.updateItemAt(scheduledItemIndex, updatedFormattedScheduledItem, formattedScheduledItemsSharedPrefs);
    }

    @Override
    public void onUpdateEndTime(String updatedEndTime, int scheduledItemIndex) {
        //Update the local linked list with the new time
        scheduledItemsEndTimes.set(scheduledItemIndex, updatedEndTime);

        //Get the new formatted string for the whole item
        String updatedFormattedScheduledItem = itemFormatter.FormatSchedulingItem(rawScheduledItems.get(scheduledItemIndex), scheduledItemsStartTimes.get(scheduledItemIndex), scheduledItemsEndTimes.get(scheduledItemIndex));

        //Update the formatted item in the recyclerview
        formattedScheduledItemsWithTimes.set(scheduledItemIndex, updatedFormattedScheduledItem);
        scheduleItemListAdapter.notifyDataSetChanged();

        //Update the end time in the shared preferences file
        sharedPrefsManager.updateItemAt(scheduledItemIndex, updatedEndTime, endTimesSharedPrefs);

        //Update the new whole formatted item in the shared preferences file
        sharedPrefsManager.updateItemAt(scheduledItemIndex, updatedFormattedScheduledItem, formattedScheduledItemsSharedPrefs);
    }

    @Override
    public void onUpdateItemName(String newItemName, int scheduledItemIndex) {
        //Update the local linked list for the raw item names
        rawScheduledItems.set(scheduledItemIndex, newItemName);

        //Get the new whole formatted item
        String updatedFormattedScheduledItem = itemFormatter.FormatSchedulingItem(rawScheduledItems.get(scheduledItemIndex), scheduledItemsStartTimes.get(scheduledItemIndex), scheduledItemsEndTimes.get(scheduledItemIndex));
        formattedScheduledItemsWithTimes.set(scheduledItemIndex, updatedFormattedScheduledItem); //Update the formatted items with the new formatted item name
        scheduleItemListAdapter.notifyDataSetChanged();

        //Update the raw item shared prefs file
        sharedPrefsManager.updateItemAt(scheduledItemIndex, newItemName, rawScheduledItemsSharedPrefs);

        //Update the formatted item shared prefs file
        sharedPrefsManager.updateItemAt(scheduledItemIndex, updatedFormattedScheduledItem, formattedScheduledItemsSharedPrefs);
    }

    @Override
    public void onUnscheduleItem(int scheduledItemIndex) {
        //---Remove all the scheduled item's data from the relevant lists---
        rawScheduledItems.remove(scheduledItemIndex); //Remove the item from the raw scheduled item names linked list
        formattedScheduledItemsWithTimes.remove(scheduledItemIndex);
        scheduledItemsStartTimes.remove(scheduledItemIndex);
        scheduledItemsEndTimes.remove(scheduledItemIndex);
        scheduledItemsCompletionStatus.remove(scheduledItemIndex);

        scheduleItemListAdapter.notifyDataSetChanged();

        //---Remove all the data from the relevant shared preferences files---
        sharedPrefsManager.removeItemAt(scheduledItemIndex, formattedScheduledItemsSharedPrefs, formattedScheduledItemsWithTimes);
        sharedPrefsManager.removeItemAt(scheduledItemIndex, rawScheduledItemsSharedPrefs, rawScheduledItems);
        sharedPrefsManager.removeItemAt(scheduledItemIndex, startTimesSharedPrefs, scheduledItemsStartTimes);
        sharedPrefsManager.removeItemAt(scheduledItemIndex, endTimesSharedPrefs, scheduledItemsEndTimes);
        sharedPrefsManager.removeItemAt(scheduledItemIndex, scheduledItemsCompletionStatusSharedPrefs, scheduledItemsCompletionStatus);

        ToggleTutorialMessage(); //Show the tutorial message if necessary
        ToggleUnscheduleAllButton(); //Disable the button if necessary
    }

    @Override
    public void onUpdateItemStatus(String complete, int scheduledItemIndex) {
        //Update the linked list for completion status
        scheduledItemsCompletionStatus.set(scheduledItemIndex, complete);
        sharedPrefsManager.updateItemAt(scheduledItemIndex, complete, scheduledItemsCompletionStatusSharedPrefs);
    }

    //This method is used to disable the 'unschedule all' FAB button when there are no items
    public void ToggleUnscheduleAllButton() {
        //Set to a nice dark grey for disabled
        if (formattedScheduledItemsWithTimes.size() == 0) {
            unscheduleAllButton.setEnabled(false);
            unscheduleAllButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor((R.color.disabledUnscheduleAllFAB))));
        }

        else {
            unscheduleAllButton.setEnabled(true);
            unscheduleAllButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.fb_unschedule_all_background)));
        }
    }

    //This method is used to hide and show the tutorial message when there are no scheduled items
    public void ToggleTutorialMessage() {
        if (formattedScheduledItemsWithTimes.size() >= 1) tutorialMessage.setVisibility(View.INVISIBLE);
        else tutorialMessage.setVisibility(View.VISIBLE); //Display the tutorial message when they are no items
    }
}