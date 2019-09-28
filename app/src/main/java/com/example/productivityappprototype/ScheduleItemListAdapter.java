package com.example.productivityappprototype;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import java.sql.Time;
import java.util.Calendar;
import java.util.LinkedList;

public class ScheduleItemListAdapter extends RecyclerView.Adapter<ScheduleItemListAdapter.ItemViewHolder> {
    private LayoutInflater mInflater;
    private LinkedList<String> rawScheduledItems; //The list of raw item names (Not including times) that the user has scheduled through the scheduling dialog. Some items may not appear in the item list.
    private final LinkedList<String> formattedScheduledItemsWithTimes; //The list of formatted items, including the times which the user has or has not selected.
    private LinkedList<String> scheduledItemsStartTimes; //Holds the associated start time for each scheduled item. If the user didn't specify a start time, then the value is "n/a"
    private LinkedList<String> scheduledItemsEndTimes; //Holds the associated end time for each scheduled item. If the user didn't specify a start time, then the value is "n/a"
    private LinkedList<String> scheduledItemsCompletionStatus;
    private UpdateScheduledItemInterface adapterInterface;

    public interface UpdateScheduledItemInterface {
        void onUpdateStartTime(String updatedStartTime, int scheduledItemIndex);
        void onUpdateEndTime(String updatedEndTime, int scheduledItemIndex);
        void onUpdateItemName(String newItemName, int scheduledItemIndex);
        void onUnscheduleItem(int scheduledItemIndex);
        void onUpdateItemStatus(String complete, int scheduledItemIndex); //When the user changes the state of the "Mark As Complete" checkbox
    }

    //Constructor for the class, for the context of the schedule fragment.
    public ScheduleItemListAdapter(ScheduleFragment scheduleFragment, LinkedList<String> rawScheduledItems, LinkedList<String> formattedScheduledItemsWithTimes, LinkedList<String> scheduledItemsStartTimes, LinkedList<String> scheduledItemsEndTimes,LinkedList<String> scheduledItemsCompletionStatus , UpdateScheduledItemInterface adapterInterface) {
        mInflater = LayoutInflater.from(scheduleFragment.getActivity()); //Initialise the inflater used to inflate the layout the view holder for each item
        this.rawScheduledItems = rawScheduledItems;
        this.formattedScheduledItemsWithTimes = formattedScheduledItemsWithTimes;
        this.scheduledItemsStartTimes = scheduledItemsStartTimes;
        this.scheduledItemsEndTimes = scheduledItemsEndTimes;
        this.scheduledItemsCompletionStatus = scheduledItemsCompletionStatus;
        this.adapterInterface = adapterInterface;
    }

    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView item; //The view holder that holds a productivity item
        final ScheduleItemListAdapter mAdapter;
        private TimePickerDialog timePickerDialog;
        private long defaultStartEndTimeInMs = 0xDEADBEEF;
        private long startTimeInMs = defaultStartEndTimeInMs; //The starting and ending time of an item if the user specifies them. Default value is 0xdeadbeef
        private long endTimeInMs = defaultStartEndTimeInMs;
        private Time startTime, endTime;
        private final int MAX_ITEM_LENGTH = 100;
        private final int MIN_ITEM_LENGTH = 1;
        private final String baseItemKey = "item:"; //The base key used to store the items in the bundle
        private final String ITEM_NOT_FOUND = "";

        //Initialises the view holder text view from the word xml resource and sets its adapter
        public ItemViewHolder(View itemView, ScheduleItemListAdapter adapter) {
            super(itemView);
            item = itemView.findViewById(R.id.item); //The text view which holds an item in the item list
            this.mAdapter = adapter;
            itemView.setOnClickListener(this); //Set the onClick listener to detect clicks
        }

        //The onclick event for each scheduled item
        @Override
        public void onClick(final View v) {
            //Give user ability to change the items and such
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getRootView().getContext());

            //Inflate the layout into a view for access to the components
            LayoutInflater inflater = LayoutInflater.from(v.getContext());
            final View dialogView = inflater.inflate(R.layout.schedule_edit_item_dialog, null);

            //Set the edit text to the name of the item
            final EditText scheduledItemEditText = dialogView.findViewById(R.id.edit_scheduled_item_name);
            final int scheduledItemIndex = getLayoutPosition(); //Get the index of the clicked scheduled item
            scheduledItemEditText.setText(rawScheduledItems.get(scheduledItemIndex));

            //Get and display the start time for the item the user selected
            TextView startTimeTextView = dialogView.findViewById(R.id.text_start_time);
            startTimeTextView.setText(scheduledItemsStartTimes.get(scheduledItemIndex));

            //Get and display the end time for the item the user selected
            TextView endTimeTextView = dialogView.findViewById(R.id.text_end_time);
            endTimeTextView.setText(scheduledItemsEndTimes.get(scheduledItemIndex));

            //Get a handle on the checkbox for marking an item as done
            final CheckBox markAsCompleted = dialogView.findViewById(R.id.checkBox_mark_as_completed);

            //Read from the shared prefs file to see if the box should be checked
            final SharedPreferences scheduledItemsCompletionStatusSharedPrefs = v.getRootView().getContext().getSharedPreferences("com.example.productivityappprototype.completion", Context.MODE_PRIVATE);
            final String fullItemKey = baseItemKey + scheduledItemIndex;
            if(scheduledItemsCompletionStatusSharedPrefs.getString(fullItemKey, ITEM_NOT_FOUND).equals("true")) markAsCompleted.setChecked(true);

            builder.setNeutralButton("Unschedule", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Remove the scheduled item from the recyclerview and the scheduled items lists
                    adapterInterface.onUnscheduleItem(scheduledItemIndex);
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //---See if the user has changed the item's completion status---
                    if(!Boolean.toString(markAsCompleted.isChecked()).equals(scheduledItemsCompletionStatusSharedPrefs.getString(fullItemKey, ITEM_NOT_FOUND))) {
                        //Strike through the text for items marked as complete, and de-strike items if necessary
                        if(markAsCompleted.isChecked()) item.setPaintFlags(item.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        else item.setPaintFlags(item.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

                        //Update the data with the new status
                        adapterInterface.onUpdateItemStatus(Boolean.toString(markAsCompleted.isChecked()), scheduledItemIndex);
                    }

                    String oldItemName = rawScheduledItems.get(scheduledItemIndex);
                    String newItemName = scheduledItemEditText.getText().toString();

                    //If the user changes the name of the scheduled item.
                    if(!oldItemName.equals(newItemName)){
                        //If the new name is legitimate
                        if(newItemName.length() >= MIN_ITEM_LENGTH && newItemName.length() <= MAX_ITEM_LENGTH) {
                            rawScheduledItems.set(scheduledItemIndex, newItemName); //Update the local linked list with the new name
                            //Update the linked lists and shared prefs files in the fragment
                            adapterInterface.onUpdateItemName(newItemName, scheduledItemIndex);
                            return;
                        }

                        //Inform user about their rejected input for the invalid cases
                        if (newItemName.length() < MIN_ITEM_LENGTH) Toast.makeText(v.getContext(), "Your item name needs to be at least one character!", Toast.LENGTH_LONG).show();
                        if (newItemName.length() > MAX_ITEM_LENGTH) Toast.makeText(v.getContext(), "Your item name needs to be under 100 characters long!", Toast.LENGTH_LONG).show();
                    }
                }
            });

            final Calendar currentTime = Calendar.getInstance();
            final long midDayInMs = 12*60*60*1000; //Used to account for the default value of mid day

            //Get a handle on the start time button
            final Button updateStartTime = dialogView.findViewById(R.id.button_start_time);
            //Set the listener for the start time button to allow for time picking to occur
            updateStartTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View buttonView) {
                    //Get the already set times or not to open the time picker at the currently selected time
                    int buttonStartHour = currentTime.get(Calendar.HOUR_OF_DAY); //Initialise with a default value of the current time
                    int buttonStartMinute = currentTime.get(Calendar.MINUTE);

                    //Update the default time picker values if they the user has chosen a time before
                    if(!scheduledItemsStartTimes.get(scheduledItemIndex).equals("n/a")) {
                        buttonStartHour = Integer.parseInt(scheduledItemsStartTimes.get(scheduledItemIndex).substring(0,2)); //get the first two characters, which is the start hour
                        buttonStartMinute = Integer.parseInt(scheduledItemsStartTimes.get(scheduledItemIndex).substring(3,5)); //get the third and fourth characters, which is the time in minutes
                    }

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

                            //Get the end time in ms to see if this time configuration is legitimate
                            if(!scheduledItemsEndTimes.get(scheduledItemIndex).equals("n/a")) {
                                int endHour = Integer.parseInt(scheduledItemsEndTimes.get(scheduledItemIndex).substring(0,2)); //get the hours
                                int endMinute = Integer.parseInt(scheduledItemsEndTimes.get(scheduledItemIndex).substring(3,5)); //get the minutes
                                endTimeInMs = (endHour*60*60*1000) + (endMinute*60*1000);
                            }

                            //If the specified time is possible
                            if(ScheduledTimeLegitimate(startTimeInMs, endTimeInMs, currentTimeInMs, v.getContext())) {
                                //Convert the selected time to a readable format
                                startTime = new Time(startTimeInMs - midDayInMs); //Set the correct time, adjusting for a default value of mid day.
                                //Update the start time text view
                                TextView startTimeSelected = dialogView.findViewById(R.id.text_start_time);
                                startTimeSelected.setText(startTime.toString()); //Show the user what time they selected

                                //Notify the fragment of the change
                                adapterInterface.onUpdateStartTime(startTime.toString(), scheduledItemIndex);
                            }

                            //When the time is not legit, nothing else needs to happen. They already get an error message. The time-textview will keep displaying the last acceptable time.
                        }
                    }, buttonStartHour, buttonStartMinute, true);

                    timePickerDialog.setTitle("Select A New Start Time");
                    timePickerDialog.show();
                }
            });


            //--Set the functionality of the Update End Time Button--
            //Initialise the calendar for the time setting buttons

            //Get a handle on the start time button
            final Button updateEndTime = dialogView.findViewById(R.id.button_end_time);
            //Set the listener for the start time button to allow for time picking to occur
            updateEndTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View buttonView) {
                    //Get the current time when the user pressed the button that opens the picker
                    int buttonEndHour = currentTime.get(Calendar.HOUR_OF_DAY);
                    if(buttonEndHour <= 23) buttonEndHour++; //Set the default end time to be an hour in advance, which is a practical time.

                    int buttonEndMinute = currentTime.get(Calendar.MINUTE);

                    //Update the default time picker values if they the user has chosen a time before
                    if(!scheduledItemsEndTimes.get(scheduledItemIndex).equals("n/a")) {
                        buttonEndHour = Integer.parseInt(scheduledItemsEndTimes.get(scheduledItemIndex).substring(0,2)); //get the first two characters, which is the start hour
                        buttonEndMinute = Integer.parseInt(scheduledItemsEndTimes.get(scheduledItemIndex).substring(3,5)); //get the third and fourth characters, which is the time in minutes
                    }

                    //Initialise the time picker dialog and its listener
                    timePickerDialog = new TimePickerDialog(v.getContext(), new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            //Convert the entered time to milliseconds to avoid a deprecated constructor for time object
                            endTimeInMs = (hourOfDay * 60 * 60 * 1000) + (minute * 60 * 1000);

                            //Get the current time again for more accuracy, as time can change in between opening the picker and actually selecting a time.
                            int currentHour = currentTime.get(Calendar.HOUR_OF_DAY);
                            int currentMinute = currentTime.get(Calendar.MINUTE);
                            long currentTimeInMs = (currentHour * 60 * 60 * 1000) + (currentMinute * 60 * 1000);

                            //Get the start time in ms to see if this time configuration is legitimate
                            if(!scheduledItemsStartTimes.get(scheduledItemIndex).equals("n/a")) {
                                int startHour = Integer.parseInt(scheduledItemsStartTimes.get(scheduledItemIndex).substring(0,2)); //get the hours
                                int startMinute = Integer.parseInt(scheduledItemsStartTimes.get(scheduledItemIndex).substring(3,5)); //get the minutes
                                startTimeInMs = (startHour*60*60*1000) + (startMinute*60*1000);
                            }

                            //If the specified time is possible
                            if(ScheduledTimeLegitimate(startTimeInMs, endTimeInMs, currentTimeInMs, v.getContext())) {
                                //Convert the selected time to a readable format
                                endTime = new Time(endTimeInMs - midDayInMs); //Set the correct time, adjusting for a default value of mid day.

                                //Update the start time text view
                                TextView endTimeSelected = dialogView.findViewById(R.id.text_end_time);
                                endTimeSelected.setText(endTime.toString()); //Show the user what time they selected

                                //Notify the fragment of the change
                                adapterInterface.onUpdateEndTime(endTime.toString(), scheduledItemIndex);
                            }

                            //When the time is not legit, nothing else needs to happen. They already get an error message. The time-textview will keep displaying the last acceptable time.
                        }
                    }, buttonEndHour, buttonEndMinute, true);

                    timePickerDialog.setTitle("Select A New End Time");
                    timePickerDialog.show();
                }
            });
            builder.setView(dialogView); //Set the custom layout for the dialog
            builder.create().show();
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
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View mItemView = mInflater.inflate(R.layout.item_list_item, viewGroup, false); //Inflate the layout for the item world holder which holds each item list item
        return new ItemViewHolder(mItemView, this); //Create the item view holder and return
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder itemViewHolder, int position) {
        String lightGrayBackgroundHex = "#d9d9d9"; //85% white according to w3 schools
        String darkGrayBackgroundHex = "#bfbfbf"; //75% white according to w3 schools

        //Make the text strike through if it needs to be
        if(scheduledItemsCompletionStatus.size() != 0 && scheduledItemsCompletionStatus.get(position).equals("true")) itemViewHolder.item.setPaintFlags(itemViewHolder.item.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        else itemViewHolder.item.setPaintFlags(itemViewHolder.item.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)); //Turn off strike through if necessary
        itemViewHolder.item.setText(formattedScheduledItemsWithTimes.get(position));

        //---Give the items an alternating coloured background---
        if((position % 2) == 1)itemViewHolder.item.setBackgroundColor(Color.parseColor(darkGrayBackgroundHex));
        else itemViewHolder.item.setBackgroundColor(Color.parseColor(lightGrayBackgroundHex));
    }

    @Override
    public int getItemCount() {
        return formattedScheduledItemsWithTimes.size();
    }
}