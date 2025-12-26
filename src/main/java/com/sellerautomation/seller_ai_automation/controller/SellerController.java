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

    @GetMapping("/")
    public String dashboard(HttpSession session, Model model) {
        
        String sellerName = (String) session.getAttribute("sellerName");
        if (sellerName == null) return "redirect:/login";
        model.addAttribute("companyName", sellerName);

        List<Product> allProducts = productRepository.findAll();
        
        List<Product> lowStockItems = productRepository.findByStockQuantityLessThan(10);
        
        model.addAttribute("lowStockItems", lowStockItems);
  
        String insights;
        if (allProducts.isEmpty()) {
            insights = "Welcome " + sellerName + "! Add products to get started.";
        } else {
            try {
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


    @PostMapping("/execute-command")
    public String executeCommand(@RequestParam String commandText, 
                                 HttpSession session, 
                                 RedirectAttributes redirectAttributes) {
        
        String companyName = (String) session.getAttribute("sellerName");
        if (companyName == null) return "redirect:/login";

        if (commandText.trim().equalsIgnoreCase("delete all products")) {
            productRepository.deleteAll(); 
            redirectAttributes.addFlashAttribute("message", "💥 BOOM! All products deleted successfully.");
            return "redirect:/";
        }

        if (commandText.toLowerCase().startsWith("add") || commandText.toLowerCase().startsWith("create")) {
             String verification = groqService.verifyProductAlignment(companyName, commandText);
             if (verification.contains("DENIED")) {
                 redirectAttributes.addFlashAttribute("error", "⛔ Access Denied: " + companyName + " does not manufacture this.");
                 return "redirect:/";
             }
        }

        try {
            if (commandText.toLowerCase().startsWith("add") || commandText.toLowerCase().startsWith("create")) {
                createProductFromCommand(commandText);
                redirectAttributes.addFlashAttribute("message", "✅ Product added for " + companyName);
            } 
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/";
    }

    private void createProductFromCommand(String commandText) throws Exception {
        
        String jsonResponse = groqService.convertCommandToJson(commandText);
        jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        Product newProduct = mapper.readValue(jsonResponse, Product.class);
        
        newProduct.setId(generateShortId());
        
        try {
            String realScore = groqService.auditProductQuality(newProduct);
            newProduct.setAiQualityScore(realScore);
        } catch (Exception e) {
            newProduct.setAiQualityScore("Audit Failed (Click Edit to retry)");
        }
        
        productRepository.save(newProduct);
    }

    private String generateShortId() {
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        
        for (int i = 0; i < 5; i++) {
            result.append(characters.charAt(rnd.nextInt(characters.length())));
        }
        return result.toString();
    }

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

    @GetMapping("/scan-competitors")
    public String scanCompetitors(RedirectAttributes attributes) {
        List<Product> products = productRepository.findAll();
        int alertCount = 0;
        java.util.Random rand = new java.util.Random();

        for (Product p : products) {
            double currentPrice = p.getPrice();
            
            p.setSuggestedPrice(null);
            p.setAlertMessage(null);
            p.setAlertType(null);

            if (currentPrice < 20.0) {
                double marketValue = 350.00; 
                p.setSuggestedPrice(marketValue);
                p.setAlertMessage("📉 Price Risk: Too low! Market avg is $" + marketValue);
                p.setAlertType("PROFIT_RISK"); 
                alertCount++;
            }
           
            else if (rand.nextInt(100) < 30) { 
                double rivalPrice = Math.round((currentPrice - 5.0) * 100.0) / 100.0; 
                
                if (rivalPrice > 0) {
                    p.setSuggestedPrice(rivalPrice - 0.01); 
                    p.setAlertMessage("⚔️ Rival Alert: Competitor is at $" + rivalPrice);
                    p.setAlertType("COMPETITOR"); 
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

    @GetMapping("/apply-price/{id}")
    public String applyPriceSuggestion(@PathVariable String id, RedirectAttributes attributes) {
        Product p = productRepository.findById(id).orElseThrow();
        
        if (p.getSuggestedPrice() != null) {
            double oldPrice = p.getPrice();
            p.setPrice(p.getSuggestedPrice());
            
            p.setSuggestedPrice(null);
            p.setAlertMessage(null);
            p.setAlertType(null);
            productRepository.save(p);
            
            attributes.addFlashAttribute("message", "✅ Price Updated: $" + oldPrice + " ➝ $" + p.getPrice());
        }
        return "redirect:/";
    }

    @GetMapping("/dismiss-alert/{id}")
    public String dismissPriceAlert(@PathVariable String id, RedirectAttributes attributes) {
        Product p = productRepository.findById(id).orElseThrow();
        
        p.setSuggestedPrice(null);
        p.setAlertMessage(null);
        p.setAlertType(null);
        productRepository.save(p);
        
        attributes.addFlashAttribute("message", "🚫 Suggestion ignored. Price remains $" + p.getPrice());
        return "redirect:/";
    }

    @GetMapping("/pricing/{id}")
    public String pricingPage(@PathVariable String id, Model model) {
        Product p = productRepository.findById(id).orElseThrow();
        String suggestedPrice = groqService.suggestPrice(p.getCostPrice() > 0 ? p.getCostPrice() : 10.0, p.getCategory());
        
        model.addAttribute("product", p);
        model.addAttribute("suggestion", suggestedPrice);
        return "pricing"; 
    }

    @PostMapping("/update-price")
    public String updatePrice(@RequestParam String id, @RequestParam double newPrice) {
        Product p = productRepository.findById(id).orElseThrow();
        p.setPrice(newPrice);
        productRepository.save(p);
        return "redirect:/";
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable String id, Model model) {
        Product p = productRepository.findById(id).orElseThrow();
        model.addAttribute("product", p);
        return "edit-product";
    }

    @PostMapping("/update-product")
    public String updateProduct(@ModelAttribute Product product) {
        
        Product existing = productRepository.findById(product.getId()).orElseThrow();
        
        existing.setTitle(product.getTitle());
        existing.setDescription(product.getDescription());
        existing.setCategory(product.getCategory());
        
        String newScore = groqService.auditProductQuality(existing); 
        
        existing.setAiQualityScore(newScore);
        
        productRepository.save(existing);
        return "redirect:/";
    }

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
