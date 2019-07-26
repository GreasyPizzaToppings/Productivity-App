package com.example.productivityappprototype;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ScheduleFragment extends Fragment implements View.OnClickListener {

    public ScheduleFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.schedule_fragment, container, false);

        //---Underline the two header text views---
        //Get a handle on the "item" header text view for the schedule tab
        TextView itemHeaderTextView = v.findViewById(R.id.text_item);

        //Set a spanning string item for the "item" header to make it underlined
        String itemHeader = getContext().getString(R.string.text_item);
        SpannableString itemHeaderSpannable = new SpannableString(itemHeader);
        itemHeaderSpannable.setSpan(new UnderlineSpan(), 0, itemHeader.length(), 0); //Make the text underlined

        itemHeaderTextView.setText(itemHeaderSpannable); //Update the text and make it underlined

        //Repeat the process for the "scheduled time" header text view
        TextView timeHeaderTextView = v.findViewById(R.id.text_time);

        String timeHeader = getContext().getString(R.string.text_time);
        Spannable timeHeaderSpannable = new SpannableString(timeHeader);
        timeHeaderSpannable.setSpan(new UnderlineSpan(), 0, timeHeader.length(), 0); //Make the text underlined

        timeHeaderTextView.setText(timeHeaderSpannable); //Update the text and make it underlined

        //Get a handle to the floating action button and set its on click listener
        FloatingActionButton scheduleTaskButton = v.findViewById(R.id.fb_schedule_item);
        scheduleTaskButton.setOnClickListener(this);

        return v;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    //The on click listener for the floating action button which displays a dialog and adds the task to the schedule
    @Override
    public void onClick(View v) {

    }
}
