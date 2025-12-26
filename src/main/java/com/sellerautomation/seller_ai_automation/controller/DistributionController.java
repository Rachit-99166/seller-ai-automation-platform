package com.sellerautomation.seller_ai_automation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerautomation.seller_ai_automation.model.DistributionCenter;
import com.sellerautomation.seller_ai_automation.model.Product;
import com.sellerautomation.seller_ai_automation.repository.DistributionCenterRepository;
import com.sellerautomation.seller_ai_automation.repository.ProductRepository;
import com.sellerautomation.seller_ai_automation.service.GroqService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DistributionController {

    @Autowired private DistributionCenterRepository dcRepository;
    @Autowired private GroqService groqService;
    @Autowired private ProductRepository productRepository;

    // 1. SHOW PAGE (Fixed Search Logic)
    @GetMapping("/distribution-centers")
    public String showDcManager(@RequestParam(required = false) String query, Model model) {
        List<DistributionCenter> allDcs = dcRepository.findAll();

        if (query != null && !query.isEmpty()) {
            String q = query.toLowerCase();
            allDcs = allDcs.stream()
                .filter(dc -> 
                    (dc.getName() != null && dc.getName().toLowerCase().contains(q)) || 
                    // FIX: Added .toString() before .toLowerCase()
                    (dc.getCarriers() != null && dc.getCarriers().toString().toLowerCase().contains(q)) ||
                    (dc.getItems() != null && dc.getItems().toString().toLowerCase().contains(q))
                ).collect(Collectors.toList());
        }

        model.addAttribute("dcs", allDcs);
        return "dc-manager";
    }

    // 2. CREATE DC (Robust Version with Error Messages)
    @PostMapping("/create-dc")
    public String createDc(@RequestParam String commandText, RedirectAttributes redirectAttributes) {
        try {
            // 1. AI Parsing (Existing Code)
            String json = groqService.parseDcCommandToJson(commandText);
            int firstBrace = json.indexOf("{");
            int lastBrace = json.lastIndexOf("}");
            if (firstBrace != -1 && lastBrace != -1) {
                json = json.substring(firstBrace, lastBrace + 1);
            }
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            DistributionCenter newDc = mapper.readValue(json, DistributionCenter.class);

            // 2. Save the DC
            dcRepository.save(newDc);

            // 3. --- NEW FEATURE: AUTO-LINK PRODUCTS ---
            List<Product> allProducts = productRepository.findAll();
            int linkedCount = 0;

            if (newDc.getItems() != null) {
                for (String dcItem : newDc.getItems()) {
                    for (Product p : allProducts) {
                        // Check if Product Title contains the DC Item name (e.g. "PS5 Slim" contains "PS5")
                        if (p.getTitle().toLowerCase().contains(dcItem.toLowerCase())) {
                            p.setDistributionCenter(newDc.getName());
                            productRepository.save(p);
                            linkedCount++;
                        }
                    }
                }
            }
            
            redirectAttributes.addFlashAttribute("message", "✅ DC Created & " + linkedCount + " products automatically linked!");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "❌ Error: " + e.getMessage());
        }
        return "redirect:/distribution-centers";
    }

    // 3. DELETE DC
    @GetMapping("/delete-dc/{id}")
    public String deleteDc(@PathVariable String id) {
        dcRepository.deleteById(id);
        return "redirect:/distribution-centers";
    }
    // 4. SHOW EDIT FORM
    @GetMapping("/edit-dc/{id}")
    public String showEditForm(@PathVariable String id, Model model) {
        DistributionCenter dc = dcRepository.findById(id).orElse(null);
        model.addAttribute("dc", dc);
        return "edit-dc"; 
    }

    // 5. PROCESS UPDATE (Handles Lists correctly)
    @PostMapping("/update-dc")
    public String updateDc(@RequestParam String id,
                           @RequestParam String name,
                           @RequestParam String address,
                           @RequestParam String closingTime,
                           @RequestParam String closingDaysStr, // Input as "Mon, Tue"
                           @RequestParam String carriersStr,    // Input as "FedEx, UPS"
                           @RequestParam String itemsStr) {     // Input as "PS5, Shoes"

        DistributionCenter dc = dcRepository.findById(id).orElse(new DistributionCenter());
        
        // Update simple fields
        dc.setId(id);
        dc.setName(name);
        dc.setAddress(address);
        dc.setClosingTime(closingTime);

        // Convert Comma-Separated Strings back to Lists
        // The regex "\\s*,\\s*" handles splits even if user adds spaces like "A, B"
        dc.setClosingDays(java.util.Arrays.asList(closingDaysStr.split("\\s*,\\s*")));
        dc.setCarriers(java.util.Arrays.asList(carriersStr.split("\\s*,\\s*")));
        dc.setItems(java.util.Arrays.asList(itemsStr.split("\\s*,\\s*")));

        dcRepository.save(dc);
        return "redirect:/distribution-centers";
    }
}
