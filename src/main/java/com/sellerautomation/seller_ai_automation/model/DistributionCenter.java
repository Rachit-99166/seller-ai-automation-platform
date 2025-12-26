package com.sellerautomation.seller_ai_automation.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;

import java.util.List;

@Document(collection = "distribution_centers")
@Data
@AllArgsConstructor
public class DistributionCenter {
    @Id
    private String id;
    private String name;           
    private String address;        
    private String closingTime;    
    
    // --- THE FIX: Change these to Lists ---
    private List<String> closingDays;    // Was String, now List
    private List<String> carriers;       // Was String, now List
    private List<String> items;    

    public DistributionCenter() {}
}
