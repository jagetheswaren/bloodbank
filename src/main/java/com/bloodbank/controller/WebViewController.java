package com.bloodbank.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Web View Controller
 * Serves server-rendered HTML views for the integrated BloodBank user interface.
 * All clinical data and business operations are executed via the REST APIs.
 */
@Hidden
@Controller
public class WebViewController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/donors")
    public String donors() {
        return "donors";
    }

    @GetMapping("/donors/new")
    public String newDonor() {
        return "donor-form";
    }

    @GetMapping("/donors/{id}/view")
    public String donorDetails(@PathVariable("id") Long id, Model model) {
        model.addAttribute("donorId", id);
        return "donor-details";
    }

    @GetMapping("/donations")
    public String donations() {
        return "donations";
    }

    @GetMapping("/donations/new")
    public String newDonation() {
        return "donation-form";
    }

    @GetMapping("/inventory")
    public String inventory() {
        return "inventory";
    }

    @GetMapping("/inventory/near-expiry")
    public String nearExpiry() {
        return "near-expiry";
    }

    @GetMapping("/inventory/expired")
    public String expired() {
        return "expired";
    }

    @GetMapping("/issues")
    public String issues() {
        return "issues";
    }

    @GetMapping("/issues/new")
    public String newIssue() {
        return "issue-blood";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
