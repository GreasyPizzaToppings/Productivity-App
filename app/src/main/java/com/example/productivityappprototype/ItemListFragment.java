package com.example.productivityappprototype;


import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Debug;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.productivityappprototype.ItemListAdapter;

import java.util.LinkedList;

public class ItemListFragment extends Fragment implements View.OnClickListener {
    private LinkedList<String> itemList = new LinkedList<>();
    private RecyclerView recyclerView;
    private com.example.productivityappprototype.ItemListAdapter adapter;


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

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //Create some sample data
        for(int item = 0; item < 25; item++) {
            itemList.addLast("Item " + item);

            Log.e("Added Item", "An item was added to a linked list. The length is now " + itemList.size());
        }

        recyclerView = getView().findViewById(R.id.recyclerview); //Get a handle to the recycler view

        //Create the adapter and supply the data to be displayed
        adapter = new ItemListAdapter(this, itemList);
        recyclerView.setAdapter(adapter); //Attach the adapter to the recycler view

        //Assign the recycler view a default layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));
    }

    @Override
    public void onClick(View v) {
        itemList.addLast("New Item");
        adapter.notifyDataSetChanged();
        recyclerView.smoothScrollToPosition(itemList.size() -1);
    }
}