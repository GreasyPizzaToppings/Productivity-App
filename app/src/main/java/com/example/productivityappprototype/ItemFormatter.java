package com.example.productivityappprototype;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.util.DisplayMetrics;

//This method contains the functionality required for the correct formatting of scheduled items for aesthetic and functional purposes.
public class ItemFormatter {
    private Paint scheduledItemPaint, timeHeaderPaint;
    private float recyclerViewWidth;
    private Context context;

    public ItemFormatter(Context context, Paint scheduledItemPaint, Paint timeHeaderPaint, float recyclerViewWidth) {
        this.scheduledItemPaint = scheduledItemPaint;
        this.timeHeaderPaint = timeHeaderPaint;
        this.recyclerViewWidth = recyclerViewWidth;
        this.context = context;
    }

    //This method takes in an item to format to allow for a visual distinction between the item name and the entered times of the item
    //It will pad short items accordingly to make the item times line up in the scheduled time tab.
    //It insert newline characters to put longer items on multiple lines to allow the times to be aligned correctly.
    public String FormatSchedulingItem(String itemToSchedule, String startTime, String endTime) {
        //--Declare and initialise DisplayMetrics object used to find the width of the display--
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(dm); //Obtain the metrics of the display

        recyclerViewWidth = dm.widthPixels - 8 - 8; //Get and log the width of the screen
        String fullTimeText = startTime + " - " + endTime; //Allow accurate measuring of the time

        //Calculate the amount of padding required to appropriately pad the time to the end of the recyclerview
        float itemBoundaryDifference = recyclerViewWidth - scheduledItemPaint.measureText(itemToSchedule) - timeHeaderPaint.measureText(context.getString(R.string.text_time)) - 12 - scheduledItemPaint.measureText("aa");

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
            if(!excessItem.equals("")) workingItem = excessItem; //After a line has been split and still has text, update the working item

            float workingItemBoundaryDifference = recyclerViewWidth - scheduledItemPaint.measureText(workingItem) - timeHeaderPaint.measureText(context.getString(R.string.text_time)) - 12 - scheduledItemPaint.measureText("aa");
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
                if(recyclerViewWidth - scheduledItemPaint.measureText(workingItemWords[word]) - timeHeaderPaint.measureText(context.getString(R.string.text_time)) - 12 - scheduledItemPaint.measureText("aa") < 0) {
                    itemWordTooLong = true;

                    String splitWords = SplitLongItemWord(workingItemWords[word]); //Get the acceptable individual words from the big word
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
                workingItemBoundaryDifference = recyclerViewWidth - scheduledItemPaint.measureText(workingItem) - timeHeaderPaint.measureText(context.getString(R.string.text_time)) - 12 - scheduledItemPaint.measureText("aa");

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
            if(!excessWord.equals("")) workingWord = excessWord; //After a line has been split and still has text, update the working item

            float wordBoundaryDifference = recyclerViewWidth - scheduledItemPaint.measureText(workingWord) - timeHeaderPaint.measureText(context.getString(R.string.text_time)) - 12 - scheduledItemPaint.measureText("aa");

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
                wordBoundaryDifference = recyclerViewWidth - scheduledItemPaint.measureText(workingWord) - timeHeaderPaint.measureText(context.getString(R.string.text_time)) - 12 - scheduledItemPaint.measureText("aa");

                //When the word is now an acceptable length
                if(wordBoundaryDifference > 0) {
                    splitWords += workingWord + " "; //Add the acceptable word to the full word

                    //Extract the excess item
                    if(excessWord.equals("")) excessWord = longItemWord.substring(workingWord.length());
                    else excessWord = excessWord.substring(workingWord.length()); //Obtain the part of the item that was separated, from the old excess item
                    break;
                }
            }
        }
    }

    //Given a pre-calculated width that needs to be padded, this method will return a suitable number of spaces.
    public String GetPadding(float widthToPad) {
        //Measure the width of one pad unit in pixels
        String padUnit = " "; //One unit of padding is one space
        float padUnitWidth = scheduledItemPaint.measureText(padUnit);

        double numberOfPadsRequired = Math.floor(widthToPad / padUnitWidth); //Round down the number of pads required. Padding less is better than padding too much
        String pad = "";

        //Add the correct number of pads to the item
        for(int pads = 0; pads < numberOfPadsRequired; pads++) pad += padUnit;
        return pad;
    }
}