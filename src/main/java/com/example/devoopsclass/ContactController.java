package com.example.devoopsclass;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContactController {
    @GetMapping(value = "/contact", produces = "text/html")
    public String index() {
        return SitePage.render("Contact | Space Hyper", "/contact", """
                <section class="content-card">
                    <p class="eyebrow">Contact</p>
                    <h1>Thanks for stopping by.</h1>
                    <p>This demo does not have public contact details configured. The page itself confirms the Java backend is responding.</p>
                    <span class="status-pill">Backend is online</span>
                </section>
                """, "");
    }
}