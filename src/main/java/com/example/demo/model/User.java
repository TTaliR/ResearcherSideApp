package com.example.demo.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class User {
    private final SimpleIntegerProperty userID;
    private final SimpleStringProperty fName;
    private final SimpleStringProperty lName;

    public User(int userID, String fName, String lName) {
        this.userID = new SimpleIntegerProperty(userID);
        this.fName = new SimpleStringProperty(fName);
        this.lName = new SimpleStringProperty(lName);
    }

    public int getUserID() {
        return userID.get();
    }

    public SimpleIntegerProperty userIDProperty() {
        return userID;
    }

    public String getFName() {
        return fName.get();
    }

    public SimpleStringProperty fNameProperty() {
        return fName;
    }

    public String getLName() {
        return lName.get();
    }

    public SimpleStringProperty lNameProperty() {
        return lName;
    }
}
