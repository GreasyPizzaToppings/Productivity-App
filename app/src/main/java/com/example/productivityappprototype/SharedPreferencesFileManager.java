package com.example.productivityappprototype;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.util.LinkedList;

public class SharedPreferencesFileManager {
    //Shared preferences objects
    private SharedPreferences rawScheduledItemsSharedPrefs;
    private SharedPreferences formattedScheduledItemsSharedPrefs;
    private SharedPreferences startTimesSharedPrefs;
    private SharedPreferences endTimesSharedPrefs;
    private SharedPreferences itemListSharedPrefs;
    private SharedPreferences scheduledItemsCompletionStatusSharedPrefs;

    //Shared preferences files
    private String rawScheduledItemsSharedPrefsFile = "com.example.productivityappprototype.raw";
    private String formattedScheduledItemsSharedPrefsFile = "com.example.productivityappprorotype.formatted";
    private String startTimesSharedPrefsFile = "com.example.productivityappprototype.startTimes";
    private String endTimesSharedPrefsFile = "com.example.productivityappprototype.endTimes";
    private String itemListSharedPrefsFile = "com.example.productivityappprototype";
    private String scheduledItemsCompletionStatusSharedPrefsFile = "com.example.productivityappprototype.completion";

    private final String ITEM_NOT_FOUND = "";
    private final String baseItemKey = "item:"; //The base key used to store the items in the bundle

    public SharedPreferencesFileManager(Context context) {
        //Initialise the shared pref objects
        rawScheduledItemsSharedPrefs = context.getSharedPreferences(rawScheduledItemsSharedPrefsFile, Context.MODE_PRIVATE);
        formattedScheduledItemsSharedPrefs = context.getSharedPreferences(formattedScheduledItemsSharedPrefsFile, Context.MODE_PRIVATE);
        startTimesSharedPrefs = context.getSharedPreferences(startTimesSharedPrefsFile, Context.MODE_PRIVATE);
        endTimesSharedPrefs = context.getSharedPreferences(endTimesSharedPrefsFile, Context.MODE_PRIVATE);
        scheduledItemsCompletionStatusSharedPrefs = context.getSharedPreferences(scheduledItemsCompletionStatusSharedPrefsFile, Context.MODE_PRIVATE);
        itemListSharedPrefs = context.getSharedPreferences(itemListSharedPrefsFile, Context.MODE_PRIVATE); //Initialise the SharedPreferences object and read in the file
    }

    //This method will move the data elements in the shared preferences file provided back one starting at a given point. This is an effective way of deleting an item and removing gaps in the data.
    //Note: This method needs to be called after the item has already been deleted from the associated linkedList datalist.
    public void removeItemAt(int indexToDelete, SharedPreferences sharedPreferences, LinkedList<String> dataList) {
        //Initialise the correct editor to use for deletion, based on the shared prefs object given
        SharedPreferences.Editor editor = sharedPreferences.edit();

        //Move the items after this element back one, overwriting the data which effectively deletes the unwanted item.
        for(int indexItem = indexToDelete; indexItem < dataList.size(); indexItem++) { //Using the rawScheduledItems linked list, as all the lists should have the same length
            //Build the key
            String finalItemKey = baseItemKey + indexItem;
            editor.putString(finalItemKey, dataList.get(indexItem)); //Move the items back one
        }

        //Delete the last item to remove the double-up of the last item
        editor.remove(baseItemKey + dataList.size());
        editor.apply();
    }

    public LinkedList<String> restoreStringsFromFile(SharedPreferences sharedPrefs, LinkedList<String> dataList) {
        for(int item = 0; item < rawScheduledItemsSharedPrefsFile.length(); item++) { //use the raw scheduled items shared preferences file as it will have the same length as all the other files
            String fullItemKey = baseItemKey + item; //Build the key used to predictably store the items in the file
            String restoredValue = sharedPrefs.getString(fullItemKey, ITEM_NOT_FOUND); //Blank default values are the error case as you cannot have an item with no length

            //Restore items that were found in the restored shared preference. Add the items to the specified linked list
            if (!restoredValue.equals(ITEM_NOT_FOUND)) {
                dataList.addLast(restoredValue);
            }
        }
        return dataList;
    }

    public void updateItemAt(int index, String newValue, SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        //Update the value at the given index in the shared prefs file with the new value
        String fullItemKey = baseItemKey + index;
        editor.putString(fullItemKey, newValue);
        editor.apply();
    }

    //This method will fill a string linked list from the data in the shared preferences file for the general item list. It is used to allow the existing items to be displayed in the 'schedule new item' dialog recyclerview.
    public LinkedList<String> getItemList() {
        LinkedList<String> itemList = new LinkedList<>();

        //Read in the data from the file to build the item list
        if (!itemListSharedPrefsFile.isEmpty()) {
            //Use the bundle size as the linked list gets reinitialised to size 0 after every config change. -1 because there is always a bool variable stored as well
            for (int item = 0; item < itemListSharedPrefsFile.length(); item++) {
                String fullItemKey = baseItemKey + item; //Build the key used to predictably store the items in the file
                String restoredItem = itemListSharedPrefs.getString(fullItemKey, ITEM_NOT_FOUND); //Blank default values are the error case as you cannot have an item with no length

                //Restore items that were found in the restored shared preference
                if (!restoredItem.equals(ITEM_NOT_FOUND)) {
                    itemList.add(item, restoredItem);
                }
            }
        }

        return itemList;
    }

    //useful method for debugging
    public void LogAllFileContents() {
        //Log the data in the raw scheduled items shared prefs file, if it is not the default value.
        for(int i = 0; i < rawScheduledItemsSharedPrefsFile.length(); i++) {
            String fullKey = baseItemKey + i;
            if(!rawScheduledItemsSharedPrefs.getString(fullKey, ITEM_NOT_FOUND).equals(ITEM_NOT_FOUND)) Log.e("raw sP file", i + ": '" + rawScheduledItemsSharedPrefs.getString(fullKey, "defaultvalue") + "'"); //Print the value out
        }

        //Log the data in the whole formatted scheduled items shared prefs file
        for(int i = 0; i < formattedScheduledItemsSharedPrefsFile.length(); i++) {
            String fullKey = baseItemKey + i;
            if(!formattedScheduledItemsSharedPrefs.getString(fullKey, ITEM_NOT_FOUND).equals(ITEM_NOT_FOUND)) Log.e("formatted sP file", i + ": '" + formattedScheduledItemsSharedPrefs.getString(fullKey, "defaultvalue") + "'"); //Print the value out
        }

        //Log the data in the start times shared prefs file
        for(int i = 0; i < startTimesSharedPrefsFile.length(); i++) {
            String fullKey = baseItemKey + i;
            if(!startTimesSharedPrefs.getString(fullKey, ITEM_NOT_FOUND).equals(ITEM_NOT_FOUND)) Log.e("start times sP file", i + ": '" + startTimesSharedPrefs.getString(fullKey, "defaultvalue") + "'"); //Print the value out
        }

        //Log the data in the end items shared prefs file
        for(int i = 0; i < endTimesSharedPrefsFile.length(); i++) {
            String fullKey = baseItemKey + i;
            if(!endTimesSharedPrefs.getString(fullKey, ITEM_NOT_FOUND).equals(ITEM_NOT_FOUND)) Log.e("end times sP file", i + ": '" + endTimesSharedPrefs.getString(fullKey, "defaultvalue") + "'"); //Print the value out
        }

        //Log the data in the item completion status shared prefs file
        for(int i = 0; i < scheduledItemsCompletionStatusSharedPrefsFile.length(); i++) {
            String fullKey = baseItemKey + i;
            if(!scheduledItemsCompletionStatusSharedPrefs.getString(fullKey, ITEM_NOT_FOUND).equals(ITEM_NOT_FOUND)) Log.e("status sP file", i + ": '" + scheduledItemsCompletionStatusSharedPrefs.getString(fullKey, "defaultvalue") + "'"); //Print the value out
        }
    }

    public static void clearSharedPreferences(Context ctx){
        File dir = new File(ctx.getFilesDir().getParent() + "/shared_prefs/");
        String[] children = dir.list();
        for (int i = 0; i < children.length; i++) {
            // clear each preference file
            ctx.getSharedPreferences(children[i].replace(".xml", ""), Context.MODE_PRIVATE).edit().clear().commit();
            //delete the file
            new File(dir, children[i]).delete();
        }
    }
}