package com.sellerautomation.seller_ai_automation.model;

import lombok.Data;

@Data
public class AiCommand {
    // Actions: FETCH_ALL, DELETE_BY_ID, CREATE_PRODUCT, UPDATE_STOCK
    private String action;
    private String targetId;
    private String payload; 
    
    // CHANGED FROM 'int' TO 'Integer' TO ALLOW NULLS
    private Integer numericValue; 
}