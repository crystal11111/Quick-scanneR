package com.example.quickscanner.ui.profile;

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_NULL;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem; // Import Menu class
import androidx.annotation.NonNull;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.quickscanner.FirebaseController;
import com.example.quickscanner.R;
import com.example.quickscanner.model.Profile;
import com.example.quickscanner.model.User;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    Button editButton;
    EditText nameEdit;
    EditText emailEdit;
    EditText linkedinEdit;
    boolean editMode;

    User myUser;
    Profile myProfile;
    FirebaseController fbController;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        fbController = new FirebaseController();
        Log.w("error", fbController.getCurrentUserUid());
        Log.w("error", "weird p1");
        fbController.getUser(fbController.getCurrentUserUid()).addOnCompleteListener(task -> {
            Log.w("error", "pleasepleasepleasepleaseplease");
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    myUser = document.toObject(User.class);
                    myProfile = myUser.getUserProfile();
                    Log.w("success:?", "weird");

                    editButton = findViewById(R.id.edit_button);
                    nameEdit  = findViewById(R.id.nameEdit);
                    emailEdit = findViewById(R.id.emailEdit);
                    linkedinEdit  = findViewById(R.id.socialEdit);

                    nameEdit.setText(myProfile.getName());
                    emailEdit.setText(myProfile.getEmail());
                    linkedinEdit.setText(myProfile.getWebsite());

                    editButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!editMode) {
                                nameEdit.setInputType(TYPE_CLASS_TEXT);
                                emailEdit.setInputType(TYPE_CLASS_TEXT);
                                linkedinEdit.setInputType(TYPE_CLASS_TEXT);
                                editMode = !editMode;
                                editButton.setText("Save");
                            } else {
                                myProfile.setName(String.valueOf(nameEdit.getText()));
                                myProfile.setEmail(String.valueOf(emailEdit.getText()));
                                myProfile.setWebsite(String.valueOf(linkedinEdit.getText()));
                                myUser.setUserProfile(myProfile);
                                fbController.updateUser(myUser);
                                nameEdit.setInputType(TYPE_NULL);
                                emailEdit.setInputType(TYPE_NULL);
                                linkedinEdit.setInputType(TYPE_NULL);
                                editMode = !editMode;
                                editButton.setText("Edit");
                            }
                        }
                    });
                } else {
                    Log.w("error", "not working:( exists");
                }
            } else {
                Log.w("error", "not working:( isSuccessful");
            }
        }).addOnFailureListener(e -> Log.w("error", e)).addOnCanceledListener(() -> {
            Log.w("error", ":33");
        });


        editMode = false;
        // backbutton
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

    }

    // Handles The Top Bar menu clicks
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle the Back button press
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);

    }
}