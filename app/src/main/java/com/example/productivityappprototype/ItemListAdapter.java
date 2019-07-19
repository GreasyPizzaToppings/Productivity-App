package com.example.productivityappprototype;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.LinkedList;

public class ItemListAdapter extends RecyclerView.Adapter<ItemListAdapter.ItemViewHolder>{
    private final LinkedList<String> mItemList;
    private LayoutInflater mInflater;

    public ItemListAdapter(ItemListFragment context, LinkedList<String> itemList) {
        mInflater = LayoutInflater.from(context.getActivity());
        this.mItemList = itemList;
    }

    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public final TextView item; //The viewholder that holds a productivity item
        final ItemListAdapter mAdapter;

        //Initialises the viewholder textview from the word xml resource and sets its adapter
        public ItemViewHolder(View itemView, ItemListAdapter adapter) {
            super(itemView);
            item = itemView.findViewById(R.id.item);
            this.mAdapter = adapter;
            itemView.setOnClickListener(this); //Set the onClick listener to detect clicks
        }

        @Override
        public void onClick(View v) {
            int mPosition = getLayoutPosition(); //Get the position of the clicked item to know what word holder was clicked
            String element = mItemList.get(mPosition); //Get the word at the selected position in the word list
            mItemList.set(mPosition, "Clicked! " + element); //Change the text to show the user it has been clicked
            mAdapter.notifyDataSetChanged(); //Get the adapter to refresh and update the recycler view to the changed data
        }
    }

    @NonNull
    @Override
    public ItemListAdapter.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View mItemView = mInflater.inflate(R.layout.itemlist_item, viewGroup, false);
        return new ItemViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemListAdapter.ItemViewHolder itemViewHolder, int position) {
        String mCurrent = mItemList.get(position);
        itemViewHolder.item.setText(mCurrent); //Display the word from the word list to the word view inside the holder of the word view
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }
}
