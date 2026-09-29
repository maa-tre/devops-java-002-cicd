package com.example.devoopsclass;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BinayaController {
    @GetMapping("/binaya")
    public String index() {
        return "this is new route added for demo";
    }
}