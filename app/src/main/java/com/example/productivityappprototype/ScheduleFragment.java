package com.example.productivityappprototype;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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

public class ScheduleFragment extends Fragment implements View.OnClickListener, ScheduleDialogItemListAdapter.AddScheduledItemInterface, ScheduleItemListAdapter.UpdateScheduledItemInterface {
    private SharedPreferences sharedPreferences;
    private String sharedPreferencesFile = "com.example.productivityappprototype";
    private final String ITEM_NOT_FOUND = "";
    private final String baseItemKey = "item:"; //The base key used to store the items in the bundle

    private LinkedList<String> itemList = new LinkedList<>(); //The list which holds items found in the item list tab. Note: does not include items the user enters through the edit text in the schedule item dialog.
    private LinkedList<String> rawScheduledItems = new LinkedList<>(); //The list of raw item names (Not including times) that the user has scheduled through the scheduling dialog. Some items may not appear in the item list.
    private LinkedList<String> formattedScheduledItemsWithTimes = new LinkedList<>(); //The list of formatted items, including the times which the user has or has not selected.
    private LinkedList<String> scheduledItemsStartTimes = new LinkedList<>(); //Holds the associated start time for each scheduled item. If the user didn't specify a start time, then the value is "n/a"
    private LinkedList<String> scheduledItemsEndTimes = new LinkedList<>(); //Holds the associated end time for each scheduled item. If the user didn't specify a start time, then the value is "n/a"

    private String selectedItemInDialog; //The string which holds the name of the item the user selects in the AlertDialog
    private EditText editTextOneTimeItem;
    private AlertDialog scheduleDialog;
    private TimePickerDialog timePickerDialog;
    private long defaultStartEndTimeInMs = 0xDEADBEEF;
    private long startTimeInMs = defaultStartEndTimeInMs; //The starting and ending time of an item is a default value. Not using 0 because 0 is a legitimate time
    private long endTimeInMs = defaultStartEndTimeInMs;
    private Time startTime, endTime;
    private RecyclerView scheduleRecyclerView;
    private com.example.productivityappprototype.ScheduleItemListAdapter scheduleItemListAdapter;
    private final int MAX_ITEM_LENGTH = 100;
    private final int MIN_ITEM_LENGTH = 1;
    private TextView itemWidthMeasuringContainer, timeHeaderTextView;
    float recyclerViewWidth, timeTextWidth; //values used for boundary calculations
    private Paint scheduledItemPaint, timeHeaderPaint;

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
        timeHeaderTextView = v.findViewById(R.id.text_time);

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
        //Get a handle to the recycler view for the scheduled items
        scheduleRecyclerView = getView().findViewById(R.id.recyclerview_schedule);

        //Create the adapter and supply the data to be displayed
        scheduleItemListAdapter = new ScheduleItemListAdapter(this, rawScheduledItems, formattedScheduledItemsWithTimes, scheduledItemsStartTimes, scheduledItemsEndTimes, this);
        scheduleRecyclerView.setAdapter(scheduleItemListAdapter); //Attach the adapter to the recycler view

        //Assign the recycler view a default layout manager
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));

        //Get a handle on the textview that all items go into to allow measuring of width of text
        itemWidthMeasuringContainer = view.findViewById(R.id.text_width_measuring);

        /*Get a handle on the textview to calculate the boundary which will allow for the time text to be placed
        underneath the "Scheduled Time" header, and for the item text to go before it
        */
        timeHeaderTextView = view.findViewById(R.id.text_time);

        //Initialise paint objects used by the 3 related scheduled item formatting methods
        timeHeaderPaint = timeHeaderTextView.getPaint(); //Used to calculate the width of the header to calculate the boundary
        scheduledItemPaint = itemWidthMeasuringContainer.getPaint();
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
        final View dialogView = inflater.inflate(R.layout.schedule_new_item_dialog, null); //Inflate the layout

        //--Initiate the recycler view in the dialog layout to display the item list items--
        UpdateItemList(); //Read in the SharedPreferences file again to ensure both item lists are identical
        RecyclerView dialogRecyclerView = dialogView.findViewById(R.id.recyclerview_schedule_dialog); //Get a handle to the recyclerview
        ScheduleDialogItemListAdapter adapter = new ScheduleDialogItemListAdapter(this, itemList, this); //Create the correct adapter and supply the data with the custom adapter
        dialogRecyclerView.setAdapter(adapter); //Set the adapter
        dialogRecyclerView.setLayoutManager(new LinearLayoutManager(dialogView.getContext())); //Assign the recyclerview a default layout manager

        builder.setView(dialogView); //Set the now-created layout to the dialog

        //Set the dialog buttons
        builder.setPositiveButton("Schedule Item", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //If the user selected an item in some way before pressing 'schedule item'
                if (selectedItemInDialog != null) {
                    rawScheduledItems.addLast(selectedItemInDialog); //Add the raw name to the list, before it is scheduled and reset to null
                    ScheduleItem(); //Schedule the item with the given data
                    return;
                }

                //See if the user entered an item in the one-time option editText instead of selecting an item in the recyclerview
                else {
                    if(editTextOneTimeItem.getText().toString().length() != 0) {
                        //Extract and store the name of the item the user entered
                        selectedItemInDialog = editTextOneTimeItem.getText().toString();

                        //When the item entered this way is legitimate (to stop extremely long names)
                        if (selectedItemInDialog.length() >= MIN_ITEM_LENGTH && selectedItemInDialog.length() <= MAX_ITEM_LENGTH) {
                            rawScheduledItems.addLast(selectedItemInDialog); //Add the raw name to the list, before it is scheduled and reset to null
                            ScheduleItem();
                            return;
                        }

                        //Inform the user about the error case about having an item greater than the maximum length
                        if (selectedItemInDialog.length() > MAX_ITEM_LENGTH) {
                            Toast.makeText(getContext(), "Your item name needs to be under 100 characters long!", Toast.LENGTH_LONG).show();
                            return;
                        }
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

        editTextOneTimeItem = dialogView.findViewById(R.id.edit_scheduled_item_name); //Get a handle to the edit text for the one-time item
        //--Set the listeners for the text in the edit text changing--
        editTextOneTimeItem.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

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

                        TextView startTimeSelected = dialogView.findViewById(R.id.text_update_start_time_selected);

                        //If the specified time is possible
                        if(ScheduledTimeLegitimate(startTimeInMs, endTimeInMs, currentTimeInMs, v.getContext())) {
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

                        TextView endTimeSelected = dialogView.findViewById(R.id.text_update_end_time_selected); //Get a handle on the text view which displays the selected time

                        //If the specified time is possible
                        if(ScheduledTimeLegitimate(startTimeInMs, endTimeInMs, currentTimeInMs, v.getContext())) {
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
                }, buttonStartHour, buttonStartMinute, true);

                timePickerDialog.setTitle("Select An End Time");
                timePickerDialog.show();
            }
        });
    }

    //This method uses the available data to input into the main recycler view in the schedule tab. It will format the entered so that it is displayed effectively.
    public void ScheduleItem() {
        String fullUnformattedItem = selectedItemInDialog; //The base item for the full item is the entered item name.
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

        String fullFormattedItem = FormatSchedulingItem(fullUnformattedItem, startTimeInText, endTimeInText); //Format the full unformatted item with times if they were entered

        //Add the formatted item to the list of scheduled items
        formattedScheduledItemsWithTimes.addLast(fullFormattedItem);
        scheduleItemListAdapter.notifyDataSetChanged(); //Refresh the recycler view to make the item appear

        //Reset the values that were used so that successive dialog uses can work fine
        selectedItemInDialog = null;
        startTime = null;
        startTimeInMs = defaultStartEndTimeInMs;
        endTime = null;
        endTimeInMs = defaultStartEndTimeInMs;
    }

    //This method takes in an item to format to allow for a visual distinction between the item name and the entered times of the item
    //It will pad short items accordingly to make the item times line up in the scheduled time tab.
    //It insert newline characters to put longer items on multiple lines to allow the times to be aligned correctly.
    public String FormatSchedulingItem(String itemToSchedule, String startTime, String endTime) {
        //--Declare and initialise DisplayMetrics object used to find the width of the display--
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(dm); //Obtain the metrics of the display

        recyclerViewWidth = dm.widthPixels - 8 - 8; //Get and log the width of the screen
        String fullTimeText = startTime + " - " + endTime; //Allow accurate measuring of the time

        //Calculate the amount of padding required to appropriately pad the time to the end of the recyclerview
        float itemBoundaryDifference = recyclerViewWidth - scheduledItemPaint.measureText(itemToSchedule) - timeHeaderPaint.measureText(getString(R.string.text_time)) - 12 - scheduledItemPaint.measureText("aa");

        //-----Pad short items appropriately-----
        if(itemBoundaryDifference >= 0) {
            //Calculate the required padding to the end of the recyclerview
            float paddingToEnd = recyclerViewWidth - scheduledItemPaint.measureText(itemToSchedule) - scheduledItemPaint.measureText(fullTimeText) - scheduledItemPaint.measureText("aaa");
            String paddedItem = itemToSchedule + GetPadding(paddingToEnd);

            //--Append the selected times to the item if they were entered--
            paddedItem += fullTimeText;
            return paddedItem; //The item is final and complete now
        }

        //-----Split up longer items onto new line(s)-----
        else return FormatLongItemName(itemToSchedule, startTime, endTime);
    }

    //This method will take in long items that span across one or more lines, and return an appropriately split and padded item such that the time text is visually separated from the item text.
    //This allows a predictable placement of items and scheduled time, for readability purposes.
    public String FormatLongItemName(String enteredItem, String startTime, String endTime) {
        String workingItem = enteredItem; //Initialise with entered item
        String fullItem = ""; //Will be build up, formatted line by formatted line until it is done
        String excessItem = "";
        String splitEnteredItem = "";
        boolean firstLine = true; //The first line has the time text and needs to be distinguished from nth lines
        boolean itemWordTooLong = false; //Is made true when a single word is too long

        //Keep formatting until the working item is an acceptable length
        while(true) {
            if(excessItem != "") workingItem = excessItem; //After a line has been split and still has text, update the working item

            float workingItemBoundaryDifference = recyclerViewWidth - scheduledItemPaint.measureText(workingItem) - timeHeaderPaint.measureText(getString(R.string.text_time)) - 12 - scheduledItemPaint.measureText("aa");
            //If the working item is not too long
            if (workingItemBoundaryDifference >= 0) {
                fullItem += workingItem; //Build the final full item and return
                return fullItem;
            }

            String[] workingItemWords = workingItem.split(" "); //Extract the words of the working item

            //Iterate through the words backwards
            for (int word = workingItemWords.length - 1; word >= 0; word--) {

                //--check for and fix massive clumps of characters that are sufficiently wide to cause problems--
                //When the single word is enough to break the boundary line
                if(recyclerViewWidth - scheduledItemPaint.measureText(workingItemWords[word]) - timeHeaderPaint.measureText(getString(R.string.text_time)) - 12 - scheduledItemPaint.measureText("aa") < 0) {
                    itemWordTooLong = true;

                    String splitWords = SplitLongItemWord(workingItemWords[word]) + " "; //Get the acceptable individual words from the big word
                    //--Update the workingItemWords--
                    //Get the words before the big word
                    String previousWords = "";
                    for(int i = 0; i < word; i++) previousWords += workingItemWords[i] + " ";

                    //Get the words after the big word, if there are any
                    String afterWords = "";
                    if(workingItemWords.length - 1 > word) {
                        for(int j = word + 1; j <= workingItemWords.length - 1; j++) {
                            afterWords += workingItemWords[j];
                            if(j != workingItemWords.length - 1) afterWords += " "; //Avoid having a space at the end of the string
                        }
                    }

                    String newWorkingItemWords = previousWords + splitWords +  afterWords;
                    splitEnteredItem = newWorkingItemWords;
                    workingItem = newWorkingItemWords; //Update the working item to include the broken up big word
                    break;
                }

                //--Delete the last word from the string--
                int endingIndex = workingItem.length() - 1 - workingItemWords[word].length();
                workingItem = workingItem.substring(0, endingIndex); //Delete the last word

                //Get the boundary difference with the now shortened word
                workingItemBoundaryDifference = recyclerViewWidth - scheduledItemPaint.measureText(workingItem) - timeHeaderPaint.measureText(getString(R.string.text_time)) - 12 - scheduledItemPaint.measureText("aa");

                //If the now-shortened item is not too long
                if(workingItemBoundaryDifference >= 0) {
                    workingItem += " "; //Add the space, to account for the splitting

                    if(firstLine) {
                        //Extract the excess item
                        if(!itemWordTooLong) excessItem = enteredItem.substring(workingItem.length()); //Obtain the part of the item that was separated. Must be done before the time is added back to the item
                        else excessItem = splitEnteredItem.substring(workingItem.length());

                        //Add the extra padding to move the time value to the end of the recyclerview
                        String timeText = startTime + " - " + endTime;
                        float paddingToEnd = recyclerViewWidth - scheduledItemPaint.measureText(workingItem) - scheduledItemPaint.measureText(timeText) - scheduledItemPaint.measureText("aaa"); //Safe buffer of three characters to prevent extending onto the next line if given measurements are not completely accurate
                        workingItem += GetPadding(paddingToEnd); //Add on the necessary padding to make the time value sit at the end of the recyclerview

                        //Append the times to the working item
                        workingItem += timeText + "\n";
                        fullItem += workingItem;

                        firstLine = false; //It is not the first line anymore
                        break;
                    }

                    //For lines other than the first line
                    else {
                        excessItem = excessItem.substring(workingItem.length()); //Obtain the part of the item that was separated, from the old excess item
                        fullItem += workingItem + "\n"; //Add this acceptable line to the full item
                        break;
                    }
                }
            }
        }
    }

    //This method deals with the cases where the user puts in a collection of characters with no space that is large enough to break the boundary for the time text
    //It will split the long 'word' at an appropriate point that no longer breaks the boundary
    public String SplitLongItemWord(String longItemWord) {
        String workingWord = longItemWord; //Initialise with the long item word, which is shortened later
        String excessWord = "";
        String splitWords = ""; //Split words by space in one string, for ease of implementation by the FormatLongItemName method

        while(true) {
            if(excessWord != "") workingWord = excessWord; //After a line has been split and still has text, update the working item

            float wordBoundaryDifference = recyclerViewWidth - scheduledItemPaint.measureText(workingWord) - timeHeaderPaint.measureText(getString(R.string.text_time)) - 12 - scheduledItemPaint.measureText("aa");

            //If the working word is not too long, build and complete the finished word
            if (wordBoundaryDifference >= 0) {
                splitWords += workingWord; //Build the final full item and return
                return splitWords;
            }

            char[] wordCharacters = workingWord.toCharArray(); //Extract the characters of the long 'word'

            //Iterate through the characters, from the end to the start
            for(int character = wordCharacters.length - 1; character >= 0; character--) {
                //Delete the last character from the original word
                int endingIndex = workingWord.length() - 1 - 1;
                workingWord = workingWord.substring(0, endingIndex); //Delete the last character

                //Calculate the new boundary difference
                wordBoundaryDifference = recyclerViewWidth - scheduledItemPaint.measureText(workingWord) - timeHeaderPaint.measureText(getString(R.string.text_time)) - 12 - scheduledItemPaint.measureText("aa");

                //When the word is now an acceptable length
                if(wordBoundaryDifference > 0) {
                    splitWords += workingWord + " "; //Add the acceptable word to the full word

                    //Extract the excess item
                    if(excessWord == "") {
                        excessWord = longItemWord.substring(workingWord.length());
                    }

                    else excessWord = excessWord.substring(workingWord.length()); //Obtain the part of the item that was separated, from the old excess item

                    break;
                }
            }
        }
    }

    public String GetPadding(float widthToPad) {

        //Measure the width of one pad unit in pixels
        String padUnit = " "; //One unit of padding is one space
        float padUnitWidth = scheduledItemPaint.measureText(padUnit);

        double numberOfPadsRequired = Math.floor(widthToPad / padUnitWidth); //Round down the number of pads required. Padding less is better than padding too much
        String pad = "";

        //Add the correct number of pads to the item
        for(int pads = 0; pads < numberOfPadsRequired; pads++) {
            pad += padUnit;
        }

        return pad;
    }


    /* This method performs a series of checks to ensure that the schedule time(s) are able to be realistically completed. If the time is confirmed to be legit, true is returned.
    Appropriate error messages are displayed via Toasts when the selected time is not legit. */
    public Boolean ScheduledTimeLegitimate(long startTimeInMs, long endTimeInMs, long currentTimeInMs, Context context) {

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

    @Override
    public void OnUpdateStartTime(String updatedStartTime, int scheduledItemIndex) {
        //Update the local linked list with the new time
        scheduledItemsStartTimes.set(scheduledItemIndex, updatedStartTime);

        //Get the new formatted string for the whole item
        String updatedFormattedScheduledItem = FormatSchedulingItem(rawScheduledItems.get(scheduledItemIndex), scheduledItemsStartTimes.get(scheduledItemIndex), scheduledItemsEndTimes.get(scheduledItemIndex));

        //Update the formatted item in the recyclerview
        formattedScheduledItemsWithTimes.set(scheduledItemIndex, updatedFormattedScheduledItem);
        scheduleItemListAdapter.notifyDataSetChanged();
    }

    @Override
    public void OnUpdateEndTime(String updatedEndTime, int scheduledItemIndex) {
        //Update the local linked list with the new time
        scheduledItemsEndTimes.set(scheduledItemIndex, updatedEndTime);

        //Get the new formatted string for the whole item
        String updatedFormattedScheduledItem = FormatSchedulingItem(rawScheduledItems.get(scheduledItemIndex), scheduledItemsStartTimes.get(scheduledItemIndex), scheduledItemsEndTimes.get(scheduledItemIndex));

        //Update the formatted item in the recyclerview
        formattedScheduledItemsWithTimes.set(scheduledItemIndex, updatedFormattedScheduledItem);
        scheduleItemListAdapter.notifyDataSetChanged();
    }

    @Override
    public void OnUpdateItemName(String newItemName, int scheduledItemIndex) {
        //update the item names
        rawScheduledItems.set(scheduledItemIndex, newItemName); //Update the raw item name in the list

        //Get the new whole formatted item
        String updatedFormattedScheduledItem = FormatSchedulingItem(rawScheduledItems.get(scheduledItemIndex), scheduledItemsStartTimes.get(scheduledItemIndex), scheduledItemsEndTimes.get(scheduledItemIndex));
        formattedScheduledItemsWithTimes.set(scheduledItemIndex, updatedFormattedScheduledItem); //Update the formatted items with the new formatted item name
        scheduleItemListAdapter.notifyDataSetChanged();
    }

    @Override
    public void OnUnscheduleItem(String itemToUnschedule, int scheduledItemIndex) {
        //---Remove all the scheduled item's data from the relevant lists---
        rawScheduledItems.remove(scheduledItemIndex); //Remove the item from the raw scheduled item names linked list
        formattedScheduledItemsWithTimes.remove(scheduledItemIndex);
        scheduledItemsStartTimes.remove(scheduledItemIndex);
        scheduledItemsEndTimes.remove(scheduledItemIndex);

        scheduleItemListAdapter.notifyDataSetChanged();
    }
}