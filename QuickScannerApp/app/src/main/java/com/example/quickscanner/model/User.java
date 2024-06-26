package com.example.quickscanner.model;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Represents a User with a profile, admin status, and lists of events they're organizing or attending.
 *  The JavaDoc comments in this code were generated with the assistance of GitHub Copilot.
 *
 */
public class User {
    private Profile userProfile;
    private Boolean isAdmin;
    private ArrayList<String> organizedEvents = new ArrayList<String>();
    private ArrayList<String> signedUpEvents = new ArrayList<String>();
    private ArrayList<String> checkedInEvents = new ArrayList<String>();
    private String Uid;
    private boolean isGeolocationEnabled;

    private boolean selected;

    /**
     * Creates a User with a specified profile.
     *
     * @param name     The user's name.
     * @param email    The user's email.
     * @param website  The user's website.
     * @param imageUrl The user's profile image URL.
     */
    public User(String name, String email, String website, String imageUrl) {
        userProfile = new Profile(name, email, website, imageUrl);
        isAdmin = false;
        organizedEvents = new ArrayList<String>();
        signedUpEvents = new ArrayList<String>();
        checkedInEvents = new ArrayList<String>();
    }

    /**
     * Creates a User with a default profile.
     */
    public User() {
        userProfile = new Profile();
        isAdmin = false;
        organizedEvents = new ArrayList<String>();
        signedUpEvents = new ArrayList<String>();
        checkedInEvents = new ArrayList<String>();
    }

    /**
     * Sets the user as an admin.
     */
    public void setAdmin() {
        isAdmin = true;
    }

    /**
     * Returns the user's profile.
     *
     * @return The user's profile.
     */
    public Profile getUserProfile() {
        return userProfile;
    }

    /**
     * Sets the user's profile.
     *
     * @param userProfile The new profile.
     */
    public void setUserProfile(Profile userProfile) {
        this.userProfile = userProfile;
    }

    /**
     * Returns whether the user is an admin.
     *
     * @return True if the user is an admin, false otherwise.
     */
    public Boolean getAdmin() {
        return isAdmin;
    }

    /**
     * gets the isGeolocationEnabled value
     *
     * @return isGeolocationEnabled The toggle value
     */
    public boolean getIsGeolocationEnabled() {
        return isGeolocationEnabled;
    }

    /**
     * toggles the isGeolocationEnabled value
     * false -> true, true -> false
     */
    public void toggleAllowsGeolocation() {
        this.isGeolocationEnabled = !this.isGeolocationEnabled;
    }

    /**
     * Sets the user's admin status.
     *
     * @param admin The new admin status.
     */
    public void setAdmin(Boolean admin) {
        isAdmin = admin;
    }

    /**
     * Returns the events the user is organizing.
     *
     * @return A list of events the user is organizing.
     */
    public ArrayList<String> getOrganizedEvents() {
        return organizedEvents;
    }

    /**
     * Sets the events the user is organizing.
     *
     * @param organizedEvents The new list of events.
     */
    public void setOrganizedEvents(ArrayList<String> organizedEvents) {
        this.organizedEvents = organizedEvents;
    }


    public ArrayList<String> getSignedUpEvents() {
        return signedUpEvents;
    }

    public void setSignedUpEvents(ArrayList<String> signedUpEvents) {
        this.signedUpEvents = signedUpEvents;
    }

    public ArrayList<String> getCheckedInEvents() {
        return checkedInEvents;
    }

    public void setCheckedInEvents(ArrayList<String> checkedInEvents) {
        this.checkedInEvents = checkedInEvents;
    }


    /**
     * Sets the isGeolocationEnabled value
     *
     * @param geolocationEnabled The new toggle value.
     */
    public void setGeolocationEnabled(boolean geolocationEnabled) {
        this.isGeolocationEnabled = geolocationEnabled;
    }

    /**
     * Returns the user's unique ID.
     *
     * @return The user's unique ID.
     */
    public String getUid() {
        return Uid;
    }

    /**
     * Sets the user's unique ID.
     *
     * @param uid The new unique ID.
     */
    public void setUid(String uid) {
        Uid = uid;
    }

    public int getOrganizedEventsSize() {
        return organizedEvents.size();
    }

    public boolean isSelected() { return selected; }

    public void setSelected(boolean selected) { this.selected = selected; }
    //needed to allow the listener to notice that things have been removed, to tell they are the same
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(Uid, user.Uid);
    }


}
