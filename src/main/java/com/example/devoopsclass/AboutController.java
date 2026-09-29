package com.example.devoopsclass;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AboutController {
    @GetMapping(value = "/about", produces = "text/html")
    public String index() {
        return SitePage.render("About | Space Hyper", "/about", """
                <section class="content-card">
                    <p class="eyebrow">About this project</p>
                    <h1>A small app with a real delivery pipeline.</h1>
                    <p>Space Hyper is a Spring Boot demo packaged with Docker and deployed to AWS EC2 through a Jenkins CI/CD pipeline.</p>
                    <p>It brings the application and its deployment workflow together in one place.</p>
                </section>
                """, "");
    }
}