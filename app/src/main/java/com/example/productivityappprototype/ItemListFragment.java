package com.example.productivityappprototype;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.LinkedList;
import static android.content.Context.MODE_PRIVATE;

public class ItemListFragment extends Fragment implements View.OnClickListener, ItemListAdapter.UpdateItemsInterface {
    public LinkedList<String> itemList = new LinkedList<>();
    private RecyclerView itemListRecyclerView;
    private TextView tutorialMessage = null;
    private com.example.productivityappprototype.ItemListAdapter adapter;
    private final int MAX_ITEM_LENGTH = 100;
    private final int MIN_ITEM_LENGTH = 1;
    private SharedPreferences itemListSharedPrefs;

    private SharedPreferencesFileManager sharedPrefsManager;

    /*The maximum number of items that can be contained within the item list recycler view. For potential performance reasons, this is limited.
    Also, if the user has more than 100 things to do, they should probably remove some items and prioritise.
    */
    private final int MAX_NO_ITEMS = 100;

    public ItemListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.item_list_fragment, container, false);

        //Get a handle to the floating action button and assign a click listener which adds an item to the list
        FloatingActionButton addItemButton = v.findViewById(R.id.fb_add_item);
        addItemButton.setOnClickListener(this);

        //Underline the header text "Items"
        String itemsHeader = "Items";
        SpannableString itemHeaderSpannable = new SpannableString(itemsHeader);
        itemHeaderSpannable.setSpan(new UnderlineSpan(), 0, itemsHeader.length(), 0); //Make the text underlined

        //Display the underlined header
        TextView itemsHeaderTextView = v.findViewById(R.id.text_items_heading);
        itemsHeaderTextView.setText(itemHeaderSpannable);

        //Initialise the SharedPreferences object
        String itemListSharedPrefsFile = "com.example.productivityappprototype";
        itemListSharedPrefs = getActivity().getSharedPreferences(itemListSharedPrefsFile, MODE_PRIVATE);

        //Initialise the shared prefs file manager
        sharedPrefsManager = new SharedPreferencesFileManager(this.getContext());

        //Restore the data from the shared prefs file
        sharedPrefsManager.restoreStringsFromFile(itemListSharedPrefs, itemList);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //Get a handle to the recycler view
        itemListRecyclerView = getView().findViewById(R.id.recyclerview_item_list);

        //Create the adapter and supply the data to be displayed
        adapter = new ItemListAdapter(this, itemList, this);
        itemListRecyclerView.setAdapter(adapter); //Attach the adapter to the recycler view

        //Assign the recycler view a default layout manager
        itemListRecyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));

        tutorialMessage = getView().findViewById(R.id.text_no_items);
        ToggleTutorialMessage();
    }

    //This method displays the textview telling the user how to add items if there are no items in the item list
    public void ToggleTutorialMessage() {
        TextView textViewItemsHeading = getView().findViewById(R.id.text_items_heading);

        //Hide the tutorial message
        if (itemList.size() >= 1) {
            textViewItemsHeading.setVisibility(View.VISIBLE);
            tutorialMessage.setVisibility(View.INVISIBLE);
        }

        //Show the tutorial message
        else {
            textViewItemsHeading.setVisibility(View.INVISIBLE);
            tutorialMessage.setVisibility(View.VISIBLE);
        }
    }

    //The click event for the floating action button, which displays a dialog to allow the user to add an item to the item list
    @Override
    public void onClick(View v) {
        //---Create and show a dialog to the user to allow them to select the name of the item---
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        //Set the properties of the dialog
        builder.setTitle("Add New Item");
        builder.setMessage("Set the name of your new item.");

        //Get the layout inflater for the custom layout and inflate the layout for the dialog
        LayoutInflater inflater = LayoutInflater.from(v.getContext());
        builder.setView(inflater.inflate(R.layout.item_dialog, null));

        //Give the dialog a positive button
        builder.setPositiveButton("Add Item", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Extract the string and add it to the recyclerview
                EditText editItemName = ((AlertDialog) dialog).findViewById(R.id.edit_item_name);
                String newItemName = editItemName.getText().toString();

                //If the user entered a valid name for the new item
                if (newItemName.length() >= MIN_ITEM_LENGTH && newItemName.length() <= MAX_ITEM_LENGTH) {

                    //When the user hasn't made too many items
                    if(itemList.size() < MAX_NO_ITEMS) {
                        itemList.addLast(newItemName); //add the new item to the recycler view

                        sharedPrefsManager.updateItemAt(itemList.size() - 1, newItemName, itemListSharedPrefs); //Add the new item to the shared prefs file

                        adapter.notifyDataSetChanged();
                        itemListRecyclerView.smoothScrollToPosition(itemList.size() - 1);

                        ToggleTutorialMessage();
                    }

                    else Toast.makeText(getContext(), "Error. You cannot have more than " + MAX_NO_ITEMS + " items. Delete some before adding more." , Toast.LENGTH_LONG).show();

                    return; //No need to check for invalid cases
                }

                //Inform user about their rejected input for the invalid cases
                if (newItemName.length() < MIN_ITEM_LENGTH) Toast.makeText(getContext(), "Your item name needs to be at least one character!", Toast.LENGTH_LONG).show();
                if (newItemName.length() > MAX_ITEM_LENGTH) Toast.makeText(getContext(), "Your item name needs to be under 100 characters long!", Toast.LENGTH_LONG).show();
            }
        });

        //Give the dialog a negative button
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.create().show(); //Build and show the dialog
    }

    @Override
    public void onDeleteItem(int itemIndex) {
        itemList.remove(itemIndex); //Delete the selected word from the local linked list
        sharedPrefsManager.removeItemAt(itemIndex, itemListSharedPrefs, itemList);//Delete the item from the shared preferences file
        adapter.notifyDataSetChanged();
        ToggleTutorialMessage();
    }

    @Override
    public void onUpdateItemName(String newItemName, int itemIndex) {
        itemList.set(itemIndex, newItemName); //Update the local linked list
        sharedPrefsManager.updateItemAt(itemIndex, newItemName, itemListSharedPrefs); //Update the shared prefs file
        adapter.notifyDataSetChanged();
    }
}