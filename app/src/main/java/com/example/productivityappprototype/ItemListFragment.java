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

public class ItemListFragment extends Fragment implements View.OnClickListener {
    public LinkedList<String> itemList = new LinkedList<>();
    private RecyclerView itemListRecyclerView;
    private TextView tutorialMessage = null;
    private com.example.productivityappprototype.ItemListAdapter adapter;
    private final int MAX_ITEM_LENGTH = 100;
    private final int MIN_ITEM_LENGTH = 1;
    private final String baseItemKey = "item:"; //The base key used to store the items in the bundle
    private SharedPreferences itemListSharedPrefs;
    private String itemListSharedPrefsFile = "com.example.productivityappprototype";
    private SharedPreferences.Editor itemListSharedPrefsEditor;
    private final String ITEM_NOT_FOUND = "";

    /*The maximum number of items that can be contained within the item list recycler view. For potential performance reasons, this is limited.
    Also, if the user has more than 100 things to do, they SHOULD remove some items and prioritise.
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
        TextView itemsHeaderTextView = v.findViewById(R.id.text_items_header);
        itemsHeaderTextView.setText(itemHeaderSpannable);

        //Initialise the SharedPreferences object
        itemListSharedPrefs = getActivity().getSharedPreferences(itemListSharedPrefsFile, MODE_PRIVATE);

        //Restore the data from the SharedPreferences file
        if(!itemListSharedPrefsFile.isEmpty()) {
            //Use the bundle size as the linked list gets reinitialised to size 0 after every config change. -1 because there is always a bool variable stored as well
            for(int item = 0; item < itemListSharedPrefsFile.length(); item++) {
                String fullItemKey = baseItemKey + item; //Build the key used to predictably store the items in the file
                String restoredItem = itemListSharedPrefs.getString(fullItemKey, ITEM_NOT_FOUND); //Blank default values are the error case as you cannot have an item with no length

                //Restore items that were found in the restored shared preference
                if (restoredItem != ITEM_NOT_FOUND) itemList.addLast(restoredItem);
            }

            if (adapter != null) adapter.notifyDataSetChanged(); //Refresh if the adapter exists
        }

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //Get a handle to the recycler view
        itemListRecyclerView = getView().findViewById(R.id.recyclerview_item_list);

        //Create the adapter and supply the data to be displayed
        adapter = new ItemListAdapter(this, itemList);
        itemListRecyclerView.setAdapter(adapter); //Attach the adapter to the recycler view

        //Assign the recycler view a default layout manager
        itemListRecyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));

        tutorialMessage = getView().findViewById(R.id.text_no_items);
        ToggleTutorialMessage();
    }

    //This method displays the textview telling the user how to add items if there are no items in the item list
    public void ToggleTutorialMessage() {
        if (itemList.size() >= 1) {
            tutorialMessage.setVisibility(View.INVISIBLE);
        }
        else {
            tutorialMessage.setVisibility(View.VISIBLE);
        }
    }

    //The click event for the floating action button, which displays a dialog to allow the user to add an item to the item list
    @Override
    public void onClick(View v) {
        //---Create and show a dialog to the user to allow them to select the name of the item---
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        //Set the properties of the dialog
        builder.setTitle("New Item");
        builder.setMessage("Set the name of your new item.");

        //Get the layout inflater for the custom layout and inflate the layout for the dialog
        LayoutInflater inflater = LayoutInflater.from(v.getContext());
        builder.setView(inflater.inflate(R.layout.item_dialog, null));

        //Give the dialog a positive button
        builder.setPositiveButton("Add Item", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Extract the string and add it to the recyclerview
                EditText editItemName = ((AlertDialog) dialog).findViewById(R.id.edit_one_time_item);
                String userItemName = editItemName.getText().toString();

                //If the user entered a valid name for the new item
                if (userItemName.length() >= MIN_ITEM_LENGTH && userItemName.length() <= MAX_ITEM_LENGTH) {

                    //When the user hasn't made too many items
                    if(itemList.size() < MAX_NO_ITEMS) {
                        itemList.addLast(userItemName); //add the new item to the recycler view
                        ToggleTutorialMessage(); //Hide the no items message after updating the list

                        adapter.notifyDataSetChanged();
                        itemListRecyclerView.smoothScrollToPosition(itemList.size() - 1);

                        //Update the value in the shared preferences file
                        String fullItemKey = baseItemKey + (itemList.size() - 1); //get the key of the changed item
                        itemListSharedPrefsEditor = itemListSharedPrefs.edit(); //Initialise the editor
                        itemListSharedPrefsEditor.putString(fullItemKey, userItemName);
                        itemListSharedPrefsEditor.apply();
                    }

                    else Toast.makeText(getContext(), "Error. You cannot have more than " + MAX_NO_ITEMS + " items. Delete some before adding more." , Toast.LENGTH_LONG).show();

                    return; //No need to check for invalid cases
                }

                //Inform user about their rejected input for the invalid cases
                if (userItemName.length() < MIN_ITEM_LENGTH) Toast.makeText(getContext(), "Your item name needs to be at least one character!", Toast.LENGTH_LONG).show();
                if (userItemName.length() > MAX_ITEM_LENGTH) Toast.makeText(getContext(), "Your item name needs to be under 100 characters long!", Toast.LENGTH_LONG).show();
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
}