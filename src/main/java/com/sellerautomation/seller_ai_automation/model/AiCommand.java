package com.sellerautomation.seller_ai_automation.model;

import lombok.Data;

@Data
public class AiCommand {
    private String action;
    private String targetId;
    private String payload; 
    
    private Integer numericValue; 
}