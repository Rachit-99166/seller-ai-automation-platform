package com.sellerautomation.seller_ai_automation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.sellerautomation.seller_ai_automation.model.Product;

import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    private String callGroqAi(String prompt) {
    try {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Concise Body Creation
        Map<String, Object> body = Map.of(
            "model", model,
            "temperature", 0.5,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        // Execute Request
        Map response = restTemplate.postForObject(apiUrl, new HttpEntity<>(body, headers), Map.class);

        // Extract Response: choices[0] -> message -> content
        List<Map> choices = (List<Map>) response.get("choices");
        Map message = (Map) choices.get(0).get("message");
        return (String) message.get("content");

    } catch (Exception e) {
        return "{}";
    }
}

    // FEATURE 1: COMMAND INTERFACE PARSER
    public String parseUserCommand(String commandText) {
        String prompt = "You are an API Command Parser. Convert this user text: '" + commandText + "' " +
                "into a JSON object with keys: 'action', 'targetId', 'payload', 'numericValue'. " +
                "Valid Actions: " +
                "1. 'FETCH_ALL' (e.g. 'show items'). " +
                "2. 'DELETE_BY_ID' (e.g. 'delete item 123'). Set targetId. " +
                "3. 'CREATE_PRODUCT' (e.g. 'add blue shoes'). Set payload='blue shoes'. " +
                "4. 'UPDATE_STOCK' (e.g. 'set stock of 123 to 50'). Set targetId='123', numericValue=50. " +
                "Return ONLY JSON.";
        return callGroqAi(prompt);
    }

    // FEATURE 2: PRODUCT PARSING
    public String enrichProductData(String rawText) {
        String prompt = "Convert this raw product info: '" + rawText + "' " +
                "into a JSON object with keys: 'title', 'description', 'category'. " +
                "Return ONLY JSON.";
        return callGroqAi(prompt);
    }

    // FEATURE 3: INVENTORY RESTOCK EMAIL
    public String generateRestockEmail(String productName, int currentStock) {
        String prompt = "Write a short, professional email to a supplier ordering more '" + productName + "'. " +
                "Current stock is critically low at " + currentStock + " units. Request urgent delivery.";
        return callGroqAi(prompt);
    }

    // FEATURE 4: QUALITY SCORE
    // FEATURE 4: QA SCORE (UPDATED to check Price, Stock, & Title)
    public String auditProductQuality(Product p) {
        String prompt = String.format(
            "Audit this e-commerce listing:\n" +
            "- Title: %s\n" +
            "- Description: %s\n" +
            "- Price: $%.2f\n" +
            "- Stock: %d\n\n" +
            "Rules for scoring (1-10):\n" +
            "1. If Price is 0 or unrealistically low, Score < 5.\n" +
            "2. If Title is less than 3 words, deduct points.\n" +
            "3. If Description is short or vague, deduct points.\n" +
            "4. Provide a Score (X/10) and brief advice.",
            p.getTitle(), p.getDescription(), p.getPrice(), p.getStockQuantity()
        );

        // We ask for a very short response to fit in the table column
        return callGroqAi(prompt + " Keep advice under 15 words.");
    }

    // FEATURE 5: DYNAMIC PRICING
    public String suggestPrice(double cost, String category) {
        String prompt = "The product costs $" + cost + " to make and is in category '" + category + "'. " +
                "Suggest a profitable selling price ending in .99. Return ONLY the number (e.g. 29.99).";
        return callGroqAi(prompt);
    }

    // FEATURE 6: SALES INSIGHTS
    // FEATURE 6: REAL-TIME MARKET ANALYSIS (UPDATED)
    // We now pass the ACTUAL list of products, not a fake string
    public String analyzeMarketTrends(List<Product> products) {
        if (products.isEmpty()) {
            return "No products found. Add items to generate AI market insights.";
        }

        // Convert product list to a simple string for the AI to read
        StringBuilder inventoryData = new StringBuilder();
        for (Product p : products) {
            inventoryData.append(String.format("- Item: %s | Category: %s | Price: $%.2f | Stock: %d\n", 
                                   p.getTitle(), p.getCategory(), p.getPrice(), p.getStockQuantity()));
        }

        // The Smart Prompt
        String prompt = "Act as a Senior Market Analyst. I have a shop with this inventory:\n" +
                inventoryData.toString() +
                "\n\nTask:\n" +
                "1. Compare my prices and stock against CURRENT REAL WORLD market trends (e.g., popularity of iPhone 16 vs Leather Jackets).\n" +
                "2. Rank my products from #1 (Highest Potential) to lowest.\n" +
                "3. Explain WHY based on price competitiveness and stock levels (e.g., 'iPhone is priced low for market, high potential').\n" +
                "4. Output strictly a 3-bullet point summary and 1 Strategy Tip.";

        return callGroqAi(prompt);
    }
    // FEATURE: AI SELLER VERIFICATION
    public String verifySellerCompany(String companyName) {
        String prompt = String.format(
            "Act as a Corporate Risk Analyst. A seller claims to represent the company '%s'.\n" +
            "Perform a background check based on your knowledge:\n" +
            "1. Is this a real, registered company? (If fake/random name, Reject)\n" +
            "2. Is the company currently in severe bankruptcy or facing massive fraud scandals? (If yes, Reject)\n" +
            "3. Is the brand sentiment generally positive/neutral? (If yes, Approve)\n\n" +
            "Output Format (STRICTLY follow this):\n" +
            "If Safe: 'APPROVED'\n" +
            "If Unsafe/Fake: 'REJECTED: [Short Reason Why]'",
            companyName
        );

        return callGroqAi(prompt); // Returns "APPROVED" or "REJECTED: ..."
    }
    // FEATURE: BRAND CONSISTENCY CHECK
    public String verifyProductAlignment(String companyName, String productCommand) {
        String prompt = String.format(
            "Act as a Brand Manager for the company '%s'.\n" +
            "A user is trying to add a product via this command: '%s'.\n" +
            "Task: Verify if this product is something '%s' actually manufactures or sells.\n" +
            "Rules:\n" +
            "1. If '%s' makes this type of product (e.g., Nike adding Shoes), output 'ALLOWED'.\n" +
            "2. If it is a competitor's product or unrelated (e.g., Nike adding PS5 or Big Mac), output 'DENIED'.\n" +
            "Output Format: Just the word 'ALLOWED' or 'DENIED'.",
            companyName, productCommand, companyName, companyName
        );

        return callGroqAi(prompt).trim();
    }
    // FEATURE: DC COMMAND PARSER
    public String parseDcCommandToJson(String commandText) {
        String prompt = String.format(
            "Act as an API. Convert this command into a JSON Object for a Logistics DB.\n" +
            "Command: '%s'\n" +
            "Fields: name, address, closingTime, closingDays, carriers, items (Array of Strings).\n" +
            "Rules:\n" +
            "1. Infer missing details if needed (e.g. invent a name if none given).\n" +
            "2. Output STRICTLY raw JSON. No markdown.\n" +
            "Example: { \"name\": \"NY Hub\", \"carriers\": \"FedEx\", \"items\": [\"Ps5\"] }",
            commandText
        );
        return callGroqAi(prompt);
    }
    public String convertCommandToJson(String commandText) {
        String prompt = String.format(
            "Act as an API. Convert this command into a JSON Object for a Product database.\n" +
            "Command: '%s'\n" +
            "Fields required: title, description, category, price (double), stockQuantity (int).\n" +
            "Rules:\n" +
            "1. Extract meaningful details from the command.\n" +
            "2. Infer missing details (e.g., if no category, guess it).\n" +
            "3. Output STRICTLY raw JSON. No markdown, no explanations.\n" +
            "Example Output: { \"title\": \"Nike Air\", \"price\": 100.0, ... }",
            commandText
        );

        return callGroqAi(prompt);
    }
    // FEATURE: SUPPLY CHAIN ORCHESTRATOR
    // FEATURE: SUPPLY CHAIN ORCHESTRATOR (FINAL HYBRID VERSION)
    public String analyzeSupplyChainEvent(String event, String productData, String dcData) {
        String prompt = String.format(
            "Act as a Chief Logistics Officer. Analyze the event '%s' based on my real-time Supply Chain Data.\n" +
            "Products: %s\n" +
            "DCs: %s\n" +
            
            "CRITICAL LOGIC RULES (Follow these strictly):\n" +
            "1. CHECK LOCATION: Before suggesting a transfer, look if the product is ALREADY in the affected city's DC.\n" +
            "2. CHECK STOCK LEVELS: If local stock is > 500, explicitly report: '✅ Stock is sufficient locally.' Do NOT warn about stockouts or suggest transfers for that item.\n" +
            "3. OPPORTUNITIES: If stock is sufficient, suggest ways to maximize sales (e.g., 'Fast Shipping' badges) instead of panic transfers.\n" +
            
            "Output Task: Provide a structured HTML Summary (use <ul>, <li>, <strong>) with these 3 specific sections:\n" +
            "1. 🚨 Impact Analysis (Briefly explain the external event and demand surge)\n" +
            "2. 📦 Inventory Status & Logistics (Apply the Logic Rules here: State clearly if stock is sufficient or if transfers are actually needed)\n" +
            "3. 🏷️ Strategic Promotions (Suggest dynamic pricing, coupons, or bundles to capitalize on the event)\n" +
            "Keep it professional, data-driven, and concise.",
            event, productData, dcData
        );
        return callGroqAi(prompt);
    }
}
