package com.example.devoopsclass;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeployController {
    @GetMapping("/deploy")
    public String index() {
        return "Hello from Spring Boot application! RUnning on deploy controller";
    }
}