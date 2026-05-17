package com.esprit.demo2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class TestController {


    @GetMapping("/hello")
    public String test() {
        return "hello from demo 2";
    }
}
