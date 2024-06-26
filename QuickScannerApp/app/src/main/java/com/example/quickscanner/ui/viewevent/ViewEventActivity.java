package com.example.quickscanner.ui.viewevent;

import static android.content.ContentValues.TAG;
import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_NULL;
import static android.text.InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE;
import static android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE;

import static com.google.android.material.internal.ViewUtils.hideKeyboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.quickscanner.controller.FirebaseAnnouncementController;
import com.example.quickscanner.databinding.ActivityVieweventNewBinding;
import com.example.quickscanner.model.Announcement;
import com.example.quickscanner.ui.attendance.AttendanceActivity;
import com.example.quickscanner.R;
import com.example.quickscanner.controller.FirebaseAttendanceController;
import com.example.quickscanner.controller.FirebaseEventController;
import com.example.quickscanner.controller.FirebaseImageController;
import com.example.quickscanner.controller.FirebaseQrCodeController;
import com.example.quickscanner.controller.FirebaseUserController;
import com.example.quickscanner.databinding.ActivityVieweventBinding;
import com.example.quickscanner.model.Event;
import com.example.quickscanner.model.User;
import com.example.quickscanner.ui.addevent.QRCodeDialogFragment;
import com.example.quickscanner.ui.adminpage.BrowseEventsActivity;
import com.example.quickscanner.ui.viewevent.map.MapActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

public class ViewEventActivity extends AppCompatActivity {
    String eventID;
    private FirebaseEventController fbEventController;
    private FirebaseImageController fbImageController;
    private FirebaseUserController fbUserController;
    private FirebaseAttendanceController fbAttendanceController;
    private Event event;
    private ActivityVieweventNewBinding binding;
    private ProgressBar loading;
    private RelativeLayout contentLayout;
    private Integer loadCount;
    private FirebaseAnnouncementController fbAnnouncementController;


    private FirebaseQrCodeController fbQRCodeController;
    // UI reference
    private Switch toggleGeolocation;

    private boolean editMode;
    public boolean doneSetting;

    private Event currentEvent;
    private Bitmap qrCodeBitmap;


    private Bitmap eventBitMap;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVieweventNewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        doneSetting = false;

        //references
        fbEventController = new FirebaseEventController();
        fbImageController = new FirebaseImageController();
        fbQRCodeController = new FirebaseQrCodeController();
        fbAnnouncementController = new FirebaseAnnouncementController();
        fbUserController = new FirebaseUserController();
        Log.d(TAG, "onCreate: " + fbUserController.getCurrentUserUid());
        fbAttendanceController = new FirebaseAttendanceController();
        toggleGeolocation = findViewById(R.id.toggle_geolocation); // geolocation switch
        loading = findViewById(R.id.loading);
        contentLayout = findViewById(R.id.contentLayout);
        loading.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);
        binding.signUpButton.setVisibility(View.GONE);
        loadCount = 5;

        Log.d("LoadCount", "Decrementing loadCount, current value: " + loadCount);

        ActivityResultLauncher<Intent> activityResultLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Uri uri = data.getData();
                        try {
                            eventBitMap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            binding.eventImageImage.setImageBitmap(eventBitMap);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

        binding.eventImageImage.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: EditMode " + editMode);
            if (editMode) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                galleryIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(galleryIntent);
            }
        });


        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh the event data
                loadCount = 5;
                loading.setVisibility(View.VISIBLE);
                contentLayout.setVisibility(View.GONE);
                binding.signUpButton.setVisibility(View.GONE);
                Log.d("LoadCount", "Decrementing loadCount, current value: " + loadCount);

                fetchEventData();
                swipeRefreshLayout.setRefreshing(false);
                qrCodeBitmap = generateQRCode(event.getPromoQrCode());
                Log.d("halpp", "event is null? " + event.getPromoQrCode());
            }
        });


        // Set click listeners
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create and show the custom dialog
                showOrganiserDetailsDialog();
            }
        };

        binding.organiserProfilePicture.setOnClickListener(listener);
        binding.organiserText.setOnClickListener(listener);

        // Display Back Button
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // Grab any Intent bundle/parameters
        Bundle inputBundle = getIntent().getExtras();
        if (inputBundle != null) {
            eventID = inputBundle.getString("eventID");
//            Log.d("Beans", eventID);
        }
        if (eventID != null) {
            Log.e("halpp", "Event ID is: " + eventID);
        }
        fetchEventData();


        // Get a reference to the announcement button
        FloatingActionButton announcementButton = findViewById(R.id.announcement_button);

        // Set an OnClickListener to the button
        announcementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an EditText for the user to input their announcement
                final EditText input = new EditText(ViewEventActivity.this);

                // Create an AlertDialog
                new AlertDialog.Builder(ViewEventActivity.this)
                        .setTitle("Announcement")
                        .setMessage("What do you wish to announce?")
                        .setView(input)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Get the user's input
                                String announcement_content = input.getText().toString();

                                Announcement announcement_actual = new Announcement(announcement_content, event.getName());
                                announcement_actual.setEventID(eventID);
                                announcement_actual.setIsMilestone(false);
                                announcement_actual.setOrganizerID(event.getOrganizerID());

                                // TODO: Handle the announcement (e.g., send it to Firebase)

                                fbAnnouncementController.addAnnouncement(eventID, announcement_actual)
                                        .addOnSuccessListener(new OnSuccessListener() {
                                            @Override
                                            public void onSuccess(Object o) {
                                                Toast.makeText(ViewEventActivity.this, "Announcement made successfully", Toast.LENGTH_LONG).show();

                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(ViewEventActivity.this, "Failed to add announcement", Toast.LENGTH_SHORT).show();
                                            }
                                        });

                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });


        // Get a reference to the share button
        FloatingActionButton shareButton = findViewById(R.id.share_button);

        // Set an OnClickListener to the button
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Convert the QR code Bitmap to a Uri
                String path = MediaStore.Images.Media.insertImage(getContentResolver(), qrCodeBitmap, "QR Code", null);
                Log.d("beans", "path is: " + qrCodeBitmap.toString());
                Log.d("beans", "this is wheere it fails");
                Uri qrCodeUri = Uri.parse(path);

                // Create an Intent with ACTION_SEND action
                Intent shareIntent = new Intent(Intent.ACTION_SEND);

                // Put the Uri of the image and the text you want to share in the Intent
                shareIntent.putExtra(Intent.EXTRA_STREAM, qrCodeUri);
                Log.d("halpp", "event is null? " + (event == null));
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Join my event titled " + event.getName() + " using this QR code");

                shareIntent.setType("image/jpeg");

                // Start the Intent
                //java.lang.NullPointerException: Attempt to invoke virtual method
                //'java.lang.String com.example.quickscanner.model.Event.getName()' on a null object reference
                startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
            }
        });

        // implement geolocation switch behaviour
        toggleGeolocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override

            public void onCheckedChanged(CompoundButton switchView, boolean isChecked)
            {
                if (event != null && doneSetting == true)
                {
                    // toggle user's geolocation preferences
                    event.toggleIsGeolocationEnabled();
                    // update user's geolocation preferences in firebase
                    fbEventController.updateEvent(event)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Event successfully Updated"))
                            .addOnFailureListener(e -> Log.d(TAG, "Event failed to update"));

                }
                else
                {
                    Log.e("ViewEventActivity", "Event object is null");
                }

            }
        });


    }


    private void showOrganiserDetailsDialog() {

        Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.dialog_organiser_details);

        //get references for all views
        ImageView organiserProfilePicture = dialog.findViewById(R.id.dialog_organiser_profile_picture);
        TextView organiserName = dialog.findViewById(R.id.dialog_organiser_name);
        TextView organiserEmail = dialog.findViewById(R.id.dialog_organiser_email);
        TextView organiserLinkedIn = dialog.findViewById(R.id.dialog_organiser_linkedin);
        ProgressBar progressBar = dialog.findViewById(R.id.progressBar);

        progressBar.setVisibility(View.VISIBLE);

        fbUserController.getUser(event.getOrganizerID()).addOnSuccessListener(new OnSuccessListener<User>() {
            public void onSuccess(User user) {
                if (user != null && user.getUserProfile() != null) {
                    // Set the organiser details

                    // Get the URL of the organizer's profile picture
                    if (user.getUserProfile().getImageUrl() == null || user.getUserProfile().getImageUrl().isEmpty()) {
                        //TODO: set default image to our default. i forget what the path is.
                        user.getUserProfile().setImageUrl("default.jpeg");
                    }
                    fbImageController.downloadImage(user.getUserProfile().getImageUrl()).addOnCompleteListener(task1 -> {
                        String url = String.valueOf(task1.getResult());
                        Picasso.get().load(url).into(organiserProfilePicture, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                progressBar.setVisibility(View.GONE);


                            }

                            @Override
                            public void onError(Exception e) {
                                progressBar.setVisibility(View.GONE);
                            }

                        });
                    });
                    if (user.getUserProfile().getName() == null || user.getUserProfile().getName().isEmpty()) {
                        organiserName.setText("Anonymous Organizer");
                    } else {
                        organiserName.setText(user.getUserProfile().getName());
                    }
                    if (user.getUserProfile().getEmail() == null || user.getUserProfile().getEmail().isEmpty()) {
                        organiserEmail.setText("No email provided");
                    } else {
                        organiserEmail.setText(user.getUserProfile().getEmail());
                    }
                    if (user.getUserProfile().getWebsite() == null || user.getUserProfile().getWebsite().isEmpty()) {
                        organiserLinkedIn.setText("No LinkedIn provided");
                    } else {
                        organiserLinkedIn.setText(user.getUserProfile().getWebsite());
                    }
                } else {
                    Log.d("halpp", "Document not retrieved, setting default image");
                    binding.organiserText.setText("Unknown");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("ViewEventActivity", "Error fetching user data: " + e.getMessage());
            }
        });

        dialog.show();
    }

    // Fetches the event data from Firestore
    private void fetchEventData() {

        String UiD = fbUserController.getCurrentUserUid();
        fbEventController.getEvent(eventID).addOnSuccessListener(new OnSuccessListener<Event>() {
            @Override
            public void onSuccess(Event gotEvent) {
                if (gotEvent == null) {
                    throw new RuntimeException("No such Event");
                }
                event = gotEvent;
                boolean oldIsGeolocationEnabled = event.getIsGeolocationEnabled();
                toggleGeolocation.setChecked(event.getIsGeolocationEnabled());
                event.setGeolocationEnabled(oldIsGeolocationEnabled);
                doneSetting = true;
                fbEventController.updateEvent(event)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Event successfully Updated"))
                        .addOnFailureListener(e -> Log.d(TAG, "Event failed to update"));
                qrCodeBitmap = generateQRCode(event.getPromoQrCode());
                setEventDataToUI(event, UiD);

                binding.signUpButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        String buttonText = binding.signUpButton.getText().toString();
                        if (buttonText.equals("Cancel Sign Up")) {
                            confirmCancelSignUp(UiD, eventID);
                            setEventDataToUI(event, UiD);
                        } else if (buttonText.equals("Sign Up for Event")) {
                            fbAttendanceController.signUp(UiD, eventID).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            setEventDataToUI(event, UiD);
                                        }


                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            if ("Event is full".equals(e.getMessage())) {
                                                Toast.makeText(ViewEventActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(ViewEventActivity.this, "Error, please try again!", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                    });
                            fetchEventData();
                        } else if (buttonText.equals("Attendance Information")) {
                            // Create an Intent to open the AttendanceActivity
                            Intent intent = new Intent(ViewEventActivity.this,
                                    AttendanceActivity.class);
                            // Add the event ID to the intent
                            intent.putExtra("eventID", eventID);
                            // Start the AttendanceActivity
                            startActivity(intent);
                        } else if (buttonText.equals("Save")) {
                            setNonEditConstraint();
                            binding.signUpButton.setText("Attendance Information");

                            event.setLocation(String.valueOf(binding.locationTextview.getText()));
                            event.setName(String.valueOf(binding.eventTitleText.getText()));
                            event.setDescription(String.valueOf(binding.eventDescriptionText.getText()));

                            binding.locationTextview.setInputType(TYPE_NULL);
                            binding.eventTitleText.setInputType(TYPE_NULL);
                            //binding.eventDescriptionText.setInputType(TYPE_NULL);
                            binding.eventDescriptionText.setFocusable(true);
                            binding.eventDescriptionText.setFocusableInTouchMode(true);
                            editMode = false;

                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                            if (eventBitMap != null) {
                                event.setImagePath(event.getEventID() + "primary");
                                ByteArrayOutputStream boas = new ByteArrayOutputStream();
                                eventBitMap.compress(Bitmap.CompressFormat.JPEG, 100, boas);
                                byte[] imageData = boas.toByteArray();
                                fbImageController.uploadImage(event.getEventID() + "primary", "event", imageData);
                            }
                            fbEventController.updateEvent(event);
                        }
                    }
                });
                decrementLoadCount("event");

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("ViewEventActivity", "Error fetching event data: " + e.getMessage());
            }
        });

    }

    private void setNonEditConstraint() {
        Button timeButton = binding.setTimeButton;
        Button locationButton = binding.setLocationButton;
        timeButton.setVisibility(View.GONE);
        locationButton.setVisibility(View.GONE);

        EditText timeText = binding.eventTimeText;
        EditText locationText = binding.locationTextview;
        EditText descriptionText = binding.eventDescriptionText;

        ConstraintLayout.LayoutParams timeParams = (ConstraintLayout.LayoutParams) timeText.getLayoutParams();
        ConstraintLayout.LayoutParams locationParams = (ConstraintLayout.LayoutParams) locationText.getLayoutParams();
        ConstraintLayout.LayoutParams descriptionParams = (ConstraintLayout.LayoutParams) descriptionText.getLayoutParams();

        locationParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
        locationParams.startToEnd = ConstraintLayout.LayoutParams.UNSET;
        locationParams.startToStart = binding.constraintLayout.getId();
        locationParams.topToBottom = timeText.getId();

        timeParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
        timeParams.startToEnd = ConstraintLayout.LayoutParams.UNSET;
        timeParams.startToStart = binding.constraintLayout.getId();
        timeParams.topToTop = ConstraintLayout.LayoutParams.UNSET;
        timeParams.topToBottom = binding.organiserProfilePicture.getId();

        descriptionParams.topMargin = 100;

        timeText.setLayoutParams(timeParams);
        locationText.setLayoutParams(locationParams);
        descriptionText.setLayoutParams(descriptionParams);
    }

    private void setEditConstraint() {
        /*Button timeButton = binding.setTimeButton;
        Button locationButton = binding.setLocationButton;
        //timeButton.setVisibility(View.VISIBLE);
        //locationButton.setVisibility(View.VISIBLE);

        EditText timeText = binding.eventTimeText;
        EditText locationText = binding.locationTextview;
        EditText descriptionText = binding.eventDescriptionText;

        ConstraintLayout.LayoutParams timeParams = (ConstraintLayout.LayoutParams) timeText.getLayoutParams();
        ConstraintLayout.LayoutParams locationParams = (ConstraintLayout.LayoutParams) locationText.getLayoutParams();
        ConstraintLayout.LayoutParams descriptionParams = (ConstraintLayout.LayoutParams) descriptionText.getLayoutParams();

        locationParams.bottomToBottom = locationButton.getId();
        locationParams.startToEnd = locationButton.getId();
        locationParams.startToStart = ConstraintLayout.LayoutParams.UNSET;
        locationParams.topToBottom = timeButton.getId();

        timeParams.bottomToBottom = timeButton.getId();
        timeParams.startToEnd = timeButton.getId();
        timeParams.startToStart = ConstraintLayout.LayoutParams.UNSET;
        timeParams.topToTop = timeButton.getId();
        timeParams.topToBottom = ConstraintLayout.LayoutParams.UNSET;


        //descriptionParams.topMargin = 30;

        //timeText.setLayoutParams(timeParams);
        //locationText.setLayoutParams(locationParams);
        //descriptionText.setLayoutParams(descriptionParams);*/
    }

    private void setEventDataToUI(Event event, String UiD) {
        editMode = false;
        //use event object to update all the views
        setNonEditConstraint();
        binding.constraintLayout.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        });
        binding.eventTitleText.setText(event.getName());
        binding.eventDescriptionText.setText(event.getDescription());
        binding.locationTextview.setText(event.getLocation());
        FloatingActionButton announcementButton = binding.announcementButton;

        if (!UiD.equals(event.getOrganizerID())) {
            // If the current user is not the organizer, hide the announcement button
            announcementButton.setEnabled(false);
        }


        binding.eventDescriptionText.setFocusable(false);
        binding.eventDescriptionText.setFocusableInTouchMode(false);
        binding.organiserProfilePicture.setImageResource(R.drawable.ic_home_black_24dp);
        if (!event.getOrganizerID().equals(fbUserController.getCurrentUserUid())) {
            toggleGeolocation.setVisibility(View.GONE);
            binding.textView5.setVisibility(View.GONE);
        }
        // disabling the set buttons for location and time
        fbUserController.getUser(event.getOrganizerID()).addOnSuccessListener(new OnSuccessListener<User>() {
            public void onSuccess(User user) {
                if (user != null && user.getUserProfile() != null) {
                    //disable this organiser text line if creating new event crashes
//                    binding.organiserText.setText(user.getUserProfile().getName());
                    // this code exists to underline the organiser's name
                    SpannableString content = new SpannableString(user.getUserProfile().getName());
                    content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                    binding.organiserText.setText(content);

                    Log.d("halpp", "Organiser name is: " + user.getUserProfile().getName());

//                    Profile organizerProfile = organizer.getUserProfile();

                    // Get the URL of the organizer's profile picture
                    fbImageController.downloadImage(user.getUserProfile().getImageUrl()).addOnCompleteListener(task1 -> {
                        String url = String.valueOf(task1.getResult());
                        Picasso.get().load(url).into(binding.organiserProfilePicture);
                        decrementLoadCount("org pic");
                    });
                }
                decrementLoadCount("user");

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("ViewEventActivity", "Error fetching user data: " + e.getMessage());
            }
        });
        binding.eventTimeText.setText(event.getTimeAsString());
        // Check if the current user is the organizer
        if (UiD.equals(event.getOrganizerID())) {
            binding.signUpButton.setText("Attendance Information");

            decrementLoadCount("button attend");


        } else {
            // Check if the user is signed up for the event
            fbAttendanceController.isUserSignedUp(eventID, UiD).addOnCompleteListener(new OnCompleteListener<Boolean>() {
                @Override
                public void onComplete(@NonNull Task<Boolean> task) {
                    if (task.isSuccessful()) {
                        boolean isSignedUp = task.getResult();
                        if (isSignedUp) {
                            binding.signUpButton.setText("Cancel Sign Up");
                            binding.signUpButton.setEnabled(true);
                            binding.signUpButton.setBackgroundColor(ContextCompat
                                    .getColor(ViewEventActivity.this, R.color.purple_500));
                        } else {
                            if (event.getMaxSpots() != null && event.getMaxSpots() <= event.getTakenSpots()) {
                                binding.signUpButton.setText("Event Full");
                                binding.signUpButton.setEnabled(false);
                                binding.signUpButton.setBackgroundColor(Color.GRAY);
                            } else {
                                binding.signUpButton.setText("Sign Up for Event");
                                binding.signUpButton.setEnabled(true);
                                binding.signUpButton.setBackgroundColor(ContextCompat
                                        .getColor(ViewEventActivity.this, R.color.purple_500));
                            }
                        }
                        decrementLoadCount("button");
                    } else {
                        Log.e(TAG, "Failed at checking sign up", task.getException());
                    }
                }
            });
        }

        //
        fbImageController.downloadImage(event.getImagePath()).addOnCompleteListener(task1 ->
        {
            if (task1.isSuccessful()) {
                String url = String.valueOf(task1.getResult());
                Picasso.get().load(url).into(binding.eventImageImage);
            } else {
                Log.d("halppp", "Document not retrieved, setting default image");
                binding.eventImageImage.setImageResource(R.drawable.ic_home_black_24dp);
            }
            decrementLoadCount("event image again?");

        });



        binding.eventTimeText.setText(event.getTimeAsString());

        Log.d("halpp", "Event ID is: " + eventID);


        //just generate the qr code from the event id and set it to the qr code image view
        //and get the image from the firebase storage and set it to the image view
        fbImageController.downloadImage(event.getImagePath()).addOnCompleteListener(task1 ->
        {
            if (task1.isSuccessful()) {
                String url = String.valueOf(task1.getResult());
                Picasso.get().load(url).into(binding.eventImageImage);
            } else {
                Log.d("halppp", "Document not retrieved, setting default image");
                binding.eventImageImage.setImageResource(R.drawable.ic_home_black_24dp);
            }
            decrementLoadCount("event image");
        });
    }


    //generates QR code
    private Bitmap generateQRCode(String text) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*         Inflate Handle Top Menu Options        */
    // Create the Top Menu bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // inflate menu
        getMenuInflater().inflate(R.menu.viewevent_top_nav_menu, menu);

        // grant organizer access
        Bundle menuBundle = getIntent().getExtras();
        String menuEventID = menuBundle.getString("eventID");
        // Query Event Object from Firebase
        new FirebaseEventController().getEvent(menuEventID)
            .addOnSuccessListener(new OnSuccessListener<Event>() {
                @Override
                public void onSuccess(Event event) {
                    if (event == null){
                        throw new RuntimeException("No such Event");
                    }
                    // event exists
                    if (Objects.equals(event.getOrganizerID(), new FirebaseUserController().getCurrentUserUid())) {
                        // user is an organizer. Allow organizer permissions.
                        menu.findItem(R.id.navigation_QR_check_in).setVisible(true);
                        menu.findItem(R.id.navigation_edit).setVisible(true);
                        menu.findItem(R.id.map).setVisible(true);
                        menu.findItem(R.id.navigation_delete).setVisible(true);
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("ViewEventActivity", "Error fetching event data: " + e.getMessage());
                }
        });


        // grant admin access
        Boolean isAdmin = menuBundle.getBoolean("adminkey", false);
        if (isAdmin) {
            menu.findItem(R.id.navigation_delete).setVisible(true);
        }

        return true;

    }

    /*    Handle click events for the Top Menu Bar    */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.navigation_edit) {
            setEditConstraint();
            binding.signUpButton.setText("Save");

            binding.locationTextview.setInputType(TYPE_CLASS_TEXT);
            binding.eventTitleText.setInputType(TYPE_CLASS_TEXT);
            binding.eventDescriptionText.setInputType(TYPE_TEXT_FLAG_MULTI_LINE);
            binding.eventDescriptionText.setFocusable(true);
            binding.eventDescriptionText.setFocusableInTouchMode(true);
            binding.eventDescriptionText.setSingleLine(false);
            editMode = true;
        }
        else if (itemId == R.id.navigation_QR_check_in)
        {
            // Handle Click

            //todo: implement dialog fragment for check in qr code
            String checkinQrCode = event.getCheckInQrCode();
            Log.d("testerrrr", "checkinQrCode: " + checkinQrCode);

            // Create and show the QR code dialog fragment
            QRCodeDialogFragment.newInstance(checkinQrCode).show(getSupportFragmentManager(), "QRCodeDialogFragment");

            return true;

        } else if (itemId == R.id.navigation_QR_promotional) {
            // Handle click

            //todo: implement dialog fragment for promotional qr code
            String promoQrCode = event.getPromoQrCode();

            Log.d("testerrrr", "promoQRcode: " + promoQrCode);


            // Create and show the QR code dialog fragment
            QRCodeDialogFragment.newInstance(promoQrCode).show(getSupportFragmentManager(), "QRCodeDialogFragment");


            return true;

        } else if (itemId == R.id.map) {
            // Handle Map click
            Intent intent = new Intent(ViewEventActivity.this, MapActivity.class);
            Bundle bundle = new Bundle();
            // Check if Geolocation is Enabled
            if (!event.getIsGeolocationEnabled()) {
                Toast.makeText(this, R.string.enable_geolocation, Toast.LENGTH_SHORT).show();
                return true;
            }
            // Add the hashed location
            bundle.putString("geoHash", event.getGeoLocation());
            // Add eventID
            bundle.putString("eventID", event.getEventID());
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.navigation_delete) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete this Event?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            fbEventController.deleteEvent(eventID)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(ViewEventActivity.this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(ViewEventActivity.this, BrowseEventsActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(ViewEventActivity.this, "Failed to delete event", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            // Handle the Back button press
            finish();
            return true;
        }
        return false;

    }

    public void confirmCancelSignUp(String UiD, String eventID) {
        // Create an AlertDialog
        new AlertDialog.Builder(ViewEventActivity.this)
                .setTitle("Cancel Sign Up")
                .setMessage("Are you sure you want to cancel your sign up?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int yesButton) {
                        fbAttendanceController.removeFromSignUp(UiD, eventID).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Refresh the UI upon successful sign-up cancellation
                                        setEventDataToUI(event, UiD);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Display a toast message upon failure
                                        Toast.makeText(ViewEventActivity.this, "Removal failed, please try again!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showContent() {
        loading.setVisibility(View.GONE);
        binding.signUpButton.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.VISIBLE);
    }

    private synchronized void decrementLoadCount(String location) {
        loadCount--;
        Log.d(TAG, "loadCount after decrement " + location + " : " + loadCount); // Corrected to log after decrement
        if (loadCount == 0) {
            showContent();
        }
    }


}

