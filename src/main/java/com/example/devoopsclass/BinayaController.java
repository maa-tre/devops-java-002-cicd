package com.example.devoopsclass;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BinayaController {
    @GetMapping(value = "/binaya", produces = "text/html")
    public String index() {
        return SitePage.render("Binaya | Space Hyper", "/binaya", """
                <section class="content-card">
                    <p class="eyebrow">Demo route</p>
                    <h1>Binaya's project page.</h1>
                    <p>This route is part of the original demo and remains available as the project grows.</p>
                    <span class="status-pill">Spring Boot route is active</span>
                </section>
                """, "");
    }
}