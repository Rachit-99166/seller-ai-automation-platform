package com.sellerautomation.seller_ai_automation.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerautomation.seller_ai_automation.model.Product;
import com.sellerautomation.seller_ai_automation.repository.ProductRepository;
import com.sellerautomation.seller_ai_automation.service.GroqService;

import jakarta.servlet.http.HttpSession;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
public class SellerController {

    @Autowired private GroqService groqService;
    @Autowired private ProductRepository productRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- DASHBOARD (Feature 1 & 6) ---
    @GetMapping("/")
    public String dashboard(HttpSession session, Model model) {
        
        // 1. SECURITY CHECK
        String sellerName = (String) session.getAttribute("sellerName");
        if (sellerName == null) return "redirect:/login";
        model.addAttribute("companyName", sellerName);

        // 2. EXISTING DATA
        List<Product> allProducts = productRepository.findAll();
        
        // --- NEW FEATURE: LOW STOCK SENTINEL ---
        // Find products with stock LESS THAN 10
        List<Product> lowStockItems = productRepository.findByStockQuantityLessThan(10);
        
        // Pass this list to the HTML
        model.addAttribute("lowStockItems", lowStockItems);
        // ---------------------------------------

        // (Your existing AI Insights Logic...)
        String insights;
        if (allProducts.isEmpty()) {
            insights = "Welcome " + sellerName + "! Add products to get started.";
        } else {
            try {
                // If we have low stock, AI should mention it!
                if (!lowStockItems.isEmpty()) {
                   insights = "⚠️ URGENT: " + lowStockItems.size() + " products are critical. Restock immediately to avoid revenue loss.";
                } else {
                   insights = groqService.analyzeMarketTrends(allProducts);
                }
            } catch (Exception e) {
                insights = "AI Analysis unavailable.";
            }
        }
        
        model.addAttribute("insights", insights);
        model.addAttribute("products", allProducts);
        return "index";
    }

    // --- FEATURE 1: COMMAND INTERFACE (FIXED) ---
    // ... inside SellerController class ...

    @PostMapping("/execute-command")
    public String executeCommand(@RequestParam String commandText, 
                                 HttpSession session, 
                                 RedirectAttributes redirectAttributes) {
        
        // 1. SECURITY: Check Login
        String companyName = (String) session.getAttribute("sellerName");
        if (companyName == null) return "redirect:/login";

        // 2. NEW FEATURE: "Delete All Products"
        // checks if user typed exactly "delete all products" (case insensitive)
        if (commandText.trim().equalsIgnoreCase("delete all products")) {
            productRepository.deleteAll(); // Wipes MongoDB
            redirectAttributes.addFlashAttribute("message", "💥 BOOM! All products deleted successfully.");
            return "redirect:/"; // Reloading the page updates the UI automatically
        }

        // 3. BRAND CHECK (Your existing AI Logic)
        if (commandText.toLowerCase().startsWith("add") || commandText.toLowerCase().startsWith("create")) {
             String verification = groqService.verifyProductAlignment(companyName, commandText);
             if (verification.contains("DENIED")) {
                 redirectAttributes.addFlashAttribute("error", "⛔ Access Denied: " + companyName + " does not manufacture this.");
                 return "redirect:/";
             }
        }

        // 4. Normal Command Execution
        try {
            if (commandText.toLowerCase().startsWith("add") || commandText.toLowerCase().startsWith("create")) {
                createProductFromCommand(commandText);
                redirectAttributes.addFlashAttribute("message", "✅ Product added for " + companyName);
            } 
            // (You can remove the old delete logic or keep it for specific deletions)
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/";
    }

    // --- HELPER: Create Product with 5-Char Custom ID ---
    // --- HELPER: Create Product & Auto-Audit ---
    private void createProductFromCommand(String commandText) throws Exception {
        
        // 1. Get JSON from AI
        String jsonResponse = groqService.convertCommandToJson(commandText);
        jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();
        
        // 2. Convert to Object
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        Product newProduct = mapper.readValue(jsonResponse, Product.class);
        
        // 3. Generate ID
        newProduct.setId(generateShortId());
        
        // --- THE FIX: Calculate Real Score Immediately ---
        // Instead of setting "Pending Audit...", we ask the AI right now.
        try {
            String realScore = groqService.auditProductQuality(newProduct);
            newProduct.setAiQualityScore(realScore);
        } catch (Exception e) {
            newProduct.setAiQualityScore("Audit Failed (Click Edit to retry)");
        }
        
        // 4. Save to MongoDB
        productRepository.save(newProduct);
    }

    // --- NEW HELPER: Generates "hf74n", "k92ms", etc. ---
    private String generateShortId() {
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        
        for (int i = 0; i < 5; i++) {
            result.append(characters.charAt(rnd.nextInt(characters.length())));
        }
        return result.toString();
    }

    // --- FEATURE 3: INVENTORY ---
    @GetMapping("/inventory")
    public String inventory(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "inventory";
    }

    @PostMapping("/generate-restock/{id}")
    public String generateRestock(@PathVariable String id, Model model) {
        Product p = productRepository.findById(id).orElseThrow();
        String emailDraft = groqService.generateRestockEmail(p.getTitle(), p.getStockQuantity());
        model.addAttribute("emailDraft", emailDraft);
        model.addAttribute("products", productRepository.findAll());
        return "inventory";
    }
    // ⚔️ FEATURE: SCAN FOR COMPETITORS
    // ⚔️ FEATURE: ADVANCED MARKET SCANNER
    @GetMapping("/scan-competitors")
    public String scanCompetitors(RedirectAttributes attributes) {
        List<Product> products = productRepository.findAll();
        int alertCount = 0;
        java.util.Random rand = new java.util.Random();

        for (Product p : products) {
            double currentPrice = p.getPrice();
            
            // RESET: Clear old alerts first
            p.setSuggestedPrice(null);
            p.setAlertMessage(null);
            p.setAlertType(null);

            // LOGIC 1: PROFIT PROTECTION (If Price is dangerously low)
            // Simulation: We assume anything under $20 is a "mistake" for high-ticket items
            if (currentPrice < 20.0) {
                // AI suggests a "Real Market Value" (e.g., $350)
                double marketValue = 350.00; 
                p.setSuggestedPrice(marketValue);
                p.setAlertMessage("📉 Price Risk: Too low! Market avg is $" + marketValue);
                p.setAlertType("PROFIT_RISK"); // Triggers Yellow Box
                alertCount++;
            }
            
            // LOGIC 2: COMPETITOR WAR (If Price is normal, but Rival is cheaper)
            // 30% chance of a rival appearing
            else if (rand.nextInt(100) < 30) { 
                double rivalPrice = Math.round((currentPrice - 5.0) * 100.0) / 100.0; // Rival is $5 cheaper
                
                if (rivalPrice > 0) {
                    p.setSuggestedPrice(rivalPrice - 0.01); // Undercut them by 1 cent
                    p.setAlertMessage("⚔️ Rival Alert: Competitor is at $" + rivalPrice);
                    p.setAlertType("COMPETITOR"); // Triggers Red Box
                    alertCount++;
                }
            }
            
            productRepository.save(p);
        }

        if (alertCount > 0) {
            attributes.addFlashAttribute("error", "⚠️ Market Scan Complete: " + alertCount + " pricing alerts found.");
        } else {
            attributes.addFlashAttribute("message", "✅ Market is stable. Your prices are optimal.");
        }
        
        return "redirect:/";
    }

    // ✅ ACCEPT SUGGESTION (Works for both Increase and Decrease)
    @GetMapping("/apply-price/{id}")
    public String applyPriceSuggestion(@PathVariable String id, RedirectAttributes attributes) {
        Product p = productRepository.findById(id).orElseThrow();
        
        if (p.getSuggestedPrice() != null) {
            double oldPrice = p.getPrice();
            p.setPrice(p.getSuggestedPrice());
            
            // Clear the alert after fixing
            p.setSuggestedPrice(null);
            p.setAlertMessage(null);
            p.setAlertType(null);
            productRepository.save(p);
            
            attributes.addFlashAttribute("message", "✅ Price Updated: $" + oldPrice + " ➝ $" + p.getPrice());
        }
        return "redirect:/";
    }

    // ❌ REJECT/CANCEL SUGGESTION
    @GetMapping("/dismiss-alert/{id}")
    public String dismissPriceAlert(@PathVariable String id, RedirectAttributes attributes) {
        Product p = productRepository.findById(id).orElseThrow();
        
        // Just clear the fields without changing price
        p.setSuggestedPrice(null);
        p.setAlertMessage(null);
        p.setAlertType(null);
        productRepository.save(p);
        
        attributes.addFlashAttribute("message", "🚫 Suggestion ignored. Price remains $" + p.getPrice());
        return "redirect:/";
    }

    // --- FEATURE 5: DYNAMIC PRICING ---
    @GetMapping("/pricing/{id}")
    public String pricingPage(@PathVariable String id, Model model) {
        Product p = productRepository.findById(id).orElseThrow();
        // Use default cost of 10.0 if not set, to ensure AI has data
        String suggestedPrice = groqService.suggestPrice(p.getCostPrice() > 0 ? p.getCostPrice() : 10.0, p.getCategory());
        
        model.addAttribute("product", p);
        model.addAttribute("suggestion", suggestedPrice);
        return "pricing"; // Returns a simple view
    }

    @PostMapping("/update-price")
    public String updatePrice(@RequestParam String id, @RequestParam double newPrice) {
        Product p = productRepository.findById(id).orElseThrow();
        p.setPrice(newPrice);
        productRepository.save(p);
        return "redirect:/";
    }

    // --- NEW FEATURE: MANUAL MODIFY PAGE ---
    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable String id, Model model) {
        Product p = productRepository.findById(id).orElseThrow();
        model.addAttribute("product", p);
        return "edit-product";
    }

    @PostMapping("/update-product")
    public String updateProduct(@ModelAttribute Product product) {
        
        // 1. Load the REAL data from Database (This has Price $880)
        Product existing = productRepository.findById(product.getId()).orElseThrow();
        
        // 2. Update ONLY the text fields from the form
        existing.setTitle(product.getTitle());
        existing.setDescription(product.getDescription());
        existing.setCategory(product.getCategory());
        
        // --- THE FIX IS HERE ---
        // DO NOT use 'product'. USE 'existing'.
        String newScore = groqService.auditProductQuality(existing); 
        
        existing.setAiQualityScore(newScore);
        
        productRepository.save(existing);
        return "redirect:/";
    }

    // --- NEW FEATURE: MANUAL STOCK PAGE ---
    @GetMapping("/stock/{id}")
    public String stockPage(@PathVariable String id, Model model) {
        Product p = productRepository.findById(id).orElseThrow();
        model.addAttribute("product", p);
        return "adjust-stock";
    }

    @PostMapping("/update-stock-manual")
    public String updateStockManual(@RequestParam String id, @RequestParam int newStock) {
        Product p = productRepository.findById(id).orElseThrow();
        p.setStockQuantity(newStock);
        productRepository.save(p);
        return "redirect:/";
    }
}
