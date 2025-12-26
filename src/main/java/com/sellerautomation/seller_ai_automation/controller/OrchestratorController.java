package com.sellerautomation.seller_ai_automation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sellerautomation.seller_ai_automation.model.DistributionCenter;
import com.sellerautomation.seller_ai_automation.model.Product;
import com.sellerautomation.seller_ai_automation.repository.DistributionCenterRepository;
import com.sellerautomation.seller_ai_automation.repository.ProductRepository;
import com.sellerautomation.seller_ai_automation.service.GroqService;

import java.util.List;

@Controller
public class OrchestratorController {

    @Autowired private ProductRepository productRepository;
    @Autowired private DistributionCenterRepository dcRepository;
    @Autowired private GroqService groqService;

    // 1. Show the Orchestrator Dashboard
    @GetMapping("/supply-chain-orchestrator")
    public String showOrchestrator() {
        return "orchestrator";
    }

    // 2. Run the Simulation
    @PostMapping("/simulate-scenario")
    public String simulateScenario(@RequestParam String scenario, Model model) {
        // Fetch ALL data to give AI full context
        List<Product> products = productRepository.findAll();
        List<DistributionCenter> dcs = dcRepository.findAll();

        // Convert lists to String summaries for the AI
        String prodString = products.stream().map(p -> p.getTitle() + " (Stock: " + p.getStockQuantity() + ")").toList().toString();
        String dcString = dcs.stream().map(d -> d.getName() + " (Items: " + d.getItems() + ")").toList().toString();

        // Ask AI for the Strategy
        String analysis = groqService.analyzeSupplyChainEvent(scenario, prodString, dcString);

        // Convert Markdown formatting to HTML if necessary (Simple cleanup)
        analysis = analysis.replace("```html", "").replace("```", "");

        model.addAttribute("scenario", scenario);
        model.addAttribute("strategy", analysis);
        
        return "orchestrator";
    }
}
