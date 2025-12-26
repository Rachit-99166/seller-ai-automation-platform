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
    private double costPrice; 
    private int stockQuantity; 
    private String aiQualityScore; 
    private Double suggestedPrice;   
    private String alertMessage;    
    private String alertType;
    private String distributionCenter;
}