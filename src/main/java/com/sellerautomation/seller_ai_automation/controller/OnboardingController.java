package com.sellerautomation.seller_ai_automation.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sellerautomation.seller_ai_automation.service.GroqService;

@Controller
public class OnboardingController {

    @Autowired
    private GroqService groqService;

    @GetMapping("/login")
    public String showLoginPage() {
        return "login"; 
    }

    @PostMapping("/verify-seller")
    public String verifySeller(@RequestParam String companyName, 
                               HttpSession session, 
                               Model model) {
        
        String aiVerdict = groqService.verifySellerCompany(companyName);

        if (aiVerdict.contains("APPROVED")) {
            session.setAttribute("sellerName", companyName);
            return "redirect:/"; 
        } else {
            String reason = aiVerdict.replace("REJECTED:", "").trim();
            model.addAttribute("error", "AI Audit Failed: " + reason);
            return "login"; 
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
