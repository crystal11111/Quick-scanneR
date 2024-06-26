package com.example.quickscanner.ui.adminpage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.quickscanner.R;
import com.example.quickscanner.controller.FirebaseEventController;
import com.example.quickscanner.model.Event;
import com.example.quickscanner.ui.viewevent.ViewEventActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Objects;

//javadocs
/**
 * This Activity is responsible for browsing all events in the database.
 */
public class BrowseEventsActivity extends AppCompatActivity {

    // EventList References
    ListView eventListView;
    ArrayList<Event> eventsDataList;
    ArrayAdapter<Event> eventAdapter;

    // Firestore References
    private FirebaseEventController fbEventController;
    FloatingActionButton deleteButton;

    //javadocs
    /**
     * This method is called when the activity is created.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_events);

        // Enable the back button
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // Firebase references
        fbEventController = new FirebaseEventController();

        // Store view references
        eventListView = findViewById(R.id.BrowseEventsListView);

        // Initialize the event data list and ArrayAdapter
        eventsDataList = new ArrayList<>();
        eventAdapter = new AdminEventArrayAdapter(this, eventsDataList);
        // Set the adapter to the ListView
        eventListView.setAdapter(eventAdapter);

        // Set an OnItemClickListener to the ListView
        eventListView.setOnItemClickListener((parent, view, position, id) -> {
            // Get the selected event
            Event selectedEvent = eventsDataList.get(position);

            // Create an Intent to start the ViewEventActivity
            Intent intent = new Intent(BrowseEventsActivity.this, ViewEventActivity.class);

            // Pass the ID of the selected event to the ViewEventActivity
            intent.putExtra("eventID", selectedEvent.getEventID());

                // Pass isAdmin boolean to the ViewEventActivity
                intent.putExtra("adminkey", true);

                // Start the ViewEventActivity
                startActivity(intent);
            
        });


        // Create FireStore Listener for Updates to the Events List.
        fbEventController.getEvents().addOnCompleteListener(events ->
        {
            if (events.isSuccessful())
            {
                // Clear the current list
                eventsDataList.clear();
                eventsDataList.addAll(events.getResult());
                // Notify the adapter that the data has changed
                eventAdapter.notifyDataSetChanged();
            }
            else
            {
                Log.d("BrowseEventsActivity", "Error getting documents: ", events.getException());
            }
        });

        deleteButton = findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(v -> new AlertDialog.Builder(BrowseEventsActivity.this)
                .setTitle("Delete Events")
                .setMessage("Are you sure you want to delete these Event(s)?")
                .setPositiveButton("Yes", (dialog, which) -> deleteSelectedEvents())
                .setNegativeButton("Cancel", null)
                .show());
    }

    //javadocs
    /**
     * This method deletes the selected events from the database.
     */
    private void deleteSelectedEvents() {
        for (Event event : eventsDataList) {
            if (event.isSelected()) {
                fbEventController.deleteEvent(event.getEventID())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(BrowseEventsActivity.this, "Event deleted", Toast.LENGTH_SHORT).show();
                            eventsDataList.remove(event);
                            eventAdapter.notifyDataSetChanged();
                        })
                        .addOnFailureListener(e -> Toast.makeText(BrowseEventsActivity.this, "Failed to delete event", Toast.LENGTH_SHORT).show());
            }
        }
    }

    //javadocs
    /**
     * This method updates the visibility of the delete button based on the selected events.
     */

    public void updateDeleteButtonVisibility() {
        if (((AdminEventArrayAdapter) eventAdapter).isAnyEventSelected()) {
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            deleteButton.setVisibility(View.GONE);
        }
    }



    //javadocs
    /**
     * This method is called when an item in the options menu is selected.
     * @param item The selected item.
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}