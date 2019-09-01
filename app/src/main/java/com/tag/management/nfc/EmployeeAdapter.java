/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tag.management.nfc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tag.management.nfc.database.EmployeeEntry;

import java.util.List;

/**
 * This EmployeeAdapter creates and binds ViewHolders, that hold the description and priority of a task,
 * to a RecyclerView to efficiently display data.
 */
public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    // Member variable to handle item clicks
    final private ItemClickListener mItemClickListener;
    // Class variables for the List that holds task data and the Context
    private List<EmployeeEntry> mTaskEntries;
    private Context mContext;

    /**
     * Constructor for the EmployeeAdapter that initializes the Context.
     *
     * @param context  the current Context
     * @param listener the ItemClickListener
     */
    EmployeeAdapter(Context context, ItemClickListener listener) {
        mContext = context;
        mItemClickListener = listener;
    }

    /**
     * Called when ViewHolders are created to fill a RecyclerView.
     *
     * @return A new EmployeeViewHolder that holds the view for each task
     */
    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the task_layout to a view
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.task_layout, parent, false);

        return new EmployeeViewHolder(view);
    }

    /**
     * Called by the RecyclerView to display data at a specified position in the Cursor.
     *
     * @param holder   The ViewHolder to bind Cursor data to
     * @param position The position of the data in the Cursor
     */
    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        // Determine the values of the wanted data
        EmployeeEntry taskEntry = mTaskEntries.get(position);
        String name = taskEntry.getEmployeeFullName();
        String imageUrl = taskEntry.getEmployeeDownloadUrl();
        String updatedAt = TimesheetUtil.getCurrentTimeUsingCalendar();
        boolean availability = taskEntry.isEmployeeAvailable();

        //Set values
        holder.employeeName.setText(name);
       // holder.updatedAtView.setText(updatedAt);
        holder.layer.setAlpha((float) (availability ? 1.0 : 0.2));

        if (!imageUrl.isEmpty()) {
            Glide.with(holder.employeePic.getContext())
                    .load(imageUrl).placeholder(R.drawable.image_placeholder)
                    .into(holder.employeePic);
        }
    }

    /**
     * Returns the number of items to display.
     */
    @Override
    public int getItemCount() {
        if (mTaskEntries == null) {
            return 0;
        }
        return mTaskEntries.size();
    }

    /**
     * When data changes, this method updates the list of taskEntries
     * and notifies the adapter to use the new values on it
     */
    void setTasks(List<EmployeeEntry> taskEntries) {
        mTaskEntries = taskEntries;
        notifyDataSetChanged();
    }

    public interface ItemClickListener {
        // void onItemClickListener(String itemId);
        void onItemClickListener(String name, String uId, String employer);

    }

    // Inner class for creating ViewHolders
    class EmployeeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // Class variables for the task description and priority TextViews
        TextView employeeName;
       // TextView updatedAtView;
        ImageView employeePic;
        LinearLayout layer;

        /**
         * Constructor for the TaskViewHolders.
         *
         * @param itemView The view inflated in onCreateViewHolder
         */
        EmployeeViewHolder(View itemView) {
            super(itemView);

            employeeName = itemView.findViewById(R.id.employeeName);
            //updatedAtView = itemView.findViewById(R.id.taskUpdatedAt);
            employeePic = itemView.findViewById(R.id.employeeImage);
            layer = itemView.findViewById(R.id.layer);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mItemClickListener.onItemClickListener(mTaskEntries.get(getAdapterPosition()).getEmployeeFullName(), mTaskEntries.get(getAdapterPosition()).getEmployeeUniqueId(), mTaskEntries.get(getAdapterPosition()).getEmployerName());
        }
    }
}