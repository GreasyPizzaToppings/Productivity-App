package com.example.productivityappprototype;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.LinkedList;

public class StatisticsManager {
    private View view;

    //global variables that are set in methods below that didn't make as much sense to return in an array of values
    private int productiveTimeInMinutes;
    private int workTimeInMinutes;
    private boolean invalidTimesFormat = true;
    private int parentheseWordIndex;
    private float strenuityMultiplier = 0;
    private float lowProportion = 0;
    private float medProportion = 0;
    private float highProportion = 0;

    private float totalWorkHours;
    private float totalProductiveHours;
    private float lowMentallyStrenuousProductiveHours;
    private float medMentallyStrenuousProductiveHours;
    private float highMentallyStrenuousProductiveHours;
    private int noItemsIncorrectlyFormatted;

    //We only need the scheduled items' text to be able to calculate all the statistics
    public StatisticsManager(View view) {
        this.view = view;
    }

    //This method calculates productivity statistics from the scheduled items for those that follow the format described below.
    public void GetProductivityStatistics(LinkedList<String> rawScheduledItems) {
        /* THE EXPECTED FORMAT
        Format: [item text] [mental strenuity indicator] ([total work time]) ([productive work time]). E.g. 'Study math .65MH (75) (50)' means studying math was 65% mentally strenuous and 35% highly strenous, took 75 minutes, of which, 50 were productive.

        The format of the mental strenuity indicator is: [some number, or not][Either L,M,H or two of these (for simplicity, 3 is not allowed)]. E.g. L, MH, .5LM, 0.375HM are all valid.
        Strenuity indicators without a leading number mean the number is evenly divided between the level(s) of mental strenuity described. E.g. L means 100% goes to "Low", MH means 50% goes to "Medium" and 50% goes to "High".
        Strenuity indicators with a leading number divide the time into the described mental strenuity categories by the described amount. E.g. .35MH means 35% goes to "Medium" and 65% goes to "High". .5L is an invalid value

        Total work time in minutes is an integer ONLY value inside a set of parentheses. E.g. '(50)' means 50 minutes of work time
        Productive work time in minutes is the time spent doing the task that was productive. E.g. '(45)' means 45 minutes were spend productively.
        Note: it is possible to be 100% productive, and for convenience, a valid format is also one where there is only one set of parentheses for both total work time and productive time. E.g. '(80)' means 80 minutes were productive, and the task took 80 minutes.
        */

        //The statistics to collect
        int totalWorkMinutes = 0;
        int totalProductiveMinutes = 0;
        float lowMentallyStrenuousWorkMinutes = 0;
        float lowMentallyStrenuousProductiveMinutes = 0;
        float medMentallyStrenuousWorkMinutes = 0;
        float medMentallyStrenuousProductiveMinutes = 0;
        float highMentallyStrenuousWorkMinutes = 0; //not used at the moment but someone else might like this.
        float highMentallyStrenuousProductiveMinutes = 0;
        noItemsIncorrectlyFormatted = 0; //Store the number of items that were not correctly formatted to allow calculation

        //Process each scheduled item
        for(int scheduledItemIndex = 0; scheduledItemIndex < rawScheduledItems.size(); scheduledItemIndex++) {
            //Get the individual words
            String scheduledItemText = rawScheduledItems.get(scheduledItemIndex);
            String[] scheduledItemWords = scheduledItemText.split(" ");

            Log.e("test", "***now testing the item: '" + scheduledItemText + "' for a valid format");

            //items with less than 2 words are immediately invalid for the format
            if(scheduledItemWords.length < 2) {
                noItemsIncorrectlyFormatted++;
                continue;
            }

            GetWorkAndProductiveTimes(scheduledItemWords);

            if(invalidTimesFormat) {
                noItemsIncorrectlyFormatted++;
                continue; //process the next scheduled item
            }

            //---Extract the data from the mental strenuity indicator---
            //Get the word we expect is strenuity indicator and numerical multiplier in lowercase
            String mentalStrenuityIndicator = scheduledItemWords[parentheseWordIndex - 1].toLowerCase();

            //---Extract the number portion (explicitly or implicitly stated)---
            String sStrenuityMultiplier = ""; //the string representation of the number at the start that determines the proportion of the work to each mental strenuity category.

            //Read until we dont find a number
            int lastIndexOfNumber = -1; //account for offset
            for(char letter : mentalStrenuityIndicator.toCharArray()) {
                String sLetter = "" + letter;
                if(sLetter.matches("[0-9.-/+]+")) {
                    sStrenuityMultiplier += letter;
                }
                else break;
            }

            //Make sure a specified strenuity multiplier is valid
            if(!IsStrenuityMultiplierValid(sStrenuityMultiplier)) {
                noItemsIncorrectlyFormatted++;
                continue;
            }

            //---Extract and check the letters determining the mental strenuity category---
            String strenuityCategory = mentalStrenuityIndicator.substring(sStrenuityMultiplier.length()).toLowerCase(); //would this fail for a number input?

            if(!IsStrenuityCategoryValid(strenuityCategory)) {
                noItemsIncorrectlyFormatted++;
                continue;
            }

            //Calculate the implicitly stated mental strenuity multiplier
            if(sStrenuityMultiplier.equals("")) strenuityMultiplier = 1f / (strenuityCategory.length()); //e.g. l = 100% goes to l, mh = 50% goes to m 50% goes to h.

            //100% of the time should go to one category if only one category is specified
            if(strenuityMultiplier != 1 && strenuityCategory.length() == 1) {
                Log.e("test", "INVALID: the strenuity multiplier of " + strenuityMultiplier + " does not make sense for the category of " + strenuityCategory);
                noItemsIncorrectlyFormatted++;
                continue;
            }

            Log.e("test", "VALID: the input item of " + scheduledItemText + " is found to be in the correct format");

            GetMentalStrenuityProportions(strenuityCategory);

            //Calculate and add the work and productive time for each category!!
            lowMentallyStrenuousWorkMinutes += Math.round(lowProportion * workTimeInMinutes);
            lowMentallyStrenuousProductiveMinutes += Math.round(lowProportion * productiveTimeInMinutes);

            medMentallyStrenuousWorkMinutes += Math.round(medProportion * workTimeInMinutes);
            medMentallyStrenuousProductiveMinutes += Math.round(medProportion * productiveTimeInMinutes);

            highMentallyStrenuousWorkMinutes += Math.round(highProportion * workTimeInMinutes);
            highMentallyStrenuousProductiveMinutes += Math.round(highProportion * productiveTimeInMinutes);

            totalWorkMinutes += workTimeInMinutes;
            totalProductiveMinutes += productiveTimeInMinutes;
        }

        Log.e("test", "There were " + noItemsIncorrectlyFormatted + " items incorrectly formatted");

        //Convert values to hours
        totalWorkHours = Math.round((totalWorkMinutes / 60f) *100f)/100f;
        totalProductiveHours = Math.round((totalProductiveMinutes / 60f)*100f)/100f;
        lowMentallyStrenuousProductiveHours = Math.round((lowMentallyStrenuousProductiveMinutes / 60f)*100f)/100f;
        medMentallyStrenuousProductiveHours = Math.round((medMentallyStrenuousProductiveMinutes / 60f)*100f)/100f;
        highMentallyStrenuousProductiveHours = Math.round((highMentallyStrenuousProductiveMinutes / 60f)*100f)/100f;

        //If there was at least one correctly formatted item, display statistics
        if(noItemsIncorrectlyFormatted != rawScheduledItems.size()) DisplayStatistics(false);
        else DisplayStatistics(true);
    }

    //Yeah I know passing this many arguments is kind of bad practice. But global variables isn't much better
    private void DisplayStatistics(boolean allInvalid) {
        //Display all the statistics with gaps between the numbers
        TextView summaryTextView = view.findViewById(R.id.text_work_summary);

        if(allInvalid) {
            summaryTextView.setText("");
            return;
        }

        summaryTextView.setText(
                "Work Time:  " + totalWorkHours + " hrs \n" +
                "Prod. Time:  " + totalProductiveHours + " hrs \n" +
                "L Prod. Time:  " + lowMentallyStrenuousProductiveHours + " hrs \n" +
                "M Prod. Time:  " + medMentallyStrenuousProductiveHours + " hrs \n" +
                "H Prod. Time:  " + highMentallyStrenuousProductiveHours + " hrs \n" +
                "No. Bad Formats:  " + noItemsIncorrectlyFormatted);
    }

    //Calculate the specific proportions for each mental strenuity category
    private void GetMentalStrenuityProportions(String strenuityCategory) {
        lowProportion = 0;
        medProportion = 0;
        highProportion = 0;

        //Look at the first letter
        if(strenuityCategory.charAt(0) == 'l') lowProportion = strenuityMultiplier;
        else if (strenuityCategory.charAt(0) == 'm') medProportion = strenuityMultiplier;
        else if (strenuityCategory.charAt(0) == 'h') highProportion = strenuityMultiplier;

        //Look at the second letter if it exists
        if(strenuityCategory.length() == 2) {
            if(strenuityCategory.charAt(1) == 'l') lowProportion = 1 - strenuityMultiplier;
            else if (strenuityCategory.charAt(1) == 'm') medProportion = 1 - strenuityMultiplier;
            else if (strenuityCategory.charAt(1) == 'h') highProportion = 1 - strenuityMultiplier;
        }

        Log.e("test", "VALUES: low: '" + lowProportion + "' med: '" + medProportion + "' high: '" + highProportion + " for the strenuity category and multiplier of " + strenuityMultiplier + strenuityCategory);
    }

    //Checks the validity of the strenuity multiplier, if it was specified
    private boolean IsStrenuityMultiplierValid(String sStrenuityMultiplier) {
        strenuityMultiplier = 0;
        //If it is only valid when it can be converted to a number, and if it is between 0 and 1 non-inclusively
        try {
            //Test the number if they specified one
            if(!sStrenuityMultiplier.equals("")) {
                strenuityMultiplier = Float.parseFloat(sStrenuityMultiplier);

                //The number can only be between 0 and 1 not inclusively
                if(!(strenuityMultiplier > 0 && strenuityMultiplier <= 1)) {
                    Log.e("test", "INVALID: the strenuityMultiplier of " + strenuityMultiplier + " is not within a good range.");
                    return false;
                }
            }
        }
        catch (Exception e) {
            Log.e("test", "INVALID: the strenuityMultiplier of " + strenuityMultiplier + " is not a number.");
            return false;
        }
        return true;
    }

    //Performs some checks to confirm the legitimacy of the mental strenuity category
    private boolean IsStrenuityCategoryValid(String strenuityCategory) {
        //Length can only be between 1 and 2.
        if(strenuityCategory.length() > 2 || strenuityCategory.length() <= 0) {
            Log.e("test", "INVALID: the strenuityCategory of " + strenuityCategory + " is longer than 2 chars or doesn't exist.");
            return false;
        }

        //All the letters in the string must be either l,m, or h.
        if(strenuityCategory.length() == 1 && !strenuityCategory.matches("[lmh]+")) {
            Log.e("test", "INVALID: the strenuityCategory of '" + strenuityCategory + "' is not wholly made up of l,m, or h");
            return false;
        }
        else if (strenuityCategory.length() == 2 && !strenuityCategory.matches("[lmh]{2}")) {
            Log.e("test", "INVALID: the strenuityCategory of '" + strenuityCategory + "' is not wholly made up of l,m, or h for 2 letters!");
            return false;
        }

        //Must not be duplicate letters in the case of 2 letters
        if(strenuityCategory.length() == 2 && strenuityCategory.charAt(0) == strenuityCategory.charAt(1)) {
            Log.e("test", "INVALID: the strenuityCategory of '" + strenuityCategory + "' has a duplicate letter");
            return false;
        }

        return true;
    }

    //Extracts the work time and productive time values
    private void GetWorkAndProductiveTimes(String[] scheduledItemWords) {
        invalidTimesFormat = true;
        workTimeInMinutes = 0;
        productiveTimeInMinutes = 0;

        //Analyse each word
        for(int wordIndex = scheduledItemWords.length - 1; wordIndex >= 0; wordIndex--) {
            String word = scheduledItemWords[wordIndex];

            //If there is a word without any parentheses but we already have a valid productiveTimeInMinutes value, then it is still legit. Otherwise it's an error
            if(!(word.contains("(") || word.contains(")"))) {
                if(productiveTimeInMinutes > 0) {
                    invalidTimesFormat = false;
                    workTimeInMinutes = productiveTimeInMinutes;
                    parentheseWordIndex = wordIndex + 1; //because this word is invalid
                }
                return;
            }

            //If the text inside is not a positive number, it is invalid.
            String textInsideParentheses = word.substring(1,word.length() -1);
            try {
                int numberInsideParentheses = Integer.parseInt(textInsideParentheses);
                if(numberInsideParentheses < 0) {
                    Log.e("test", "INVALID: over 100% efficiency detected!.");
                    return;
                }

                //if this number was at the very end, this is the number of productive minutes
                if(wordIndex == scheduledItemWords.length -1) productiveTimeInMinutes = numberInsideParentheses;

                    //Otherwise it was the work time in minutes
                else {
                    workTimeInMinutes = numberInsideParentheses;

                    //it is impossible to have over 100% efficiency
                    if(productiveTimeInMinutes > workTimeInMinutes) {
                        Log.e("test", "INVALID: The productive time is greater than the work time.");
                        return;
                    }

                    //When we have successfully retrieved both values, and they are legit
                    else {
                        invalidTimesFormat = false; //the format has been good so far
                        parentheseWordIndex = wordIndex;
                        return;
                    }
                }
            }
            catch (Exception e) { //When there was no number within the parentheses, it's invalid.
                Log.e("test", "INVALID: the value " + textInsideParentheses + " is not an integer number.");
                return;
            }
        }
    }
}