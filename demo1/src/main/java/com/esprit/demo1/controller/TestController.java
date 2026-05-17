package com.esprit.demo1.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class TestController {


    @GetMapping("/hello")
    public String test() {
        return "hello from demo 1";
    }
}
