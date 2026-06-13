package com.esprit.deliverymanagement.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {


    @GetMapping("/hello")
    public String test() {
        return "hello from delivery-management";
    }
}
