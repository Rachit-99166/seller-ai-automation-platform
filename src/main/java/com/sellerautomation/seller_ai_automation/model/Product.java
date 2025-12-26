package com.sellerautomation.seller_ai_automation.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String title;
    private String description;
    private String category;
    private double price;
    private double costPrice; // For Dynamic Pricing feature
    private int stockQuantity; // For Inventory feature
    private String aiQualityScore; // Feature: QA Score
    private Double suggestedPrice;   // The specific amount (e.g., 349.99)
private String alertMessage;     // "Rival undercut you" OR "Price too low"
private String alertType;
private String distributionCenter;
}