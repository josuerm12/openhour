package com.openhour.backend.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController 
public class DefaultController {
    @GetMapping("/")
    public String home() {
        return "Welcome to the OpenHour API!";
    }
}
