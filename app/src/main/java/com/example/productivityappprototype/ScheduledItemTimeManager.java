package com.example.productivityappprototype;

import android.content.Context;
import android.widget.Toast;

public class ScheduledItemTimeManager {
    private Context context;

    public ScheduledItemTimeManager(Context context) {
        this.context = context;

    }

    /* This method performs a series of checks to ensure that the schedule time(s) are able to be realistically completed. If the time is confirmed to be legit, true is returned.
        Appropriate error messages are displayed via Toasts when the selected time is not legit. */
    public Boolean IsScheduledTimeLegitimate(long startTimeInMs, long endTimeInMs, long currentTimeInMs, Context context) {
        long defaultStartEndTimeInMs = 0xDEADBEEF;
        //Check the cases when both a start time and an end time are entered
        if(startTimeInMs != defaultStartEndTimeInMs && endTimeInMs != defaultStartEndTimeInMs) {
            //1. Check if the user has planned to start or end an item before the current time (in the past)
            if(startTimeInMs < currentTimeInMs  || endTimeInMs < currentTimeInMs) {
                Toast.makeText(context, "You cannot schedule an item to start or end before the current time!", Toast.LENGTH_LONG).show(); //Display an appropriate error message to let user know what was wrong about their selection
                return false;
            }

            //2. If the start time is not before the end time it is invalid.
            if(!(startTimeInMs < endTimeInMs)) {
                Toast.makeText(context, "Your start time needs to be before your end time!", Toast.LENGTH_LONG).show(); //Display an appropriate error message to let user know what was wrong about their selection
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
    //This method will analyse the given start and end times in ms to calculate and return the text used to display the item duration to the user
    public String GetDurationText(String startTimeInText, String endTimeInText) {
        //Duration can only be calculated if start and end times exist. ("n/a" is the default value for not-selected)
        String notApplicable = context.getString(R.string.n_a);
        if(startTimeInText.equals(notApplicable) || endTimeInText.equals(notApplicable)) return  notApplicable;

        //Get the time in ms from the given times
        long calculatedStartTimeInMs = GetStringTimeInMs(startTimeInText);
        long calculatedEndTimeInMs = GetStringTimeInMs(endTimeInText);

        //Calculate the duration in ms
        long durationInMs = calculatedEndTimeInMs - calculatedStartTimeInMs;
        int durationMinutes = (int)(durationInMs / (1000*60));
        int durationHours = durationMinutes / 60;

        //Build the duration text that has hour(s)
        if(durationHours >= 1) {
            durationMinutes = durationMinutes - (durationHours * 60); //Get the remaining minutes
            String durationText = "";

            //Add the grammatically correct hour(s) part
            if(durationHours == 1) durationText += durationHours + " hour ";
            else durationText += durationHours + " hours ";

            //Add the grammatically correct minutes minutes if there is a value
            if(durationMinutes == 0) return durationText; //skip the '0 minutes' part
            else if(durationMinutes == 1) return durationText + durationMinutes + " minute"; //e.g. 1 hour 1 minute
            else return durationText + durationMinutes + " minutes"; //e.g. 2 hours 37 minutes
        }

        //Build the duration text that is only in minutes
        if(durationMinutes == 1) return durationMinutes + " minute";
        else return durationMinutes + " minutes";
    }

    public long GetStringTimeInMs(String validTime) {
        String[] timeParts = validTime.split(":"); //Get the hour, minute, and second sections

        //Calculate the start time in ms.
        long calculatedTimeInMs = Integer.parseInt(timeParts[0]) * 1000 * 60 * 60; //The hours
        calculatedTimeInMs += Integer.parseInt(timeParts[1]) * 1000 * 60; //The minutes

        return calculatedTimeInMs;
    }
}