package com.example.devoopsclass;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeployController {
    @GetMapping(value = "/deploy", produces = "text/html")
    public String index() {
        return SitePage.render("Deployment | Space Hyper", "/deploy", """
                <section class="content-card">
                    <p class="eyebrow">Deployment</p>
                    <h1>From source code to EC2.</h1>
                    <p>Jenkins builds the Java application, runs its tests, packages a Docker image, and deploys the successful build to AWS EC2.</p>
                    <span class="status-pill">Java application is running on AWS EC2</span>
                </section>
                """, "");
    }
}