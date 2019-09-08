package com.example.productivityappprototype;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.LinkedList;

//This adapter is much like the ItemListAdapter, except it is for use inside an AlertDialog in the ScheduleFragment only. It has different onClick functionality that is distinctly different to the ItemListAdapter.
public class ScheduleDialogItemListAdapter extends RecyclerView.Adapter<ScheduleDialogItemListAdapter.ItemViewHolder> {
    private LayoutInflater mInflater;
    private final LinkedList<String> itemList; //The list of the physical data, such as item 1, item 2, etc.
    private LinkedList<TextView> itemViewHolderList; //The list of the text view elements which are the item view holder
    private AddScheduledItemInterface adapterInterface;
    private TextView previouslySelectedItem;
    private final String lightGrayBackgroundHex = "#d9d9d9"; //85% white according to w3 schools
    private final String darkGrayBackgroundHex = "#bfbfbf"; //75% white according to w3 schools

    //This interface tells the ScheduleFragment what item was selected, to enable the functionality of the schedule new item dialog.
    public interface AddScheduledItemInterface {
        void OnClickDialogItem(String itemName, boolean selected);
    }

    //Constructor for the class, for the context of the schedule fragment.
    public ScheduleDialogItemListAdapter(ScheduleFragment context, LinkedList<String> itemList, AddScheduledItemInterface scheduleAdapterInterface) {
        mInflater = LayoutInflater.from(context.getActivity()); //Initialise the inflater used to inflate the layout the view holder for each item
        this.itemList = itemList; //Establish a connection between the list from the context of the ScheduleFragment, and this context in the adapter
        this.adapterInterface = scheduleAdapterInterface; //Establish the connection between the interface between the fragment and the adapter
        itemViewHolderList = new LinkedList<>(); //Initialise the linked list for the itemViewHolders
    }

    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView item; //The view holder that holds a productivity item
        final ScheduleDialogItemListAdapter mAdapter;

        //Initialises the view holder text view from the word xml resource and sets its adapter
        public ItemViewHolder(View itemView, ScheduleDialogItemListAdapter adapter) {
            super(itemView);
            item = itemView.findViewById(R.id.item); //The text view which holds an item in the item list
            this.mAdapter = adapter;
            itemView.setOnClickListener(this); //Set the onClick listener to detect clicks
            itemViewHolderList.addLast(item); //Build a collection of the item views in the recycler view
        }

        //The onclick event for each item view holder in the recycler view
        @Override
        public void onClick(View v) {
            //Reset each of the items to their default colour
            for(int itemIndex = 0; itemIndex < itemViewHolderList.size(); itemIndex++) { //for(TextView itemHolder : itemViewHolderList) {
                if((itemIndex % 2) == 1) itemViewHolderList.get(itemIndex).setBackgroundColor(Color.parseColor(darkGrayBackgroundHex));
                else itemViewHolderList.get(itemIndex).setBackgroundColor(Color.parseColor(lightGrayBackgroundHex));
            }

            //Only show that an item is selected if it was different to the item that was selected before
            if(item != previouslySelectedItem) {
                //Change the background colour of the item in the recycler view, to give visual feedback to the user
                item.setBackgroundColor(Color.rgb(132, 195, 237));
                adapterInterface.OnClickDialogItem(item.getText().toString(), true); //Make the method call from the interface, to be received by the fragment
                previouslySelectedItem = item; //Store the value of the item that was selected, to allow selected items to be unselected
            }

            //When the user deselects a previously selected item
            else {
                adapterInterface.OnClickDialogItem(item.getText().toString(), false);
                //Reset the previouslySelected item so that the user can reselect a deselected item
                previouslySelectedItem = null;
            }
        }
    }

    @NonNull
    @Override
    public ScheduleDialogItemListAdapter.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View mItemView = mInflater.inflate(R.layout.item_list_item, viewGroup, false);
        return new ItemViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleDialogItemListAdapter.ItemViewHolder itemViewHolder, int position) {
        String currentItem = itemList.get(position);
        itemViewHolder.item.setText(currentItem);

        //---Give the items an alternating coloured background---
        if((position % 2) == 1)itemViewHolder.item.setBackgroundColor(Color.parseColor(darkGrayBackgroundHex));
        else itemViewHolder.item.setBackgroundColor(Color.parseColor(lightGrayBackgroundHex));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}