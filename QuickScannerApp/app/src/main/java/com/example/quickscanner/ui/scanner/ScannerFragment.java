package com.example.quickscanner.ui.scanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.quickscanner.controller.FirebaseAnnouncementController;
import com.example.quickscanner.model.Announcement;
import com.example.quickscanner.singletons.SettingsDataSingleton;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.widget.Toast;

import com.example.quickscanner.R;
import com.example.quickscanner.controller.FirebaseAttendanceController;
import com.example.quickscanner.controller.FirebaseEventController;
import com.example.quickscanner.controller.FirebaseQrCodeController;
import com.example.quickscanner.controller.FirebaseUserController;
import com.example.quickscanner.controller.QRScanner;
import com.example.quickscanner.model.Event;
import com.example.quickscanner.ui.viewevent.ViewEventActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/*basically:
    if user scans sign up/registration/rsvp code, register them for event
    and show them the event details
    if user scans check-in code, check them in and show them the event details
    if user scans invalid code, show them an error message
    if user scans admin code, make them an admin
* */

//javadocs
/**
 * This Fragment hosts our QR Scanner for users to scan QR codes.
 * Can check in to events, sign up for events, or unlock admin privileges.
 */
public class ScannerFragment extends Fragment {

    private static final int REQUEST_CODE_GALLERY = 10;
    //private static final int RESULT_OK = 30;
    int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_PERMISSIONS = 100;


    private TextView textView;
    private QRScanner qrScanner;
    private String hashedUserLocation;

    private Button galleryButton;
    private Button scanButton;

    private FirebaseUserController firebaseUserController;
    private FirebaseAttendanceController fbAttendanceController;
    private FirebaseUserController fbUserController;
    private FirebaseQrCodeController fbQrCodeController;
    private FirebaseEventController fbEventController;
    private FirebaseAnnouncementController fbAnnouncementController;

    //javadocs
    /**
     * This method creates the view for the ScannerFragment.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment
     * @param container The parent view that the fragment's UI should be attached to
     * @param savedInstanceState This fragment's previously saved state, if it is being reconstructed from a previous saved state
     * @return The view for the ScannerFragment
     */
    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        // Access the TextView
        textView = view.findViewById(R.id.scanner_text);
        scanButton = view.findViewById(R.id.scan_button);
        galleryButton = view.findViewById(R.id.gallery_button);

        qrScanner = new QRScanner(getContext());

        fbAttendanceController = new FirebaseAttendanceController();
        fbUserController = new FirebaseUserController();
        fbQrCodeController = new FirebaseQrCodeController();
        fbEventController = new FirebaseEventController();
        firebaseUserController = new FirebaseUserController();
        fbAnnouncementController = new FirebaseAnnouncementController();

        return view;
    }

    //javadocs
    /**
     * This method is called when the view is created.
     * @param view The view returned by onCreateView
     * @param savedInstanceState This fragment's previously saved state, if it is being reconstructed from a previous saved state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        final String[] returnedText = new String[1];
        textView.setText("Ready to rock and roll");

        // Example Usage: Get the Hashed Location from the Singleton
        /*         - Note: This usage returns NULL if device permissions are disabled
         *                  If permissions are enabled, returns hashed geolocation as a String
         */
        hashedUserLocation = SettingsDataSingleton.getInstance().getHashedGeoLocation();


        //idk why it needs to make an intent array instead of a single intent object
        //but this is how it works so I'm not going to question it
        galleryButton.setOnClickListener(v -> {
            // Check if the READ_EXTERNAL_STORAGE permission is already granted
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.d("YEET", "permission denied bench");
                // If not, request the permission
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
            } else {
                openGallery();
            }
        });

        scanButton.setOnClickListener(v -> {
            // Scan the QR code
            qrScanner.scanQRCode(qrCodeValue -> {

                AtomicBoolean isUserAdmin = new AtomicBoolean(false);
                String usedId = fbUserController.getCurrentUserUid();
                DocumentReference adminAuthRef = FirebaseFirestore.getInstance().collection("config").document("Admin Auth Code");

                adminAuthRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String adminAuthCode = documentSnapshot.getString("Code");
                        if (qrCodeValue.equals(adminAuthCode)) {
                            //get current user reference, and make the isAdmin field true in firebase
                            DocumentReference userRef = FirebaseFirestore.getInstance().collection("users").document(usedId);

                            userRef.update("admin", true).addOnSuccessListener(aVoid -> {
                                Log.d("testerrr", "User is now an admin");
                                isUserAdmin.set(true);


                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> getActivity().invalidateOptionsMenu()); // Invalidate the options menu
                                }

                                Toast.makeText(getContext(), "Admin Privileges Unlocked", Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                });


                //continue only if user is not an admin
                if (!isUserAdmin.get()) {

                    // Check if it's a check-in QR code
                    fbQrCodeController.getCheckInEventFromQr(qrCodeValue).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Event event = task.getResult();
                            if (event != null) {
                                // If it's a check-in QR code, try checking in
                                tryCheckIn(usedId, event);

                            } else {
                                // If it's not a check-in code, try to get it as a promo code
                                fbQrCodeController.getPromoEventFromQr(qrCodeValue).addOnCompleteListener(promoTask -> {
                                    if (promoTask.isSuccessful()) {
                                        Event promoEvent = promoTask.getResult();
                                        if (promoEvent != null) {
                                            // If it's a promo code, launch event details
                                            launchEventDetails(getContext(), promoEvent.getEventID());
                                        } else {
                                            // If it's neither a check-in nor a promo code, show error message
                                            Toast.makeText(getContext(), "Invalid QR code", Toast.LENGTH_LONG).show();
                                        }
                                    } else {
                                        // If there was an error getting promo event, show error message
                                        Toast.makeText(getContext(), "Error getting promo event", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }

                        }
                        else {
                            // If there was an error getting check-in event, show error message
                            Toast.makeText(getContext(), "Error getting check-in event", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        });

    }

    //javadocs
/**
     * This method attempts to check in a user to an event.
     * If the user is the organizer of the event, a message is displayed and the method returns.
     * If the check-in is successful, a message is displayed and the user is taken to the event details page.
     * If the check-in fails, a message is displayed.
     * @param attendee The ID of the user to check in
     * @param event The event to check in to
     * @return null
     */
    private Void tryCheckIn(String attendee, Event event) {
        if (Objects.equals(attendee, event.getOrganizerID())) {
            Log.e("testerrr", "You are the organizer of this event");
            Toast.makeText(getContext(), "You are the organizer of this event", Toast.LENGTH_LONG).show();
            return null;
        }

        fbAttendanceController.checkIn(attendee, event.getEventID()).addOnSuccessListener(checkedIn ->
        {
            Log.d("testerrr", "Checked in successfully!");
            Toast.makeText(getContext(), "Checked in successfully!", Toast.LENGTH_LONG).show();

            checkMilestones(event);

            launchEventDetails(getContext(), event.getEventID());


        }).addOnFailureListener(e ->
        {
            Toast.makeText(getContext(), "Failed to check in", Toast.LENGTH_LONG).show();
            Log.e("testerrr", "Failed to check in: " + e.getMessage());
        });
        return null;
    }

    //javadocs
    /**
     * This method checks if the event has reached a milestone in attendance.
     * If the event has reached a milestone, a message is displayed and an announcement is added to the event.
     * @param event The event to check for milestones
     */
    private void checkMilestones(Event event) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference eventsRef = db.collection("Events").document(event.getEventID()).collection("liveCounts").document("currentAttendance");

        eventsRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            // Get the field value
                            Object fieldValue = documentSnapshot.get("attendanceCount");
                            // Log the field value
                            Log.d("miless", "Field value: " + fieldValue);

                            //convert to int
                            int currentAttendance = Integer.parseInt(fieldValue.toString());

                            //setup milestones code

                            //int list of milestones: 1,5,10
                            int[] milestones = {1, 3, 5, 10, 20,30,40,50,60,70,80,90,100};

                            for (int milestone : milestones) {
                                if (currentAttendance == milestone) {
                                    //if the current attendance is equal to a milestone, give the user a badge
                                    //and show a toast message
                                    Log.d("miless", "Milestone reached: " + milestone);
                                    Toast.makeText(getContext(), "Milestone reached: " + milestone, Toast.LENGTH_LONG).show();

                                    String msg = "Congratulations, your event " + event.getName() + " has reached a milestone of " + milestone + " attendees!";
                                    //add announcement to the event
                                    Announcement ann_actual = new Announcement(msg, event.getName());
                                    ann_actual.setOrganizerID(event.getOrganizerID());
                                    ann_actual.setIsMilestone(true);
                                    ann_actual.setEventID(event.getEventID());
                                    fbAnnouncementController.addAnnouncement(event.getEventID(), ann_actual);

                                }
                            }


                        } else {
                            Log.d("miless", "No such document");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Firestore", "Error getting document", e);
                    }
                });


//        Log.d("miless", "Current attendance: " + currentAttendance);
    }

    //javadocs
    /**
     * This method launches the event details activity.
     * @param context The context of the current state of the application
     * @param eventId The ID of the event to view
     */
    private void launchEventDetails(Context context, String eventId) {
        // Create an Intent to start the event details activity
        Intent intent = new Intent(context, ViewEventActivity.class);

        // Put the event ID as an extra in the Intent
        intent.putExtra("eventID", eventId);

        // Start the activity with the Intent
        startActivity(intent);

    }

    //javadocs
    /**
     * This method opens the gallery to select an image.
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    //javadocs
    /**
     * This method is called when the user responds to a permission request.
     * If the permission is granted, the gallery is opened.
     * If the permission is denied, a message is displayed.
     * @param requestCode The request code passed to requestPermissions
     * @param permissions The requested permissions
     * @param grantResults The grant results for the corresponding permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            Toast.makeText(getActivity(), "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }


    //javadocs
    /**
     * This method is called when the fragment is destroyed.
     */
    // Scan image from the gallery when click the select from gallery button
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if the result comes from the gallery Intent
        if (requestCode == REQUEST_CODE_GALLERY && resultCode == Activity.RESULT_OK && data != null) {
            // Get the Uri of the selected image
            Uri imageUri = data.getData();

            try {
                // Read QR code from the selected image
                String qrCodeValue = QRScanner.readQRCodeFromUri(requireContext(), imageUri);
                if (qrCodeValue != null && !qrCodeValue.isEmpty()) {
                    // Check if it's a check-in QR code
                    fbQrCodeController.getCheckInEventFromQr(qrCodeValue).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Event checkInEvent = task.getResult();
                            if (checkInEvent != null) {
                                // If it's a check-in QR code, try checking in
                                tryCheckIn(fbUserController.getCurrentUserUid(), checkInEvent);
                            } else {
                                // If it's not a check-in QR code, try to get it as a promo code
                                fbQrCodeController.getPromoEventFromQr(qrCodeValue).addOnCompleteListener(promoTask -> {
                                    if (promoTask.isSuccessful()) {
                                        Event promoEvent = promoTask.getResult();
                                        if (promoEvent != null) {
                                            // If it's a promo code, launch event details
                                            launchEventDetails(getContext(), promoEvent.getEventID());
                                        } else {
                                            // If it's neither a check-in nor a promo code, show error message

//                                            Toast.makeText(getContext(), "Invalid QR code", Toast.LENGTH_LONG).show();
                                            Log.d("testerrr", "Invalid QR code");
                                        }
                                    } else {
                                        // If there was an error getting promo event, show error message
                                        Toast.makeText(getContext(), "Error getting promo event", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        } else {
                            // If there was an error getting check-in event, show error message
                            Toast.makeText(getContext(), "Error getting check-in event", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    // Handle the case when qrCodeValue is null or empty
                    Toast.makeText(getContext(), "Invalid QR code", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Error reading QR code", Toast.LENGTH_SHORT).show();
            }
        }
    }
}