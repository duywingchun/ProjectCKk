package com.example.projectck.models;

public class Food {

    private String id;
    private String name;
    private String description;
    private double price;
    private String imageUrl;

    public Food() {
    }

    public Food(String id,
                String name,
                String description,
                double price,
                String imageUrl) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
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

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}