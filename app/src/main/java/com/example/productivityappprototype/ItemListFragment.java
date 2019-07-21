//TODO
//Save the items that the user manually put into the item list when a device configuration change occurs
//Have one-time creation of sample data so that when you rotate the device, the sample data will get deleted, to make things easier to work with? Maybe some other solution? I don't understand.

package com.example.productivityappprototype;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.LinkedList;

public class ItemListFragment extends Fragment implements View.OnClickListener {
    public LinkedList<String> itemList = new LinkedList<>();
    private RecyclerView recyclerView;
    private TextView tutorialMessage = null;
    private com.example.productivityappprototype.ItemListAdapter adapter;
    private final int MAX_ITEM_LENGTH = 100;
    private final int MIN_ITEM_LENGTH = 1;

    public ItemListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.item_list_fragment, container, false);

        //Get a handle to the floating action button and assign a click listener which adds an item to the list
        FloatingActionButton addItemButton = (FloatingActionButton) v.findViewById(R.id.fb_add_item);
        addItemButton.setOnClickListener(this);


        //---Restore the data from the bundle----
        if(itemList != null && savedInstanceState != null) {
            String baseItemKey = "item:";
            //Use the bundle size as the linked list gets reinitialised to size 0 after every config change. -1 because there is always a bool variable stored as well
            for(int item = 0; item < savedInstanceState.size() - 1; item++) {
                String fullItemKey = baseItemKey + item; //Will build keys like: item:0, item:1 ... used to store and access each item in the bundle
                String restoredItem = savedInstanceState.getString(fullItemKey);
                if (restoredItem != null && restoredItem.length() != 0) {
                    itemList.addLast(restoredItem); //Add the non-null item back
                }
            }

            if (adapter != null) {
                adapter.notifyDataSetChanged(); //Refresh if the adapter exists
            }
        }
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //Get a handle to the recycler view
        recyclerView = getView().findViewById(R.id.recyclerview);

        //Create the adapter and supply the data to be displayed
        adapter = new ItemListAdapter(this, itemList);
        recyclerView.setAdapter(adapter); //Attach the adapter to the recycler view

        //Assign the recycler view a default layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));

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
                EditText editItemName = ((AlertDialog) dialog).findViewById(R.id.edit_item_name);
                String userItemName = editItemName.getText().toString();

                //If the user entered a valid name for the new item
                if (userItemName.length() > 0 && userItemName.length() <= 100) {
                    //add the new item to the recycler view
                    itemList.addLast(userItemName);
                    ToggleTutorialMessage(); //Hide the no items message
                    adapter.notifyDataSetChanged();
                    recyclerView.smoothScrollToPosition(itemList.size() - 1);
                    return; //No need to check for invalid cases
                }

                //Inform user about their rejected input for the invalid cases
                if (userItemName.length() < MIN_ITEM_LENGTH) {
                    Toast.makeText(getContext(), "Your item name needs to be at least one character!", Toast.LENGTH_LONG).show();
                }

                if (userItemName.length() > MAX_ITEM_LENGTH) {
                    Toast.makeText(getContext(), "Your item name needs to be under 100 characters long!", Toast.LENGTH_LONG).show();
                }
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
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(itemList == null) {
            return;
        }

        //Store each of the users's items with a predictable key
        String baseItemKey = "item:";
        for(int item = 0; item < itemList.size(); item++) {
            String fullItemKey = baseItemKey + item; //Will build keys like: item:0, item:1 ... used to store and access each item in the bundle
            savedInstanceState.putString(fullItemKey, itemList.get(item));
        }
    }
}