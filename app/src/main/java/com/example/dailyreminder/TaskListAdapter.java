package com.example.dailyreminder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dailyreminder.R; // Corrected import

import java.util.List;

public class TaskListAdapter extends ArrayAdapter<Task> {

    public TaskListAdapter(Context context, List<Task> tasks) {
        super(context, 0, tasks);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        Task task = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_task, parent, false);
        }

        // Lookup view for data population
        TextView taskTitle = convertView.findViewById(R.id.taskTitleTextView);
        TextView taskDescription = convertView.findViewById(R.id.taskDescriptionTextView);
        TextView taskTime = convertView.findViewById(R.id.taskTimeTextView);

        // Populate the data into the template view using the data object
        if (task != null) {
            taskTitle.setText(task.getTitle());
            taskDescription.setText(task.getDescription());
            taskTime.setText(task.getTime());
        }

        // Return the completed view to render on screen
        return convertView;
    }
}
