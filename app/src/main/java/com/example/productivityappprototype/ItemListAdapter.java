package com.example.productivityappprototype;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.LinkedList;

public class ItemListAdapter extends RecyclerView.Adapter<ItemListAdapter.ItemViewHolder> {
    private final LinkedList<String> itemList;
    private LayoutInflater mInflater;
    private TextView tutorialMessage = null;
    private final int MAX_ITEM_LENGTH = 100;
    private final int MIN_ITEM_LENGTH = 1;
    private final String baseItemKey = "item:"; //The base key used to store the items in the bundle
    private SharedPreferences itemListSharedPrefs;
    private String itemListSharedPrefsFile = "com.example.productivityappprototype";
    private SharedPreferences.Editor itemListSharedPrefsEditor;
    private final String lightGrayBackgroundHex = "#d9d9d9"; //85% white according to w3 schools
    private final String darkGrayBackgroundHex = "#bfbfbf"; //75% white according to w3 schools

    public ItemListAdapter(ItemListFragment context, LinkedList<String> itemList) {
        mInflater = LayoutInflater.from(context.getActivity());
        this.itemList = itemList;

        //Initialise the SharedPreferences object
        itemListSharedPrefs = context.getActivity().getSharedPreferences(itemListSharedPrefsFile, Context.MODE_PRIVATE);
        itemListSharedPrefsEditor = itemListSharedPrefs.edit(); //Initialise the editor
    }

    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView item; //The viewholder that holds a productivity item
        final ItemListAdapter mAdapter;

        //Initialises the viewholder textview from the word xml resource and sets its adapter
        public ItemViewHolder(View itemView, ItemListAdapter adapter) {
            super(itemView);
            item = itemView.findViewById(R.id.item);
            this.mAdapter = adapter;
            itemView.setOnClickListener(this); //Set the onClick listener to detect clicks
        }


        //On the click of the item in the recyclerview, display an alert dialog to allow the user to change the name of the item, or to delete it
        @Override
        public void onClick(final View v) {
            //Create an alertdialog builder object to build the dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getRootView().getContext());

            //Set the properties of the dialog
            builder.setTitle("Item Properties");
            builder.setMessage("Enter a new name for this item");

            //Get the layout inflater for the custom layout and inflate the layout for the dialog
            LayoutInflater inflater = LayoutInflater.from(v.getContext());

            View dialogView = inflater.inflate(R.layout.item_dialog, null); //Inflate the custom layout into a view

            //Attempt to get a handle on the edit text ui component inside to set the text to the current item so the user knows what item they are editing
            EditText itemEditText = dialogView.findViewById(R.id.edit_one_time_item);

            //Set the edit text to the current name of the item for increased UI ease of access
            int itemIndex = getLayoutPosition();
            itemEditText.setText(itemList.get(itemIndex));

            //Set the other button to delete the item from the list
            builder.setNeutralButton(R.string.dialog_delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int itemIndex = getLayoutPosition(); //Get the position of the clicked item to know what word holder was clicked
                    itemList.remove(itemIndex); //Delete the selected word from the item list

                    //Delete the item from the shared preferences file using custom method to shift the data and avoid gaps
                    RemoveItemAt(itemIndex);

                    //Show the tutorial message if there are no items
                    if(itemList.size() == 0) {
                        tutorialMessage = v.getRootView().findViewById(R.id.text_no_items);
                        tutorialMessage.setVisibility(View.VISIBLE);
                    }

                    mAdapter.notifyDataSetChanged(); //Get the adapter to refresh and update the recycler view to the changed data
                }
            });

            //Set the negative button for the dialog
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            //Set the positive button for the dialog to accept the new name
            builder.setPositiveButton(R.string.dialog_done, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Get a handle on the edittext in the custom layout
                    EditText editItemName = ((AlertDialog) dialog).findViewById(R.id.edit_one_time_item);
                    String newItemName = editItemName.getText().toString();

                    //Update the name of the view in the recyclerview
                    if (newItemName.length() >= MIN_ITEM_LENGTH && newItemName.length() <= MAX_ITEM_LENGTH) {
                        int itemIndex = getLayoutPosition(); //Get the position of the clicked item to know what word holder was clicked
                        itemList.set(itemIndex, newItemName); //Update the word in the wordlist with the new word
                        notifyDataSetChanged();

                        //Update the value in the shared preferences file
                        String fullItemKey = baseItemKey + itemIndex; //get the key of the changed item
                        itemListSharedPrefsEditor.putString(fullItemKey, newItemName);
                        itemListSharedPrefsEditor.apply();

                        return; //No need to check for invalid cases
                    }

                    //Inform user about their rejected input for the invalid cases
                    if (newItemName.length() < MIN_ITEM_LENGTH) {
                        Toast.makeText(v.getRootView().getContext(), "Your item name needs to be at least one character!", Toast.LENGTH_LONG).show();
                    }

                    if (newItemName.length() > MAX_ITEM_LENGTH) {
                        Toast.makeText(v.getRootView().getContext(), "Your item name needs to be under 100 characters long!", Toast.LENGTH_LONG).show();
                    }
                }
            });

            builder.setView(dialogView); //Attach this custom layout to the dialog
            builder.create().show();
        }
    }

    public void RemoveItemAt(int indexDelete) {

        //Move the items after this element back one, overwriting the data which effectively deletes the unwanted item.
        for(int indexItem = indexDelete; indexItem < itemList.size(); indexItem++) {
            //Build the key
            String finalItemKey = baseItemKey + indexItem;

            itemListSharedPrefsEditor.putString(finalItemKey, itemList.get(indexItem));
        }

        //Delete the last item to remove the double-up of the last item
        itemListSharedPrefsEditor.remove((baseItemKey + itemList.size()));
        itemListSharedPrefsEditor.apply();
    }


    @NonNull
    @Override
    public ItemListAdapter.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View mItemView = mInflater.inflate(R.layout.item_list_item, viewGroup, false);
        return new ItemViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemListAdapter.ItemViewHolder itemViewHolder, int position) {
        String mCurrent = itemList.get(position);
        itemViewHolder.item.setText(mCurrent); //Display the word from the word list to the word view inside the holder of the word view

        //---Give the items an alternating coloured background---
        if((position % 2) == 1)itemViewHolder.item.setBackgroundColor(Color.parseColor(darkGrayBackgroundHex));
        else itemViewHolder.item.setBackgroundColor(Color.parseColor(lightGrayBackgroundHex));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}