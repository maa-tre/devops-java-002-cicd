package com.example.devoopsclass;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AboutController {
    @GetMapping("/about")
    public String index() {
        return "this is new route added for about page";
    }
}