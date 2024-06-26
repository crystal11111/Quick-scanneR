package com.example.quickscanner.ui.adminpage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.quickscanner.R;
import com.example.quickscanner.controller.FirebaseUserController;
import com.example.quickscanner.model.User;
import com.example.quickscanner.ui.profile.ProfileActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Objects;

//javadocs
/**
 * This Activity is responsible for browsing all profiles in the database.
 */
public class BrowseProfilesActivity extends AppCompatActivity {

    // ProfileList References
    ListView profileListView;
    ArrayList<User> profilesDataList;

    //had to change to ProfileArrayAdapter from ArrayAdapter<User> as it does not implement
    //check box functionality.
    ProfileArrayAdapter profileAdapter;

    // FirebaseController References
    private FirebaseUserController fbUserController;

    //javadocs
    /**
     * This method is called when the activity is created.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_profiles);

        // Enable the back button
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // FirebaseUserController instance
        fbUserController = new FirebaseUserController();

        // Store view references
        profileListView = findViewById(R.id.BrowseProfilesListView);

        // Initialize the profile data list and ArrayAdapter
        profilesDataList = new ArrayList<>();
        profileAdapter = new ProfileArrayAdapter(this, profilesDataList);
        // Set the adapter to the ListView
        profileListView.setAdapter(profileAdapter);
        updateDeleteButtonVisibility();
        // Create FireStore Listener for Updates to the Profiles List.
        fbUserController.getUsers().addOnSuccessListener(users -> {
            profilesDataList.clear();  // removes current data
                profilesDataList.addAll(users); // adds new data from db
            profileAdapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> Log.e("Firestore failed to load users:", e.toString()));

        // Inside onCreate method
        profileListView.setOnItemClickListener((parent, view, position, id) -> {
            User selectedProfile = profilesDataList.get(position);
            Intent intent = new Intent(BrowseProfilesActivity.this, ProfileActivity.class);
            intent.putExtra("selectedProfileId", selectedProfile.getUid());
            intent.putExtra("isAdmin", true);  // Add this line
            startActivity(intent);
        });

        // Find the delete button
        FloatingActionButton deleteButton = findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(v -> new AlertDialog.Builder(BrowseProfilesActivity.this)
                .setTitle("Delete Profiles")
                .setMessage("Are you sure you want to delete these Profile(s)?")
                .setPositiveButton("Yes", (dialog, which) -> deleteSelectedProfiles())
                .setNegativeButton("Cancel", null)
                .show());
    }

    //javadocs
    /**
     * This method deletes the selected profiles from the database.
     */
    private void deleteSelectedProfiles() {
        for (User user : profilesDataList) {
            if (user.isSelected()) {
                fbUserController.deleteUser(user.getUid())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(BrowseProfilesActivity.this, "Profile deleted", Toast.LENGTH_SHORT).show();
                            profilesDataList.remove(user);
                            profileAdapter.notifyDataSetChanged();
                        })
                        .addOnFailureListener(e -> Toast.makeText(BrowseProfilesActivity.this, "Failed to delete profile", Toast.LENGTH_SHORT).show());
            }
        }
    }

    //javadocs
    /**
     * This method updates the visibility of the delete button based on the number of selected profiles.
     */
    public void updateDeleteButtonVisibility() {
        FloatingActionButton deleteButton = findViewById(R.id.delete_button);
        if (profileAdapter.isAnyUserSelected()) {
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            deleteButton.setVisibility(View.GONE);
        }
    }

    //javadocs
    /**
     * This method is called when the activity is resumed.
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