package com.example.quickscanner.ui.addevent;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.quickscanner.R;
import com.example.quickscanner.controller.FirebaseEventController;
import com.example.quickscanner.singletons.ConferenceConfigSingleton;
import com.example.quickscanner.controller.FirebaseImageController;
import com.example.quickscanner.controller.FirebaseQrCodeController;
import com.example.quickscanner.controller.FirebaseUserController;
import com.example.quickscanner.model.Event;
import com.google.firebase.Timestamp;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

//javadocs for this class
/**
 * This class is responsible for adding an event to the database.
 * It allows the user to input the event name, description, location, geolocation, time, and maximum attendees.
 * The user can also upload an image for the event.
 * The user can also generate a QR code for the event.
 * The user can also view the event details.
 */

public class AddEventActivity extends AppCompatActivity
{
    /* Uses Open Street Maps to display user's
     *  check-in geolocation
     *  Credits: https://github.com/osmdroid/osmdroid/wiki/How-to-use-the-osmdroid-library-(Java)
     *           https://developer.android.com/training/permissions/requesting
     */

    private TextView eventDescriptionTextView;
    private TextView eventNameEditText;
    private ImageView eventImageView;
    private String editedEventName;
    private String editedEventDescription;
    private EditText locationEditText;
    private EditText geolocationEditText;
    private EditText timeEditText;
    private Timestamp eventTime;
    private String minTime;
    private String maxTime;
    private EditText maxAttendeeEditText;


    private ArrayAdapter<Event> eventAdapter;
    private List<Event> eventDataList = new ArrayList<>();
    private ActivityResultLauncher<Intent> resultLauncher;
    private ConferenceConfigSingleton configSingleton;
    private FirebaseEventController fbEventController;
    private FirebaseImageController fbImageController;
    private FirebaseUserController fbUserController;
    private FirebaseQrCodeController fbQrCodeController;


    private Bitmap eventImageMap;



    // Used to get an event location string back from the Map Activity Fragment.
    // Credit: https://developer.android.com/training/basics/intents/result
    //javadocs
    /**
     * This is an ActivityResultLauncher that is used to get the location string from the Map Activity Fragment.
     * It is used to get the geolocation of the event.
     */
    ActivityResultLauncher<Intent> mapGetLocation = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->
            {
                if (result.getResultCode() == Activity.RESULT_OK)
                {
                    Intent data = result.getData();
                    if (data != null)
                    {
                        String geoHash = data.getStringExtra("geoHash");
                        // Handle the location string
                        geolocationEditText.setText(geoHash);

                    }
                }
            });


    /**
     * This method is called when the activity is first created.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addevent);
        fbEventController = new FirebaseEventController();
        fbImageController = new FirebaseImageController();
        fbUserController = new FirebaseUserController();
        fbQrCodeController = new FirebaseQrCodeController();
        configSingleton = ConferenceConfigSingleton.getInstance();

        // Initialize views
        eventDescriptionTextView = findViewById(R.id.EventDescription);
        eventNameEditText = findViewById(R.id.EventName);
        eventImageView = findViewById(R.id.imageView);

        locationEditText = findViewById(R.id.location_textview);
        geolocationEditText = findViewById(R.id.geolocation_textview);
        maxAttendeeEditText = findViewById(R.id.maxattendee_textview);

        timeEditText = findViewById(R.id.time_textview);
        timeEditText.setFocusable(false); // can't type, must interact with listener
        timeEditText.setOnClickListener(view -> showDateTimePicker());

        // Initialize the event data list and ArrayAdapter
        eventDataList = new ArrayList<>();
        eventAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, eventDataList);

        // back button
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // Location Text Click behaviour
        geolocationEditText = findViewById(R.id.geolocation_textview);
        geolocationEditText.setFocusable(false); // can't type, must interact with listener
        geolocationEditText.setOnClickListener(view -> {
            // Open map activity to choose your location
            // Credit: https://developer.android.com/training/basics/intents/result
            Intent intent = new Intent(AddEventActivity.this, MapActivity.class);
            // Pass the current location to the map activity
            Bundle bundle = new Bundle(1);
            bundle.putString("geoHash", geolocationEditText.getText().toString());
            intent.putExtras(bundle);
            // start map activity
            mapGetLocation.launch(intent);
        });

        // Edit Button 1
        ImageButton editBtn1 = findViewById(R.id.editBtn1);
        editBtn1.setOnClickListener(v -> showTextDialog("Edit Event Name", (dialog, which) ->
        {
            editedEventName = ((EditText) ((AlertDialog) dialog).findViewById(R.id.input)).getText().toString();
            eventNameEditText.setText(editedEventName);
        }));

        // Edit Button 2
        ImageButton editBtn2 = findViewById(R.id.editBtn2);
        editBtn2.setOnClickListener(v -> showTextDialog("Edit Event Description", (dialog, which) ->
        {
            editedEventDescription = ((EditText) ((AlertDialog) dialog).findViewById(R.id.input)).getText().toString();
            eventDescriptionTextView.setText(editedEventDescription);
        }));

        // Image Button
        ImageButton editImageButton = findViewById(R.id.editImageButton);
        registerResult();
        editImageButton.setOnClickListener(view -> pickImage());

//        // QR code generation
//        ImageButton generateQRbtn = findViewById(R.id.generateQRbtn);
//        generateQRbtn.setOnClickListener(v -> {
//            QRCodeDialogFragment.newInstance(null).show(getSupportFragmentManager(), "QRCodeDialogFragment");
//        });

        // Create Event Button
        Button createEventInsideBtn = findViewById(R.id.CreateEventInsideBtn);
        createEventInsideBtn.setOnClickListener(v ->
        {
            // Check if necessary fields are filled
            if (editedEventName != null && !editedEventName.isEmpty() &&
                    editedEventDescription != null && !editedEventDescription.isEmpty() &&
                    eventTime != null && locationEditText.getText() != null && !locationEditText.getText().toString().isEmpty())
            {

                // Retrieve the text from the EditText fields
                String location = locationEditText.getText().toString();
                String geolocation = geolocationEditText.getText().toString();

                // Create an Event object with the edited values
                Event newEvent = new Event(editedEventName, editedEventDescription,
                        fbUserController.getCurrentUserUid(), eventTime, location);
                newEvent.setGeoLocation(geolocation);

                // Add the event to the database
                addEventToFirestore(newEvent,eventDataList,eventAdapter);

            }
        });
    }



    // Add event to Firestore
    //javadocs
    /**
     * This method adds an event to the Firestore database.
     * @param event The event to be added to the database.
     */
    private void addEventToFirestore(Event event, List<Event> eventsDataList, ArrayAdapter<Event> eventAdapter)
    {
        String maxAttendeesString = maxAttendeeEditText.getText().toString();
        if (!maxAttendeesString.isEmpty()) {
            int maxAttendees = Integer.parseInt(maxAttendeesString);
            event.setMaxSpots(maxAttendees);
        }

        fbEventController.addEvent(event)
                .addOnSuccessListener(documentReference ->
                {
                    Log.d("AddEventActivity", "Event added with ID: " +
                            documentReference.getId());
                    // additional actions if needed
                    event.setEventID(documentReference.getId());
                    if (eventImageMap != null)
                    {
                        event.setImagePath(event.getEventID() + "primary");
                        ByteArrayOutputStream boas = new ByteArrayOutputStream();
                        eventImageMap.compress(Bitmap.CompressFormat.JPEG, 100, boas);
                        byte[] imageData = boas.toByteArray();
                        fbImageController.uploadImage(event.getImagePath(), "event", imageData);
                    }
                    //promo qr code
                    fbQrCodeController.addQrCode(fbUserController.getCurrentUserUid())
                            .addOnSuccessListener(promoDocumentReference ->
                            {
                                Log.d("testerrrr", "QR code added with ID: " + promoDocumentReference.getId());
                                // additional actions if needed
                                event.setPromoQrCode(promoDocumentReference.getId());
                                Log.d("testerrrr", "event has set check in code: " + event.getPromoQrCode());
                                //check in qr code
                                fbQrCodeController.addQrCode(fbUserController.getCurrentUserUid())
                                        .addOnSuccessListener(checkDocumentReference ->
                                        {
                                            Log.d("testerrrr", "QR code added with ID: " + checkDocumentReference.getId());
                                            // additional actions if needed
                                            event.setCheckInQrCode(checkDocumentReference.getId());

                                            Log.d("testerrrr", "event has set check in code: " + event.getCheckInQrCode());
                                            Log.d("uteeee", "event is is: " + event.getEventID());
                                            Log.d("AddEventActivity", "before updateEvent: updating event with ID: " + event.getEventID());
                                            addEventToUserOrganizedEvents(event.getEventID());

                                            fbEventController.updateEvent(event);
                                            finish();

                                            //show the event details page on this event
                                        });


                            })
                            .addOnFailureListener(e ->
                            {
                                Log.e("AddEventActivity", "Error adding QR code", e);
                                // Handle the error appropriately
                            });

                })
                .addOnFailureListener(e ->
                {
                    Log.e("AddEventActivity", "Error adding event", e);
                    // Handle the error appropriately
                });





        //get current event id instance


    }

    //javadocs
    /**
     * This method adds an event to the user's list of organized events.
     * @param eventId The ID of the event to be added to the user's list of organized events.
     */
    private void addEventToUserOrganizedEvents(String eventId)
    {
        // Get the current user's ID
        String currentUserId = fbUserController.getCurrentUserUid();

        // Retrieve the current user's data from Firestore
        fbUserController.getUser(currentUserId)
                .addOnSuccessListener(user ->
                {
                    // Add the new event's ID to the user's organizedEvents list

                    user.getOrganizedEvents().add(eventId);

                    // Update the user's data in Firestore
                    fbUserController.updateUser(user)
                            .addOnSuccessListener(aVoid ->
                            {
//                                Log.d("uteeee", eventId.toString());
                            })
                            .addOnFailureListener(e ->
                                    Log.e("AddEventActivity",
                                            "Error updating user's organized events", e));
                });

    }


    //javadocs
    /**
     * This method is called when the user clicks on the back button.
     * @param item The item that was clicked.
     * @return True if the back button was clicked, false otherwise.
     */
    // Handles The Top Bar menu clicks
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            // Handle the Back button press
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    //javadocs
    /**
     * This method shows a dialog box with an EditText field.
     * The user can input text into the EditText field.
     * @param title The title of the dialog box.
     * @param positiveClickListener The listener for the positive button.
     */
    private void showTextDialog(String title, DialogInterface.OnClickListener positiveClickListener)
    {
        final EditText input = new EditText(this);
        input.setId(R.id.input); // Set the ID for the EditText
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("OK", (dialog, which) ->
                {
                    if (title.equals("Edit Event Name"))
                    {
                        editedEventName = input.getText().toString();
                        eventNameEditText.setText(editedEventName);
                    }
                    else if (title.equals("Edit Event Description"))
                    {
                        editedEventDescription = input.getText().toString();
                        eventDescriptionTextView.setText(editedEventDescription);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    //javadocs
    /**
     * This method allows the user to pick an image from the gallery.
     */
    // To pick an image from the gallery
    private void pickImage()
    {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        resultLauncher.launch(intent);
    }

    //javadocs
    /**
     * This method registers the result of the image picker.
     */
    // Register the result of the image picker
    private void registerResult()
    {
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result ->
                {
                    if (result.getResultCode() == Activity.RESULT_OK)
                    {
                        Intent data = result.getData();
                        Uri imageUri = data.getData();
                        try
                        {
                            eventImageMap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            // Set the selected image bitmap to the eventImageView
                            eventImageView.setImageBitmap(eventImageMap); // Update this line
                        } catch (Exception e)
                        {
                            Toast.makeText(AddEventActivity.this, "Error: " +
                                    e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //javadocs
    /**
     * This method shows a dialog box with a DatePicker and a TimePicker.
     * The user can select a date and time.
     */
    private void showDateTimePicker() {
        final Calendar date = Calendar.getInstance();
        final Calendar currentDate = Calendar.getInstance();

        // Use ConferenceConfigSingleton to get the min and max dates
        Calendar minDate = Calendar.getInstance();
        minDate.set(configSingleton.getMinYear(), configSingleton.getMinMonth(), configSingleton.getMinDay());

        Calendar maxDate = Calendar.getInstance();
        maxDate.set(configSingleton.getMaxYear(), configSingleton.getMaxMonth(), configSingleton.getMaxDay());

        if (currentDate.after(minDate)) {
            minDate = currentDate;
        }
        // Set the min and max dates for the DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(AddEventActivity.this, (view, year, monthOfYear, dayOfMonth) -> {
            date.set(year, monthOfYear, dayOfMonth);
            showTimePicker(date);
        }, minDate.get(Calendar.YEAR), minDate.get(Calendar.MONTH), minDate.get(Calendar.DATE));
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
        datePickerDialog.show();
    }

    //javadocs
    /**
     * This method shows a dialog box with a TimePicker.
     * The user can select a time.
     * @param date The date for which the time is being selected.
     */
    private void showTimePicker(final Calendar date) {
        final Calendar currentDate = Calendar.getInstance();

        // Uses ConferenceConfigSingleton to get the min and max times
        int minHour = configSingleton.getMinHour();
        int minMinute = configSingleton.getMinMinute();
        int maxHour = configSingleton.getMaxHour();
        int maxMinute = configSingleton.getMaxMinute();

        // Sets the min and max times for the TimePickerDialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(AddEventActivity.this, (view, hourOfDay, minute) -> {
            date.set(Calendar.HOUR_OF_DAY, hourOfDay);
            date.set(Calendar.MINUTE, minute);
            if (date.before(currentDate))
            {
                Toast.makeText(AddEventActivity.this, "Time has passed, choose a different time.", Toast.LENGTH_LONG).show();
                showTimePicker(date);
            }
            if (hourOfDay < minHour || hourOfDay > maxHour || (hourOfDay == minHour && minute < minMinute) || (hourOfDay == maxHour && minute > maxMinute)) {
                Toast.makeText(AddEventActivity.this, "Invalid time. Please select a time between " + formatTime(minHour, minMinute) + " and " + formatTime(maxHour, maxMinute) + ".", Toast.LENGTH_LONG).show();
                showTimePicker(date);
            } else {

                eventTime = new Timestamp(date.getTime());
                timeEditText.setText(formatDateTime(date));
            }
        }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false);
        timePickerDialog.show();
    }

    //javadocs
    /**
     * This method formats the time as a string.
     * @param hour The hour of the time.
     * @param minute The minute of the time.
     * @return The formatted time as a string.
     */
    private String formatTime(int hour, int minute) {
        // creates calendar object
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        // simple date format that outputs the hour in 12-hour format, the minute,
        // and if its AM or PM
        SimpleDateFormat date = new SimpleDateFormat("h:mm a", Locale.getDefault());

        // Sets the time zone from ConferenceConfigSingleton
        date.setTimeZone(TimeZone.getTimeZone(configSingleton.getTimeZone()));
        // returns formatted time as string
        return date.format(calendar.getTime());
    }

    //javadocs
    /**
     * This method formats the date and time as a string.
     * @param date The date and time to be formatted.
     * @return The formatted date and time as a string.
     */
    private String formatDateTime(Calendar date) {
        // simple date format that outputs the date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy h:mm a", Locale.getDefault());

        // Sets the time zone from ConferenceConfigSingleton
        dateFormat.setTimeZone(TimeZone.getTimeZone(configSingleton.getTimeZone()));
        // returns formatted date and time as string
        return dateFormat.format(date.getTime());
    }

}