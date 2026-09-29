package com.example.devoopsclass;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping(value = "/", produces = "text/html")
    public String index() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Space Hyper CI/CD Demo</title>
                    <style>
                        body {
                            align-items: center;
                            background: linear-gradient(135deg, #101827, #243b65);
                            color: #f4f7ff;
                            display: flex;
                            font-family: system-ui, sans-serif;
                            justify-content: center;
                            margin: 0;
                            min-height: 100vh;
                            text-align: center;
                        }
                        main {
                            padding: 2rem;
                        }
                        h1 {
                            font-size: clamp(2rem, 7vw, 4rem);
                            margin-bottom: 0.75rem;
                        }
                        p {
                            color: #c4d2ed;
                            font-size: 1.2rem;
                        }
                        .badge {
                            border: 1px solid #68e0b0;
                            border-radius: 999px;
                            color: #68e0b0;
                            display: inline-block;
                            margin-top: 1rem;
                            padding: 0.5rem 1rem;
                        }
                        nav {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 0.75rem;
                            justify-content: center;
                            margin-top: 2rem;
                        }
                        nav a {
                            border: 1px solid rgba(196, 210, 237, 0.3);
                            border-radius: 999px;
                            color: #c4d2ed;
                            padding: 0.6rem 1rem;
                            text-decoration: none;
                            transition: background-color 160ms ease, border-color 160ms ease, color 160ms ease;
                        }
                        nav a:hover,
                        nav a:focus-visible {
                            background: rgba(104, 224, 176, 0.12);
                            border-color: #68e0b0;
                            color: #68e0b0;
                        }
                        @media (prefers-reduced-motion: reduce) {
                            nav a {
                                transition: none;
                            }
                        }
                    </style>
                </head>
                <body>
                    <main>
                        <h1>Space Hyper</h1>
                        <p> Java application is running on AWS EC2.</p>
                        <span class="badge">Deployed automatically with Jenkins CI/CD</span>
                        <nav aria-label="Route navigation">
                            <a href="/about">About</a>
                            <a href="/contact">Contact</a>
                            <a href="/deploy">Deploy</a>
                            <a href="/binaya">Binaya</a>
                        </nav>
                    </main>
                </body>
                </html>
                """;
    }
}
