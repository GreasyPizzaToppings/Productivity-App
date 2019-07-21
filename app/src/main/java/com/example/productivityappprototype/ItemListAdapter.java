package com.example.productivityappprototype;

import android.content.DialogInterface;
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

    public ItemListAdapter(ItemListFragment context, LinkedList<String> itemList) {
        mInflater = LayoutInflater.from(context.getActivity());
        this.itemList = itemList;
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
            builder.setView(inflater.inflate(R.layout.item_dialog, null));

            //Set the other button to delete the item from the list
            builder.setNeutralButton("Delete item", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int mPosition = getLayoutPosition(); //Get the position of the clicked item to know what word holder was clicked
                    itemList.remove(mPosition); //Delete the selected word

                    //Show the tutorial message if there are no items
                    if(itemList.size() == 0) {
                        tutorialMessage = v.getRootView().findViewById(R.id.text_no_items);
                        tutorialMessage.setVisibility(View.VISIBLE);
                    }

                    mAdapter.notifyDataSetChanged(); //Get the adapter to refresh and update the recycler view to the changed data
                }
            });

            //Set the negative button for the dialog
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            //Set the positive button for the dialog to accept the new name
            builder.setPositiveButton("Edit Item", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Get a handle on the edittext in the custom layout
                    EditText editItemName = ((AlertDialog) dialog).findViewById(R.id.edit_item_name);
                    String newItemName = editItemName.getText().toString();

                    //Update the name of the view in the recyclerview
                    if (newItemName.length() > 0 && newItemName.length() <= 100) {
                        int mPosition = getLayoutPosition(); //Get the position of the clicked item to know what word holder was clicked
                        itemList.set(mPosition, newItemName); //Update the word in the wordlist with the new word
                        notifyDataSetChanged();
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

            builder.create().show(); //Build and create the dialog
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
        String mCurrent = itemList.get(position);
        itemViewHolder.item.setText(mCurrent); //Display the word from the word list to the word view inside the holder of the word view
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

}
