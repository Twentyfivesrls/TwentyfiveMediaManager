package com.example.twentyfivemediamanager.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class ControllerTest {
    @GetMapping("/prova")
    public String test() {

        System.out.println("nel testing");
        return "CIAO";
    }
}
