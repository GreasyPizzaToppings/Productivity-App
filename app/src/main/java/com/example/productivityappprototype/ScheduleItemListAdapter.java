package com.example.productivityappprototype;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.LinkedList;

public class ScheduleItemListAdapter extends RecyclerView.Adapter<ScheduleItemListAdapter.ItemViewHolder> {
    private LayoutInflater mInflater;
    private final LinkedList<String> itemList; //The list of the physical data, such as item 1, item 2, etc.

    //Constructor for the class, for the context of the schedule fragment.
    public ScheduleItemListAdapter(ScheduleFragment context, LinkedList<String> itemList) {
        mInflater = LayoutInflater.from(context.getActivity()); //Initialise the inflater used to inflate the layout the view holder for each item
        this.itemList = itemList; //Establish a connection between the list from the context of the ScheduleFragment, and this context in the adapter
    }

    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView item; //The view holder that holds a productivity item
        final ScheduleItemListAdapter mAdapter;

        //Initialises the view holder text view from the word xml resource and sets its adapter
        public ItemViewHolder(View itemView, ScheduleItemListAdapter adapter) {
            super(itemView);
            item = itemView.findViewById(R.id.item); //The text view which holds an item in the item list
            this.mAdapter = adapter;
            itemView.setOnClickListener(this); //Set the onClick listener to detect clicks
        }

        //The onclick event for each scheduled item
        @Override
        public void onClick(View v) {
            //Give user ability to change the items and such
        }
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View mItemView = mInflater.inflate(R.layout.itemlist_item, viewGroup, false); //Inflate the layout for the item world holder which holds each item list item
        return new ItemViewHolder(mItemView, this); //Create the item view holder and return
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleItemListAdapter.ItemViewHolder itemViewHolder, int position) {
        String currentItem = itemList.get(position);
        itemViewHolder.item.setText(currentItem);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}

