package com.example.quickscanner.model;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;


public class Event implements Parcelable {
    public String name;
    public String description;
    public String imagePath;
    public User organizer;

    public String time;
    public String location;
    public String eventID;
    public String organizerID;


    public ArrayList<String> signUps = new ArrayList<>();
    public ArrayList<String> checkIns = new ArrayList<>();

    private boolean isGeolocationEnabled;



    public int takenSpots ;
    public Integer maxSpots;


    public Event(String name, String description, User organizer) {
        this.name = name;
        this.description = description;
        this.organizer = organizer;
        imagePath = "default.jpeg";
        this.takenSpots  = 0;
    }

    public Event(String name, String description, User organizer, String time, String location) {
        this.name = name;
        this.description = description;
        imagePath = "default.jpeg";
        this.organizer = organizer;
        this.time = time;
        this.location = location;
        this.takenSpots = 0;
    }

    public Event() {
        this.takenSpots = 0;
    }

    // Parcelable implementation
    protected Event(Parcel in) {
        name = in.readString();
        description = in.readString();
        imagePath = in.readString();
    }

    public static final Creator<Event> CREATOR = new Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(imagePath);
    }

    /**
     * toggles the isGeolocationEnabled value
     * false -> true, true -> false
     */
    public void toggleIsGeolocationEnabled() {
        this.isGeolocationEnabled = !this.isGeolocationEnabled;
    }


    // Getters
    public String getName() {return name;}
    public String getDescription() {return description;}
    public String getImagePath() {return imagePath;}
    public User getOrganizer() {return organizer;}
    public String getTime() {return time;}
    public String getLocation() {return location;}
    public String getEventID() {return eventID;}
    public String getOrganizerID() {return organizerID;}

    public boolean getIsGeolocationEnabled() {return isGeolocationEnabled; }

    public ArrayList<String> getSignUps() {return signUps;}

    public ArrayList<String> getCheckIns() {return checkIns;}

    public Integer getMaxSpots() {return maxSpots;}
    public int getTakenSpots() {return takenSpots;}


    // Setters
    public void setName(String name) {this.name = name;}
    public void setDescription(String description) {this.description = description;}
    public void setImagePath(String imagePath) {this.imagePath = imagePath;}
    public void setOrganizer(User organizer) {this.organizer = organizer;}
    public void setTime(String time) {this.time = time;}
    public void setLocation(String location) {this.location = location;}
    public void setEventID(String eventID) {this.eventID = eventID;}
    public void setOrganizerID(String organizerID) {this.organizerID = organizerID;}

    public void setGeolocationEnabled(boolean geolocationEnabled) {this.isGeolocationEnabled = geolocationEnabled;}

    public void setSignUps(ArrayList<String> signUps) {this.signUps = signUps;}
    public void setCheckIns(ArrayList<String> checkIns) {this.checkIns = checkIns;}
    public void setMaxSpots(Integer maxSpots) {this.maxSpots = maxSpots;}
    public void setTakenSpots(int takenSpots) {this.takenSpots = takenSpots;}

}
