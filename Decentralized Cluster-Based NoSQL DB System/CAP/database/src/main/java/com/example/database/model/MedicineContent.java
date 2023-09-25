package com.example.database.model;

public class MedicineContent {
    private String id;
    private String name;
    private String expirationDate;

    public MedicineContent(String id, String name, String expirationDate) {
        this.id = id;
        this.name = name;
        this.expirationDate = expirationDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }
}

